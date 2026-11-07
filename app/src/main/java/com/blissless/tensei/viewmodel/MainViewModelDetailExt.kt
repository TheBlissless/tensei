package com.blissless.tensei.viewmodel

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.blissless.tensei.BuildConfig
import com.blissless.tensei.R
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.blissless.tensei.data.anime.AnimeRepository
import com.blissless.tensei.data.anime.AiringScheduleApiDownException
import com.blissless.tensei.data.cache.CacheManager
import com.blissless.tensei.data.anime.AnimeScheduleUnavailableException
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.mal.MalApiService
import com.blissless.tensei.data.prefs.UserPreferences
import com.blissless.tensei.data.models.AiringScheduleAnime
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.data.models.CharacterData
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.DetailedAnimeStaffEdge
import com.blissless.tensei.data.models.StaffData
import com.blissless.tensei.data.models.ExploreCacheData
import com.blissless.tensei.data.models.ExploreMedia
import com.blissless.tensei.data.models.HomeCacheData
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.MediaTag
import com.blissless.tensei.data.models.StoredFavorite
import com.blissless.tensei.data.models.StudioData
import com.blissless.tensei.data.models.TmdbEpisode
import com.blissless.tensei.data.models.UserActivity
import com.blissless.tensei.data.models.UserAnimeStats
import com.blissless.tensei.data.models.UserFavoriteAnime
import com.blissless.tensei.update.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.model.Track
import com.blissless.tensei.torrent.MagnetData
import com.blissless.tensei.torrent.MagnetExtensionClient
import okhttp3.OkHttpClient
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.startApiRetryLoop
import com.blissless.tensei.viewmodel.offerCrossProviderSync
import com.blissless.tensei.viewmodel.showCrossProviderCopyDialog
import com.blissless.tensei.viewmodel.runCrossProviderDiffSync
import com.blissless.tensei.viewmodel.isAniListActive
import com.blissless.tensei.viewmodel.isMalActive
import com.blissless.tensei.viewmodel.isBothActive
import com.blissless.tensei.viewmodel.queueSync
import com.blissless.tensei.viewmodel.fetchMalList
import com.blissless.tensei.viewmodel.initManga
import com.blissless.tensei.viewmodel.clearMangaUserData
import com.blissless.tensei.viewmodel.fetchMangaExplore
import com.blissless.tensei.viewmodel.restoreMangaExploreFromCache
import com.blissless.tensei.viewmodel.fetchMalMangaList
import com.blissless.tensei.viewmodel.fetchMangaLists
import com.blissless.tensei.viewmodel._mangaContinueReading
import com.blissless.tensei.viewmodel._mangaCurrentlyReading
import com.blissless.tensei.viewmodel._mangaPlanningToRead
import com.blissless.tensei.viewmodel._mangaCompleted
import com.blissless.tensei.viewmodel._mangaPaused
import com.blissless.tensei.viewmodel._mangaDropped
import com.blissless.tensei.viewmodel.fetchMangaUserProfile
import com.blissless.tensei.viewmodel.toggleMalFavoriteById
import com.blissless.tensei.viewmodel.loadMalFavoritesFromCache
import com.blissless.tensei.viewmodel.fetchAniListFavorites
import com.blissless.tensei.viewmodel.loadAniListFavoritesFromStorage
import com.blissless.tensei.viewmodel.toggleAniListFavorite
import com.blissless.tensei.viewmodel.refreshReleasingAnimeProgress
import com.blissless.tensei.viewmodel.playEpisodeWithExtension
import com.blissless.tensei.viewmodel.fetchExtensionHosterVideos
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.loadAvailableStreamExtensions
import com.blissless.tensei.viewmodel.fetchMagnetEpisodes
import com.blissless.tensei.viewmodel.fetchMagnetForEpisode
import com.blissless.tensei.viewmodel.fetchStreamUrlForEpisode
import com.blissless.tensei.viewmodel.ensureMagnetClient
import com.blissless.tensei.viewmodel.clearMagnetEpisodes
import com.blissless.tensei.viewmodel.getMagnetEpisodeNumbers
import com.blissless.tensei.viewmodel.getMagnetForEpisode
// Extension functions on AnimeRepository (defined in com.blissless.tensei.data)
import com.blissless.tensei.data.anime.fetchTmdbEpisodes
import com.blissless.tensei.data.anime.fetchAnimeRelationsList
import com.blissless.tensei.data.anime.fetchAnimeRecommendationsList
import com.blissless.tensei.util.ErrorHandler
import com.blissless.tensei.data.anime.fetchDetailedAnimeFromMal
import com.blissless.tensei.data.anime.fetchCharacter
import com.blissless.tensei.data.anime.fetchStaff
import com.blissless.tensei.data.anime.fetchAllCharacters
import com.blissless.tensei.data.anime.fetchAllStaff
import com.blissless.tensei.data.cache.getCachedTmdbEpisodes
import com.blissless.tensei.data.cache.cacheTmdbEpisodes
import com.blissless.tensei.data.cache.clearTmdbEpisodeCache
import com.blissless.tensei.data.cache.pruneTmdbEpisodeCache

