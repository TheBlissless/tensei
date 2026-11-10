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
import com.blissless.tensei.viewmodel.updateHomeState
import com.blissless.tensei.viewmodel.shouldSyncMangaListsOnFreshWindow
import com.blissless.tensei.data.prefs.getAllLocalAnimeStatus
import com.blissless.tensei.data.cache.loadHomeDataFromCache
import com.blissless.tensei.data.cache.loadExploreDataFromCache
import com.blissless.tensei.data.cache.invalidateUserCache
import com.blissless.tensei.data.cache.loadAiringScheduleCache
import com.blissless.tensei.data.cache.saveHomeDataToCache
import com.blissless.tensei.data.cache.saveExploreDataToCache

// Home / offline data: offline list maintenance, cached loading for
// home/explore/airing, and manual refresh flows.

fun MainViewModel.updateOfflineLists() {
    val statusMap = userPreferences.getAllLocalAnimeStatus()
    val cache = cacheManager.detailedAnimeCache.value
    val favorites = userPreferences.localFavorites.value
    val currentlyWatching = mutableListOf<AnimeMedia>()
    val planningToWatch = mutableListOf<AnimeMedia>()
    val completed = mutableListOf<AnimeMedia>()
    val onHold = mutableListOf<AnimeMedia>()
    val dropped = mutableListOf<AnimeMedia>()
    statusMap.forEach { (id, entry) ->
        val cachedAnime = cache[id]
        val favorite = favorites[id]
        val anime = if (cachedAnime != null) {
            AnimeMedia(
                id = cachedAnime.id,
                title = cachedAnime.title,
                titleEnglish = cachedAnime.titleEnglish,
                cover = cachedAnime.cover,
                banner = cachedAnime.banner,
                progress = entry.progress,
                totalEpisodes = cachedAnime.episodes,
                latestEpisode = cachedAnime.latestEpisode ?: cachedAnime.nextAiringEpisode?.let { it - 1 },
                status = cachedAnime.status ?: "",
                averageScore = cachedAnime.averageScore,
                genres = cachedAnime.genres,
                listStatus = entry.status,
                year = cachedAnime.year,
                malId = cachedAnime.malId,
                format = cachedAnime.format,
                userScore = entry.score,
                updatedAt = entry.updatedAt
            )
        } else if (favorite != null) {
            AnimeMedia(
                id = favorite.id,
                title = favorite.title,
                cover = favorite.cover,
                banner = favorite.banner,
                progress = entry.progress,
                totalEpisodes = entry.totalEpisodes,
                listStatus = entry.status,
                year = favorite.year,
                averageScore = favorite.averageScore,
                userScore = entry.score,
                updatedAt = entry.updatedAt
            )
        } else if (entry.title.isNotEmpty()) {
            // Use data stored in LocalAnimeEntry itself
            AnimeMedia(
                id = entry.id,
                title = entry.title,
                cover = entry.cover,
                banner = entry.banner,
                progress = entry.progress,
                totalEpisodes = entry.totalEpisodes,
                listStatus = entry.status,
                year = entry.year,
                averageScore = entry.averageScore,
                userScore = entry.score,
                updatedAt = entry.updatedAt
            )
        } else {
            null
        }
        if (anime != null) {
            when (entry.status) {
                "CURRENT" -> currentlyWatching.add(anime)
                "PLANNING" -> planningToWatch.add(anime)
                "COMPLETED" -> completed.add(anime)
                "PAUSED" -> onHold.add(anime)
                "DROPPED" -> dropped.add(anime)
            }
        }
    }
    // Keep the cache's stored ordering rather than re-sorting by score, so the
    // "Last Updated" default the list screens offer reflects the saved order.
    _offlineCurrentlyWatching.value = currentlyWatching
    _offlinePlanningToWatch.value = planningToWatch
    _offlineCompleted.value = completed
    _offlineOnHold.value = onHold
    _offlineDropped.value = dropped
    // Prefetch streams for offline "Continue Watching" list
    // prefetchOfflineWatchingStreams() // Disabled for now
}
suspend fun MainViewModel.loadHomeDataWithCache() {
    val homeData = cacheManager.loadHomeDataFromCache()
    if (homeData != null) {
        updateHomeState(homeData)
    }
    val now = System.currentTimeMillis()
    // Cache written by versions before `updatedAt` existed has no per-entry timestamps.
    // Sorting those lists by last-updated can only fall back to their (possibly mutated)
    // stored order, which makes Completed look shuffled. Treat that data as stale so the
    // provider is queried once and every entry gets its real last-updated timestamp.
    // Read the state flows (populated by both the cache and local tracking), not just
    // `homeData`, so an expired cache with locally restored manga still refreshes.
    val hasUnknownListTimestamps = (isAniListActive || isMalActive) && run {
        val unknownAnimeTimestamps = listOf(
            _currentlyWatching.value, _planningToWatch.value, _completed.value,
            _onHold.value, _dropped.value
        ).any { list -> list.any { it.updatedAt <= 0L } }
        // Manga is mirrored from local tracking. A pre-upgrade cache has all-zero timestamps
        // per list; require the whole non-empty list to be unknown so a single local-only
        // legacy track can't force a provider refresh on every launch.
        val unknownMangaTimestamps = listOf(
            _mangaCurrentlyReading.value, _mangaPlanningToRead.value, _mangaCompleted.value,
            _mangaPaused.value, _mangaDropped.value
        ).any { list -> list.isNotEmpty() && list.all { it.updatedAt <= 0L } }
        unknownAnimeTimestamps || unknownMangaTimestamps
    }
    // Home lists always prefer AniList when it's logged in — MAL is only the primary
    // list source when it's the sole provider. If the AniList fetch fails (API down),
    // the fallback below pulls the MAL list instead.
    val malMain = isMalActive && !isAniListActive
    if (!hasUnknownListTimestamps && now - lastHomeRefreshTime < MainViewModel.MIN_REFRESH_INTERVAL_MS) {
        _isLoadingHome.value = false
        refreshReleasingAnimeProgress()
        // Manga home lists are restored from the home cache and also mirrored in the
        // local track database, so a fresh window skips the AniList re-sync entirely.
        // Only sync when the cache has no manga data at all (e.g. first run after login).
        if (shouldSyncMangaListsOnFreshWindow(homeData)) {
            viewModelScope.launch {
                if (isAniListActive) fetchMangaLists()
                else if (isMalActive) fetchMalMangaList()
            }
        }
        return
    }
    _isLoadingHome.value = true
    val userSuccess: Boolean
    val listsSuccess: Boolean
    val mangaListsSuccess: Boolean
    coroutineScope {
        if (malMain) {
            // MAL is the main provider: pull its list first, but still fetch the AniList
            // user id so AniList manga tracking and list fallback keep working. Do NOT
            // overwrite the home lists with AniList data.
            userSuccess = async { if (isAniListActive) fetchUser() else true }.await()
            listsSuccess = async { fetchMalList() }.await()
            mangaListsSuccess = async {
                if (isAniListActive) fetchMangaLists()
                else if (isMalActive) { fetchMalMangaList(); true }
                else true
            }.await()
        } else {
            // fetchUser MUST complete before fetchLists/fetchMangaLists — both need _userId
            // which is set by fetchUser. Running them in parallel was a bug that caused
            // anime lists to silently fail (fetchLists returned false because _userId was null).
            userSuccess = async { fetchUser() }.await()
            val listsDeferred = async { fetchLists() }
            val mangaListsDeferred = async { fetchMangaLists() }
            listsSuccess = listsDeferred.await()
            mangaListsSuccess = mangaListsDeferred.await()
        }
    }
    _isLoadingHome.value = false
    // Fall back to the other provider when the preferred one is unavailable (e.g. API outage)
    // so the home lists don't go empty.
    if (!listsSuccess) {
        if (malMain) {
            if (isAniListActive) {
                fetchLists()
            }
        } else {
            if (isMalActive) {
                fetchMalList()
                // Prefer the MAL username/avatar when AniList is unavailable.
                val malUser = malApiService.getAuthManager().userInfo.value
                malUsername?.let { _userName.value = it }
                malUser?.picture?.let { _userAvatar.value = it }
            }
        }
    }
    if (userSuccess && listsSuccess) {
        lastHomeRefreshTime = System.currentTimeMillis()
    }
    refreshReleasingAnimeProgress()
    // prefetchContinueWatchingStreams() // Disabled for now
}

