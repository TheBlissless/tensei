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
import com.blissless.tensei.viewmodel.isMangaChaptersLoadCancelled
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


@Composable
fun MangaReaderScreen(
    manga: MangaMedia,
    initialChapterIndex: Int,
    viewModel: MainViewModel,
    isOled: Boolean,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    // The reader is hosted in a Dialog, so LocalContext is the dialog's context (a
    // ContextThemeWrapper), NOT an Activity. Walk the wrapper chain to find the real
    // Activity so requestedOrientation changes actually take effect.
    val activity = context.findActivity()
    // The reader is hosted in a Dialog, so LocalView is a view inside the dialog's own
    // window. System-bar control must target THAT window (not the activity's), otherwise
    // hiding the bars has no visible effect. See applyReaderFullscreen.
    val view = LocalView.current
    DisposableEffect(Unit) {
        onDispose {
            // Release any rotation lock applied by the reader so the rest of the app
            // goes back to following the system orientation setting.
            applyReaderRotationLock(activity, false)
            // Restore the system bars if the reader hid them.
            applyReaderFullscreen(view, false)
            // Drop the prefetch cache so it doesn't hold stale image lists in memory.
            viewModel.clearMangaChapterImagesCache()
            // Push any debounced progress/status changes to AniList immediately so chapters
            // read in the last few seconds aren't lost if the app is killed during the debounce.
            viewModel.flushMangaSync()
            // Refresh the home Continue Reading card with the latest scroll progress
            // when leaving the reader mid-chapter.
            viewModel.refreshMangaTracking()
            // Clear Discord Rich Presence when leaving the reader.
            com.blissless.tensei.discord.DiscordRichPresence.clearPresence()
        }
    }
    val chapters by viewModel.mangaChapters.collectAsState()
    val chapterImages by viewModel.mangaChapterImages.collectAsState()
    val chapterImagesError by viewModel.mangaChapterImagesError.collectAsState()
    val chapterImageHeaders by viewModel.mangaImageHeaders.collectAsState()
    var currentChapterIndex by remember { mutableIntStateOf(initialChapterIndex.coerceAtLeast(0)) }
    var readerMode by remember { mutableStateOf(
        when (viewModel.mangaReaderMode.value) {
            "left_to_right" -> ReaderMode.LEFT_TO_RIGHT
            "right_to_left" -> ReaderMode.RIGHT_TO_LEFT
            else -> ReaderMode.VERTICAL_SCROLL
        }
    ) }
    var showControls by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(initialChapterIndex < 0) }
    var scrollProgress by remember { mutableFloatStateOf(0f) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    // True while the "Next Chapter" button is shown (reader settled at the end of a chapter).
    var showNextChapterButton by remember { mutableStateOf(false) }
    // Frozen snapshot of the next chapter while the exit animation plays.
    var cachedNextChapterForButton by remember { mutableStateOf<MangaChapter?>(null) }
    LaunchedEffect(showNextChapterButton, currentChapterIndex) {
        if (showNextChapterButton) {
            cachedNextChapterForButton = chapters.getOrNull(currentChapterIndex + 1)
        }
    }
    // True when the current chapter was opened via the next-chapter button — suppresses restoring
    // the saved (stale) scroll position so the new chapter always opens at the top.
    var suppressResumeRestore by remember { mutableStateOf(false) }
    // Pending resume progress for horizontal reader modes: set in selectChapter() when resuming,
    // consumed after images load to compute the correct initial page index.
    var pendingResumeProgress by remember { mutableFloatStateOf(-1f) }
    // True after a chapter is tapped in the chapter list, while its images are still loading.
    // The list stays open with a loading overlay on top, then dismisses once the images arrive.
    var pendingChapterLoad by remember { mutableStateOf(false) }
    // The chapter index whose load is being awaited (set in selectChapter). -1 when idle.
    var pendingChapterIndex by remember { mutableIntStateOf(-1) }
    // The images/error actually on screen, and which chapter they belong to. Kept unchanged
    // while a new chapter loads so the current screen (chapter list or current chapter) stays
    // visible behind the loading overlay; it swaps to the new chapter only once its images are
    // ready (or it failed).
    var displayedImages by remember { mutableStateOf<List<String>?>(null) }
    var displayedImagesError by remember { mutableStateOf<String?>(null) }
    var displayedChapterIndex by remember { mutableIntStateOf(-1) }
    // Chapter whose image load the user cancelled. The load effect below re-runs whenever the
    // load state changes, so the cancelled chapter is remembered to block that automatic
    // restart; it is cleared when the user picks or retries a chapter.
    var cancelledChapterId by remember { mutableStateOf<String?>(null) }
    val loadCancelledMessage = stringResource(R.string.reader_load_cancelled)
    // Read marks come from the local track's FLOAT progress (314.1 after that subchapter
    // was read) instead of the model's truncated Int: 314.1 must show as read while the
    // 314.2 after it must not. Whichever source is further along wins, so a model that
    // only knows whole chapters never hides what the track already recorded.
    val readIndices = remember(chapters, manga.progress, manga.id) {
        val readUpTo = maxOf(
            viewModel.mangaTrackManager?.getTrack(manga.id)?.progress ?: 0f,
            manga.progress.toFloat()
        )
        mutableStateOf(
            chapters.mapIndexedNotNull { index, ch ->
                if (ch.chapterNumber > 0f && ch.chapterNumber <= readUpTo) index else null
            }.toSet()
        )
    }
    val scope = rememberCoroutineScope()

    val currentChapter = chapters.getOrNull(currentChapterIndex)
    val isFirstChapter = currentChapterIndex == 0
    val isLastChapter = currentChapterIndex >= chapters.lastIndex

    // Force-persist the latest scrollProgress + chapter on reader exit. The throttled
    // writer inside updateMangaScrollProgress can otherwise leave the persisted
    // scrollProgress at a stale page (snapshotFlow only emits on change, so a steady
    // position never re-fires the throttled write). rememberUpdatedState captures the
    // latest values so onDispose reads them at exit time, not at composition time.
    val latestScrollProgress by rememberUpdatedState(scrollProgress)
    val latestChapter by rememberUpdatedState(currentChapter)
    DisposableEffect(manga.id) {
        onDispose {
            val chapter = latestChapter
            if (chapter != null && latestScrollProgress > 0f) {
                viewModel.updateMangaScrollProgress(
                    mangaId = manga.id,
                    scrollProgress = latestScrollProgress,
                    mangaTitle = manga.title,
                    mangaCover = manga.cover,
                    chapter = chapter,
                    force = true,
                )
            }
        }
    }
    // Index of the chapter the saved scroll position belongs to. Progress races ahead once the
    // sync threshold is crossed (a completed chapter), while the user may still be inside it —
    // so resume must target the scroll's chapter, not progress+1 (the "Ch. 21 · Page 26/27" bug).
    val scrollChapterIndex = remember(chapters, manga.scrollChapterId) {
        if (manga.scrollChapterId != null) chapters.indexOfFirst { it.chapterId == manga.scrollChapterId } else -1
    }
    // Entry redirect: Continue Reading hands us index == progress (the next unread chapter once
    // the previous crossed the sync threshold). If the saved scroll belongs to a different
    // chapter, open THAT one instead (restoring the true reading position) before anything else
    // is put on screen. Only applies when the reader is opened directly into a chapter (not into
    // the chapter list), and runs once per reader session.
    var entryRedirectDone by remember { mutableStateOf(initialChapterIndex < 0) }
    LaunchedEffect(chapters, scrollChapterIndex, showChapterList) {
        if (entryRedirectDone || showChapterList || chapters.isEmpty()) return@LaunchedEffect
        entryRedirectDone = true
        if (scrollChapterIndex >= 0 && currentChapterIndex != scrollChapterIndex) {
            currentChapterIndex = scrollChapterIndex
            currentPageIndex = 0
            scrollProgress = 0f
            pendingResumeProgress = manga.scrollProgress
            suppressResumeRestore = false
            pendingChapterLoad = false
            pendingChapterIndex = -1
        }
    }
    val useDataSaver = viewModel.mangaDataSaver.value
    val isLoadingChapters by viewModel.isLoadingMangaChapters.collectAsState()
    val hasLoadedChapters by viewModel.hasLoadedMangaChapters.collectAsState()
    val isChapterLoadCancelled by viewModel.isMangaChaptersLoadCancelled.collectAsState()
    val selectedExtension by viewModel.selectedExtensionAuthority.collectAsState()
    val mangaSourceMethod by viewModel.mangaSourceMethod.collectAsState()
    val defaultExternalMangaExtension by viewModel.defaultExternalMangaExtension.collectAsState()
    val hasMangaSource = selectedExtension != null ||
        (mangaSourceMethod == "external" && !defaultExternalMangaExtension.isNullOrBlank())
    val showPageIndicator by viewModel.mangaPageIndicator.collectAsState()
    val syncThreshold by viewModel.mangaSyncThreshold.collectAsState()
    val lockRotation by viewModel.mangaLockRotation.collectAsState()
    val fullscreen by viewModel.mangaFullscreen.collectAsState()
    val nextChapterButton by viewModel.mangaAutoAdvance.collectAsState()

    // Lock/unlock the screen rotation to the current orientation while reading.
    // Runs on entry (with the persisted setting) and every time the toggle changes.
    LaunchedEffect(lockRotation) {
        applyReaderRotationLock(activity, lockRotation)
    }

    // Hide/show the system bars (status bar + navigation) on the DIALOG's window while
    // reading, matching the anime player's fullscreen behavior. The dialog window may not
    // be attached yet at first composition, so re-apply once shortly after.
    // Fullscreen only applies while actually reading a chapter — the chapter selection
    // screen keeps the system bars visible.
    LaunchedEffect(fullscreen, showChapterList) {
        val effectiveFullscreen = fullscreen && !showChapterList
        applyReaderFullscreen(view, effectiveFullscreen)
        delay(250)
        applyReaderFullscreen(view, effectiveFullscreen)
    }

    // Prefetch the NEXT chapter's image list once the reader nears the end of the current
    // chapter, so the next-chapter button (and manual next-chapter) switches instantly instead
    // of waiting on a scrape round-trip.
    LaunchedEffect(currentPageIndex, scrollProgress, currentChapterIndex, chapterImages, readerMode) {
        val images = chapterImages ?: return@LaunchedEffect
        if (images.isEmpty()) return@LaunchedEffect
        val nearEnd = if (readerMode == ReaderMode.VERTICAL_SCROLL) {
            scrollProgress >= 0.9f
        } else {
            currentPageIndex >= images.lastIndex - 1
        }
        if (nearEnd) {
            val next = chapters.getOrNull(currentChapterIndex + 1) ?: return@LaunchedEffect
            viewModel.prefetchMangaChapterImages(
                next,
                mangaTitle = manga.title,
                mangaId = manga.id
            )
        }
    }

    LaunchedEffect(currentChapter, showChapterList, pendingChapterLoad) {
        if (!showChapterList || pendingChapterLoad) {
            currentChapter?.let { chapter ->
                // A cancelled load stays cancelled until the user picks or retries a chapter.
                if (cancelledChapterId == chapter.chapterId) return@let
                viewModel.loadChapterImages(
                    chapterId = chapter.chapterId,
                    useDataSaver = useDataSaver,
                    mangaTitle = manga.title,
                    chapterTitle = chapter.title,
                    mangaId = manga.id
                )
            }
        }
    }

    // Once the chapter images finish loading (or fail), put them on screen. The VM cancels any
    // earlier chapter load when a new one starts, so the settled value is always for the
    // currently-selected chapter. During the load the PREVIOUS screen (chapter list or current
    // chapter) stays visible behind the loading overlay instead of flashing a blank loading view.
    LaunchedEffect(chapterImages, chapterImagesError) {
        if (chapterImages != null || chapterImagesError != null) {
            displayedImages = chapterImages
            displayedImagesError = chapterImagesError
            displayedChapterIndex = currentChapterIndex
            if (pendingResumeProgress < 0f && manga.scrollProgress > 0f &&
                (currentChapterIndex == scrollChapterIndex || (scrollChapterIndex < 0 && currentChapterIndex == manga.progress))
            ) {
                pendingResumeProgress = manga.scrollProgress
            }
            // Update Discord Rich Presence with current manga/chapter info
            val chapter = currentChapter
            if (chapterImages != null && chapter != null) {
                val drm = viewModel.discordRichPresence.value
                if (drm) {
                    com.blissless.tensei.discord.DiscordRichPresence.connect()
                    com.blissless.tensei.discord.DiscordRichPresence.setMangaPresence(
                        mangaTitle = manga.title,
                        chapterLabel = chapter.title.ifEmpty { chapter.chapterId },
                    )
                }
            }
        }
    }

    // If a chapter load was started from the chapter list (or a chapter transition), dismiss the
    // loading overlay once the NEW chapter's images are on screen, and close the list so the
    // reader takes over. Guarded by displayedChapterIndex == pendingChapterIndex so stale
    // displayed images from a previously-read chapter never close the list prematurely.
    LaunchedEffect(displayedImages, displayedImagesError, displayedChapterIndex, pendingChapterLoad, pendingChapterIndex) {
        if (pendingChapterLoad &&
            displayedChapterIndex == pendingChapterIndex &&
            (displayedImages != null || displayedImagesError != null)
        ) {
            pendingChapterLoad = false
            pendingChapterIndex = -1
            showChapterList = false
        }
    }

    // Load chapters when the list is empty (needed both for the chapter list view and for
    // direct reading), AND refetch the fresh extension list every time the chapter selection
    // screen opens (showChapterList -> true) so it always reflects the extension's current
    // chapters. Skipped while no manga extension is selected: without a source there's no
    // real chapter list, and the synthetic fallback produces wrong chapter numbers for
    // releasing manga.
    LaunchedEffect(manga.id, hasMangaSource, showChapterList) {
        val shouldLoad = chapters.isEmpty() || showChapterList
        if (shouldLoad && hasMangaSource) {
            runCatching { viewModel.fetchMangaDetail(manga.id) }
            viewModel.loadMangaChapters(manga.id, manga.title)
        }
    }

    // Persist the page count of the loaded chapter so home's Continue Reading card can show
    // "pages left" for the manga being read.
    LaunchedEffect(chapterImages) {
        val images = chapterImages
        if (images != null && images.isNotEmpty()) {
            viewModel.updateMangaChapterPages(manga.id, images.size)
        }
    }

    // When a chapter is opened that isn't the Continue-Reading resume target (index == progress
    // OR the saved scroll's chapter), clear the saved scroll position so backing out of a
    // merely-opened chapter never leaves a stale Continue Reading card (the page count is set
    // on load, but scrollProgress 0 means the card won't show). Covers direct opens (Read Now /
    // home card) that skip selectChapter. Opening the scroll's own chapter must NOT clear it.
    LaunchedEffect(currentChapterIndex) {
        val isScrollChapterOpened = scrollChapterIndex >= 0 && currentChapterIndex == scrollChapterIndex
        if (currentChapterIndex != manga.progress && !isScrollChapterOpened) {
            viewModel.updateMangaScrollProgress(manga.id, 0f)
        }
    }

    // Handle the system back button — if a chapter is open, go back to the chapter list
    // (which stays open in the background); back on the chapter list closes the reader.
    fun handleReaderBack() {
        if (!showChapterList && chapters.isNotEmpty()) {
            // Was reading — go back to chapter list
            showChapterList = true
            showControls = false
        } else {
            // Was on chapter list — close reader
            onClose()
        }
    }
    // The reader lives in a Compose Dialog, whose content does NOT inherit the dialog's back
    // dispatcher (Compose Dialog never provides LocalOnBackPressedDispatcherOwner), so a plain
    // BackHandler registers on the ACTIVITY's dispatcher and never fires — the dialog's own
    // back handling wins and dismisses the whole reader (landing on the home screen instead of
    // the chapter list). Instead we register the callback directly on the DIALOG's
    // OnBackPressedDispatcher. Compose hosts the dialog as an androidx.activity.ComponentDialog
    // (an OnBackPressedDispatcherOwner on every API level), reachable as the window callback.
    // This works both with predictive back (API 33+) and classic key events, so back always
    // returns to the chapter list first. The window may not be attached at first composition,
    // so the dialog is re-resolved shortly after; the callback is removed on dispose.
    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleReaderBack()
        }
    }
    LaunchedEffect(backCallback) {
        var dialog = view.findDialogWindow()?.callback as? ComponentDialog
        if (dialog == null) {
            delay(250)
            dialog = view.findDialogWindow()?.callback as? ComponentDialog
        }
        if (dialog != null) {
            dialog.onBackPressedDispatcher.addCallback(backCallback)
        }
        try {
            awaitCancellation()
        } finally {
            backCallback.remove()
        }
    }

    fun selectChapter(index: Int, startAtTop: Boolean = false) {
        val chapter = chapters.getOrNull(index) ?: run {
            android.util.Log.w("MangaReader", "selectChapter: index $index out of range, IGNORED")
            return
        }
        val resuming = !startAtTop && (
            currentChapterIndex >= 0 && scrollChapterIndex >= 0 && index == scrollChapterIndex ||
            scrollChapterIndex < 0 && index == manga.progress
            )
        currentChapterIndex = index
        currentPageIndex = 0
        scrollProgress = 0f
        suppressResumeRestore = !resuming
        showNextChapterButton = false
        if (resuming && manga.scrollProgress > 0f) {
            pendingResumeProgress = manga.scrollProgress
        } else {
            pendingResumeProgress = -1f
        }
        if (!resuming) {
            viewModel.updateMangaScrollProgress(manga.id, 0f)
        }
        pendingChapterLoad = true
        pendingChapterIndex = index
        showControls = false
        // Picking a chapter explicitly always loads it, even if a previous load of it was
        // cancelled, and drops any stale error (e.g. from a cancelled or failed chapter).
        cancelledChapterId = null
        displayedImagesError = null
    }

    // A chapter only counts as read once it is actually read (scrolled past the sync
    // threshold). Opening a chapter does NOT mark it read in the list — that happens here,
    // triggered from the scroll callbacks when the threshold is crossed.
    fun markChapterReadInListUi() {
        if (currentChapterIndex in 0 until chapters.size) {
            readIndices.value = readIndices.value + currentChapterIndex
        }
    }

    // Show a "Next Chapter" button when the reader settles at the end of the current chapter,
    // in BOTH vertical-scroll and single-page (LTR/RTL) modes. This replaces auto-advance:
    // nothing is opened automatically — the user taps the button. Debounced so rapid swipes
    // don't flash it, and hidden whenever the reader leaves the end of the chapter or the
    // setting is off.
    LaunchedEffect(currentPageIndex, scrollProgress, chapterImages, showChapterList, nextChapterButton, readerMode, isLastChapter) {
        if (!nextChapterButton || showChapterList || isLastChapter) {
            showNextChapterButton = false
            return@LaunchedEffect
        }
        val images = chapterImages
        if (images == null || images.isEmpty()) {
            showNextChapterButton = false
            return@LaunchedEffect
        }
        val atEnd = if (readerMode == ReaderMode.VERTICAL_SCROLL) {
            currentPageIndex >= images.lastIndex && scrollProgress >= 0.95f
        } else {
            currentPageIndex >= images.lastIndex
        }
        if (!atEnd) {
            showNextChapterButton = false
            return@LaunchedEffect
        }
        delay(700)
        // Re-check after the debounce: still at the end of the same chapter.
        val stillAtEnd = if (readerMode == ReaderMode.VERTICAL_SCROLL) {
            currentPageIndex >= images.lastIndex && scrollProgress >= 0.95f
        } else {
            currentPageIndex >= images.lastIndex
        }
        showNextChapterButton = stillAtEnd
    }

    // Retry the current chapter image load (used by the error UI)
    fun retryChapterLoad() {
        // Clear the stale error so the reader shows its loading state while retrying.
        displayedImagesError = null
        cancelledChapterId = null
        currentChapter?.let { chapter ->
            viewModel.loadChapterImages(chapter.chapterId, useDataSaver, manga.title, chapter.title, manga.id)
        }
    }

    // Cancel the current chapter image load: stops the request and leaves the reader on the
    // chapter that is already on screen (or on its error/retry screen) instead of spinning.
    fun cancelChapterLoad() {
        val chapter = currentChapter ?: return
        cancelledChapterId = chapter.chapterId
        viewModel.cancelChapterImageLoad()
        pendingChapterLoad = false
        pendingChapterIndex = -1
        // If nothing is on screen yet, fall back to the error/retry screen so the reader
        // does not spin forever.
        if (displayedImages.isNullOrEmpty()) {
            displayedImages = null
            displayedImagesError = loadCancelledMessage
        }
    }

    // The reader is hosted in a Dialog; make that window's background transparent so the
    // chapter-loading overlays can be genuinely see-through (like the anime stream-loading
    // overlay) and reveal the screen behind instead of a solid black box.
    LaunchedEffect(Unit) {
        view.findDialogWindow()?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        delay(250)
        view.findDialogWindow()?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    // True while there is genuinely nothing to display (initial chapter-list or chapter-image
    // loading). Once a screen is showing (chapter list, a chapter, or an error), the background
    // stays opaque so that screen remains visible behind the translucent loading overlay during
    // chapter transitions instead of showing through to whatever is behind the dialog.
    val isLoadingAny = (chapters.isEmpty() && (isLoadingChapters || !hasLoadedChapters)) ||
        (!showChapterList && displayedImages == null && displayedImagesError == null)

    Box(
        modifier = Modifier.fillMaxSize().background(
            when {
                isLoadingAny -> Color.Transparent
                isOled -> Color.Black
                else -> Color(0xFF1a1a1a)
            }
        )
    ) {
        val branch = when {
            showChapterList || chapters.isEmpty() -> "chapter_list"
            readerMode == ReaderMode.VERTICAL_SCROLL -> "vertical_scroll"
            else -> "paged"
        }
        // Log only when the significant render state actually changes (avoids spam during scroll)
        var lastRenderSig by remember { mutableStateOf("") }
        val renderSig = "$branch|${chapters.size}|$currentChapterIndex|$readerMode|${chapterImages?.size ?: "null"}"
        if (lastRenderSig != renderSig) {
            lastRenderSig = renderSig
        }
        when {
            // Show chapter list when explicitly requested OR when chapters haven't loaded yet
            // (shows a loading state inside MangaChapterListWithGroups)
            showChapterList || chapters.isEmpty() -> {
                // A COMPLETED series is fully read, so every row shows as read and the
                // list stays at the top instead of jumping to a "next unread" chapter
                // that cannot exist. Mirrors the anime episode list, which skips its
                // scroll-to-current for a completed title, and the detail screen, which
                // already reports the whole chapter count as read for COMPLETED.
                val isSeriesCompleted = manga.listStatus == "COMPLETED" ||
                    (manga.listStatus.isBlank() && manga.totalChapters > 0 && manga.progress >= manga.totalChapters)
                // First unread row, subchapters included: 314.1 IS the next thing to read
                // once 313 is done and 314 never shipped as a whole chapter. Filtering to
                // integer chapters here used to jump the continue button past unread
                // subchapters straight to 315.
                val nextUnreadIndex = (0 until chapters.size).firstOrNull {
                    it !in readIndices.value
                } ?: chapters.size
                MangaChapterListWithGroups(
                    chapters = chapters,
                    isLoadingChapters = isLoadingChapters,
                    hasLoadedChapters = hasLoadedChapters,
                    isLoadCancelled = isChapterLoadCancelled,
                    readIndices = if (isSeriesCompleted) chapters.indices.toSet() else readIndices.value,
                    // chapters.size means "no unread chapter": nothing to scroll to,
                    // nothing to auto-expand, and the header drops the continue button.
                    nextChapterToRead = if (isSeriesCompleted) chapters.size else nextUnreadIndex,
                    onChapterClick = { selectChapter(it) },
                    onContinueReading = {
                        if (nextUnreadIndex in chapters.indices) selectChapter(nextUnreadIndex)
                    },
                    onRetryLoadChapters = {
                        scope.launch {
                            runCatching { viewModel.fetchMangaDetail(manga.id) }
                            viewModel.loadMangaChapters(manga.id, manga.title)
                        }
                    },
                    onCancelLoadChapters = {
                        viewModel.cancelMangaChapterLoad()
                        // The chapter-list loading screen lives in the fullscreen reader
                        // Dialog layered over the manga detail screen. Stopping the load
                        // alone kept that dialog open: a fully transparent body over the
                        // detail screen with the dialog window's background dim still
                        // greying it out — an invisible overlay that only the back button
                        // dismissed. Closing the reader is what actually exits the
                        // loading: the user lands straight back on the screen underneath.
                        onClose()
                    },
                    onBack = {
                        handleReaderBack()
                    }
                )
            }

            readerMode == ReaderMode.VERTICAL_SCROLL -> {
                // Keyed by chapter so a chapter change resets the scroll/pager state to the top;
                // displayedImages (possibly still the previous chapter's) stays on screen behind
                // the loading overlay until the new chapter's images arrive.
                key(currentChapter?.chapterId) {
                    VerticalScrollReader(
                        chapterImages = displayedImages ?: emptyList(),
                        chapterImagesError = displayedImagesError,
                        chapter = currentChapter,
                        headers = chapterImageHeaders,
                        totalChapters = chapters.size,
                        currentIndex = currentChapterIndex,
                        scrollProgress = scrollProgress,
                        // Restore target for the vertical reader: prefer the LIVE scroll position (so
                        // switching from a single-page mode back to vertical resumes where you were),
                        // and fall back to the saved resume position only on the initial entry.
                        restoreProgress = when {
                            scrollProgress > 0f -> scrollProgress
                            !suppressResumeRestore && (currentChapterIndex == scrollChapterIndex || (scrollChapterIndex < 0 && currentChapterIndex == manga.progress)) -> manga.scrollProgress
                            else -> -1f
                        },
                        showControls = showControls,
                        onScrollProgress = {
                            if (viewModel.onMangaScrollProgress(
                                    mangaId = manga.id,
                                    chapter = currentChapter,
                                    scrollPercent = it,
                                    mangaTitle = manga.title,
                                    mangaCover = manga.cover
                                )
                            ) markChapterReadInListUi()
                            scrollProgress = it
                        },
                        onCurrentPage = { page -> currentPageIndex = page },
                        onToggleControls = { showControls = !showControls },
                        onRetry = { retryChapterLoad() },
                        onCancel = { cancelChapterLoad() }
                    )
                }
            }

            readerMode == ReaderMode.LEFT_TO_RIGHT || readerMode == ReaderMode.RIGHT_TO_LEFT -> {
                key(currentChapter?.chapterId) {
                    PagedMangaReader(
                        chapterImages = displayedImages ?: emptyList(),
                        chapterImagesError = displayedImagesError,
                        mode = readerMode,
                        headers = chapterImageHeaders,
                        initialPage = currentPageIndex.coerceIn(0, (displayedImages?.size ?: 1) - 1),
                        restorePage = if (pendingResumeProgress >= 0f && displayedImages != null && displayedImages!!.size > 1) {
                            // Pager mode stores scrollProgress = pageIndex / (total - 1) exactly,
                            // so multiplying back by (total - 1) recovers the index modulo IEEE
                            // 754 float precision. The +0.5f rounds to nearest so e.g. 2.9999998
                            // (from 3/7) restores to 3, not 2.
                            val page = ((pendingResumeProgress * (displayedImages!!.size - 1)) + 0.5f)
                                .toInt()
                                .coerceIn(0, displayedImages!!.size - 1)
                            pendingResumeProgress = -1f
                            page
                        } else -1,
                        onToggleControls = { showControls = !showControls },
                        onPrevChapter = { if (!isFirstChapter) selectChapter(currentChapterIndex - 1) },
                        onNextChapter = { if (!isLastChapter) selectChapter(currentChapterIndex + 1) },
                        onRetry = { retryChapterLoad() },
                        onCancel = { cancelChapterLoad() },
                        onPageChanged = { page, total ->
                            currentPageIndex = page
                            if (total > 1) {
                                val progress = page.toFloat() / (total - 1).toFloat()
                                if (viewModel.onMangaScrollProgress(
                                        mangaId = manga.id,
                                        chapter = currentChapter,
                                        scrollPercent = progress,
                                        mangaTitle = manga.title,
                                        mangaCover = manga.cover
                                    )
                                ) markChapterReadInListUi()
                                scrollProgress = progress
                            }
                        }
                    )
                }
            }
        }

        // Loading overlay shown over whatever screen is currently displayed (the chapter list,
        // or the current chapter while a next/prev transition loads). Drawn after the branch
        // content so it sits on top, and dismissed once the new chapter's images are ready.
        if (pendingChapterLoad) {
            ChapterLoadingView(onCancel = { cancelChapterLoad() })
        }

        // ─── Overlay: top bar (only when reading) ─────────────────────
        AnimatedVisibility(
            visible = showControls && !showChapterList,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = manga.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentChapter != null) {
                                Text(
                                    text = currentChapter.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    // The top bar is always dark, so the theme's onSurfaceVariant
                                    // (dark in the light theme) would be unreadable here.
                                    color = ReaderTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { handleReaderBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { if (currentChapterIndex > 0) selectChapter(currentChapterIndex - 1) },
                            enabled = currentChapterIndex > 0
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = stringResource(R.string.reader_previous_chapter),
                                tint = if (currentChapterIndex > 0) ReaderTextStrong else ReaderTextStrong.copy(alpha = 0.3f)
                            )
                        }
                        IconButton(
                            onClick = { if (currentChapterIndex < chapters.lastIndex) selectChapter(currentChapterIndex + 1) },
                            enabled = currentChapterIndex < chapters.lastIndex
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.reader_next_chapter),
                                tint = if (currentChapterIndex < chapters.lastIndex) ReaderTextStrong else ReaderTextStrong.copy(alpha = 0.3f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.95f),
                        titleContentColor = ReaderTextStrong,
                        navigationIconContentColor = ReaderTextStrong,
                        actionIconContentColor = ReaderTextStrong
                    )
                )
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.95f))) {
                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.2f)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(scrollProgress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(ReaderAccent)
                    )
                    // AniList sync-threshold marker — shows where the sync-threshold mark-as-read
                    // kicks in on the progress bar.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(syncThreshold / 100f)
                            .fillMaxHeight()
                            .background(Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                }
            }
        }

        // ─── Overlay: "Next Chapter" button ──────────────────────────
        // Appears after the debounce once the reader settles at the end of a chapter (all modes)
        // when the Next Chapter Button setting is on. Tapping it opens the next chapter from the
        // top; scrolling away from the end hides it.
        AnimatedVisibility(
            visible = showNextChapterButton && !showChapterList && !isLastChapter,
            enter = fadeIn(tween(150)) + slideInVertically(tween(200)) { it / 2 },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val nextLabel = cachedNextChapterForButton?.let { ch ->
                val num = extractChapterNum(ch.title)
                if (num != "?") stringResource(R.string.reader_chapter_short, num) else ch.title
            } ?: ""
            Button(
                onClick = {
                    selectChapter(currentChapterIndex + 1, startAtTop = true)
                },
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 84.dp)
                    .height(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        // Tinted with the button's content color so the arrow stays legible
                        // on the circle in both app themes.
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.reader_next_chapter),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (nextLabel.isNotBlank()) {
                        Text(
                            text = nextLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ─── Overlay: page indicator + bottom bar ─────────────────────
        // The page indicator sits directly above the bottom controls bar instead of
        // floating independently. It stays visible whenever the setting is on (independent
        // of the controls overlay); when the controls bar is hidden it rests at the bottom
        // edge above the navigation bar.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            val totalPages = chapterImages?.size ?: 0
            if (showPageIndicator && !showChapterList && totalPages > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentPageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = ReaderTextStrong
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = showControls && !showChapterList,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.setMangaLockRotation(!lockRotation) },
                        modifier = Modifier.align(Alignment.CenterStart).size(32.dp)
                    ) {
                        Icon(
                            if (lockRotation) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.reader_toggle_rotation_lock),
                            tint = if (lockRotation) ReaderAccent else ReaderTextFaint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    ReaderModeSegmentedToggle(
                        currentMode = readerMode,
                        onSelect = { mode ->
                            readerMode = mode
                            viewModel.setMangaReaderMode(
                                when (mode) {
                                    ReaderMode.VERTICAL_SCROLL -> "vertical_scroll"
                                    ReaderMode.LEFT_TO_RIGHT -> "left_to_right"
                                    ReaderMode.RIGHT_TO_LEFT -> "right_to_left"
                                }
                            )
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.setMangaFullscreen(!fullscreen) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(R.string.reader_toggle_fullscreen),
                                tint = if (fullscreen) ReaderAccent else ReaderTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setMangaPageIndicator(!showPageIndicator) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = stringResource(R.string.reader_toggle_page_indicator),
                                tint = if (showPageIndicator) ReaderAccent else ReaderTextFaint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!hasMangaSource) {
            AlertDialog(
                onDismissRequest = onClose,
                title = { Text(stringResource(R.string.reader_no_extension_selected)) },
                text = { Text(stringResource(R.string.reader_no_extension_message)) },
                confirmButton = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.reader_go_to_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.reader_close))
                    }
                }
            )
        }
    }
}
