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
import com.blissless.tensei.data.cache.initializeVideoCache
import com.blissless.tensei.data.cache.loadStreamCache
import com.blissless.tensei.data.cache.loadExtensionStreamCache
import com.blissless.tensei.data.cache.loadPlaybackPositions
import com.blissless.tensei.data.cache.loadTmdbEpisodeCache

@UnstableApi
class MainViewModel : ViewModel() {

    companion object {
        internal const val TAG = "MainViewModel"
        internal const val CLIENT_ID = BuildConfig.CLIENT_ID_ANILIST
        internal const val MIN_REFRESH_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        internal const val SCHEDULE_REFRESH_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes (airing schedule is stable)
        internal const val SCHEDULE_REOPEN_INTERVAL_MS = 5 * 60 * 1000L // 5 minute cooldown when reopening a screen backed by AniList
        internal const val ANILIST_RECOVERY_INTERVAL_MS = 30_000L // 30s — probe AniList while any source is on fallback
        internal const val MANUAL_REFRESH_COOLDOWN_MS = 30_000L // 30 seconds between manual refreshes
        internal const val SYNC_DEBOUNCE_MS = 2000L // 2 seconds debounce for API sync
        internal const val FAVORITE_DEBOUNCE_MS = 1000L // 1 second debounce for favorite toggles
    }

    internal lateinit var userPreferences: UserPreferences
    internal lateinit var cacheManager: CacheManager
    internal lateinit var repository: AnimeRepository
    internal lateinit var context: Context
    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    internal var sourceManager: com.blissless.tensei.player.stream.SourceManager? = null

    data class PreFetchedExtensionData(
        val source: AnimeCatalogueSource,
        val sAnime: SAnime,
        val episodes: List<SEpisode>
    )
    internal val _preFetchedExtensionData = mutableMapOf<Int, PreFetchedExtensionData>()

    fun getPreFetchedExtensionData(animeId: Int): PreFetchedExtensionData? =
        _preFetchedExtensionData[animeId]

    private val _preFetchedEpisodeNumbers = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val preFetchedEpisodeNumbers: StateFlow<Map<Int, Set<Int>>> = _preFetchedEpisodeNumbers.asStateFlow()

    private val _availableExtensions = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableExtensions: StateFlow<List<Pair<String, String>>> = _availableExtensions.asStateFlow()

    fun loadAvailableExtensions() {
        viewModelScope.launch(Dispatchers.IO) {
            val sm = sourceManager ?: return@launch
            sm.loadSources()
            _availableExtensions.value = sm.getSources().map {
                it.extension.name.removePrefix("Aniyomi: ") to it.extension.packageName
            }.distinctBy { it.second }
        }
    }

    private fun preLoadExtensionSources() {
        viewModelScope.launch(Dispatchers.IO) {
            sourceManager?.loadSources()
        }
    }

