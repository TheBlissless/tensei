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

@UnstableApi
class CacheManager(internal val sharedPreferences: SharedPreferences) {

    companion object {
        internal const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L
        internal const val STREAM_CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        // Playback within this tail of the media counts as finished: no Continue Watching card.
        internal const val PLAYBACK_FINISHED_TAIL_MS = 10_000L

        internal const val CACHE_EXPLORE_TIME = "cache_explore_time"
        internal const val CACHE_HOME_TIME = "cache_home_time"
        internal const val CACHE_EXPLORE_DATA = "cache_explore_data"
        internal const val CACHE_MANGA_EXPLORE_TIME = "cache_manga_explore_time"
        internal const val CACHE_MANGA_EXPLORE_DATA = "cache_manga_explore_data"
        internal const val CACHE_HOME_DATA = "cache_home_data"
        internal const val CACHE_STREAM_DATA = "cache_stream_data"
        internal const val CACHE_AIRING_TIME = "cache_airing_time"
        internal const val CACHE_AIRING_DATA = "cache_airing_data"
        internal const val CACHE_PLAYBACK_POSITIONS = "cache_playback_positions"
        internal const val CACHE_EXTENSION_STREAMS = "cache_extension_streams"
        
        // Video cache settings
        internal const val VIDEO_CACHE_SIZE_BYTES = 1024L * 1024 * 1024 // 1 GB default - more space for offline content
        internal var videoCache: SimpleCache? = null
        internal var videoDbProvider: StandaloneDatabaseProvider? = null
        internal var isCacheInitialized = false
    }

    // Initialize video cache - call this once when app starts
    @OptIn(UnstableApi::class)
        
    
        

        

    
    

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    internal val _prefetchedStreams = MutableStateFlow<Map<String, AniwatchStreamResult?>>(emptyMap())
    val prefetchedStreams: StateFlow<Map<String, AniwatchStreamResult?>> = _prefetchedStreams.asStateFlow()

    internal val _prefetchedEpisodeInfo = MutableStateFlow<Map<String, EpisodeStreams?>>(emptyMap())
    val prefetchedEpisodeInfo: StateFlow<Map<String, EpisodeStreams?>> = _prefetchedEpisodeInfo.asStateFlow()

    private val _detailedAnimeCache = MutableStateFlow<Map<Int, DetailedAnimeData>>(emptyMap())
    val detailedAnimeCache: StateFlow<Map<Int, DetailedAnimeData>> = _detailedAnimeCache.asStateFlow()

    private val _characterCache = MutableStateFlow<Map<Int, CharacterData>>(emptyMap())
    val characterCache: StateFlow<Map<Int, CharacterData>> = _characterCache.asStateFlow()

    private val _staffCache = MutableStateFlow<Map<Int, StaffData>>(emptyMap())
    val staffCache: StateFlow<Map<Int, StaffData>> = _staffCache.asStateFlow()

    private val _allCharactersCache = MutableStateFlow<Map<Int, List<CharacterData>>>(emptyMap())
    val allCharactersCache: StateFlow<Map<Int, List<CharacterData>>> = _allCharactersCache.asStateFlow()

    private val _allStaffCache = MutableStateFlow<Map<Int, List<DetailedAnimeStaffEdge>>>(emptyMap())
    val allStaffCache: StateFlow<Map<Int, List<DetailedAnimeStaffEdge>>> = _allStaffCache.asStateFlow()

    internal val _playbackPositions = MutableStateFlow<Map<String, Long>>(emptyMap())
    val playbackPositions: StateFlow<Map<String, Long>> = _playbackPositions.asStateFlow()

    internal val _playbackDurations = MutableStateFlow<Map<String, Long>>(emptyMap())
    val playbackDurations: StateFlow<Map<String, Long>> = _playbackDurations.asStateFlow()

    internal val _startedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val startedAt: StateFlow<Map<String, Long>> = _startedAt.asStateFlow()

