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

// TMDB episode metadata cache (own SharedPreferences file with versioning
// and persistent-id pruning).

fun CacheManager.getCachedTmdbEpisodes(animeId: Int, status: String? = null): List<TmdbEpisode>? {
    if (status == "RELEASING") return null
    val data = _tmdbEpisodeCache.value[animeId]
    if (data != null && animeId !in _persistentTmdbIds) {
        val timestamp = _tmdbCacheTimestamps[animeId]
        if (timestamp != null) {
            val age = System.currentTimeMillis() - timestamp
            if (age > TMDB_CACHE_MAX_AGE_MS) {
                _tmdbEpisodeCache.value -= animeId
                _tmdbCacheTimestamps.remove(animeId)
                return null
            }
        }
    }
    return data
}

fun CacheManager.cacheTmdbEpisodes(animeId: Int, episodes: List<TmdbEpisode>) {
    _tmdbEpisodeCache.value += (animeId to episodes)
    _tmdbCacheTimestamps[animeId] = System.currentTimeMillis()
    saveTmdbEpisodeCache()
}

fun CacheManager.clearTmdbEpisodeCache(animeId: Int) {
    _tmdbEpisodeCache.value -= animeId
    _tmdbCacheTimestamps.remove(animeId)
    _persistentTmdbIds.remove(animeId)
    saveTmdbEpisodeCache()
    savePersistentTmdbIds()
}

fun CacheManager.pruneTmdbEpisodeCache(retainedIds: Set<Int>) {
    val current = _tmdbEpisodeCache.value
    val filtered = current.filterKeys { it in retainedIds }
    val removedIds = current.keys - filtered.keys
    removedIds.forEach { _persistentTmdbIds.remove(it) }
    // Mark retained IDs as persistent and refresh their timestamps
    val now = System.currentTimeMillis()
    retainedIds.forEach { id ->
        _persistentTmdbIds.add(id)
        _tmdbCacheTimestamps[id] = now
    }
    if (filtered.size != current.size || removedIds.isNotEmpty()) {
        _tmdbEpisodeCache.value = filtered
        savePersistentTmdbIds()
        saveTmdbEpisodeCache()
    }
}

fun CacheManager.loadTmdbEpisodeCache() {
    try {
        // Drop the persisted cache whenever the format of the cached data
        // changes (e.g. movie entries now expanding to the anime's episode count)
        val storedVersion = sharedPreferences.getInt(TMDB_CACHE_VERSION_PREFS, 0)
        if (storedVersion != TMDB_CACHE_VERSION) {
            sharedPreferences.edit {
                remove(TMDB_CACHE_PREFS)
                remove(TMDB_PERSISTENT_IDS_PREFS)
                putInt(TMDB_CACHE_VERSION_PREFS, TMDB_CACHE_VERSION)
            }
            return
        }
        // Load persistent IDs
        val idsData = sharedPreferences.getString(TMDB_PERSISTENT_IDS_PREFS, null)
        if (idsData != null) {
            _persistentTmdbIds.addAll(json.decodeFromString<List<Int>>(idsData))
        }
        val data = sharedPreferences.getString(TMDB_CACHE_PREFS, null) ?: return
        val parsed = json.decodeFromString<Map<String, String>>(data)
        val now = System.currentTimeMillis()
        val restored = mutableMapOf<Int, List<TmdbEpisode>>()
        for ((key, value) in parsed) {
            val parts = key.split("|")
            if (parts.size == 2) {
                val id = parts[0].toIntOrNull() ?: continue
                val timestamp = parts[1].toLongOrNull() ?: continue
                if (id in _persistentTmdbIds || (now - timestamp) < TMDB_CACHE_MAX_AGE_MS) {
                    val episodes = json.decodeFromString<List<TmdbEpisode>>(value)
                    restored[id] = episodes
                    _tmdbCacheTimestamps[id] = timestamp
                }
            }
        }
        _tmdbEpisodeCache.value = restored
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}

fun CacheManager.saveTmdbEpisodeCache() {
    try {
        val entries = _tmdbEpisodeCache.value.mapValues { (_, episodes) ->
            json.encodeToString(episodes)
        }
        val timestamped = entries.mapKeys { (id, _) ->
            val ts = _tmdbCacheTimestamps[id] ?: System.currentTimeMillis()
            "$id|$ts"
        }
        sharedPreferences.edit { putString(TMDB_CACHE_PREFS, json.encodeToString(timestamped)) }
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}

fun CacheManager.savePersistentTmdbIds() {
    try {
        sharedPreferences.edit { putString(TMDB_PERSISTENT_IDS_PREFS, json.encodeToString(_persistentTmdbIds.toList())) }
    } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
}
