package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// Persisted cache models (offline home/explore/airing data, stream caches,
// playback positions and continue-watching entries) — see data/cache/.



// ============================================
// CACHE MODELS
// ============================================

@Serializable
data class StreamCacheData(val entries: Map<String, StreamCacheEntry>)


@Serializable
data class StreamCacheEntry(
    val stream: CachedStream?,
    val episodeInfo: CachedEpisodeInfo?,
    val timestamp: Long
)


@Serializable
data class CachedQuality(
    val quality: String,
    val url: String,
    val width: Int
)


@Serializable
data class CachedStream(
    val url: String,
    val isDirectStream: Boolean,
    val headers: Map<String, String>?,
    val subtitleUrl: String?,
    val serverName: String,
    val category: String,
    val qualities: List<CachedQuality> = emptyList(),
    // Skip timestamps (in seconds)
    val introStart: Int? = null,
    val introEnd: Int? = null,
    val outroStart: Int? = null,
    val outroEnd: Int? = null
)


@Serializable
data class CachedEpisodeInfo(
    val subServers: List<CachedServer>,
    val dubServers: List<CachedServer>,
    val animeId: String,
    val episodeId: String
)


@Serializable
data class CachedServer(
    val name: String,
    val url: String,
    val qualities: List<CachedQuality> = emptyList()
)


@Serializable
data class CachedTrack(
    val url: String,
    val lang: String,
)


@Serializable
data class CachedVideo(
    val videoUrl: String,
    val videoTitle: String,
    val resolution: Int?,
    val headers: Map<String, String>?,
    val subtitleTracks: List<CachedTrack>,
    val audioTracks: List<CachedTrack>,
)


@Serializable
data class CachedHoster(
    val hosterUrl: String,
    val hosterName: String,
)


@Serializable
data class CachedExtensionStream(
    val url: String,
    val referer: String,
    val subtitleUrl: String?,
    val subtitleTracks: List<CachedTrack>,
    val videoTitle: String,
    val videos: List<CachedVideo>,
    val hosters: List<CachedHoster>?,
    val videoHeaders: Map<String, String>,
    val cachedAt: Long,
)


@Serializable
data class AiringCacheData(
    val scheduleByDay: Map<Int, List<AiringScheduleAnime>>,
    val airingAnimeList: List<AiringScheduleAnime>
)


@Serializable
data class ExploreCacheData(
    val featuredAnime: List<ExploreAnime>,
    val seasonalAnime: List<ExploreAnime>,
    val topSeries: List<ExploreAnime>,
    val topMovies: List<ExploreAnime>,
    val actionAnime: List<ExploreAnime>,
    val romanceAnime: List<ExploreAnime>,
    val comedyAnime: List<ExploreAnime>,
    val fantasyAnime: List<ExploreAnime>,
    val scifiAnime: List<ExploreAnime>
)


@Serializable
data class HomeCacheData(
    val currentlyWatching: List<AnimeMedia>,
    val planningToWatch: List<AnimeMedia>,
    val completed: List<AnimeMedia>,
    val onHold: List<AnimeMedia>,
    val dropped: List<AnimeMedia>,
    val userId: Int?,
    val userName: String?,
    val userAvatar: String?,
    val mangaContinueReading: List<MangaMedia> = emptyList(),
    val mangaCurrentlyReading: List<MangaMedia> = emptyList(),
    val mangaPlanningToRead: List<MangaMedia> = emptyList(),
    val mangaCompleted: List<MangaMedia> = emptyList(),
    val mangaPaused: List<MangaMedia> = emptyList(),
    val mangaDropped: List<MangaMedia> = emptyList()
)


@Serializable
data class PlaybackPositionCache(
    val positions: Map<String, Long>,
    val durations: Map<String, Long> = emptyMap(),
    val startedAt: Map<String, Long> = emptyMap()
)


data class ContinueWatchingEntry(
    val animeId: Int,
    val episode: Int,
    val animeTitle: String,
    val animeTitleEnglish: String?,
    val animeCover: String,
    val animeBanner: String?,
    val position: Long,
    val duration: Long,
    val startedAt: Long
)
