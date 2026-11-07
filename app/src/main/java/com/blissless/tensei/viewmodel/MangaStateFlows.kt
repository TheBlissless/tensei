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

// Manga state: every manga-related StateFlow (lists, explore, detail,
// chapters, chapter images, profile), their jobs and caches, plus init and
// local-tracking restore. Bodies of the domain logic live in the other
// Manga*Ext files in this package.



// â”€â”€â”€ State Flows â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

internal val _mangaContinueReading = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaContinueReading: StateFlow<List<MangaMedia>> get() = _mangaContinueReading.asStateFlow()


internal val _mangaCurrentlyReading = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaCurrentlyReading: StateFlow<List<MangaMedia>> get() = _mangaCurrentlyReading.asStateFlow()


internal val _mangaPlanningToRead = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaPlanningToRead: StateFlow<List<MangaMedia>> get() = _mangaPlanningToRead.asStateFlow()


internal val _mangaCompleted = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaCompleted: StateFlow<List<MangaMedia>> get() = _mangaCompleted.asStateFlow()


internal val _mangaPaused = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaPaused: StateFlow<List<MangaMedia>> get() = _mangaPaused.asStateFlow()


internal val _mangaDropped = MutableStateFlow<List<MangaMedia>>(emptyList())

val MainViewModel.mangaDropped: StateFlow<List<MangaMedia>> get() = _mangaDropped.asStateFlow()


internal val _mangaExploreSections = MutableStateFlow<Map<String, List<MangaExploreMedia>>>(emptyMap())

val MainViewModel.mangaExploreSections: StateFlow<Map<String, List<MangaExploreMedia>>> get() = _mangaExploreSections.asStateFlow()


// Latest data source driving the manga explore rows: "anilist" or "mal" (fallback while
// AniList is down/unreachable). Drives automatic recovery — see retryMangaExploreFromMalFallback().
internal val _mangaExploreSource = MutableStateFlow<String?>(null)

val MainViewModel.mangaExploreSource: StateFlow<String?> get() = _mangaExploreSource.asStateFlow()


internal val _mangaDetail = MutableStateFlow<MangaDetail?>(null)

val MainViewModel.mangaDetail: StateFlow<MangaDetail?> get() = _mangaDetail.asStateFlow()


// Latest data source for the most recently fetched manga detail: "anilist" or "mal".
// Drives automatic recovery on the detail + "View All" screens.
internal val _mangaDetailSource = MutableStateFlow<String?>(null)

val MainViewModel.mangaDetailSource: StateFlow<String?> get() = _mangaDetailSource.asStateFlow()


// In-memory copies of the "View All" relations/recommendations lists, keyed by the manga
// id they were opened for. clearMangaDetail() wipes _mangaDetail when popping back from a
// child detail, so these keep the restored View All screens seeded instantly instead of
// re-spinning the loader.
internal val _mangaAllRelationsCache = MutableStateFlow<Map<Int, List<MangaRelation>>>(emptyMap())

internal val _mangaAllRecommendationsCache = MutableStateFlow<Map<Int, List<MangaMedia>>>(emptyMap())


internal val _mangaChapters = MutableStateFlow<List<MangaChapter>>(emptyList())

val MainViewModel.mangaChapters: StateFlow<List<MangaChapter>> get() = _mangaChapters.asStateFlow()


/**
 * The extension-derived total chapter count for the currently loaded manga (the display
 * denominator, e.g. 354 for a releasing title whose AniList entry is stale at 352).
 * 0 until a chapter list has been loaded. Used by the detail screen so its "Chapters" stats
 * match the extension count instead of the stale AniList value.
 */
internal val _mangaTotalChapters = MutableStateFlow<Int>(0)

val MainViewModel.mangaTotalChapters: StateFlow<Int> get() = _mangaTotalChapters.asStateFlow()


/**
 * Chapter image loading state â€” null = loading, empty list = error/no source,
 * non-empty = success. Surfaces errors distinctly so the reader can render an error UI.
 */
internal val _mangaChapterImages = MutableStateFlow<List<String>?>(null)

val MainViewModel.mangaChapterImages: StateFlow<List<String>?> get() = _mangaChapterImages.asStateFlow()


/** Human-readable error message when chapter images fail to load. Null = no error. */
internal val _mangaChapterImagesError = MutableStateFlow<String?>(null)

val MainViewModel.mangaChapterImagesError: StateFlow<String?> get() = _mangaChapterImagesError.asStateFlow()


/**
 * Headers to attach to chapter image requests from a manga extension source
 * (User-Agent + source Referer, taken from the source's own HttpSource.headers).
 * Null for the built-in scrape path, which falls back to a URL-derived Referer.
 */
internal val _mangaImageHeaders = MutableStateFlow<okhttp3.Headers?>(null)

val MainViewModel.mangaImageHeaders: StateFlow<okhttp3.Headers?> get() = _mangaImageHeaders.asStateFlow()


/**
 * Cached image URL lists keyed by chapterId. Populated by [loadChapterImages] on success and
 * by [prefetchMangaChapterImages] (which scrapes the NEXT chapter while the reader is near the
 * end of the current one). Serving from this cache makes chapter transitions â€” including
 * auto-advance â€” instant instead of waiting on a scrape round-trip.
 */
internal val _mangaChapterImagesCache = MutableStateFlow<Map<String, List<String>>>(emptyMap())


/** Chapter IDs with an in-flight prefetch, to avoid duplicate scrapes. */
internal val _prefetchingChapterIds = mutableSetOf<String>()


