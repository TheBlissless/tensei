package eu.kanade.tachiyomi.animesource.model

/**
 * Container returned by `AnimeSource.getAnimeSeasonUpdate(...)` —
 * the seasons-flavoured version of `SAnimeEpisodeUpdate`.
 *
 * Ported from Aniyomi's `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/SAnimeSeasonUpdate.kt`.
 *
 * Some anime sources model multi-season anime as a flat episode list
 * (use `SAnimeEpisodeUpdate` for those); others model them as a tree
 * of SAnime children (use `SAnimeSeasonUpdate` for those).
 */
data class SAnimeSeasonUpdate(
    val anime: SAnime,
    val seasons: List<SAnime>,
)
