package com.blissless.tensei.ui.screens.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.TagData
import com.blissless.tensei.ui.dialogs.AnimeRatingSheet
import com.blissless.tensei.ui.dialogs.HomeAnimeStatusDialog
import com.blissless.tensei.ui.dialogs.userScoreToDisplay
import com.blissless.tensei.ui.components.DetailRailSkeleton
import com.blissless.tensei.ui.components.DetailTextCardSkeleton
import com.blissless.tensei.ui.components.RelatedMediaCard
import com.blissless.tensei.ui.components.SkeletonPill
import com.blissless.tensei.ui.components.SkeletonTextLine
import com.blissless.tensei.ui.components.StaffEdgeJobText
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.asRelatedMedia
import com.blissless.tensei.ui.components.BannerOverlayText
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.statusAccentFor
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.ui.screens.episode.RichEpisodeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.toggleAniListFavorite
import com.blissless.tensei.viewmodel.toggleOfflineFavorite
import com.blissless.tensei.viewmodel.setLocalAnimeStatus
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.updateAnimeStatus
import com.blissless.tensei.viewmodel.updateAnimeScore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedAnimeScreen(
    anime: DetailedAnimeData,
    viewModel: MainViewModel,
    isOled: Boolean = false,
    currentStatus: String? = null,
    currentProgress: Int? = null,
    isLoggedIn: Boolean = false,
    isFavorite: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit = onDismiss,
    onSwipeToClose: () -> Unit = {},
    stackDepth: Int = 1,
    onPlayEpisode: (Int, String?) -> Unit = { _, _ -> },
    onUpdateStatus: (String?) -> Unit = {},
    onUpdateProgress: (Int) -> Unit = {},
    onRemove: () -> Unit = {},
    onUpdateLocalStatus: (String?) -> Unit = {},
    onRemoveLocalStatus: () -> Unit = {},
    onRelationClick: (AnimeRelation) -> Unit = {},
    onRecommendationClick: (ExploreAnime) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onViewAllCast: () -> Unit = {},
    onViewAllStaff: () -> Unit = {},
    onViewAllRelations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRecommendations: (Int, String, String?) -> Unit = { _, _, _ -> },
    preferEnglishTitles: Boolean = true,
    onNavigateToSettings: (() -> Unit)? = null,
    onNoExtension: () -> Unit = {},
    settingsReturnVersion: Int = 0,
    initialCardBounds: MainViewModel.CardBounds? = null
) {
    val context = LocalContext.current
    var showFullDescription by remember { mutableStateOf(false) }
    var showAllTags by remember { mutableStateOf(false) }

    var detailedData by remember { mutableStateOf<DetailedAnimeData?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }
    var relations by remember { mutableStateOf<List<AnimeRelation>>(emptyList()) }

    var isVisible by remember { mutableStateOf(false) }
    var previousAnimeId by remember { mutableIntStateOf(anime.id) }
    var isTransitioning by remember { mutableStateOf(false) }

    var selectedTagForDescription by remember { mutableStateOf<TagData?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var showEpisodeSelection by remember { mutableStateOf(false) }
    var showNoDefaultExtDialog by remember { mutableStateOf(false) }
    var reopenEpisodePickerAfterSettings by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var showRemoveFavoriteDialog by remember { mutableStateOf(false) }

    val defaultExtPkg by viewModel.defaultExtensionPackage.collectAsState()
    val defaultMagnetExt by viewModel.defaultMagnetExtension.collectAsState()
    val defaultStreamExt by viewModel.defaultStreamExtension.collectAsState()
    val streamMethod by viewModel.streamMethod.collectAsState()
    val magnetExtensions by viewModel.availableMagnetExtensions.collectAsState()
    val localFavorites by viewModel.localFavorites.collectAsState()
    val aniListFavorites by viewModel.aniListFavorites.collectAsState()
    // MAL-local favorites — consulted so the heart icon reflects locally-stored anime favorites
    // even when loginProvider == MAL (no AniList sync happens, so _aniListFavorites is empty).
    // In ANILIST / BOTH mode this list is typically a subset of aniListFavorites, so OR-ing
    // them is safe and won't cause false positives.
    val malFavorites by viewModel.malFavorites.collectAsState()
    val localAnimeStatus by viewModel.localAnimeStatus.collectAsState()
    val currentlyWatching by viewModel.currentlyWatching.collectAsState()
    val planningToWatch by viewModel.planningToWatch.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val onHold by viewModel.onHold.collectAsState()
    val dropped by viewModel.dropped.collectAsState()
    val localFavExists = localFavorites.containsKey(anime.id)
    val aniListIsFavorite = aniListFavorites.any { it.id == anime.id }
    val malIsFavorite = malFavorites.any { it.id == anime.id || it.malId == anime.id }
    // Heart icon source of truth: true if the anime is favorited via ANY of the available
    // sources — the passed-in isFavorite (computed upstream from aniListFavoriteIds),
    // the in-memory AniList list, the MAL-local list, or the offline local store. The
    // OR-ing covers MAL-only mode (where _aniListFavorites is empty but _malFavorites has
    // the user's favorites), BOTH-mode (both lists have the entry), and ANILIST-only mode
    // (only _aniListFavorites has the entry).
    val effectiveIsFavorite = when {
        isLoggedIn -> isFavorite || aniListIsFavorite || malIsFavorite
        else -> localFavExists
    }

    // Un-favouriting is destructive and trivially undoable only by re-tapping, so route it
    // through a confirmation. The actual toggle is deferred to the dialog's confirm action.
    val toggleFavorite: () -> Unit = {
        if (isLoggedIn) {
            val animeMedia = AnimeMedia(
                id = anime.id,
                title = anime.title,
                titleEnglish = anime.titleEnglish,
                cover = anime.cover,
                banner = anime.banner,
                totalEpisodes = anime.episodes,
                averageScore = anime.averageScore,
                genres = anime.genres,
                year = anime.year
            )
            viewModel.toggleAniListFavorite(anime.id, animeMedia)
        } else {
            viewModel.toggleOfflineFavorite(
                anime.id,
                anime.title,
                anime.cover,
                anime.banner,
                anime.year,
                anime.averageScore,
                titleEnglish = anime.titleEnglish
            )
        }
    }
    val onFavoriteClick: () -> Unit = {
        if (effectiveIsFavorite) showRemoveFavoriteDialog = true else toggleFavorite()
    }
    val effectiveLocalStatus = if (isLoggedIn) null else localAnimeStatus[anime.id]?.status
    val effectiveLocalProgress = if (isLoggedIn) null else localAnimeStatus[anime.id]?.progress

    var displayProgress by remember { mutableIntStateOf(currentProgress ?: effectiveLocalProgress ?: 0) }
    LaunchedEffect(currentProgress, effectiveLocalProgress) {
        displayProgress = currentProgress ?: effectiveLocalProgress ?: 0
    }

    val effectiveOnUpdateStatus = if (isLoggedIn) onUpdateStatus else onUpdateLocalStatus
    val effectiveOnUpdateProgress = if (isLoggedIn) onUpdateProgress else { progress ->
        viewModel.setLocalAnimeStatus(
            anime.id,
            localAnimeStatus[anime.id]?.copy(progress = progress)
                ?: LocalAnimeEntry(
                    id = anime.id,
                    status = effectiveLocalStatus ?: "CURRENT",
                    progress = progress,
                    totalEpisodes = anime.episodes,
                            title = anime.title,
                            cover = anime.cover,
                    banner = anime.banner,
                    year = anime.year,
                    averageScore = anime.averageScore
                )
        )
    }
    val effectiveOnRemove = if (isLoggedIn) onRemove else onRemoveLocalStatus

    val statusToCheck = if (isLoggedIn) currentStatus else effectiveLocalStatus
    val statusProgress = displayProgress
    val totalEps = anime.episodes

    val listUserScore = (currentlyWatching + planningToWatch + completed + onHold + dropped)
        .firstOrNull { it.id == anime.id }?.userScore
    val effectiveUserScore = (if (isLoggedIn) listUserScore else localAnimeStatus[anime.id]?.score)
        ?.takeIf { it > 0 }

    val slideOffset = remember { Animatable(1000f) }
    val dismissSlideOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        viewModel.loadAvailableMagnetExtensions()
    }

    LaunchedEffect(Unit) {
        slideOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(200, easing = LinearEasing)
        )
    }

    fun dismissWithAnimation() {
        scope.launch {
            dismissSlideOffset.snapTo(0f)
            dismissSlideOffset.animateTo(
                targetValue = 1000f,
                animationSpec = tween(150, easing = LinearEasing)
            )
            onDismiss()
            onSwipeToClose()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (slideOffset.value > 0 || dismissSlideOffset.value > 0) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "alpha"
    )

    var transitionProgress by remember { mutableFloatStateOf(0f) }

    val coverAnimationProgress = remember(anime.id) { Animatable(0f) }

    LaunchedEffect(initialCardBounds) {
        if (initialCardBounds != null) {
            transitionProgress = 1f
            coverAnimationProgress.snapTo(0f)
            coverAnimationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            isVisible = true
        } else {
            isVisible = true
            coverAnimationProgress.snapTo(0f)
            transitionProgress = 0f
        }
    }

    LaunchedEffect(anime.id, initialCardBounds) {
        isLoadingDetails = true

        if (previousAnimeId != 0 && previousAnimeId != anime.id) {
            isTransitioning = true
            coverAnimationProgress.snapTo(0f)
            delay(150.milliseconds)
            isTransitioning = false
        }
        previousAnimeId = anime.id

        // Try to fetch detailed data
        try {
            detailedData = viewModel.fetchDetailedAnimeData(anime.id, anime.malId, title = anime.titleEnglish ?: anime.title)
            // If fetch returns null (not found or error), keep using original anime data
            if (detailedData == null) {
                detailedData = anime
            }
            relations = detailedData?.relations ?: anime.relations
        } catch (_: Exception) {
            detailedData = anime
        } finally {
            isLoadingDetails = false
        }
    }

    // Auto-recovery: while the detail page is showing MAL-fallback data (AniList down),
    // re-check AniList every 60s. The reload shows the loading state; when AniList answers
    // again the detail â€” and its relations/recommendations rows â€” swap back seamlessly.
    val animeDetailSource by viewModel.animeDetailSource.collectAsState()
    LaunchedEffect(anime.id, animeDetailSource) {
        while (animeDetailSource == "mal") {
            delay(30_000)
            isLoadingDetails = true
            try {
                val refreshed = viewModel.fetchDetailedAnimeData(anime.id, anime.malId, title = anime.titleEnglish ?: anime.title)
                if (refreshed != null) {
                    detailedData = refreshed
                    relations = refreshed.relations
                }
            } catch (_: Exception) {
            } finally {
                isLoadingDetails = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isLoadingDetails = false
        }
    }

    val displayData = detailedData ?: anime

    val windowInfo = LocalWindowInfo.current
    val screenHeightPx = windowInfo.containerSize.height.toFloat()
    val dismissThreshold = screenHeightPx / 3f

    val offsetY = remember { Animatable(0f) }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = 0,
        initialFirstVisibleItemScrollOffset = 0
    )

    var isAtTop by remember { mutableStateOf(true) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                isAtTop = index == 0 && offset == 0
            }
    }

    val statusDisplay = when (displayData.status) {
        "RELEASING" -> stringResource(R.string.anime_detail_status_airing)
        "FINISHED" -> stringResource(R.string.anime_detail_status_released)
        "NOT_YET_RELEASED" -> stringResource(R.string.anime_detail_status_not_yet_aired)
        "CANCELLED" -> stringResource(R.string.anime_detail_status_cancelled)
        "HIATUS" -> stringResource(R.string.anime_detail_status_hiatus)
        else -> displayData.status ?: stringResource(R.string.anime_detail_unknown)
    }

    // The Information card's Status cell shows the user's tracking status, not the series
    // publication status (that one already has the header badge). Driven by the live tracking
    // state so it updates the instant the status dialog saves.
    val userStatusDisplay = statusToCheck?.let { StatusLabels[it] ?: it }

    val formatDisplay = when (displayData.format) {
        "TV" -> stringResource(R.string.anime_detail_format_tv_series)
        "TV_SHORT" -> stringResource(R.string.anime_detail_format_tv_short)
        "MOVIE" -> stringResource(R.string.anime_detail_format_movie)
        "SPECIAL" -> stringResource(R.string.anime_detail_format_special)
        "OVA" -> "OVA"
        "ONA" -> "ONA"
        "MUSIC" -> stringResource(R.string.anime_detail_format_music)
        else -> displayData.format ?: stringResource(R.string.anime_detail_unknown)
    }

    // Shared drag-to-dismiss behavior — see ui/components/SheetDismissGestures.kt
    val nestedScrollConnection = com.blissless.tensei.ui.components.rememberSheetDismissScrollConnection(
        offsetY = offsetY,
        isAtTop = { isAtTop },
        dismissThreshold = dismissThreshold,
        onDismiss = { dismissWithAnimation() },
    )

    Dialog(
        onDismissRequest = onNavigateBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val animeMedia = AnimeMedia(
            id = anime.id,
            title = anime.title,
            titleEnglish = anime.titleEnglish,
            cover = anime.cover,
            banner = anime.banner,
            progress = displayProgress,
            totalEpisodes = anime.episodes,
            latestEpisode = anime.latestEpisode,
            status = anime.status ?: "",
            averageScore = anime.averageScore,
            genres = anime.genres,
            listStatus = "",
            format = anime.format
        )
        if (showEpisodeSelection) {
            RichEpisodeScreen(
                anime = animeMedia,
                viewModel = viewModel,
                isOled = isOled,
                preferEnglishTitles = preferEnglishTitles,
                onDismiss = { showEpisodeSelection = false },
                onEpisodeSelect = { episode, _ ->
                    showEpisodeSelection = false
                    onPlayEpisode(episode, null)
                }
            )
        }

        if (showNoDefaultExtDialog) {
            NoDefaultExtensionDialog(
                onDismiss = { showNoDefaultExtDialog = false; reopenEpisodePickerAfterSettings = false },
                onGoToSettings = {
                    reopenEpisodePickerAfterSettings = true
                    onNoExtension()
                },
            )
        }

        LaunchedEffect(settingsReturnVersion) {
            if (settingsReturnVersion > 0 && reopenEpisodePickerAfterSettings) {
                reopenEpisodePickerAfterSettings = false
                val hasDefault = streamMethod == "magnet" && (defaultStreamExt != null || defaultMagnetExt != null) || streamMethod == "direct" && defaultExtPkg.isNotEmpty()
                if (hasDefault) {
                    showEpisodeSelection = true
                } else {
                    showNoDefaultExtDialog = true
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (slideOffset.value + dismissSlideOffset.value).roundToInt()) }
                .graphicsLayer {
                    this.alpha = alpha
                }
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .background(if (isOled) Color.Black else MaterialTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
                // Release-settle: a low-velocity release never produces a fling, so onPreFling is
                // never invoked and the sheet would stay stuck half-translated; and a fling
                // dismissal can be interrupted mid-animation, leaving the sheet faded but open.
                // On pointer-up, wait for any fling handling to start, then settle the sheet:
                // finish a stalled dismissal, dismiss past the threshold, or spring back.
                .pointerInput(dismissThreshold) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var anyPressed = true
                        while (anyPressed) {
                            val event = awaitPointerEvent()
                            anyPressed = event.changes.any { it.pressed }
                        }
                        // Defer the settle check to the composition scope: the gesture scope is a
                        // restricted coroutine scope (can't delay() from inside it), and this
                        // gives any fling-handling a moment to win first.
                        scope.launch {
                            delay(120)
                            if (offsetY.isRunning || dismissSlideOffset.isRunning) return@launch
                            if (dismissSlideOffset.value > 0f || offsetY.value > dismissThreshold) {
                                // A fling dismissal started but stalled, or the sheet is past the
                                // dismiss threshold â€” close it so it never stays stuck half-open.
                                dismissWithAnimation()
                            } else if (offsetY.value > 0f) {
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    }
                }
        ) {
            if (!displayData.banner.isNullOrEmpty() || displayData.cover.isNotEmpty()) {
                val bannerImage = displayData.banner ?: displayData.cover

                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { fullscreenImageUrl = bannerImage }
                ) {
                    AsyncImage(
                        model = bannerImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                this.alpha = 0.4f
                            }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            }

            IconButton(
                onClick = {
                    val shareText = buildString {
                        append(displayData.title)
                        append("\n\n")
                        append(com.blissless.tensei.network.Endpoints.AniList.animePageUrl(displayData.id))
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .zIndex(10f)
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.anime_detail_share), tint = Color.White, modifier = Modifier.size(24.dp))
            }

            IconButton(
                onClick = { if (stackDepth > 2) onDismiss() else onNavigateBack() },
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .zIndex(10f)
            ) {
                Icon(
                    if (stackDepth > 2) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (stackDepth > 2) stringResource(R.string.anime_detail_close) else stringResource(R.string.common_back),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)).zIndex(5f)
            )

            if (isLoadingDetails) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp)
                        .zIndex(10f),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
            val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
            val cinematicProgress = rememberCinematicAnimation("detailed_anime")
            val density = LocalDensity.current

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 140.dp + statusBarsPadding.calculateTopPadding(),
                    bottom = 32.dp + navigationBarsPadding.calculateBottomPadding()
                )
            ) {
item {
                AnimeDetailHeaderItem(
                    initialCardBounds = initialCardBounds,
                    alpha = alpha,
                    coverAnimationProgress = coverAnimationProgress,
                    displayData = displayData,
                    formatDisplay = formatDisplay,
                    statusDisplay = statusDisplay,
                    isLoadingDetails = isLoadingDetails,
                    onShowFullscreenImage = { fullscreenImageUrl = it },
                )
            }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    WatchNowButton(
                        status = displayData.status,
                        streamMethod = streamMethod,
                        hasDefaultMagnetExt = defaultMagnetExt != null,
                        hasDefaultStreamExt = defaultStreamExt != null,
                        hasDefaultExtPkg = defaultExtPkg.isNotEmpty(),
                        onNoDefaultExtension = { showNoDefaultExtDialog = true },
                        onShowEpisodeSelection = { showEpisodeSelection = true },
                    )
                }