/** In-flight chapter-image load. Cancelled whenever a new chapter is requested so only the
 *  most recent load can write to [MainViewModel.mangaChapterImages] â€” otherwise a slower,
 *  stale fetch from a previous chapter (rapid next/prev taps) would overwrite the new one. */
internal var chapterImagesJob: Job? = null


// ─── External (Mihon) manga extension state ───────────────────────────────
// Set when mangaSourceMethod == "external". Maps the AniList manga id to the
// resolved source + its SManga handle (needed to rebuild the chapter list), and
// the routing chapterId to the source + SChapter used to fetch pages.
internal val _externalMangaCache = mutableMapOf<Int, Pair<Source, SManga>>()

internal val _externalChapterCache = mutableMapOf<String, Pair<Source, SChapter>>()


fun MainViewModel.clearMangaChapterImagesCache() {
    chapterImagesJob?.cancel()
    _mangaChapterImagesCache.value = emptyMap()
    _prefetchingChapterIds.clear()
}


internal val _mangaDexId = MutableStateFlow<String?>(null)

val MainViewModel.mangaDexId: StateFlow<String?> get() = _mangaDexId.asStateFlow()


internal val _mangaExtensionTitle = MutableStateFlow<String?>(null)

val MainViewModel.mangaExtensionTitle: StateFlow<String?> get() = _mangaExtensionTitle.asStateFlow()


internal val _isLoadingManga = MutableStateFlow(false)

val MainViewModel.isLoadingManga: StateFlow<Boolean> get() = _isLoadingManga.asStateFlow()


internal val _isLoadingMangaChapters = MutableStateFlow(false)

val MainViewModel.isLoadingMangaChapters: StateFlow<Boolean> get() = _isLoadingMangaChapters.asStateFlow()


// Tracks whether a chapter load has COMPLETED (success or failure) for the current manga.
// While false, the reader shows the loading state immediately instead of flashing the
// "No chapters found" empty state on the first frame before loadMangaChapters kicks in.
internal val _hasLoadedMangaChapters = MutableStateFlow(false)

val MainViewModel.hasLoadedMangaChapters: StateFlow<Boolean> get() = _hasLoadedMangaChapters.asStateFlow()


/** In-flight chapter-list load. Kept so the reader's "Loading chapters…" screen can cancel a
 *  stalled load, mirroring the anime stream loader's cancel button. */
internal var mangaChaptersJob: Job? = null


/** Cancel an in-flight chapter-list load. The reader stops spinning and falls back to its
 *  "no chapters found" state instead of waiting forever on a hanging extension. */
fun MainViewModel.cancelMangaChapterLoad() {
    mangaChaptersJob?.cancel()
    mangaChaptersJob = null
    _isLoadingMangaChapters.value = false
    _hasLoadedMangaChapters.value = true
}


// Profile state â€” real AniList favorites and activity, not faked from tracking lists
internal val _mangaFavorites = MutableStateFlow<List<MangaFavorite>>(emptyList())

val MainViewModel.mangaFavorites: StateFlow<List<MangaFavorite>> get() = _mangaFavorites.asStateFlow()


internal val _mangaActivity = MutableStateFlow<List<MangaActivityNode>>(emptyList())

val MainViewModel.mangaActivity: StateFlow<List<MangaActivityNode>> get() = _mangaActivity.asStateFlow()


internal val _mangaUserProfile = MutableStateFlow<com.blissless.tensei.data.models.MangaUserProfile?>(null)

val MainViewModel.mangaUserProfile: StateFlow<com.blissless.tensei.data.models.MangaUserProfile?> get() = _mangaUserProfile.asStateFlow()


/** Track-set of manga the current user has favorited on AniList (for heart-icon state). */
internal val _favoritedMangaIds = MutableStateFlow<Set<Int>>(emptySet())

val MainViewModel.favoritedMangaIds: StateFlow<Set<Int>> get() = _favoritedMangaIds.asStateFlow()


// â”€â”€â”€ Initialization â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

fun MainViewModel.initManga() {
    _mangaRepository = MangaRepository()
    _mangaTrackManager = MangaTrackManager(context)
    _mangaDexManager = MangaDexManager()
    loadLocalMangaTracking()
    // App-start safety net: flip any CURRENT manga that already sits at its final chapter of a
    // FINISHED (released) series to COMPLETED, using media status persisted from prior syncs.
    reconcileAllCompletions()
    // Discover installed extensions on every init â€” cheap and surfaces new installs.
    discoverExtensions()
    // Restore the user's previously-selected extension authority (if any).
    restoreExtensionSelection()
}


internal fun MainViewModel.loadLocalMangaTracking() {
    val tracker = mangaTrackManager ?: return
    val reading = tracker.getContinueReading().map { toMangaMedia(it) }
    val currentlyReading = tracker.getCurrentlyReading().map { toMangaMedia(it) }
    val planning = tracker.getPlanningToRead().map { toMangaMedia(it) }
    val completed = tracker.getCompleted().map { toMangaMedia(it) }
    val paused = tracker.getPaused().map { toMangaMedia(it) }
    val dropped = tracker.getDropped().map { toMangaMedia(it) }
    _mangaContinueReading.value = reading
    _mangaCurrentlyReading.value = currentlyReading
    _mangaPlanningToRead.value = planning
    _mangaCompleted.value = completed
    _mangaPaused.value = paused
    _mangaDropped.value = dropped
}
