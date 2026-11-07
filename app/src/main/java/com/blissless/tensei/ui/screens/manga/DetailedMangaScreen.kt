package com.blissless.tensei.ui.screens.manga

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import com.blissless.tensei.viewmodel.malMangaFavorites
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description             
import androidx.compose.material.icons.filled.EmojiEvents            
import androidx.compose.material.icons.filled.Favorite                
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.blissless.tensei.ui.components.DetailRailSkeleton
import com.blissless.tensei.ui.components.DetailSpecGridSkeleton
import com.blissless.tensei.ui.components.DetailStatStripSkeleton
import com.blissless.tensei.ui.components.DetailTextCardSkeleton
import com.blissless.tensei.ui.components.RelatedMediaCard
import com.blissless.tensei.ui.components.SkeletonPill
import com.blissless.tensei.ui.components.SkeletonTextLine
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.asRelatedMedia
import com.blissless.tensei.ui.components.BannerOverlayText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coil.compose.AsyncImage
import com.blissless.tensei.R
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaRelation
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaRankingEntry
import com.blissless.tensei.data.models.TagData
import com.blissless.tensei.ui.screens.details.GenresCard
import com.blissless.tensei.ui.screens.details.SynopsisCard
import com.blissless.tensei.ui.screens.details.TagsCard
import com.blissless.tensei.ui.screens.details.easeOut
import com.blissless.tensei.ui.screens.details.formatDate
import com.blissless.tensei.util.LocaleDate
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.statusAccentFor
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.ui.theme.MangaStatusLabels
import com.blissless.tensei.ui.dialogs.userScoreToDisplay
import com.blissless.tensei.viewmodel.clearMangaDetail
import com.blissless.tensei.viewmodel.fetchMangaDetail
import com.blissless.tensei.viewmodel.loadMangaChapters
import com.blissless.tensei.viewmodel.mangaChapters
import com.blissless.tensei.viewmodel.mangaDetail
import com.blissless.tensei.viewmodel.mangaTotalChapters
import com.blissless.tensei.viewmodel.isLoadingManga
import com.blissless.tensei.viewmodel.isLoadingMangaChapters
import com.blissless.tensei.viewmodel.mangaDetailSource
import com.blissless.tensei.viewmodel.toggleMangaFavorite
import com.blissless.tensei.viewmodel.favoritedMangaIds
import com.blissless.tensei.viewmodel.updateMangaStatus
import com.blissless.tensei.viewmodel.updateMangaScore
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.viewmodel.mangaDropped
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.viewmodel.toggleOfflineFavorite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedMangaScreen(
    manga: MangaMedia,
    viewModel: MainViewModel,
    isOled: Boolean = false,
    currentStatus: String? = null,
    currentProgress: Int? = null,
    isLoggedIn: Boolean = false,
    isFavorite: Boolean = false,
    preferEnglishTitles: Boolean = true,
    autoShowChapters: Boolean = false,
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit = onDismiss,
    onCloseAll: () -> Unit = onDismiss,
    onSwipeToClose: () -> Unit = {},
    stackDepth: Int = 1,
    onUpdateStatus: (String?, Int?) -> Unit = { _, _ -> },
    onUpdateProgress: (Int) -> Unit = {},
    onRemove: () -> Unit = {},
    onRelationClick: (MangaRelation) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onViewAllCharacters: () -> Unit = {},
    onViewAllStaff: () -> Unit = {},
    onViewAllRelations: () -> Unit = {},
    onViewAllRecommendations: () -> Unit = {},
    onStartReader: (chapterIndex: Int) -> Unit = {},
    navigateToMangaDetail: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var showFullDescription by remember { mutableStateOf(false) }
    var showAllTags by remember { mutableStateOf(false) }
    DisposableEffect(manga.id) {
        onDispose {
        }
    }

    val detail by viewModel.mangaDetail.collectAsState()
    val chapters by viewModel.mangaChapters.collectAsState()
    val isLoading by viewModel.isLoadingManga.collectAsState()
    val isLoadingChapters by viewModel.isLoadingMangaChapters.collectAsState()
    val selectedExtension by viewModel.selectedExtensionAuthority.collectAsState()
    val mangaSourceMethod by viewModel.mangaSourceMethod.collectAsState()
    val defaultExternalMangaExtension by viewModel.defaultExternalMangaExtension.collectAsState()
    val favoritedMangaIds by viewModel.favoritedMangaIds.collectAsState()
    // MAL-local favorites — consulted when loginProvider == MAL so the heart reflects
    // locally-stored manga favorites even when AniList isn't the active provider. In
    // ANILIST / BOTH mode this set is typically a subset of favoritedMangaIds (the AniList
    // source of truth) so OR-ing them is safe and won't cause false positives.
    val malMangaFavoriteIds by viewModel.malMangaFavorites.collectAsState()
    // Reactive favorite state — true if the manga is favorited via EITHER provider's local
    // store. Overrides the static isFavorite parameter passed in from the call site.
    val isMangaFavorited = manga.id in favoritedMangaIds || manga.id in malMangaFavoriteIds
    var showRemoveFavoriteDialog by remember { mutableStateOf(false) }

    // Un-favouriting is destructive and trivially undoable only by re-tapping, so route it
    // through a confirmation. The actual toggle is deferred to the dialog's confirm action.
    val toggleFavorite: () -> Unit = {
        if (isLoggedIn) {
            viewModel.toggleMangaFavorite(manga.id)
        } else {
            viewModel.toggleOfflineFavorite(manga.id, manga.title, manga.cover, manga.banner, manga.year, manga.averageScore, titleEnglish = manga.titleEnglish)
        }
    }
    val onFavoriteClick: () -> Unit = {
        if (isMangaFavorited) showRemoveFavoriteDialog = true else toggleFavorite()
    }

    // Live status/progress from the local tracking lists so the status dialog and
    // status chip update immediately after a change (no need to reopen the screen).
    val currentlyReadingManga by viewModel.mangaCurrentlyReading.collectAsState()
    val planningToReadManga by viewModel.mangaPlanningToRead.collectAsState()
    val completedManga by viewModel.mangaCompleted.collectAsState()
    val pausedManga by viewModel.mangaPaused.collectAsState()
    val droppedManga by viewModel.mangaDropped.collectAsState()
    val liveTrack = (currentlyReadingManga + planningToReadManga + completedManga + pausedManga + droppedManga)
        .find { it.id == manga.id }
    val liveStatus = liveTrack?.listStatus
    val liveProgress = liveTrack?.progress ?: currentProgress ?: manga.progress

    var showStatusDialog by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var selectedTagForDescription by remember { mutableStateOf<TagData?>(null) }
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

    var autoOpenedChapters by remember { mutableStateOf(false) }
    if (autoShowChapters) {
        LaunchedEffect(chapters, isLoadingChapters) {
            if (!isLoadingChapters && chapters.isNotEmpty() && !autoOpenedChapters) {
                autoOpenedChapters = true
                onStartReader(-1)
            }
        }
    }

    val slideOffset = remember { Animatable(1000f) }
    val dismissSlideOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(manga.id, selectedExtension, mangaSourceMethod, defaultExternalMangaExtension) {
        viewModel.fetchMangaDetail(manga.id, manga.malId)
        viewModel.loadMangaChapters(manga.id, manga.title)
    }

    // NOTE: We intentionally do NOT call viewModel.clearMangaDetail() on dispose here.
    // When the user taps "Read Now", the detail screen closes (leaving composition) and
    // the reader opens. If we cleared manga detail/chapters here, the reader would lose
    // the already-loaded chapters and have to reload them (race condition with LaunchedEffect
    // key = manga.id, which won't re-fire). Instead, clearMangaDetail is called by
    // MainActivity when the reader is fully dismissed (onClose).

    // Auto-recovery: while the detail is showing MAL-fallback data (AniList down),
    // re-check AniList every 60s via fetchMangaDetail, which shows the loading skeleton
    // until AniList answers and swaps the detail back to the real AniList data.
    val mangaDetailSource by viewModel.mangaDetailSource.collectAsState()
    LaunchedEffect(manga.id, mangaDetailSource) {
        while (mangaDetailSource == "mal") {
            delay(30_000)
            viewModel.fetchMangaDetail(manga.id, manga.malId)
        }
    }

    LaunchedEffect(Unit) {
        slideOffset.animateTo(targetValue = 0f, animationSpec = tween(200, easing = LinearEasing))
    }

    fun dismissWithAnimation() {
        scope.launch {
            dismissSlideOffset.snapTo(0f)
            dismissSlideOffset.animateTo(targetValue = 1000f, animationSpec = tween(150, easing = LinearEasing))
            onDismiss()
            onSwipeToClose()
        }
    }

    // Handle system back button â€” dismiss the detail screen
    BackHandler { dismissWithAnimation() }

    val alpha by animateFloatAsState(
        targetValue = if (slideOffset.value > 0 || dismissSlideOffset.value > 0) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing), label = "alpha"
    )

    // Blank listStatus (default) means the user has no status â€” normalize to null so the
    // "Add to List" section doesn't show a stale chip or "0 / x" progress.
    val statusToCheck = (liveStatus ?: currentStatus ?: manga.listStatus).takeIf { it.isNotBlank() }
    val statusProgress = liveProgress
    val liveScore = (liveTrack?.userScore ?: manga.userScore)?.takeIf { it > 0 }
    // Total chapters from the user-selected extension (current release count). Overrides the
    // stale AniList value once loadMangaChapters has run for this manga.
    val extensionTotalChapters by viewModel.mangaTotalChapters.collectAsState()
    val totalCh = if (extensionTotalChapters > 0) extensionTotalChapters else manga.totalChapters

    val displayData = detail ?: manga.asDetail()

    // Single source of truth for the displayed chapter count so the header ("N ch.") and the
    // Information card's "Chapters" cell can never disagree. The extension-derived total wins
    // because AniList counts extras/prologues the release actually splits differently.
    val resolvedChapterCount = extensionTotalChapters.takeIf { it > 0 }
        ?: displayData.chapters.takeIf { it > 0 }

    var lastDetailSig by remember { mutableStateOf("") }
    val detailSig = "${detail != null}|${displayData.description != null}|${displayData.genres.size}|${displayData.tags.size}|" +
        "${displayData.characters?.nodes?.size ?: 0}|${displayData.staff?.edges?.size ?: 0}|${displayData.relations.size}|" +
        "${displayData.recommendations.size}|${displayData.popularity}|${displayData.favourites}|${displayData.year}|" +
        "${displayData.format}|${displayData.source}|${displayData.volumes}|${displayData.chapters}|${displayData.status}|$isLoading"
    if (lastDetailSig != detailSig) {
        lastDetailSig = detailSig
    }

    val statusDisplay = when (displayData.status) {
        "RELEASING" -> stringResource(R.string.manga_detail_status_publishing)
        "FINISHED" -> stringResource(R.string.manga_detail_status_finished)
        "NOT_YET_RELEASED" -> stringResource(R.string.manga_detail_status_not_yet_published)
        "CANCELLED" -> stringResource(R.string.manga_detail_status_cancelled)
        "HIATUS" -> stringResource(R.string.manga_detail_status_hiatus)
        else -> displayData.status ?: stringResource(R.string.manga_detail_unknown)
    }

    // The Information card's Status cell shows the user's tracking status, not the series
    // publication status (that one already has the header badge). Driven by the live tracking
    // state so it updates the instant the status dialog saves.
    val userStatusDisplay = statusToCheck?.let { MangaStatusLabels[it] ?: it }

    val formatDisplay = when (displayData.format) {
        "MANGA" -> stringResource(R.string.manga_detail_format_manga)
        "NOVEL" -> stringResource(R.string.manga_detail_format_light_novel)
        "ONE_SHOT" -> stringResource(R.string.manga_detail_format_one_shot)
        "DOUJIN" -> stringResource(R.string.manga_detail_format_doujin)
        "MANHWA" -> stringResource(R.string.manga_detail_format_manhwa)
        "MANHUA" -> stringResource(R.string.manga_detail_format_manhua)
        "OEL" -> stringResource(R.string.manga_detail_format_oel_manga)
        else -> displayData.format ?: stringResource(R.string.manga_detail_unknown)
    }

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

    // Shared drag-to-dismiss behavior — see ui/components/SheetDismissGestures.kt
    val nestedScrollConnection = com.blissless.tensei.ui.components.rememberSheetDismissScrollConnection(
        offsetY = offsetY,
        isAtTop = { isAtTop },
        dismissThreshold = dismissThreshold,
        onDismiss = { dismissWithAnimation() },
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, (slideOffset.value + dismissSlideOffset.value).roundToInt()) }
                    .graphicsLayer { this.alpha = alpha }
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .background(if (isOled) Color.Black else MaterialTheme.colorScheme.background)
                    .nestedScroll(nestedScrollConnection)
                    // Release-settle: a low-velocity release never produces a fling, so onPreFling
                    // is never invoked and the sheet would stay stuck half-translated; and a fling
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
                            // Defer the settle check to the composition scope: the gesture scope is
                            // a restricted coroutine scope (can't delay() from inside it), and this
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
                // Banner
                if (!displayData.banner.isNullOrEmpty() || displayData.cover.isNotEmpty()) {
                val bannerImage = displayData.banner ?: displayData.cover
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { fullscreenImageUrl = bannerImage }
                ) {
                    AsyncImage(
                        model = bannerImage, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = 0.4f }
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent, Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                    )
                }
            }

            // Share button
            IconButton(
                onClick = {
                    val shareText = buildString {
                        append(displayData.title)
                        append("\n\n")
                        append("https://anilist.co/manga/${displayData.id}")
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .zIndex(10f)
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.manga_detail_share), tint = Color.White, modifier = Modifier.size(24.dp))
            }

            // Close button
            IconButton(
                onClick = { if (stackDepth > 2) onCloseAll() else dismissWithAnimation() },
                modifier = Modifier
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, start = 16.dp)
                    .align(Alignment.TopStart)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .zIndex(10f)
            ) {
                Icon(
                    if (stackDepth > 2) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (stackDepth > 2) stringResource(R.string.manga_detail_close) else stringResource(R.string.common_back),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Top bar pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)).zIndex(5f)
            )

            if (isLoading) {
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
            val density = LocalDensity.current

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 140.dp + statusBarsPadding.calculateTopPadding(),
                    bottom = 32.dp + navigationBarsPadding.calculateBottomPadding()
                )
            ) {
                // Cover + Title section
item {
                MangaDetailHeaderItem(
                    alpha = alpha,
                    displayData = displayData,
                    formatDisplay = formatDisplay,
                    statusDisplay = statusDisplay,
                    isLoading = isLoading,
                    resolvedChapterCount = resolvedChapterCount,
                    onShowFullscreenImage = { fullscreenImageUrl = it },
                )
            }

                // Chapter Button (replaces WatchNowButton)
item {
                MangaDetailProgressItem(
                    alpha = alpha,
                    liveProgress = liveProgress,
                    onStartReader = onStartReader,
                )
            }

                // Status Card (Add to List / Change + Favorite)
item {
                MangaDetailStatusItem(
                    manga = manga,
                    alpha = alpha,
                    isMangaFavorited = isMangaFavorited,
                    liveScore = liveScore,
                    onFavoriteClick = onFavoriteClick,
                    statusProgress = statusProgress,
                    statusToCheck = statusToCheck,
                    totalCh = totalCh,
                    onShowStatusDialogChange = { showStatusDialog = it },
                    onShowRatingSheetChange = { showRatingSheet = it },
                )
            }

                // Info Card (manga specs)
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    MangaInfoCard(
                        displayData = displayData,
                        userStatusDisplay = userStatusDisplay,
                        chaptersCount = resolvedChapterCount,
                        isLoading = isLoading,
                    )
                }

                // Genres
                if (displayData.genres.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        GenresCard(genres = displayData.genres)
                    }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(headerWidthFraction = 0.35f, bodyLines = 1)
                    }
                }

                // Tags
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
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(headerWidthFraction = 0.5f, bodyLines = 2)
                    }
                }

                // Synopsis
                if (!displayData.description.isNullOrEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SynopsisCard(
                            description = displayData.description,
                            showFullDescription = showFullDescription,
                            onToggleShowFull = { showFullDescription = !showFullDescription },
                        )
                    }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(bodyLines = 4)
                    }
                }

                // Relations
                val relations = displayData.relations
                if (relations.isNotEmpty()) {
item {
                MangaDetailRelationsItem(
                    relations = relations,
                    alpha = alpha,
                    onRelationClick = onRelationClick,
                    onViewAllRelations = onViewAllRelations,
                    preferEnglishTitles = preferEnglishTitles,
                )
            }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 5, itemWidth = 110.dp)
                    }
                }

                // Recommendations
                val recommendations = displayData.recommendations
                if (recommendations.isNotEmpty()) {
item {
                MangaDetailRecommendationsItem(
                    recommendations = recommendations,
                    alpha = alpha,
                    density = density,
                    slideOffset = slideOffset,
                    onViewAllRecommendations = onViewAllRecommendations,
                    navigateToMangaDetail = navigateToMangaDetail,
                    preferEnglishTitles = preferEnglishTitles,
                )
            }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 5, itemWidth = 110.dp)
                    }
                }

                // Characters
                val characters = displayData.characters?.nodes
                if (!characters.isNullOrEmpty()) {
item {
                MangaDetailCharactersItem(
                    characters = characters,
                    alpha = alpha,
                    density = density,
                    slideOffset = slideOffset,
                    onCharacterClick = onCharacterClick,
                    onViewAllCharacters = onViewAllCharacters,
                )
            }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 6, itemWidth = 80.dp, posterAspectRatio = 1f)
                    }
                }

                // Staff
                val staff = displayData.staff?.edges
                if (!staff.isNullOrEmpty()) {
item {
                MangaDetailStaffItem(
                    staff = staff,
                    alpha = alpha,
                    density = density,
                    slideOffset = slideOffset,
                    onStaffClick = onStaffClick,
                    onViewAllStaff = onViewAllStaff,
                )
            }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailRailSkeleton(itemCount = 6, itemWidth = 80.dp, posterAspectRatio = 1f)
                    }
                }

                // Rankings
                val rankings = displayData.rankings
                if (rankings.isNotEmpty()) {
item {
                MangaDetailRankingsItem(
                    rankings = rankings,
                    alpha = alpha,
                )
            }
                } else if (isLoading) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        DetailTextCardSkeleton(headerWidthFraction = 0.4f, bodyLines = 4)
                    }
                }

                // Synonyms
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedTagForDescription != null) {
        val tag: TagData = selectedTagForDescription!!
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { selectedTagForDescription = null },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(tag.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                tag.rank?.let { rank ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.manga_detail_rank_percent, rank), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))
                val description = tag.description ?: stringResource(R.string.manga_detail_no_description)
                Text(anilistAnnotated(description, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primary), style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
            }
        }
    }

    if (fullscreenImageUrl != null) {
        Dialog(onDismissRequest = { fullscreenImageUrl = null }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { fullscreenImageUrl = null } })
                AsyncImage(model = fullscreenImageUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(16.dp))
            }
        }
    }

    if (showStatusDialog) {
        MangaStatusDialog(
            title = manga.title,
            titleEnglish = manga.titleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            coverUrl = manga.cover,
            currentStatus = statusToCheck,
            currentProgress = statusProgress,
            totalChapters = totalCh,
            isOled = isOled,
            onUpdate = { status, progress ->
                onUpdateStatus(status, progress)
                if (progress != null) {
                    onUpdateProgress(progress)
                    viewModel.updateMangaProgress(manga.id, progress.toFloat())
                }
                showStatusDialog = false
            },
            onRemove = {
                onRemove()
                showStatusDialog = false
            },
            onDismiss = { showStatusDialog = false }
        )
    }

    if (showRemoveFavoriteDialog) {
        val removeFavoriteTitle = if (preferEnglishTitles && !manga.titleEnglish.isNullOrEmpty()) manga.titleEnglish else manga.title
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRemoveFavoriteDialog = false },
            title = { Text(stringResource(R.string.manga_detail_remove_favorite_title)) },
            text = { Text(stringResource(R.string.manga_detail_remove_favorite_message, removeFavoriteTitle)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showRemoveFavoriteDialog = false
                    toggleFavorite()
                }) { Text(stringResource(R.string.manga_detail_remove)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRemoveFavoriteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showRatingSheet) {
        MangaRatingSheet(
            title = if (preferEnglishTitles && !manga.titleEnglish.isNullOrEmpty()) manga.titleEnglish else manga.title,
            coverUrl = manga.cover,
            currentScore = liveScore,
            isOled = isOled,
            onDismiss = { showRatingSheet = false },
            onScoreSaved = { score ->
                if (score != null) {
                    viewModel.updateMangaScore(manga.id, score, malId = manga.malId)
                }
                showRatingSheet = false
            }
        )
    }
}