fun MainViewModel.updateHomeState(data: HomeCacheData) {
    _currentlyWatching.value = data.currentlyWatching
    _planningToWatch.value = data.planningToWatch
    _completed.value = data.completed
    _onHold.value = data.onHold
    _dropped.value = data.dropped
    _userId.value = data.userId
    _userName.value = data.userName
    _userAvatar.value = data.userAvatar
    // Only restore manga lists when the cache actually has manga data, so a stale or
    // empty cache never overrides the locally-tracked manga lists.
    if (data.mangaCurrentlyReading.isNotEmpty() ||
        data.mangaPlanningToRead.isNotEmpty() ||
        data.mangaCompleted.isNotEmpty() ||
        data.mangaPaused.isNotEmpty() ||
        data.mangaDropped.isNotEmpty() ||
        data.mangaContinueReading.isNotEmpty()
    ) {
        _mangaContinueReading.value = data.mangaContinueReading
        _mangaCurrentlyReading.value = data.mangaCurrentlyReading
        _mangaPlanningToRead.value = data.mangaPlanningToRead
        _mangaCompleted.value = data.mangaCompleted
        _mangaPaused.value = data.mangaPaused
        _mangaDropped.value = data.mangaDropped
    }
}

fun MainViewModel.shouldSyncMangaListsOnFreshWindow(homeData: HomeCacheData?): Boolean {
    val data = homeData ?: return true
    return data.mangaContinueReading.isEmpty() &&
        data.mangaCurrentlyReading.isEmpty() &&
        data.mangaPlanningToRead.isEmpty() &&
        data.mangaCompleted.isEmpty() &&
        data.mangaPaused.isEmpty() &&
        data.mangaDropped.isEmpty()
}

