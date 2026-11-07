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
import com.blissless.tensei.data.manga.fetchMangaMalDetail

// Manga detail screen data: detail fetch, characters/staff, relations and
// recommendations (with their caches).



// â”€â”€â”€ Detail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

suspend fun MainViewModel.fetchMangaDetail(mangaId: Int, malId: Int? = null) {
    _isLoadingManga.value = true
    _mangaDetailSource.value = null
    val token = authToken.value
    var detail = mangaRepository?.fetchMangaDetail(mangaId, token)
    // MAL-sourced cards (explore / search fallback, or relations pushed from one) carry the
    // MAL id as their own id. Looking that value up in AniList can silently return an
    // unrelated Media that merely shares the same numeric id — which is why tapping a
    // fallback card used to open the wrong manga. Trust the AniList detail only when its
    // idMal matches the expected MAL id; otherwise discard it and fall through to MAL.
    if (detail != null && malId != null && malId > 0 && detail.malId != malId) {
        android.util.Log.w("MangaDetail", "fetchMangaDetail: AniList id=$mangaId is not MAL id $malId (got idMal=${detail.malId}) — discarding wrong media")
        detail = null
    }
    if (detail != null) _mangaDetailSource.value = "anilist"
    // AniList unavailable: fall back to the official MAL manga detail API so the
    // detail screen still renders a full page instead of only the shallow card data.
    // Entries that originated from a MAL fallback (explore/search/relations) carry the
    // MAL id as their own id, so use explicit malId when present, otherwise mangaId.
    val malIdToUse = malId?.takeIf { it > 0 } ?: mangaId
    if (detail == null && malIdToUse > 0) {
        android.util.Log.w("MangaDetail", "fetchMangaDetail: AniList failed — trying MAL manga detail fallback for malId=$malIdToUse")
        detail = mangaRepository?.fetchMangaMalDetail(malIdToUse)
        if (detail != null) {
            _mangaDetailSource.value = "mal"
        }
    }
    _mangaDetail.value = detail
    if (detail != null) {
        mangaTrackManager?.updateMangaInfo(mangaId, detail.title, detail.cover, detail.titleEnglish, detail.status)
    } else {
        android.util.Log.w("MangaDetail", "fetchMangaDetail: FAILED (null) mangaId=$mangaId â€” detail screen will fall back to shallow MangaMedia.asDetail()")
    }
    _isLoadingManga.value = false
}


fun MainViewModel.clearMangaDetail() {
    _mangaDetail.value = null
    _mangaChapters.value = emptyList()
    _mangaTotalChapters.value = 0
    _mangaChapterImages.value = null
    _mangaChapterImagesError.value = null
    _mangaDexId.value = null
    _mangaExtensionTitle.value = null
    _hasLoadedMangaChapters.value = false
}


/** Clear all AniList user-scoped manga state on logout. */
fun MainViewModel.clearMangaUserData() {
    _mangaContinueReading.value = emptyList()
    _mangaCurrentlyReading.value = emptyList()
    _mangaPlanningToRead.value = emptyList()
    _mangaCompleted.value = emptyList()
    _mangaPaused.value = emptyList()
    _mangaDropped.value = emptyList()
    _mangaFavorites.value = emptyList()
    _mangaActivity.value = emptyList()
    _mangaUserProfile.value = null
    _favoritedMangaIds.value = emptySet()
    _malMangaFavorites.value = emptySet()
    _mangaExploreSections.value = emptyMap()
    _mangaDetail.value = null
    _mangaChapters.value = emptyList()
    _mangaTotalChapters.value = 0
    _mangaChapterImages.value = null
    _mangaChapterImagesError.value = null
    _mangaChapterImagesCache.value = emptyMap()
    _mangaDexId.value = null
    _mangaExtensionTitle.value = null
    _hasLoadedMangaChapters.value = false
    _isLoadingManga.value = false
    _isLoadingMangaChapters.value = false
}


suspend fun MainViewModel.fetchMangaAllCharacters(mangaId: Int): List<MangaCharacterNode> =
    mangaRepository?.fetchMangaAllCharacters(mangaId) ?: emptyList()


suspend fun MainViewModel.fetchMangaAllStaff(mangaId: Int): List<MangaStaffEdge> =
    mangaRepository?.fetchMangaAllStaff(mangaId) ?: emptyList()


/// Entries resolved by the detail page — used to seed the "View All" relations/recommendations
/// screens so they render instantly instead of re-hitting the network.
fun MainViewModel.cachedMangaRelations(mangaId: Int, malId: Int? = null): List<MangaRelation> {
    mangaDetail.value?.takeIf { it.id == mangaId || (malId != null && malId > 0 && it.id == malId) }?.relations?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    return _mangaAllRelationsCache.value[mangaId].orEmpty()
}


fun MainViewModel.cachedMangaRecommendations(mangaId: Int, malId: Int? = null): List<MangaMedia> {
    mangaDetail.value?.takeIf { it.id == mangaId || (malId != null && malId > 0 && it.id == malId) }?.recommendations?.takeIf { it.isNotEmpty() }
        ?.let { return it }
    return _mangaAllRecommendationsCache.value[mangaId].orEmpty()
}


suspend fun MainViewModel.fetchMangaAllRelations(mangaId: Int, malId: Int? = null, force: Boolean = false): List<MangaRelation> {
    // The detail page already resolved relations, so serve those first — the "View All"
    // screen mirrors what the detail row already shows instead of re-hitting AniList.
    // force=true bypasses the cache so auto-recovery can re-check AniList.
    if (!force) cachedMangaRelations(mangaId, malId).takeIf { it.isNotEmpty() }?.let { return it }
    val relations = mangaRepository?.fetchMangaAllRelations(mangaId) ?: emptyList()
    if (relations.isNotEmpty()) {
        _mangaDetailSource.value = "anilist"
        _mangaAllRelationsCache.value = _mangaAllRelationsCache.value + (mangaId to relations)
        return relations
    }
    // Nothing loaded for this id yet: fetch the MAL manga detail directly. Entries that
    // originated from a MAL fallback carry the MAL id as their own id (see fetchMangaDetail).
    val idToUse = malId?.takeIf { it > 0 } ?: mangaId
    val malRelations = mangaRepository?.fetchMangaMalDetail(idToUse)?.relations.orEmpty()
    if (malRelations.isNotEmpty()) _mangaAllRelationsCache.value = _mangaAllRelationsCache.value + (mangaId to malRelations)
    return malRelations
}


suspend fun MainViewModel.fetchMangaAllRecommendations(mangaId: Int, malId: Int? = null, force: Boolean = false): List<MangaMedia> {
    if (!force) cachedMangaRecommendations(mangaId, malId).takeIf { it.isNotEmpty() }?.let { return it }
    val recommendations = mangaRepository?.fetchMangaAllRecommendations(mangaId) ?: emptyList()
    if (recommendations.isNotEmpty()) {
        _mangaDetailSource.value = "anilist"
        _mangaAllRecommendationsCache.value = _mangaAllRecommendationsCache.value + (mangaId to recommendations)
        return recommendations
    }
    val idToUse = malId?.takeIf { it > 0 } ?: mangaId
    val malRecommendations = mangaRepository?.fetchMangaMalDetail(idToUse)?.recommendations.orEmpty()
    if (malRecommendations.isNotEmpty()) {
        _mangaAllRecommendationsCache.value = _mangaAllRecommendationsCache.value + (mangaId to malRecommendations)
    }
    return malRecommendations
}
