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
import com.blissless.tensei.data.prefs.getLocalAnimeStatus
import com.blissless.tensei.data.prefs.updateLocalAnimeProgress
import com.blissless.tensei.data.prefs.clearLocalAnimeStatus
import com.blissless.tensei.data.anime.updateStatus
import com.blissless.tensei.data.prefs.toggleLocalFavorite
import com.blissless.tensei.data.prefs.setLocalAnimeStatus

// Anime tracking: progress/score/status updates, list moves, completion
// reconciliation, and the local (offline) tracking store with its sync
// helpers.

fun MainViewModel.updateAnimeProgress(mediaId: Int, progress: Int) {
    val currentEntry = userPreferences.getLocalAnimeStatus(mediaId)
    if (currentEntry != null) {
        val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
        userPreferences.updateLocalAnimeProgress(mediaId, progress, cachedAnime?.episodes ?: currentEntry.totalEpisodes)
        updateOfflineLists()
    }
    // Immediately update progress in logged-in lists
    updateProgressInLists(mediaId, progress)
    // Auto-complete: if this progress update reaches the total episode count of a FINISHED
    // anime, flip the status from CURRENT to COMPLETED (mirrors manga's reconcileCompletion).
    reconcileAnimeCompletion(mediaId, progressOverride = progress)
    val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
    val malId = cachedAnime?.malId
    if (isMalActive && malId == null) {
        viewModelScope.launch {
            cacheManager.clearDetailedAnimeCache(mediaId)
            val details = fetchDetailedAnimeData(mediaId)
            var resolvedMalId = details?.malId
            if (resolvedMalId == null && details == null) {
                val allAnime = _currentlyWatching.value + _planningToWatch.value + _completed.value + _onHold.value + _dropped.value
                val animeFromList = allAnime.find { it.id == mediaId }
                if (animeFromList != null) {
                    resolvedMalId = malApiService.searchAnimeByTitle(animeFromList.title)
                }
            }
            if (resolvedMalId != null) {
                queueSync(mediaId, "progress", malId = resolvedMalId, progress = progress)
            }
        }
        return
    }
    queueSync(mediaId, "progress", malId = malId, progress = progress)
}

fun MainViewModel.updateProgressInLists(mediaId: Int, progress: Int) {
    val now = System.currentTimeMillis()
    val updateInList: (MutableStateFlow<List<AnimeMedia>>, (AnimeMedia) -> AnimeMedia) -> Unit = { list, updater ->
        list.value = list.value.map { if (it.id == mediaId) updater(it) else it }
    }
    updateInList(_currentlyWatching) { it.copy(progress = progress, updatedAt = now) }
    updateInList(_planningToWatch) { it.copy(progress = progress, updatedAt = now) }
    updateInList(_completed) { it.copy(progress = progress, updatedAt = now) }
    updateInList(_onHold) { it.copy(progress = progress, updatedAt = now) }
    updateInList(_dropped) { it.copy(progress = progress, updatedAt = now) }
}

fun MainViewModel.updateScoreInLists(mediaId: Int, score: Int) {
    val now = System.currentTimeMillis()
    val updateInList: (MutableStateFlow<List<AnimeMedia>>, (AnimeMedia) -> AnimeMedia) -> Unit = { list, updater ->
        list.value = list.value.map { if (it.id == mediaId) updater(it) else it }
    }
    updateInList(_currentlyWatching) { it.copy(userScore = score, updatedAt = now) }
    updateInList(_planningToWatch) { it.copy(userScore = score, updatedAt = now) }
    updateInList(_completed) { it.copy(userScore = score, updatedAt = now) }
    updateInList(_onHold) { it.copy(userScore = score, updatedAt = now) }
    updateInList(_dropped) { it.copy(userScore = score, updatedAt = now) }
}
/**
 * Score-only update for an anime — does NOT change the user's list status. Mirrors manga's
 * updateMangaScore. Previously, the rating dialog (AnimeRatingSheet → onScoreSaved) routed
 * through updateAnimeStatus with status = statusToCheck ?: "CURRENT", which:
 *   - moved the anime to Currently Watching if it had no list status (statusToCheck == null)
 *   - moved the anime back to whatever status it had (still a redundant moveAnimeBetweenLists
 *     call that re-rendered the row and triggered status-side-effect reconcileAnimeCompletion)
 * Now the rating dialog routes through this function, which:
 *   - updates the local entry's score only (preserving status + progress)
 *   - updates the in-memory list's score only (no list-shuffling)
 *   - queues a "score"-only sync (which executePendingSyncs already handles via
 *     repository.updateScore + malApiService.updateAnimeStatus(malId, null, score, null))
 */

