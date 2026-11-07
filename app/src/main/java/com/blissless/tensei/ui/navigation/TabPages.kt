package com.blissless.tensei.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.ui.screens.explore.AnimeScreen
import com.blissless.tensei.ui.screens.explore.MangaScreen
import com.blissless.tensei.ui.screens.home.HomeScreen
import com.blissless.tensei.ui.screens.airing.ScheduleScreen
import com.blissless.tensei.viewmodel.MainViewModel

/**
 * The four bottom-navigation tab pages of the root screen:
 *
 *  - page 0 — airing [ScheduleScreen]
 *  - page 1 — [HomeScreen] (continue watching / reading, lists)
 *  - page 2 — [AnimeScreen] (explore)
 *  - page 3 — [MangaScreen] (explore)
 *
 * Extracted verbatim from the [com.blissless.tensei.MainScreen] composable;
 * each page receives its callbacks from the root screen.
 */
@Composable
fun TabPages(
    viewModel: MainViewModel,
    currentPage: Int,
    isOled: Boolean,
    isLoggedIn: Boolean,
    preferEnglishTitles: Boolean,
    showStatusColors: Boolean,
    showAnimeCardButtons: Boolean,
    showMangaCardButtons: Boolean,
    showMangaStatusColors: Boolean,
    settingsReturnVersion: Int,
    currentlyWatching: List<AnimeMedia>,
    planningToWatch: List<AnimeMedia>,
    completed: List<AnimeMedia>,
    onHold: List<AnimeMedia>,
    dropped: List<AnimeMedia>,
    malFavoriteIds: Set<Int>,
    aniListFavoriteIds: Set<Int>,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    startedAt: Map<String, Long>,
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit,
    onShowAnimeDialog: (com.blissless.tensei.data.models.ExploreAnime, com.blissless.tensei.data.models.ExploreAnime?) -> Unit,
    onShowDetailedAnimeFromMal: (Int) -> Unit,
    onShowDetailedAnimeFromAniList: (Int) -> Unit,
    onClearAnimeStack: () -> Unit,
    openCharacter: (Int) -> Unit,
    openStaff: (Int) -> Unit,
    openAllCast: (Int, String, String?) -> Unit,
    openAllStaff: (Int, String, String?) -> Unit,
    openAllRelations: (Int, String, String?) -> Unit,
    openAllRecommendations: (Int, String, String?) -> Unit,
    onSearchClick: () -> Unit,
    onNoExtension: () -> Unit,
    onNoMangaExtension: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOverlayOpenChange: (Boolean) -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    onMangaReadClick: (MangaMedia) -> Unit,
    onMangaClick: (MangaMedia) -> Unit,
    onMangaInfoClick: (MangaMedia) -> Unit,
    onMangaContinueReadingClick: (MangaMedia) -> Unit,
    onMangaDismissClick: (MangaMedia) -> Unit,
    onSetScheduleDialogOpen: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val favoriteIds = if (viewModel.loginProvider.collectAsState().value == LoginProvider.MAL) malFavoriteIds else aniListFavoriteIds

    when (currentPage) {
        0 -> ScheduleScreen(
            viewModel = viewModel,
            isOled = isOled,
            isVisible = true,
            preferEnglishTitles = preferEnglishTitles,
            isLoggedIn = isLoggedIn,
            onPlayEpisode = onPlayEpisode,
            onAnimeDialogOpen = { isOpen -> onSetScheduleDialogOpen(isOpen) },
            onCharacterClick = openCharacter,
            onStaffClick = openStaff,
            onViewAllCast = openAllCast,
            onViewAllStaff = openAllStaff,
            onViewAllRelations = openAllRelations,
            onViewAllRecommendations = openAllRecommendations,
            onNoExtension = onNoExtension,
            settingsReturnVersion = settingsReturnVersion,
            onAnimeDetailMangaClick = openMangaDetail,
            onSearchClick = onSearchClick
        )
        1 -> HomeScreen(
            viewModel = viewModel,
            isLoggedIn = isLoggedIn,
            isOled = isOled,
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            onOverlayOpenChange = onOverlayOpenChange,
            onNavigateToSettings = onNavigateToSettings,
            onNoExtension = onNoExtension,
            settingsReturnVersion = settingsReturnVersion,
            favoriteIds = favoriteIds,
            onPlayEpisode = onPlayEpisode,
            onLoginClick = onLoginClick,
            onShowAnimeDialog = onShowAnimeDialog,
            onShowDetailedAnimeFromMal = onShowDetailedAnimeFromMal,
            onShowDetailedAnimeFromAniList = onShowDetailedAnimeFromAniList,
            onCharacterClick = openCharacter,
            onStaffClick = openStaff,
            onViewAllCast = openAllCast,
            onViewAllStaff = openAllStaff,
            onViewAllRelations = openAllRelations,
            onViewAllRecommendations = openAllRecommendations,
            onNavigateToSearch = onSearchClick,
            onProfileClick = onProfileClick,
            playbackPositions = playbackPositions,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick,
            onMangaContinueReadingClick = onMangaContinueReadingClick,
            onMangaDismissClick = onMangaDismissClick,
            onAnimeDetailMangaClick = openMangaDetail,
            playbackDurations = playbackDurations,
            startedAt = startedAt
        )
        2 -> AnimeScreen(
            viewModel = viewModel,
            isLoggedIn = isLoggedIn,
            isOled = isOled,
            showStatusColors = showStatusColors,
            showAnimeCardButtons = showAnimeCardButtons,
            preferEnglishTitles = preferEnglishTitles,
            favoriteIds = favoriteIds,
            onPlayEpisode = onPlayEpisode,
            currentlyWatching = currentlyWatching,
            planningToWatch = planningToWatch,
            completed = completed,
            onHold = onHold,
            dropped = dropped,
            isVisible = true,
            onClearAnimeStack = onClearAnimeStack,
            onCharacterClick = openCharacter,
            onStaffClick = openStaff,
            onViewAllCast = openAllCast,
            onViewAllStaff = openAllStaff,
            onViewAllRelations = openAllRelations,
            onViewAllRecommendations = openAllRecommendations,
            onSearchClick = onSearchClick,
            onNoExtension = onNoExtension,
            settingsReturnVersion = settingsReturnVersion,
            onAnimeDetailMangaClick = openMangaDetail
        )
        3 -> MangaScreen(
            viewModel = viewModel,
            isOled = isOled,
            showMangaCardButtons = showMangaCardButtons,
            showMangaStatusColors = showMangaStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            isVisible = true,
            onSearchClick = onSearchClick,
            onMangaClick = { manga ->
                openMangaDetail(
                    MangaMedia(
                        id = manga.id,
                        title = manga.title.romaji ?: manga.title.english ?: context.getString(R.string.app_unknown),
                        titleEnglish = manga.title.english,
                        cover = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: "",
                        totalChapters = manga.chapters ?: 0,
                        averageScore = manga.averageScore,
                        malId = manga.idMal
                    )
                )
            },
            onMangaReadClick = { manga ->
                onMangaReadClick(
                    MangaMedia(
                        id = manga.id,
                        title = manga.title.romaji ?: manga.title.english ?: context.getString(R.string.app_unknown),
                        titleEnglish = manga.title.english,
                        cover = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: "",
                        totalChapters = manga.chapters ?: 0,
                        averageScore = manga.averageScore,
                        malId = manga.idMal
                    )
                )
            },
            onMangaNoExtension = onNoMangaExtension,
            settingsReturnVersion = settingsReturnVersion
        )
    }
}
