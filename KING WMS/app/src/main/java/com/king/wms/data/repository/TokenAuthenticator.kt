package com.king.wms.data.repository

import com.king.wms.data.model.ApiResponse
import com.king.wms.data.model.RefreshData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a 401, transparently mint a fresh access token via POST /auth/refresh (which
 * uses the refresh cookie held by [cookieJar]) and retry the original request once.
 * If refresh fails the call surfaces the 401 and the UI sends the user back to login.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val cookieJar: InMemoryCookieJar,
    private val settings: SettingsStore,
    private val json: Json,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once (two Authorization-bearing responses in the chain)? Give up.
        if (responseCount(response) >= 2) return null
        val newToken = runBlocking { refresh() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private suspend fun refresh(): String? {
        // A bare client that shares the cookie jar, so the httpOnly refresh cookie is sent.
        val client = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val base = effectiveBaseUrl(settings.server.first())
        val req = Request.Builder()
            .url(base + "auth/refresh")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string()
                if (!resp.isSuccessful || body == null) return null
                val parsed = json.decodeFromString(
                    ApiResponse.serializer(RefreshData.serializer()), body
                )
                val token = parsed.data?.accessToken ?: return null
                tokenStore.save(token)
                token
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
