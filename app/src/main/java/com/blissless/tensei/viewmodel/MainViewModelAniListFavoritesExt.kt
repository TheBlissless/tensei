package com.blissless.tensei.viewmodel

import androidx.lifecycle.viewModelScope
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.api.myanimelist.LoginProvider
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.MediaCoverImage
import com.blissless.tensei.data.models.MediaTitle
import com.blissless.tensei.data.models.UserFavoriteAnime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

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

fun MainViewModel.fetchUserActivity() {
    val userId = _userId.value ?: return
    viewModelScope.launch { repository.fetchUserActivity(userId)?.let { _userActivity.value = it } }
}

/** The user's rating (raw 0-100) for an anime, from the loaded lists or local status. */
private fun MainViewModel.animeUserScore(mediaId: Int): Int? {
    val allAnime = _currentlyWatching.value + _planningToWatch.value + _completed.value + _onHold.value + _dropped.value
    return allAnime.firstOrNull { it.id == mediaId }?.userScore
        ?: userPreferences.getLocalAnimeStatus(mediaId)?.score
}

fun MainViewModel.fetchUserStats() {
    val userId = _userId.value ?: return
    viewModelScope.launch {
        repository.fetchUserStats(userId)?.let {
            _userStats.value = it.data.User.statistics.anime
        }
    }
}

fun MainViewModel.fetchAniListFavorites() {
    val userId = _userId.value ?: return
    viewModelScope.launch {
        repository.fetchUserFavorites(userId)?.let { response ->
            val apiFavorites = response.data.User.favourites.anime.nodes
            // Merge API favorites with locally stored favorites. CRITICAL: the local set is the
            // source of truth for "what the user currently wants favorited", because AniList's
            // GraphQL endpoint is CDN-cached for several seconds. Without this gate, a sequence
            // like:
            //   1. User removes favorite → local set drops the id
            //   2. User reopens Profile within ~5 s → fetchAniListFavorites runs
            //   3. API still returns the removed anime (CDN-stale)
            //   4. mergedFavorites starts from apiFavorites → re-adds the removed anime
            // would silently re-add a just-removed favorite.
            //
            // The fix: intersect the API response with the local set. Any API entry NOT in the
            // local set is a stale-CDN echo of a removed favorite and must be dropped. Local-only
            // entries (added but not yet propagated to AniList) are appended at the end.
            val localFavoriteIds = userPreferences.aniListFavorites.value
            val mergedFavorites = apiFavorites
                .filter { it.id in localFavoriteIds }
                .map { apiFav ->
                    apiFav.copy(userScore = apiFav.userScore ?: animeUserScore(apiFav.id))
                }
                .map { apiFav ->
                    // Prefer the local in-memory entry's userScore (it may be more recent than
                    // the API's, which lags by the CDN TTL).
                    val localFav = _aniListFavorites.value.find { it.id == apiFav.id }
                    localFav?.copy(userScore = localFav.userScore ?: apiFav.userScore) ?: apiFav
                }
                .toMutableList()

            // Add any favorites that were added locally but not on API yet (e.g. added offline,
            // or added but the AniList CDN cache hasn't invalidated to reflect the new state).
            localFavoriteIds.forEach { localId ->
                if (mergedFavorites.none { it.id == localId }) {
                    val localOnly = _aniListFavorites.value.find { it.id == localId }
                    if (localOnly != null) {
                        mergedFavorites.add(localOnly)
                    }
                }
            }

            _aniListFavorites.value = mergedFavorites
        }
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
}}

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
