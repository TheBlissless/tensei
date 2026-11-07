package com.blissless.tensei.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.blissless.tensei.mal.MalMangaListEntry
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.manga.MangaDexManager
import com.blissless.tensei.data.manga.MangaRepository
import com.blissless.tensei.data.manga.MangaTrackManager
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import com.blissless.tensei.extensions.manga.MangaSourceApi
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaTrack
import com.blissless.tensei.data.models.chapterBaseNumber
import com.blissless.tensei.data.models.countDistinctChapters
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.json.JSONObject

// Manga user lists, profile and AniList favorites.



suspend fun MainViewModel.fetchMangaLists(): Boolean {
    val userId = _userId.value ?: return false
    val token = authToken.value ?: return false
    val lists = mangaRepository?.fetchUserMangaLists(userId, token) ?: return false

    // Merge AniList data with local tracks instead of blindly overwriting.
    // This prevents locally-tracked manga from disappearing when AniList returns
    // an empty response (network hiccup, silent parse failure, or user has
    // local-only tracks not yet synced to AniList).
    val anilistCurrent = lists["CURRENT"] ?: lists["Reading"]
    val anilistPlanning = lists["PLANNING"] ?: lists["Plan to Read"]
    val anilistCompleted = lists["COMPLETED"]
    val anilistPaused = lists["PAUSED"]
    val anilistDropped = lists["DROPPED"]

    // Build a merged list: start with local tracks, then add/update from AniList
    val localTracker = mangaTrackManager
    if (localTracker != null) {
        // Sync AniList entries into local tracking
        anilistCurrent?.forEach { m ->
            if (m.malId != null) localTracker.mergeDuplicateMangaTrack(m.id, m.malId)
            localTracker.ensureTrack(m.id, m.title, m.cover, m.totalChapters, m.averageScore, m.titleEnglish, m.listEntryId, m.malId, mediaStatus = m.status.takeIf { it.isNotBlank() })
            localTracker.updateTrackingStatus(m.id, "CURRENT")
            // Never downgrade local progress: a stale AniList response (push still in
            // flight, or a network hiccup) must not roll back chapters the user just read.
            if (m.progress > 0) localTracker.updateChapterProgressKeepMax(m.id, m.progress.toFloat())
            if (m.userScore != null) localTracker.updateScore(m.id, m.userScore)
        }
        anilistPlanning?.forEach { m ->
            if (m.malId != null) localTracker.mergeDuplicateMangaTrack(m.id, m.malId)
            localTracker.ensureTrack(m.id, m.title, m.cover, m.totalChapters, m.averageScore, m.titleEnglish, m.listEntryId, m.malId, mediaStatus = m.status.takeIf { it.isNotBlank() })
            localTracker.updateTrackingStatus(m.id, "PLANNING")
            if (m.userScore != null) localTracker.updateScore(m.id, m.userScore)
        }
        anilistCompleted?.forEach { m ->
            if (m.malId != null) localTracker.mergeDuplicateMangaTrack(m.id, m.malId)
            localTracker.ensureTrack(m.id, m.title, m.cover, m.totalChapters, m.averageScore, m.titleEnglish, m.listEntryId, m.malId, mediaStatus = m.status.takeIf { it.isNotBlank() })
            localTracker.updateTrackingStatus(m.id, "COMPLETED")
            if (m.userScore != null) localTracker.updateScore(m.id, m.userScore)
        }
        anilistPaused?.forEach { m ->
            if (m.malId != null) localTracker.mergeDuplicateMangaTrack(m.id, m.malId)
            localTracker.ensureTrack(m.id, m.title, m.cover, m.totalChapters, m.averageScore, m.titleEnglish, m.listEntryId, m.malId, mediaStatus = m.status.takeIf { it.isNotBlank() })
            localTracker.updateTrackingStatus(m.id, "PAUSED")
            if (m.userScore != null) localTracker.updateScore(m.id, m.userScore)
        }
        anilistDropped?.forEach { m ->
            if (m.malId != null) localTracker.mergeDuplicateMangaTrack(m.id, m.malId)
            localTracker.ensureTrack(m.id, m.title, m.cover, m.totalChapters, m.averageScore, m.titleEnglish, m.listEntryId, m.malId, mediaStatus = m.status.takeIf { it.isNotBlank() })
            localTracker.updateTrackingStatus(m.id, "DROPPED")
            if (m.userScore != null) localTracker.updateScore(m.id, m.userScore)
        }
    }

    // Reload from local (which now includes both local-only and AniList-synced tracks)
    loadLocalMangaTracking()
    reconcileAllCompletions()
    saveHomeDataToCache()
    return true
}


// â”€â”€â”€ Profile: real AniList favorites & activity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Fetch the user's manga favorites + activity + profile stats from AniList.
 * Replaces the previous "fake favorites = union of tracking lists" approach.
 */
