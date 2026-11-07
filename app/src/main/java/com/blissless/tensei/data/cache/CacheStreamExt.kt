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

// Resolved-stream caches (SharedPreferences) and the in-memory extension
// stream cache.

fun CacheManager.loadStreamCache() {
    try {
        val cachedData = sharedPreferences.getString(CacheManager.CACHE_STREAM_DATA, null) ?: return
        val cacheData = json.decodeFromString<StreamCacheData>(cachedData)
        val now = System.currentTimeMillis()
        val streamMap = mutableMapOf<String, AniwatchStreamResult?>()
        val episodeMap = mutableMapOf<String, EpisodeStreams?>()
        cacheData.entries.filter { (now - it.value.timestamp) < CacheManager.STREAM_CACHE_DURATION_MS }
            .forEach { (key, entry) ->
                streamMap[key] = entry.stream?.let {
                    val qualities = it.qualities.map { q -> QualityOption(q.quality, q.url, q.width) }
                    AniwatchStreamResult(
                        it.url,
                        it.isDirectStream,
                        it.headers,
                        it.subtitleUrl,
                        it.serverName,
                        it.category,
                        qualities,
                        // Skip timestamps
                        it.introStart,
                        it.introEnd,
                        it.outroStart,
                        it.outroEnd
                    )
                }
                entry.episodeInfo?.let { ep ->
                    episodeMap[key] = EpisodeStreams(
                        ep.subServers.map { s ->
                            val quals = s.qualities.map { q -> QualityOption(q.quality, q.url, q.width) }
                            ServerInfo(s.name, s.url, quals)
                        },
                        ep.dubServers.map { s ->
                            val quals = s.qualities.map { q -> QualityOption(q.quality, q.url, q.width) }
                            ServerInfo(s.name, s.url, quals)
                        },
                        ep.animeId, ep.episodeId
                    )
                }
            }
        _prefetchedStreams.value = streamMap
        _prefetchedEpisodeInfo.value = episodeMap
    } catch (_: Exception) {
    }
}

fun CacheManager.saveStreamCache() {
    try {
        val now = System.currentTimeMillis()
        val entries = _prefetchedStreams.value.mapValues { (key, stream) ->
            val ep = _prefetchedEpisodeInfo.value[key]
            StreamCacheEntry(
                stream = stream?.let {
                    val qualities = it.qualities.map { q -> CachedQuality(q.quality, q.url, q.width) }
                    CachedStream(
                        it.url,
                        it.isDirectStream,
                        it.headers,
                        it.subtitleUrl,
                        it.serverName,
                        it.category,
                        qualities,
                        // Skip timestamps
                        it.introStart,
                        it.introEnd,
                        it.outroStart,
                        it.outroEnd
                    )
                },
                episodeInfo = ep?.let { info ->
                    CachedEpisodeInfo(
                        info.subServers.map { s ->
                            val quals = s.qualities.map { q -> CachedQuality(q.quality, q.url, q.width) }
                            CachedServer(s.name, s.url, quals)
                        },
                        info.dubServers.map { s ->
                            val quals = s.qualities.map { q -> CachedQuality(q.quality, q.url, q.width) }
                            CachedServer(s.name, s.url, quals)
                        },
                        info.animeId, info.episodeId
                    )
                },
                timestamp = now
            )
        }
        val jsonString = json.encodeToString(StreamCacheData.serializer(), StreamCacheData(entries))
        sharedPreferences.edit { putString(CacheManager.CACHE_STREAM_DATA, jsonString) }
    } catch (_: Exception) {
    }
}

fun CacheManager.hasStream(key: String): Boolean {
    val exists = _prefetchedStreams.value.containsKey(key)
    return exists
}
/**
 * Clear stream cache for a specific episode.
 * Call this when a stream fails to force refetch on next attempt.
 */

fun CacheManager.invalidateStreamCache(animeId: Int, episode: Int, category: String? = null) {
    val keysToRemove = mutableListOf<String>()
    if (category != null) {
        // Remove specific category cache
        keysToRemove.add("${animeId}_${episode}_$category")
    } else {
        // Remove both sub and dub caches
        keysToRemove.add("${animeId}_${episode}_sub")
        keysToRemove.add("${animeId}_${episode}_dub")
    }
    // Also remove the old-style key
    keysToRemove.add("${animeId}_$episode")
    val newMap = _prefetchedStreams.value.toMutableMap()
    var removed = false
    keysToRemove.forEach { key ->
        if (newMap.containsKey(key)) {
            newMap.remove(key)
            removed = true
        }
    }
    if (removed) {
        _prefetchedStreams.value = newMap
        saveStreamCache()
    }
}

fun CacheManager.getCachedExtensionStream(key: String): CachedExtensionStream? {
    val cached = _extensionStreamCache[key] ?: return null
    val now = System.currentTimeMillis()
    if (now - cached.cachedAt > CacheManager.STREAM_CACHE_DURATION_MS) {
        _extensionStreamCache.remove(key)
        saveExtensionStreamCache()
        return null
    }
    return cached
}

fun CacheManager.cacheExtensionStream(key: String, data: CachedExtensionStream) {
    _extensionStreamCache[key] = data
    saveExtensionStreamCache()
}

fun CacheManager.invalidateExtensionStreamCache(key: String) {
    _extensionStreamCache.remove(key)
    saveExtensionStreamCache()
}

fun CacheManager.clearAnimeExtensionStreamCaches(animeId: Int) {
    val prefix = "${animeId}_"
    val keysToRemove = _extensionStreamCache.keys.filter { it.startsWith(prefix) }
    keysToRemove.forEach { _extensionStreamCache.remove(it) }
    if (keysToRemove.isNotEmpty()) {
        saveExtensionStreamCache()
    }
}

fun CacheManager.clearAllExtensionStreamCaches() {
    _extensionStreamCache.clear()
    saveExtensionStreamCache()
}

fun CacheManager.loadExtensionStreamCache() {
    try {
        val raw = sharedPreferences.getString(CacheManager.CACHE_EXTENSION_STREAMS, null) ?: return
        val parsed = json.decodeFromString<Map<String, CachedExtensionStream>>(raw)
        val now = System.currentTimeMillis()
        _extensionStreamCache.clear()
        _extensionStreamCache.putAll(parsed.filter { (now - it.value.cachedAt) < CacheManager.STREAM_CACHE_DURATION_MS })
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}

fun CacheManager.saveExtensionStreamCache() {
    try {
        val raw = json.encodeToString(_extensionStreamCache)
        sharedPreferences.edit { putString(CacheManager.CACHE_EXTENSION_STREAMS, raw) }
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}
// Time-based expiration for detailed anime cache
