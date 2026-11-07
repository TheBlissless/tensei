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
import com.blissless.tensei.data.cache.saveMangaExploreToCache
import com.blissless.tensei.data.cache.loadMangaExploreFromCache
import com.blissless.tensei.data.manga.fetchMangaExploreFromMal

// Manga search and explore: simple + advanced search, explore fetching
// with MAL fallback and cache restore.



internal fun toMangaMedia(track: MangaTrack): MangaMedia {
    return MangaMedia(
        id = track.mangaId,
        title = track.title,
        titleEnglish = track.titleEnglish,
        cover = track.cover,
        progress = track.progress.toInt(),
        totalChapters = track.totalChapters,
        totalVolumes = track.totalVolumes,
        listStatus = track.status,
        listEntryId = track.listEntryId,
        malId = track.malId,
        scrollProgress = track.scrollProgress,
        currentChapterPages = track.currentChapterPages,
        scrollChapterId = track.scrollChapterId,
        scrollChapterNumber = track.scrollChapterNumber,
        userScore = track.score,
        averageScore = track.averageScore
    )
}


// â”€â”€â”€ Search & Explore â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Basic manga search â€” kept for backward compat. Returns results via callback.
 * For new UI, prefer [searchMangaAdvanced] which returns via a StateFlow.
 */
fun MainViewModel.searchManga(query: String, page: Int = 1, onResult: (List<MangaExploreMedia>) -> Unit = {}) {
    viewModelScope.launch {
        val results = mangaRepository?.searchManga(query, page) ?: emptyList()
        onResult(results)
    }
}


/**
 * Advanced manga search with genre/format/status/sort filters. Returns results synchronously
 * (caller should call from a coroutine scope).
 */
suspend fun MainViewModel.searchMangaAdvanced(
    search: String?,
    genres: List<String> = emptyList(),
    format: String? = null,
    status: String? = null,
    sort: String = "SEARCH_MATCH",
    page: Int = 1,
    perPage: Int = 30
): List<MangaExploreMedia> {
    return mangaRepository?.searchMangaAdvanced(search, genres, format, status, sort, page, perPage) ?: emptyList()
}


suspend fun MainViewModel.fetchMangaExplore(silent: Boolean = false) {
    if (!silent) _isLoadingManga.value = true
    // Pass the auth token when available â€” AniList may treat authenticated requests differently
    // during rate-limiting/outages (HTTP 403 "API temporarily disabled").
    val token = authToken.value
    var usedMalFallback = false
    var sections = mangaRepository?.fetchExploreSections(token) ?: run {
        android.util.Log.w("MangaExplore", "fetchMangaExplore: mangaRepository is null or fetchExploreSections returned null")
        emptyMap()
    }
    // AniList unavailable: fall back to MAL manga ranking so the explore screen still
    // shows trending/popular/top rows instead of nothing. Only sections that are currently
    // EMPTY are filled — previously-loaded AniList rows are never replaced with MAL data.
    // Genre rows stay empty (MAL has no genre ranking).
    if (sections.isEmpty()) {
        android.util.Log.w("MangaExplore", "fetchMangaExplore: AniList explore empty — using MAL ranking fallback")
        val malSections = mangaRepository?.fetchMangaExploreFromMal() ?: emptyMap()
        if (malSections.isNotEmpty()) {
            usedMalFallback = true
            // Production fallback: only fill section keys that have no data yet.
            val existing = _mangaExploreSections.value
            val merged = existing.toMutableMap()
            for ((key, list) in malSections) {
                if (list.isNotEmpty() && (existing[key] ?: emptyList()).isEmpty()) merged[key] = list
            }
            sections = merged
        }
    }
    // Only overwrite existing data with a successful fetch â€” never wipe cached sections
    // with an empty response (network hiccup or API outage).
    if (sections.isNotEmpty()) {
        _mangaExploreSections.value = sections
        if (usedMalFallback) {
            // MAL data is a transient stopgap — never let it overwrite the persisted
            // AniList snapshot, so a restart still starts from real AniList data.
            _mangaExploreSource.value = "mal"
        } else {
            cacheManager.saveMangaExploreToCache(sections)
            _mangaExploreSource.value = "anilist"
        }
    }
    if (!silent) _isLoadingManga.value = false
}


/**
 * Auto-recovery from a MAL-fallback manga explore: while the manga explore screen is showing
 * MAL ranking data (AniList unavailable), the screen periodically calls this to retry AniList.
 * The reload is visible (loading skeleton) so the user sees the MAL → AniList swap. As soon as
 * AniList answers, the regular success path replaces the MAL rows, the source flips back to
 * "anilist", and the data is persisted.
 */
fun MainViewModel.retryMangaExploreFromMalFallback() {
    if (_mangaExploreSource.value != "mal") return
    viewModelScope.launch { fetchMangaExplore() }
}


/**
 * Restore the persisted manga explore sections so the Explore screen shows content
 * immediately on startup instead of waiting for a fresh fetch.
 */
fun MainViewModel.restoreMangaExploreFromCache() {
    val cached = cacheManager.loadMangaExploreFromCache()
    if (!cached.isNullOrEmpty()) {
        _mangaExploreSections.value = cached
    }
}
