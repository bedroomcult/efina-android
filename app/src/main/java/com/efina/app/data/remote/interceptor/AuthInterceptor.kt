package com.efina.app.data.remote.interceptor

import com.efina.app.util.Constants
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches credentials to every outbound request.
 *
 * Mirrors what src/utils/apiFetch.ts does for the web client, because the backend
 * requires the same two things:
 *
 *  1. `Authorization: Bearer <token>` — _middleware.js accepts a session token
 *     from the cookie *or* this header, and validates the session id inside it
 *     against the `sessions` table on every request.
 *
 *  2. `X-CSRF-Token: <csrf>` on state-changing verbs. _middleware.js calls
 *     requiresCsrfProtection(method) and rejects a missing token with 403. The
 *     web client sends it from localStorage; native holds it in
 *     EncryptedSharedPreferences. Without this header every POST/PUT/PATCH/DELETE
 *     from the app would fail with 403 regardless of a valid session.
 *
 * Login and register are exempt: there is no session yet, so the middleware's
 * public route list skips them.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        if (isPublic(path)) return chain.proceed(original)

        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        tokenManager.getToken()?.let { builder.header("Authorization", "Bearer $it") }

        // CSRF applies to state-changing methods only, matching the middleware.
        if (original.method in CSRF_METHODS) {
            tokenManager.getCsrfToken()?.let { builder.header("X-CSRF-Token", it) }
        }

        return chain.proceed(builder.build())
    }

    private fun isPublic(path: String): Boolean =
        path.contains("auth/login") ||
            path.contains("auth/register") ||
            path.contains("auth/reset-password") ||
            path.contains("api/public/")

    private companion object {
        val CSRF_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }
}

/**
 * Single source of truth for the session. Backed by EncryptedSharedPreferences so
 * the token and CSRF secret are not readable from a rooted device's app data.
 */
@Singleton
class TokenManager @Inject constructor(
    prefs: SecurePrefs
) {
    private val store = prefs.secure

    fun saveSession(token: String, csrfToken: String?, userId: Long, email: String) {
        store.edit()
            .putString(Constants.KEY_AUTH_TOKEN, token)
            .putString(Constants.KEY_CSRF_TOKEN, csrfToken)
            .putLong(Constants.KEY_USER_ID, userId)
            .putString(Constants.KEY_USER_EMAIL, email)
            .apply()
    }

    fun saveToken(token: String) {
        store.edit().putString(Constants.KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(): String? = store.getString(Constants.KEY_AUTH_TOKEN, null)

    fun getCsrfToken(): String? = store.getString(Constants.KEY_CSRF_TOKEN, null)

    fun saveUserId(userId: Long) {
        store.edit().putLong(Constants.KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long = store.getLong(Constants.KEY_USER_ID, -1L)

    fun saveUserEmail(email: String) {
        store.edit().putString(Constants.KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? = store.getString(Constants.KEY_USER_EMAIL, null)

    fun hasPin(): Boolean = store.getString(Constants.KEY_USER_PIN, null) != null

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearAll() = store.edit().clear().apply()
}