fun MainViewModel.updateAnimeScore(mediaId: Int, score: Int, malId: Int? = null) {
    // Resolve the MAL id from the cache if the caller didn't provide one — needed so the
    // MAL-side score update can run in BOTH mode.
    val resolvedMalId = malId ?: cacheManager.detailedAnimeCache.value[mediaId]?.malId
    // Update the local entry's score only — preserve the existing status and progress so
    // an anime the user rated without being in any list doesn't get auto-bucketed into
    // Currently Watching, and an anime in (e.g.) Planning doesn't get yanked out.
    val currentEntry = userPreferences.getLocalAnimeStatus(mediaId)
    if (currentEntry != null) {
        setLocalAnimeStatus(
            mediaId,
            currentEntry.copy(score = score)
        )
    }
    // Update the in-memory list's score without moving the entry between lists.
    updateScoreInLists(mediaId, score)
    // Queue a score-only sync (no status mutation). executePendingSyncs' "score" branch
    // calls repository.updateScore (AniList SaveMediaListEntry with score only) and
    // malApiService.updateAnimeStatus(malId, null, score, null) — both score-only mutations
    // that don't touch status.
    queueSync(mediaId, "score", malId = resolvedMalId, score = score)
}

fun MainViewModel.updateAnimeStatus(mediaId: Int, status: String, progress: Int? = null, score: Int? = null) {
    val currentEntry = userPreferences.getLocalAnimeStatus(mediaId)
    val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
    val malId = cachedAnime?.malId
    val resolvedScore = score ?: currentEntry?.score
    if (isMalActive && malId == null) {
        viewModelScope.launch {
            cacheManager.clearDetailedAnimeCache(mediaId)
            val details = fetchDetailedAnimeData(mediaId)
            var resolvedMalId = details?.malId
            if (resolvedMalId == null && details == null) {
                val allAnime = _currentlyWatching.value + _planningToWatch.value + _completed.value + _onHold.value + _dropped.value
                val animeFromList = allAnime.find { it.id == mediaId }
                if (animeFromList != null) {
                    resolvedMalId = malApiService.searchAnimeByTitle(animeFromList.title)
                }
            } else if (resolvedMalId == null) {
                resolvedMalId = malApiService.searchAnimeByTitle(details.title)
            }
            setLocalAnimeStatus(
                mediaId,
                LocalAnimeEntry(
                    id = mediaId,
                    status = status,
                    progress = progress ?: currentEntry?.progress ?: 0,
                    totalEpisodes = details?.episodes ?: currentEntry?.totalEpisodes ?: 0,
                    score = resolvedScore
                )
            )
            moveAnimeBetweenLists(mediaId, status, progress)
            queueSync(mediaId, "status", malId = resolvedMalId, status = status, progress = progress, score = resolvedScore)
        }
        return
    }
    setLocalAnimeStatus(
        mediaId,
        LocalAnimeEntry(
            id = mediaId,
            status = status,
            progress = progress ?: currentEntry?.progress ?: 0,
            totalEpisodes = cachedAnime?.episodes ?: currentEntry?.totalEpisodes ?: 0,
            score = resolvedScore
        )
    )
    // Immediately update logged-in lists for instant visual feedback
    moveAnimeBetweenLists(mediaId, status, progress)
    if (resolvedScore != null) {
        updateScoreInLists(mediaId, resolvedScore)
    }
    queueSync(mediaId, "status", malId = malId, status = status, progress = progress, score = resolvedScore)
    // Auto-complete: regardless of which status the user picked, if the progress they set
    // reaches the total episode count of a FINISHED anime, flip it to COMPLETED. This covers
    // the status dialog flow (HomeAnimeStatusDialog → onUpdate → updateAnimeStatus) which used
    // to leave a 24/24 anime stuck at its previous status with a wrong badge. Mirrors manga's
    // updateMangaStatus which calls reconcileCompletion at the end. The inner guards in
    // reconcileAnimeCompletion handle: already-COMPLETED (no-op), mediaStatus != FINISHED
    // (no-op — currently-airing shows stay put), progress < totalEpisodes (no-op).
    reconcileAnimeCompletion(mediaId, progressOverride = progress ?: -1)
}

