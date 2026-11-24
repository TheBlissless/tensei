package com.blissless.tensei.viewmodel

import androidx.lifecycle.viewModelScope
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.MediaCoverImage
import com.blissless.tensei.data.models.MediaTitle
import com.blissless.tensei.data.models.UserFavoriteAnime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import com.blissless.tensei.data.prefs.getLocalAnimeStatus
import com.blissless.tensei.data.anime.fetchUserFavorites
import com.blissless.tensei.data.prefs.isAniListFavorite
import com.blissless.tensei.data.anime.fetchUserActivity
import com.blissless.tensei.data.prefs.saveAniListFavorites
import com.blissless.tensei.data.prefs.toggleAniListFavorite

/**
 * AniList favorites and user-activity logic for [MainViewModel].
 *
 * Extracted from MainViewModel.kt. Handles:
 * - Fetching user activity / stats / favorites from the AniList API
 * - Loading favorites from local storage (offline-first)
 * - Toggling a favorite (delegates to MAL path when logged in via MAL,
 *   otherwise does local-first update + queues a debounced API sync)
 * - Refreshing the latest-episode field for currently-airing anime in the
 *   user's "currently watching" list
 *
 * Public signatures preserved as extension functions.
 */

/** Returns the fetch Job so callers (e.g. the profile screen) can await it for loading state. */
fun MainViewModel.fetchUserActivity(): Job = viewModelScope.launch {
    val userId = _userId.value ?: return@launch
    repository.fetchUserActivity(userId)?.let { _userActivity.value = it }
}

/** The user's rating (raw 0-100) for an anime, from the loaded lists or local status. */
private fun MainViewModel.animeUserScore(mediaId: Int): Int? {
    val allAnime = _currentlyWatching.value + _planningToWatch.value + _completed.value + _onHold.value + _dropped.value
    return allAnime.firstOrNull { it.id == mediaId }?.userScore
        ?: userPreferences.getLocalAnimeStatus(mediaId)?.score
}

fun MainViewModel.fetchUserStats(): Job = viewModelScope.launch {
    val userId = _userId.value ?: return@launch
    repository.fetchUserStats(userId)?.let {
        _userStats.value = it.data.User.statistics.anime
    }
}

fun MainViewModel.fetchAniListFavorites(): Job = viewModelScope.launch {
    val userId = _userId.value ?: return@launch
    val response = repository.fetchUserFavorites(userId) ?: return@launch
    val apiFavorites = response.data.User.favourites.anime.nodes
    val localFavoriteIds = userPreferences.aniListFavorites.value
    val now = System.currentTimeMillis()

    // forget toggles older than the window; from then on the API is authoritative
    recentFavoriteChanges.entries.removeAll {
        now - it.value.changedAtMs > MainViewModel.FAVORITE_STALE_WINDOW_MS
    }

    fun recentChange(mediaId: Int): MainViewModel.RecentFavoriteChange? =
        recentFavoriteChanges[mediaId]

    fun isSuppressed(mediaId: Int): Boolean {
        // A removal that is still queued (or was just made) must not be resurrected by
        // an API/CDN response that still contains the old favorite.
        val pending = pendingSyncs[mediaId]
        if (pending?.type == "favorite" && pending.favoriteAdded == false) return true
        return recentChange(mediaId)?.added == false
    }

    fun isOptimisticAdd(mediaId: Int): Boolean {
        // An add that hasn't reached AniList yet must survive until the next fetch
        // confirms it. Without this, an API response could hide a favorite the user
        // just made in the app.
        val pending = pendingSyncs[mediaId]
        if (pending?.type == "favorite" && pending.favoriteAdded == true) return true
        return recentChange(mediaId)?.added == true && mediaId in localFavoriteIds
    }

    val apiIds = apiFavorites.map { it.id }.toSet()
    val localEntriesById = _aniListFavorites.value.associateBy { it.id }

    // AniList is the source of truth for the actual favorite list. Do NOT intersect the
    // API response with the local id set: a fresh install / cleared preferences have an
    // empty local mirror, which previously hid every favorite that exists on AniList.
    val mergedFavorites = apiFavorites
        .filterNot { isSuppressed(it.id) }
        .map { apiFav ->
            // API metadata is authoritative; only the user's own score comes from local
            // state (the API query doesn't include it). Local placeholders must never
            // replace a real title/cover.
            val localScore = localEntriesById[apiFav.id]?.userScore
            apiFav.copy(userScore = apiFav.userScore ?: localScore ?: animeUserScore(apiFav.id))
        }
        .toMutableList()

    // Keep local-only entries only while their add is still pending/recent. Once the API
    // has answered, a favorite absent from AniList is no longer a favorite.
    localFavoriteIds.forEach { localId ->
        if (localId !in apiIds && isOptimisticAdd(localId) && !isSuppressed(localId)) {
            localEntriesById[localId]?.let { mergedFavorites.add(it) }
        }
    }

    _aniListFavorites.value = mergedFavorites

    // Re-seed the local mirror from the resolved list so detail screens show the right
    // heart state and a later fetch can't discard favorites added from AniList's website.
    val resolvedIds = mergedFavorites.map { it.id }.toSet()
    if (resolvedIds != localFavoriteIds) {
        userPreferences.saveAniListFavorites(resolvedIds)
    }
}