fun MainViewModel.fetchMangaUserProfile() {
    val token = authToken.value ?: return
    viewModelScope.launch {
        val userId = _userId.value
        val favoritesDeferred = async { mangaRepository?.fetchUserMangaFavorites(token) ?: emptyList() }
        val profileDeferred = async { mangaRepository?.fetchMangaUserProfile(token) }
        val activityDeferred = async {
            if (userId != null) mangaRepository?.fetchUserMangaActivity(userId, token) ?: emptyList()
            else emptyList()
        }
        val favorites = favoritesDeferred.await()
        val profile = profileDeferred.await()
        val activity = activityDeferred.await()
        _mangaFavorites.value = favorites
        _favoritedMangaIds.value = favorites.map { it.id }.toSet()
        _mangaUserProfile.value = profile
        _mangaActivity.value = activity
    }
}


/**
 * Toggle the manga favorite state for the given AniList media ID.
 * Updates the local favorited-ids set immediately for responsive UI, then calls AniList.
 */
fun MainViewModel.toggleMangaFavorite(mangaId: Int) {
    // Branch on login provider, mirroring the anime path's toggleAniListFavorite:
    //   - MAL-only mode: route to toggleMalMangaFavorite (local-only — MAL has no favorite-
    //     write API). Without this branch, MAL-only users couldn't favorite manga at all
    //     because the early `authToken.value ?: return` below would silently no-op.
    //   - ANILIST / BOTH mode: run the AniList path (toggleMangaFavorite mutation).
    //     BOTH-mode additionally patches the MAL-local mirror inside the success branch
    //     below so the store stays consistent if the user later switches to MAL-only.
    if (_loginProvider.value == LoginProvider.MAL) {
        toggleMalMangaFavorite(mangaId)
        // Optimistic update of the AniList-side state too so the heart icon fills even
        // in MAL-only mode (DetailedMangaScreen reads _favoritedMangaIds for the heart).
        // Without this, the heart would stay outlined even after a successful local add.
        val current = _favoritedMangaIds.value
        _favoritedMangaIds.value = if (mangaId in current) current - mangaId else current + mangaId
        return
    }
    val token = authToken.value ?: return
    // Optimistic update on BOTH the favorited-ids set (used by heart icons on detail screens)
    // AND the favorites list (used by the profile screen's FavoritesContent). Mirrors the
    // anime path's pattern in MainViewModelAniListFavoritesExt.toggleAniListFavorite.
    //
    // Critical: do NOT immediately re-fetch the canonical favorites list after the toggle.
    // AniList's GraphQL endpoint is CDN-cached for several seconds, so the follow-up
    // fetchUserMangaFavorites round-trip would return the OLD favorites (without the
    // newly-added manga) and clobber the optimistic update — making the heart icon
    // flip red for a split second then turn back off, even though the toggle succeeded.
    // Trust the optimistic state; a later fetchMangaUserProfile (on app open / profile
    // screen open) will reconcile with the canonical server-side list once the CDN cache
    // has invalidated.
    val willBeAdded = mangaId !in _favoritedMangaIds.value
    val previousIds = _favoritedMangaIds.value
    val previousList = _mangaFavorites.value
    _favoritedMangaIds.value = if (willBeAdded) previousIds + mangaId else previousIds - mangaId
    _mangaFavorites.value = if (willBeAdded) {
        // Adding: keep the list as-is — the canonical entry will be filled in by a later
        // fetchMangaUserProfile call. We don't have the manga's full payload here, and
        // inserting a placeholder would just be immediately replaced.
        previousList
    } else {
        // Removing: drop the entry from the rendered list immediately so the card
        // disappears from the profile screen before any API round-trip.
        previousList.filter { it.id != mangaId }
    }
    viewModelScope.launch {
        val ok = mangaRepository?.toggleMangaFavorite(mangaId, token) ?: false
        if (!ok) {
            // Revert on failure — restore both the id set and the list to their prior state.
            // The user will see the heart flip back; consider surfacing a toast.
            _favoritedMangaIds.value = previousIds
            _mangaFavorites.value = previousList
        } else {
            // BOTH-mode mirror: if the user is logged into BOTH AniList and MAL, also patch
            // the MAL-local manga favorites store so it stays in sync with what the user
            // just did. Without this, a manga favorite added during BOTH-mode would be
            // missing from the MAL-local store, and if the user later switches to MAL-only
            // mode (e.g. by logging out of AniList), their just-added manga favorite would
            // disappear. The MAL-local store is the fallback source of truth when AniList
            // isn't available, so it must mirror the user's intent even when the AniList
            // API is the primary write target. toggleMalMangaFavorite is local-only (no MAL
            // API call) so this is cheap and won't trigger any network traffic.
            //
            // Done inside the success branch (not unconditionally) so we don't mirror a
            // favorite whose AniList push failed — that would leave the MAL-local store
            // ahead of the actual AniList state.
            if (isBothActive) {
                toggleMalMangaFavorite(mangaId)
            }
        }
        // Success: do NOT immediately re-fetch the favorites list. The optimistic update is
        // already in place; trust it until the next natural refresh (app open / profile open)
        // which will land after AniList's CDN cache has invalidated.
    }
}


/** Returns true if the given manga is currently favorited on AniList. */
fun MainViewModel.isMangaFavorited(mangaId: Int): Boolean = mangaId in _favoritedMangaIds.value
