package com.blissless.tensei.ui.screens.player

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.MimeTypes
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.player.engine.PlayerEngine
import com.blissless.tensei.player.engine.SubtitleConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Player operations extracted from the [PlayerScreen] composable.
 *
 * Each function is an extension on [PlayerScreenState] and receives the
 * pieces it needs from the current composition (engine, coroutine scope,
 * callbacks) as explicit parameters — so calling sites always pass the
 * fresh values and no stale closures can be captured, exactly matching
 * the original local-function behavior.
 */

/** Maps a subtitle URL to the MIME type ExoPlayer expects for its extension. */
internal fun subtitleMimeType(url: String): String {
    val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
    return when (ext) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.TEXT_VTT
    }
}

/**
 * Seeks to [position] ms.
 *
 * Keeps the last known buffered extent instead of collapsing it to the seek
 * target: the scrub bar's buffer segment would otherwise vanish here and only
 * reappear once the 500ms poll resumes, which reads as a flicker on every
 * scrub/double tap.
 */
internal fun PlayerScreenState.seekToPosition(
    engine: PlayerEngine,
    isTorrentStream: Boolean,
    onTorrentSeek: ((positionMs: Long, durationMs: Long) -> Unit)?,
    position: Long,
) {
    hasError = false
    playbackError = null
    bufferIntent = true
    maxBufferedPosition = maxOf(maxBufferedPosition, position)
    bufferedPosition = position
    if (isTorrentStream) {
        onTorrentSeek?.invoke(position, engine.duration)
        engine.seekTo(position)
        return
    }
    // For non-torrent streams, seekOutsideBuffer handles HLS natively,
    // inside-buffer seeks and clip-based re-prepare for out-of-buffer seeks.
    engine.seekOutsideBuffer(position)
}

/** Seeks by [milliseconds] relative to the displayed position (double-tap zones). */
internal fun PlayerScreenState.seekBy(
    engine: PlayerEngine,
    scope: CoroutineScope,
    isTorrentStream: Boolean,
    onTorrentSeek: ((positionMs: Long, durationMs: Long) -> Unit)?,
    milliseconds: Long,
) {
    isManuallySeeking = true
    seekRetryCount = 0
    fastHideUntil = System.currentTimeMillis() + 2500

    // Show skip indicator (separate from player UI)
    skipIndicatorText = if (milliseconds > 0) "+${abs(milliseconds / 1000)}s" else "-${abs(milliseconds / 1000)}s"
    skipIsForward = milliseconds >= 0
    showSkipIndicator = true

    // Handle accumulated skips within 300ms window
    val now = System.currentTimeMillis()
    if (now - lastTapTime < 300) {
        accumulatedSkipMs += milliseconds
    } else {
        accumulatedSkipMs = milliseconds
    }
    lastTapTime = now

    // Always seek by single skip amount, not accumulated.
    // Use the displayed position instead of engine.currentPosition because the
    // engine may still be seeking from a previous scrub and return a stale value.
    val engineDuration = engine.duration
    val newPosition = if (engineDuration > 0) {
        (currentPosition + milliseconds).coerceIn(0, engineDuration)
    } else {
        (currentPosition + milliseconds).coerceAtLeast(0)
    }
    seekToPosition(engine, isTorrentStream, onTorrentSeek, newPosition)
    currentPosition = newPosition
    sliderValue = newPosition.toFloat()

    // Update text with accumulated skip time
    val totalSeconds = abs(accumulatedSkipMs / 1000)
    skipIndicatorText = if (accumulatedSkipMs > 0) "+${totalSeconds}s" else "-${totalSeconds}s"

    // Schedule reset after 500ms of no taps
    skipResetJob?.cancel()
    skipResetJob = scope.launch {
        delay(500.milliseconds)
        showSkipIndicator = false
        isManuallySeeking = false
        accumulatedSkipMs = 0L
    }
}

