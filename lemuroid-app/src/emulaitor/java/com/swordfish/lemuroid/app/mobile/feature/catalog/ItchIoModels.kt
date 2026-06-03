package com.swordfish.lemuroid.app.mobile.feature.catalog

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * itch.io game discovered via RSS browse endpoint
 */
data class ItchGame(
    val id: String,
    val title: String,
    val plainTitle: String,
    val url: String,
    val coverUrl: String?,
    val price: String,
    val currency: String,
    val description: String?,
    val pubDate: String?,
    val platforms: String,
    val systemId: String,
    val itchTag: String
) {
    val isFree: Boolean get() = price == "$0.00" || price.isBlank()

    val sizeFormatted: String get() = when {
        platforms.isBlank() -> "Homebrew"
        else -> platforms.replace("yes", "").replace(Regex("\\s+"), " ").trim().ifBlank { "Homebrew" }
    }

    val flag: String get() = "🎮"

    val author: String get() {
        // Extract author from URL: https://author.itch.io/game
        val regex = Regex("https://([\\w\\d\\-_]+)\\.itch\\.io")
        return regex.find(url)?.groupValues?.getOrNull(1) ?: ""
    }
}

/**
 * Upload entry from itch.io API (GET /games/{id}/uploads)
 */
data class ItchUpload(
    val id: Int,
    val filename: String,
    val displayName: String?,
    val size: Long?,
    val type: String,
    val traits: Map<String, Boolean>,
    val storage: String,
    val createdAt: String?
) {
    val isExternal: Boolean get() = storage == "external"

    val isRomCandidate: Boolean get() {
        if (isExternal) return false
        // Skip non-ROM types: executables, demos, soundtracks
        if (type == "soundtrack" || type == "manual" || type == "other") return false
        val ext = filename.substringAfterLast('.', "").lowercase()
        // Skip PC executables and Android APKs
        if (ext in setOf("exe", "msi", "dmg", "app", "deb", "rpm", "apk", "appimage", "sh")) return false
        // Accept known ROM extensions or generic archives
        return ext in setOf("nes", "sfc", "smc", "gba", "gbc", "gb", "gen", "md", "sms", "gg",
            "n64", "z64", "v64", "iso", "bin", "cue", "chd", "pce", "zip", "7z", "rar")
    }

    val sizeFormatted: String get() = when (val s = size ?: 0L) {
        in 0..1023 -> "$s B"
        in 1024..1024 * 1024 - 1 -> "${s / 1024} KB"
        in 1024 * 1024..1024 * 1024 * 1024 - 1 -> "${s / (1024 * 1024)} MB"
        else -> "${s / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Download URL response from itch.io API (GET /uploads/{id}/download)
 */
data class ItchDownloadResponse(
    val url: String?
)

// --- JSON parsing DTOs (Gson @Keep) ---

@Keep
data class JsonItchUploadsResponse(
    val uploads: List<JsonItchUpload> = emptyList()
)

@Keep
data class JsonItchUpload(
    val id: Int = 0,
    val filename: String = "",
    @SerializedName("display_name") val displayName: String? = null,
    val size: Long? = null,
    val type: String = "default",
    // itch.io returns an object like {"p_windows": true, "p_linux": false}, not an array
    val traits: Map<String, Boolean> = emptyMap(),
    val storage: String = "internal",
    @SerializedName("created_at") val createdAt: String? = null
)

@Keep
data class JsonItchProfileResponse(
    val user: JsonItchUser? = null
)

@Keep
data class JsonItchUser(
    val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)

/**
 * Mapping from EmulAItor systemId to itch.io browse tag
 */
object ItchSystemTagMap {
    // itch.io tags that exist and return 200 OK for `/games/tag-<X>.xml`
    val mapping: Map<String, String> = mapOf(
        "nes" to "nes",
        "snes" to "snes",
        "gba" to "game-boy-advance",
        "gbc" to "gameboy",
        "genesis" to "sega-genesis"
    )

    /** Systems with no meaningful itch.io homebrew content */
    val unsupportedSystems: Set<String> = setOf("n64", "psx", "psp", "arcade")

    fun getTag(systemId: String): String? = mapping[systemId]

    fun hasTag(systemId: String): Boolean = systemId in mapping

    fun isUnsupported(systemId: String): Boolean = systemId in unsupportedSystems
}
