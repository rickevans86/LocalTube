package com.localtube.app.data

import com.localtube.app.data.db.MetaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Keyless artwork/metadata: Cinemeta for search/metadata, Metahub for images.
 * kind is "movie" or "series".
 */
object ArtworkClient {
    private const val CINEMETA = "https://v3-cinemeta.strem.io"
    private const val METAHUB = "https://images.metahub.space"

    fun posterUrl(imdbId: String): String = "$METAHUB/poster/medium/$imdbId/img"
    fun backdropUrl(imdbId: String): String = "$METAHUB/background/medium/$imdbId/img"

    data class SearchResult(
        val imdbId: String,
        val title: String,
        val year: String?,
        val poster: String?,
    )

    suspend fun search(kind: String, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val json = getJson("$CINEMETA/catalog/$kind/top/search=$encoded.json") ?: return@withContext emptyList()
            val metas = json.optJSONArray("metas") ?: return@withContext emptyList()
            (0 until metas.length()).mapNotNull { i ->
                val o = metas.getJSONObject(i)
                val id = o.optString("id")
                if (!id.startsWith("tt")) return@mapNotNull null
                SearchResult(
                    imdbId = id,
                    title = o.optString("name", query),
                    year = o.optString("releaseInfo").ifBlank { null },
                    poster = o.optString("poster").ifBlank { null },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun metaById(kind: String, imdbId: String): SearchResult? = withContext(Dispatchers.IO) {
        try {
            val json = getJson("$CINEMETA/meta/$kind/$imdbId.json") ?: return@withContext null
            val meta = json.optJSONObject("meta") ?: return@withContext null
            SearchResult(
                imdbId = imdbId,
                title = meta.optString("name", imdbId),
                year = meta.optString("releaseInfo").ifBlank { null },
                poster = meta.optString("poster").ifBlank { null },
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Builds a MetaEntity for an item; null if nothing matched. */
    suspend fun match(kind: String, key: String, query: String, imdbHint: String?): MetaEntity? {
        val id = imdbHint ?: search(kind, query).firstOrNull()?.imdbId ?: return null
        val info = metaById(kind, id)
        return MetaEntity(
            itemKey = key,
            kind = kind,
            query = query,
            imdbId = id,
            title = info?.title ?: query,
            year = info?.year,
            posterUrl = posterUrl(id),
            backdropUrl = backdropUrl(id),
            manual = false,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private fun getJson(urlStr: String): JSONObject? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != 200) {
                null
            } else {
                JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            }
        } finally {
            conn.disconnect()
        }
    }
}