    fun preFetchExtensionEpisodes(anime: AnimeMedia, packageName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val tag = "preFetchExt"
            Log.i(tag, "anime=\"${anime.title}\" id=${anime.id} pkg=$packageName")
            _preFetchedExtensionData.remove(anime.id)
            _preFetchedEpisodeNumbers.value -= anime.id

            val pkg = packageName ?: defaultExtensionPackage.value
            if (pkg.isEmpty()) {
                Log.w(tag, "pkg is empty, returning empty set")
                _preFetchedEpisodeNumbers.value += (anime.id to emptySet())
                return@launch
            }

            val sm = sourceManager
            if (sm == null) {
                Log.w(tag, "sourceManager is null")
                _preFetchedEpisodeNumbers.value += (anime.id to emptySet())
                return@launch
            }

            try {
                sm.loadSources()
            } catch (e: Exception) {
                Log.e(tag, "sm.loadSources() threw", e)
                _preFetchedEpisodeNumbers.value += (anime.id to emptySet())
                return@launch
            }
            val allSources = sm.getSources()
            val sw = allSources.find { it.extension.packageName == pkg }
            if (sw == null) {
                Log.e(tag, "no source found matching pkg=$pkg among ${allSources.size} loaded source(s)")
                _preFetchedEpisodeNumbers.value += (anime.id to emptySet())
                return@launch
            }
            val source = sw.source
            Log.i(tag, "matched source: ${source.name} (lang=${source.lang})")

            val searchTerms = listOfNotNull(anime.titleEnglish, anime.title).distinct()
            var matchedSAnime: SAnime? = null
            for (query in searchTerms) {
                try {
                    val page = source.getSearchAnime(1, query, AnimeFilterList())
                    page.animes.take(5).forEachIndexed { i, a ->  }
                    matchedSAnime = page.animes.firstOrNull { a ->
                        a.title.contains(anime.title, ignoreCase = true) ||
                        (anime.titleEnglish != null && a.title.contains(anime.titleEnglish, ignoreCase = true))
                    } ?: page.animes.firstOrNull()
                    if (matchedSAnime != null) {
                        Log.i(tag, "matched anime: \"${matchedSAnime.title}\" url=${matchedSAnime.url}")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(tag, "getSearchAnime(\"$query\") threw", e)
                    ErrorHandler.ignore(TAG, "operation failed (best-effort)", e)
                }
            }

            val sAnime = matchedSAnime
            if (sAnime == null) {
                Log.e(tag, "no anime matched after all search terms, returning empty set")
                _preFetchedEpisodeNumbers.value += (anime.id to emptySet())
                return@launch
            }

            val sEpisodes = try {
                val eps = source.getEpisodeList(sAnime)
                Log.i(tag, "  got ${eps.size} episodes")
                eps.take(5).forEachIndexed { i, e ->  }
                eps
            } catch (e: Exception) {
                Log.e(tag, "getEpisodeList threw, retrying via getAnimeDetails + getEpisodeList", e)
                try {
                    val details = source.getAnimeDetails(sAnime)
                    val eps = source.getEpisodeList(details)
                    Log.i(tag, "  retry got ${eps.size} episodes")
                    eps
                } catch (e2: Exception) {
                    Log.e(tag, "retry also failed", e2)
                    ErrorHandler.report(TAG, "operation failed, returning empty list", e2)
                    emptyList()
                }
            }

            _preFetchedExtensionData[anime.id] = PreFetchedExtensionData(source, sAnime, sEpisodes)
            _preFetchedEpisodeNumbers.value += (anime.id to sEpisodes.map { it.episode_number.toInt() }.toSet())
            Log.i(tag, "DONE: stored ${sEpisodes.size} episode numbers for anime.id=${anime.id}")
        }
    }

    // ─── Magnet Extension Support ────────────────────────────────────────
    // State holders (internal so viewmodel/MainViewModelMagnetExt.kt can access them).
    // Method implementations live in that file.
    internal var magnetExtensionClient: MagnetExtensionClient? = null

    internal val _availableMagnetExtensions = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableMagnetExtensions: StateFlow<List<Pair<String, String>>> = _availableMagnetExtensions.asStateFlow()

    internal val _availableStreamExtensions = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val availableStreamExtensions: StateFlow<List<Pair<String, String>>> = _availableStreamExtensions.asStateFlow()

    val defaultMagnetExtension: StateFlow<String?> get() = userPreferences.defaultMagnetExtension

    internal val _magnetEpisodes = MutableStateFlow<Map<Int, MagnetData>>(emptyMap())
    val magnetEpisodes: StateFlow<Map<Int, MagnetData>> = _magnetEpisodes.asStateFlow()