fun MainViewModel.loadAniListFavoritesFromStorage() {
    // Load favorites from UserPreferences (IDs only)
    val favoriteIds = userPreferences.aniListFavorites.value

    if (favoriteIds.isEmpty()) {
        _aniListFavorites.value = emptyList()
        return
    }

    // Convert IDs to UserFavoriteAnime placeholders (will be enriched by detailedAnimeCache if available)
    val favorites = favoriteIds.map { id ->
        val cached = cacheManager.detailedAnimeCache.value[id]
        if (cached != null) {
            UserFavoriteAnime(
                id = cached.id,
                title = MediaTitle(romaji = cached.title, english = cached.titleEnglish),
                coverImage = MediaCoverImage(extraLarge = cached.cover),
                episodes = cached.episodes,
                averageScore = cached.averageScore,
                genres = cached.genres,
                seasonYear = cached.year,
                userScore = animeUserScore(cached.id),
                idMal = cached.malId
            )
        } else {
            // Try to find in currently watching lists
            val allAnime = _currentlyWatching.value + _planningToWatch.value + _completed.value + _onHold.value + _dropped.value
            val anime = allAnime.find { it.id == id }
            if (anime != null) {
UserFavoriteAnime(
                    id = anime.id,
                    title = MediaTitle(romaji = anime.title, english = anime.titleEnglish),
                    coverImage = MediaCoverImage(extraLarge = anime.cover),
                    episodes = anime.totalEpisodes,
                    averageScore = anime.averageScore,
                    genres = anime.genres,
                    seasonYear = anime.year,
                    userScore = anime.userScore,
                    idMal = anime.malId
                )
            } else {
                UserFavoriteAnime(
                    id = id,
                    title = MediaTitle(romaji = "Loading...", english = null),
                    coverImage = MediaCoverImage(large = "", medium = ""),
                    episodes = null,
                    averageScore = null,
                    genres = emptyList(),
                    seasonYear = null,
                    userScore = userPreferences.getLocalAnimeStatus(id)?.score
                )
            }
        }
    }
    _aniListFavorites.value = favorites
}

