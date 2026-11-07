package com.blissless.tensei.data.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.core.content.edit
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.blissless.tensei.data.models.AiringCacheData
import com.blissless.tensei.data.models.AiringScheduleAnime
import com.blissless.tensei.data.models.CharacterData
import com.blissless.tensei.data.models.DetailedAnimeStaffEdge
import com.blissless.tensei.data.models.StaffData
import com.blissless.tensei.data.models.AniwatchStreamResult
import com.blissless.tensei.data.models.CachedEpisodeInfo
import com.blissless.tensei.data.models.CachedExtensionStream
import com.blissless.tensei.data.models.CachedQuality
import com.blissless.tensei.data.models.CachedServer
import com.blissless.tensei.data.models.CachedStream
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.EpisodeStreams
import com.blissless.tensei.data.models.ExploreCacheData
import com.blissless.tensei.data.models.HomeCacheData
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.PlaybackPositionCache
import com.blissless.tensei.data.models.QualityOption
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.data.models.StreamCacheData
import com.blissless.tensei.data.models.StreamCacheEntry
import com.blissless.tensei.data.models.TmdbEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import com.blissless.tensei.util.ErrorHandler
import com.blissless.tensei.data.anime.json

// Playback position persistence (resume points, durations,
// continue-watching entries).

fun CacheManager.loadPlaybackPositions() {
    try {
        val cachedData = sharedPreferences.getString(CacheManager.CACHE_PLAYBACK_POSITIONS, null) ?: return
        val cacheData = json.decodeFromString<PlaybackPositionCache>(cachedData)
        _playbackPositions.value = cacheData.positions
        _playbackDurations.value = cacheData.durations
        _startedAt.value = cacheData.startedAt
    } catch (_: Exception) {
    }
}

fun CacheManager.savePlaybackPosition(animeId: Int, episode: Int, position: Long, duration: Long = 0L, isOffline: Boolean = false) {
    val key = "${animeId}_$episode${if (isOffline) "_offline" else ""}"
    // Playback that reached the last few seconds is finished, not something to come back to:
    // never create a Continue Watching entry for it, and drop an existing one when the user
    // scrubs or watches to the end. Unknown duration (0) can't be judged, so it saves as usual.
    if (duration > 0L && position > 0L && duration - position <= CacheManager.PLAYBACK_FINISHED_TAIL_MS) {
        clearPlaybackPosition(animeId, episode, isOffline)
        return
    }
    _playbackPositions.value += (key to position)
    if (duration > 0L) {
        _playbackDurations.value += (key to duration)
    }
    if (!_startedAt.value.containsKey(key) && position > 0L) {
        _startedAt.value += (key to System.currentTimeMillis())
    }
    try {
        val jsonString = json.encodeToString(
            PlaybackPositionCache.serializer(),
            PlaybackPositionCache(_playbackPositions.value, _playbackDurations.value, _startedAt.value)
        )
        sharedPreferences.edit { putString(CacheManager.CACHE_PLAYBACK_POSITIONS, jsonString) }
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}

fun CacheManager.getPlaybackPosition(animeId: Int, episode: Int, isOffline: Boolean = false): Long = _playbackPositions.value["${animeId}_$episode${if (isOffline) "_offline" else ""}"] ?: 0L

fun CacheManager.getPlaybackDuration(animeId: Int, episode: Int, isOffline: Boolean = false): Long = _playbackDurations.value["${animeId}_$episode${if (isOffline) "_offline" else ""}"] ?: 0L

fun CacheManager.clearPlaybackPosition(animeId: Int, episode: Int, isOffline: Boolean = false) {
    val key = "${animeId}_$episode${if (isOffline) "_offline" else ""}"
    if (_playbackPositions.value.containsKey(key)) {
        _playbackPositions.value -= key
        _playbackDurations.value -= key
        _startedAt.value -= key
        sharedPreferences.edit {
            val jsonString = json.encodeToString(
                PlaybackPositionCache.serializer(),
                PlaybackPositionCache(_playbackPositions.value, _playbackDurations.value, _startedAt.value)
            )
            putString(CacheManager.CACHE_PLAYBACK_POSITIONS, jsonString)
        }
    }
}

fun CacheManager.clearAllPlaybackPositionsForAnime(animeId: Int) {
    val prefix = "${animeId}_"
    val newMap = _playbackPositions.value.filterKeys { !it.startsWith(prefix) }
    _playbackPositions.value = newMap
    _playbackDurations.value = _playbackDurations.value.filterKeys { !it.startsWith(prefix) }
    _startedAt.value = _startedAt.value.filterKeys { !it.startsWith(prefix) }
    sharedPreferences.edit {
        val jsonString = json.encodeToString(
            PlaybackPositionCache.serializer(),
            PlaybackPositionCache(_playbackPositions.value, _playbackDurations.value, _startedAt.value)
        )
        putString(CacheManager.CACHE_PLAYBACK_POSITIONS, jsonString)
    }
}

fun CacheManager.removeContinueWatchingEntry(animeId: Int, episode: Int, isOffline: Boolean = false) {
    clearPlaybackPosition(animeId, episode, isOffline)
}