fun MainViewModel.moveAnimeBetweenLists(mediaId: Int, newStatus: String, newProgress: Int?) {
    // Find the anime in the current list
    val allLists = listOf(
        _currentlyWatching.value to { l: List<AnimeMedia> -> _currentlyWatching.value = l },
        _planningToWatch.value to { l: List<AnimeMedia> -> _planningToWatch.value = l },
        _completed.value to { l: List<AnimeMedia> -> _completed.value = l },
        _onHold.value to { l: List<AnimeMedia> -> _onHold.value = l },
        _dropped.value to { l: List<AnimeMedia> -> _dropped.value = l }
    )
    var anime: AnimeMedia? = null
    var sourceListIndex = -1
    for ((index, pair) in allLists.withIndex()) {
        val (list, _) = pair
        val found = list.find { it.id == mediaId }
        if (found != null) {
            anime = found
            sourceListIndex = index
            break
        }
    }
    // If anime not found in any list, create a new entry from cached/local data
    if (anime == null) {
        val localEntry = userPreferences.getLocalAnimeStatus(mediaId)
        val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
        anime = if (cachedAnime != null) {
            AnimeMedia(
                id = cachedAnime.id,
                title = cachedAnime.title,
                titleEnglish = cachedAnime.titleEnglish,
                cover = cachedAnime.cover,
                banner = cachedAnime.banner,
                progress = newProgress ?: 0,
                totalEpisodes = cachedAnime.episodes,
                latestEpisode = cachedAnime.nextAiringEpisode,
                status = cachedAnime.status ?: "",
                averageScore = cachedAnime.averageScore,
                genres = cachedAnime.genres,
                listStatus = newStatus,
                year = cachedAnime.year,
                malId = cachedAnime.malId,
                format = cachedAnime.format
            )
        } else if (localEntry != null) {
            AnimeMedia(
                id = localEntry.id,
                title = localEntry.title.ifEmpty { "Unknown" },
                cover = localEntry.cover,
                banner = localEntry.banner,
                progress = newProgress ?: localEntry.progress,
                totalEpisodes = localEntry.totalEpisodes,
                listStatus = newStatus,
                year = localEntry.year,
                averageScore = localEntry.averageScore
            )
        } else {
            // No data available, skip
            return
        }
    }
    // If anime was found in a source list, remove from it first
    if (sourceListIndex >= 0) {
        val (sourceList, sourceSetter) = allLists[sourceListIndex]
        sourceSetter(sourceList.filter { it.id != mediaId })
    }
    // Update the anime with new status and progress
    val updatedAnime = anime.copy(
        listStatus = newStatus,
        progress = newProgress ?: anime.progress,
        updatedAt = System.currentTimeMillis()
    )
    // Add to target list
    val targetList = when (newStatus) {
        "CURRENT" -> _currentlyWatching
        "PLANNING" -> _planningToWatch
        "COMPLETED" -> _completed
        "PAUSED" -> _onHold
        "DROPPED" -> _dropped
        else -> return
    }
    targetList.value += updatedAnime
}
/**
 * Anime counterpart of manga's reconcileCompletion. When the user's progress has reached
 * the total episode count of a FINISHED anime, the list status is auto-flipped from CURRENT
 * to COMPLETED. Mirrors the manga rule (MainViewModelMangaExt.reconcileCompletion):
 *
 *  - already COMPLETED → no-op
 *  - media status not FINISHED → no-op (don't auto-complete currently-airing shows;
 *    a 12/12 progress on a RELEASING series just means caught up, not done)
 *  - totalEpisodes <= 0 → no-op (unknown total, can't tell)
 *  - effective progress < totalEpisodes → no-op
 *  - status != CURRENT → no-op (an explicit PAUSED/DROPPED/PLANNING choice is never
 *    overridden silently; only a series the user is actively watching gets auto-completed)
 *
 * This fixes the bug where a user marked 24/24 progress on a finished anime but the list
 * status stayed CURRENT — the home screen kept it under "Currently Watching" and the
 * detail screen showed an empty/missing status badge.
 *
 * `progressOverride` lets the caller pass the just-written progress without waiting for
 * the in-memory list to reflect it.
 */

