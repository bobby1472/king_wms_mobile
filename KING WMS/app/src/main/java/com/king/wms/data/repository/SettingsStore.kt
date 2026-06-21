package com.king.wms.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.king.wms.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "king_wms_settings")

/** Persists an optional runtime server address so the app can be re-pointed without a rebuild. */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serverKey = stringPreferencesKey("server_authority")

    /** The raw value the operator typed, e.g. "192.168.1.150:4000" (null/blank = use build default). */
    val server: Flow<String?> = context.settingsDataStore.data.map { it[serverKey] }

    suspend fun save(value: String) {
        context.settingsDataStore.edit { it[serverKey] = value.trim() }
    }

    suspend fun clear() {
        context.settingsDataStore.edit { it.remove(serverKey) }
    }
}

/** Parse a typed server ("192.168.1.150:4000" or "http://host:port") into an HttpUrl. */
fun serverToHttpUrl(input: String?): HttpUrl? {
    val s = input?.trim().orEmpty()
    if (s.isBlank()) return null
    val withScheme = if (s.startsWith("http://") || s.startsWith("https://")) s else "http://$s"
    return withScheme.toHttpUrlOrNull()
}

/** Effective API base URL (ends with /api/v1/): the runtime override if set, else the build default. */
fun effectiveBaseUrl(serverAuthority: String?): String {
    val u = serverToHttpUrl(serverAuthority) ?: return BuildConfig.API_BASE_URL
    return "${u.scheme}://${u.host}:${u.port}/api/v1/"
}

/**
 * Rewrites each request's scheme/host/port to the configured runtime server (if any),
 * leaving the path (`/api/v1/...`) intact. No override → requests hit the build-time host.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val settings: SettingsStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val parsed = serverToHttpUrl(runBlocking { settings.server.first() })
        val request = if (parsed != null) {
            val url = chain.request().url.newBuilder()
                .scheme(parsed.scheme).host(parsed.host).port(parsed.port).build()
            chain.request().newBuilder().url(url).build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
