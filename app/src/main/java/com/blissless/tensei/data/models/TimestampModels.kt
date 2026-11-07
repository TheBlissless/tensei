package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// Skip-timestamp models for the player (intro/outro markers from Animekai,
// AniSkip and AnimeThemes — see player/skip/).



// ============================================
// SKIP TIMESTAMP MODELS
// ============================================

/**
 * Represents a single skip timestamp marker (intro, outro, recap, etc.)
 *
 * @param startTime The start time in seconds
 * @param skipType The type of skip ("op", "ed", "recap", "mixed-ed", "mixed-op")
 * @param skipId Unique identifier for this skip marker
 */
@Serializable
data class Timestamp(
    val startTime: Double,
    val skipType: String,
    val skipId: String
)


/**
 * Represents all skip timestamps for an episode.
 * Contains intro/outro/recap timing information for skip functionality.
 *
 * @param episodeNumber The episode number these timestamps belong to
 * @param introStart Start time of the opening in seconds (null if no intro)
 * @param introEnd End time of the opening in seconds (null if no intro)
 * @param creditsStart Start time of the ending credits in seconds (null if no outro)
 * @param creditsEnd End time of the ending credits in seconds (null if no outro)
 * @param recapStart Start time of a recap in seconds (null if no recap)
 * @param recapEnd End time of a recap in seconds (null if no recap)
 * @param allTimestamps List of all timestamp markers for this episode
 */
@Serializable
data class EpisodeTimestamps(
    val episodeNumber: Int,
    val introStart: Long? = null,
    val introEnd: Long? = null,
    val creditsStart: Long? = null,
    val creditsEnd: Long? = null,
    val recapStart: Long? = null,
    val recapEnd: Long? = null,
    val allTimestamps: List<Timestamp> = emptyList()
) {
    /**
     * Check if this episode has any valid skip timestamps
     */
    fun hasTimestamps(): Boolean {
        return introStart != null || creditsStart != null || recapStart != null
    }

}
