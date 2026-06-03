package com.swordfish.lemuroid.app.mobile.feature.metadata

import com.google.gson.Gson
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.StorageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.URLEncoder
import com.swordfish.lemuroid.BuildConfig

class TheGamesDBMetadataProvider(
    private val client: OkHttpClient,
    private val settingsManager: SettingsManager,
    private val gson: Gson = Gson()
) : GameMetadataProvider {

    companion object {
        private const val BASE_URL = "https://api.thegamesdb.net/v1/Games/ByGameName"
        private const val FIELDS = "overview,genres,publishers,developers,release_date"
        private const val INCLUDE = "boxart,developers,publishers"
    }

    override suspend fun retrieveMetadata(storageFile: StorageFile): GameMetadata? = withContext(Dispatchers.IO) {
        // Use key from local.properties (injected via BuildConfig), or fallback if empty
        val userKey = settingsManager.theGamesDbApiKey()
        val apiKey = if (userKey.isNotBlank()) userKey else BuildConfig.THEGAMESDB_API_KEY

        val searchName = cleanRomName(storageFile.extensionlessName)
        Timber.d("Searching TheGamesDB for: $searchName")
        
        return@withContext searchByName(searchName, apiKey)
    }

    private fun searchByName(name: String, apiKey: String): GameMetadata? {
        return searchByNameMultiple(name, apiKey).firstOrNull()
    }

    private fun cleanRomName(name: String): String {
        return name
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*\\]"), "")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }

    override suspend fun searchByName(name: String): List<GameMetadata> = withContext(Dispatchers.IO) {
        val userKey = settingsManager.theGamesDbApiKey()
        val apiKey = if (userKey.isNotBlank()) userKey else BuildConfig.THEGAMESDB_API_KEY
        
        Timber.d("Searching multiple results from TheGamesDB for: $name")
        return@withContext searchByNameMultiple(name, apiKey)
    }

    private fun searchByNameMultiple(name: String, apiKey: String): List<GameMetadata> {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val url = "$BASE_URL?apikey=$apiKey&name=$encodedName&fields=$FIELDS&include=$INCLUDE"
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "EmulAItor/1.0")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.w("TheGamesDB request failed: ${response.code}")
                return emptyList()
            }

            val body = response.body?.string() ?: return emptyList()
            Timber.d("TheGamesDB response: $body")
            val apiResponse = gson.fromJson(body, TGDBResponse::class.java)
            
            val games = apiResponse.data?.games ?: return emptyList()
            val boxartBaseUrl = apiResponse.include?.boxart?.base_url?.thumb ?: ""
            val boxartData = apiResponse.include?.boxart?.data ?: emptyMap()
            val developersMap = apiResponse.include?.developers ?: emptyMap()
            val publishersMap = apiResponse.include?.publishers ?: emptyMap()

            return games.mapNotNull { game ->
                val gameId = game.id?.toString() ?: return@mapNotNull null

                // Resolver nombre de developer desde el mapa include
                val developerName = game.developers?.firstOrNull()?.let { devId ->
                    developersMap[devId]?.name ?: devId.toString()
                }
                val publisherName = game.publishers?.firstOrNull()?.let { pubId ->
                    publishersMap[pubId]?.name ?: pubId.toString()
                }

                // Buscar carátula frontal (tipo "front") o la primera disponible
                val coverImage = boxartData[gameId]
                    ?.firstOrNull { it.type == "front" }
                    ?: boxartData[gameId]?.firstOrNull()
                val thumbnailUrl = if (coverImage != null && boxartBaseUrl.isNotBlank()) {
                    "$boxartBaseUrl${coverImage.filename}"
                } else null

                GameMetadata(
                    name = game.game_title,
                    romName = null,
                    system = null,
                    developer = developerName,
                    publisher = publisherName,
                    year = game.release_date?.take(4)?.toIntOrNull(),
                    genre = game.genres?.firstOrNull()?.let { genreId ->
                        apiResponse.include?.genres?.get(genreId)?.name ?: genreId.toString()
                    },
                    description = game.overview,
                    thumbnail = thumbnailUrl
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "Error searching TheGamesDB for multiple results")
            return emptyList()
        }
    }

    // JSON Data Structures
    data class TGDBResponse(
        val data: TGDBData?,
        val include: TGDBInclude?
    )
    data class TGDBData(val games: List<TGDBGame>?)
    data class TGDBGame(
        val id: Int?,
        val game_title: String?,
        val release_date: String?,
        val overview: String?,
        val developers: List<Int>?,
        val publishers: List<Int>?,
        val genres: List<Int>?
    )
    data class TGDBInclude(
        val boxart: TGDBBoxart?,
        val developers: Map<Int, TGDBNamedEntity>?,
        val publishers: Map<Int, TGDBNamedEntity>?,
        val genres: Map<Int, TGDBNamedEntity>?
    )
    data class TGDBBoxart(
        val base_url: TGDBBaseUrl?,
        val data: Map<String, List<TGDBImage>>?
    )
    data class TGDBBaseUrl(
        val original: String?,
        val small: String?,
        val thumb: String?,
        val cropped_center_thumb: String?,
        val medium: String?,
        val large: String?
    )
    data class TGDBImage(
        val id: Int?,
        val type: String?,
        val side: String?,
        val filename: String?,
        val resolution: String?
    )
    data class TGDBNamedEntity(val id: Int?, val name: String?)
}
