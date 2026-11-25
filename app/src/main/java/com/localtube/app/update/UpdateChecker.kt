package com.localtube.app.update

import com.localtube.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateResult {
    data class Available(val tag: String, val apkUrl: String) : UpdateResult
    data object UpToDate : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * Checks GitHub Releases for a newer signed APK. The repo is public so no token
 * is needed. Release tags are v1.0.<versionCode>, so the trailing integer is the
 * version code to compare against the installed build.
 */
class UpdateChecker {

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = conn.responseCode
            if (code != 200) return@withContext UpdateResult.Failed("HTTP $code")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(body)
            val tag = json.getString("tag_name")
            val latestCode = tag.substringAfterLast('.').toIntOrNull()
                ?: return@withContext UpdateResult.Failed("Unparseable tag $tag")
            val assets = json.getJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            when {
                latestCode <= BuildConfig.VERSION_CODE -> UpdateResult.UpToDate
                apkUrl == null -> UpdateResult.Failed("No APK asset in $tag")
                else -> UpdateResult.Available(tag, apkUrl)
            }
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: "Network error")
        }
    }

    companion object {
        private const val OWNER = "rickevans86"
        private const val REPO = "LocalTube"
        private const val LATEST_URL =
            "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    }
}
