package com.king.wms.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.king.wms.BuildConfig
import com.king.wms.data.api.WmsApi
import com.king.wms.data.repository.HostSelectionInterceptor
import com.king.wms.data.repository.InMemoryCookieJar
import com.king.wms.data.repository.TokenAuthenticator
import com.king.wms.data.repository.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttp(
        tokenStore: TokenStore,
        cookieJar: InMemoryCookieJar,
        authenticator: TokenAuthenticator,
        hostSelection: HostSelectionInterceptor,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            // Refresh the access token on 401 and retry once.
            .authenticator(authenticator)
            // Re-point scheme/host/port to the runtime server (Settings), if configured.
            .addInterceptor(hostSelection)
            .addInterceptor { chain ->
                // Attach the bearer token, unless the request already carries one
                // (e.g. the authenticator's retry, which sets the fresh token itself).
                val req = chain.request()
                val out = if (req.header("Authorization") == null) {
                    val token = runBlocking { tokenStore.token.first() }
                    if (!token.isNullOrBlank()) {
                        req.newBuilder().addHeader("Authorization", "Bearer $token").build()
                    } else req
                } else req
                chain.proceed(out)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): WmsApi = retrofit.create(WmsApi::class.java)
}
