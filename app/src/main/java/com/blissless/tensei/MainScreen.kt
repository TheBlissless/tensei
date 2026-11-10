package com.blissless.tensei

// The root app screen: owns the navigation state (tab pages, overlay dialogs,
// the manga navigation stack and the playback pipeline), wires them together
// and delegates rendering to the host composables in ui.navigation and
// ui.screens.player.

import android.graphics.Rect
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.player.playback.PlaybackController
import com.blissless.tensei.player.playback.rememberPlaybackState
import com.blissless.tensei.ui.components.BottomNavigationBar
import com.blissless.tensei.ui.components.StreamErrorDialog
import com.blissless.tensei.ui.components.UpdateAvailableDialog
import com.blissless.tensei.ui.navigation.MangaNavHost
import com.blissless.tensei.ui.navigation.MangaNavState
import com.blissless.tensei.ui.navigation.OverlayState
import com.blissless.tensei.ui.navigation.SelectedAnimeDetailOverlay
import com.blissless.tensei.ui.navigation.ExploreAnimeDetailOverlay
import com.blissless.tensei.ui.navigation.CharacterOverlay
import com.blissless.tensei.ui.navigation.StaffOverlay
import com.blissless.tensei.ui.navigation.AllCastOverlay
import com.blissless.tensei.ui.navigation.AllStaffOverlay
import com.blissless.tensei.ui.navigation.AllRelationsOverlay
import com.blissless.tensei.ui.navigation.AllRecommendationsOverlay
import com.blissless.tensei.ui.navigation.TabPages
import com.blissless.tensei.ui.screens.player.PlayerHost
import com.blissless.tensei.ui.screens.search.SearchScreen
import com.blissless.tensei.ui.screens.settings.SettingsScreen
import com.blissless.tensei.ui.screens.status.StatusListScreen
import com.blissless.tensei.ui.screens.profile.UserProfileScreen
import com.blissless.tensei.update.UpdateViewModel
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.ExtensionsViewModel
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.clearMangaDetail
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.dismissMangaContinueReading
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.fetchDetailedAnimeDataByMalId
import com.blissless.tensei.viewmodel.loginWithAniList
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import kotlinx.coroutines.launch

/**
 * The root app screen.
 *
 * Owns the top-level navigation state and wires the feature hosts together:
 *
 *  - the four tab pages ([TabPages]),
 *  - the anime detail overlays ([ExploreAnimeDetailOverlay] /
 *    [SelectedAnimeDetailOverlay]),
 *  - the person/relation grids ([CharacterOverlay] … [AllRecommendationsOverlay]),
 *  - the manga navigation stack ([MangaNavHost] + [MangaNavState]),
 *  - the in-app player ([PlayerHost], driven by
 *    [com.blissless.tensei.player.playback.PlaybackController] and
 *    [com.blissless.tensei.player.playback.PlaybackState]),
 *  - the settings dialog, search/profile overlays and startup dialogs.
 *
 * Only state and wiring live here — rendering is delegated to the hosts.
 */
