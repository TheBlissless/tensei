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
import com.blissless.tensei.data.anime.fetchAnimeExploreFromMal
import com.blissless.tensei.data.cache.saveAiringScheduleCache
import com.blissless.tensei.data.anime.fetchAiringScheduleAnimeScheduleFallback
import com.blissless.tensei.data.cache.loadAiringScheduleCache
import com.blissless.tensei.data.anime.fetchAiringSchedule

// Explore and airing schedule data (AniList with MAL fallback).

fun MainViewModel.fetchExploreData(force: Boolean = false, silent: Boolean = false) {
    val now = System.currentTimeMillis()
    if (!force && now - lastExploreRefreshTime < MainViewModel.MIN_REFRESH_INTERVAL_MS) {
        return
    }
    viewModelScope.launch {
        if (!silent) _isLoadingExplore.value = true
        val (response, error) = repository.fetchBatchedExploreWithError(useCache = !force)
        if (response == null) {
            // AniList unavailable: fill the main home rows from MAL ranking so the
            // explore screen still shows content — but ONLY the rows that are currently
            // EMPTY. Previously-loaded AniList entries are never replaced by MAL data;
            // genre rows are left untouched (MAL ranking has no genre filter).
            val malSections = repository.fetchAnimeExploreFromMal()
            val hadExisting = _featuredAnime.value.isNotEmpty() ||
                _seasonalAnime.value.isNotEmpty() ||
                _topSeries.value.isNotEmpty() ||
                _topMovies.value.isNotEmpty()
            if (_featuredAnime.value.isEmpty()) {
                _featuredAnime.value = (malSections["featured"] ?: emptyList()).map { mapExploreMedia(it) }
            }
            if (_seasonalAnime.value.isEmpty()) {
                _seasonalAnime.value = (malSections["seasonal"] ?: emptyList()).map { mapExploreMedia(it) }
            }
            if (_topSeries.value.isEmpty()) {
                _topSeries.value = (malSections["topSeries"] ?: emptyList()).map { mapExploreMedia(it) }
            }
            if (_topMovies.value.isEmpty()) {
                _topMovies.value = (malSections["topMovies"] ?: emptyList()).map { mapExploreMedia(it) }
            }
            val malFilled = _featuredAnime.value.isNotEmpty() ||
                _seasonalAnime.value.isNotEmpty() ||
                _topSeries.value.isNotEmpty() ||
                _topMovies.value.isNotEmpty()
            if (malFilled) {
                _apiError.value = null
                _exploreDataSource.value = "mal"
                // Only persist a MAL-based snapshot when there was nothing pre-existing to
                // preserve — never let MAL data overwrite the persisted AniList cache.
                if (!hadExisting) saveExploreDataToCache()
                lastExploreRefreshTime = System.currentTimeMillis()
            } else {
                _apiError.value = error ?: context.getString(R.string.runtime_failed_to_load_content)
            }
            if (!silent) _isLoadingExplore.value = false
            return@launch
        }
        val success = try {
            _featuredAnime.value = response.data.featured.media.map { mapExploreMedia(it) }
            _seasonalAnime.value = response.data.seasonal.media.map { mapExploreMedia(it) }
            _topSeries.value = response.data.topSeries.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 70 }
            _topMovies.value = response.data.topMovies.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 70 }
            _actionAnime.value = response.data.action.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 60 }
            _romanceAnime.value = response.data.romance.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 60 }
            _comedyAnime.value = response.data.comedy.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 60 }
            _fantasyAnime.value = response.data.fantasy.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 60 }
            _scifiAnime.value = response.data.scifi.media.map { mapExploreMedia(it) }.filter { (it.averageScore ?: 0) >= 60 }
            saveExploreDataToCache()
            _apiError.value = null
            _exploreDataSource.value = "anilist"
            true
        } catch (e: Exception) {
            _apiError.value = e.message ?: context.getString(R.string.runtime_failed_to_load_content)
            false
        }
        if (!silent) _isLoadingExplore.value = false
        if (success) {
            lastExploreRefreshTime = System.currentTimeMillis()
        }
    }
}
/**
 * Auto-recovery from a MAL-fallback explore: while the explore screen is showing MAL
 * ranking data (AniList unavailable), the screen periodically calls this to retry AniList.
 * The reload is visible (loading skeleton) so the user sees the MAL → AniList swap.
 * As soon as AniList answers, the regular success path replaces the MAL rows and the
 * source flips back to "anilist".
 */

fun MainViewModel.retryExploreFromMalFallback() {
    if (_exploreDataSource.value != "mal") return
    viewModelScope.launch { fetchExploreData(force = true) }
}