    // Magnet extension methods — implementations live in viewmodel/MainViewModelMagnetExt.kt
    // (fun ensureMagnetClient, loadAvailableMagnetExtensions, parseTitleForSearch,
    //  fetchMagnetEpisodes, fetchMagnetEpisodesSync, clearMagnetEpisodes,
    //  getMagnetEpisodeNumbers, getMagnetForEpisode, fetchMagnetForEpisode,
    //  fetchStreamUrlForEpisode)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            _isOffline.value = false
        }

        override fun onLost(network: android.net.Network) {
            _isOffline.value = true
        }

        override fun onCapabilitiesChanged(network: android.net.Network, capabilities: NetworkCapabilities) {
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _isOffline.value = !hasInternet
        }
    }

    internal var apiRetryJob: Job? = null

    // Sync queue for debounced AniList API calls
    internal data class PendingSync(val type: String, val mediaId: Int, val malId: Int? = null, val status: String? = null, val progress: Int? = null, val score: Int? = null, val entryId: Int? = null, val favoriteAdded: Boolean? = null)
    internal val pendingSyncs = mutableMapOf<Int, PendingSync>() // mediaId -> pending sync
    internal var syncJob: Job? = null
    internal var favoriteSyncJob: Job? = null

    // MAL sync methods — implementations live in viewmodel/MainViewModelMalSyncExt.kt
    // (fun startApiRetryLoop, queueSync, executeFavoriteSyncs, executePendingSyncs,
    //  mapToMalStatus, mapFromMalStatus, fetchMalList, toggleMalFavoriteById,
    //  loadMalFavoritesFromCache)

    // Track last refresh time to prevent rapid re-fetches (persisted to survive app restarts)
    internal var lastHomeRefreshTime: Long
        get() = userPreferences.getLastHomeRefreshTime()
        set(value) = userPreferences.setLastHomeRefreshTime(value)

    internal var lastExploreRefreshTime: Long
        get() = userPreferences.getLastExploreRefreshTime()
        set(value) = userPreferences.setLastExploreRefreshTime(value)

    internal var lastScheduleRefreshTime: Long
        get() = userPreferences.getLastScheduleRefreshTime()
        set(value) = userPreferences.setLastScheduleRefreshTime(value)

    // UI State
    internal val _userId = MutableStateFlow<Int?>(null)

    internal val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    internal val _userAvatar = MutableStateFlow<String?>(null)
    val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

    internal val _userBanner = MutableStateFlow<String?>(null)
    val userBanner: StateFlow<String?> = _userBanner.asStateFlow()

    internal val _userBio = MutableStateFlow<String?>(null)
    val userBio: StateFlow<String?> = _userBio.asStateFlow()

    internal val _userSiteUrl = MutableStateFlow<String?>(null)
    val userSiteUrl: StateFlow<String?> = _userSiteUrl.asStateFlow()

    internal val _userCreatedAt = MutableStateFlow<Long?>(null)
    val userCreatedAt: StateFlow<Long?> = _userCreatedAt.asStateFlow()

    internal val _isLoadingExplore = MutableStateFlow(false)
    val isLoadingExplore: StateFlow<Boolean> = _isLoadingExplore.asStateFlow()

    internal val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    internal val _apiError = MutableStateFlow<String?>(null)
    val apiError: StateFlow<String?> = _apiError.asStateFlow()

    internal val _isLoadingHome = MutableStateFlow(false)
    val isLoadingHome: StateFlow<Boolean> = _isLoadingHome.asStateFlow()

    internal val _isLoadingSchedule = MutableStateFlow(false)
    val isLoadingSchedule: StateFlow<Boolean> = _isLoadingSchedule.asStateFlow()

    internal var airingScheduleFetchInProgress = false

    // True when the last successful schedule fetch had to use the AnimeSchedule fallback
    // (AniList was down), so the next screen reopen always refetches instead of skipping.
    internal var lastScheduleUsedFallback = false

    // Anime lists
    internal val _currentlyWatching = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val currentlyWatching: StateFlow<List<AnimeMedia>> = _currentlyWatching.asStateFlow()

    internal val _planningToWatch = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val planningToWatch: StateFlow<List<AnimeMedia>> = _planningToWatch.asStateFlow()

    internal val _completed = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val completed: StateFlow<List<AnimeMedia>> = _completed.asStateFlow()

    internal val _onHold = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val onHold: StateFlow<List<AnimeMedia>> = _onHold.asStateFlow()

    internal val _dropped = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val dropped: StateFlow<List<AnimeMedia>> = _dropped.asStateFlow()

    // Offline anime lists (for logged-out users)
    internal val _offlineCurrentlyWatching = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val offlineCurrentlyWatching: StateFlow<List<AnimeMedia>> = _offlineCurrentlyWatching.asStateFlow()

    internal val _offlinePlanningToWatch = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val offlinePlanningToWatch: StateFlow<List<AnimeMedia>> = _offlinePlanningToWatch.asStateFlow()

    internal val _offlineCompleted = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val offlineCompleted: StateFlow<List<AnimeMedia>> = _offlineCompleted.asStateFlow()

    internal val _offlineOnHold = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val offlineOnHold: StateFlow<List<AnimeMedia>> = _offlineOnHold.asStateFlow()

    internal val _offlineDropped = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val offlineDropped: StateFlow<List<AnimeMedia>> = _offlineDropped.asStateFlow()

    // Latest data source driving the explore rows: "anilist" or "mal" (fallback shown
    // while AniList is down/unreachable). Drives automatic recovery — see retryExploreFromMalFallback().
    internal val _exploreDataSource = MutableStateFlow<String?>(null)
    val exploreDataSource: StateFlow<String?> get() = _exploreDataSource.asStateFlow()

    // Latest data source for the most recently fetched anime detail: "anilist" or "mal".
    // Drives automatic recovery on the detail + "View All" screens.
    internal val _animeDetailSource = MutableStateFlow<String?>(null)
    val animeDetailSource: StateFlow<String?> get() = _animeDetailSource.asStateFlow()

    // Explore data
    internal val _featuredAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val featuredAnime: StateFlow<List<ExploreAnime>> = _featuredAnime.asStateFlow()

    internal val _seasonalAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val seasonalAnime: StateFlow<List<ExploreAnime>> = _seasonalAnime.asStateFlow()

    internal val _topSeries = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val topSeries: StateFlow<List<ExploreAnime>> = _topSeries.asStateFlow()

    internal val _topMovies = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val topMovies: StateFlow<List<ExploreAnime>> = _topMovies.asStateFlow()

    internal val _actionAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val actionAnime: StateFlow<List<ExploreAnime>> = _actionAnime.asStateFlow()

    internal val _romanceAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val romanceAnime: StateFlow<List<ExploreAnime>> = _romanceAnime.asStateFlow()

    internal val _comedyAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val comedyAnime: StateFlow<List<ExploreAnime>> = _comedyAnime.asStateFlow()

    internal val _fantasyAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val fantasyAnime: StateFlow<List<ExploreAnime>> = _fantasyAnime.asStateFlow()

    internal val _scifiAnime = MutableStateFlow<List<ExploreAnime>>(emptyList())
    val scifiAnime: StateFlow<List<ExploreAnime>> = _scifiAnime.asStateFlow()

    // Schedule
    internal val _airingSchedule = MutableStateFlow<Map<Int, List<AiringScheduleAnime>>>(emptyMap())
    val airingSchedule: StateFlow<Map<Int, List<AiringScheduleAnime>>> = _airingSchedule.asStateFlow()

    internal val _airingAnimeList = MutableStateFlow<List<AiringScheduleAnime>>(emptyList())
    val airingAnimeList: StateFlow<List<AiringScheduleAnime>> = _airingAnimeList.asStateFlow()

    // Other UI state
    internal val _userActivity = MutableStateFlow<List<UserActivity>>(emptyList())
    val userActivity: StateFlow<List<UserActivity>> = _userActivity.asStateFlow()

    internal val _userStats = MutableStateFlow<UserAnimeStats?>(null)
    val userStats: StateFlow<UserAnimeStats?> = _userStats.asStateFlow()

    // AniList Favorites
    internal val _aniListFavorites = MutableStateFlow<List<UserFavoriteAnime>>(emptyList())
    val aniListFavorites: StateFlow<List<UserFavoriteAnime>> = _aniListFavorites.asStateFlow()

    internal var malUsername: String? = null
    internal val _malUsername = MutableStateFlow<String?>(null)
    val malUsernameFlow: StateFlow<String?> = _malUsername.asStateFlow()
    internal val _malAvatar = MutableStateFlow<String?>(null)
    val malAvatar: StateFlow<String?> = _malAvatar.asStateFlow()

    private val _isFavoriteRateLimited = MutableStateFlow(false)
    val isFavoriteRateLimited: StateFlow<Boolean> = _isFavoriteRateLimited.asStateFlow()

    // Preferences Delegations
    val authToken: StateFlow<String?> get() = userPreferences.authToken
    val themeMode: StateFlow<String> get() = userPreferences.themeMode
    val isOled: StateFlow<Boolean> get() = userPreferences.isOled
    val appIcon: StateFlow<String> get() = userPreferences.appIcon
    val maxPerformance: StateFlow<Boolean> get() = userPreferences.maxPerformance
    val disableMaterialColors: StateFlow<Boolean> get() = userPreferences.disableMaterialColors
    val colorMode: StateFlow<String> get() = userPreferences.colorMode
    val hideEpisodeDescription: StateFlow<Boolean> get() = userPreferences.hideEpisodeDescription
    val preferredCategory: StateFlow<String> get() = userPreferences.preferredCategory
    val showStatusColors: StateFlow<Boolean> get() = userPreferences.showStatusColors
    val showAnimeCardButtons: StateFlow<Boolean> get() = userPreferences.showAnimeCardButtons
    val showMangaCardButtons: StateFlow<Boolean> get() = userPreferences.showMangaCardButtons
    val showMangaStatusColors: StateFlow<Boolean> get() = userPreferences.showMangaStatusColors
    val preferEnglishTitles: StateFlow<Boolean> get() = userPreferences.preferEnglishTitles
    val advancedSearch: StateFlow<Boolean> get() = userPreferences.advancedSearch
    val trackingPercentage: StateFlow<Int> get() = userPreferences.trackingPercentage
    val forwardSkipSeconds: StateFlow<Int> get() = userPreferences.forwardSkipSeconds
    val backwardSkipSeconds: StateFlow<Int> get() = userPreferences.backwardSkipSeconds
    val autoSkipOpening: StateFlow<Boolean> get() = userPreferences.autoSkipOpening
    val autoSkipEnding: StateFlow<Boolean> get() = userPreferences.autoSkipEnding
    val autoPlayNextEpisode: StateFlow<Boolean> get() = userPreferences.autoPlayNextEpisode
    val supportsPiP: StateFlow<Boolean> get() = userPreferences.supportsPiP
    val discordRichPresence: StateFlow<Boolean> get() = userPreferences.discordRichPresence
    val localFavorites: StateFlow<Map<Int, StoredFavorite>> get() = userPreferences.localFavorites
    val localAnimeStatus: StateFlow<Map<Int, LocalAnimeEntry>> get() = userPreferences.localAnimeStatus
    val defaultExtensionPackage: StateFlow<String> get() = userPreferences.defaultExtensionPackage
    val defaultSubtitleLang: StateFlow<String> get() = userPreferences.defaultSubtitleLang
    val startupScreen: StateFlow<Int> get() = userPreferences.startupScreen
    val appLanguage: StateFlow<String> get() = userPreferences.appLanguage

    // Buffer Settings
    val bufferAheadSeconds: StateFlow<Int> get() = userPreferences.bufferAheadSeconds
    val bufferSizeMb: StateFlow<Int> get() = userPreferences.bufferSizeMb
    val showBufferIndicator: StateFlow<Boolean> get() = userPreferences.showBufferIndicator
    val checkUpdatesOnStart: StateFlow<Boolean> get() = userPreferences.checkUpdatesOnStart
    val malAsMainProvider: StateFlow<Boolean> get() = userPreferences.malAsMainProvider
    val autoUpdateExtensions: StateFlow<Boolean> get() = userPreferences.autoUpdateExtensions
    val streamMethod: StateFlow<String> get() = userPreferences.streamMethod
    val defaultStreamExtension: StateFlow<String?> get() = userPreferences.defaultStreamExtension
    val mangaReaderMode: StateFlow<String> get() = userPreferences.mangaReaderMode
    val mangaDataSaver: StateFlow<Boolean> get() = userPreferences.mangaDataSaver
    val mangaPageLayout: StateFlow<String> get() = userPreferences.mangaPageLayout
    val mangaImageScaling: StateFlow<String> get() = userPreferences.mangaImageScaling
    val mangaPageIndicator: StateFlow<Boolean> get() = userPreferences.mangaPageIndicator
    val mangaLockRotation: StateFlow<Boolean> get() = userPreferences.mangaLockRotation
    val mangaFullscreen: StateFlow<Boolean> get() = userPreferences.mangaFullscreen
    val mangaAutoAdvance: StateFlow<Boolean> get() = userPreferences.mangaAutoAdvance
    val mangaSyncThreshold: StateFlow<Int> get() = userPreferences.mangaSyncThreshold

    // Notification tap events
    private val _openExtensionsEvents = Channel<Unit>(Channel.BUFFERED)
    val openExtensionsEvents: Flow<Unit> = _openExtensionsEvents.receiveAsFlow()

    fun requestOpenExtensions() {
        _openExtensionsEvents.trySend(Unit)
    }
    val swipeVolume: StateFlow<Boolean> get() = userPreferences.swipeVolume
    val swipeBrightness: StateFlow<Boolean> get() = userPreferences.swipeBrightness
    val swipeSwap: StateFlow<Boolean> get() = userPreferences.swipeSwap

    // Pending update from startup check
    private val _pendingUpdateRelease = MutableStateFlow<GitHubRelease?>(null)
    val pendingUpdateRelease: StateFlow<GitHubRelease?> = _pendingUpdateRelease.asStateFlow()

    val playbackPositions: StateFlow<Map<String, Long>> get() = cacheManager.playbackPositions

    // Cache methods — implementations live in viewmodel/MainViewModelCacheExt.kt
    // (fun getCacheDataSourceFactory, savePlaybackPosition, … clearNonEssentialCaches)

    // MAL API Service
    internal lateinit var malApiService: MalApiService

    // Login provider tracking
    internal val _loginProvider = MutableStateFlow(LoginProvider.NONE)
    val loginProvider: StateFlow<LoginProvider> = _loginProvider.asStateFlow()

    // MAL favorites (stored locally with full anime data)
    internal val _malFavorites = MutableStateFlow<List<AnimeMedia>>(emptyList())
    val malFavorites: StateFlow<List<AnimeMedia>> = _malFavorites.asStateFlow()

    // Toast messages for UI feedback
    internal val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    internal val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    // Card bounds for shared element transition
    data class CardBounds(val animeId: Int, val coverUrl: String, val bounds: android.graphics.RectF)
    private val _exploreAnimeCardBounds = MutableStateFlow<CardBounds?>(null)
    val exploreAnimeCardBounds: StateFlow<CardBounds?> = _exploreAnimeCardBounds.asStateFlow()

    private val _homeAnimeCardBounds = MutableStateFlow<CardBounds?>(null)
    val homeAnimeCardBounds: StateFlow<CardBounds?> = _homeAnimeCardBounds.asStateFlow()

    private val _hideNavbar = MutableStateFlow(false)
    val hideNavbar: StateFlow<Boolean> = _hideNavbar.asStateFlow()

    fun setHideNavbar(hide: Boolean) {
        _hideNavbar.value = hide
    }

    fun setExploreAnimeCardBounds(animeId: Int, coverUrl: String, bounds: android.graphics.RectF?) {
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            _exploreAnimeCardBounds.value = CardBounds(animeId, coverUrl, bounds)
        }
    }

    fun setHomeAnimeCardBounds(animeId: Int, coverUrl: String, bounds: android.graphics.RectF?) {
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            _homeAnimeCardBounds.value = CardBounds(animeId, coverUrl, bounds)
        }
    }

    fun clearExploreAnimeCardBounds() {
        _exploreAnimeCardBounds.value = null
    }

    fun clearHomeAnimeCardBounds() {
        _homeAnimeCardBounds.value = null
    }

    // Is logged in (either AniList or MAL)
    val isLoggedIn: Boolean get() = _loginProvider.value != LoginProvider.NONE

    fun init(context: Context, hasToken: Boolean) {
        this.context = context.applicationContext
        userPreferences = UserPreferences(context)
        cacheManager = CacheManager(userPreferences.getSharedPreferences())
        repository = AnimeRepository(userPreferences, cacheManager)
        malApiService = MalApiService(context)
        sourceManager = com.blissless.tensei.player.stream.SourceManager(context)

        // Initialize manga managers
        initManga()

        // Initialize video cache for offline playback
        cacheManager.initializeVideoCache(context)

        // Check connectivity and register callback for auto-detection
        checkConnectivity()
        registerConnectivityCallback()
        startApiRetryLoop()

        userPreferences.loadPreferences(hasToken)
        cacheManager.loadStreamCache()
        cacheManager.loadExtensionStreamCache()
        cacheManager.loadPlaybackPositions()
        cacheManager.loadTmdbEpisodeCache()
        loadAiringScheduleCache()
        updateOfflineLists()

        // Check login provider (a user can be logged into both AniList and MAL simultaneously)
        val anilistLoggedIn = hasToken
        val malLoggedIn = malApiService.getAuthManager().isLoggedIn
        _loginProvider.value = when {
            anilistLoggedIn && malLoggedIn -> LoginProvider.BOTH
            anilistLoggedIn -> LoginProvider.ANILIST
            malLoggedIn -> LoginProvider.MAL
            else -> LoginProvider.NONE
        }
        if (malLoggedIn) {
            loadMalUserData()
        }

        viewModelScope.launch {
            // Pre-load extension sources so they're available when episode screen opens
            preLoadExtensionSources()
            // Run home data, explore data, and airing schedule in PARALLEL for faster startup
            launch {
                if (hasToken || _loginProvider.value != LoginProvider.NONE) {
                    loadHomeDataWithCache()
                    if (hasToken) {
                        loadAniListFavoritesFromStorage()
                        fetchAniListFavorites()
                    }
                } else {
                    // prefetchOfflineWatchingStreams() // Disabled for now
                }
            }

            launch { loadExploreDataWithCache() }
            launch { fetchAiringSchedule() }
            launch {
                // Show persisted manga sections immediately, then refresh in the background.
                restoreMangaExploreFromCache()
                fetchMangaExplore()
            }

            // On app start with both providers logged in, ask the user whether they want to run
            // the one-way cross-provider copy. Never auto-sync on start; syncing is manual only.
            launch {
                if (_loginProvider.value == LoginProvider.BOTH) {
                    showCrossProviderCopyDialog(markDoneOnDismiss = false)
                }
            }

            // Check for updates on start if enabled
            if (userPreferences.checkUpdatesOnStart.value) {
                checkForUpdatesSilently()
            }
        }
    }

    fun checkForUpdatesSilently() {
        viewModelScope.launch {
            try {
                val url = com.blissless.tensei.network.Endpoints.GitHub.TENSEI_LATEST_RELEASE
                val request = okhttp3.Request.Builder().url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                val response = withContext(Dispatchers.IO) {
                    OkHttpClient().newCall(request).execute()
                }
                if (!response.isSuccessful) return@launch
                val body = withContext(Dispatchers.IO) { response.body.string() }
                val json = com.blissless.tensei.util.TenseiJson.default
                val release = json.decodeFromString<GitHubRelease>(body)
                val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                val cleanTag = release.tagName.removePrefix("v").removePrefix("V")
                val parts1 = cleanTag.split(".").map { it.toIntOrNull() ?: 0 }
                val parts2 = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                val maxLen = maxOf(parts1.size, parts2.size)
                var cmp = 0
                for (i in 0 until maxLen) {
                    val p1 = parts1.getOrElse(i) { 0 }
                    val p2 = parts2.getOrElse(i) { 0 }
                    if (p1 != p2) { cmp = p1 - p2; break }
                }
                if (cmp > 0) {
                    _pendingUpdateRelease.value = release
                }
            } catch (e: Exception) { ErrorHandler.ignore(TAG, "operation failed (best-effort)", e) }
        }
    }

    private fun checkConnectivity() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOffline.value = !isConnected
        } catch (_: Exception) {
            _isOffline.value = false
        }
    }

    private fun registerConnectivityCallback() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityCallback = networkCallback
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) { ErrorHandler.ignore(TAG, "registerConnectivityCallback failed (best-effort)", e) }
    }
































    suspend fun fetchUser(): Boolean {
        val result = repository.fetchUser()?.let {
            _userId.value = it.data.Viewer.id
            _userName.value = it.data.Viewer.name
            _userAvatar.value = it.data.Viewer.avatar?.large ?: it.data.Viewer.avatar?.medium
            _userBanner.value = it.data.Viewer.bannerImage
            _userBio.value = it.data.Viewer.about
            _userSiteUrl.value = it.data.Viewer.siteUrl
            _userCreatedAt.value = it.data.Viewer.createdAt
            it.data.Viewer.statistics?.anime?.let { stats ->
                _userStats.value = stats
            }
            true
        } ?: false
        return result
    }

    suspend fun fetchLists(): Boolean {
        val userId = _userId.value ?: return false
        val response = repository.fetchMediaLists(userId) ?: return false

        val grouped = response.data.MediaListCollection.lists.flatMap { list ->
            list.entries.map { entry ->
                val anime = AnimeMedia(
                    id = entry.mediaId,
                    title = entry.media.title.romaji ?: entry.media.title.english ?: "Unknown",
                    titleEnglish = entry.media.title.english,
                    cover = entry.media.coverImage?.extraLarge ?: "",
                    banner = entry.media.bannerImage,
                    progress = entry.progress ?: 0,
                    totalEpisodes = entry.media.episodes ?: 0,
                    latestEpisode = entry.media.nextAiringEpisode?.episode?.let { it - 1 },
                    status = entry.media.status ?: "",
                    averageScore = entry.media.averageScore,
                    genres = entry.media.genres ?: emptyList(),
                    listStatus = list.status ?: list.name,
                    listEntryId = entry.id,
                    userScore = entry.score,
                    year = entry.media.seasonYear,
                    malId = entry.media.idMal
                )
                (list.status ?: list.name) to anime
            }
        }.groupBy({ it.first }, { it.second })

        _currentlyWatching.value = grouped["CURRENT"] ?: grouped["Watching"] ?: emptyList()
        _planningToWatch.value = grouped["PLANNING"] ?: grouped["Plan to Watch"] ?: emptyList()
        _completed.value = grouped["COMPLETED"] ?: emptyList()
        _onHold.value = grouped["PAUSED"] ?: emptyList()
        _dropped.value = grouped["DROPPED"] ?: emptyList()

        // Startup safety net: auto-complete any CURRENT anime whose progress has already reached
        // the total episode count of a FINISHED series. This catches series the user finished
        // elsewhere (AniList website, another client) whose status hasn't been refreshed locally.
        reconcileAllAnimeCompletions()

        saveHomeDataToCache()
        return true
    }
















    internal val isScheduleOnFallback: Boolean get() = lastScheduleUsedFallback

























































    val playbackDurations: StateFlow<Map<String, Long>> get() = cacheManager.playbackDurations
    val startedAt: StateFlow<Map<String, Long>> get() = cacheManager.startedAt
    // Playback position & stream cache — implementations live in viewmodel/MainViewModelCacheExt.kt
    // (fun savePlaybackPosition, getPlaybackPosition, clearPlaybackPosition,
    //  removeContinueWatchingEntry, invalidateStreamCache, getCachedExtensionStream,
    //  cacheExtensionStream, invalidateExtensionStreamCache, clearAnimeExtensionStreamCaches,
    //  clearAllExtensionStreamCaches, removeFromVideoCache, getVideoCacheSize, clearNonEssentialCaches)

    /**
     * Get stream for a specific server.
     * Uses the watchPath from the provider.
     */

    /**
     * Get stream using Miruro. Scrapes preferred first, then other in background.
     */

        
        











    internal var cachedTags: List<MediaTag>? = null






    internal val manualRefreshTimes = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * Returns true if a manual refresh for [key] is allowed right now and records it.
     * Returns false (with a toast) if the user just refreshed [key] within
     * [MANUAL_REFRESH_COOLDOWN_MS].
     */



    val tmdbEpisodeCache: StateFlow<Map<Int, List<TmdbEpisode>>> get() = cacheManager.tmdbEpisodeCache




    override fun onCleared() {
        super.onCleared()
        connectivityCallback?.let { callback ->
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: Exception) { ErrorHandler.ignore(TAG, "unregisterNetworkCallback failed (best-effort)", e) }
        }
    }
}

