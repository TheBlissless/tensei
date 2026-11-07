package com.blissless.tensei.ui.screens.player

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.EpisodeTimestamps
import com.blissless.tensei.player.engine.EmbeddedSubtitleTrack
import kotlinx.coroutines.Job

/**
 * Holder for the mutable state of the in-player screen
 * ([PlayerScreen]) — previously ~70 individual `remember` locals inside
 * the composable.
 *
 * As with [com.blissless.tensei.player.playback.PlaybackState], each
 * property is a `mutableStateOf` delegate so snapshot reads keep working
 * unchanged, while the player UI can be split across several files that
 * all operate on this one holder.
 *
 * [showTimeLeft] is the one exception: it is backed by a
 * [MutableState] supplied by the composable so it can be created with
 * `rememberSaveable` there and survive configuration changes.
 *
 * Stream-scoped fields (timestamps, skip flags, restore/prefetch markers)
 * are reset by [resetForNewStream], which the composable invokes through a
 * `remember(videoUrl)` key — reproducing the original keyed `remember`
 * reset semantics exactly (reset during composition, when the key changes).
 */
class PlayerScreenState(
    initialSubtitlesEnabled: Boolean,
    initialSelectedQuality: String,
    initialSubtitleProfile: SubtitleProfileData,
    showTimeLeftState: MutableState<Boolean>,
) {
    // ─── Progress / error tracking ──────────────────────────────────────
    var hasTriggeredProgressUpdate by mutableStateOf(false)
    var playbackError by mutableStateOf<String?>(null)
    var hasError by mutableStateOf(false)
    var isChangingServer by mutableStateOf(false)
    var serverChangeTrigger by mutableIntStateOf(0)
    var hasPlaybackStarted by mutableStateOf(false)
    var isManuallySeeking by mutableStateOf(false)
    var seekRetryCount by mutableIntStateOf(0)
    var isInitialLoading by mutableStateOf(false)
    var pendingAutoRetry by mutableStateOf<String?>(null)

    /** Servers already tried automatically for the current stream. */
    val autoRetryServers = mutableSetOf<String>()

    var resizeModeIndex by mutableIntStateOf(0)

    // ─── Playback status ────────────────────────────────────────────────
    var showControls by mutableStateOf(true)
    var isPlaying by mutableStateOf(true)
    var isBuffering by mutableStateOf(false)

    /** Raw "should be buffering" intent; [isBuffering] only follows it after a debounce. */
    var bufferIntent by mutableStateOf(false)
    var isOffline by mutableStateOf(false)
    var controlsVisible by mutableStateOf(false)
    var currentPosition by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var bufferedPosition by mutableLongStateOf(0L)
    var maxBufferedPosition by mutableLongStateOf(0L)

    // ─── Menu / dialog visibility ───────────────────────────────────────
    var showServerMenu by mutableStateOf(false)
    var showQualityMenu by mutableStateOf(false)
    var showSpeedMenu by mutableStateOf(false)
    var showSubtitleMenu by mutableStateOf(false)
    var showPlayerSettings by mutableStateOf(false)
    var showSubtitleSettings by mutableStateOf(false)

    // ─── Subtitles ──────────────────────────────────────────────────────
    var subtitlesEnabled by mutableStateOf(initialSubtitlesEnabled)
    var selectedSubtitleIndex by mutableIntStateOf(0)
    var subtitleProfileData by mutableStateOf(initialSubtitleProfile)
    var subtitleViewRef by mutableStateOf<androidx.media3.ui.SubtitleView?>(null)
    var embeddedSubtitleTracks by mutableStateOf<List<EmbeddedSubtitleTrack>>(emptyList())
    var selectedEmbeddedTrackIndex by mutableIntStateOf(-1)

    // ─── Gestures / scrubbing ───────────────────────────────────────────
    var accumulatedSkipMs by mutableLongStateOf(0L)
    var lastTapTime by mutableLongStateOf(0L)
    var sliderValue by mutableFloatStateOf(0f)
    var isDragging by mutableStateOf(false)
    var wasPlayingBeforeScrub by mutableStateOf(false)

    /** YouTube-style timestamp: the left indicator flips between elapsed/total and time left. */
    var showTimeLeft by showTimeLeftState

    var showSkipIndicator by mutableStateOf(false)
    var skipIndicatorText by mutableStateOf("")
    var skipIsForward by mutableStateOf(true)
    var skipResetJob by mutableStateOf<Job?>(null)

    /** After a seek the controls hide quickly instead of waiting out the full auto-hide delay. */
    var fastHideUntil by mutableLongStateOf(0L)

    // ─── Volume / brightness gestures ───────────────────────────────────
    var playerVolume by mutableFloatStateOf(1f)
    var currentBrightness by mutableFloatStateOf(0.5f)
    var showVolumeOverlay by mutableStateOf(false)
    var showBrightnessOverlay by mutableStateOf(false)

    // ─── Quality ────────────────────────────────────────────────────────
    var selectedQuality by mutableStateOf(initialSelectedQuality)
    var pendingQualityChange by mutableStateOf<String?>(null)
    var savedPositionForQuality by mutableLongStateOf(0L)

    // ─── Stream-scoped state (reset by resetForNewStream) ───────────────
    var episodeTimestamps by mutableStateOf<EpisodeTimestamps?>(null)
    var isFetchingTimestamps by mutableStateOf(false)
    var hasSkippedIntro by mutableStateOf(false)
    var hasSkippedOutro by mutableStateOf(false)
    var showSkipOpeningButton by mutableStateOf(false)
    var showSkipEndingButton by mutableStateOf(false)
    var introEnteredTime by mutableLongStateOf(0L)
    var creditsEnteredTime by mutableLongStateOf(0L)
    var hasFetchedTimestamps by mutableStateOf(false)
    var actualEpisodeLength by mutableStateOf<Int?>(null)
    var hasShownFallbackToast by mutableStateOf(false)
    var hasRestoredPosition by mutableStateOf(false)
    var hasTriggeredPrefetch by mutableStateOf(false)

    /** Resets the stream-scoped fields; invoked by the composable when videoUrl changes. */
    fun resetForNewStream() {
        episodeTimestamps = null
        isFetchingTimestamps = false
        hasSkippedIntro = false
        hasSkippedOutro = false
        showSkipOpeningButton = false
        showSkipEndingButton = false
        introEnteredTime = 0L
        creditsEnteredTime = 0L
        hasFetchedTimestamps = false
        actualEpisodeLength = null
        hasShownFallbackToast = false
        hasRestoredPosition = false
        hasTriggeredPrefetch = false
    }
}