fun MainViewModel.toggleAniListFavorite(mediaId: Int, anime: AnimeMedia? = null) {
    if (_loginProvider.value == LoginProvider.MAL) {
        // Toggle MAL favorite using the ID-based method
        toggleMalFavoriteById(mediaId)
    } else {
        // Toggle AniList favorite - local-first with persistence
        // Check both in-memory list AND persisted storage to determine current state
        val isFavoriteInMemory = _aniListFavorites.value.any { it.id == mediaId }
        val isFavoriteInStorage = userPreferences.isAniListFavorite(mediaId)
        val isFavorite = isFavoriteInMemory || isFavoriteInStorage
        val willBeAdded = !isFavorite

        // Remember the intent so a just-fetched (or cached) AniList list can't undo it.
        recentFavoriteChanges[mediaId] =
            MainViewModel.RecentFavoriteChange(willBeAdded, System.currentTimeMillis())

        // Update persisted storage
        userPreferences.toggleAniListFavorite(mediaId)

        // Update UI list
        if (isFavorite) {
            _aniListFavorites.value = _aniListFavorites.value.filter { it.id != mediaId }
        } else {
            if (anime != null) {
                val userFavorite = UserFavoriteAnime(
                    id = anime.id,
                    title = MediaTitle(romaji = anime.title, english = anime.titleEnglish),
                    coverImage = MediaCoverImage(extraLarge = anime.cover),
                    episodes = anime.totalEpisodes,
                    averageScore = anime.averageScore,
                    genres = anime.genres,
                    seasonYear = anime.year,
                    userScore = anime.userScore
                )
                _aniListFavorites.value += userFavorite
            } else {
                val cachedAnime = cacheManager.detailedAnimeCache.value[mediaId]
                val placeholder = UserFavoriteAnime(
                    id = mediaId,
                    title = MediaTitle(romaji = cachedAnime?.title ?: "Loading...", english = cachedAnime?.titleEnglish),
                    coverImage = MediaCoverImage(extraLarge = cachedAnime?.cover ?: ""),
                    episodes = cachedAnime?.episodes,
                    averageScore = cachedAnime?.averageScore,
                    genres = cachedAnime?.genres ?: emptyList(),
                    seasonYear = cachedAnime?.year,
                    userScore = animeUserScore(mediaId)
                )
                _aniListFavorites.value += placeholder
            }
        }

        // Queue the API call for debounced sync with the desired state
        queueSync(mediaId, "favorite", favoriteAdded = willBeAdded)

        // BOTH-mode mirror: if the user is logged into BOTH AniList and MAL, also patch the
        // MAL-local favorites store so it stays in sync with what the user just did. Without
        // this, a favorite added during BOTH-mode would be missing from the MAL-local store,
        // and if the user later switches to MAL-only mode (e.g. by logging out of AniList),
        // their just-added favorite would disappear. The MAL-local store is the fallback
        // source of truth when AniList isn't available, so it must mirror the user's intent
        // even when the AniList API is the primary write target. toggleMalFavoriteById is
        // local-only (no MAL API call) so this is cheap and won't trigger any network traffic.
        if (isBothActive) {
            toggleMalFavoriteById(mediaId)
        }
    }
}

// ============================================
// PREFETCHING - Streams for adjacent episodes
// ============================================

internal suspend fun MainViewModel.refreshReleasingAnimeProgress() {
    if (_loginProvider.value == LoginProvider.MAL) {
        return
    }

    val releasing = _currentlyWatching.value.filter { it.status == "RELEASING" }
    if (releasing.isEmpty()) return
    releasing.chunked(3).forEach { chunk ->
        chunk.map { anime ->
            viewModelScope.async {
                repository.fetchDetailedAnime(anime.id)?.let { media ->
                    val newLatestEpisode = media.nextAiringEpisode?.episode?.let { it - 1 }
                    if (newLatestEpisode != anime.latestEpisode) return@async anime.copy(latestEpisode = newLatestEpisode)
                }
                null
            }
        }.awaitAll().filterNotNull().forEach { updated ->
            _currentlyWatching.value = _currentlyWatching.value.map { if (it.id == updated.id) updated else it }
        }
    }
    saveHomeDataToCache()
}