item {
                AnimeDetailStatusItem(
                    anime = anime,
                    isLoggedIn = isLoggedIn,
                    alpha = alpha,
                    effectiveIsFavorite = effectiveIsFavorite,
                    effectiveUserScore = effectiveUserScore,
                    onFavoriteClick = onFavoriteClick,
                    statusProgress = statusProgress,
                    statusToCheck = statusToCheck,
                    onShowStatusDialogChange = { showStatusDialog = it },
                    onShowRatingSheetChange = { showRatingSheet = it },
                )
            }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    InfoCard(
                        displayData = displayData,
                        userStatusDisplay = userStatusDisplay,
                        isLoading = isLoadingDetails,
                    )
                }


                if (displayData.trailerUrl != null) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        TrailerCard(
                            trailerUrl = displayData.trailerUrl,
                            trailerThumbnail = displayData.trailerThumbnail,
                        )
                    }
                }

                if (displayData.genres.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        GenresCard(genres = displayData.genres)
                    }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(headerWidthFraction = 0.35f, bodyLines = 1)
                    }
                }

                if (displayData.tags.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        TagsCard(
                            tags = displayData.tags,
                            showAllTags = showAllTags,
                            onTagClick = { selectedTagForDescription = it },
                            onToggleShowAll = { showAllTags = !showAllTags },
                        )
                    }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(headerWidthFraction = 0.5f, bodyLines = 2)
                    }
                }

                if (!displayData.description.isNullOrEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SynopsisCard(
                            description = displayData.description,
                            showFullDescription = showFullDescription,
                            onToggleShowFull = { showFullDescription = !showFullDescription },
                        )
                    }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(bodyLines = 4)
                    }
                }

                // Show all relations including manga â€” manga relations are clickable and
                // will open the manga detail screen (wired via onRelationClick in MainActivity).
                val filteredRelations = displayData.relations

                if (filteredRelations.isNotEmpty()) {
item {
                AnimeDetailRelationsItem(
                    displayData = displayData,
                    alpha = alpha,
                    cinematicProgress = cinematicProgress,
                    density = density,
                    preferEnglishTitles = preferEnglishTitles,
                    onRelationClick = onRelationClick,
                    onViewAllRelations = onViewAllRelations,
                )
            }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 5, itemWidth = 110.dp)
                    }
                }

                // Recommendations
                val recommendations = displayData.recommendations
                if (recommendations.isNotEmpty()) {
item {
                AnimeDetailRecommendationsItem(
                    displayData = displayData,
                    alpha = alpha,
                    cinematicProgress = cinematicProgress,
                    density = density,
                    preferEnglishTitles = preferEnglishTitles,
                    onRecommendationClick = onRecommendationClick,
                    onViewAllRecommendations = onViewAllRecommendations,
                )
            }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 5, itemWidth = 110.dp)
                    }
                }

                // Cast Section
                val castList = displayData.characters?.nodes
                if (!castList.isNullOrEmpty()) {
item {
                AnimeDetailCharactersItem(
                    castList = castList,
                    alpha = alpha,
                    cinematicProgress = cinematicProgress,
                    density = density,
                    onCharacterClick = onCharacterClick,
                    onViewAllCast = onViewAllCast,
                )
            }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 6, itemWidth = 80.dp, posterAspectRatio = 1f)
                    }
                }

                // Staff Section
                val staffList = displayData.staff?.edges
                if (!staffList.isNullOrEmpty()) {
item {
                AnimeDetailStaffItem(
                    staffList = staffList,
                    alpha = alpha,
                    cinematicProgress = cinematicProgress,
                    density = density,
                    onStaffClick = onStaffClick,
                    onViewAllStaff = onViewAllStaff,
                )
            }
                } else if (isLoadingDetails) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 6, itemWidth = 80.dp, posterAspectRatio = 1f)
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedTagForDescription != null) {
        val tag: TagData = selectedTagForDescription!!
        ModalBottomSheet(
            onDismissRequest = { selectedTagForDescription = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        tag.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                tag.rank?.let { rank ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.anime_detail_rank, rank),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))
                val description = tag.description ?: stringResource(R.string.anime_detail_no_description)
                Text(
                    anilistAnnotated(description, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primary),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 24.sp
                )
            }
        }
    }

    if (fullscreenImageUrl != null) {
        Dialog(onDismissRequest = { fullscreenImageUrl = null }) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures { fullscreenImageUrl = null }
                    }
                )
                AsyncImage(
                    model = fullscreenImageUrl,
                    contentDescription = stringResource(R.string.anime_detail_fullscreen_image),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
            }
        }
    }

    if (showStatusDialog) {
        val animeMedia = AnimeMedia(
            id = anime.id,
            title = anime.title,
            titleEnglish = anime.titleEnglish,
            cover = anime.cover,
            banner = anime.banner,
            progress = statusProgress,
            totalEpisodes = totalEps,
            latestEpisode = displayData.latestEpisode,
            listStatus = statusToCheck ?: "",
            averageScore = anime.averageScore,
            userScore = effectiveUserScore,
            year = anime.year
        )
        HomeAnimeStatusDialog(
            anime = animeMedia,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onDismiss = { showStatusDialog = false },
            onRemove = {
                effectiveOnRemove()
                showStatusDialog = false
            },
            onUpdate = { status: String, progress: Int? ->
                if (isLoggedIn) {
                    viewModel.updateAnimeStatus(anime.id, status, progress, null)
                } else {
                    effectiveOnUpdateStatus(status)
                    if (progress != null) effectiveOnUpdateProgress(progress)
                    viewModel.setLocalAnimeStatus(
                        anime.id,
                        LocalAnimeEntry(
                            id = anime.id,
                            status = status,
                            progress = progress ?: localAnimeStatus[anime.id]?.progress ?: 0,
                            totalEpisodes = anime.episodes,
                            title = anime.title,
                            cover = anime.cover,
                            banner = anime.banner,
                            year = anime.year,
                            averageScore = anime.averageScore,
                            score = localAnimeStatus[anime.id]?.score
                        )
                    )
                }
                if (progress != null) displayProgress = progress
                showStatusDialog = false
            }
        )
    }

    if (showRemoveFavoriteDialog) {
        val removeFavoriteTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemoveFavoriteDialog = false },
            title = { Text(stringResource(R.string.anime_detail_remove_favorite)) },
            text = { Text(stringResource(R.string.anime_detail_remove_favorite_message, removeFavoriteTitle)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showRemoveFavoriteDialog = false
                    toggleFavorite()
                }) { Text(stringResource(R.string.anime_detail_remove)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRemoveFavoriteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showRatingSheet) {
        val animeMedia = AnimeMedia(
            id = anime.id,
            title = anime.title,
            titleEnglish = anime.titleEnglish,
            cover = anime.cover,
            banner = anime.banner,
            totalEpisodes = anime.episodes,
            averageScore = anime.averageScore,
            userScore = effectiveUserScore,
            year = anime.year
        )
        AnimeRatingSheet(
            anime = animeMedia,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onDismiss = { showRatingSheet = false },
            onScoreSaved = { score ->
                if (isLoggedIn) {
                    // Score-only update — does NOT flip the list status. Previously this called
                    // updateAnimeStatus with status = statusToCheck ?: "CURRENT", which moved
                    // the anime to Currently Watching if it had no list status (statusToCheck
                    // was null) — a surprising side effect for a rating action. Now we route
                    // through updateAnimeScore, which only touches the score field locally
                    // and queues a score-only AniList/MAL sync.
                    //
                    // score is Int? (from AnimeRatingSheet's onScoreSaved: (Int?) -> Unit
                    // signature — the sheet passes null when the user clears the rating). We
                    // guard null here and skip the update entirely in that case; a future
                    // improvement could route null through a dedicated clearScore path.
                    if (score != null) {
                        viewModel.updateAnimeScore(anime.id, score, malId = anime.malId)
                    }
                } else {
                    // Offline / logged-out: update the local entry's score only, preserving
                    // the existing status and progress. If the anime has no local entry yet,
                    // create one with status = "CURRENT" as a sensible default (the user is
                    // rating it, so they presumably intend to track it — better than leaving
                    // it untracked and having the score silently dropped on next app restart).
                    //
                    // Reads from the screen-scoped `localAnimeStatus` (collected at the top of
                    // DetailedAnimeScreen via collectAsState) — NOT from a fresh collectAsState()
                    // call here, because this lambda is a click handler (not a @Composable scope)
                    // and collectAsState() can only be called from a composable.
                    val existingEntry = localAnimeStatus[anime.id]
                    if (existingEntry != null) {
                        if (score != null) {
                            viewModel.setLocalAnimeStatus(anime.id, existingEntry.copy(score = score))
                        }
                    } else {
                        viewModel.setLocalAnimeStatus(
                            anime.id,
                            LocalAnimeEntry(
                                id = anime.id,
                                status = "CURRENT",
                                progress = statusProgress,
                                totalEpisodes = anime.episodes,
                                title = anime.title,
                                cover = anime.cover,
                                banner = anime.banner,
                                year = anime.year,
                                averageScore = anime.averageScore,
                                score = score
                            )
                        )
                    }
                }
                showRatingSheet = false
            },
        )
    }
}

