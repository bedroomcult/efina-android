package com.efina.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Checks GitHub for a newer release of the app.
 *
 * Only runs when the user has enabled "Notify new updates" in Settings.
 * Fetches the latest release from the GitHub API and compares its tag against
 * the current [BuildConfig.VERSION_NAME].
 */
@Singleton
class UpdateChecker @Inject constructor(
    private val preferences: PreferencesManager
) {

    data class UpdateInfo(val version: String, val url: String)

    /**
     * Returns an [UpdateInfo] when a newer release exists and notifications are enabled,
     * otherwise null. Never throws — network or parse failures silently return null.
     */
    suspend fun checkForUpdatesIfEnabled(currentVersion: String): UpdateInfo? {
        val enabled = withContext(Dispatchers.IO) { preferences.updateNotifications.first() }
        if (!enabled) return null
        return try {
            checkGitHubRelease(currentVersion)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun checkGitHubRelease(currentVersion: String): UpdateInfo? {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.github.com/repos/bedroomcult/efina-android/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "efina-android")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body()?.string() ?: return null

        val json = Json { ignoreUnknownKeys = true }
        val release = json.decodeFromString<GitHubRelease>(body)

        if (isNewer(release.tagName, currentVersion)) {
            return UpdateInfo(release.tagName, release.htmlUrl)
        }
        return null
    }

    /** Compares semantic versions like "v1.0.28" vs "1.0.27". */
    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(r.size, l.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrDefault(i, 0)
            val lv = l.getOrDefault(i, 0)
            if (rv != lv) return rv > lv
        }
        return false
    }
}

@Serializable
private data class GitHubRelease(
    val tag_name: String,
    val html_url: String
)

private val GitHubRelease.tagName: String get() = tag_name
private val GitHubRelease.htmlUrl: String get() = html_url