fun MainViewModel.loadExploreDataWithCache() {
    val cachedData = cacheManager.loadExploreDataFromCache()
    if (cachedData != null) {
        updateExploreState(cachedData)
        // Always kick off a silent background refresh after restoring from cache
        // so the explore screen (including episode counts for currently-airing
        // anime) does not stay stale until the next manual refresh. The
        // force = false path honors the 5-minute cooldown so we don't hammer
        // AniList on rapid app re-opens.
        fetchExploreData(silent = true)
    } else {
        fetchExploreData(force = true)
    }
}

fun MainViewModel.updateExploreState(data: ExploreCacheData) {
    _featuredAnime.value = data.featuredAnime
    _seasonalAnime.value = data.seasonalAnime
    _topSeries.value = data.topSeries
    _topMovies.value = data.topMovies
    _actionAnime.value = data.actionAnime
    _romanceAnime.value = data.romanceAnime
    _comedyAnime.value = data.comedyAnime
    _fantasyAnime.value = data.fantasyAnime
    _scifiAnime.value = data.scifiAnime
}

fun MainViewModel.loadAiringScheduleCache() {
    cacheManager.loadAiringScheduleCache()?.let {
        val list = it.airingAnimeList.distinctBy { a -> a.id }
        _airingAnimeList.value = list
        _airingSchedule.value = list.groupBy { anime ->
            Calendar.getInstance().apply { timeInMillis = anime.airingAt * 1000L }.get(Calendar.DAY_OF_WEEK) - 1
        }
    }
}
// API calls

fun MainViewModel.saveHomeDataToCache() = cacheManager.saveHomeDataToCache(HomeCacheData(
    _currentlyWatching.value, _planningToWatch.value, _completed.value, _onHold.value, _dropped.value,
    _userId.value, _userName.value, _userAvatar.value,
    mangaContinueReading = _mangaContinueReading.value,
    mangaCurrentlyReading = _mangaCurrentlyReading.value,
    mangaPlanningToRead = _mangaPlanningToRead.value,
    mangaCompleted = _mangaCompleted.value,
    mangaPaused = _mangaPaused.value,
    mangaDropped = _mangaDropped.value
))

fun MainViewModel.saveExploreDataToCache() = cacheManager.saveExploreDataToCache(ExploreCacheData(_featuredAnime.value, _seasonalAnime.value, _topSeries.value, _topMovies.value, _actionAnime.value, _romanceAnime.value, _comedyAnime.value, _fantasyAnime.value, _scifiAnime.value))

