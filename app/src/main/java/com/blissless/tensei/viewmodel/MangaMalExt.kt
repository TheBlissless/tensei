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
import com.blissless.tensei.data.prefs.getMalMangaFavorites
import com.blissless.tensei.data.prefs.saveMalMangaFavorites

// MAL (MyAnimeList) fallback for manga: favorites sync, list fetching,
// status mapping and id resolution.



// â”€â”€â”€ MAL manga sync & favorites â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** MAL manga favorites held locally (MAL has no favorite-write API) keyed by AniList/manga id. */
internal val _malMangaFavorites = MutableStateFlow<Set<Int>>(emptySet())

val MainViewModel.malMangaFavorites: StateFlow<Set<Int>> get() = _malMangaFavorites.asStateFlow()


/**
 * Fetch the user's MAL manga list and merge it into local manga tracking, mirroring
 * [fetchMalList] for anime. MAL entries are matched to AniList manga by the manga's
 * `idMal` when known, falling back to the MAL id itself. Used as the MAL fallback when
 * AniList is unavailable, and for the diff-sync/reconcile path.
 */
internal suspend fun MainViewModel.fetchMalMangaList() {
    if (!isMalActive) return

    val entries = malApiService.getMangaList()
    val localTracker = mangaTrackManager ?: return

    for (entry in entries) {
        val malId = entry.node.id
        val status = entry.list_status?.status
        val progress = entry.list_status?.num_chapters_read ?: 0
        val score = entry.list_status?.score
        val updatedAt = parseMalUpdatedAt(entry.list_status?.updated_at)

        // Resolve an AniList manga key via idMal when possible; else fall back to the MAL id.
        val mangaKey = resolveMangaIdForMal(malId) ?: malId

        val title = entry.node.alternative_titles?.en ?: entry.node.title

        localTracker.ensureTrack(mangaKey, title, entry.node.main_picture?.large ?: entry.node.main_picture?.medium ?: "", entry.node.num_chapters, null, null, null, malId, mediaStatus = mapMangaMediaStatusFromMal(entry.node.status), updatedAt = updatedAt)
        localTracker.updateTrackingStatus(mangaKey, mapMangaStatusFromMal(status), updatedAt = updatedAt)
        if (progress > 0) localTracker.updateChapterProgressKeepMax(mangaKey, progress.toFloat())
        if (score != null && score > 0) localTracker.updateScore(mangaKey, (score * 10).coerceAtMost(100), updatedAt = updatedAt)
    }

    loadLocalMangaTracking()
    reconcileAllCompletions()
    loadMalMangaFavoritesFromCache()
    saveHomeDataToCache()
}


/** Map an AniList manga id â†’ its MAL id (idMal), using detail + tracked lists. */
internal fun MainViewModel.resolveMangaIdForMal(malId: Int): Int? {
    _mangaDetail.value?.takeIf { it.malId == malId }?.id?.let { return it }
    val inLists = _mangaContinueReading.value + _mangaCurrentlyReading.value +
        _mangaPlanningToRead.value + _mangaCompleted.value +
        _mangaPaused.value + _mangaDropped.value
    return inLists.firstOrNull { it.malId == malId }?.id
}


/** Map a MAL manga status to an AniList manga status. */
internal fun mapMangaStatusFromMal(malStatus: String?): String {
    return when (malStatus) {
        "reading" -> "CURRENT"
        "plan_to_read" -> "PLANNING"
        "completed" -> "COMPLETED"
        "on_hold" -> "PAUSED"
        "dropped" -> "DROPPED"
        else -> "PLANNING"
    }
}


/** Map a MAL manga PUBLISHING status to an AniList media status. */
internal fun mapMangaMediaStatusFromMal(malStatus: String?): String? = when (malStatus) {
    "currently_publishing" -> "RELEASING"
    "finished" -> "FINISHED"
    "on_hiatus" -> "HIATUS"
    "discontinued" -> "CANCELLED"
    "not_yet_published" -> "NOT_YET_RELEASED"
    else -> null
}


/** Restore locally-persisted MAL manga favorites. */
internal fun MainViewModel.loadMalMangaFavoritesFromCache() {
    _malMangaFavorites.value = userPreferences.getMalMangaFavorites()
}


/**
 * Toggle a manga's MAL "favorite" state. Since MAL's public API has no write endpoint for
 * profile favorites, this is purely local (mirrors anime's toggleMalFavoriteById), keyed by
 * the manga's AniList id. AniList favoriting remains API-backed and is handled separately.
 */
fun MainViewModel.toggleMalMangaFavorite(mangaId: Int) {
    val current = _malMangaFavorites.value
    _malMangaFavorites.value = if (mangaId in current) current - mangaId else current + mangaId
    userPreferences.saveMalMangaFavorites(_malMangaFavorites.value.toList())
}


/** True when the manga (by AniList id) is in the local MAL-favorites set. */
fun MainViewModel.isMangaMalFavorited(mangaId: Int): Boolean = mangaId in _malMangaFavorites.value
