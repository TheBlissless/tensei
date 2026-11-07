package com.blissless.tensei.ui.screens.manga

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.Surface
import android.view.View
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.countDistinctChapters
import com.blissless.tensei.viewmodel.clearMangaChapterImagesCache
import com.blissless.tensei.viewmodel.cancelChapterImageLoad
import com.blissless.tensei.viewmodel.cancelMangaChapterLoad
import com.blissless.tensei.viewmodel.fetchMangaDetail
import com.blissless.tensei.viewmodel.flushMangaSync
import com.blissless.tensei.viewmodel.hasLoadedMangaChapters
import com.blissless.tensei.viewmodel.isLoadingMangaChapters
import com.blissless.tensei.viewmodel.loadChapterImages
import com.blissless.tensei.viewmodel.loadMangaChapters
import com.blissless.tensei.viewmodel.mangaChapterImages
import com.blissless.tensei.viewmodel.mangaChapterImagesError
import com.blissless.tensei.viewmodel.mangaImageHeaders
import com.blissless.tensei.viewmodel.mangaChapters
import com.blissless.tensei.viewmodel.mangaTrackManager
import com.blissless.tensei.viewmodel.onMangaScrollProgress
import com.blissless.tensei.viewmodel.prefetchMangaChapterImages
import com.blissless.tensei.viewmodel.refreshMangaTracking
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.setMangaAutoAdvance
import com.blissless.tensei.viewmodel.setMangaFullscreen
import com.blissless.tensei.viewmodel.setMangaPageIndicator
import com.blissless.tensei.viewmodel.setMangaReaderMode
import com.blissless.tensei.viewmodel.setMangaLockRotation
import com.blissless.tensei.viewmodel.updateMangaChapterPages
import com.blissless.tensei.viewmodel.updateMangaScrollProgress
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.Headers

// The two reader implementations (continuous vertical scroll and paged)
// plus the mode toggle UI.



@Composable
internal fun ReaderModeSegmentedToggle(
    currentMode: ReaderMode,
    onSelect: (ReaderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(2.dp)
    ) {
        ReaderModeToggleItem(
            icon = Icons.Default.ViewAgenda,
            contentDescription = stringResource(R.string.reader_mode_vertical),
            selected = currentMode == ReaderMode.VERTICAL_SCROLL,
            onClick = { onSelect(ReaderMode.VERTICAL_SCROLL) }
        )
        ReaderModeToggleItem(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.reader_mode_ltr),
            selected = currentMode == ReaderMode.LEFT_TO_RIGHT,
            onClick = { onSelect(ReaderMode.LEFT_TO_RIGHT) }
        )
        ReaderModeToggleItem(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.reader_mode_rtl),
            selected = currentMode == ReaderMode.RIGHT_TO_LEFT,
            onClick = { onSelect(ReaderMode.RIGHT_TO_LEFT) }
        )
    }
}


@Composable
private fun ReaderModeToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            // The toggle sits in the dark bottom bar, so the selected item uses a light
            // accent (the theme's primary is dark in the light app theme).
            .background(if (selected) ReaderAccent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (selected) Color(0xFF062033) else ReaderTextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}


