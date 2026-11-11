package com.blissless.tensei.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.blissless.tensei.player.playback.PlaybackController
import com.blissless.tensei.player.playback.PlaybackState
import com.blissless.tensei.player.stream.PlayerData
import com.blissless.tensei.ui.navigation.OverlayState
import com.blissless.tensei.ui.screens.episode.RichEpisodeList
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.cacheTmdbEpisodes
import com.blissless.tensei.viewmodel.clearPlaybackPosition
import com.blissless.tensei.viewmodel.fetchTmdbEpisodes
import com.blissless.tensei.viewmodel.getCachedTmdbEpisodes
import com.blissless.tensei.viewmodel.getCacheDataSourceFactory
import com.blissless.tensei.viewmodel.savePlaybackPosition
import com.blissless.tensei.viewmodel.setAutoPlayNextEpisode
import com.blissless.tensei.viewmodel.setSwipeBrightness
import com.blissless.tensei.viewmodel.setSwipeSwap
import com.blissless.tensei.viewmodel.setSwipeVolume
import com.blissless.tensei.viewmodel.setSupportsPiP
import com.blissless.tensei.viewmodel.updateAnimeProgress

/**
 * Hosts the in-app player region of the root screen: the [PlayerScreen]
 * (kept alive at a fixed composition position so toggling fullscreen does
 * not restart playback), the episode list below it in mini mode, and the
 * back handling that belongs to the player.
 *
 * Extracted verbatim from the [com.blissless.tensei.MainScreen] composable;
 * playback state lives in [state] and is driven by [controller].
 */
