package com.blissless.tensei.ui.navigation

import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData

/**
 * Shared helpers for the overlay/detail navigation used by the root screen's
 * overlay hosts ([AnimeOverlays], [InfoOverlays], [MangaNavHost]).
 */

/** AniList media formats that should open the manga detail instead of the anime detail. */
val MANGA_FORMATS = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")

/**
 * Maps fetched [DetailedAnimeData] to the [ExploreAnime] shape used by the
 * overlay dialogs.
 *
 * Note: `malId` is intentionally omitted — every original inline mapping that
 * used this shape left it at the default. The one call site that needs it
 * ([MainViewModel.fetchDetailedAnimeData]-based "open by AniList id" from the
 * home screen) builds its own [ExploreAnime].
 */
fun DetailedAnimeData.toExploreAnime(): ExploreAnime = ExploreAnime(
    id = id,
    title = title,
    titleEnglish = titleEnglish,
    cover = cover,
    banner = banner,
    episodes = episodes,
    latestEpisode = latestEpisode,
    averageScore = averageScore,
    genres = genres,
    year = year,
    format = format,
)

/** True when a relation with this format should open the manga detail. */
fun isMangaFormat(format: String?): Boolean = format == null || format in MANGA_FORMATS

/**
 * Fetches full anime data by AniList id and maps it for overlay navigation.
 * Returns null when the fetch fails; callers decide how to report it.
 */
suspend fun fetchAnimeForOverlay(viewModel: MainViewModel, animeId: Int): ExploreAnime? =
    viewModel.fetchDetailedAnimeData(animeId)?.toExploreAnime()

/**
 * Maps fetched [DetailedAnimeData] to a fresh, untracked [AnimeMedia].
 *
 * Used when a relation/recommendation opens the full-screen (status-list)
 * anime detail: progress starts at 0 and list tracking fields are empty —
 * the detail screen reads live tracking state from the view model instead.
 */
fun DetailedAnimeData.toAnimeMedia(): AnimeMedia = AnimeMedia(
    id = id,
    title = title,
    titleEnglish = titleEnglish,
    cover = cover,
    banner = banner,
    progress = 0,
    totalEpisodes = episodes,
    latestEpisode = latestEpisode,
    status = status ?: "",
    averageScore = averageScore,
    genres = genres,
    listStatus = "",
    listEntryId = 0,
    year = year,
    malId = malId,
)