@Composable
internal fun VerticalScrollReader(
    chapterImages: List<String>,
    chapterImagesError: String?,
    chapter: MangaChapter?,
    headers: Headers? = null,
    totalChapters: Int,
    currentIndex: Int,
    scrollProgress: Float,
    restoreProgress: Float = -1f,
    showControls: Boolean,
    onScrollProgress: (Float) -> Unit,
    onCurrentPage: (Int) -> Unit = {},
    onToggleControls: () -> Unit,
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var restored by remember { mutableStateOf(false) }

    LaunchedEffect(chapter) {
        listState.scrollToItem(0)
        restored = false
    }

    // Restore the saved reading position once images are available (oni-style resume).
    // Only applies to the chapter that was last being read (see caller's restoreProgress).
    LaunchedEffect(chapter, chapterImages) {
        if (!restored && restoreProgress > 0f && chapterImages.isNotEmpty()) {
            delay(150)
            // Vertical scroll stores scrollProgress = currentScroll / maxScroll, where
            // currentScroll is the TOP of the viewport (firstVisibleItemIndex-based) and
            // maxScroll = total * itemSize - viewportHeight. When 1 page fills the screen
            // (the common manga case), maxScroll = (total - 1) * itemSize, so the correct
            // inverse is scrollProgress * (total - 1) — NOT scrollProgress * total.
            //
            // The +0.5f rounds to the NEAREST page index, which corresponds to the
            // best-overlap page (the one the reader was actually displaying when the user
            // exited). Without it, a scrollProgress of 2.6/74 (page 4 best-overlap in a
            // 75-page chapter) truncates to index 2 (page 3) instead of index 3 (page 4).
            // This mirrors the formula used by HomeScreen's currentPage computation.
            val targetIndex = ((restoreProgress * (chapterImages.size - 1)) + 0.5f)
                .toInt()
                .coerceIn(0, chapterImages.size - 1)
            listState.scrollToItem(targetIndex)
            restored = true
        }
    }

    // Reading progress (0..1) — pixel-based scroll fraction, matching oni.
    // Writes are done in the COLLECT lambda (outside snapshotFlow's read-only
    // snapshot); writing state inside the snapshotFlow block itself would throw
    // "Cannot modify a state object in a read-only snapshot".
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@snapshotFlow 0f
            if (totalItems <= 1) return@snapshotFlow 1f

            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull() ?: return@snapshotFlow 0f
            val itemSize = firstVisibleItem.size
            val viewportHeight = layoutInfo.viewportSize.height

            val currentScroll = listState.firstVisibleItemIndex.toFloat() * itemSize.toFloat() + listState.firstVisibleItemScrollOffset.toFloat()
            val maxScroll = (totalItems.toFloat() * itemSize.toFloat() - viewportHeight.toFloat()).coerceAtLeast(0f)
            if (maxScroll <= 0f) 0f else (currentScroll / maxScroll).coerceIn(0f, 1f)
        }.collect { progress: Float ->
            onScrollProgress(progress)
        }
    }

    // Track the current PAGE (the one the user is actually looking at) by
    // watching the LazyColumn's layout, using the item with the largest
    // viewport overlap (oni's approach). Runs in the collect lambda so state
    // writes are legal.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@collect
                val best = visibleItems.maxByOrNull { item ->
                    val overlapTop = maxOf(item.offset, layoutInfo.viewportStartOffset)
                    val overlapBottom = minOf(item.offset + item.size, layoutInfo.viewportEndOffset)
                    maxOf(0, overlapBottom - overlapTop)
                }
                if (best != null) {
                    onCurrentPage(best.index)
                }
            }
    }

    // Error state — show error message + retry button instead of empty list
    if (chapterImagesError != null) {
        ChapterLoadErrorView(
            message = chapterImagesError,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    // Loading state — chapter just started loading
    if (chapterImages.isEmpty()) {
        ChapterLoadingView(modifier = Modifier.fillMaxSize(), onCancel = onCancel)
        return
    }

    // Vertical scroll mode — one zoomable page per row (7:10 box, edge-to-edge
    // width), matching oni's MihonZoomableImage. Pinch/double-tap zoom works per
    // page; single-finger drag at 1x passes through so the LazyColumn scrolls.
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(chapterImages, key = { index, _ -> "page_$index" }) { index, imageUrl ->
            MihonZoomableImage(
                imageUrl = imageUrl,
                contentDescription = stringResource(R.string.reader_page_number, index + 1),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(7f / 10f),
                fillWidth = true,
                headers = headers,
                onSingleTap = { onToggleControls() }
            )
        }
    }
}