fun MainViewModel.reconcileAnimeCompletion(mediaId: Int, progressOverride: Int = -1) {
    // Find the anime across all logged-in lists to read its current listStatus, totalEpisodes,
    // and media status. We don't consult userPreferences.localAnimeStatus here because the
    // logged-in lists are the source of truth when the user is signed in; for offline-only
    // users, completion is implied by progress == totalEpisodes without needing a status flip.
    val allAnime = _currentlyWatching.value + _planningToWatch.value +
            _completed.value + _onHold.value + _dropped.value
    val anime = allAnime.find { it.id == mediaId } ?: return
    // Already COMPLETED → nothing to do. Any other status (CURRENT / PLANNING / PAUSED /
    // DROPPED) is eligible for auto-completion when progress reaches the total episode count
    // of a FINISHED anime — the user marked all episodes watched, the show is done.
    if (anime.listStatus == "COMPLETED") return
    if (anime.totalEpisodes <= 0) return
    // Media status FINISHED is required. The anime model's `status` field carries AniList's
    // media status (FINISHED / RELEASING / NOT_YET_RELEASED / CANCELLED). The detailed cache
    // is the freshest source; fall back to the list entry if the cache has been evicted.
    val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
    val mediaStatus = cachedAnime?.status ?: anime.status
    if (mediaStatus != "FINISHED") return
    val effectiveProgress = if (progressOverride >= 0) progressOverride else anime.progress
    if (effectiveProgress < anime.totalEpisodes) return
    // Flip the status to COMPLETED. moveAnimeBetweenLists takes care of removing the entry
    // from _currentlyWatching and adding it to _completed (instant UI update), then we queue
    // the server-side status sync (which also carries progress so AniList/MAL stay consistent).
    moveAnimeBetweenLists(mediaId, "COMPLETED", effectiveProgress)
    // Update the local entry too so offline mirrors match (status list rebuild reads this).
    val localEntry = userPreferences.getLocalAnimeStatus(mediaId)
    if (localEntry != null) {
        setLocalAnimeStatus(
            mediaId,
            localEntry.copy(status = "COMPLETED", progress = effectiveProgress)
        )
    }
    queueSync(
        mediaId,
        "status",
        malId = cachedAnime?.malId ?: anime.malId,
        status = "COMPLETED",
        progress = effectiveProgress,
        score = anime.userScore
    )
}
/**
 * Startup safety net: sweep every CURRENT anime for the completion rule. Mirrors manga's
 * reconcileAllCompletions. Called after fetchLists() so a series that the user finished
 * outside the app (e.g. on the AniList website) but whose local listStatus hasn't been
 * refreshed gets auto-completed once the freshly-fetched lists land.
 */

