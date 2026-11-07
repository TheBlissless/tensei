package com.blissless.tensei.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.WindowManager
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blissless.tensei.R
import com.blissless.tensei.player.skip.AnimeSkipService
import com.blissless.tensei.data.models.EpisodeStreams
import com.blissless.tensei.data.models.EpisodeTimestamps
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.util.longToast
import com.blissless.tensei.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.player.engine.EmbeddedSubtitleTrack
import com.blissless.tensei.player.engine.ExoPlayerEngine
import com.blissless.tensei.player.engine.PlayerEngine
import com.blissless.tensei.player.engine.SubtitleConfig

/**
 * Side-effect host for the player engine: the engine lifecycle listener,
 * position polling, timestamp fetching (AnimeSkip/AnimeThemes fallback),
 * intro/outro skip detection, progress tracking, auto-retry on errors and
 * the PiP/cleanup disposables — everything previously declared inline in
 * the [PlayerScreen] composable body.
 *
 * Extracted verbatim; operates on the shared [PlayerScreenState] holder.
 * The player's local operations are passed in as functions so they always
 * reference the current composition's values.
 */
@Composable
internal fun PlayerEngineEffects(
    s: PlayerScreenState,
    engine: com.blissless.tensei.player.engine.PlayerEngine,
    animekaiTimestamps: com.blissless.tensei.data.models.EpisodeTimestamps?,
    effectiveTimestamps: com.blissless.tensei.data.models.EpisodeTimestamps,
    videoUrl: String,
    referer: String,
    subtitleUrl: String?,
    subtitleTracks: List<eu.kanade.tachiyomi.animesource.model.Track>,
    currentEpisode: Int,
    totalEpisodes: Int,
    animeId: Int,
    animeName: String,
    animeYear: Int?,
    malId: Int,
    currentServerName: String,
    currentCategory: String,
    requestedCategory: String,
    savedPosition: Long,
    pendingSeekPosition: Long?,
    isLatestEpisode: Boolean,
    isFallbackStream: Boolean,
    isTorrentStream: Boolean,
    isInPiPMode: Boolean,
    autoSkipOpening: Boolean,
    autoSkipEnding: Boolean,
    autoPlayNextEpisode: Boolean,
    discordRichPresence: Boolean,
    episodeInfo: com.blissless.tensei.data.models.EpisodeStreams?,
    extensionServers: List<com.blissless.tensei.data.models.ServerInfo>,
    extensionOkHttpClient: okhttp3.OkHttpClient?,
    extensionVideoHeaders: Map<String, String>,
    onProgressUpdate: (percentage: Int) -> Unit,
    onPositionSaved: ((position: Long) -> Unit)?,
    onSavePosition: ((position: Long, duration: Long) -> Unit)?,
    onClearPlaybackPosition: ((animeId: Int, episode: Int) -> Unit)?,
    onPreviousEpisode: (() -> Unit)?,
    onNextEpisode: (() -> Unit)?,
    onServerChange: ((serverName: String, category: String) -> Unit)?,
    onExtensionServerChange: ((hosterName: String) -> Unit)?,
    onInvalidateStreamCache: (() -> Unit)?,
    onRefreshStream: (() -> Unit)?,
    onPrefetchNextExtensionEpisode: (() -> Unit)?,
    onPrefetchAdjacent: (() -> Unit)?,
    onTorrentSeek: ((positionMs: Long, durationMs: Long) -> Unit)?,
    onBackClick: (() -> Unit)?,
    isNetworkAvailable: () -> Boolean,
    formatTime: (Long) -> String,
    handleServerChange: (serverName: String, category: String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val animeSkipService = remember { AnimeSkipService(context) }

    LaunchedEffect(engine) {
        engine.setListener(object : PlayerEngine.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                s.isPlaying = isPlaying
                if (isPlaying) {
                    s.hasPlaybackStarted = true
                }
                val act = context as? Activity
                if (act is com.blissless.tensei.MainActivity) {
                    if (act.isInPiPMode.value) {
                        act.updatePiPPlayPauseIcon(isPlaying)
                    } else {
                        // Refresh the auto-enter PIP params so the system caches the correct
                        // play/pause icon for the moment the user actually enters PIP.
                        // Otherwise the snapshot taken when the player first appeared (still
                        // loading, engine not isPlaying) would show the wrong icon on entry.
                        act.refreshAutoEnterPiPSnapshot()
                    }
                }
            }

            override fun onTracksChanged(tracks: List<EmbeddedSubtitleTrack>) {
                s.embeddedSubtitleTracks = tracks
            }

            override fun onError(error: String) {
                Log.e("PlayerScreen", "onError: msg=$error s.isChangingServer=$s.isChangingServer s.isInitialLoading=$s.isInitialLoading url=${videoUrl.take(80)}")
                if (s.isChangingServer) {
                    return
                }

                if (!isNetworkAvailable()) {
                    s.isOffline = true
                    s.bufferIntent = true
                    s.hasError = false
                    s.playbackError = null
                    return
                }

                if ((s.isManuallySeeking || s.seekRetryCount > 0) && s.seekRetryCount < 3) {
                    s.seekRetryCount++
                    s.hasError = false
                    s.playbackError = null
                    s.bufferIntent = true
                    val seekPos = s.currentPosition
                    Log.w("PlayerScreen", "onError: retry #$s.seekRetryCount seekPos=$seekPos isTorrentStream=$isTorrentStream error=$error")
                    if (isTorrentStream && seekPos > 0) {
                        onTorrentSeek?.invoke(seekPos, engine.duration)
                        engine.seekTo(seekPos)
                    } else if (seekPos > 0) {
                        engine.seekOutsideBuffer(seekPos)
                    } else {
                        engine.prepare()
                    }
                    return
                }

                // For torrent streams (local server), don't auto-refresh — that
                // restarts the entire torrent download. Instead, retry the seek
                // so the engine reconnects to the TorrentStreamServer after more
                // data has downloaded.
                if (s.isInitialLoading && isTorrentStream) {
                    s.seekRetryCount = 1
                    s.hasError = false
                    s.playbackError = null
                    s.bufferIntent = true
                    onTorrentSeek?.invoke(s.currentPosition, engine.duration)
                    engine.prepare()
                    return
                }

                // Auto-retry: try next untried server before anything else
                if (onExtensionServerChange != null && extensionServers.isNotEmpty()) {
                    s.autoRetryServers.add(currentServerName)
                    extensionServers.find { it.url == videoUrl }?.let { s.autoRetryServers.add(it.name) }
                    fun srvCat(name: String): String = when {
                        name.contains("dub", ignoreCase = true) -> "dub"
                        name.contains("sub", ignoreCase = true) -> "sub"
                        else -> "other"
                    }
                    val curCat = srvCat(currentServerName)
                    val remaining = extensionServers.filter {
                        it.name !in s.autoRetryServers && it.url != videoUrl
                    }
                    val sameCat = remaining.filter { srvCat(it.name) == curCat }
                    val otherCat = remaining.filter { srvCat(it.name) != curCat }
                    val nextServer = (sameCat + otherCat).firstOrNull()
                    if (nextServer != null) {
                        // Pass URL (not name) — `handleExtensionServerChange`
                        // now looks up by URL so duplicates are correctly
                        // disambiguated when auto-retrying.
                        s.pendingAutoRetry = nextServer.url
                        return
                    }
                    Log.w("PlayerScreen", "All extension servers exhausted")
                }

                // Auto-refresh for initial load / re-entry failure (stale cached URL).
                // isAutoRefreshing in MainActivity prevents infinite refreshes.
                if (s.isInitialLoading && onRefreshStream != null) {
                    onInvalidateStreamCache?.invoke()
                    onRefreshStream.invoke()
                    return
                }

                Log.e("PlayerScreen", "SURFACING ERROR TO USER: msg=$error s.isManuallySeeking=$s.isManuallySeeking s.seekRetryCount=$s.seekRetryCount s.isInitialLoading=$s.isInitialLoading")
                s.hasError = true
                s.playbackError = error
                s.showControls = true
                Log.e("PlayerScreen", "Playback error details: msg=$error videoUrl=${videoUrl.take(120)}")
            }

            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when (state) { PlayerEngine.STATE_IDLE -> "IDLE"; PlayerEngine.STATE_BUFFERING -> "BUFFERING"; PlayerEngine.STATE_READY -> "READY"; PlayerEngine.STATE_ENDED -> "ENDED"; else -> "$state" }
                s.bufferIntent = state == PlayerEngine.STATE_BUFFERING
                if (state == PlayerEngine.STATE_READY) {
                    s.hasError = false
                    s.playbackError = null
                    s.isChangingServer = false
                    s.bufferIntent = false
                    s.hasPlaybackStarted = true
                    s.isInitialLoading = false
                    s.autoRetryServers.clear()
                    if (s.pendingQualityChange != null && s.savedPositionForQuality > 0) {
                        val wasPlaying = engine.playWhenReady
                        val restorePos = s.savedPositionForQuality
                        engine.stop()
                        engine.clearMediaItems()
                        engine.loadMedia(
                            url = videoUrl,
                            mimeType = null,
                            startPositionMs = restorePos,
                            subtitleConfigs = emptyList(),
                            headers = extensionVideoHeaders,
                            referer = referer,
                            httpClient = extensionOkHttpClient,
                        )
                        engine.prepare()
                        engine.playWhenReady = wasPlaying
                        if (engine is ExoPlayerEngine) {
                            com.blissless.tensei.player.stream.PlayerData.exoPlayer = engine.getExoPlayer()
                        }
                        s.pendingQualityChange = null
                        s.savedPositionForQuality = 0L
                    }
                }
                if (state == PlayerEngine.STATE_ENDED) {
                    onClearPlaybackPosition?.invoke(animeId, currentEpisode)
                    if (autoPlayNextEpisode && onNextEpisode != null && !s.isChangingServer) {
                        if (isLatestEpisode) {
                            context.toast(context.getString(R.string.player_latest_episode_watched))
                        } else {
                            onNextEpisode.invoke()
                        }
                    }
                }
            }
        })
    }

    DisposableEffect(engine) {
        com.blissless.tensei.player.stream.PlayerData.playerEngine = engine
        if (engine is ExoPlayerEngine) {
            com.blissless.tensei.player.stream.PlayerData.exoPlayer = engine.getExoPlayer()
        }
        onDispose {
            engine.stop()
            engine.release()
            com.blissless.tensei.player.stream.PlayerData.exoPlayer = null
            com.blissless.tensei.player.stream.PlayerData.playerEngine = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (activity as? com.blissless.tensei.MainActivity)?.releasePiPMediaSession()
            onSavePosition?.invoke(s.currentPosition, s.duration)
            activity?.window?.let { window ->
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
                // Restore system brightness when leaving the player
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (discordRichPresence) {
                com.blissless.tensei.discord.DiscordRichPresence.clearPresence()
            }
        }
    }

    // Discord Rich Presence — set when playback starts, update countdown periodically
    LaunchedEffect(s.hasPlaybackStarted, discordRichPresence, animeName, currentEpisode, totalEpisodes) {
        if (s.hasPlaybackStarted && discordRichPresence && animeName.isNotEmpty()) {
            com.blissless.tensei.discord.DiscordRichPresence.connect()
            // Wait for s.duration to become available before sending presence
            while (engine.duration <= 0) {
                delay(250L.milliseconds)
            }
            com.blissless.tensei.discord.DiscordRichPresence.setAnimePresence(
                animeName = animeName,
                episode = currentEpisode,
                totalEpisodes = totalEpisodes,
                durationMs = engine.duration,
                currentPositionMs = engine.currentPosition,
            )
            while (true) {
                delay(30_000L.milliseconds)
                com.blissless.tensei.discord.DiscordRichPresence.setAnimePresence(
                    animeName = animeName,
                    episode = currentEpisode,
                    totalEpisodes = totalEpisodes,
                    durationMs = engine.duration,
                    currentPositionMs = engine.currentPosition,
                )
            }
        }
    }

    LaunchedEffect(videoUrl, s.serverChangeTrigger) {
        s.hasError = false
        s.playbackError = null
        s.hasRestoredPosition = false
        s.hasSkippedIntro = false
        s.hasSkippedOutro = false
        s.introEnteredTime = 0L
        s.creditsEnteredTime = 0L
        s.hasTriggeredPrefetch = false
        s.isChangingServer = false
        s.hasPlaybackStarted = false
        s.bufferedPosition = 0L
        s.maxBufferedPosition = 0L
        s.isOffline = false
        s.seekRetryCount = 0
        s.isInitialLoading = true

        // No explicit stop()/clearMediaItems() here: loadMedia() replaces the
        // current file (ExoPlayer releases and rebuilds the player).

        // Use pendingSeekPosition for refresh-based seeks, otherwise restore savedPosition.
        // pendingSeekPosition is cleared externally (onBackClick / new episode load) to
        // avoid races when multiple consecutive seeks trigger overlapping refreshes.
        val restorePosition = pendingSeekPosition ?: savedPosition
        val startPositionMs = if (restorePosition > 0) restorePosition else 0L

        val subtitleConfigs = if (s.subtitlesEnabled && subtitleTracks.isNotEmpty()) {
            subtitleTracks.mapIndexed { index, track ->
                SubtitleConfig(
                    url = track.url,
                    mimeType = subtitleMimeType(track.url),
                    language = track.lang,
                    selected = index == s.selectedSubtitleIndex,
                )
            }
        } else if (subtitleUrl != null) {
            listOf(SubtitleConfig(
                url = subtitleUrl,
                mimeType = subtitleMimeType(subtitleUrl),
                language = "en",
                selected = true,
            ))
        } else {
            emptyList()
        }

        engine.loadMedia(
            url = videoUrl,
            mimeType = null, // engine auto-detects
            startPositionMs = startPositionMs,
            subtitleConfigs = subtitleConfigs,
            headers = extensionVideoHeaders,
            referer = referer,
            httpClient = extensionOkHttpClient,
        )
        engine.prepare()

        // The engine creates its backing ExoPlayer during loadMedia(); keep
        // PlayerData in sync for PiP compatibility.
        if (engine is ExoPlayerEngine) {
            com.blissless.tensei.player.stream.PlayerData.exoPlayer = engine.getExoPlayer()
        }

        s.hasPlaybackStarted = true

        s.hasTriggeredProgressUpdate = false
        s.currentPosition = startPositionMs
        s.sliderValue = startPositionMs.toFloat()
    }

    LaunchedEffect(engine.playbackState, s.hasRestoredPosition, videoUrl) {
        if (engine.playbackState == PlayerEngine.STATE_READY && s.hasPlaybackStarted && !s.hasRestoredPosition) {
            s.hasRestoredPosition = true
            // Start playback after seek
            engine.playWhenReady = true
        }
    }

    LaunchedEffect(s.isPlaying, s.hasTriggeredPrefetch, s.hasError, isLatestEpisode) {
        if (s.isPlaying && !s.hasTriggeredPrefetch && !s.hasError && onPrefetchAdjacent != null && !isLatestEpisode) {
            delay(5000.milliseconds)
            if (!s.hasTriggeredPrefetch && s.isPlaying && !s.hasError) {
                s.hasTriggeredPrefetch = true
                onPrefetchAdjacent.invoke()
            }
        }
    }

    LaunchedEffect(videoUrl, isFallbackStream) {
        if (isFallbackStream && !s.hasShownFallbackToast && videoUrl.isNotEmpty()) {
            s.hasShownFallbackToast = true
            val message = if (requestedCategory == "dub") context.getString(R.string.player_dub_not_available_playing_sub) else context.getString(R.string.player_sub_not_available_playing_dub)
            context.longToast(message)
        }
    }

    // Prefetch next extension episode on first playback
    LaunchedEffect(s.hasPlaybackStarted) {
        if (s.hasPlaybackStarted && extensionServers.isNotEmpty()) {
            onPrefetchNextExtensionEpisode?.invoke()
        }
    }

    LaunchedEffect(engine, videoUrl) {
        while (true) {
            delay(500.milliseconds)
            if (!s.isDragging && !s.isManuallySeeking) {
                s.currentPosition = engine.currentPosition
                s.duration = engine.duration
                s.bufferedPosition = engine.bufferedPosition
                s.maxBufferedPosition = s.bufferedPosition
                // Self-heal: a seek sets the buffering intent optimistically, and in-buffer
                // seeks may never emit a state change, so resync it from the engine here.
                if (!s.isOffline) {
                    s.bufferIntent = engine.playbackState == PlayerEngine.STATE_BUFFERING
                }
                if (s.duration > 0) {
                    s.sliderValue = s.currentPosition.toFloat()
                    if (s.actualEpisodeLength == null && s.duration > 60000 && engine.playbackState == PlayerEngine.STATE_READY) {
                        s.actualEpisodeLength = (s.duration / 1000).toInt()
                    }
                }
            }
        }
    }

    // FALLBACK: Only fetch from AnimeSkip/AnimeThemes if Animekai timestamps are NOT available
    LaunchedEffect(
        s.actualEpisodeLength,
        videoUrl,
        malId,
        animeYear,
        animeName,
        animekaiTimestamps?.hasTimestamps()
    ) {
        val epLength = s.actualEpisodeLength
        if (epLength == null || s.hasFetchedTimestamps) return@LaunchedEffect

        if (animekaiTimestamps?.hasTimestamps() == true) {
            s.hasFetchedTimestamps = true
            return@LaunchedEffect
        }

        s.isFetchingTimestamps = true

        withContext(Dispatchers.IO) {
            try {
                val timestamps = if (malId > 0) {
                    animeSkipService.getSkipTimestampsWithFallback(
                        malId = malId,
                        episodeNumber = currentEpisode,
                        episodeLength = epLength,
                        animeName = animeName,
                        animeYear = animeYear,
                        animeId = animeId
                    )
                } else if (animeName.isNotEmpty()) {
                    animeSkipService.getSkipTimestampsByName(
                        animeName = animeName,
                        episodeNumber = currentEpisode,
                        episodeLength = epLength,
                        year = animeYear
                    )
                } else null

                if (timestamps != null && timestamps.hasTimestamps()) {
                    s.episodeTimestamps = timestamps
                }
            } catch (_: Exception) {
            }
        }

        s.isFetchingTimestamps = false
        s.hasFetchedTimestamps = true
    }

    LaunchedEffect(s.currentPosition, effectiveTimestamps, s.hasError, s.isManuallySeeking, s.isChangingServer, s.isDragging, s.isPlaying, s.controlsVisible) {
        if (s.hasError || s.isChangingServer || s.isDragging) return@LaunchedEffect

        val ts = effectiveTimestamps
        val posSeconds = s.currentPosition / 1000

        if (ts.introStart != null && ts.introEnd != null) {
            val isInIntro = posSeconds >= ts.introStart && posSeconds < ts.introEnd
            if (isInIntro) {
                if (autoSkipOpening && !s.hasSkippedIntro && !s.isManuallySeeking) {
                    engine.seekTo(ts.introEnd * 1000L)
                    s.hasSkippedIntro = true
                }
                if (!autoSkipOpening) {
                    if (s.introEnteredTime == 0L) {
                        s.introEnteredTime = System.currentTimeMillis()
                    }
                    val elapsed = System.currentTimeMillis() - s.introEnteredTime
                    val shouldShow = s.controlsVisible || !s.isPlaying || elapsed < 5000
                    s.showSkipOpeningButton = shouldShow
                } else {
                    s.showSkipOpeningButton = false
                }
            } else {
                if (s.introEnteredTime != 0L) {
                }
                s.introEnteredTime = 0L
                s.showSkipOpeningButton = false
            }
        }

        if (ts.creditsStart != null && onNextEpisode != null) {
            val isInCredits = posSeconds >= ts.creditsStart
            if (isInCredits) {
                if (autoSkipEnding && !s.hasSkippedOutro && !s.isManuallySeeking) {
                    if (isLatestEpisode) {
                        context.toast(context.getString(R.string.player_latest_episode_watched))
                    } else {
                        onNextEpisode.invoke()
                    }
                    s.hasSkippedOutro = true
                }
                if (!autoSkipEnding) {
                    if (s.creditsEnteredTime == 0L) {
                        s.creditsEnteredTime = System.currentTimeMillis()
                    }
                    val elapsed = System.currentTimeMillis() - s.creditsEnteredTime
                    val shouldShow = s.controlsVisible || !s.isPlaying || elapsed < 5000
                    s.showSkipEndingButton = shouldShow
                } else {
                    s.showSkipEndingButton = false
                }
            } else {
                if (s.creditsEnteredTime != 0L) {
                }
                s.creditsEnteredTime = 0L
                s.showSkipEndingButton = false
            }
        }
    }

    LaunchedEffect(engine, s.hasTriggeredProgressUpdate) {
        while (!s.hasTriggeredProgressUpdate) {
            delay(1000.milliseconds)
            if (engine.playbackState == PlayerEngine.STATE_READY && engine.duration > 0) {
                val percentage = ((engine.currentPosition.toFloat() / engine.duration) * 100).toInt()
                onProgressUpdate(percentage)
            }
        }
    }
    LaunchedEffect(s.showControls, s.isPlaying, s.isDragging, s.hasError, s.showServerMenu, s.showQualityMenu, s.showSpeedMenu, s.showSubtitleMenu, s.showPlayerSettings, s.isManuallySeeking, s.fastHideUntil) {
        if (s.showControls && s.isPlaying && !s.isDragging && !s.hasError && !s.showServerMenu && !s.showQualityMenu && !s.showSpeedMenu && !s.showSubtitleMenu && !s.showPlayerSettings && !s.isManuallySeeking) {
            // Right after a seek the user is done interacting: hide much sooner than usual.
            val fastHide = System.currentTimeMillis() < s.fastHideUntil
            delay((if (fastHide) 700 else 2000).milliseconds)
            if (s.showControls && !s.isDragging && !s.hasError && s.isPlaying && !s.showServerMenu && !s.showSpeedMenu && !s.showSubtitleMenu && !s.showPlayerSettings && !s.isManuallySeeking) {
                s.showControls = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.stop()
            engine.clearMediaItems()
            engine.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, engine) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val mainAct = activity as? com.blissless.tensei.MainActivity
                val inPiP = mainAct?.isInPiPMode?.value == true
                val autoEnterPiP = mainAct?.shouldAutoEnterPiP == true
                if (!inPiP && !autoEnterPiP) {
                    engine.pause()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(s.pendingAutoRetry) {
        val target = s.pendingAutoRetry ?: return@LaunchedEffect
        s.pendingAutoRetry = null
        s.isChangingServer = true
        s.hasPlaybackStarted = false
        s.hasError = false
        s.playbackError = null
        if (extensionServers.isNotEmpty()) {
            onExtensionServerChange?.invoke(target)
        } else {
            handleServerChange(target, currentCategory)
        }
    }
}
