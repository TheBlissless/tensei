package com.blissless.tensei.ui.screens.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.ui.components.SectionTitle
import com.blissless.tensei.ui.components.AppIconCircle
import com.blissless.tensei.ui.components.CarouselPageDots
import com.blissless.tensei.ui.components.ExploreTopBarRow
import com.blissless.tensei.ui.components.SearchCircleAction
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.screens.manga.MangaStatusDialog
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.util.ErrorHandler
import com.blissless.tensei.viewmodel.fetchMangaExplore
import com.blissless.tensei.viewmodel.isLoadingManga
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaExploreSections
import com.blissless.tensei.viewmodel.mangaExploreSource
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.retryMangaExploreFromMalFallback
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.updateMangaStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import java.util.Locale
import com.blissless.tensei.ui.components.RailCardTitle
import com.blissless.tensei.ui.components.TenseiErrorBanner
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.viewmodel.tryManualRefresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaScreen(
    viewModel: MainViewModel,
    isOled: Boolean = false,
    showMangaCardButtons: Boolean = false,
    showMangaStatusColors: Boolean = false,
    preferEnglishTitles: Boolean = true,
    isVisible: Boolean = true,
    onSearchClick: () -> Unit = {},
    onMangaClick: (MangaExploreMedia) -> Unit = {},
    onMangaReadClick: (MangaExploreMedia) -> Unit = {},
    onMangaNoExtension: () -> Unit = {},
    settingsReturnVersion: Int = 0
) {
    val mangaExploreSections by viewModel.mangaExploreSections.collectAsState()
    val isLoadingManga by viewModel.isLoadingManga.collectAsState()
    val mangaCurrentlyReading by viewModel.mangaCurrentlyReading.collectAsState()
    val mangaPlanningToRead by viewModel.mangaPlanningToRead.collectAsState()
    val mangaCompleted by viewModel.mangaCompleted.collectAsState()
    val selectedMangaExtension by viewModel.selectedExtensionAuthority.collectAsState()
    val mangaSourceMethod by viewModel.mangaSourceMethod.collectAsState()
    val defaultExternalMangaExtension by viewModel.defaultExternalMangaExtension.collectAsState()
    val hasMangaSource = selectedMangaExtension != null ||
        (mangaSourceMethod == "external" && !defaultExternalMangaExtension.isNullOrBlank())
    val apiError by viewModel.apiError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val appIcon by viewModel.appIcon.collectAsState()

    // Create a map of mangaId -> status for quick lookup
    val mangaStatusMap = remember(mangaCurrentlyReading, mangaPlanningToRead, mangaCompleted) {
        buildMap {
            mangaCurrentlyReading.forEach { put(it.id, "CURRENT") }
            mangaPlanningToRead.forEach { put(it.id, "PLANNING") }
            mangaCompleted.forEach { put(it.id, "COMPLETED") }
        }
    }
    val mangaProgressMap = remember(mangaCurrentlyReading, mangaPlanningToRead, mangaCompleted) {
        buildMap {
            (mangaCurrentlyReading + mangaPlanningToRead + mangaCompleted)
                .forEach { if (it.progress > 0) put(it.id, it.progress) }
        }
    }

    var showMangaNoExtensionDialog by remember { mutableStateOf(false) }
    var pendingMangaReadAfterSettings by remember { mutableStateOf<MangaExploreMedia?>(null) }
    var showMangaStatusDialog by remember { mutableStateOf(false) }
    var selectedMangaForStatus by remember { mutableStateOf<MangaExploreMedia?>(null) }

    // Category list screen state (mirrors home's status list screen overlay)
    var showCategoryList by remember { mutableStateOf(false) }
    var categoryListTitle by remember { mutableStateOf("") }
    var categoryListIcon by remember { mutableStateOf(Icons.AutoMirrored.Filled.MenuBook) }
    var categoryListManga by remember { mutableStateOf<List<MangaExploreMedia>>(emptyList()) }

    // Hide the bottom navbar while the category list overlay is open (mirror home's status list).
    LaunchedEffect(showCategoryList) {
        viewModel.setHideNavbar(showCategoryList)
    }

    fun openMangaCategory(title: String, icon: ImageVector, list: List<MangaExploreMedia>) {
        categoryListTitle = title
        categoryListIcon = icon
        categoryListManga = list
        showCategoryList = true
    }

    // No-default-manga-extension dialog for the Read Now button. The user is taken to
    // the chapter selection only after a manga extension has been chosen.
    if (showMangaNoExtensionDialog) {
        AlertDialog(
            onDismissRequest = { showMangaNoExtensionDialog = false; pendingMangaReadAfterSettings = null },
            title = { Text(stringResource(R.string.explore_no_extension_selected)) },
            text = { Text(stringResource(R.string.explore_no_manga_extension_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showMangaNoExtensionDialog = false
                    onMangaNoExtension()
                }) {
                    Text(stringResource(R.string.explore_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMangaNoExtensionDialog = false; pendingMangaReadAfterSettings = null }) {
                    Text(stringResource(R.string.explore_close))
                }
            }
        )
    }

    LaunchedEffect(settingsReturnVersion) {
        val pending = pendingMangaReadAfterSettings
        if (settingsReturnVersion > 0 && pending != null) {
            pendingMangaReadAfterSettings = null
            if (!hasMangaSource) {
                showMangaNoExtensionDialog = true
            } else {
                onMangaReadClick(pending)
            }
        }
    }

    // Status dialog for manga carousel Save button
    if (showMangaStatusDialog && selectedMangaForStatus != null) {
        val manga = selectedMangaForStatus!!
        MangaStatusDialog(
            title = manga.title.romaji ?: manga.title.english ?: stringResource(R.string.explore_unknown),
            titleEnglish = manga.title.english,
            preferEnglishTitles = preferEnglishTitles,
            coverUrl = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: "",
            currentStatus = mangaStatusMap[manga.id],
            currentProgress = mangaProgressMap[manga.id] ?: 0,
            totalChapters = manga.chapters ?: 0,
            isOled = isOled,
            onDismiss = { showMangaStatusDialog = false },
            onRemove = {
                viewModel.removeMangaTracking(manga.id)
                showMangaStatusDialog = false
            },
            onUpdate = { status, progress ->
                viewModel.updateMangaStatus(manga.id, status, progress, null, malId = manga.idMal, title = manga.title.romaji ?: manga.title.english ?: "", cover = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: "")
                if (progress != null) {
                    viewModel.updateMangaProgress(manga.id, progress.toFloat())
                }
                showMangaStatusDialog = false
            }
        )
    }

    val scrollState = rememberScrollState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Reset pull-to-refresh when loading completes
    LaunchedEffect(isLoadingManga) {
        if (!isLoadingManga) {
            isRefreshing = false
        }
    }

    // True once the manga API has returned anything at all.
    val hasAnyMangaData = mangaExploreSections.values.any { it.isNotEmpty() }

    // Manga explore load cycle: show a loading screen until data arrives or the fetch concludes.
    var mangaTimedOut by remember { mutableStateOf(false) }

    // Whole-screen skeleton: show it immediately on first open (before any fetch has
    // started) and keep it until the fetch returns anything or the fetch cycle concludes.
    var exploreFetchesStarted by remember { mutableStateOf(false) }
    LaunchedEffect(isLoadingManga) {
        if (isLoadingManga) exploreFetchesStarted = true
    }
    val showMangaSkeleton =
        !hasAnyMangaData && (!exploreFetchesStarted || isLoadingManga)

    // Fetch trigger — keyed only on visibility/timeout, so a failed fetch can't re-trigger
    // itself into a request loop when the API is down.
    LaunchedEffect(isVisible, mangaTimedOut) {
        if (isVisible && mangaExploreSections.values.all { it.isEmpty() } && !isLoadingManga && !mangaTimedOut) {
            viewModel.fetchMangaExplore()
        }
    }

    // When the manga fetch concludes without data, surface the API banner immediately.
    LaunchedEffect(isLoadingManga, mangaExploreSections, mangaTimedOut) {
        if (!isLoadingManga && mangaExploreSections.values.all { it.isEmpty() }) mangaTimedOut = true
    }

    // Quit the failure state as soon as data arrives.
    LaunchedEffect(mangaExploreSections) {
        if (mangaExploreSections.values.any { it.isNotEmpty() }) mangaTimedOut = false
    }

    // Auto-recovery: while the manga explore rows come from the MAL fallback (AniList down),
    // quietly re-try AniList on an interval so the screen swaps back as soon as AniList
    // recovers — MAL is a transient stopgap, AniList is the source of truth.
    val mangaExploreSource by viewModel.mangaExploreSource.collectAsState()
    LaunchedEffect(isVisible, mangaExploreSource) {
        while (isVisible && mangaExploreSource == "mal") {
            delay(30_000)
            viewModel.retryMangaExploreFromMalFallback()
        }
    }

    val requestRefresh: () -> Unit = {
        if (viewModel.tryManualRefresh("manga")) {
            isRefreshing = true
            scope.launch {
                viewModel.fetchMangaExplore()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = requestRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (showMangaSkeleton) {
                // Whole-screen skeleton. Same composable the Anime tab uses; the status-bar
                // inset matters here because neither tab draws its top bar while loading.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .statusBarsPadding()
                ) {
                    ExploreScreenSkeleton()
                    Spacer(Modifier.height(Sizes.listBottomNavClearance))
                }
            } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
            if (apiError != null || isOffline) {
                TenseiErrorBanner(
                    message = if (isOffline) stringResource(R.string.home_offline) else stringResource(R.string.home_anilist_unavailable),
                    icon = if (isOffline) Icons.Default.SignalWifiOff else Icons.Default.CloudOff,
                    onRetry = requestRefresh,
                    isOffline = isOffline,
                    isOled = isOled,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // No "Manga" header — sections flow directly.
            if (mangaExploreSections.isNotEmpty()) {
                val sectionLabelMap = mapOf(
                    "trending" to stringResource(R.string.explore_trending_now),
                    "popular" to stringResource(R.string.explore_most_popular),
                    "topRated" to stringResource(R.string.explore_top_rated),
                    "favourites" to stringResource(R.string.explore_most_favourited),
                    "action" to stringResource(R.string.explore_genre_action),
                    "romance" to stringResource(R.string.explore_genre_romance),
                    "comedy" to stringResource(R.string.explore_genre_comedy),
                    "fantasy" to stringResource(R.string.explore_genre_fantasy),
                    "sci-fi" to stringResource(R.string.explore_genre_scifi),
                    "seinen" to stringResource(R.string.explore_genre_seinen)
                )
                val mangaSectionOrder = listOf(
                    "trending", "popular", "topRated", "favourites",
                    "action", "comedy", "fantasy", "romance", "sci-fi", "seinen"
                )
                val mangaSectionIconMap = mapOf(
                    "trending" to Icons.Default.Whatshot,
                    "popular" to Icons.Default.Favorite,
                    "topRated" to Icons.Default.Star,
                    "favourites" to Icons.Default.Bookmark,
                    "action" to Icons.Default.Whatshot,
                    "romance" to Icons.Default.Favorite,
                    "comedy" to Icons.Default.SentimentSatisfied,
                    "fantasy" to Icons.Default.AutoAwesome,
                    "sci-fi" to Icons.Default.Explore,
                    "seinen" to Icons.AutoMirrored.Filled.MenuBook
                )

                // Featured carousel fed by the trending section (limited to 10 items).
                val trendingList = mangaExploreSections["trending"].orEmpty().take(10)
                if (trendingList.isNotEmpty()) {
                    MangaFeaturedCarousel(
                        mangaList = trendingList,
                        onStatusClick = { manga ->
                            selectedMangaForStatus = manga
                            showMangaStatusDialog = true
                        },
                        onReadClick = { manga ->
                            if (!hasMangaSource) {
                                pendingMangaReadAfterSettings = manga
                                showMangaNoExtensionDialog = true
                            } else {
                                onMangaReadClick(manga)
                            }
                        },
                        onInfoClick = onMangaClick,
                        mangaStatusMap = mangaStatusMap,
                        preferEnglishTitles = preferEnglishTitles,
                        appIcon = appIcon,
                        onSearchClick = onSearchClick,
                        autoScrollEnabled = isVisible,
                        isVisible = isVisible,
                        isDialogOpen = showMangaStatusDialog || showMangaNoExtensionDialog
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Section rows in fixed order. Trending now also renders as a row with the full
                // 50 manga below the carousel (which only shows the first 10).
                mangaSectionOrder.forEachIndexed { sectionIndex, key ->
                    val list = mangaExploreSections[key].orEmpty()
                    if (list.isNotEmpty()) {
                        val label = sectionLabelMap[key] ?: key.replaceFirstChar { it.uppercase() }
                        SectionTitle(
                            label,
                            list.size,
                            isOled,
                            onClick = {
                                openMangaCategory(
                                    label,
                                    mangaSectionIconMap[key] ?: Icons.AutoMirrored.Filled.MenuBook,
                                    list
                                )
                            }
                        )
                        MangaExploreHorizontalRow(
                            mangaList = list,
                            isOled = isOled,
                            preferEnglishTitles = preferEnglishTitles,
                            showMangaCardButtons = showMangaCardButtons,
                            showMangaStatusColors = showMangaStatusColors,
                            mangaStatusMap = mangaStatusMap,
                            onMangaClick = onMangaClick,
                            onStatusClick = { manga ->
                                selectedMangaForStatus = manga
                                showMangaStatusDialog = true
                            },
                            onReadClick = { manga ->
                                if (!hasMangaSource) {
                                    pendingMangaReadAfterSettings = manga
                                    showMangaNoExtensionDialog = true
                                } else {
                                    onMangaReadClick(manga)
                                }
                            },
                            listIndex = sectionIndex,
                            isVisible = isVisible
                        )
                    }
                }
} else if (!mangaTimedOut) {
                // Fetch is still resolving — same skeleton as the whole-screen state,
                // instead of a spinner or a bare empty screen.
                ExploreScreenSkeleton()
            } else {
                // Manga fetch concluded with nothing returned — show the AniList unavailable banner.
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.home_anilist_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.explore_manga_nothing_returned),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            mangaTimedOut = false
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.explore_retry))
                        }
                    }
                }
            }

            // Bottom clearance for the navigation bar, reserved the same way the
            // airing schedule does it: one trailing spacer, not container padding
            // plus a spacer (which added up to 160dp).
            Spacer(modifier = Modifier.height(Sizes.listBottomNavClearance))
            }
        }
        }

        // Full-screen category list overlay (mirrors home's status list screen)
        AnimatedVisibility(
            visible = showCategoryList,
            enter = slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { (it * 0.15f).toInt() } + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { (it * 0.15f).toInt() } + fadeOut(animationSpec = tween(250))
        ) {
                ExploreCategoryListScreen(
                    title = categoryListTitle,
                    icon = categoryListIcon,
                    mangaList = categoryListManga,
                    isManga = true,
                    preferEnglishTitles = preferEnglishTitles,
                    onMangaClick = onMangaClick,
                    onMangaBookmarkClick = { manga ->
                        selectedMangaForStatus = manga
                        showMangaStatusDialog = true
                    },
                    onBackClick = { showCategoryList = false },
                    onDismiss = { showCategoryList = false }
                )
        }

    }
}