    // TMDB episode cache - 24h TTL for non-downloaded anime, persistent for downloaded anime
    internal val _tmdbEpisodeCache = MutableStateFlow<Map<Int, List<TmdbEpisode>>>(emptyMap())
    val tmdbEpisodeCache: StateFlow<Map<Int, List<TmdbEpisode>>> = _tmdbEpisodeCache.asStateFlow()
    internal val _tmdbCacheTimestamps = mutableMapOf<Int, Long>()
    internal val TMDB_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    internal val TMDB_CACHE_PREFS = "tmdb_episode_cache"
    internal val TMDB_PERSISTENT_IDS_PREFS = "tmdb_persistent_ids"
    internal val TMDB_CACHE_VERSION = 4
    internal val TMDB_CACHE_VERSION_PREFS = "tmdb_cache_version"
    internal val _persistentTmdbIds = mutableSetOf<Int>()








































    internal val _extensionStreamCache = mutableMapOf<String, CachedExtensionStream>()








    private val _detailedAnimeCacheTimestamps = mutableMapOf<Int, Long>()
    private val DETAILED_ANIME_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    private val MAX_DETAILED_ANIME_CACHE_SIZE = 50

    fun cacheDetailedAnime(animeId: Int, data: DetailedAnimeData) {
        _detailedAnimeCache.value += (animeId to data)
        _detailedAnimeCacheTimestamps[animeId] = System.currentTimeMillis()
        trimDetailedAnimeCacheToLimit()
    }

    fun clearDetailedAnimeCache(animeId: Int) {
        val updated = _detailedAnimeCache.value.toMutableMap()
        updated.remove(animeId)
        _detailedAnimeCache.value = updated
        _detailedAnimeCacheTimestamps.remove(animeId)
    }

    fun clearExpiredDetailedAnimeCache() {
        val now = System.currentTimeMillis()
        val expiredKeys = _detailedAnimeCacheTimestamps.filter { (_, timestamp) ->
            (now - timestamp) > DETAILED_ANIME_CACHE_MAX_AGE_MS
        }.keys

        if (expiredKeys.isNotEmpty()) {
            val updated = _detailedAnimeCache.value.toMutableMap()
            expiredKeys.forEach { key ->
                updated.remove(key)
                _detailedAnimeCacheTimestamps.remove(key)
            }
            _detailedAnimeCache.value = updated
        }
    }

    private fun trimDetailedAnimeCacheToLimit() {
        if (_detailedAnimeCache.value.size > MAX_DETAILED_ANIME_CACHE_SIZE) {
            val sortedByTime = _detailedAnimeCacheTimestamps.entries.sortedBy { it.value }
            val keysToRemove = sortedByTime.take(_detailedAnimeCache.value.size - MAX_DETAILED_ANIME_CACHE_SIZE).map { it.key }

            val updated = _detailedAnimeCache.value.toMutableMap()
            keysToRemove.forEach { key ->
                updated.remove(key)
                _detailedAnimeCacheTimestamps.remove(key)
            }
            _detailedAnimeCache.value = updated
        }
    }

    // Character cache
    private val _characterCacheTimestamps = mutableMapOf<Int, Long>()
    private val CHARACTER_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private val MAX_CHARACTER_CACHE_SIZE = 100

    fun cacheCharacter(characterId: Int, data: CharacterData) {
        _characterCache.value += (characterId to data)
        _characterCacheTimestamps[characterId] = System.currentTimeMillis()
        trimCharacterCacheToLimit()
    }

    fun getCachedCharacter(characterId: Int): CharacterData? {
        val timestamp = _characterCacheTimestamps[characterId] ?: return null
        if (System.currentTimeMillis() - timestamp > CHARACTER_CACHE_MAX_AGE_MS) {
            _characterCacheTimestamps.remove(characterId)
            val updated = _characterCache.value.toMutableMap()
            updated.remove(characterId)
            _characterCache.value = updated
            return null
        }
        return _characterCache.value[characterId]
    }

    private fun trimCharacterCacheToLimit() {
        if (_characterCache.value.size > MAX_CHARACTER_CACHE_SIZE) {
            val sortedByTime = _characterCacheTimestamps.entries.sortedBy { it.value }
            val keysToRemove = sortedByTime.take(_characterCache.value.size - MAX_CHARACTER_CACHE_SIZE).map { it.key }
            val updated = _characterCache.value.toMutableMap()
            keysToRemove.forEach { key ->
                updated.remove(key)
                _characterCacheTimestamps.remove(key)
            }
            _characterCache.value = updated
        }
    }