// Anime detail data: detail fetch (+cache), relations, recommendations,
// characters/staff, advanced search, tags and TMDB episode metadata.

suspend fun MainViewModel.fetchDetailedAnimeData(animeId: Int, malId: Int? = null, title: String? = null): DetailedAnimeData? {
    // Relations/recommendations from a MAL fallback carry their MAL id as their own
    // id, so animeId doubles as the MAL id only when no title is known (those items
    // never pass a title). Schedule-fallback items carry a fabricated route.hashCode()
    // id — when a title IS provided, never reuse that id as a MAL id.
    val effectiveMalId = malId?.takeIf { it > 0 }
        ?: animeId.takeIf { title.isNullOrBlank() }
    var media = repository.fetchDetailedAnime(animeId)
    // If not found and have MAL ID, try finding by MAL ID
    if (media == null && malId != null && malId > 0) {
        val foundMedia = repository.findAnimeByMalId(malId)
        if (foundMedia != null) {
            media = repository.fetchDetailedAnime(foundMedia.id)
        }
    }
    if (media == null) {
        // AniList unavailable (or forced for testing): fall back to the official MAL
        // anime detail API.
        if (effectiveMalId != null && effectiveMalId > 0) {
            Log.w("AnimeDetailDebug", "fetchDetailedAnimeData AniList failed — trying MAL detail fallback for malId=$effectiveMalId")
            val malData = repository.fetchDetailedAnimeFromMal(effectiveMalId)
            if (malData != null) {
                _animeDetailSource.value = "mal"
                cacheManager.cacheDetailedAnime(animeId, malData)
                return malData
            }
        }
        // IDs unusable (e.g. AnimeSchedule fallback items carry a fabricated id and no MAL
        // id): resolve the anime by title search as a last resort before giving up.
        if (!title.isNullOrBlank()) {
            Log.w("AnimeDetailDebug", "fetchDetailedAnimeData IDs failed — trying title search '$title'")
            try {
                val hits = repository.searchAnime(title)
                var hitIndex = 0
                while (media == null && hitIndex < hits.size) {
                    val hit = hits[hitIndex]
                    hitIndex++
                    media = repository.fetchDetailedAnime(hit.id)
                    if (media != null) {
                        _animeDetailSource.value = "anilist"
                    }
                }
            } catch (e: Exception) {
                Log.w("AnimeDetailDebug", "fetchDetailedAnimeData title search failed: ${e.message}")
            }
        }
        if (media == null) {
            // AniList title search failed too — fall back to the official MAL title search,
            // mirroring the usual AniList-first / MAL-fallback order used elsewhere.
            if (!title.isNullOrBlank()) {
                Log.w("AnimeDetailDebug", "fetchDetailedAnimeData AniList title search failed — trying MAL title search '$title'")
                try {
                    val malIdByTitle = malApiService.searchAnimeByTitle(title)
                    if (malIdByTitle != null && malIdByTitle > 0) {
                        val malData = repository.fetchDetailedAnimeFromMal(malIdByTitle)
                        if (malData != null) {
                            _animeDetailSource.value = "mal"
                            cacheManager.cacheDetailedAnime(animeId, malData)
                            return malData
                        }
                    } else {
                        Log.w("AnimeDetailDebug", "fetchDetailedAnimeData MAL title search returned no malId for '$title'")
                    }
                } catch (e: Exception) {
                    Log.w("AnimeDetailDebug", "fetchDetailedAnimeData MAL title search failed: ${e.message}")
                }
            }
            _animeDetailSource.value = null
            Log.e("AnimeDetailDebug", "fetchDetailedAnimeData RESULT=null (fallback used) id=$animeId malId=$malId")
            return null
        }
    }
    val relationsList = media.relations?.edges?.mapNotNull { edge ->
        edge.node?.let { node ->
            AnimeRelation(
                id = node.id,
                title = node.title?.english ?: node.title?.romaji ?: "Unknown",
                titleRomaji = node.title?.romaji,
                cover = node.coverImage?.extraLarge ?: "",
                episodes = node.episodes,
                latestEpisode = node.nextAiringEpisode?.episode?.let { it - 1 },
                averageScore = node.averageScore,
                format = node.format,
                relationType = edge.relationType ?: "UNKNOWN"
            )
        }
    } ?: emptyList()
    val detailedData = DetailedAnimeData(
        id = media.id,
        malId = media.idMal,
        title = media.title?.romaji ?: media.title?.english ?: "Unknown",
        titleRomaji = media.title?.romaji, titleEnglish = media.title?.english, titleNative = media.title?.native,
        cover = media.coverImage?.extraLarge ?: "", banner = media.bannerImage, description = media.description,
        episodes = media.episodes ?: 0, duration = media.duration, status = media.status,
        averageScore = media.averageScore, popularity = media.popularity, favourites = media.favourites,
        genres = media.genres ?: emptyList(), tags = media.tags ?: emptyList(), season = media.season, year = media.seasonYear ?: media.startDate?.year,
        format = media.format, source = media.source,
        studios = media.studios?.nodes?.map { StudioData(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        startDate = media.startDate?.let { "${it.year}-${it.month}-${it.day}" },
        endDate = media.endDate?.let { "${it.year}-${it.month}-${it.day}" },
        latestEpisode = media.nextAiringEpisode?.episode?.let { it - 1 },
        nextAiringEpisode = media.nextAiringEpisode?.episode, nextAiringTime = media.nextAiringEpisode?.airingAt,
        relations = relationsList,
        isAdult = media.isAdult,
        characters = media.characters,
        trailerUrl = media.trailer?.let { tr ->
            val videoId = tr.id ?: return@let null
            when (tr.site) {
                "youtube" -> com.blissless.tensei.network.Endpoints.YouTube.watchUrl(videoId)
                "dailymotion" -> com.blissless.tensei.network.Endpoints.Dailymotion.watchUrl(videoId)
                else -> null
            }
        },
        trailerThumbnail = media.trailer?.let { tr ->
            val videoId = tr.id ?: return@let null
            if (tr.site == "youtube") {
                com.blissless.tensei.network.Endpoints.YouTube.thumbnailUrl(videoId)
            } else null
        },
        staff = media.staff,
        recommendations = media.recommendations?.nodes?.mapNotNull { r ->
            r.mediaRecommendation?.let { m ->
                ExploreAnime(
                    id = m.id,
                    title = m.title?.romaji ?: m.title?.english ?: "Unknown",
                    titleEnglish = m.title?.english,
                    cover = m.coverImage?.extraLarge ?: "",
                    banner = m.bannerImage,
                    episodes = m.episodes ?: 0,
                    latestEpisode = m.nextAiringEpisode?.episode?.let { it - 1 },
                    averageScore = m.averageScore,
                    genres = m.genres ?: emptyList(),
                    year = m.seasonYear,
                    malId = m.idMal,
                    format = m.format
                )
            }
        } ?: emptyList()
    )
    cacheManager.cacheDetailedAnime(animeId, detailedData)
    _animeDetailSource.value = "anilist"
    return detailedData
}

fun MainViewModel.cachedAnimeRelations(animeId: Int): List<AnimeRelation>? =
    cacheManager.detailedAnimeCache.value[animeId]?.relations?.takeIf { it.isNotEmpty() }

fun MainViewModel.cachedAnimeRecommendations(animeId: Int): List<AnimeRelation>? =
    cacheManager.detailedAnimeCache.value[animeId]?.recommendations
        ?.map { rec ->
            AnimeRelation(
                id = rec.id,
                title = rec.title,
                titleRomaji = null,
                cover = rec.cover,
                episodes = rec.episodes.takeIf { it > 0 },
                averageScore = rec.averageScore,
                format = rec.format,
                relationType = "RECOMMENDATION"
            )
        }
        ?.takeIf { it.isNotEmpty() }

suspend fun MainViewModel.fetchAnimeRelations(animeId: Int, force: Boolean = false): List<AnimeRelation>? {
    // The detail page already resolved and cached relations, so serve those first —
    // the "View All" screen mirrors what the detail row already shows instead of
    // re-hitting AniList. force=true bypasses the cache so auto-recovery can re-check AniList.
    if (!force) cachedAnimeRelations(animeId)?.let { return it }
    val relations = repository.fetchAnimeRelationsList(animeId)
    if (!relations.isNullOrEmpty()) {
        _animeDetailSource.value = "anilist"
        return relations
    }
    // Not cached yet: fall back to the MAL detail. Entries that originated from
    // MAL fallbacks carry the MAL id as their id, so try it as both an AniList
    // id (above) and a MAL id here.
    val detail = fetchDetailedAnimeData(animeId, malId = animeId)
    return detail?.relations?.takeIf { it.isNotEmpty() }
}

suspend fun MainViewModel.fetchAnimeRecommendations(animeId: Int, force: Boolean = false): List<AnimeRelation>? {
    if (!force) cachedAnimeRecommendations(animeId)?.let { return it }
    val recommendations = repository.fetchAnimeRecommendationsList(animeId)
    if (!recommendations.isNullOrEmpty()) {
        _animeDetailSource.value = "anilist"
        return recommendations
    }
    val detail = fetchDetailedAnimeData(animeId, malId = animeId)
    return detail?.recommendations?.map { rec ->
        AnimeRelation(
            id = rec.id,
            title = rec.title,
            titleRomaji = null,
            cover = rec.cover,
            episodes = rec.episodes.takeIf { it > 0 },
            averageScore = rec.averageScore,
            format = rec.format,
            relationType = "RECOMMENDATION"
        )
    }?.takeIf { it.isNotEmpty() }
}

suspend fun MainViewModel.fetchDetailedAnimeDataByMalId(malId: Int): DetailedAnimeData? {
    val media = repository.findAnimeByMalId(malId) ?: return null
    return fetchDetailedAnimeData(media.id)
}

suspend fun MainViewModel.fetchCharacter(characterId: Int): CharacterData? {
    cacheManager.getCachedCharacter(characterId)?.let { return it }
    val data = repository.fetchCharacter(characterId)
    if (data != null) cacheManager.cacheCharacter(characterId, data)
    return data
}

suspend fun MainViewModel.fetchStaff(staffId: Int): StaffData? {
    cacheManager.getCachedStaff(staffId)?.let { return it }
    val data = repository.fetchStaff(staffId)
    if (data != null) cacheManager.cacheStaff(staffId, data)
    return data
}

suspend fun MainViewModel.fetchAllCharacters(animeId: Int): List<CharacterData>? {
    cacheManager.getCachedAllCharacters(animeId)?.let { return it }
    val data = repository.fetchAllCharacters(animeId)
    if (data != null) cacheManager.cacheAllCharacters(animeId, data)
    return data
}

suspend fun MainViewModel.fetchAllStaff(animeId: Int): List<DetailedAnimeStaffEdge>? {
    cacheManager.getCachedAllStaff(animeId)?.let { return it }
    val data = repository.fetchAllStaff(animeId)
    if (data != null) cacheManager.cacheAllStaff(animeId, data)
    return data
}

suspend fun MainViewModel.searchAnimeAdvanced(
    search: String? = null,
    genres: List<String>? = null,
    tags: List<String>? = null,
    format: String? = null,
    status: String? = null,
    season: String? = null,
    seasonYear: Int? = null,
    sort: String = "POPULARITY_DESC",
    isAdult: Boolean? = null,
    page: Int = 1,
    perPage: Int = 30
) = repository.searchAnimeAdvanced(
    search = search, genres = genres, tags = tags,
    format = format, status = status,
    season = season, seasonYear = seasonYear,
    sort = sort, isAdult = isAdult,
    page = page, perPage = perPage
).map { mapExploreMedia(it) }

suspend fun MainViewModel.getAllTags(): List<MediaTag> {
    if (cachedTags == null) cachedTags = repository.fetchAllTags()
    return cachedTags!!
}
// AniList favorites & user activity — implementations live in viewmodel/MainViewModelAniListFavoritesExt.kt
// (fun fetchUserActivity, fetchUserStats, fetchAniListFavorites,
//  loadAniListFavoritesFromStorage, toggleAniListFavorite, refreshReleasingAnimeProgress)
// Misc

suspend fun MainViewModel.fetchTmdbEpisodes(title: String, id: Int, year: Int? = null, format: String? = null, latest: Int = Int.MAX_VALUE, animeEpisodes: Int? = null) = repository.fetchTmdbEpisodes(title, id, year, format, latest, animeEpisodes)

fun MainViewModel.getCachedTmdbEpisodes(animeId: Int, status: String? = null): List<TmdbEpisode>? = cacheManager.getCachedTmdbEpisodes(animeId, status)

fun MainViewModel.cacheTmdbEpisodes(animeId: Int, episodes: List<TmdbEpisode>) = cacheManager.cacheTmdbEpisodes(animeId, episodes)

fun MainViewModel.clearTmdbEpisodeCache(animeId: Int) = cacheManager.clearTmdbEpisodeCache(animeId)

fun MainViewModel.pruneTmdbEpisodeCache(retainedIds: Set<Int>) = cacheManager.pruneTmdbEpisodeCache(retainedIds)