fun MainViewModel.reconcileAllAnimeCompletions() {
    val toComplete = mutableListOf<Pair<Int, Int>>()
    for (anime in _currentlyWatching.value) {
        if (anime.listStatus != "CURRENT") continue
        if (anime.totalEpisodes <= 0) continue
        if (anime.status != "FINISHED") continue
        if (anime.progress < anime.totalEpisodes) continue
        toComplete += anime.id to anime.progress
    }
    for ((mediaId, progress) in toComplete) {
        moveAnimeBetweenLists(mediaId, "COMPLETED", progress)
        val anime = _completed.value.find { it.id == mediaId }
        val localEntry = userPreferences.getLocalAnimeStatus(mediaId)
        if (localEntry != null) {
            setLocalAnimeStatus(
                mediaId,
                localEntry.copy(status = "COMPLETED", progress = progress)
            )
        }
        queueSync(
            mediaId,
            "status",
            malId = anime?.malId,
            status = "COMPLETED",
            progress = progress,
            score = anime?.userScore
        )
    }
}

fun MainViewModel.removeAnimeFromList(mediaId: Int) {
    val entryId = (currentlyWatching.value + planningToWatch.value + completed.value + onHold.value + dropped.value)
        .find { it.id == mediaId }?.listEntryId
    // Immediately remove from all lists for instant feedback
    _currentlyWatching.value = _currentlyWatching.value.filter { it.id != mediaId }
    _planningToWatch.value = _planningToWatch.value.filter { it.id != mediaId }
    _completed.value = _completed.value.filter { it.id != mediaId }
    _onHold.value = _onHold.value.filter { it.id != mediaId }
    _dropped.value = _dropped.value.filter { it.id != mediaId }
    setLocalAnimeStatus(mediaId, null)
    if (entryId != null) {
        queueSync(mediaId, "delete", entryId = entryId)
    }
}

fun MainViewModel.discardLocalChanges() {
    userPreferences.clearLocalAnimeStatus()
    updateOfflineLists()
}

fun MainViewModel.addLocalToAniListOnlyNew() {
    viewModelScope.launch {
        val localStatus = localAnimeStatus.value
        val allAniListEntries = currentlyWatching.value + planningToWatch.value + completed.value + onHold.value + dropped.value
        for ((mediaId, entry) in localStatus) {
            if (allAniListEntries.none { it.id == mediaId }) {
                repository.updateStatus(mediaId, entry.status, entry.progress)
            }
        }
        userPreferences.clearLocalAnimeStatus()
        updateOfflineLists()
        fetchLists()
    }
}

fun MainViewModel.overwriteAniListWithLocal() {
    viewModelScope.launch {
        val localStatus = localAnimeStatus.value
        val allAniListEntries = currentlyWatching.value + planningToWatch.value + completed.value + onHold.value + dropped.value
        for (entry in allAniListEntries) {
            val localEntry = localStatus[entry.id]
            if (localEntry != null) {
                repository.updateStatus(entry.id, localEntry.status, localEntry.progress)
            }
        }
        for ((mediaId, entry) in localStatus) {
            if (allAniListEntries.none { it.id == mediaId }) {
                repository.updateStatus(mediaId, entry.status, entry.progress)
            }
        }
        userPreferences.clearLocalAnimeStatus()
        updateOfflineLists()
        fetchLists()
    }
}
// Settings — implementations live in viewmodel/MainViewModelSettingsExt.kt
// (fun setThemeMode, setDefaultExtensionPackage, invalidateAllStreamCaches,
//  setDisableMaterialColors, … setDefaultMagnetExtension)
// Favorites

fun MainViewModel.toggleLocalFavorite(mediaId: Int) {
    userPreferences.toggleLocalFavorite(mediaId)
    updateOfflineLists()
}

fun MainViewModel.toggleOfflineFavorite(animeId: Int, title: String, cover: String, banner: String?, year: Int?, averageScore: Int?, titleEnglish: String? = null) {
    userPreferences.toggleLocalFavorite(animeId, title, cover, banner, year, averageScore, titleEnglish)
    updateOfflineLists()
}

fun MainViewModel.setLocalAnimeStatus(mediaId: Int, entry: LocalAnimeEntry?) {
    userPreferences.setLocalAnimeStatus(mediaId, entry)
    updateOfflineLists()
}
// Playback