@Composable
internal fun PagedMangaReader(
    chapterImages: List<String>,
    chapterImagesError: String?,
    mode: ReaderMode,
    headers: Headers? = null,
    initialPage: Int,
    restorePage: Int = -1,
    onToggleControls: () -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onPageChanged: (page: Int, total: Int) -> Unit = { _, _ -> }
) {
    // Error state — show error message + retry button
    if (chapterImagesError != null) {
        ChapterLoadErrorView(
            message = chapterImagesError,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    if (chapterImages.isEmpty()) {
        ChapterLoadingView(modifier = Modifier.fillMaxSize(), onCancel = onCancel)
        return
    }

    val isRtl = mode == ReaderMode.RIGHT_TO_LEFT
    // The pager is torn down and recreated whenever a chapter's images change
    // (chapterImages is cleared to null during loading), so `initialPage` here
    // both resumes the current page on mode switches AND starts new chapters at
    // page 0 — matching oni.
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, chapterImages.lastIndex),
        pageCount = { chapterImages.size }
    )
    val scope = rememberCoroutineScope()

    // Restore to a specific page after initial composition (for "Continue Reading" in horizontal modes).
    // The pager is already created by the time this fires, so scrollToPage jumps to the correct position.
    LaunchedEffect(restorePage, chapterImages.size) {
        if (restorePage in 0 until chapterImages.size && restorePage != pagerState.currentPage) {
            pagerState.scrollToPage(restorePage)
        }
    }

    // Report the current page upward so the reader can show a page indicator
    // and feed scroll progress back to the ViewModel for AniList sync. Fires
    // once per page transition via distinctUntilChanged, matching oni.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> onPageChanged(page, chapterImages.size) }
    }

    // Track the current page's zoom state. While zoomed in, the pager's swipe
    // is disabled so the user can pan without flipping pages.
    var currentPageZoomed by remember { mutableStateOf(false) }

    // Per-page double-tap zoom trigger. Incremented when a double-tap is
    // detected at the pager level (before HorizontalPager consumes the event).
    // MihonZoomableImage watches this and toggles zoom accordingly.
    val doubleTapTriggers = remember { mutableStateMapOf<Int, Int>() }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = isRtl,
            userScrollEnabled = !currentPageZoomed,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                // Detect double-taps at the pager level using the Initial pass,
                // which runs BEFORE HorizontalPager's own gesture detection.
                .pointerInput(pagerState.currentPage) {
                    awaitEachGesture {
                        val down1 = awaitFirstDown(requireUnconsumed = false)
                        val t1 = down1.uptimeMillis
                        waitForUpOrCancellation() ?: return@awaitEachGesture
                        val down2 = awaitFirstDown(requireUnconsumed = false)
                        val t2 = down2.uptimeMillis
                        if (t2 - t1 < 300) {
                            val page = pagerState.currentPage
                            doubleTapTriggers[page] = (doubleTapTriggers[page] ?: 0) + 1
                            down2.consume()
                        }
                    }
                }
        ) { pageIndex ->
            // Reset the zoom flag when the user lands on a new page: the new
            // page starts at 1x, so the pager should be swipeable until pinched.
            var isZoomed by remember(pageIndex) { mutableStateOf(false) }
            var pageLayoutSize by remember(pageIndex) { mutableStateOf(Size.Zero) }

            LaunchedEffect(pagerState.currentPage, isZoomed) {
                currentPageZoomed = if (pagerState.currentPage == pageIndex) isZoomed else false
            }

            MihonZoomableImage(
                imageUrl = chapterImages[pageIndex],
                contentDescription = stringResource(R.string.reader_page_number, pageIndex + 1),
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { pageLayoutSize = it.toSize() },
                fillWidth = false,
                headers = headers,
                doubleTapTrigger = doubleTapTriggers[pageIndex] ?: 0,
                onSingleTap = { tap ->
                    if (isZoomed) return@MihonZoomableImage

                    val width = pageLayoutSize.width
                    if (width <= 0f) return@MihonZoomableImage

                    // Middle zone (~40%) toggles the chrome; left/right zones
                    // flip pages (handling chapter boundaries at the edges).
                    val isMiddleZone = tap.x > width * 0.3f && tap.x < width * 0.7f
                    if (isMiddleZone) {
                        onToggleControls()
                        return@MihonZoomableImage
                    }

                    val isPrevZone = if (isRtl) tap.x > width * 0.7f else tap.x < width * 0.3f

                    scope.launch {
                        when {
                            isPrevZone && pagerState.currentPage > 0 ->
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            !isPrevZone && pagerState.currentPage < chapterImages.lastIndex ->
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            !isPrevZone && pagerState.currentPage == chapterImages.lastIndex ->
                                onNextChapter()
                            isPrevZone && pagerState.currentPage == 0 ->
                                onPrevChapter()
                        }
                    }
                },
                onZoomChanged = { zoomed -> isZoomed = zoomed }
            )
        }
    }
}
