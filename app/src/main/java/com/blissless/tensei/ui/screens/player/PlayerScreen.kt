package com.blissless.tensei.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.blissless.tensei.R
import com.blissless.tensei.util.formatPlaybackTime
import com.blissless.tensei.data.models.EpisodeStreams
import com.blissless.tensei.data.models.EpisodeTimestamps
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.data.models.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.player.engine.ExoPlayerEngine

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    referer: String,
    subtitleUrl: String? = null,
    subtitleTracks: List<eu.kanade.tachiyomi.animesource.model.Track> = emptyList(),
    currentEpisode: Int = 1,
    totalEpisodes: Int = 0,
    animeName: String = "",
    episodeTitle: String? = null,
    animeId: Int = 0,
    malId: Int = 0,
    animeYear: Int? = null,
    isLoadingStream: Boolean = false,
    episodeInfo: EpisodeStreams? = null,
    currentServerName: String = "",
    currentCategory: String = "sub",
    isFallbackStream: Boolean = false,
    requestedCategory: String = "sub",
    forwardSkipSeconds: Int = 10,
    backwardSkipSeconds: Int = 10,
    autoSkipOpening: Boolean = false,
    autoSkipEnding: Boolean = false,
    autoPlayNextEpisode: Boolean = false,
    savedPosition: Long = 0L,
    pendingSeekPosition: Long? = null,
    onSetPendingSeekPosition: ((Long?) -> Unit)? = null,
    currentQuality: String = "Auto",
    isLatestEpisode: Boolean = false,
    disableMaterialColors: Boolean = false,
    isOled: Boolean = false,
    showBufferIndicator: Boolean = true,
    bufferAheadSeconds: Int = 30,
    swipeVolume: Boolean = false,
    swipeBrightness: Boolean = false,
    swipeSwap: Boolean = false,
    onSwipeVolumeChange: ((Boolean) -> Unit)? = null,
    onSwipeBrightnessChange: ((Boolean) -> Unit)? = null,
    onSwipeSwapChange: ((Boolean) -> Unit)? = null,
    animekaiIntroStart: Int? = null,
    animekaiIntroEnd: Int? = null,
    animekaiOutroStart: Int? = null,
    animekaiOutroEnd: Int? = null,
    onSavePosition: ((Long, Long) -> Unit)? = null,
    onClearPlaybackPosition: ((Int, Int) -> Unit)? = null,
    onPositionSaved: ((Long) -> Unit)? = null,
    onProgressUpdate: (percentage: Int) -> Unit = {},
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    onServerChange: ((serverName: String, category: String) -> Unit)? = null,
    onPlaybackError: (() -> Unit)? = null,
    onInvalidateStreamCache: (() -> Unit)? = null,
    onRefreshStream: (() -> Unit)? = null,
    onPrefetchAdjacent: (() -> Unit)? = null,
    onGetCacheDataSourceFactory: (String) -> CacheDataSource.Factory? = { null },
    onBackClick: (() -> Unit)? = null,
    isFullscreen: Boolean = true,
    onFullscreenChanged: ((Boolean) -> Unit)? = null,
    extensionOkHttpClient: okhttp3.OkHttpClient? = null,
    extensionVideoHeaders: Map<String, String> = emptyMap(),
    extensionServers: List<ServerInfo> = emptyList(),
    extensionName: String = "",
    onExtensionServerChange: ((hosterName: String) -> Unit)? = null,
    onPrefetchNextExtensionEpisode: (() -> Unit)? = null,
    onAutoPlayNextEpisodeChanged: ((Boolean) -> Unit)? = null,
    isTorrentStream: Boolean = false,
    onTorrentSeek: ((positionMs: Long, durationMs: Long) -> Unit)? = null,
    supportsPiP: Boolean = false,
    onPiPToggle: ((Boolean) -> Unit)? = null,
    isInPiPMode: Boolean = false,
    onPlayerBoundsChanged: ((left: Int, top: Int, right: Int, bottom: Int) -> Unit)? = null,
    discordRichPresence: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Player UI state holder (see PlayerScreenState) — created once per
    // composition. Stream-scoped fields (timestamps, skip/restore markers)
    // reset when videoUrl changes, reproducing the original keyed-remember
    // semantics (reset during composition, on key change).
    val showTimeLeftState = rememberSaveable { mutableStateOf(false) }
    val s = remember {
        PlayerScreenState(
            initialSubtitlesEnabled = subtitleTracks.isNotEmpty(),
            initialSelectedQuality = currentQuality,
            initialSubtitleProfile = loadSubtitleProfileData(context),
            showTimeLeftState = showTimeLeftState,
        )
    }
    remember(videoUrl) { s.resetForNewStream() }

    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "16:9",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch",
    )

    // Cycles: 16:9 (fit, letterboxed) <-> Stretch (fill the whole screen)

    // Fullscreen state is owned by the host (lifted so it survives player recreation
    // on episode changes). The host mirrors it back in as this parameter.
    fun applyFullscreenWindow(fullscreen: Boolean) {
        activity?.let { act ->
            val window = act.window
            if (fullscreen) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                WindowCompat.setDecorFitsSystemWindows(window, false)
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                @Suppress("DEPRECATION")
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    LaunchedEffect(isFullscreen) {
        applyFullscreenWindow(isFullscreen)
    }

    // Handle fullscreen toggle
    fun toggleFullscreen() {
        onFullscreenChanged?.invoke(!isFullscreen)
    }

    // Exit fullscreen when closing
    fun exitFullscreen() {
        if (isFullscreen) {
            onFullscreenChanged?.invoke(false)
        }
    }

    // Raw "should be buffering" intent; s.isBuffering below is only set from this after a
    // debounce so the spinner never blinks off/on around a seek.


    // YouTube-style timestamp: the left indicator flips between elapsed/total and time left.

    // After a seek (double tap or scrub release) the controls should go away quickly
    // instead of waiting out the full auto-hide delay.

    // Debounced buffering indicator: only surface the spinner when buffering outlasts a
    // short window, and keep it up briefly so a quick READY flip can't make it flicker.
    LaunchedEffect(s.bufferIntent) {
        if (s.bufferIntent) {
            delay(350.milliseconds)
            s.isBuffering = true
        } else {
            delay(400.milliseconds)
            s.isBuffering = false
        }
    }


    LaunchedEffect(s.showControls, s.hasError, s.showSkipIndicator, isInPiPMode) {
        if (isInPiPMode) {
            delay(300.milliseconds)
            s.controlsVisible = false
        } else {
            s.controlsVisible = s.showControls || s.hasError || s.showSkipIndicator
        }
    }

    // Helper to check if device has internet connection
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }



    val scope = rememberCoroutineScope()


    // PRIMARY: Use Animekai timestamps if available, create initial timestamps immediately
    val animekaiTimestamps = remember(animekaiIntroStart, animekaiIntroEnd, animekaiOutroStart, animekaiOutroEnd, currentEpisode) {
        if (animekaiIntroStart != null || animekaiOutroStart != null) {
            EpisodeTimestamps(
                episodeNumber = currentEpisode,
                introStart = animekaiIntroStart?.toLong(),
                introEnd = animekaiIntroEnd?.toLong(),
                creditsStart = animekaiOutroStart?.toLong(),
                creditsEnd = animekaiOutroEnd?.toLong(),
                recapStart = null,
                recapEnd = null,
                allTimestamps = buildList {
                    if (animekaiIntroStart != null) add(Timestamp(animekaiIntroStart.toDouble(), "op", "op"))
                    if (animekaiOutroStart != null) add(Timestamp(animekaiOutroStart.toDouble(), "ed", "ed"))
                }
            )
        } else null
    }

    val effectiveTimestamps by remember(s.episodeTimestamps, animekaiTimestamps) {
        derivedStateOf {
            s.episodeTimestamps ?: animekaiTimestamps ?: EpisodeTimestamps(
                episodeNumber = currentEpisode,
                introStart = null,
                introEnd = null,
                creditsStart = null,
                creditsEnd = null,
                recapStart = null,
                recapEnd = null,
                allTimestamps = emptyList()
            )
        }
    }

    // Update selected quality when currentQuality prop changes
    LaunchedEffect(currentQuality) {
        s.selectedQuality = currentQuality
    }

    val engine = remember(context, bufferAheadSeconds) {
        ExoPlayerEngine(context, bufferAheadSeconds) { ref -> onGetCacheDataSourceFactory(ref) }
    }

    fun formatTime(ms: Long): String = formatPlaybackTime(ms)

    fun handleServerChange(serverName: String, category: String) =
        s.handleServerChange(engine, scope, onSavePosition, onPositionSaved, onServerChange, serverName, category)

    // Engine side effects (listener, polling, timestamp/skip/progress
    // handling, auto-retry, cleanup) — see PlayerEngineEffects.kt.
    PlayerEngineEffects(
        s = s,
        engine = engine,
        animekaiTimestamps = animekaiTimestamps,
        effectiveTimestamps = effectiveTimestamps,
        videoUrl = videoUrl,
        referer = referer,
        subtitleUrl = subtitleUrl,
        subtitleTracks = subtitleTracks,
        currentEpisode = currentEpisode,
        totalEpisodes = totalEpisodes,
        animeId = animeId,
        animeName = animeName,
        animeYear = animeYear,
        malId = malId,
        currentServerName = currentServerName,
        currentCategory = currentCategory,
        requestedCategory = requestedCategory,
        savedPosition = savedPosition,
        pendingSeekPosition = pendingSeekPosition,
        isLatestEpisode = isLatestEpisode,
        isFallbackStream = isFallbackStream,
        isTorrentStream = isTorrentStream,
        isInPiPMode = isInPiPMode,
        autoSkipOpening = autoSkipOpening,
        autoSkipEnding = autoSkipEnding,
        autoPlayNextEpisode = autoPlayNextEpisode,
        discordRichPresence = discordRichPresence,
        episodeInfo = episodeInfo,
        extensionServers = extensionServers,
        extensionOkHttpClient = extensionOkHttpClient,
        extensionVideoHeaders = extensionVideoHeaders,
        onProgressUpdate = onProgressUpdate,
        onPositionSaved = onPositionSaved,
        onSavePosition = onSavePosition,
        onClearPlaybackPosition = onClearPlaybackPosition,
        onPreviousEpisode = onPreviousEpisode,
        onNextEpisode = onNextEpisode,
        onServerChange = onServerChange,
        onExtensionServerChange = onExtensionServerChange,
        onInvalidateStreamCache = onInvalidateStreamCache,
        onRefreshStream = onRefreshStream,
        onPrefetchNextExtensionEpisode = onPrefetchNextExtensionEpisode,
        onPrefetchAdjacent = onPrefetchAdjacent,
        onTorrentSeek = onTorrentSeek,
        onBackClick = onBackClick,
        isNetworkAvailable = ::isNetworkAvailable,
        formatTime = ::formatTime,
        handleServerChange = ::handleServerChange,
    )

    fun seekToPosition(position: Long) = s.seekToPosition(engine, isTorrentStream, onTorrentSeek, position)


    fun seekBy(milliseconds: Long) = s.seekBy(engine, scope, isTorrentStream, onTorrentSeek, milliseconds)


    fun performManualSeek(position: Long) = s.performManualSeek(engine, scope, isTorrentStream, onTorrentSeek, position)



    fun handlePlaybackError() = s.handlePlaybackError(onInvalidateStreamCache, onPlaybackError)


    fun rebuildWithSubtitles(enable: Boolean) =
        s.rebuildWithSubtitles(engine, videoUrl, subtitleUrl, subtitleTracks, enable)


    fun getActiveSubtitleSettings(): SubtitleSettings = s.getActiveSubtitleSettings()


    fun saveSubtitleProfileData(data: SubtitleProfileData) = s.saveSubtitleProfileData(context, data)


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { coords: LayoutCoordinates ->
                val bounds = coords.boundsInWindow()
                onPlayerBoundsChanged?.invoke(
                    bounds.left.toInt(), bounds.top.toInt(),
                    bounds.right.toInt(), bounds.bottom.toInt()
                )
            }
    ) {
        // Compact layout (video shown in a 16:9 box with the episode list below):
        // shrink paddings, hide secondary text and use smaller buttons so the
        // controls fit the reduced-height video area.
        val isCompact = !isFullscreen
        // Player view - recreate when server or engine changes
        key(s.serverChangeTrigger) {
            AndroidView(
                factory = { _ ->
                    // Apply initial settings; the engine owns and configures its own view
                    val v = engine.view
                    if (v is PlayerView) {
                        v.useController = false
                        v.setShowNextButton(false)
                        v.setShowPreviousButton(false)
                        v.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        v.controllerShowTimeoutMs = 3000
                        v.controllerAutoShow = false
                        engine.setResizeMode(resizeModes[s.resizeModeIndex].first)
                        val activeSubSettings = getActiveSubtitleSettings()
                        v.subtitleView?.apply { applySubtitleStyle(this, activeSubSettings) }
                    }
                    v
                },
                modifier = Modifier
                    .fillMaxSize(),
                update = { view ->
                    if (view is PlayerView) {
                        engine.setResizeMode(resizeModes[s.resizeModeIndex].first)
                        s.subtitleViewRef = view.subtitleView
                        val activeSubSettings = getActiveSubtitleSettings()
                        view.subtitleView?.apply { applySubtitleStyle(this, activeSubSettings) }
                    }
                }
            )
        }

        LaunchedEffect(s.subtitleProfileData) {
            s.subtitleViewRef?.let { view ->
                applySubtitleStyle(view, getActiveSubtitleSettings())
            }
        }

        // Apply resize mode to the active ExoPlayer engine (sets PlayerView.resizeMode).
        LaunchedEffect(s.resizeModeIndex) {
            engine.setResizeMode(resizeModes[s.resizeModeIndex].first)
        }

        // 2. Active Gesture Zones (Middle Layer)
        // These handle seeking and toggling controls. Defined first so they are "under" the padding zones.

        // Left Seek Zone (30% width, offset by padding)
        // Also handles vertical drag for volume when swipeVolume is enabled
        var lastLeftTapTime by remember { mutableLongStateOf(0L) }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .padding(start = if (isCompact) 0.dp else 40.dp)
                .align(Alignment.CenterStart)
                .pointerInput(swipeVolume, swipeBrightness, swipeSwap) {
                    val leftEnabled = if (swipeSwap) swipeBrightness else swipeVolume
                    if (leftEnabled) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (!s.hasError) {
                                    if (swipeSwap) {
                                        val brightnessChange = -(dragAmount / 1000f)
                                        s.currentBrightness = (s.currentBrightness + brightnessChange).coerceIn(0.01f, 1f)
                                        activity?.let { act ->
                                            val lp = act.window.attributes
                                            lp.screenBrightness = s.currentBrightness
                                            act.window.attributes = lp
                                        }
                                        s.showBrightnessOverlay = true
                                        scope.launch {
                                            delay(1500.milliseconds)
                                            s.showBrightnessOverlay = false
                                        }
                                    } else {
                                        val volumeChange = -(dragAmount / 500f)
                                        s.playerVolume = (s.playerVolume + volumeChange).coerceIn(0f, 1f)
                                        engine.setVolume(s.playerVolume)
                                        s.showVolumeOverlay = true
                                        scope.launch {
                                            delay(1500.milliseconds)
                                            s.showVolumeOverlay = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(backwardSkipSeconds) {
                    detectTapGestures(
                        onTap = {
                            if (!s.hasError) {
                                val now = System.currentTimeMillis()
                                if (now - lastLeftTapTime < 300) {
                                    // Double tap - seek. Undo the first tap's control toggle so the
                                    // gesture leaves s.showControls exactly as it was before it started
                                    // (stays up when paused+visible, stays hidden when paused+hidden).
                                    s.showControls = !s.showControls
                                    seekBy(-(backwardSkipSeconds * 1000L))
                                } else {
                                    // Single tap - toggle controls
                                    s.showControls = !s.showControls
                                }
                                lastLeftTapTime = now
                            }
                        }
                    )
                }
        )

        // Center Toggle Zone (40% width)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(Alignment.Center)
                .pointerInput(Unit) { detectTapGestures(onTap = { if (!s.hasError) s.showControls = !s.showControls }) }
        )

        // Right Seek Zone (30% width, offset by padding)
        // Also handles vertical drag for brightness when swipeBrightness is enabled
        var lastRightTapTime by remember { mutableLongStateOf(0L) }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .padding(end = if (isCompact) 0.dp else 40.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(swipeVolume, swipeBrightness, swipeSwap) {
                    val rightEnabled = if (swipeSwap) swipeVolume else swipeBrightness
                    if (rightEnabled) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                if (!s.hasError) {
                                    if (swipeSwap) {
                                        val volumeChange = -(dragAmount / 500f)
                                        s.playerVolume = (s.playerVolume + volumeChange).coerceIn(0f, 1f)
                                        engine.setVolume(s.playerVolume)
                                        s.showVolumeOverlay = true
                                        scope.launch {
                                            delay(1500.milliseconds)
                                            s.showVolumeOverlay = false
                                        }
                                    } else {
                                        val brightnessChange = -(dragAmount / 1000f)
                                        s.currentBrightness = (s.currentBrightness + brightnessChange).coerceIn(0.01f, 1f)
                                        activity?.let { act ->
                                            val lp = act.window.attributes
                                            lp.screenBrightness = s.currentBrightness
                                            act.window.attributes = lp
                                        }
                                        s.showBrightnessOverlay = true
                                        scope.launch {
                                            delay(1500.milliseconds)
                                            s.showBrightnessOverlay = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(forwardSkipSeconds) {
                    detectTapGestures(
                        onTap = {
                            if (!s.hasError) {
                                val now = System.currentTimeMillis()
                                if (now - lastRightTapTime < 300) {
                                    // Double tap - seek; undo the first tap's control toggle (see left zone).
                                    s.showControls = !s.showControls
                                    seekBy(forwardSkipSeconds * 1000L)
                                } else {
                                    // Single tap - toggle controls
                                    s.showControls = !s.showControls
                                }
                                lastRightTapTime = now
                            }
                        }
                    )
                }
        )

        // 3. Padding Zones (Top Layer over Active Zones, Under UI Controls)
        // These consume touches to prevent UI toggling in safe areas.
        // Defined after active zones so they take precedence in overlap areas.
        // Only in fullscreen — compact player uses minimal chrome.
        if (!isCompact) {
            // Left Padding
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            )

            // Right Padding
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            )

            // Top Padding (Matches side padding logic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.TopCenter)
                    .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            )
        }

        val subServers = episodeInfo?.subServers ?: emptyList()
        val dubServers = episodeInfo?.dubServers ?: emptyList()

        val introStartRatio = if (s.duration > 0 && effectiveTimestamps.introStart != null) {
            (effectiveTimestamps.introStart!! * 1000).toFloat() / s.duration.toFloat()
        } else null
        val introEndRatio = if (s.duration > 0 && effectiveTimestamps.introEnd != null) {
            (effectiveTimestamps.introEnd!! * 1000).toFloat() / s.duration.toFloat()
        } else null
        val creditsStartRatio = if (s.duration > 0 && effectiveTimestamps.creditsStart != null) {
            (effectiveTimestamps.creditsStart!! * 1000).toFloat() / s.duration.toFloat()
        } else null
        val creditsAtEnd = if (s.duration > 0 && effectiveTimestamps.creditsEnd != null) {
            val creditsEndSeconds = effectiveTimestamps.creditsEnd!! * 1000
            val durationDiff = s.duration - creditsEndSeconds
            durationDiff < 30000 // Credits end within 30 seconds of the end
        } else false

        // Controls UI with darkening overlay (drawn first) — see PlayerControlsUi.kt
        PlayerControlsUi(
            s = s,
            context = context,
            scope = scope,
            engine = engine,
            videoUrl = videoUrl,
            animeName = animeName,
            episodeTitle = episodeTitle,
            currentEpisode = currentEpisode,
            totalEpisodes = totalEpisodes,
            currentServerName = currentServerName,
            currentCategory = currentCategory,
            extensionServers = extensionServers,
            extensionName = extensionName,
            isFullscreen = isFullscreen,
            isCompact = isCompact,
            isLatestEpisode = isLatestEpisode,
            isLoadingStream = isLoadingStream,
            isOled = isOled,
            showBufferIndicator = showBufferIndicator,
            supportsPiP = supportsPiP,
            autoPlayNextEpisode = autoPlayNextEpisode,
            swipeVolume = swipeVolume,
            swipeBrightness = swipeBrightness,
            swipeSwap = swipeSwap,
            subtitleUrl = subtitleUrl,
            subtitleTracks = subtitleTracks,
            resizeModes = resizeModes,
            effectiveTimestamps = effectiveTimestamps,
            subServers = subServers,
            dubServers = dubServers,
            introStartRatio = introStartRatio,
            introEndRatio = introEndRatio,
            creditsStartRatio = creditsStartRatio,
            creditsAtEnd = creditsAtEnd,
            disableMaterialColors = disableMaterialColors,
            onBackClick = onBackClick,
            onPreviousEpisode = onPreviousEpisode,
            onNextEpisode = onNextEpisode,
            onPiPToggle = onPiPToggle,
            onServerChange = onServerChange,
            onExtensionServerChange = onExtensionServerChange,
            onAutoPlayNextEpisodeChanged = onAutoPlayNextEpisodeChanged,
            onSwipeVolumeChange = onSwipeVolumeChange,
            onSwipeBrightnessChange = onSwipeBrightnessChange,
            onSwipeSwapChange = onSwipeSwapChange,
            toggleFullscreen = ::toggleFullscreen,
            exitFullscreen = ::exitFullscreen,
            seekToPosition = ::seekToPosition,
            performManualSeek = ::performManualSeek,
            handleServerChange = ::handleServerChange,
            handlePlaybackError = ::handlePlaybackError,
            rebuildWithSubtitles = ::rebuildWithSubtitles,
            getActiveSubtitleSettings = ::getActiveSubtitleSettings,
            saveSubtitleProfileData = ::saveSubtitleProfileData,
            formatTime = ::formatTime,
        )

    }
}

// loadSubtitleProfileData and applySubtitleStyle moved to SubtitleStyleHelpers.kt

