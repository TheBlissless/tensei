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
import com.blissless.tensei.data.prefs.clearAllUserData
import com.blissless.tensei.data.prefs.clearMalFavorites

// Authentication: MAL and AniList login/logout flows, OAuth redirect
// handling and MAL user bootstrap.

fun MainViewModel.loadMalUserData() {
    val malAuth = malApiService.getAuthManager()
    val userInfo = malAuth.userInfo.value
    if (userInfo != null) {
        _userName.value = userInfo.name
        _userAvatar.value = userInfo.picture
        malUsername = userInfo.name
        _malUsername.value = userInfo.name
        _malAvatar.value = userInfo.picture
    }
}

fun MainViewModel.loginWithMal() {
    if (BuildConfig.MAL_CLIENT_ID.isBlank()) {
        return
    }
    val uri = malApiService.getAuthUrl(BuildConfig.MAL_CLIENT_ID)
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    context.startActivity(intent)
}

fun MainViewModel.handleMalAuthAuthCode(uriString: String) {
    if (uriString.isEmpty()) {
        return
    }
    if (!uriString.startsWith("animescraper://success?code=") && !uriString.startsWith("animescraper://success")) {
        return
    }
    val code = uriString.substringAfter("code=").substringBefore("&")
    if (code.isEmpty()) {
        viewModelScope.launch { _toastMessage.emit(context.getString(R.string.runtime_mal_login_no_auth_code)) }
        return
    }
    viewModelScope.launch {
        _toastMessage.emit(context.getString(R.string.runtime_mal_login_in_progress))
        val success = malApiService.exchangeCodeForToken(code, BuildConfig.MAL_CLIENT_ID, null)
        if (success) {
            val anilistStillLoggedIn = userPreferences.authToken.value != null
            // Do NOT clear the AniList token — we support simultaneous login.
            _loginProvider.value = if (anilistStillLoggedIn) LoginProvider.BOTH else LoginProvider.MAL
            loadMalUserData()
            // AniList is the primary home list provider whenever it's logged in — never let
            // the MAL list overwrite the AniList home lists on login. Only fetch MAL as the
            // list source when MAL is the sole provider.
            if (!anilistStillLoggedIn) {
                fetchMalList()
                fetchMalMangaList()
            }
            // prefetchOfflineWatchingStreams() // Disabled for now
            _toastMessage.emit(context.getString(R.string.runtime_mal_login_success))
            // If the user is now on both providers, offer the one-time copy + ongoing sync.
            if (anilistStillLoggedIn) {
                viewModelScope.launch { offerCrossProviderSync() }
            }
        } else {
            _toastMessage.emit(context.getString(R.string.runtime_mal_login_token_error))
        }
    }
}

fun MainViewModel.loginWithAniList() {
    // Do NOT clear MAL data — we support simultaneous login. If MAL is already logged in,
    // the provider becomes BOTH once the token comes back.
    val url = com.blissless.tensei.network.Endpoints.AniList.authUrl(MainViewModel.CLIENT_ID)
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
}

fun MainViewModel.handleAuthRedirect(intent: Intent?) {
    intent?.dataString?.takeIf { it.startsWith("animescraper://success") }?.let { uri ->
        uri.replace("#", "?").toUri().getQueryParameter("access_token")?.let { token ->
            userPreferences.saveToken(token)
            val malStillLoggedIn = malApiService.getAuthManager().isLoggedIn
            _loginProvider.value = if (malStillLoggedIn) LoginProvider.BOTH else LoginProvider.ANILIST
            viewModelScope.launch {
                _isLoadingHome.value = true
                fetchUser()
                // AniList is always the preferred home list source when it's freshly logged
                // in, even if MAL was already active — AniList stays the primary provider.
                fetchLists()
                fetchMangaLists()
                _isLoadingHome.value = false
                fetchAiringSchedule(force = true)
                fetchMangaExplore()
                fetchMangaUserProfile()
                // prefetchContinueWatchingStreams() // Disabled for now
                // If the user is now on both providers, offer the one-time copy + ongoing sync.
                if (malStillLoggedIn) {
                    offerCrossProviderSync()
                }
            }
        }
    }
}

fun MainViewModel.logout() {
    when (_loginProvider.value) {
        LoginProvider.ANILIST, LoginProvider.BOTH -> {
            userPreferences.clearAllUserData()
            userPreferences.clearToken()
            clearMangaUserData()
        }
        LoginProvider.MAL -> {
            malApiService.getAuthManager().clearToken()
            userPreferences.clearMalFavorites()
            userPreferences.clearMalMangaFavorites()
            _malFavorites.value = emptyList()
            malUsername = null
            _malUsername.value = null
            _malAvatar.value = null
        }
        LoginProvider.NONE -> {}
    }
    // In BOTH mode, clear MAL data too (drop: the user is fully logging out).
    if (_loginProvider.value == LoginProvider.BOTH) {
        malApiService.getAuthManager().clearToken()
        userPreferences.clearMalFavorites()
        userPreferences.clearMalMangaFavorites()
        _malFavorites.value = emptyList()
        malUsername = null
        _malUsername.value = null
        _malAvatar.value = null
    }
    cacheManager.clearAllCaches()
    _loginProvider.value = LoginProvider.NONE
    _userId.value = null; _userName.value = null; _userAvatar.value = null
    _userBanner.value = null; _userBio.value = null; _userSiteUrl.value = null; _userCreatedAt.value = null
    _currentlyWatching.value = emptyList(); _planningToWatch.value = emptyList(); _completed.value = emptyList(); _onHold.value = emptyList(); _dropped.value = emptyList()
    _aniListFavorites.value = emptyList()
    _isLoadingHome.value = false
    viewModelScope.launch {
        _logoutEvent.emit(Unit)
    }
}
/**
 * Log out a single provider (used for per-provider logout buttons in Settings when both
 * are logged in). The other provider stays signed in and _loginProvider is recomputed.
 */

fun MainViewModel.logoutProvider(target: LoginProvider) {
    when (target) {
        LoginProvider.ANILIST -> {
            userPreferences.clearAllUserData()
            userPreferences.clearToken()
            clearMangaUserData()
            cacheManager.clearAllCaches()
            _userId.value = null; _userName.value = null; _userAvatar.value = null
            _userBanner.value = null; _userBio.value = null; _userSiteUrl.value = null; _userCreatedAt.value = null
            _currentlyWatching.value = emptyList(); _planningToWatch.value = emptyList()
            _completed.value = emptyList(); _onHold.value = emptyList(); _dropped.value = emptyList()
            _aniListFavorites.value = emptyList()
            _isLoadingHome.value = false
            val malStillLoggedIn = malApiService.getAuthManager().isLoggedIn
            _loginProvider.value = if (malStillLoggedIn) LoginProvider.MAL else LoginProvider.NONE
            if (malStillLoggedIn) {
                // Restore MAL profile into the shared fields so the UI reflects it.
                _userName.value = _malUsername.value
                _userAvatar.value = _malAvatar.value
            }
        }
        LoginProvider.MAL -> {
            malApiService.getAuthManager().clearToken()
            userPreferences.clearMalFavorites()
            userPreferences.clearMalMangaFavorites()
            _malFavorites.value = emptyList()
            malUsername = null
            _malUsername.value = null
            _malAvatar.value = null
            val anilistStillLoggedIn = userPreferences.authToken.value != null
            _loginProvider.value = if (anilistStillLoggedIn) LoginProvider.ANILIST else LoginProvider.NONE
        }
        LoginProvider.BOTH, LoginProvider.NONE -> {}
    }
}
