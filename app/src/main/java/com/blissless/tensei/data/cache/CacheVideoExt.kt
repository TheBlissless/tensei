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

// Media3 video byte cache: initialization, cache data sources and progress
// queries.

fun CacheManager.initializeVideoCache(context: Context) {
    if (CacheManager.isCacheInitialized) return
    try {
        val cacheDir = File(context.cacheDir, "video_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val evictor = LeastRecentlyUsedCacheEvictor(CacheManager.VIDEO_CACHE_SIZE_BYTES)
        val dbProvider = StandaloneDatabaseProvider(context)
        CacheManager.videoDbProvider = dbProvider
        CacheManager.videoCache = SimpleCache(cacheDir, evictor, dbProvider)
        CacheManager.isCacheInitialized = true
    } catch (_: Exception) {
        // Cache initialization failed, continue without caching
    }
}
// Get a CacheDataSource.Factory that uses the video cache
@OptIn(UnstableApi::class)

fun CacheManager.getCacheDataSourceFactory(referer: String, extensionClient: OkHttpClient? = null, extensionHeaders: Map<String, String> = emptyMap()): CacheDataSource.Factory? {
    val cache = CacheManager.videoCache ?: return null
    val trustClient = try {
        eu.kanade.tachiyomi.network.NetworkHelper.getInstance().trustAllClient
    } catch (_: Exception) { null }
    val httpDataSourceFactory = if (extensionClient != null && extensionHeaders.isNotEmpty()) {
        OkHttpDataSource.Factory(extensionClient)
            .setDefaultRequestProperties(extensionHeaders)
    } else if (extensionClient != null) {
        OkHttpDataSource.Factory(extensionClient)
            .setDefaultRequestProperties(mapOf("Referer" to referer))
    } else if (extensionHeaders.isNotEmpty()) {
        OkHttpDataSource.Factory(trustClient!!)
            .setDefaultRequestProperties(extensionHeaders)
    } else {
        OkHttpDataSource.Factory(trustClient!!)
            .setDefaultRequestProperties(mapOf("Referer" to referer))
    }
    return CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
}

fun CacheManager.getContentLength(videoUrl: String, referer: String): Long {
    return try {
        val connection = java.net.URL(videoUrl).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.setRequestProperty("Referer", referer)
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val length = connection.contentLengthLong
        connection.disconnect()
        length
    } catch (_: Exception) {
        -1
    }
}
// Check if a video is fully cached
@OptIn(UnstableApi::class)

fun CacheManager.isVideoFullyCached(videoUrl: String): Boolean {
    val cache = CacheManager.videoCache ?: return false
    return try {
        val cachedSpans = cache.getCachedSpans(videoUrl)
        val cachedBytes = cachedSpans.sumOf { it.length }
        val contentLength = getContentLength(videoUrl, "")
        contentLength in 1..cachedBytes
    } catch (_: Exception) {
        false
    }
}
// Get cache progress for a video
@OptIn(UnstableApi::class)

fun CacheManager.getCacheProgress(videoUrl: String): Pair<Long, Long>? {
    val cache = CacheManager.videoCache ?: return null
    return try {
        val cachedSpans = cache.getCachedSpans(videoUrl)
        val cachedBytes = cachedSpans.sumOf { it.length }
        val contentLength = getContentLength(videoUrl, "")
        if (contentLength > 0) {
            Pair(cachedBytes, contentLength)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}
