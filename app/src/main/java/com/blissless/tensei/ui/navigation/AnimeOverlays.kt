package com.blissless.tensei.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.addExploreAnimeToList
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.removeAnimeFromList
import com.blissless.tensei.viewmodel.setLocalAnimeStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Hosts for the two anime detail overlay variants used by the root screen.
 *
 * Both wrap [DetailedAnimeScreen]; they differ in how the anime was opened:
 *
 *  - [ExploreAnimeDetailOverlay] — opened from an explore/home card as an
 *    [OverlayState.ExploreAnimeDialog], with its own back-stack of previously
 *    open dialogs (see [OverlayState.previousStates]).
 *  - [SelectedAnimeDetailOverlay] — opened from a status-list card with
 *    bounds-continuity (the screen grows out of the tapped card).
 *
 * Extracted verbatim from the [com.blissless.tensei.MainScreen] composable;
 * callbacks are provided by the root screen.
 */

/**
 * The anime detail overlay opened from explore/home cards.
 */
@Composable
fun ExploreAnimeDetailOverlay(
    dialog: OverlayState.ExploreAnimeDialog,
    viewModel: MainViewModel,
    isOled: Boolean,
    isLoggedIn: Boolean,
    preferEnglishTitles: Boolean,
    settingsReturnVersion: Int,
    animeStatusMap: Map<Int, String>,
    animeProgressMap: Map<Int, Int>,
    favoriteIds: Set<Int>,
    localAnimeStatus: Map<Int, LocalAnimeEntry>,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DetailedAnimeScreen(
        anime = dialog.anime.toDetailedAnimeData(),
        viewModel = viewModel,
        isOled = isOled,
        settingsReturnVersion = settingsReturnVersion,
        currentStatus = animeStatusMap[dialog.anime.id],
        currentProgress = animeProgressMap[dialog.anime.id],
        isFavorite = favoriteIds.contains(dialog.anime.id),
        initialCardBounds = viewModel.exploreAnimeCardBounds.collectAsState().value,
        stackDepth = dialog.previousStates.size + 1,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onSwipeToClose = { setOverlayState(OverlayState.None) },
        onPlayEpisode = { episode, _ ->
            val animeMedia = AnimeMedia(
                id = dialog.anime.id,
                title = dialog.anime.title,
                titleEnglish = dialog.anime.titleEnglish,
                cover = dialog.anime.cover,
                banner = dialog.anime.banner,
                progress = 0,
                totalEpisodes = dialog.anime.episodes,
                latestEpisode = dialog.anime.latestEpisode,
                status = "",
                averageScore = dialog.anime.averageScore,
                genres = dialog.anime.genres,
                listStatus = "",
                listEntryId = 0,
                year = dialog.anime.year,
                malId = dialog.anime.malId
            )
            viewModel.addExploreAnimeToList(dialog.anime, "CURRENT")
            onPlayEpisode(animeMedia, episode, null)
            setOverlayState(OverlayState.None)
        },
        onUpdateStatus = { status ->
            if (status != null) {
                viewModel.addExploreAnimeToList(dialog.anime, status)
            }
        },
        onRemove = {
            viewModel.removeAnimeFromList(dialog.anime.id)
        },
        onUpdateLocalStatus = { status ->
            val currentEntry = localAnimeStatus[dialog.anime.id]
            if (status != null) {
                viewModel.setLocalAnimeStatus(
                    dialog.anime.id,
                    LocalAnimeEntry(
                        id = dialog.anime.id,
                        status = status,
                        progress = currentEntry?.progress ?: 0,
                        totalEpisodes = dialog.anime.episodes
                    )
                )
            } else {
                viewModel.setLocalAnimeStatus(dialog.anime.id, null)
            }
        },
        onRemoveLocalStatus = {
            viewModel.setLocalAnimeStatus(dialog.anime.id, null)
        },
        isLoggedIn = isLoggedIn,
        onNavigateToSettings = onNavigateToSettings,
        onNoExtension = onNavigateToSettings,
        onRelationClick = { relation ->
            if (isMangaFormat(relation.format)) {
                openMangaDetail(
                    MangaMedia(
                        id = relation.id,
                        title = relation.title,
                        cover = relation.cover,
                        totalChapters = 0,
                        averageScore = relation.averageScore,
                        format = relation.format
                    )
                )
                setMangaAutoShowChapters(false)
            } else {
                scope.launch {
                    try {
                        delay(100.milliseconds)
                        val newAnime = fetchAnimeForOverlay(viewModel, relation.id)
                        if (newAnime != null) {
                            viewModel.clearExploreAnimeCardBounds()
                            setOverlayState(
                                OverlayState.ExploreAnimeDialog(
                                    anime = newAnime,
                                    firstAnime = dialog.firstAnime ?: dialog.anime,
                                    isFirstOpen = false,
                                    previousStates = dialog.previousStates + dialog
                                )
                            )
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found_id, relation.id))
                        }
                    } catch (e: Exception) {
                        context.toast(context.getString(R.string.app_error, e.message))
                    }
                }
            }
        },
        onRecommendationClick = { rec ->
            if (isMangaFormat(rec.format)) {
                openMangaDetail(
                    MangaMedia(
                        id = rec.id,
                        title = rec.title,
                        cover = rec.cover,
                        totalChapters = rec.episodes,
                        averageScore = rec.averageScore,
                        format = rec.format
                    )
                )
                setMangaAutoShowChapters(false)
            } else {
                scope.launch {
                    try {
                        delay(100.milliseconds)
                        val newAnime = fetchAnimeForOverlay(viewModel, rec.id)
                        if (newAnime != null) {
                            viewModel.clearExploreAnimeCardBounds()
                            setOverlayState(
                                OverlayState.ExploreAnimeDialog(
                                    anime = newAnime,
                                    firstAnime = dialog.firstAnime ?: dialog.anime,
                                    isFirstOpen = false,
                                    previousStates = dialog.previousStates + dialog
                                )
                            )
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    } catch (e: Exception) {
                        context.toast(context.getString(R.string.app_error, e.message))
                    }
                }
            }
        },
        onCharacterClick = { characterId ->
            setOverlayState(
                OverlayState.CharacterDialog(
                    characterId = characterId,
                    animeId = dialog.anime.id,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onStaffClick = { staffId ->
            setOverlayState(
                OverlayState.StaffDialog(
                    staffId = staffId,
                    animeId = dialog.anime.id,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onViewAllCast = {
            setOverlayState(
                OverlayState.AllCastDialog(
                    animeId = dialog.anime.id,
                    animeTitle = dialog.anime.title,
                    animeTitleEnglish = dialog.anime.titleEnglish,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onViewAllStaff = {
            setOverlayState(
                OverlayState.AllStaffDialog(
                    animeId = dialog.anime.id,
                    animeTitle = dialog.anime.title,
                    animeTitleEnglish = dialog.anime.titleEnglish,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onViewAllRelations = { animeId, title, titleEnglish ->
            setOverlayState(
                OverlayState.AllRelationsDialog(
                    animeId = animeId,
                    animeTitle = title,
                    animeTitleEnglish = titleEnglish,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onViewAllRecommendations = { animeId, title, titleEnglish ->
            setOverlayState(
                OverlayState.AllRecommendationsDialog(
                    animeId = animeId,
                    animeTitle = title,
                    animeTitleEnglish = titleEnglish,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        preferEnglishTitles = preferEnglishTitles,
    )
}

/**
 * The anime detail overlay opened from a status-list card (bounds-continuous).
 */
@Composable
fun SelectedAnimeDetailOverlay(
    anime: AnimeMedia,
    viewModel: MainViewModel,
    isOled: Boolean,
    isLoggedIn: Boolean,
    preferEnglishTitles: Boolean,
    settingsReturnVersion: Int,
    animeStatusMap: Map<Int, String>,
    animeProgressMap: Map<Int, Int>,
    favoriteIds: Set<Int>,
    localAnimeStatus: Map<Int, LocalAnimeEntry>,
    initialCardBounds: MainViewModel.CardBounds?,
    setOverlayState: (OverlayState) -> Unit,
    onDismiss: () -> Unit,
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit,
    onReplaceAnime: (AnimeMedia) -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DetailedAnimeScreen(
        anime = anime.toDetailedAnimeData(),
        viewModel = viewModel,
        isOled = isOled,
        settingsReturnVersion = settingsReturnVersion,
        currentStatus = animeStatusMap[anime.id],
        currentProgress = animeProgressMap[anime.id],
        isFavorite = favoriteIds.contains(anime.id),
        initialCardBounds = initialCardBounds,
        onDismiss = onDismiss,
        onNavigateBack = onDismiss,
        onSwipeToClose = onDismiss,
        onPlayEpisode = { episode, _ ->
            onPlayEpisode(anime, episode, null)
            onDismiss()
        },
        onUpdateStatus = { status ->
            if (status != null) {
                viewModel.addExploreAnimeToList(
                    ExploreAnime(
                        id = anime.id,
                        title = anime.title,
                        titleEnglish = anime.titleEnglish,
                        cover = anime.cover,
                        banner = anime.banner,
                        episodes = anime.totalEpisodes,
                        latestEpisode = anime.latestEpisode,
                        averageScore = anime.averageScore,
                        genres = anime.genres,
                        year = anime.year,
                        format = anime.format
                    ),
                    status
                )
            }
        },
        onRemove = {
            viewModel.removeAnimeFromList(anime.id)
            onDismiss()
        },
        onRelationClick = { relation ->
            if (isMangaFormat(relation.format)) {
                openMangaDetail(
                    MangaMedia(
                        id = relation.id,
                        title = relation.title,
                        cover = relation.cover,
                        totalChapters = 0,
                        averageScore = relation.averageScore,
                        format = relation.format
                    )
                )
                setMangaAutoShowChapters(false)
            } else {
                scope.launch {
                    try {
                        delay(100.milliseconds)
                        val detailedData = viewModel.fetchDetailedAnimeData(relation.id)
                        if (detailedData != null) {
                            onReplaceAnime(detailedData.toAnimeMedia())
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    } catch (_: Exception) {
                        context.toast(context.getString(R.string.app_anime_not_found))
                    }
                }
            }
        },
        onRecommendationClick = { rec ->
            if (isMangaFormat(rec.format)) {
                openMangaDetail(
                    MangaMedia(
                        id = rec.id,
                        title = rec.title,
                        cover = rec.cover,
                        totalChapters = rec.episodes,
                        averageScore = rec.averageScore,
                        format = rec.format
                    )
                )
                setMangaAutoShowChapters(false)
            } else {
                scope.launch {
                    try {
                        delay(100.milliseconds)
                        val detailedData = viewModel.fetchDetailedAnimeData(rec.id)
                        if (detailedData != null) {
                            onReplaceAnime(detailedData.toAnimeMedia())
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    } catch (_: Exception) {
                        context.toast(context.getString(R.string.app_anime_not_found))
                    }
                }
            }
        },
        onCharacterClick = { characterId ->
            setOverlayState(OverlayState.CharacterDialog(characterId = characterId, animeId = anime.id))
        },
        onStaffClick = { staffId ->
            setOverlayState(OverlayState.StaffDialog(staffId = staffId, animeId = anime.id))
        },
        onViewAllCast = {
            setOverlayState(OverlayState.AllCastDialog(animeId = anime.id, animeTitle = anime.title, animeTitleEnglish = anime.titleEnglish))
        },
        onViewAllStaff = {
            setOverlayState(OverlayState.AllStaffDialog(animeId = anime.id, animeTitle = anime.title, animeTitleEnglish = anime.titleEnglish))
        },
        onViewAllRelations = { animeId, title, titleEnglish ->
            setOverlayState(OverlayState.AllRelationsDialog(animeId = animeId, animeTitle = title, animeTitleEnglish = titleEnglish))
        },
        onViewAllRecommendations = { animeId, title, titleEnglish ->
            setOverlayState(OverlayState.AllRecommendationsDialog(animeId = animeId, animeTitle = title, animeTitleEnglish = titleEnglish))
        },
        isLoggedIn = isLoggedIn,
        onUpdateLocalStatus = { status ->
            val currentEntry = localAnimeStatus[anime.id]
            if (status != null) {
                viewModel.setLocalAnimeStatus(
                    anime.id,
                    LocalAnimeEntry(
                        id = anime.id,
                        status = status,
                        progress = currentEntry?.progress ?: 0,
                        totalEpisodes = anime.totalEpisodes
                    )
                )
            } else {
                viewModel.setLocalAnimeStatus(anime.id, null)
            }
        },
        onRemoveLocalStatus = { viewModel.setLocalAnimeStatus(anime.id, null) },
        preferEnglishTitles = preferEnglishTitles,
        onNavigateToSettings = onNavigateToSettings,
        onNoExtension = onNavigateToSettings
    )
}