    // Staff cache
    private val _staffCacheTimestamps = mutableMapOf<Int, Long>()
    private val STAFF_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private val MAX_STAFF_CACHE_SIZE = 100

    fun cacheStaff(staffId: Int, data: StaffData) {
        _staffCache.value += (staffId to data)
        _staffCacheTimestamps[staffId] = System.currentTimeMillis()
        trimStaffCacheToLimit()
    }

    fun getCachedStaff(staffId: Int): StaffData? {
        val timestamp = _staffCacheTimestamps[staffId] ?: return null
        if (System.currentTimeMillis() - timestamp > STAFF_CACHE_MAX_AGE_MS) {
            _staffCacheTimestamps.remove(staffId)
            val updated = _staffCache.value.toMutableMap()
            updated.remove(staffId)
            _staffCache.value = updated
            return null
        }
        return _staffCache.value[staffId]
    }

    private fun trimStaffCacheToLimit() {
        if (_staffCache.value.size > MAX_STAFF_CACHE_SIZE) {
            val sortedByTime = _staffCacheTimestamps.entries.sortedBy { it.value }
            val keysToRemove = sortedByTime.take(_staffCache.value.size - MAX_STAFF_CACHE_SIZE).map { it.key }
            val updated = _staffCache.value.toMutableMap()
            keysToRemove.forEach { key ->
                updated.remove(key)
                _staffCacheTimestamps.remove(key)
            }
            _staffCache.value = updated
        }
    }

    // All Characters cache (keyed by animeId)
    private val _allCharactersCacheTimestamps = mutableMapOf<Int, Long>()
    private val ALL_CHARACTERS_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private val MAX_ALL_CHARACTERS_CACHE_SIZE = 50

    fun cacheAllCharacters(animeId: Int, data: List<CharacterData>) {
        _allCharactersCache.value += (animeId to data)
        _allCharactersCacheTimestamps[animeId] = System.currentTimeMillis()
        trimAllCharactersCacheToLimit()
    }

    fun getCachedAllCharacters(animeId: Int): List<CharacterData>? {
        val timestamp = _allCharactersCacheTimestamps[animeId] ?: return null
        if (System.currentTimeMillis() - timestamp > ALL_CHARACTERS_CACHE_MAX_AGE_MS) {
            _allCharactersCacheTimestamps.remove(animeId)
            val updated = _allCharactersCache.value.toMutableMap()
            updated.remove(animeId)
            _allCharactersCache.value = updated
            return null
        }
        return _allCharactersCache.value[animeId]
    }

    private fun trimAllCharactersCacheToLimit() {
        if (_allCharactersCache.value.size > MAX_ALL_CHARACTERS_CACHE_SIZE) {
            val sortedByTime = _allCharactersCacheTimestamps.entries.sortedBy { it.value }
            val keysToRemove = sortedByTime.take(_allCharactersCache.value.size - MAX_ALL_CHARACTERS_CACHE_SIZE).map { it.key }
            val updated = _allCharactersCache.value.toMutableMap()
            keysToRemove.forEach { key ->
                updated.remove(key)
                _allCharactersCacheTimestamps.remove(key)
            }
            _allCharactersCache.value = updated
        }
    }

    // All Staff cache (keyed by animeId)
    private val _allStaffCacheTimestamps = mutableMapOf<Int, Long>()
    private val ALL_STAFF_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    private val MAX_ALL_STAFF_CACHE_SIZE = 50

    fun cacheAllStaff(animeId: Int, data: List<DetailedAnimeStaffEdge>) {
        _allStaffCache.value += (animeId to data)
        _allStaffCacheTimestamps[animeId] = System.currentTimeMillis()
        trimAllStaffCacheToLimit()
    }

    fun getCachedAllStaff(animeId: Int): List<DetailedAnimeStaffEdge>? {
        val timestamp = _allStaffCacheTimestamps[animeId] ?: return null
        if (System.currentTimeMillis() - timestamp > ALL_STAFF_CACHE_MAX_AGE_MS) {
            _allStaffCacheTimestamps.remove(animeId)
            val updated = _allStaffCache.value.toMutableMap()
            updated.remove(animeId)
            _allStaffCache.value = updated
            return null
        }
        return _allStaffCache.value[animeId]
    }

