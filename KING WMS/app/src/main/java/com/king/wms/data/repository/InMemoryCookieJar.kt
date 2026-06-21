package com.king.wms.data.repository

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal in-memory cookie jar. The backend sets the refresh token as an httpOnly
 * cookie on /auth/login; retaining it (per host) lets /auth/refresh mint a new
 * access token when the short-lived one (15 min) expires. Single warehouse host,
 * so a flat per-host store is enough — cookies are dropped when the process dies,
 * which is fine (the user simply logs in again).
 */
@Singleton
class InMemoryCookieJar @Inject constructor() : CookieJar {
    private val byHost = HashMap<String, MutableMap<String, Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val jar = byHost.getOrPut(url.host) { mutableMapOf() }
        cookies.forEach { jar[it.name] = it }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        byHost[url.host]?.values?.toList() ?: emptyList()

    @Synchronized
    fun clear() = byHost.clear()
}
