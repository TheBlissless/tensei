package com.blissless.tensei.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ContinueWatchingEntry
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.dialogs.HomeAnimeStatusDialog
import com.blissless.tensei.ui.dialogs.OfflineFavoritesDialog
import com.blissless.tensei.ui.screens.manga.MangaStatusDialog
import com.blissless.tensei.ui.screens.episode.RichEpisodeScreen
import com.blissless.tensei.ui.components.HomeAnimeCardBounds
import com.blissless.tensei.ui.components.HomeAnimeHorizontalList
import com.blissless.tensei.ui.components.HomeStatusColors
import com.blissless.tensei.ui.components.LoadingSkeleton
import com.blissless.tensei.ui.components.SectionHeader
import com.blissless.tensei.ui.components.TenseiCircleAction
import com.blissless.tensei.ui.components.TenseiErrorBanner
import com.blissless.tensei.ui.components.TenseiScrimChip
import com.blissless.tensei.ui.theme.PillShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.theme.artworkGradient
import com.blissless.tensei.ui.components.ContinueWatchingEpisodeRow
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.components.AppIconCircle
import com.blissless.tensei.ui.components.ExploreTopBarRow
import com.blissless.tensei.ui.components.RailCardTitle
import com.blissless.tensei.ui.components.SearchCircleAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.ui.screens.status.StatusListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaContinueReading
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaDropped
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.removeContinueWatchingEntry
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.updateMangaStatus
import com.blissless.tensei.util.toast
import com.blissless.tensei.util.longToast
import com.blissless.tensei.viewmodel.tryManualRefresh
import com.blissless.tensei.viewmodel.refreshHome
import com.blissless.tensei.viewmodel.removeAnimeFromList
import com.blissless.tensei.viewmodel.updateAnimeStatus
import com.blissless.tensei.viewmodel.addExploreAnimeToList
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.toggleLocalFavorite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isLoggedIn: Boolean,
    isOled: Boolean = false,
    showStatusColors: Boolean = true,
    preferEnglishTitles: Boolean = true,
    favoriteIds: Set<Int> = emptySet(),
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit = { _, _, _ -> },
    onLoginClick: () -> Unit = {},
    onShowAnimeDialog: (ExploreAnime, ExploreAnime?) -> Unit = { _, _ -> },
    onShowDetailedAnimeFromMal: (Int) -> Unit = {},
    onShowDetailedAnimeFromAniList: (Int) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onViewAllCast: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllStaff: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRelations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRecommendations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onOverlayOpenChange: (Boolean) -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null,
    onNoExtension: () -> Unit = {},
    settingsReturnVersion: Int = 0,
    onNavigateToSearch: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMangaClick: (MangaMedia) -> Unit = {},
    onMangaInfoClick: (MangaMedia) -> Unit = {},
    onMangaContinueReadingClick: (MangaMedia) -> Unit = {},
    onMangaDismissClick: (MangaMedia) -> Unit = {},
    onAnimeDetailMangaClick: (MangaMedia) -> Unit = {},
    currentScreenIndex: Int = 0,
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    startedAt: Map<String, Long> = emptyMap()
) {
    val currentlyWatching by viewModel.currentlyWatching.collectAsState()
    val planningToWatch by viewModel.planningToWatch.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val onHold by viewModel.onHold.collectAsState()
    val dropped by viewModel.dropped.collectAsState()
    val isLoading by viewModel.isLoadingHome.collectAsState()

    val offlineCurrentlyWatching by viewModel.offlineCurrentlyWatching.collectAsState()
    val offlinePlanningToWatch by viewModel.offlinePlanningToWatch.collectAsState()
    val offlineCompleted by viewModel.offlineCompleted.collectAsState()
    val offlineOnHold by viewModel.offlineOnHold.collectAsState()
    val offlineDropped by viewModel.offlineDropped.collectAsState()

    val localFavorites by viewModel.localFavorites.collectAsState()
    val localAnimeStatus by viewModel.localAnimeStatus.collectAsState()

    val userName by viewModel.userName.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()

    val appIcon by viewModel.appIcon.collectAsState()

    // â”€â”€â”€ Manga state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val mangaContinueReading by viewModel.mangaContinueReading.collectAsState()
    val mangaCurrentlyReading by viewModel.mangaCurrentlyReading.collectAsState()
    val mangaPlanningToRead by viewModel.mangaPlanningToRead.collectAsState()
    val mangaCompleted by viewModel.mangaCompleted.collectAsState()
    val mangaPaused by viewModel.mangaPaused.collectAsState()
    val mangaDropped by viewModel.mangaDropped.collectAsState()

    val context = LocalContext.current

    var selectedAnime by remember { mutableStateOf<AnimeMedia?>(null) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var reopenEpisodePickerAfterSettings by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showMangaStatusDialog by remember { mutableStateOf(false) }
    var statusListMangaForDialog by remember { mutableStateOf<MangaMedia?>(null) }
    var showOfflineFavoritesDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showDetailedAnimeScreen by remember { mutableStateOf(false) }
    var showNoExtensionDialog by remember { mutableStateOf(false) }
    val defaultPkg by viewModel.defaultExtensionPackage.collectAsState()
    val defaultMagnetExt by viewModel.defaultMagnetExtension.collectAsState()
    val defaultStreamExt by viewModel.defaultStreamExtension.collectAsState()
    val streamMethod by viewModel.streamMethod.collectAsState()
    val magnetExtensions by viewModel.availableMagnetExtensions.collectAsState()

    // Status list screen state
    var showStatusListScreen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableMagnetExtensions()
    }

    LaunchedEffect(showStatusListScreen) {
        onOverlayOpenChange(showStatusListScreen)
    }
    var statusListTitle by remember { mutableStateOf("") }
    var statusListIcon by remember { mutableStateOf(Icons.Default.PlayArrow) }
    var statusListType by remember { mutableStateOf("") }
    var statusListIsManga by remember { mutableStateOf(false) }

    // Track first anime for back navigation
    var firstAnime by remember { mutableStateOf<AnimeMedia?>(null) }

    // Card bounds for shared element transition
    var currentCardBounds by remember { mutableStateOf<MainViewModel.CardBounds?>(null) }

    // Home status rails all follow the provider's "Last Updated" order. Freshly
    // edited entries carry a newer timestamp, so they move to the front immediately
    // instead of being appended to the end of their list.
    fun List<AnimeMedia>.byLastUpdated(): List<AnimeMedia> = sortedByDescending { it.updatedAt }
    fun List<MangaMedia>.byLastUpdatedManga(): List<MangaMedia> = sortedByDescending { it.updatedAt }

    val effectiveCurrentlyWatching = remember(isLoggedIn, currentlyWatching, offlineCurrentlyWatching) {
        (if (isLoggedIn) currentlyWatching else offlineCurrentlyWatching).byLastUpdated()
    }
    val effectivePlanningToWatch = remember(isLoggedIn, planningToWatch, offlinePlanningToWatch) {
        (if (isLoggedIn) planningToWatch else offlinePlanningToWatch).byLastUpdated()
    }
    val effectiveCompleted = remember(isLoggedIn, completed, offlineCompleted) {
        (if (isLoggedIn) completed else offlineCompleted).byLastUpdated()
    }
    val effectiveOnHold = remember(isLoggedIn, onHold, offlineOnHold) {
        (if (isLoggedIn) onHold else offlineOnHold).byLastUpdated()
    }
    val effectiveDropped = remember(isLoggedIn, dropped, offlineDropped) {
        (if (isLoggedIn) dropped else offlineDropped).byLastUpdated()
    }

    // Manga lists are mirrored from local tracking, which carries the same
    // provider last-updated timestamps (stamped on every local edit).
    val effectiveMangaCurrentlyReading = remember(mangaCurrentlyReading) { mangaCurrentlyReading.byLastUpdatedManga() }
    val effectiveMangaPlanningToRead = remember(mangaPlanningToRead) { mangaPlanningToRead.byLastUpdatedManga() }
    val effectiveMangaCompleted = remember(mangaCompleted) { mangaCompleted.byLastUpdatedManga() }
    val effectiveMangaPaused = remember(mangaPaused) { mangaPaused.byLastUpdatedManga() }
    val effectiveMangaDropped = remember(mangaDropped) { mangaDropped.byLastUpdatedManga() }

    val allListsEmpty = effectiveCurrentlyWatching.isEmpty() && effectivePlanningToWatch.isEmpty() && effectiveCompleted.isEmpty() && effectiveOnHold.isEmpty() && effectiveDropped.isEmpty()

    val allAnime = remember(effectiveCurrentlyWatching, effectivePlanningToWatch, effectiveCompleted, effectiveOnHold, effectiveDropped) {
        effectiveCurrentlyWatching + effectivePlanningToWatch + effectiveCompleted + effectiveOnHold + effectiveDropped
    }

    val animeById = remember(allAnime) {
        allAnime.associateBy { it.id }
    }

    val continueWatchingEpisodes = remember(allAnime, playbackPositions, startedAt) {
        val seen = mutableSetOf<String>()
        // Don't show a Continue Watching card unless the user has actually
        // started watching — playback position must be at least 10 seconds
        // in. This mirrors CacheManager.PLAYBACK_FINISHED_TAIL_MS (the tail
        // that marks an episode as finished) and prevents cards created by
        // accidental taps or instant exits from cluttering the home screen.
        val CONTINUE_WATCHING_MIN_POSITION_MS = 10_000L
        playbackPositions.filter { (key, pos) ->
            pos >= CONTINUE_WATCHING_MIN_POSITION_MS && !key.endsWith("_offline")
        }.mapNotNull { (key, pos) ->
            val parts = key.split("_")
            if (parts.size >= 2) {
                val animeId = parts[0].toIntOrNull()
                val episode = parts[1].toIntOrNull()
                if (animeId != null && episode != null && seen.add(key)) {
                    val anime = animeById[animeId]
                    if (anime != null) {
                        ContinueWatchingEntry(
                            animeId = animeId,
                            episode = episode,
                            animeTitle = anime.title,
                            animeTitleEnglish = anime.titleEnglish,
                            animeCover = anime.cover,
                            animeBanner = anime.banner,
                            position = pos,
                            duration = playbackDurations[key] ?: 0L,
                            startedAt = startedAt[key] ?: 0L
                        )
                    } else null
                } else null
            } else null
        }.sortedByDescending { it.startedAt }
    }

    val activeMangaContinueReading = mangaContinueReading.filter { manga ->
        val pageTotal = manga.currentChapterPages
        val scrollProgress = manga.scrollProgress.coerceIn(0f, 1f)
        // Compute the best-overlap page (the one the reader was displaying when the user
        // exited). scrollProgress is stored as currentScroll / maxScroll where currentScroll
        // is the TOP of the viewport; multiplying by (total - 1) and rounding to nearest
        // recovers the best-overlap page index (see ReaderScreen vertical restore comment
        // for the full derivation). Only show the card once the user has reached at least
        // page 2; the upper bound hides it once they've scrolled to the last page.
        val currentPage = ((scrollProgress * (pageTotal - 1)) + 0.5f).toInt() + 1
        pageTotal > 1 && currentPage in 2 until pageTotal
    }

    val hasOfflineContent = !isLoggedIn && (localFavorites.isNotEmpty() || localAnimeStatus.isNotEmpty())
    val showWelcomeCard = !isLoggedIn && allListsEmpty && !hasOfflineContent

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var previousScreenIndex by remember { mutableIntStateOf(currentScreenIndex) }

    // Force recomposition when lists change by tracking a version counter
    var listVersion by remember { mutableIntStateOf(0) }

    // Update listVersion when lists change to trigger recomposition
    LaunchedEffect(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) {
        listVersion++
    }

    val homeScrollState = rememberScrollState()

    val disableMaterialColors by viewModel.disableMaterialColors.collectAsState(initial = false)

    val tmdbEpisodeCache by viewModel.tmdbEpisodeCache.collectAsState()

    val apiError by viewModel.apiError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()

    LaunchedEffect(currentScreenIndex) {
        if (currentScreenIndex != previousScreenIndex) {
            previousScreenIndex = currentScreenIndex
        }
    }

    LaunchedEffect(showStatusListScreen) {
        viewModel.setHideNavbar(showStatusListScreen)
    }

    BackHandler(enabled = showStatusListScreen) { showStatusListScreen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (apiError != null || isOffline) {
                TenseiErrorBanner(
                    message = if (isOffline) stringResource(R.string.home_offline) else stringResource(R.string.home_anilist_unavailable),
                    icon = if (isOffline) Icons.Default.SignalWifiOff else Icons.Default.CloudOff,
                    isOffline = isOffline,
                    isOled = isOled,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ExploreTopBarRow(
                leading = { AppIconCircle(appIcon) },
                center = {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(PillShape)
                            .clickable { showProfileSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (isLoggedIn && userAvatar != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(userAvatar).build(),
                                contentDescription = stringResource(R.string.profile_sheet_avatar_content_description),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = stringResource(R.string.profile_sheet_user_content_description),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = if (isLoggedIn) (userName ?: stringResource(R.string.profile_sheet_default_username)) else "Tensei",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.profile_tap_to_view_profile),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                trailing = { SearchCircleAction(onClick = onNavigateToSearch) }
            )

            // Matches the Explore tabs: 16dp spacer + TenseiSectionHeader's own 8dp top
            // padding = 24dp above the first section header, same as carousel -> "This Season".
            Spacer(Modifier.height(16.dp))

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { if (viewModel.tryManualRefresh("home")) { isRefreshing = true; viewModel.refreshHome() } },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(homeScrollState)
                ) {
                    if (showWelcomeCard) {
                        HomeWelcomeCard(
                            appIcon = appIcon,
                            onLoginClick = onLoginClick,
                        )
                    }

                    if (isLoading && allListsEmpty && activeMangaContinueReading.isEmpty()) {
                        LoadingSkeleton()
                    } else {
                        val onAnimeClick: (AnimeMedia, HomeAnimeCardBounds?) -> Unit = { anime, _ -> selectedAnime = anime; showEpisodeSheet = true }
                        val onInfoClick: (AnimeMedia, HomeAnimeCardBounds?) -> Unit = { anime, bounds ->
                            val cardBounds = bounds?.let {
                                MainViewModel.CardBounds(anime.id, anime.cover, it.bounds)
                            }
                            currentCardBounds = cardBounds
                            viewModel.clearExploreAnimeCardBounds()
                            selectedAnime = anime
                            if (firstAnime == null) firstAnime = anime
                            showDetailedAnimeScreen = true
                        }

                        Column {
                            if (continueWatchingEpisodes.isNotEmpty()) {
                                SectionHeader(
                                    title = stringResource(R.string.home_continue_watching),
                                    icon = Icons.Default.PlayArrow,
                                    count = continueWatchingEpisodes.size,
                                    iconTint = HomeStatusColors.getColor("CURRENT"),
                                    onClick = {
                                        statusListTitle = context.getString(R.string.home_continue_watching)
                                        statusListIcon = Icons.Default.PlayArrow
                                        statusListType = "CURRENT"
                                        statusListIsManga = false
                                        showStatusListScreen = true
                                    }
                                )
                                ContinueWatchingEpisodeRow(
                                    entries = continueWatchingEpisodes,
                                    playbackPositions = playbackPositions,
                                    playbackDurations = playbackDurations,
                                    tmdbEpisodeCache = tmdbEpisodeCache,
                                    preferEnglishTitles = preferEnglishTitles,
                                    disableMaterialColors = disableMaterialColors,
                                    onPlayClick = { entry: ContinueWatchingEntry ->
                                        val anime = animeById[entry.animeId]
                                        if (anime != null) {
                                            val released = anime.latestEpisode ?: anime.totalEpisodes
                                            if (anime.latestEpisode != null && entry.episode > released) {
                                                context.toast(context.getString(R.string.home_episode_not_aired))
                                            } else {
                                                onPlayEpisode(anime, entry.episode, null)
                                            }
                                        }
                                    },
                                    onDismissClick = { entry: ContinueWatchingEntry ->
                                        viewModel.removeContinueWatchingEntry(entry.animeId, entry.episode)
                                    }
                                )
                            }

                            // Manga "Continue Reading" â€” direct resume cards, same style as anime's
                            // Continue Watching, placed at the top alongside it.
                            // Only show cards for a chapter that is actively in progress â€” hide
                            // entries whose current chapter hasn't been opened (no page count) or
                            // was scrolled to the end (finished, e.g. 25/25).
                            if (activeMangaContinueReading.isNotEmpty()) {
                                SectionHeader(
                                    title = stringResource(R.string.home_continue_reading),
                                    icon = Icons.Default.Bookmark,
                                    count = activeMangaContinueReading.size,
                                    iconTint = HomeStatusColors.getColor("CURRENT"),
                                    onClick = {
                                        statusListTitle = context.getString(R.string.home_continue_reading)
                                        statusListIcon = Icons.Default.PlayArrow
                                        statusListType = "CURRENT"
                                        statusListIsManga = true
                                        showStatusListScreen = true
                                    }
                                )
                                MangaContinueReadingRow(
                                    mangaList = activeMangaContinueReading,
                                    isOled = isOled,
                                    preferEnglishTitles = preferEnglishTitles,
                                    onResumeClick = onMangaContinueReadingClick,
                                    onDismissClick = onMangaDismissClick
                                )
                            }

                            HomeAnimeStatusSections(
                                effectiveCurrentlyWatching = effectiveCurrentlyWatching,
                                effectivePlanningToWatch = effectivePlanningToWatch,
                                effectiveCompleted = effectiveCompleted,
                                effectiveOnHold = effectiveOnHold,
                                effectiveDropped = effectiveDropped,
                                showStatusColors = showStatusColors,
                                preferEnglishTitles = preferEnglishTitles,
                                playbackPositions = playbackPositions,
                                playbackDurations = playbackDurations,
                                disableMaterialColors = disableMaterialColors,
                                currentScreenIndex = currentScreenIndex,
                                viewModel = viewModel,
                                onAnimeClick = onAnimeClick,
                                onInfoClick = onInfoClick,
                                onOpenStatusList = { title, icon, type, isManga ->
                                    statusListTitle = title
                                    statusListIcon = icon
                                    statusListType = type
                                    statusListIsManga = isManga
                                    showStatusListScreen = true
                                },
                            )

                            // ─── Manga sections ─────────────────────────────
                            HomeMangaStatusSections(
                                mangaCurrentlyReading = effectiveMangaCurrentlyReading,
                                mangaPlanningToRead = effectiveMangaPlanningToRead,
                                mangaCompleted = effectiveMangaCompleted,
                                mangaPaused = effectiveMangaPaused,
                                mangaDropped = effectiveMangaDropped,
                                isOled = isOled,
                                preferEnglishTitles = preferEnglishTitles,
                                onMangaClick = onMangaClick,
                                onMangaInfoClick = onMangaInfoClick,
                                onOpenStatusList = { title, icon, type, isManga ->
                                    statusListTitle = title
                                    statusListIcon = icon
                                    statusListType = type
                                    statusListIsManga = isManga
                                    showStatusListScreen = true
                                },
                            )

                            if (allListsEmpty && !showWelcomeCard) {
                                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                        shadowElevation = 1.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                                            Spacer(Modifier.height(12.dp))
                                            Text(stringResource(R.string.home_lists_empty), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(Modifier.height(4.dp))
                                            Text(stringResource(R.string.home_lists_empty_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            // Bottom clearance for the navigation bar, the same way the
                            // airing schedule reserves it: one trailing spacer, not
                            // container padding plus a spacer (which added up to 160dp).
                            Spacer(modifier = Modifier.height(Sizes.listBottomNavClearance))
                    }
                }
            }
        }
    }

        // Status list overlay — see HomeStatusSections.kt
        HomeStatusListOverlay(
            visible = showStatusListScreen,
            statusListTitle = statusListTitle,
            statusListIcon = statusListIcon,
            statusListType = statusListType,
            statusListIsManga = statusListIsManga,
            continueWatchingEpisodes = continueWatchingEpisodes,
            animeById = animeById,
            effectiveCurrentlyWatching = effectiveCurrentlyWatching,
            effectivePlanningToWatch = effectivePlanningToWatch,
            effectiveCompleted = effectiveCompleted,
            effectiveOnHold = effectiveOnHold,
            effectiveDropped = effectiveDropped,
            activeMangaContinueReading = activeMangaContinueReading,
            mangaCurrentlyReading = effectiveMangaCurrentlyReading,
            mangaPlanningToRead = effectiveMangaPlanningToRead,
            mangaCompleted = effectiveMangaCompleted,
            mangaPaused = effectiveMangaPaused,
            mangaDropped = effectiveMangaDropped,
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            onPlayEpisode = onPlayEpisode,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick,
            onMangaContinueReadingClick = onMangaContinueReadingClick,
            onEpisodeSheetAnime = { anime -> selectedAnime = anime; showEpisodeSheet = true },
            onStatusDialogAnime = { anime -> selectedAnime = anime; showStatusDialog = true },
            onInfoClick = { anime, bounds ->
                val cardBounds = bounds?.let {
                    MainViewModel.CardBounds(anime.id, anime.cover, it.bounds)
                }
                currentCardBounds = cardBounds
                viewModel.clearExploreAnimeCardBounds()
                selectedAnime = anime
                if (firstAnime == null) firstAnime = anime
                showDetailedAnimeScreen = true
            },
            onMangaStatusClick = { manga ->
                statusListMangaForDialog = manga
                showMangaStatusDialog = true
            },
            onClose = { showStatusListScreen = false },
        )

    }

    // Dialogs
    if (showEpisodeSheet && selectedAnime != null) {
        if (streamMethod == "magnet" && (defaultStreamExt != null || defaultMagnetExt != null) || streamMethod == "direct" && defaultPkg.isNotEmpty()) {
            RichEpisodeScreen(
                anime = selectedAnime!!,
                viewModel = viewModel,
                isOled = isOled,
                preferEnglishTitles = preferEnglishTitles,
                onDismiss = { showEpisodeSheet = false },
                onEpisodeSelect = { episode, title ->
                    onPlayEpisode(selectedAnime!!, episode, title)
                    showEpisodeSheet = false
                }
            )
        }
    }

    LaunchedEffect(showEpisodeSheet, selectedAnime) {
        val hasDefault = streamMethod == "magnet" && (defaultStreamExt != null || defaultMagnetExt != null) || streamMethod == "direct" && defaultPkg.isNotEmpty()
        if (showEpisodeSheet && selectedAnime != null && !hasDefault) {
            showEpisodeSheet = false
            showNoExtensionDialog = true
        }
    }

    LaunchedEffect(settingsReturnVersion) {
        if (settingsReturnVersion > 0 && reopenEpisodePickerAfterSettings) {
            reopenEpisodePickerAfterSettings = false
            if (selectedAnime != null) {
                val hasDefault = streamMethod == "magnet" && (defaultStreamExt != null || defaultMagnetExt != null) || streamMethod == "direct" && defaultPkg.isNotEmpty()
                if (hasDefault) {
                    showEpisodeSheet = true
                } else {
                    showNoExtensionDialog = true
                }
            }
        }
    }

    if (showNoExtensionDialog) {
        AlertDialog(
            onDismissRequest = { showNoExtensionDialog = false },
            title = { Text(stringResource(R.string.home_no_extension_title)) },
            text = { Text(stringResource(R.string.home_no_extension_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showNoExtensionDialog = false
                    reopenEpisodePickerAfterSettings = true
                    onNoExtension()
                }) {
                    Text(stringResource(R.string.home_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoExtensionDialog = false; reopenEpisodePickerAfterSettings = false }) {
                    Text(stringResource(R.string.home_close))
                }
            }
        )
    }

    if (showStatusDialog && selectedAnime != null) {
        HomeAnimeStatusDialog(
            anime = selectedAnime!!,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onDismiss = { showStatusDialog = false },
            onRemove = {
                viewModel.removeAnimeFromList(selectedAnime!!.id)
                showStatusDialog = false
            },
            onUpdate = { status, progress ->
                viewModel.updateAnimeStatus(selectedAnime!!.id, status, progress, null)
                showStatusDialog = false
            })
    }

    if (showMangaStatusDialog && statusListMangaForDialog != null) {
        val sm = statusListMangaForDialog!!
        MangaStatusDialog(
            title = sm.title,
            titleEnglish = sm.titleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            coverUrl = sm.cover,
            currentStatus = sm.listStatus,
            currentProgress = sm.progress,
            totalChapters = sm.totalChapters,
            isOled = isOled,
            onUpdate = { status, progress ->
                viewModel.updateMangaStatus(sm.id, status, progress, null, malId = sm.malId, title = sm.title, cover = sm.cover)
                if (progress != null) viewModel.updateMangaProgress(sm.id, progress.toFloat())
                showMangaStatusDialog = false
            },
            onRemove = {
                viewModel.removeMangaTracking(sm.id)
                showMangaStatusDialog = false
            },
            onDismiss = { showMangaStatusDialog = false }
        )
    }

    // Collect card bounds from ViewModel
    val viewModelCardBounds by viewModel.exploreAnimeCardBounds.collectAsState()
    val viewModelHomeCardBounds by viewModel.homeAnimeCardBounds.collectAsState()
    val effectiveCardBounds = viewModelHomeCardBounds ?: viewModelCardBounds

    LaunchedEffect(effectiveCardBounds) {
        if (effectiveCardBounds != null && currentCardBounds == null && showDetailedAnimeScreen) {
            currentCardBounds = effectiveCardBounds
        }
    }

    if (showDetailedAnimeScreen && selectedAnime != null) {
        val detailedAnimeData = selectedAnime!!.toDetailedAnimeData()
        val currentStatus by remember(listVersion, selectedAnime!!.id) {
            derivedStateOf { selectedAnime?.listStatus }
        }
        val currentProgress by remember(listVersion, selectedAnime!!.id) {
            derivedStateOf { selectedAnime?.progress }
        }
        DetailedAnimeScreen(
            anime = detailedAnimeData,
            viewModel = viewModel,
            isOled = isOled,
            settingsReturnVersion = settingsReturnVersion,
            currentStatus = currentStatus,
            currentProgress = currentProgress,
            isLoggedIn = isLoggedIn,
            isFavorite = favoriteIds.contains(selectedAnime!!.id),
            initialCardBounds = currentCardBounds,
            onDismiss = {
                currentCardBounds = null
                // Go back to first anime if navigated, otherwise close
                if (firstAnime != null && selectedAnime?.id != firstAnime?.id) {
                    selectedAnime = firstAnime
                } else {
                    showDetailedAnimeScreen = false
                    firstAnime = null
                }
            },
            onSwipeToClose = {
                currentCardBounds = null
                showDetailedAnimeScreen = false
                firstAnime = null
            },
            onPlayEpisode = { episode, _ ->
                onPlayEpisode(selectedAnime!!, episode, null)
                showDetailedAnimeScreen = false
            },
            onUpdateStatus = { status ->
                if (status != null) {
                    viewModel.addExploreAnimeToList(
                        ExploreAnime(
                            id = selectedAnime!!.id,
                            title = selectedAnime!!.title,
                            cover = selectedAnime!!.cover,
                            banner = selectedAnime!!.banner,
                            episodes = selectedAnime!!.totalEpisodes,
                            latestEpisode = selectedAnime!!.latestEpisode,
                            averageScore = selectedAnime!!.averageScore,
                            genres = selectedAnime!!.genres,
                            year = selectedAnime!!.year,
                            format = selectedAnime!!.format
                        ),
                        status
                    )
                }
            },
            onRemove = {
                viewModel.removeAnimeFromList(selectedAnime!!.id)
                showDetailedAnimeScreen = false
            },
            onRelationClick = { relation ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (relation.format == null || relation.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = relation.id,
                            title = relation.title,
                            cover = relation.cover,
                            totalChapters = 0,
                            averageScore = relation.averageScore,
                            format = relation.format
                        )
                    )
                } else {
                    scope.launch {
                        try {
                            currentCardBounds = null
                            viewModel.clearHomeAnimeCardBounds()
                            val detailedData = viewModel.fetchDetailedAnimeData(relation.id)
                            if (detailedData != null) {
                                selectedAnime = AnimeMedia(
                                    id = detailedData.id,
                                    title = detailedData.title,
                                    titleEnglish = detailedData.titleEnglish,
                                    cover = detailedData.cover,
                                    banner = detailedData.banner,
                                    progress = 0,
                                    totalEpisodes = detailedData.episodes,
                                    latestEpisode = detailedData.latestEpisode,
                                    status = detailedData.status ?: "",
                                    averageScore = detailedData.averageScore,
                                    genres = detailedData.genres,
                                    listStatus = "",
                                    listEntryId = 0,
                                    year = detailedData.year,
                                    malId = detailedData.malId
                                )
                            } else {
                                context.toast(context.getString(R.string.home_anime_not_found))
                            }
                            } catch (_: Exception) {
                                context.toast(context.getString(R.string.home_anime_not_found))
                        }
                    }
                }
            },
            onRecommendationClick = { rec ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (rec.format == null || rec.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = rec.id,
                            title = rec.title,
                            cover = rec.cover,
                            totalChapters = rec.episodes,
                            averageScore = rec.averageScore,
                            format = rec.format
                        )
                    )
                } else {
                    scope.launch {
                        try {
                            currentCardBounds = null
                            viewModel.clearHomeAnimeCardBounds()
                            val detailedData = viewModel.fetchDetailedAnimeData(rec.id)
                            if (detailedData != null) {
                                selectedAnime = AnimeMedia(
                                    id = detailedData.id,
                                    title = detailedData.title,
                                    titleEnglish = detailedData.titleEnglish,
                                    cover = detailedData.cover,
                                    banner = detailedData.banner,
                                    progress = 0,
                                    totalEpisodes = detailedData.episodes,
                                    latestEpisode = detailedData.latestEpisode,
                                    status = detailedData.status ?: "",
                                    averageScore = detailedData.averageScore,
                                    genres = detailedData.genres,
                                    listStatus = "",
                                    listEntryId = 0,
                                    year = detailedData.year,
                                    malId = detailedData.malId
                                )
                            } else {
                                context.toast(context.getString(R.string.home_anime_not_found))
                            }
                            } catch (_: Exception) {
                                context.toast(context.getString(R.string.home_anime_not_found))
                        }
                    }
                }
            },
            onNoExtension = {
                onNoExtension()
            },
            onCharacterClick = onCharacterClick,
            onStaffClick = onStaffClick,
            onViewAllCast = { onViewAllCast(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllStaff = { onViewAllStaff(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllRelations = { animeId, title, titleEnglish ->
                onViewAllRelations(animeId, title, titleEnglish)
            },
            onViewAllRecommendations = { animeId, title, titleEnglish ->
                onViewAllRecommendations(animeId, title, titleEnglish)
            }
        )
    }

    if (showOfflineFavoritesDialog) {
        OfflineFavoritesDialog(
            favorites = localFavorites,
            onDismiss = { showOfflineFavoritesDialog = false },
            onAnimeClick = { anime -> onShowAnimeDialog(anime, null) },
            onRemoveFavorite = { id -> viewModel.toggleLocalFavorite(id) },
            preferEnglishTitles = preferEnglishTitles
        )
    }

    if (showProfileSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoggedIn) {
                        if (userAvatar != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(userAvatar).build(),
                                contentDescription = stringResource(R.string.profile_sheet_avatar_content_description),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(52.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.profile_sheet_user_content_description), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = appIconDrawable(appIcon),
                                contentDescription = stringResource(R.string.profile_sheet_app_content_description),
                                modifier = Modifier.size(44.dp).clip(CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            if (isLoggedIn) (userName ?: stringResource(R.string.profile_sheet_default_username)) else "Tensei",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isLoggedIn) stringResource(R.string.profile_sheet_subtitle_logged_in) else stringResource(R.string.profile_sheet_subtitle_guest),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoggedIn) {
                    ProfileSheetItem(
                        icon = Icons.Default.AccountCircle,
                        label = stringResource(R.string.profile_sheet_my_profile),
                        onClick = {
                            showProfileSheet = false
                            onProfileClick()
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    ProfileSheetItem(
                        icon = Icons.Default.Favorite,
                        label = stringResource(R.string.profile_favorites),
                        onClick = {
                            showProfileSheet = false
                            showOfflineFavoritesDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                ProfileSheetItem(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.settings_title),
                    onClick = {
                        showProfileSheet = false
                        onNavigateToSettings?.invoke()
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Stop refreshing when loading completes or after timeout
    LaunchedEffect(isLoading, isRefreshing) {
        if (isRefreshing) {
            // Use a timeout to ensure refreshing stops even if loading state gets stuck
            delay(15000.milliseconds)
            isRefreshing = false
        }
    }
}


@Composable
private fun ProfileSheetItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
