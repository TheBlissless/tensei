package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// Stream-selection models: available servers, qualities and resolved episode
// streams shown in the player UI.



// ============================================
// STREAM MODELS (API)
// ============================================

/**
 * Quality option for a stream (e.g., 1080p, 720p, 480p).
 */
data class QualityOption(
    val quality: String,   // e.g., "1080p", "720p", "480p"
    val url: String,
    val width: Int
)


/**
 * Result of a stream fetch operation.
 * Includes skip timestamps from Animekai embed.
 */
data class AniwatchStreamResult(
    val url: String,
    val isDirectStream: Boolean = true,
    val headers: Map<String, String>? = null,
    val subtitleUrl: String? = null,
    val serverName: String = "Unknown",
    val category: String = "sub",
    val qualities: List<QualityOption> = emptyList(),
    // Skip timestamps from Animekai (in seconds)
    val introStart: Int? = null,
    val introEnd: Int? = null,
    val outroStart: Int? = null,
    val outroEnd: Int? = null
)


/**
 * Server information for an episode.
 */
data class ServerInfo(
    val name: String,
    val url: String,
    val qualities: List<QualityOption> = emptyList()
)


/**
 * Episode streams containing sub and dub servers.
 */
data class EpisodeStreams(
    val subServers: List<ServerInfo>,
    val dubServers: List<ServerInfo>,
    val animeId: String = "",
    val episodeId: String = ""
)