    private fun trimAllStaffCacheToLimit() {
        if (_allStaffCache.value.size > MAX_ALL_STAFF_CACHE_SIZE) {
            val sortedByTime = _allStaffCacheTimestamps.entries.sortedBy { it.value }
            val keysToRemove = sortedByTime.take(_allStaffCache.value.size - MAX_ALL_STAFF_CACHE_SIZE).map { it.key }
            val updated = _allStaffCache.value.toMutableMap()
            keysToRemove.forEach { key ->
                updated.remove(key)
                _allStaffCacheTimestamps.remove(key)
            }
            _allStaffCache.value = updated
        }
    }

    fun invalidateAllStreamCaches() {
        _prefetchedStreams.value = emptyMap()
        _prefetchedEpisodeInfo.value = emptyMap()
        saveStreamCache()
    }

    fun clearAllCaches() {
        _prefetchedStreams.value = emptyMap()
        _prefetchedEpisodeInfo.value = emptyMap()
        _detailedAnimeCache.value = emptyMap()
        _detailedAnimeCacheTimestamps.clear()
        _characterCache.value = emptyMap()
        _characterCacheTimestamps.clear()
        _staffCache.value = emptyMap()
        _staffCacheTimestamps.clear()
        _allCharactersCache.value = emptyMap()
        _allCharactersCacheTimestamps.clear()
        _allStaffCache.value = emptyMap()
        _allStaffCacheTimestamps.clear()
        _tmdbCacheTimestamps.clear()
        _persistentTmdbIds.clear()
        sharedPreferences.edit { clear() }
    }

    // Remove a specific video URL from the ExoPlayer cache
    // CacheDataSource uses CacheKeyFactory.DEFAULT which keys by uri.toString()
    @OptIn(UnstableApi::class)
    fun removeFromVideoCache(videoUrl: String) {
        val cache = videoCache ?: return
        try {
            cache.removeResource(videoUrl)
        } catch (e: Exception) { ErrorHandler.ignore("CacheManager", "operation failed (best-effort)", e) }
    }

    // ExoPlayer video cache management
    @OptIn(UnstableApi::class)
    fun clearVideoCache(context: Context): Long {
        var bytesCleared = 0L

        // First release the cache if it exists
        val cache = videoCache
        if (cache != null) {
            try {
                cache.release()
                videoCache = null
                isCacheInitialized = false
            } catch (_: Exception) {
                // Cache release failed
            }
        }

        // Clear from disk
        try {
            val cacheDir = File(context.cacheDir, "video_cache")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    bytesCleared += file.length()
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Disk cleanup failed
        }

        return bytesCleared
    }

    @OptIn(UnstableApi::class)
    fun getVideoCacheSize(context: Context): Long {
        val cache = videoCache
        if (cache != null) {
            try {
                val cacheDir = File(context.cacheDir, "video_cache")
                if (cacheDir.exists()) {
                    return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                }
            } catch (_: Exception) {
                // Error getting cache size
            }
        }

        // Fallback to disk calculation
        try {
            val cacheDir = File(context.cacheDir, "video_cache")
            if (cacheDir.exists()) {
                return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }
        } catch (_: Exception) {
            // Error getting disk cache size
        }
        return 0L
    }

    // Clear all non-essential caches (for settings "Clear Cache" button)
    fun clearNonEssentialCaches(context: Context) {
        clearVideoCache(context)
        clearExpiredDetailedAnimeCache()
        trimDetailedAnimeCacheToLimit()

        // Clear character/staff/all caches but keep playback positions
        _characterCache.value = emptyMap()
        _characterCacheTimestamps.clear()
        _staffCache.value = emptyMap()
        _staffCacheTimestamps.clear()
        _allCharactersCache.value = emptyMap()
        _allCharactersCacheTimestamps.clear()
        _allStaffCache.value = emptyMap()
        _allStaffCacheTimestamps.clear()

        // Clear stream cache but keep playback positions
        _prefetchedStreams.value = emptyMap()
        _prefetchedEpisodeInfo.value = emptyMap()
        sharedPreferences.edit { remove(CACHE_STREAM_DATA) }
    }
}

