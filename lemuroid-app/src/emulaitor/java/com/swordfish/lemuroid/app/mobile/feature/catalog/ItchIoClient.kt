package com.swordfish.lemuroid.app.mobile.feature.catalog

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * ItchIoClient - HTTP client for itch.io public RSS + API endpoints.
 *
 * Discovery: public RSS feeds (no auth needed)
 * Downloads: API key required for /games/{id}/uploads and /uploads/{id}/download
 */
class ItchIoClient {

    companion object {
        private const val TAG = "Catalog.ItchIo"
        private const val ITCH_BASE = "https://itch.io"
        private const val ITCH_API = "https://api.itch.io"
        private const val USER_AGENT = "EmulAItor/1.0"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val gson = Gson()

    /**
     * Search games by itch.io tag using the public RSS feed, then filter to free ones only.
     *
     * NOTE: The `/games/free/tag-X.xml` endpoint is blocked by Cloudflare (HTTP 403),
     * so we use `/games/tag-X.xml` and filter client-side by `price == "$0.00"`.
     *
     * No API key required.
     * Returns list of free ItchGame parsed from XML.
     */
    suspend fun searchGamesByTag(
        tag: String,
        page: Int = 1
    ): List<ItchGame> = withContext(Dispatchers.IO) {
        val url = "$ITCH_BASE/games/tag-${URLEncoder.encode(tag, "UTF-8")}.xml?page=$page"
        Log.d(TAG, "Searching itch.io RSS: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "RSS HTTP Error: ${response.code} for $url")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val all = parseRssGames(body, tag)
            // Filter free games only (price == "$0.00" or starts with "$0")
            val free = all.filter { it.price.trim().startsWith("$0") }
            Log.d(TAG, "Parsed ${all.size} total games, ${free.size} free (tag=$tag)")
            free
        } catch (e: Exception) {
            Log.e(TAG, "RSS search failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Resolve game_id from a game URL using the public data.json endpoint.
     * No API key required.
     */
    suspend fun getGameIdFromUrl(gameUrl: String): Int? = withContext(Dispatchers.IO) {
        val dataUrl = "${gameUrl.trimEnd('/')}/data.json"
        Log.d(TAG, "Resolving game ID: $dataUrl")

        try {
            val request = Request.Builder()
                .url(dataUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "data.json HTTP Error: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string() ?: return@withContext null
            val json = gson.fromJson(body, Map::class.java) as? Map<String, Any>
            (json?.get("id") as? Number)?.toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve game ID: ${e.message}")
            null
        }
    }

    /**
     * List uploads for a game. Requires API key.
     * Returns only ROM-candidate uploads (filtered by ItchUpload.isRomCandidate).
     */
    suspend fun listUploads(
        gameId: Int,
        apiKey: String
    ): List<ItchUpload> = withContext(Dispatchers.IO) {
        val url = "$ITCH_API/games/$gameId/uploads?api_key=$apiKey"
        Log.d(TAG, "Listing uploads for game $gameId")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Uploads HTTP Error: ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val apiResponse = gson.fromJson(body, JsonItchUploadsResponse::class.java)

            apiResponse.uploads.map { jsonUpload ->
                ItchUpload(
                    id = jsonUpload.id,
                    filename = jsonUpload.filename,
                    displayName = jsonUpload.displayName,
                    size = jsonUpload.size,
                    type = jsonUpload.type,
                    traits = jsonUpload.traits,
                    storage = jsonUpload.storage,
                    createdAt = jsonUpload.createdAt
                )
            }.filter { it.isRomCandidate }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list uploads: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get download URL for a specific upload. Requires API key.
     * itch.io may respond with:
     *   - An HTTP 302 redirect to the final URL (we capture Location header)
     *   - A JSON object: {"url": "https://..."}
     *   - A JSON string: "https://..."
     *   - A plain URL text body
     * Returns the resolved download URL or null.
     */
    suspend fun getDownloadUrl(
        uploadId: Int,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        // Use a non-redirecting client so we can capture the Location header if present.
        val noRedirectClient = httpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val url = "$ITCH_API/uploads/$uploadId/download?api_key=$apiKey"
        Log.d(TAG, "Getting download URL for upload $uploadId")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = noRedirectClient.newCall(request).execute()

            // Redirect case: return Location header directly
            if (response.code in 300..399) {
                return@withContext response.header("Location").also {
                    Log.d(TAG, "Resolved via redirect: $it")
                }
            }

            if (!response.isSuccessful) {
                Log.e(TAG, "Download URL HTTP Error: ${response.code}")
                return@withContext null
            }

            val body = response.body?.string()?.trim()
            if (body.isNullOrBlank()) return@withContext null

            // Try JSON object {"url": "..."}
            try {
                val resp = gson.fromJson(body, ItchDownloadResponse::class.java)
                if (!resp.url.isNullOrBlank()) return@withContext resp.url
            } catch (_: Exception) { /* fallthrough */ }

            // Try JSON string "..."
            try {
                val str = gson.fromJson(body, String::class.java)
                if (!str.isNullOrBlank() && str.startsWith("http")) return@withContext str
            } catch (_: Exception) { /* fallthrough */ }

            // Plain URL text
            if (body.startsWith("http")) return@withContext body

            Log.e(TAG, "Unrecognized download URL response: ${body.take(100)}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get download URL: ${e.message}")
            null
        }
    }

    /**
     * Validate an API key by pinging the profile endpoint.
     * Returns true if the key is valid.
     */
    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val url = "$ITCH_API/profile?api_key=$apiKey"
        Log.d(TAG, "Validating API key...")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            val valid = response.isSuccessful
            Log.d(TAG, "API key validation result: $valid (${response.code})")
            valid
        } catch (e: Exception) {
            Log.e(TAG, "API key validation failed: ${e.message}")
            false
        }
    }

    /**
     * Get the username associated with an API key (for display purposes).
     */
    suspend fun getProfileName(apiKey: String): String? = withContext(Dispatchers.IO) {
        val url = "$ITCH_API/profile?api_key=$apiKey"

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val profileResp = gson.fromJson(body, JsonItchProfileResponse::class.java)
            profileResp.user?.displayName ?: profileResp.user?.username
        } catch (e: Exception) {
            null
        }
    }

    // --- RSS XML Parsing ---

    private fun parseRssGames(xml: String, tag: String): List<ItchGame> {
        val games = mutableListOf<ItchGame>()
        try {
            // Simple XML parsing for RSS items (no external XML library needed)
            val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            val items = itemRegex.findAll(xml)

            for (itemMatch in items) {
                val itemXml = itemMatch.groupValues[1]
                val guid = extractXmlTag(itemXml, "guid") ?: continue
                val title = extractXmlTag(itemXml, "title") ?: continue
                val plainTitle = extractXmlTag(itemXml, "plainTitle") ?: title
                val imageUrl = extractXmlTag(itemXml, "imageurl")
                val price = extractXmlTag(itemXml, "price") ?: "$0.00"
                val currency = extractXmlTag(itemXml, "currency") ?: "USD"
                val link = extractXmlTag(itemXml, "link") ?: guid
                val rawDescription = extractXmlTag(itemXml, "description")
                val pubDate = extractXmlTag(itemXml, "pubDate")
                val platforms = extractPlatforms(itemXml)

                // Extract img src from description if coverUrl is missing
                val imgSrcFromDesc = rawDescription?.let {
                    Regex("""<img[^>]+src="([^"]+)"""").find(it)?.groupValues?.get(1)
                }
                val cleanDescription = rawDescription?.let { stripHtml(it) }

                // Determine systemId from tag
                val systemId = ItchSystemTagMap.mapping.entries
                    .firstOrNull { it.value == tag }?.key ?: tag

                games.add(ItchGame(
                    id = guid,
                    title = title,
                    plainTitle = plainTitle,
                    url = link,
                    coverUrl = imageUrl ?: imgSrcFromDesc,
                    price = price,
                    currency = currency,
                    description = cleanDescription?.take(300),
                    pubDate = pubDate,
                    platforms = platforms,
                    systemId = systemId,
                    itchTag = tag
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "RSS parse error: ${e.message}")
        }

        Log.d(TAG, "Parsed ${games.size} games from RSS (tag=$tag)")
        return games
    }

    private fun extractXmlTag(xml: String, tag: String): String? {
        // Handle CDATA: <tag><![CDATA[value]]></tag>
        val cdataRegex = Regex("<$tag><!\\[CDATA\\[(.*?)\\]\\]></$tag>", RegexOption.DOT_MATCHES_ALL)
        cdataRegex.find(xml)?.groupValues?.get(1)?.trim()?.let { return it }

        // Handle plain: <tag>value</tag>
        val plainRegex = Regex("<$tag>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL)
        return plainRegex.find(xml)?.groupValues?.get(1)?.trim()
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<img[^>]*>"), "")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractPlatforms(itemXml: String): String {
        val platforms = mutableListOf<String>()
        val platformSection = extractXmlTag(itemXml, "platforms") ?: return ""
        if (platformSection.contains("<windows>yes</windows>")) platforms.add("Windows")
        if (platformSection.contains("<osx>yes</osx>")) platforms.add("macOS")
        if (platformSection.contains("<linux>yes</linux>")) platforms.add("Linux")
        if (platformSection.contains("<android>yes</android>")) platforms.add("Android")
        if (platformSection.contains("<html>yes</html>")) platforms.add("Web")
        return platforms.joinToString(", ")
    }
}