/** Performs a scrub-bar seek (drag release) at [position] ms. */
internal fun PlayerScreenState.performManualSeek(
    engine: PlayerEngine,
    scope: CoroutineScope,
    isTorrentStream: Boolean,
    onTorrentSeek: ((positionMs: Long, durationMs: Long) -> Unit)?,
    position: Long,
) {
    isManuallySeeking = true
    seekRetryCount = 0
    fastHideUntil = System.currentTimeMillis() + 3500
    seekToPosition(engine, isTorrentStream, onTorrentSeek, position)
    currentPosition = position
    sliderValue = position.toFloat()
    skipResetJob?.cancel()
    skipResetJob = scope.launch {
        delay(1500.milliseconds)
        isManuallySeeking = false
    }
}

/**
 * Switches to another server/category: saves the current position first
 * (no explicit stop here — loadMedia replaces the current file on the
 * player), bumps the server-change trigger and forwards to the host.
 */
internal fun PlayerScreenState.handleServerChange(
    engine: PlayerEngine,
    scope: CoroutineScope,
    onSavePosition: ((position: Long, duration: Long) -> Unit)?,
    onPositionSaved: ((position: Long) -> Unit)?,
    onServerChange: ((serverName: String, category: String) -> Unit)?,
    serverName: String,
    category: String,
) {
    isChangingServer = true
    hasPlaybackStarted = false
    hasError = false
    playbackError = null

    // Save current position BEFORE switching.
    val currentDur = engine.duration
    onSavePosition?.invoke(engine.currentPosition, if (currentDur > 0) currentDur else 0L)
    onPositionSaved?.invoke(engine.currentPosition)

    // Small delay before triggering server change to ensure the error popup
    // disappears.
    scope.launch {
        delay(50.milliseconds)
        serverChangeTrigger++
    }
    onServerChange?.invoke(serverName, category)
}

/** Reports a playback error up to the host (cache invalidation + error surface). */
internal fun PlayerScreenState.handlePlaybackError(
    onInvalidateStreamCache: (() -> Unit)?,
    onPlaybackError: (() -> Unit)?,
) {
    onInvalidateStreamCache?.invoke()
    onPlaybackError?.invoke()
}

/**
 * Rebuilds the media source with or without the selected subtitle tracks,
 * preserving the current position and play-when-ready state.
 */
internal fun PlayerScreenState.rebuildWithSubtitles(
    engine: PlayerEngine,
    videoUrl: String,
    subtitleUrl: String?,
    subtitleTracks: List<eu.kanade.tachiyomi.animesource.model.Track>,
    enable: Boolean,
) {
    subtitlesEnabled = enable
    val position = engine.currentPosition
    val playWhenReady = engine.playWhenReady
    val subtitleConfigs = if (subtitlesEnabled && subtitleTracks.isNotEmpty()) {
        subtitleTracks.mapIndexed { index, track ->
            SubtitleConfig(
                url = track.url,
                mimeType = subtitleMimeType(track.url),
                language = track.lang,
                selected = index == selectedSubtitleIndex,
            )
        }
    } else if (subtitlesEnabled && subtitleUrl != null) {
        listOf(
            SubtitleConfig(
                url = subtitleUrl,
                mimeType = subtitleMimeType(subtitleUrl),
                language = "en",
                selected = true,
            )
        )
    } else {
        emptyList()
    }
    if (!enable) {
        engine.disableSubtitles()
    } else {
        selectedEmbeddedTrackIndex = -1
    }
    engine.rebuildWithSubtitles(videoUrl, subtitleConfigs, position, playWhenReady)
}

/** The subtitle style settings of the currently active profile. */
internal fun PlayerScreenState.getActiveSubtitleSettings(): SubtitleSettings {
    val data = subtitleProfileData
    return data.profiles.getOrElse(data.activeProfileIndex) { SubtitleSettings.DEFAULT }
}

/** Persists the subtitle profiles to the app's shared preferences. */
internal fun PlayerScreenState.saveSubtitleProfileData(context: Context, data: SubtitleProfileData) {
    subtitleProfileData = data
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val encoded = json.encodeToString(SubtitleProfileData.serializer(), data)
    context.getSharedPreferences("anilist_prefs", Context.MODE_PRIVATE).edit {
        putString("subtitle_profiles", encoded)
            .putInt("subtitle_active_profile", data.activeProfileIndex)
    }
}
