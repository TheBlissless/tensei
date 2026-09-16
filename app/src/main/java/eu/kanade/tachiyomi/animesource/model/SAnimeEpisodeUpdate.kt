package eu.kanade.tachiyomi.animesource.model

/**
 * Container returned by `AnimeSource.getAnimeEpisodeUpdate(...)` —
 * Aniyomi's combined API that fetches the refreshed anime details
 * and the refreshed episode list in a single round-trip.
 *
 * Ported from Aniyomi's `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/SAnimeEpisodeUpdate.kt`.
 *
 * Why a combined container instead of a Pair? Because some sources may
 * want to populate additional fields when both fetches run together
 * (e.g., the new episode list depends on data from the refreshed
 * anime details — `season_number`, `status`, etc.).
 */
data class SAnimeEpisodeUpdate(
    val anime: SAnime,
    val episodes: List<SEpisode>,
)