fun MainViewModel.refreshHome(force: Boolean = false) {
    val now = System.currentTimeMillis()
    // Skip if recently refreshed (unless forced)
    if (!force && now - lastHomeRefreshTime < MainViewModel.MIN_REFRESH_INTERVAL_MS) {
        // Still re-sync manga tracking — it's a single lightweight AniList query, and the
        // pull-to-refresh gesture should reflect AniList-tracked manga even inside the window.
        if (_loginProvider.value == LoginProvider.MAL) {
            viewModelScope.launch { fetchMalMangaList() }
        } else {
            viewModelScope.launch { fetchMangaLists() }
        }
        return
    }
    lastHomeRefreshTime = now
    cacheManager.invalidateUserCache()
    viewModelScope.launch {
        _isLoadingHome.value = true
        // Home lists prefer AniList whenever it's logged in; MAL is primary only when it's
        // the sole provider. The fallback branches below pull the other provider when the
        // preferred one is unavailable (e.g. API outage).
        val malMain = _loginProvider.value == LoginProvider.MAL
        if (malMain) {
            // MAL is the main provider: pull its list first, fall back to AniList.
            val malOk = fetchMalList()
            if (!malOk && isAniListActive) {
                fetchLists()
            }
            if (isAniListActive) {
                fetchMangaLists()
            } else {
                fetchMalMangaList()
            }
        } else {
            // AniList is the main provider; only fall back to MAL when it fails.
            val listsOk = fetchLists()
            fetchMangaLists()
            if (!listsOk && isMalActive) {
                fetchMalList()
            }
        }
        _isLoadingHome.value = false
        // Reconcile both providers so MAL mirrors AniList (the main provider). This prunes
        // MAL-only manga/anime and pushes AniList changes — the differential, automatic sync.
        // It runs on the same throttle as refreshHome (MainViewModel.MIN_REFRESH_INTERVAL_MS) and only when
        // both providers are logged in; it no-ops otherwise.
        if (isBothActive && !userPreferences.malAsMainProvider.value) {
            runCrossProviderDiffSync()
        }
        // prefetchContinueWatchingStreams() // Disabled for now
    }
}
// Per-screen manual refresh timestamps so pull-to-refresh can't be spammed.

fun MainViewModel.tryManualRefresh(key: String): Boolean {
    val now = System.currentTimeMillis()
    val last = manualRefreshTimes[key] ?: 0L
    if (now - last < MainViewModel.MANUAL_REFRESH_COOLDOWN_MS) {
        viewModelScope.launch { _toastMessage.emit(context.getString(R.string.runtime_refresh_cooldown)) }
        return false
    }
    manualRefreshTimes[key] = now
    return true
}

fun MainViewModel.forceRefreshExplore() = fetchExploreData(force = true)

fun MainViewModel.addExploreAnimeToList(anime: ExploreAnime, status: String, score: Int? = null) {
    queueSync(anime.id, "status", malId = anime.malId, status = status, progress = if (status == "CURRENT") 0 else null, score = score)
    updateAnimeStatus(anime.id, status, if (status == "CURRENT") 0 else null, score)
}
data class ExtensionStreamResult(
    val url: String,
    val referer: String,
    val subtitleUrl: String?,
    val subtitleTrackList: List<Track> = emptyList(),
    val videoTitle: String,
    val videos: List<Video> = emptyList(),
    val hosters: List<Hoster>? = null,
    val extensionClient: OkHttpClient? = null,
    val videoHeaders: Map<String, String> = emptyMap(),
    val source: AnimeCatalogueSource? = null,
    val episode: SEpisode? = null,
    /** Hoster names for each video, in the same order as [videos].
     *  Used by `buildServerListFromVideos` to include the provider name
     *  in each dropdown entry's display name (e.g. "Vidstream-2: SUB - 1080p"
     *  vs "HD-1: SUB - 1080p"). Without this, multiple hosters that
     *  return the same video URL would show as indistinguishable entries.
     *
     *  This is a LIST (not a Map) because multiple videos can share
     *  the same URL — a Map would deduplicate by key and lose the
     *  hoster-to-video association. */
    val videoHosterNames: List<String> = emptyList(),
)
// Extension playback — implementations live in viewmodel/MainViewModelExtensionPlaybackExt.kt
// (suspend fun playEpisodeWithExtension, suspend fun fetchExtensionHosterVideos)
