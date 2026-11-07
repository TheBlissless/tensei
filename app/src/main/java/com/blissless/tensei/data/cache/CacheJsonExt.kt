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

// JSON-blob caches in SharedPreferences for home/explore/manga/airing data
// (with validity timestamps).

fun CacheManager.invalidateUserCache() {
    sharedPreferences.edit {
        remove(CacheManager.CACHE_HOME_DATA)
            .remove(CacheManager.CACHE_HOME_TIME)
    }
}

fun CacheManager.isCacheValid(cacheKey: String, customDuration: Long = CacheManager.CACHE_DURATION_MS): Boolean {
    val cacheTime = sharedPreferences.getLong(cacheKey, 0)
    val now = System.currentTimeMillis()
    return (now - cacheTime) < customDuration
}

fun CacheManager.setCacheTime(cacheKey: String) {
    sharedPreferences.edit { putLong(cacheKey, System.currentTimeMillis()) }
}

fun CacheManager.saveHomeDataToCache(data: HomeCacheData) {
    try {
        val jsonString = json.encodeToString(HomeCacheData.serializer(), data)
        sharedPreferences.edit { putString(CacheManager.CACHE_HOME_DATA, jsonString) }
        setCacheTime(CacheManager.CACHE_HOME_TIME)
    } catch (_: Exception) {
    }
}

fun CacheManager.loadHomeDataFromCache(): HomeCacheData? {
    val cachedData = sharedPreferences.getString(CacheManager.CACHE_HOME_DATA, null)
    if (cachedData != null && isCacheValid(CacheManager.CACHE_HOME_TIME)) {
        return try {
            json.decodeFromString<HomeCacheData>(cachedData)
        } catch (e: Exception) { ErrorHandler.report("CacheManager", "operation failed, returning null", e); null }
    }
    return null
}

fun CacheManager.saveExploreDataToCache(data: ExploreCacheData) {
    try {
        val jsonString = json.encodeToString(ExploreCacheData.serializer(), data)
        sharedPreferences.edit { putString(CacheManager.CACHE_EXPLORE_DATA, jsonString) }
        setCacheTime(CacheManager.CACHE_EXPLORE_TIME)
    } catch (_: Exception) {
    }
}

fun CacheManager.loadExploreDataFromCache(): ExploreCacheData? {
    val cachedData = sharedPreferences.getString(CacheManager.CACHE_EXPLORE_DATA, null)
    if (cachedData != null && isCacheValid(CacheManager.CACHE_EXPLORE_TIME)) {
        return try {
            json.decodeFromString<ExploreCacheData>(cachedData)
        } catch (e: Exception) { ErrorHandler.report("CacheManager", "operation failed, returning null", e); null }
    }
    return null
}

fun CacheManager.saveMangaExploreToCache(sections: Map<String, List<MangaExploreMedia>>) {
    try {
        val jsonString = json.encodeToString(sections)
        sharedPreferences.edit { putString(CacheManager.CACHE_MANGA_EXPLORE_DATA, jsonString) }
        setCacheTime(CacheManager.CACHE_MANGA_EXPLORE_TIME)
    } catch (_: Exception) {
    }
}

fun CacheManager.loadMangaExploreFromCache(): Map<String, List<MangaExploreMedia>>? {
    val cachedData = sharedPreferences.getString(CacheManager.CACHE_MANGA_EXPLORE_DATA, null)
    if (cachedData != null && isCacheValid(CacheManager.CACHE_MANGA_EXPLORE_TIME)) {
        return try {
            json.decodeFromString<Map<String, List<MangaExploreMedia>>>(cachedData)
        } catch (e: Exception) { ErrorHandler.report("CacheManager", "operation failed, returning null", e); null }
    }
    return null
}

fun CacheManager.saveAiringScheduleCache(scheduleByDay: Map<Int, List<AiringScheduleAnime>>, airingAnimeList: List<AiringScheduleAnime>) {
    try {
        val cacheData = AiringCacheData(scheduleByDay, airingAnimeList)
        val jsonString = json.encodeToString(AiringCacheData.serializer(), cacheData)
        sharedPreferences.edit { putString(CacheManager.CACHE_AIRING_DATA, jsonString) }
        setCacheTime(CacheManager.CACHE_AIRING_TIME)
    } catch (_: Exception) {
    }
}

fun CacheManager.loadAiringScheduleCache(): AiringCacheData? {
    val cachedData = sharedPreferences.getString(CacheManager.CACHE_AIRING_DATA, null) ?: return null
    return try {
        json.decodeFromString<AiringCacheData>(cachedData)
    } catch (e: Exception) { ErrorHandler.report("CacheManager", "operation failed, returning null", e); null }
}