@Composable
private fun MangaInfoCard(
    displayData: MangaDetail,
    userStatusDisplay: String?,
    chaptersCount: Int?,
    isLoading: Boolean = false,
) {
    val locale = LocaleDate.localeOf(LocalContext.current)
    val popularityLabel = stringResource(R.string.manga_detail_popularity)
    val scoreLabel = stringResource(R.string.manga_detail_score)
    val favoritesLabel = stringResource(R.string.manga_detail_favorites)
    val volumesLabel = stringResource(R.string.manga_detail_volumes)
    val formatLabel = stringResource(R.string.manga_detail_format)
    val statusLabel = stringResource(R.string.manga_detail_status)
    val sourceLabel = stringResource(R.string.manga_detail_source)
    val authorLabel = stringResource(R.string.manga_detail_author)
    val chaptersLabel = stringResource(R.string.manga_detail_chapters)
    val startedLabel = stringResource(R.string.manga_detail_started)
    val endedLabel = stringResource(R.string.manga_detail_ended)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.manga_detail_information), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.manga_detail_overview_details), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Hero stats: Popularity / Score / Favorites
            val heroStats = buildList {
                displayData.popularity?.takeIf { it > 0 }?.let { add(popularityLabel to formatNumber(it)) }
                displayData.averageScore?.takeIf { it > 0 }?.let { add(scoreLabel to String.format(Locale.US, "%.1f", it / 10.0)) }
                displayData.favourites?.takeIf { it > 0 }?.let { add(favoritesLabel to formatNumber(it)) }
            }

            if (heroStats.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    heroStats.forEachIndexed { index, (label, value) ->
                        val accent = when (label) {
                            scoreLabel -> tenseiColors.rating
                            volumesLabel -> MaterialTheme.colorScheme.tertiary
                            popularityLabel -> MaterialTheme.colorScheme.primary
                            favoritesLabel -> Color(0xFFEC4899)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val icon = when (label) {
                            scoreLabel -> Icons.Default.Star
                            favoritesLabel -> Icons.Default.Favorite
                            else -> null
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                icon?.let {
                                    Icon(it, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                        }
                        if (index < heroStats.lastIndex) {
                            Box(modifier = Modifier.width(1.dp).height(28.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

if (heroStats.isEmpty() && isLoading) {
                    DetailStatStripSkeleton()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bento spec grid
                val specs = buildList {
                displayData.format?.let {
                    add(SpecEntry(label = formatLabel, value = it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }, icon = Icons.Default.Category))
                }
                userStatusDisplay?.let {
                    add(SpecEntry(label = statusLabel, value = it, icon = Icons.Default.PlayArrow))
                }
                displayData.source?.let {
                    add(SpecEntry(label = sourceLabel, value = it.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }, icon = Icons.Default.Description))
                }
                displayData.authorNames()?.let {
                    // Authors get a full row â€” author lists are long and get
                    // squeezed into a truncated half-width cell otherwise.
                    add(SpecEntry(label = authorLabel, value = it, icon = Icons.Default.Person, fullSpan = true))
                }
                if (chaptersCount != null) {
                    add(SpecEntry(label = chaptersLabel, value = chaptersCount.toString()))
                }
                displayData.volumes?.let {
                    add(SpecEntry(label = volumesLabel, value = it.toString()))
                }
                val startedDate = displayData.startDate
                if (startedDate != null) {
                    add(SpecEntry(label = startedLabel, value = formatDate(startedDate, locale)))
                } else displayData.year?.let {
                    add(SpecEntry(label = startedLabel, value = it.toString()))
                }
                if (displayData.status != "RELEASING" && displayData.status != "NOT_YET_RELEASED") {
                    val endedDate = displayData.endDate
                    if (endedDate != null) {
                        add(SpecEntry(label = endedLabel, value = formatDate(endedDate, locale)))
                    } else displayData.endYear?.let {
                        add(SpecEntry(label = endedLabel, value = it.toString()))
                    }
                }
            }

if (specs.isEmpty() && isLoading) {
                    DetailSpecGridSkeleton()
                }

                var i = 0
                while (i < specs.size) {
                val current = specs[i]
                val next = specs.getOrNull(i + 1)
                if (next != null && !current.fullSpan) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BentoSpecCell(current, modifier = Modifier.weight(1f))
                        BentoSpecCell(next, modifier = Modifier.weight(1f))
                    }
                    i += 2
                } else {
                    BentoSpecCell(current, modifier = Modifier.fillMaxWidth())
                    i += 1
                }
                if (i < specs.size) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

private data class SpecEntry(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val fullSpan: Boolean = false
)

@Composable
private fun BentoSpecCell(spec: SpecEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .padding(14.dp)
    ) {
        Text(spec.label.uppercase(), style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            spec.icon?.let { ic ->
                Icon(ic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(spec.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun formatNumber(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000 -> "${n / 1_000}K"
    else -> n.toString()
}

private fun MangaMedia.asDetail(): MangaDetail = MangaDetail(
    id = id,
    title = title,
    titleEnglish = titleEnglish,
    cover = cover,
    banner = banner,
    chapters = totalChapters,
    volumes = totalVolumes,
    status = status,
    averageScore = averageScore,
    genres = genres,
    year = year,
    format = format,
    siteUrl = siteUrl,
    malId = malId
)

private fun MangaDetail.authorNames(): String? {
    val edges = staff?.edges ?: return null
    val preferred = edges.filter {
        it.role?.contains("Story", ignoreCase = true) == true ||
            it.role?.contains("Art", ignoreCase = true) == true
    }
    val chosen = if (preferred.isNotEmpty()) preferred else edges
    val names = chosen.mapNotNull { it.node?.name?.full }.distinct()
    return if (names.isEmpty()) null else names.take(3).joinToString(", ")
}