@Composable
fun PlayerHost(
    state: PlaybackState,
    controller: PlaybackController,
    viewModel: MainViewModel,
    isOled: Boolean,
    preferEnglishTitles: Boolean,
    supportsPiP: Boolean,
    isInPiPMode: Boolean,
    autoSkipOpening: Boolean,
    autoSkipEnding: Boolean,
    autoPlayNextEpisode: Boolean,
    forwardSkipSeconds: Int,
    backwardSkipSeconds: Int,
    swipeVolume: Boolean,
    swipeBrightness: Boolean,
    swipeSwap: Boolean,
    disableMaterialColors: Boolean,
    showBufferIndicator: Boolean,
    bufferAheadSeconds: Int,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    animeProgressMap: Map<Int, Int>,
    overlayState: OverlayState,
    onPlayerBoundsChanged: (Int, Int, Int, Int) -> Unit,
    scheduleDialogOpen: Boolean,
    onSetScheduleDialogOpen: (Boolean) -> Unit,
    onClearAnimeStack: () -> Unit,
) {
    val anime = state.currentAnime
    if (anime != null) {
        val released = anime.latestEpisode ?: anime.totalEpisodes
        // Movies have no meaningful episode title/number. Prefer the provider format, but
        // fall back to a single episode when a play path lost the format (local-only tracks).
        val isMovie = anime.format?.equals("MOVIE", ignoreCase = true) == true ||
            (anime.format.isNullOrBlank() && anime.totalEpisodes == 1)
        val tmdbEpisodes by viewModel.tmdbEpisodeCache.collectAsState()
        val playerDisplayTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
        val hideEpisodeDescription by viewModel.hideEpisodeDescription.collectAsState()
        val discordRichPresence by viewModel.discordRichPresence.collectAsState()

        @Composable
        fun PlayerUi() {
        PlayerScreen(
            videoUrl = state.currentVideoUrl!!,
            referer = state.currentReferer,
            subtitleUrl = state.currentSubtitleUrl,
            subtitleTracks = state.currentSubtitleTracks,
            currentEpisode = state.currentEpisode,
            totalEpisodes = state.totalEpisodes,
            isMovie = isMovie,
            animeName = playerDisplayTitle,
            episodeTitle = state.currentEpisodeTitle,
            animeId = anime.id,
            malId = anime.malId ?: 0,
            animeYear = anime.year,
            isLoadingStream = state.isLoadingStream,
            episodeInfo = state.currentEpisodeInfo,
            currentServerName = state.currentServerName,
            currentCategory = state.currentCategory,
            isFallbackStream = state.isFallbackStream && !state.isManualServerChange,
            requestedCategory = state.requestedCategory,
            forwardSkipSeconds = forwardSkipSeconds,
            backwardSkipSeconds = backwardSkipSeconds,
            savedPosition = state.savedPlaybackPosition,
            pendingSeekPosition = state.pendingSeekPosition,
            onSetPendingSeekPosition = {
                state.pendingSeekPosition = it
                if (it != null) state.isAutoRefreshing = false  // Allow new refresh on consecutive seeks
            },
            currentQuality = state.currentQuality,
            // Animekai timestamps (PRIMARY source)
            animekaiIntroStart = state.animekaiIntroStart,
            animekaiIntroEnd = state.animekaiIntroEnd,
            animekaiOutroStart = state.animekaiOutroStart,
            animekaiOutroEnd = state.animekaiOutroEnd,
            onSavePosition = { position, duration ->
                viewModel.savePlaybackPosition(anime.id, state.currentEpisode, position, duration)
            },
            onClearPlaybackPosition = { _, episode ->
                viewModel.clearPlaybackPosition(anime.id, episode)
            },
            onPositionSaved = { position ->
                state.savedPlaybackPosition = position
            },
            onProgressUpdate = { percentage ->
                val trackingPercent = viewModel.trackingPercentage.value
                if (percentage >= trackingPercent && anime.id > 0) {
                    viewModel.updateAnimeProgress(anime.id, state.currentEpisode)
                    // Keep the Continue Watching card after auto-sync: do NOT clear the
                    // playback position here. The savePlaybackPosition call (which fires
                    // on every progress tick via onSavePosition above) already handles the
                    // "user watched to the very end" case — it auto-clears the position
                    // when the playback is within PLAYBACK_FINISHED_TAIL_MS (10s) of the
                    // duration. This mirrors the manga side (onMangaScrollProgress
                    // explicitly KEEPS the scroll position after the threshold is crossed).
                    // A user who crosses the tracking threshold but exits before the last
                    // few seconds keeps a Continue Watching card that resumes mid-episode,
                    // which matches their mental model of "I haven't finished this yet".
                    // The episode's progress is still incremented via updateAnimeProgress
                    // above, so the home screen's progress chip (e.g. "5 / 12") and the
                    // detail screen's chapter list (e.g. ep 5 marked as watched) reflect
                    // the auto-sync correctly.
                    if (!state.hasPrefetchedNextOnTracking && state.currentEpisode < released) {
                        state.hasPrefetchedNextOnTracking = true
                        controller.prefetchExtensionNextEpisode()
                    }
                }
            },
            onPreviousEpisode = if (state.currentEpisode > 1) ({ controller.onPreviousEpisode() }) else null,
            onNextEpisode = if (state.currentEpisode < released) ({ controller.onNextEpisode() }) else null,
            isLatestEpisode = released in 1..state.currentEpisode,
            onPlaybackError = { controller.onPlaybackError() },
            onInvalidateStreamCache = { controller.invalidateCurrentStreamCache() },
            onRefreshStream = {
                if (!state.isAutoRefreshing) {
                    state.isAutoRefreshing = true
                    state.currentAnime?.let { a ->
                        controller.loadAndPlayEpisode(a, state.currentEpisode, isAutoRefresh = true)
                    }
                }
            },
            autoSkipOpening = autoSkipOpening,
            autoSkipEnding = autoSkipEnding,
            autoPlayNextEpisode = autoPlayNextEpisode,
            onAutoPlayNextEpisodeChanged = { viewModel.setAutoPlayNextEpisode(it) },
            swipeVolume = swipeVolume,
            swipeBrightness = swipeBrightness,
            swipeSwap = swipeSwap,
            onSwipeVolumeChange = { viewModel.setSwipeVolume(it) },
            onSwipeBrightnessChange = { viewModel.setSwipeBrightness(it) },
            onSwipeSwapChange = { viewModel.setSwipeSwap(it) },
            disableMaterialColors = disableMaterialColors,
            isOled = isOled,
            showBufferIndicator = showBufferIndicator,
            bufferAheadSeconds = bufferAheadSeconds,
            onGetCacheDataSourceFactory = { referer ->
                viewModel.getCacheDataSourceFactory(referer, state.extensionOkHttpClient, state.extensionVideoHeaders)
            },
            onBackClick = {
                state.playerFullscreen = true
                state.showPlayer = false
                state.currentVideoUrl = null
                state.pendingSeekPosition = null
                state.clearExtensionPlayback()
            },
            isFullscreen = state.playerFullscreen,
            onFullscreenChanged = { state.playerFullscreen = it },
            extensionOkHttpClient = state.extensionOkHttpClient,
            extensionVideoHeaders = state.extensionVideoHeaders,
            extensionServers = state.extensionServers,
            extensionName = state.extensionName,
            onExtensionServerChange = { hosterName -> controller.handleExtensionServerChange(hosterName) },
            onPrefetchNextExtensionEpisode = { controller.prefetchExtensionNextEpisode() },
            isTorrentStream = state.torrentStreamServer != null,
            onTorrentSeek = if (state.torrentStreamServer != null) ({ posMs, durMs ->
                val fileSize = state.currentTorrentFileSize
                if (fileSize > 0) state.torrentEngine.prioritizeForSeek(posMs, fileSize, durMs)
            }) else null,
            supportsPiP = supportsPiP,
            onPiPToggle = { viewModel.setSupportsPiP(it) },
            isInPiPMode = isInPiPMode,
            onPlayerBoundsChanged = onPlayerBoundsChanged,
            discordRichPresence = discordRichPresence,
        )
        }

        // Preload TMDB episode metadata (titles/descriptions/images) for the list
        // shown below the player when it leaves fullscreen.
        LaunchedEffect(anime.id, state.playerFullscreen) {
            if (!state.playerFullscreen && viewModel.getCachedTmdbEpisodes(anime.id) == null) {
                try {
                    val episodes = viewModel.fetchTmdbEpisodes(
                        anime.title, anime.id, anime.year, anime.format,
                        animeEpisodes = anime.totalEpisodes.takeIf { it > 0 },
                    )
                    viewModel.cacheTmdbEpisodes(anime.id, episodes)
                } catch (_: Exception) {}
            }
        }

        // Player is always composed at the same position; only its size changes
        // on fullscreen toggles so playback is not restarted.
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black)
        ) {
        Box(
            modifier = if (state.playerFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            }
        ) {
            PlayerUi()
        }
        if (!state.playerFullscreen) {
            RichEpisodeList(
                episodeCount = maxOf(state.totalEpisodes, state.currentEpisode, released),
                releasedCount = released,
                currentEpisode = state.currentEpisode,
                currentProgress = maxOf(animeProgressMap[anime.id] ?: 0, state.currentEpisode - 1),
                isOled = isOled,
                tmdbEpisodes = tmdbEpisodes[anime.id] ?: emptyList(),
                playbackPositions = playbackPositions,
                playbackDurations = playbackDurations,
                animeId = anime.id,
                animeTitle = playerDisplayTitle,
                episodeTitle = state.currentEpisodeTitle,
                isMovie = isMovie,
                hideDescription = hideEpisodeDescription,
                onEpisodeSelect = { ep -> controller.loadAndPlayEpisode(anime, ep) },
                onClose = {
                    state.showPlayer = false
                    state.currentVideoUrl = null
                    state.pendingSeekPosition = null
                    state.clearExtensionPlayback()
                },
                onEnterFullscreen = { state.playerFullscreen = true },
            )
        }
        }
    }

    BackHandler {
        when {
            scheduleDialogOpen -> {
                onSetScheduleDialogOpen(false)
            }
            overlayState !is OverlayState.None -> {
                onClearAnimeStack()
            }
            else -> {
                if (state.showPlayer && state.currentVideoUrl != null) {
                    state.playerFullscreen = true
                    state.showPlayer = false
                    state.currentVideoUrl = null
                    state.clearExtensionPlayback()
                }
            }
        }
    }
}

/** Clears the per-playback extension references so the next playback starts clean. */
private fun PlaybackState.clearExtensionPlayback() {
    extensionOkHttpClient = null
    extensionVideoHeaders = emptyMap()
    extensionHosters = null
    extensionServers = emptyList()
    extensionStreamEntries = emptyList()
    cachedExtensionNext = null
    PlayerData.extensionSource = null
    PlayerData.extensionEpisode = null
    PlayerData.allHosters = emptyList()
}