fun MainViewModel.mapExploreMedia(media: ExploreMedia): ExploreAnime {
    val title = media.title.romaji ?: media.title.english ?: "Unknown"
    val episodes = media.episodes ?: 0
    val latestEpisode = media.nextAiringEpisode?.episode?.let { it - 1 }
    return ExploreAnime(
        id = media.id,
        title = title,
        titleEnglish = media.title.english,
        cover = media.coverImage?.extraLarge ?: "",
        banner = media.bannerImage,
        episodes = episodes,
        latestEpisode = latestEpisode,
        averageScore = media.averageScore,
        genres = media.genres ?: emptyList(),
        year = media.startDate?.year ?: media.seasonYear,
        malId = media.idMal,
        isAdult = media.isAdult,
        format = media.format,
        status = media.status
    )
}

fun MainViewModel.fetchAiringSchedule(force: Boolean = false, minIntervalMs: Long = MainViewModel.SCHEDULE_REFRESH_INTERVAL_MS) {
    val now = System.currentTimeMillis()
    val cached = cacheManager.loadAiringScheduleCache()
    if (cached != null && !force && now - lastScheduleRefreshTime < minIntervalMs) {
        return
    }
    if (airingScheduleFetchInProgress) {
        return
    }
    airingScheduleFetchInProgress = true
    viewModelScope.launch {
        _isLoadingSchedule.value = true
        try {
            val airingList = try {
                val schedules = repository.fetchAiringSchedule()
                schedules.filter { it.media != null }.map { schedule ->
                    val media = schedule.media!!
                    val title = media.title.romaji ?: media.title.english ?: "Unknown"
                    val titleEnglish = media.title.english
                    val episodes = media.episodes ?: 0
                    AiringScheduleAnime(
                        id = media.id,
                        title = title,
                        titleEnglish = titleEnglish,
                        cover = schedule.media.coverImage?.extraLarge ?: "",
                        episodes = episodes,
                        airingEpisode = schedule.episode,
                        airingAt = schedule.airingAt,
                        timeUntilAiring = schedule.timeUntilAiring,
                        averageScore = media.averageScore,
                        genres = media.genres ?: emptyList(),
                        year = media.seasonYear,
                        malId = media.idMal,
                        isAdult = media.isAdult
                    )
                }.sortedBy { it.airingAt }
                    .also { lastScheduleUsedFallback = false }
            } catch (_: AiringScheduleApiDownException) {
                Log.w("AiringDebug", "AniList API down — using AnimeSchedule fallback")
                lastScheduleUsedFallback = true
                fetchAnimeScheduleWithToast()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("AiringDebug", "AniList fetch failed unexpectedly (${e::class.simpleName}: ${e.message}) — using AnimeSchedule fallback", e)
                lastScheduleUsedFallback = true
                fetchAnimeScheduleWithToast()
            }
            val deduped = airingList.distinctBy { it.id }
            if (deduped.isNotEmpty()) {
                val scheduleByDay = deduped.groupBy { anime ->
                    val calendar = Calendar.getInstance().apply { timeInMillis = anime.airingAt * 1000L }
                    calendar.get(Calendar.DAY_OF_WEEK) - 1
                }
                _airingSchedule.value = scheduleByDay
                _airingAnimeList.value = deduped
                cacheManager.saveAiringScheduleCache(scheduleByDay, deduped)
                lastScheduleRefreshTime = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Keep existing cached data on failure
            Log.e("AiringDebug", "Airing schedule fetch entirely failed: ${e::class.simpleName}: ${e.message}", e)
        }
        _isLoadingSchedule.value = false
        airingScheduleFetchInProgress = false
    }
}
/**
 * Called when the airing schedule screen is reopened. If the last fetch had to fall
 * back to the AnimeSchedule timetable (AniList was down), always refetch so new/weekly
 * data appears immediately. Otherwise respect a short 5-minute reload cooldown.
 */

fun MainViewModel.fetchAiringScheduleOnReopen() {
    if (lastScheduleUsedFallback) {
        fetchAiringSchedule(force = true)
    } else {
        fetchAiringSchedule(force = false, minIntervalMs = MainViewModel.SCHEDULE_REOPEN_INTERVAL_MS)
    }
}
/** True when the airing schedule is showing AnimeSchedule fallback data (AniList was down). */

suspend fun MainViewModel.fetchAnimeScheduleWithToast(): List<AiringScheduleAnime> = try {
    repository.fetchAiringScheduleAnimeScheduleFallback().also {
    }
} catch (e: AnimeScheduleUnavailableException) {
    Log.e("AiringDebug", "AnimeSchedule fallback unavailable: ${e.message}", e)
    _toastMessage.emit(context.getString(R.string.runtime_schedule_service_unavailable))
    emptyList()
}