@RequiresApi(Build.VERSION_CODES.R)
@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isOled: Boolean,
    showStatusColors: Boolean,
    showAnimeCardButtons: Boolean,
    showMangaCardButtons: Boolean,
    showMangaStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    advancedSearch: Boolean,
    isLoggedIn: Boolean
) {
    val hideNavbar by viewModel.hideNavbar.collectAsState()
    val context = LocalContext.current
    val activity = context as MainActivity
    val scope = rememberCoroutineScope()
    val extViewModel: ExtensionsViewModel = viewModel()
    val extUiState by extViewModel.uiState.collectAsState()

    val startupScreen by viewModel.startupScreen.collectAsState()
    val currentPageState = remember { mutableIntStateOf(startupScreen) }
    var currentPage by currentPageState

    var overlayOpen by remember { mutableStateOf(false) }

    val currentlyWatching by viewModel.currentlyWatching.collectAsState()
    val planningToWatch by viewModel.planningToWatch.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val onHold by viewModel.onHold.collectAsState()
    val dropped by viewModel.dropped.collectAsState()

    val selectedMangaExtension by viewModel.selectedExtensionAuthority.collectAsState()
    val mangaSourceMethod by viewModel.mangaSourceMethod.collectAsState()
    val defaultExternalMangaExtension by viewModel.defaultExternalMangaExtension.collectAsState()
    val hasMangaSource = selectedMangaExtension != null ||
        (mangaSourceMethod == "external" && !defaultExternalMangaExtension.isNullOrBlank())

    val forwardSkipSeconds by viewModel.forwardSkipSeconds.collectAsState(initial = 10)
    val backwardSkipSeconds by viewModel.backwardSkipSeconds.collectAsState(initial = 10)

    val aniListFavorites by viewModel.aniListFavorites.collectAsState()
    val aniListFavoriteIds = remember(aniListFavorites) { aniListFavorites.map { it.id }.toSet() }
    val malFavorites by viewModel.malFavorites.collectAsState()
    val malFavoriteIds = remember(malFavorites) { malFavorites.map { it.id }.toSet() }
    val localAnimeStatus by viewModel.localAnimeStatus.collectAsState()
    val playbackPositions by viewModel.playbackPositions.collectAsState()
    val playbackDurations by viewModel.playbackDurations.collectAsState()
    val startedAt by viewModel.startedAt.collectAsState()

    val autoSkipOpening by viewModel.autoSkipOpening.collectAsState(initial = false)
    val autoSkipEnding by viewModel.autoSkipEnding.collectAsState(initial = false)
    val autoPlayNextEpisode by viewModel.autoPlayNextEpisode.collectAsState(initial = false)
    val supportsPiP by viewModel.supportsPiP.collectAsState(initial = false)

    val disableMaterialColors by viewModel.disableMaterialColors.collectAsState(initial = false)
    val showBufferIndicator by viewModel.showBufferIndicator.collectAsState(initial = true)
    val bufferAheadSeconds by viewModel.bufferAheadSeconds.collectAsState(initial = 30)
    val swipeVolume by viewModel.swipeVolume.collectAsState(initial = false)
    val swipeBrightness by viewModel.swipeBrightness.collectAsState(initial = false)
    val swipeSwap by viewModel.swipeSwap.collectAsState(initial = false)

    // ─── Screen-level navigation state ────────────────────────────────────
    var overlayState by remember { mutableStateOf<OverlayState>(OverlayState.None) }
    var scheduleDialogOpen by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var showUserProfilePage by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var pendingSettingsGroup by remember { mutableStateOf<String?>(null) }
    var settingsReturnVersion by remember { mutableIntStateOf(0) }

    /** (anime, episode) parked while the user configures a stream source in Settings. */
    var pendingAnimeAfterSettings by remember { mutableStateOf<Pair<AnimeMedia, Int>?>(null) }

    /** The anime opened from a status-list card (fullscreen detail overlay). */
    var selectedAnimeState by remember { mutableStateOf<AnimeMedia?>(null) }
    var showDetailedAnimeScreen by remember { mutableStateOf(false) }
    var currentCardBounds by remember { mutableStateOf<MainViewModel.CardBounds?>(null) }
    var detailedAnimeFromMal by remember { mutableStateOf<DetailedAnimeData?>(null) }

    var showStatusListScreen by remember { mutableStateOf(false) }
    var statusListTitle by remember { mutableStateOf("") }
    var statusListType by remember { mutableStateOf("") }
    var statusListIcon by remember { mutableStateOf<ImageVector?>(null) }
    var statusListAnime by remember { mutableStateOf<List<AnimeMedia>>(emptyList()) }

    // ─── Playback pipeline ────────────────────────────────────────────────
    // Snapshot state lives in PlaybackState (see player/playback/); all
    // orchestration lives in PlaybackController.
    val playbackState = rememberPlaybackState()
    val playback = remember {
        PlaybackController(
            state = playbackState,
            viewModel = viewModel,
            context = context,
            scope = scope,
        ) { anime, episode ->
            pendingAnimeAfterSettings = anime to episode
            playbackState.showNoExtDialog = true
        }
    }

    val isInPiPMode by activity.isInPiPMode
    LaunchedEffect(isInPiPMode) {
        if (!isInPiPMode && playbackState.showPlayer) {
            playbackState.playerFullscreen = activity.wasFullscreenBeforePiP
        }
    }
    LaunchedEffect(playbackState.showPlayer, supportsPiP, playbackState.playerFullscreen) {
        activity.shouldAutoEnterPiP = playbackState.showPlayer && supportsPiP
        activity.wasFullscreenBeforePiP = playbackState.playerFullscreen
        activity.updatePiPParams(playbackState.showPlayer && supportsPiP)
    }

    // ─── Manga navigation stack ───────────────────────────────────────────
    val mangaNav = remember { MangaNavState() }

    val openMangaDetail: (MangaMedia) -> Unit = { m ->
        viewModel.clearMangaDetail()
        mangaNav.openDetail(m)
    }
    val popMangaDetail: () -> Unit = {
        mangaNav.popDetail()
        viewModel.clearMangaDetail()
    }
    val closeAllManga: () -> Unit = {
        mangaNav.closeAll()
        viewModel.clearMangaDetail()
    }
    /** Opens the reader directly (from an explore/home card) without a detail screen behind. */
    val openMangaReader: (MangaMedia, Boolean, Int) -> Unit = { m, autoShowChapters, chapterIndex ->
        mangaNav.autoShowChapters = autoShowChapters
        mangaNav.detailStack = mangaNav.detailStack + m
        mangaNav.readerChapterIndex = chapterIndex
        mangaNav.showReader = true
    }

    // ─── Derived list state ───────────────────────────────────────────────
    val animeStatusMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped) {
        val map = mutableMapOf<Int, String>()
        currentlyWatching.forEach { map[it.id] = "CURRENT" }
        planningToWatch.forEach { map[it.id] = "PLANNING" }
        completed.forEach { map[it.id] = "COMPLETED" }
        onHold.forEach { map[it.id] = "PAUSED" }
        dropped.forEach { map[it.id] = "DROPPED" }
        map
    }

    val animeProgressMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped) {
        val map = mutableMapOf<Int, Int>()
        currentlyWatching.forEach { if (it.progress > 0) map[it.id] = it.progress }
        planningToWatch.forEach { if (it.progress > 0) map[it.id] = it.progress }
        completed.forEach { if (it.progress > 0) map[it.id] = it.progress }
        onHold.forEach { if (it.progress > 0) map[it.id] = it.progress }
        dropped.forEach { if (it.progress > 0) map[it.id] = it.progress }
        map
    }

    // ─── Shared overlay navigation callbacks ──────────────────────────────
    val onShowAnimeDialog: (ExploreAnime, ExploreAnime?) -> Unit = { anime, previousAnime ->
        val currentDialog = overlayState as? OverlayState.ExploreAnimeDialog
        val firstAnime = currentDialog?.firstAnime ?: previousAnime ?: anime
        val isFirstOpen = currentDialog == null
        val prevStates = if (currentDialog != null) currentDialog.previousStates + currentDialog else emptyList()
        viewModel.clearExploreAnimeCardBounds()
        viewModel.clearHomeAnimeCardBounds()
        overlayState = OverlayState.ExploreAnimeDialog(anime = anime, firstAnime = firstAnime, isFirstOpen = isFirstOpen, previousStates = prevStates)
    }

    val onClearAnimeStack: () -> Unit = {
        val prev = overlayState.previousStates
        overlayState = if (prev.isNotEmpty()) prev.last() else OverlayState.None
    }

    val onShowDetailedAnimeFromMal: (Int) -> Unit = { malId ->
        kotlinx.coroutines.MainScope().launch {
            val detailedData = viewModel.fetchDetailedAnimeDataByMalId(malId)
            detailedAnimeFromMal = detailedData
        }
    }

    // Opens the anime detail dialog AniList-first, falling back to MAL when the
    // AniList fetch fails or the id only maps to a MAL entry.
    val onShowDetailedAnime: (Int, Int) -> Unit = { animeId, malId ->
        scope.launch {
            val detailedData = viewModel.fetchDetailedAnimeData(animeId, malId = malId.takeIf { it > 0 })
            if (detailedData != null) {
                val newAnime = ExploreAnime(
                    id = detailedData.id,
                    title = detailedData.title,
                    titleEnglish = detailedData.titleEnglish,
                    cover = detailedData.cover,
                    banner = detailedData.banner,
                    episodes = detailedData.episodes,
                    latestEpisode = detailedData.latestEpisode,
                    averageScore = detailedData.averageScore,
                    genres = detailedData.genres,
                    year = detailedData.year,
                    format = detailedData.format,
                    malId = detailedData.malId
                )
                overlayState = OverlayState.ExploreAnimeDialog(anime = newAnime, firstAnime = newAnime, isFirstOpen = false)
            } else {
                context.toast(context.getString(R.string.app_anime_not_found))
            }
        }
    }

    val onPlayEpisode: (AnimeMedia, Int, String?) -> Unit = { anime, episode, title ->
        playback.onPlayEpisode(anime, episode, title)
    }

    // Standard "open person/relation dialog" handlers shared by the tab pages
    // and the search overlay.
    val openCharacter: (Int) -> Unit = { characterId ->
        overlayState = OverlayState.CharacterDialog(characterId = characterId, animeId = 0)
    }
    val openStaff: (Int) -> Unit = { staffId ->
        overlayState = OverlayState.StaffDialog(staffId = staffId, animeId = 0)
    }
    val openAllCast: (Int, String, String?) -> Unit = { animeId, animeTitle, animeTitleEnglish ->
        overlayState = OverlayState.AllCastDialog(animeId = animeId, animeTitle = animeTitle, animeTitleEnglish = animeTitleEnglish)
    }
    val openAllStaff: (Int, String, String?) -> Unit = { animeId, animeTitle, animeTitleEnglish ->
        overlayState = OverlayState.AllStaffDialog(animeId = animeId, animeTitle = animeTitle, animeTitleEnglish = animeTitleEnglish)
    }
    val openAllRelations: (Int, String, String?) -> Unit = { animeId, animeTitle, animeTitleEnglish ->
        overlayState = OverlayState.AllRelationsDialog(animeId = animeId, animeTitle = animeTitle, animeTitleEnglish = animeTitleEnglish)
    }
    val openAllRecommendations: (Int, String, String?) -> Unit = { animeId, animeTitle, animeTitleEnglish ->
        overlayState = OverlayState.AllRecommendationsDialog(animeId = animeId, animeTitle = animeTitle, animeTitleEnglish = animeTitleEnglish)
    }

    val onNavigateToSettings: () -> Unit = {
        showSettings = true
    }
    val onNoExtension: () -> Unit = {
        showSettings = true
        pendingSettingsGroup = if (extUiState.extensions.isEmpty()) "extensions" else "stream"
    }
    val onNoMangaExtension: () -> Unit = {
        showSettings = true
        pendingSettingsGroup = "reader"
    }
    val onOpenSettingsForGroup: (String?) -> Unit = { group ->
        showSettings = true
        pendingSettingsGroup = group
    }

    // ─── Anime detail overlays ────────────────────────────────────────────
    val exploreDialog = overlayState as? OverlayState.ExploreAnimeDialog
    if (exploreDialog != null) {
        ExploreAnimeDetailOverlay(
            dialog = exploreDialog,
            viewModel = viewModel,
            isOled = isOled,
            isLoggedIn = isLoggedIn,
            preferEnglishTitles = preferEnglishTitles,
            settingsReturnVersion = settingsReturnVersion,
            animeStatusMap = animeStatusMap,
            animeProgressMap = animeProgressMap,
            favoriteIds = aniListFavoriteIds,
            localAnimeStatus = localAnimeStatus,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
            onPlayEpisode = onPlayEpisode,
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
            onNavigateToSettings = {
                overlayState = OverlayState.None
                onNoExtension()
            },
        )
    }

    // Anime detail opened from a status-list card (bounds-continuous).
    if (selectedAnimeState != null && showDetailedAnimeScreen) {
        val selectedAnime = selectedAnimeState!!
        SelectedAnimeDetailOverlay(
            anime = selectedAnime,
            viewModel = viewModel,
            isOled = isOled,
            isLoggedIn = isLoggedIn,
            preferEnglishTitles = preferEnglishTitles,
            settingsReturnVersion = settingsReturnVersion,
            animeStatusMap = animeStatusMap,
            animeProgressMap = animeProgressMap,
            favoriteIds = aniListFavoriteIds,
            localAnimeStatus = localAnimeStatus,
            initialCardBounds = currentCardBounds,
            setOverlayState = { overlayState = it },
            onDismiss = {
                // Close the detail and restore a suspended manga grid underneath.
                showDetailedAnimeScreen = false
                mangaNav.restoreOverlayIfPending()
                if (mangaNav.detailStack.isNotEmpty()) {
                    mangaNav.showDetailScreen = true
                }
            },
            onPlayEpisode = onPlayEpisode,
            onReplaceAnime = { replacement ->
                currentCardBounds = null
                selectedAnimeState = replacement
                showDetailedAnimeScreen = true
            },
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
            onNavigateToSettings = {
                overlayState = OverlayState.None
                onNoExtension()
            },
        )
    }

    // ─── Manga navigation (details, reader, All * grids) ──────────────────
    MangaNavHost(
        state = mangaNav,
        viewModel = viewModel,
        isOled = isOled,
        preferEnglishTitles = preferEnglishTitles,
        isLoggedIn = isLoggedIn,
        settingsReturnVersion = settingsReturnVersion,
        hasMangaSource = hasMangaSource,
        setOverlayState = { overlayState = it },
        openMangaDetail = openMangaDetail,
        onOpenAnimeDetailFromManga = { anime ->
            selectedAnimeState = anime
            mangaNav.showDetailScreen = false
            showDetailedAnimeScreen = true
        },
        onOpenSettings = onOpenSettingsForGroup,
    )

    // Resume a parked anime playback intent after returning from Settings
    // (runs after the manga resume effect inside MangaNavHost, matching the
    // original single-effect ordering).
    LaunchedEffect(settingsReturnVersion) {
        val pendingAnime = pendingAnimeAfterSettings
        if (settingsReturnVersion > 0 && pendingAnime != null) {
            pendingAnimeAfterSettings = null
            val (pendingPlayAnime, pendingPlayEpisode) = pendingAnime
            if (playback.hasUsableAnimeExtensionDefault()) {
                playback.loadAndPlayEpisode(pendingPlayAnime, pendingPlayEpisode)
            } else {
                playbackState.showNoExtDialog = true
            }
        }
    }

    // ─── Person / relation overlays ───────────────────────────────────────
    val characterDialog = overlayState as? OverlayState.CharacterDialog
    if (characterDialog != null) {
        CharacterOverlay(
            dialog = characterDialog,
            viewModel = viewModel,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
        )
    }

    val staffDialog = overlayState as? OverlayState.StaffDialog
    if (staffDialog != null) {
        StaffOverlay(
            dialog = staffDialog,
            viewModel = viewModel,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
        )
    }

    val allCastDialog = overlayState as? OverlayState.AllCastDialog
    if (allCastDialog != null) {
        AllCastOverlay(
            dialog = allCastDialog,
            viewModel = viewModel,
            preferEnglishTitles = preferEnglishTitles,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
        )
    }

    val allStaffDialog = overlayState as? OverlayState.AllStaffDialog
    if (allStaffDialog != null) {
        AllStaffOverlay(
            dialog = allStaffDialog,
            viewModel = viewModel,
            preferEnglishTitles = preferEnglishTitles,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
        )
    }

    val allRelationsDialog = overlayState as? OverlayState.AllRelationsDialog
    if (allRelationsDialog != null) {
        AllRelationsOverlay(
            dialog = allRelationsDialog,
            viewModel = viewModel,
            preferEnglishTitles = preferEnglishTitles,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
        )
    }

    val allRecommendationsDialog = overlayState as? OverlayState.AllRecommendationsDialog
    if (allRecommendationsDialog != null) {
        AllRecommendationsOverlay(
            dialog = allRecommendationsDialog,
            viewModel = viewModel,
            preferEnglishTitles = preferEnglishTitles,
            setOverlayState = { overlayState = it },
            onNavigateBack = onClearAnimeStack,
            openMangaDetail = openMangaDetail,
            setMangaAutoShowChapters = { mangaNav.autoShowChapters = it },
        )
    }

    // ─── No-anime-extension redirect dialog ───────────────────────────────
    if (playbackState.showNoExtDialog) {
        AlertDialog(
            onDismissRequest = { playbackState.showNoExtDialog = false; pendingAnimeAfterSettings = null },
            title = { Text(stringResource(R.string.app_no_extension_title)) },
            text = {
                Text(stringResource(R.string.app_no_extension_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    playbackState.showNoExtDialog = false
                    overlayState = OverlayState.None
                    onNoExtension()
                }) {
                    Text(stringResource(R.string.app_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { playbackState.showNoExtDialog = false; pendingAnimeAfterSettings = null }) {
                    Text(stringResource(R.string.app_close))
                }
            }
        )
    }

    // ─── Player region (or, when hidden, the main scaffold) ───────────────
    if (playbackState.showPlayer && playbackState.currentVideoUrl != null) {
        PlayerHost(
            state = playbackState,
            controller = playback,
            viewModel = viewModel,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            supportsPiP = supportsPiP,
            isInPiPMode = isInPiPMode,
            autoSkipOpening = autoSkipOpening,
            autoSkipEnding = autoSkipEnding,
            autoPlayNextEpisode = autoPlayNextEpisode,
            forwardSkipSeconds = forwardSkipSeconds,
            backwardSkipSeconds = backwardSkipSeconds,
            swipeVolume = swipeVolume,
            swipeBrightness = swipeBrightness,
            swipeSwap = swipeSwap,
            disableMaterialColors = disableMaterialColors,
            showBufferIndicator = showBufferIndicator,
            bufferAheadSeconds = bufferAheadSeconds,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            animeProgressMap = animeProgressMap,
            overlayState = overlayState,
            onPlayerBoundsChanged = { l, t, r, b -> activity.playerViewBounds = Rect(l, t, r, b) },
            scheduleDialogOpen = scheduleDialogOpen,
            onSetScheduleDialogOpen = { scheduleDialogOpen = it },
            onClearAnimeStack = onClearAnimeStack,
        )
    } else {
        if (showSettings) {
            val settingsInitialGroup = pendingSettingsGroup
            Dialog(
                onDismissRequest = { showSettings = false; pendingSettingsGroup = null; settingsReturnVersion++ },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        autoSkipOpening = autoSkipOpening,
                        autoSkipEnding = autoSkipEnding,
                        autoPlayNextEpisode = autoPlayNextEpisode,
                        preferredCategory = viewModel.preferredCategory.collectAsState().value,
                        initialGroup = settingsInitialGroup,
                        onBack = { showSettings = false; pendingSettingsGroup = null; settingsReturnVersion++ }
                    )
                }
            }
        }

        Scaffold(
            containerColor = if (isOled) Color.Black else MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TabPages(
                        viewModel = viewModel,
                        currentPage = currentPage,
                        isOled = isOled,
                        isLoggedIn = isLoggedIn,
                        preferEnglishTitles = preferEnglishTitles,
                        showStatusColors = showStatusColors,
                        showAnimeCardButtons = showAnimeCardButtons,
                        showMangaCardButtons = showMangaCardButtons,
                        showMangaStatusColors = showMangaStatusColors,
                        settingsReturnVersion = settingsReturnVersion,
                        currentlyWatching = currentlyWatching,
                        planningToWatch = planningToWatch,
                        completed = completed,
                        onHold = onHold,
                        dropped = dropped,
                        malFavoriteIds = malFavoriteIds,
                        aniListFavoriteIds = aniListFavoriteIds,
                        playbackPositions = playbackPositions,
                        playbackDurations = playbackDurations,
                        startedAt = startedAt,
                        onPlayEpisode = onPlayEpisode,
                        onShowAnimeDialog = onShowAnimeDialog,
                        onShowDetailedAnimeFromMal = onShowDetailedAnimeFromMal,
                        onShowDetailedAnimeFromAniList = { aniListId ->
                            scope.launch {
                                val detailedData = viewModel.fetchDetailedAnimeData(aniListId)
                                if (detailedData != null) {
                                    val newAnime = ExploreAnime(
                                        id = detailedData.id,
                                        title = detailedData.title,
                                        titleEnglish = detailedData.titleEnglish,
                                        cover = detailedData.cover,
                                        banner = detailedData.banner,
                                        episodes = detailedData.episodes,
                                        latestEpisode = detailedData.latestEpisode,
                                        averageScore = detailedData.averageScore,
                                        genres = detailedData.genres,
                                        year = detailedData.year,
                                        format = detailedData.format
                                    )
                                    overlayState = OverlayState.ExploreAnimeDialog(anime = newAnime, firstAnime = newAnime, isFirstOpen = false)
                                } else {
                                    context.toast(context.getString(R.string.app_anime_not_found))
                                }
                            }
                        },
                        onClearAnimeStack = onClearAnimeStack,
                        openCharacter = openCharacter,
                        openStaff = openStaff,
                        openAllCast = openAllCast,
                        openAllStaff = openAllStaff,
                        openAllRelations = openAllRelations,
                        openAllRecommendations = openAllRecommendations,
                        onSearchClick = { showSearchScreen = true },
                        onNoExtension = onNoExtension,
                        onNoMangaExtension = onNoMangaExtension,
                        onNavigateToSettings = onNavigateToSettings,
                        onLoginClick = { viewModel.loginWithAniList() },
                        onProfileClick = { showUserProfilePage = true },
                        onOverlayOpenChange = { overlayOpen = it },
                        openMangaDetail = openMangaDetail,
                        onMangaReadClick = { m -> openMangaReader(m, true, -1) },
                        onMangaClick = { manga ->
                            if (!hasMangaSource) {
                                mangaNav.pendingAfterSettings = manga
                                mangaNav.pendingResumeAfterSettings = false
                                mangaNav.showNoExtensionDialog = true
                            } else {
                                openMangaReader(manga, true, -1)
                            }
                        },
                        onMangaInfoClick = { manga ->
                            mangaNav.autoShowChapters = false
                            openMangaDetail(manga)
                        },
                        onMangaContinueReadingClick = { manga ->
                            if (!hasMangaSource) {
                                mangaNav.pendingAfterSettings = manga
                                mangaNav.pendingResumeAfterSettings = true
                                mangaNav.showNoExtensionDialog = true
                            } else {
                                openMangaReader(manga, false, manga.progress.coerceAtLeast(0))
                            }
                        },
                        onMangaDismissClick = { manga ->
                            viewModel.dismissMangaContinueReading(manga.id)
                        },
                        onSetScheduleDialogOpen = { scheduleDialogOpen = it },
                    )
                }

                AnimatedVisibility(
                    visible = showSearchScreen,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(250))
                ) {
                    SearchScreen(
                        viewModel = viewModel,
                        isOled = isOled,
                        isLoggedIn = isLoggedIn,
                        preferEnglishTitles = preferEnglishTitles,
                        advancedSearch = advancedSearch,
                        showAnimeStatusColors = showStatusColors,
                        showMangaStatusColors = showMangaStatusColors,
                        currentlyWatching = currentlyWatching,
                        planningToWatch = planningToWatch,
                        completed = completed,
                        onHold = onHold,
                        dropped = dropped,
                        localAnimeStatus = localAnimeStatus,
                        favoriteIds = if (viewModel.loginProvider.collectAsState().value == LoginProvider.MAL) malFavoriteIds else aniListFavoriteIds,
                        onClose = { showSearchScreen = false },
                        onPlayEpisode = onPlayEpisode,
                        onCharacterClick = openCharacter,
                        onStaffClick = openStaff,
                        onViewAllCast = openAllCast,
                        onViewAllStaff = openAllStaff,
                        onViewAllRelations = openAllRelations,
                        onViewAllRecommendations = openAllRecommendations,
                        onNoExtension = onNoExtension,
                        settingsReturnVersion = settingsReturnVersion,
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
                        onAnimeDetailMangaClick = openMangaDetail
                    )
                }

                if (playbackState.isLoadingStream) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .pointerInput(Unit) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.app_loading_stream), color = Color.White)
                            val serverAttempt = playbackState.currentServerAttempt
                            if (serverAttempt != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (playbackState.currentServerAttemptIsFallback) stringResource(R.string.app_trying_server_fallback, serverAttempt) else stringResource(R.string.app_trying_server, serverAttempt),
                                    color = Color.Yellow,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = {
                                    playbackState.loadingJob?.cancel()
                                    playbackState.isLoadingStream = false
                                    playbackState.loadingJob = null
                                }
                            ) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    }
                }

                if (showStatusListScreen) {
                    StatusListScreen(
                        title = statusListTitle,
                        icon = statusListIcon ?: Icons.Default.PlayArrow,
                        animeList = statusListAnime,
                        listType = statusListType,
                        showStatusColors = showStatusColors,
                        preferEnglishTitles = preferEnglishTitles,
                        onAnimeClick = { anime, bounds ->
                            viewModel.setHomeAnimeCardBounds(anime.id, anime.cover, bounds?.bounds)
                            selectedAnimeState = anime
                            showDetailedAnimeScreen = true
                        },
                        onPlayClick = { anime ->
                            val nextEp = anime.progress + 1
                            val released = anime.latestEpisode ?: anime.totalEpisodes
                            if (anime.latestEpisode != null && nextEp > released) {
                                context.toast(context.getString(R.string.app_episode_not_aired))
                            } else {
                                onPlayEpisode(anime, nextEp, null)
                            }
                        },
                        onInfoClick = { anime, bounds ->
                            viewModel.setHomeAnimeCardBounds(anime.id, anime.cover, bounds?.bounds)
                            selectedAnimeState = anime
                            showDetailedAnimeScreen = true
                        },
                        onStatusClick = { anime ->
                            viewModel.setExploreAnimeCardBounds(anime.id, anime.cover, null)
                            selectedAnimeState = anime
                            showDetailedAnimeScreen = true
                        },
                        onBackClick = { showStatusListScreen = false },
                        onDismiss = {
                            viewModel.setHideNavbar(false)
                            showStatusListScreen = false
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showUserProfilePage,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(250))
                ) {
                    UserProfileScreen(
                        viewModel = viewModel,
                        preferEnglishTitles = preferEnglishTitles,
                        onBack = { showUserProfilePage = false },
                        onShowDetailedAnime = onShowDetailedAnime,
                        onMangaClick = { manga ->
                            openMangaDetail(manga)
                        },
                    )
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    BottomNavigationBar(
                        selectedIndex = currentPage,
                        isOled = isOled,
                        disableMaterialColors = disableMaterialColors,
                        hideNavbar = hideNavbar,
                        isLoadingStream = playbackState.isLoadingStream,
                        showSearchScreen = showSearchScreen || showUserProfilePage,
                        onSelect = {
                            currentPage = it
                        },
                        scope = scope,
                    )
                }

                playbackState.streamError?.let { error ->
                    StreamErrorDialog(
                        error = error,
                        onDismiss = { playbackState.streamError = null },
                    )
                }

                // Update check on startup
                val updateViewModel: UpdateViewModel = viewModel()
                val pendingUpdateState by viewModel.pendingUpdateRelease.collectAsState()
                val updateState by updateViewModel.uiState.collectAsState()
                val updaterAppIcon by viewModel.appIcon.collectAsState()
                var showUpdateDialog by remember { mutableStateOf(false) }

                // Collect toast messages from UpdateViewModel
                LaunchedEffect(Unit) {
                    updateViewModel.toastMessage.collect { message ->
                        context.toast(message)
                    }
                }

                LaunchedEffect(pendingUpdateState) {
                    if (pendingUpdateState != null) {
                        showUpdateDialog = true
                    }
                }

                // Close the update dialog once the package installer opens;
                // on download failure it stays open so the error can be retried.
                LaunchedEffect(updateState.downloadedFile) {
                    if (updateState.downloadedFile != null) {
                        showUpdateDialog = false
                    }
                }

                val pendingUpdate = pendingUpdateState
                if (showUpdateDialog && pendingUpdate != null) {
                    UpdateAvailableDialog(
                        release = pendingUpdate,
                        isDownloading = updateState.isDownloading,
                        downloadProgress = updateState.downloadProgress,
                        downloadError = updateState.error,
                        appIcon = updaterAppIcon,
                        onDismiss = { showUpdateDialog = false },
                        onUpdate = { updateViewModel.setReleaseAndDownload(pendingUpdate) },
                    )
                }
            }
        }
    }
}
