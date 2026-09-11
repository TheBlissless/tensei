package com.blissless.tensei.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.blissless.tensei.api.myanimelist.MalMangaListEntry
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.FORCE_MAL_DETAIL_FOR_TESTING
import com.blissless.tensei.data.manga.MangaDexManager
import com.blissless.tensei.data.manga.MangaRepository
import com.blissless.tensei.data.manga.MangaTrackManager
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaTrack
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

data class InstalledExtension(val label: String, val packageName: String) {
    val authority: String get() {
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessMangaExtension(packageName)) {
            return packageName.removeSuffix(".manga") + ".provider"
        }
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessStreamExtension(packageName)) {
            return packageName.removeSuffix(".anime.stream") + ".provider"
        }
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessTorrentExtension(packageName)) {
            return packageName.removeSuffix(".anime.torrent") + ".provider"
        }
        return "$packageName.provider"
    }
}

data class ExtensionChapter(
    val number: String,
    val title: String,
    val id: String,
    val index: Int,
    val pageCount: Int
)

// â”€â”€â”€ Managers (lazily initialized from MainViewModel.init) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
var MainViewModel.mangaRepository: MangaRepository?
    get() = _mangaRepository
    set(value) { _mangaRepository = value }
private var _mangaRepository: MangaRepository? = null

var MainViewModel.mangaTrackManager: MangaTrackManager?
    get() = _mangaTrackManager
    set(value) { _mangaTrackManager = value }
private var _mangaTrackManager: MangaTrackManager? = null

var MainViewModel.mangaDexManager: MangaDexManager?
    get() = _mangaDexManager
    set(value) { _mangaDexManager = value }
private var _mangaDexManager: MangaDexManager? = null

// â”€â”€â”€ Extension State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val _installedExtensions = MutableStateFlow<List<InstalledExtension>>(emptyList())
val MainViewModel.installedExtensions: StateFlow<List<InstalledExtension>> get() = _installedExtensions.asStateFlow()

private val _selectedExtensionAuthority = MutableStateFlow<String?>(null)
val MainViewModel.selectedExtensionAuthority: StateFlow<String?> get() = _selectedExtensionAuthority.asStateFlow()

/**
 * Discover installed manga extensions. Detects by package name pattern
 * (com.blissless.*.manga) and falls back to beacon labels for backward compat.
 * Safe to call repeatedly.
 */
fun MainViewModel.discoverExtensions() {
    val pm = context.packageManager
    val extensions = mutableListOf<InstalledExtension>()
    val seenPackages = mutableSetOf<String>()

    // 1) Package-name-based detection for *.manga
    val installedPkgs = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(
                (android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.GET_CONFIGURATIONS).toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.GET_CONFIGURATIONS)
        }
    } catch (_: Exception) { emptyList() }
    for (pkg in installedPkgs) {
        val pkgName = pkg.packageName
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessMangaExtension(pkgName)) {
            seenPackages.add(pkgName)
            extensions.add(
                InstalledExtension(
                    label = com.blissless.tensei.extensions.ExtensionDetector.extensionDisplayName(pkgName),
                    packageName = pkgName
                )
            )
        }
    }

    // 2) Beacon fallback (backward compat)
    val beaconIntent = Intent("com.blissless.mangaclient.EXTENSION_BEACON")
    val resolveInfoList = context.packageManager.queryBroadcastReceivers(beaconIntent, 0)
    for (info in resolveInfoList) {
        val pkgName = info.activityInfo.packageName
        if (pkgName in seenPackages) continue
        val label = info.loadLabel(pm).toString()
        if (label.startsWith("Oni: ", ignoreCase = true) ||
            label.startsWith("Tensei: ", ignoreCase = true)) {
            extensions.add(InstalledExtension(label = label, packageName = pkgName))
        }
    }

    _installedExtensions.value = extensions.sortedBy { it.label }
}

/**
 * Select an extension as the active source for chapter lists and image fetching.
 * Persists across app restarts via UserPreferences.
 */
fun MainViewModel.selectExtension(authority: String?) {
    _selectedExtensionAuthority.value = authority
    userPreferences.setSelectedMangaExtensionAuthority(authority)
    _mangaTotalChapters.value = 0
    mangaTrackManager?.resetTotalChaptersForReleasing()
    loadLocalMangaTracking()
}

private fun MainViewModel.restoreExtensionSelection() {
    val saved = userPreferences.getSelectedMangaExtensionAuthority()
    if (saved != null) {
        // Migrate stale authorities from old package names (e.g. "com.blissless.x.manga.provider" -> "com.blissless.x.provider")
        val migrated = migratedAuthority(saved)
        if (migrated != saved) {
            userPreferences.setSelectedMangaExtensionAuthority(migrated)
        }
        _selectedExtensionAuthority.value = migrated
    }
}

private fun migratedAuthority(saved: String): String {
    if (!saved.endsWith(".provider")) return saved
    val withoutProvider = saved.removeSuffix(".provider")
    if (withoutProvider.endsWith(".manga")) return withoutProvider.removeSuffix(".manga") + ".provider"
    if (withoutProvider.endsWith(".anime.stream")) return withoutProvider.removeSuffix(".anime.stream") + ".provider"
    if (withoutProvider.endsWith(".anime.torrent")) return withoutProvider.removeSuffix(".anime.torrent") + ".provider"
    return saved
}

private suspend fun MainViewModel.fetchExtensionChapterList(mangaTitle: String): Pair<List<ExtensionChapter>?, Int>? {
    val authority = _selectedExtensionAuthority.value ?: return null
    if (mangaTitle.isBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("content://$authority/chapters")
                .buildUpon()
                .appendQueryParameter("manga", mangaTitle)
                .appendQueryParameter("anime", mangaTitle)
                .build()
            android.util.Log.d("MangaDebug", "=== CHAPTER FETCH START ===")
            android.util.Log.d("MangaDebug", "Authority: '$authority'")
            android.util.Log.d("MangaDebug", "URI: $uri")
            android.util.Log.d("MangaDebug", "Title: '$mangaTitle'")
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null) {
                android.util.Log.e("MangaDebug", "RESULT: cursor is NULL â€” content provider not found at authority='$authority'")
                android.util.Log.e("MangaDebug", "Check manifest: android:authorities should be '$authority'")
                return@withContext null
            }
            cursor.use { c ->
                android.util.Log.d("MangaDebug", "Cursor column count: ${c.columnCount}, column names: ${c.columnNames?.joinToString()}")
                if (!c.moveToFirst()) {
                    android.util.Log.e("MangaDebug", "RESULT: cursor has 0 rows")
                    return@withContext null
                }
                val col = c.getColumnIndex("data")
                if (col < 0) {
                    android.util.Log.e("MangaDebug", "RESULT: 'data' column not found. Available columns: ${c.columnNames?.joinToString()}")
                    return@withContext null
                }
                val jsonData = c.getString(col)
                android.util.Log.d("MangaDebug", "Raw JSON length: ${jsonData.length} chars")
                android.util.Log.d("MangaDebug", "Raw JSON (first 500): ${jsonData.take(500)}")
                val json = JSONObject(jsonData)
                if (json.has("error")) {
                    android.util.Log.e("MangaDebug", "Extension error: ${json.getString("error")}")
                    return@withContext null
                }
                android.util.Log.d("MangaDebug", "JSON keys: ${json.keys().asSequence().toList()}")
                val totalChapters = json.optInt("totalChapters", 0)
                val chaptersArr = json.optJSONArray("chapters")
                android.util.Log.d("MangaDebug", "totalChapters from JSON: $totalChapters")
                android.util.Log.d("MangaDebug", "chapters array is null: ${chaptersArr == null}")
                android.util.Log.d("MangaDebug", "chapters array length: ${chaptersArr?.length() ?: 0}")
                val chapters = mutableListOf<ExtensionChapter>()
                if (chaptersArr != null) {
                    for (i in 0 until chaptersArr.length()) {
                        val ch = chaptersArr.optJSONObject(i) ?: continue
                        chapters.add(
                            ExtensionChapter(
                                number = ch.optString("number", ""),
                                title = ch.optString("title", ""),
                                id = ch.optString("id", ""),
                                index = ch.optInt("index", i),
                                pageCount = ch.optInt("pageCount", 0)
                            )
                        )
                    }
                }
                android.util.Log.d("MangaDebug", "Parsed chapters count: ${chapters.size}")
                if (chapters.isNotEmpty()) {
                    android.util.Log.d("MangaDebug", "First chapter: number='${chapters.first().number}' title='${chapters.first().title}'")
                    android.util.Log.d("MangaDebug", "Last chapter: number='${chapters.last().number}' title='${chapters.last().title}'")
                }
                android.util.Log.d("MangaDebug", "=== CHAPTER FETCH END ===")
                Pair(chapters, totalChapters)
            }
        } catch (e: Exception) {
            android.util.Log.e("MangaDebug", "EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}

private fun MainViewModel.fetchExtensionChapterImages(mangaTitle: String, chapterParam: String, authority: String): List<String>? {
    android.util.Log.d("MangaExt", "fetchExtensionChapterImages: title='$mangaTitle' chapter='$chapterParam' authority='$authority'")
    return try {
        val uri = Uri.parse("content://$authority/scrape")
            .buildUpon()
            .appendQueryParameter("manga", mangaTitle)
            .appendQueryParameter("anime", mangaTitle)
            .appendQueryParameter("chapter", chapterParam)
            .build()
        android.util.Log.d("MangaExt", "fetchExtensionChapterImages: querying URI=$uri")
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        if (cursor == null) {
            android.util.Log.w("MangaExt", "fetchExtensionChapterImages: cursor is null")
            return null
        }
        cursor.use { c ->
            if (!c.moveToFirst()) {
                android.util.Log.w("MangaExt", "fetchExtensionChapterImages: cursor has no rows")
                return@use null
            }
            val col = c.getColumnIndex("data")
            if (col < 0) {
                android.util.Log.w("MangaExt", "fetchExtensionChapterImages: no 'data' column in cursor")
                return@use null
            }
            val jsonData = c.getString(col)
            android.util.Log.d("MangaExt", "fetchExtensionChapterImages: raw JSON (first 200 chars): ${jsonData.take(200)}")
            val json = JSONObject(jsonData)
            if (json.has("error")) {
                android.util.Log.w("MangaExt", "fetchExtensionChapterImages: extension error: ${json.optString("error")}")
                return@use null
            }
            val chapter = json.optJSONObject("chapter") ?: run {
                android.util.Log.w("MangaExt", "fetchExtensionChapterImages: no 'chapter' object in JSON")
                return@use null
            }
            val imagesArr = chapter.optJSONArray("images") ?: run {
                android.util.Log.w("MangaExt", "fetchExtensionChapterImages: no 'images' array in chapter")
                return@use null
            }
            val images = (0 until imagesArr.length()).map { imagesArr.getString(it) }
            android.util.Log.d("MangaExt", "fetchExtensionChapterImages: got ${images.size} images, first=${images.firstOrNull()?.take(80)}")
            images
        }
    } catch (e: Exception) {
        android.util.Log.w("MangaExt", "fetchExtensionChapterImages failed: ${e.message}", e)
        null
    }
}

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

private val _mangaExploreSections = MutableStateFlow<Map<String, List<MangaExploreMedia>>>(emptyMap())
val MainViewModel.mangaExploreSections: StateFlow<Map<String, List<MangaExploreMedia>>> get() = _mangaExploreSections.asStateFlow()

// Latest data source driving the manga explore rows: "anilist" or "mal" (fallback while
// AniList is down/unreachable). Drives automatic recovery — see retryMangaExploreFromMalFallback().
private val _mangaExploreSource = MutableStateFlow<String?>(null)
val MainViewModel.mangaExploreSource: StateFlow<String?> get() = _mangaExploreSource.asStateFlow()

private val _mangaDetail = MutableStateFlow<MangaDetail?>(null)
val MainViewModel.mangaDetail: StateFlow<MangaDetail?> get() = _mangaDetail.asStateFlow()

// Latest data source for the most recently fetched manga detail: "anilist" or "mal".
// Drives automatic recovery on the detail + "View All" screens.
private val _mangaDetailSource = MutableStateFlow<String?>(null)
val MainViewModel.mangaDetailSource: StateFlow<String?> get() = _mangaDetailSource.asStateFlow()

// In-memory copies of the "View All" relations/recommendations lists, keyed by the manga
// id they were opened for. clearMangaDetail() wipes _mangaDetail when popping back from a
// child detail, so these keep the restored View All screens seeded instantly instead of
// re-spinning the loader.
private val _mangaAllRelationsCache = MutableStateFlow<Map<Int, List<MangaRelation>>>(emptyMap())
private val _mangaAllRecommendationsCache = MutableStateFlow<Map<Int, List<MangaMedia>>>(emptyMap())

private val _mangaChapters = MutableStateFlow<List<MangaChapter>>(emptyList())
val MainViewModel.mangaChapters: StateFlow<List<MangaChapter>> get() = _mangaChapters.asStateFlow()

/**
 * The extension-derived total chapter count for the currently loaded manga (the display
 * denominator, e.g. 354 for a releasing title whose AniList entry is stale at 352).
 * 0 until a chapter list has been loaded. Used by the detail screen so its "Chapters" stats
 * match the extension count instead of the stale AniList value.
 */
private val _mangaTotalChapters = MutableStateFlow<Int>(0)
val MainViewModel.mangaTotalChapters: StateFlow<Int> get() = _mangaTotalChapters.asStateFlow()

/**
 * Chapter image loading state â€” null = loading, empty list = error/no source,
 * non-empty = success. Surfaces errors distinctly so the reader can render an error UI.
 */
private val _mangaChapterImages = MutableStateFlow<List<String>?>(null)
val MainViewModel.mangaChapterImages: StateFlow<List<String>?> get() = _mangaChapterImages.asStateFlow()

/** Human-readable error message when chapter images fail to load. Null = no error. */
private val _mangaChapterImagesError = MutableStateFlow<String?>(null)
val MainViewModel.mangaChapterImagesError: StateFlow<String?> get() = _mangaChapterImagesError.asStateFlow()

/**
 * Cached image URL lists keyed by chapterId. Populated by [loadChapterImages] on success and
 * by [prefetchMangaChapterImages] (which scrapes the NEXT chapter while the reader is near the
 * end of the current one). Serving from this cache makes chapter transitions â€” including
 * auto-advance â€” instant instead of waiting on a scrape round-trip.
 */
private val _mangaChapterImagesCache = MutableStateFlow<Map<String, List<String>>>(emptyMap())

/** Chapter IDs with an in-flight prefetch, to avoid duplicate scrapes. */
private val _prefetchingChapterIds = mutableSetOf<String>()

/** In-flight chapter-image load. Cancelled whenever a new chapter is requested so only the
 *  most recent load can write to [MainViewModel.mangaChapterImages] â€” otherwise a slower,
 *  stale fetch from a previous chapter (rapid next/prev taps) would overwrite the new one. */
private var chapterImagesJob: Job? = null

fun MainViewModel.clearMangaChapterImagesCache() {
    chapterImagesJob?.cancel()
    _mangaChapterImagesCache.value = emptyMap()
    _prefetchingChapterIds.clear()
}

private val _mangaDexId = MutableStateFlow<String?>(null)
val MainViewModel.mangaDexId: StateFlow<String?> get() = _mangaDexId.asStateFlow()

private val _mangaExtensionTitle = MutableStateFlow<String?>(null)
val MainViewModel.mangaExtensionTitle: StateFlow<String?> get() = _mangaExtensionTitle.asStateFlow()

private val _isLoadingManga = MutableStateFlow(false)
val MainViewModel.isLoadingManga: StateFlow<Boolean> get() = _isLoadingManga.asStateFlow()

private val _isLoadingMangaChapters = MutableStateFlow(false)
val MainViewModel.isLoadingMangaChapters: StateFlow<Boolean> get() = _isLoadingMangaChapters.asStateFlow()

// Tracks whether a chapter load has COMPLETED (success or failure) for the current manga.
// While false, the reader shows the loading state immediately instead of flashing the
// "No chapters found" empty state on the first frame before loadMangaChapters kicks in.
private val _hasLoadedMangaChapters = MutableStateFlow(false)
val MainViewModel.hasLoadedMangaChapters: StateFlow<Boolean> get() = _hasLoadedMangaChapters.asStateFlow()

// Profile state â€” real AniList favorites and activity, not faked from tracking lists
private val _mangaFavorites = MutableStateFlow<List<MangaFavorite>>(emptyList())
val MainViewModel.mangaFavorites: StateFlow<List<MangaFavorite>> get() = _mangaFavorites.asStateFlow()

private val _mangaActivity = MutableStateFlow<List<MangaActivityNode>>(emptyList())
val MainViewModel.mangaActivity: StateFlow<List<MangaActivityNode>> get() = _mangaActivity.asStateFlow()

private val _mangaUserProfile = MutableStateFlow<com.blissless.tensei.data.models.MangaUserProfile?>(null)
val MainViewModel.mangaUserProfile: StateFlow<com.blissless.tensei.data.models.MangaUserProfile?> get() = _mangaUserProfile.asStateFlow()

/** Track-set of manga the current user has favorited on AniList (for heart-icon state). */
private val _favoritedMangaIds = MutableStateFlow<Set<Int>>(emptySet())
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

private fun MainViewModel.loadLocalMangaTracking() {
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

private fun toMangaMedia(track: MangaTrack): MangaMedia {
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
    android.util.Log.d("MangaExplore", "fetchMangaExplore: start, silent=$silent, mangaRepository=${mangaRepository != null}")
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
    android.util.Log.d("MangaExplore", "fetchMangaExplore: got ${sections.size} sections, keys=${sections.keys}")
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
    val token = authToken.value ?: return
    // Optimistic update
    val current = _favoritedMangaIds.value
    _favoritedMangaIds.value = if (mangaId in current) current - mangaId else current + mangaId
    viewModelScope.launch {
        val ok = mangaRepository?.toggleMangaFavorite(mangaId, token) ?: false
        if (!ok) {
            // Revert on failure
            _favoritedMangaIds.value = current
        } else {
            // Refresh the favorites list to stay in sync
            val refreshed = mangaRepository?.fetchUserMangaFavorites(token) ?: emptyList()
            _mangaFavorites.value = refreshed
            _favoritedMangaIds.value = refreshed.map { it.id }.toSet()
        }
    }
}

/** Returns true if the given manga is currently favorited on AniList. */
fun MainViewModel.isMangaFavorited(mangaId: Int): Boolean = mangaId in _favoritedMangaIds.value

// â”€â”€â”€ Detail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

suspend fun MainViewModel.fetchMangaDetail(mangaId: Int, malId: Int? = null) {
    android.util.Log.d("MangaDetail", "fetchMangaDetail: START mangaId=$mangaId malId=$malId")
    _isLoadingManga.value = true
    _mangaDetailSource.value = null
    val token = authToken.value
    var detail = if (FORCE_MAL_DETAIL_FOR_TESTING) null else mangaRepository?.fetchMangaDetail(mangaId, token)
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
            android.util.Log.d("MangaDetail", "fetchMangaDetail: MAL fallback OK mangaId=$mangaId title='${detail.title}'")
        }
    }
    _mangaDetail.value = detail
    if (detail != null) {
        mangaTrackManager?.updateMangaInfo(mangaId, detail.title, detail.cover, detail.titleEnglish, detail.status)
        android.util.Log.d("MangaDetail", "fetchMangaDetail: SUCCESS mangaId=$mangaId title='${detail.title}' " +
            "desc=${detail.description != null} genres=${detail.genres.size} tags=${detail.tags.size} " +
            "chars=${detail.characters?.nodes?.size ?: 0} staff=${detail.staff?.edges?.size ?: 0} " +
            "relations=${detail.relations.size} recs=${detail.recommendations.size} " +
            "popularity=${detail.popularity} favourites=${detail.favourites} year=${detail.year} " +
            "format=${detail.format} source=${detail.source} volumes=${detail.volumes} " +
            "rankings=${detail.rankings.size} externalLinks=${detail.externalLinks.size}")
    } else {
        android.util.Log.w("MangaDetail", "fetchMangaDetail: FAILED (null) mangaId=$mangaId â€” detail screen will fall back to shallow MangaMedia.asDetail()")
    }
    _isLoadingManga.value = false
    android.util.Log.d("MangaDetail", "fetchMangaDetail: END mangaId=$mangaId isLoadingManga=${_isLoadingManga.value}")
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

// â”€â”€â”€ Chapters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Load the chapter list for a manga. Matches oni's exact flow:
 *
 * 1. Fetch MangaDex aggregate FIRST (for count/volume metadata only â€” NOT for the chapter list)
 * 2. Fetch extension chapter list (atsu.moe) â€” this is the actual source of truth
 * 3. If extension returns chapters â†’ use them with URL scheme `anilist_${mediaId}_ch_${number}`
 * 4. If extension returns nothing â†’ synthesize 1..N fallback using AniList/MangaDex count
 *
 * The chapter URL is a routing token, NOT a real URL. The chapter number is later extracted
 * from the chapter TITLE (not the URL) when scraping images, matching oni's contract.
 */
suspend fun MainViewModel.loadMangaChapters(mangaId: Int, title: String) {
    android.util.Log.d("MangaChapters", "loadMangaChapters: mangaId=$mangaId title='$title'")
    _isLoadingMangaChapters.value = true
    _hasLoadedMangaChapters.value = false
    _mangaTotalChapters.value = 0
    mangaTrackManager?.resetTotalChapters(mangaId)

    // Without a manga extension there is no real chapter source, so skip the synthetic
    // fallback (AniList chapter count) entirely â€” it is wrong for releasing manga. The reader
    // shows the "select an extension" screen instead of a bogus list.
    if (_selectedExtensionAuthority.value == null) {
        android.util.Log.w("MangaChapters", "loadMangaChapters: no extension selected â€” skipping chapter list generation")
        _mangaChapters.value = emptyList()
        _hasLoadedMangaChapters.value = true
        _isLoadingMangaChapters.value = false
        return
    }

    val detail = _mangaDetail.value
    android.util.Log.d("MangaChapters", "loadMangaChapters: detail=${detail != null} detail.chapters=${detail?.chapters} detail.title=${detail?.title}")
    var chapters = emptyList<MangaChapter>()

    // Resolve the title â€” prefer English, then Romaji, then the passed-in title
    val resolvedTitle = detail?.titleEnglish?.takeIf { it.isNotBlank() }
        ?: detail?.title?.takeIf { it.isNotBlank() }
        ?: title
    android.util.Log.d("MangaChapters", "loadMangaChapters: resolvedTitle='$resolvedTitle'")

    // --- 1. Fetch MangaDex aggregate for count metadata (NOT for the chapter list) ---
    var mdLatestChapter: Int? = null
    var mdVolumeCount: Int? = null
    val mangaDexId = mangaDexManager?.findMangaByAniListId(resolvedTitle, mangaId)
    android.util.Log.d("MangaChapters", "loadMangaChapters: MangaDex lookup -> mangaDexId=$mangaDexId")
    _mangaDexId.value = mangaDexId
    if (mangaDexId != null) {
        val aggregate = mangaDexManager?.fetchAggregate(mangaDexId)
        if (aggregate != null) {
            // Get the highest chapter number from the aggregate (not the count of entries,
            // which can be wrong due to volumes/others grouping)
            val maxChapter = aggregate.volumes?.values?.flatMap { vol ->
                vol.chapters?.values?.mapNotNull { it.chapter?.toFloatOrNull()?.toInt() } ?: emptyList()
            }?.maxOrNull()
            mdLatestChapter = maxChapter
            mdVolumeCount = aggregate.volumes?.size
            android.util.Log.d("MangaChapters", "loadMangaChapters: MangaDex aggregate maxChapter=$maxChapter volumes=${mdVolumeCount}")
            mangaTrackManager?.updateMangaDexId(mangaId, mangaDexId)
        }
    }

    // --- 2. Fetch extension chapter list (atsu.moe) â€” the actual source of truth ---
    val titlesToTry = listOfNotNull(
        resolvedTitle,
        detail?.titleEnglish?.takeIf { it.isNotBlank() },
        detail?.title?.takeIf { it.isNotBlank() },
        title
    ).distinct()
    android.util.Log.d("MangaChapters", "loadMangaChapters: titlesToTry=$titlesToTry")

    var extChapters: List<ExtensionChapter>? = null
    var extTotalChapters = 0
    var matchedTitle: String? = null
    for (t in titlesToTry) {
        android.util.Log.d("MangaChapters", "loadMangaChapters: trying extension with title='$t'")
        val extResult = fetchExtensionChapterList(t)
        val resultChapters = extResult?.first
        val resultTotal = extResult?.second ?: 0
        // Keep the highest totalChapters seen across attempts â€” a later failed/null result
        // must not wipe the count reported by an earlier successful query.
        if (resultTotal > extTotalChapters) extTotalChapters = resultTotal
        android.util.Log.d("MangaChapters", "loadMangaChapters: extension returned ${resultChapters?.size ?: 0} chapters (total=$resultTotal) for '$t'")
        if (resultChapters != null && resultChapters.isNotEmpty()) {
            extChapters = resultChapters
            matchedTitle = t
            break
        }
    }

    if (extChapters != null && extChapters.isNotEmpty()) {
        android.util.Log.d("MangaDebug", "PATH: extension chapters (count=${extChapters.size}, totalChapters=$extTotalChapters)")
        _mangaExtensionTitle.value = matchedTitle
        // Build ChapterInfo list with oni's URL scheme: anilist_${mediaId}_ch_${number}
        // Sort by extension's index (oldest-first, chapter 1 at index 0)
        chapters = extChapters.sortedBy { it.index }.mapIndexed { idx, ch ->
            MangaChapter(
                url = "anilist_${mangaId}_ch_${ch.number}",
                title = if (ch.title.isNotBlank()) "Chapter ${ch.number}: ${ch.title}" else "Chapter ${ch.number}",
                chapterId = "anilist_${mangaId}_ch_${ch.number}",
                chapterNumber = ch.number.toFloatOrNull() ?: idx.toFloat()
            )
        }
    } else if (extTotalChapters > 0) {
        // The extension reports the CURRENT release count (totalChapters) but returned no
        // chapter entries for this title (e.g. the source page only exposes the count).
        // Use the extension total â€” the authoritative current number â€” as the chapter list.
        android.util.Log.d("MangaChapters", "loadMangaChapters: extension returned only totalChapters=$extTotalChapters, building synthetic list")
        _mangaExtensionTitle.value = null
        chapters = (1..extTotalChapters).map { i ->
            MangaChapter(
                url = "anilist_${mangaId}_ch_$i",
                title = "Chapter $i",
                chapterId = "anilist_${mangaId}_ch_$i",
                chapterNumber = i.toFloat()
            )
        }
    } else {
        // Extension gave no data at all (e.g. offline). Fall back to synthetic chapter list.
        android.util.Log.d("MangaChapters", "loadMangaChapters: no extension chapters, using synthetic fallback")
        _mangaExtensionTitle.value = null
        // --- 3. Fallback: synthetic chapter list from the best available count ---
        // Only reached when the extension returned neither chapters nor a total.
        // Priority: MangaDex latest > AniList chapters > 0
        // AniList often returns chapters=null for ongoing manga, so it's the last resort.
        val fallbackTotal = when {
            mdLatestChapter != null && mdLatestChapter > 0 -> mdLatestChapter
            detail?.chapters != null && detail.chapters > 0 -> detail.chapters
            else -> 0
        }
        android.util.Log.d("MangaChapters", "loadMangaChapters: fallbackTotal=$fallbackTotal (mdLatest=$mdLatestChapter, detail.chapters=${detail?.chapters})")
        if (fallbackTotal > 0) {
            chapters = (1..fallbackTotal).map { i ->
                MangaChapter(
                    url = "anilist_${mangaId}_ch_$i",
                    title = "Chapter $i",
                    chapterId = "anilist_${mangaId}_ch_$i",
                    chapterNumber = i.toFloat()
                )
            }
        }
    }

    android.util.Log.d("MangaDebug", "loadMangaChapters: final chapters.size=${chapters.size}")
    _mangaChapters.value = chapters

    // Count only integer chapters for the display total â€” semi-chapters (e.g. 238.5) should
    // not inflate the denominator or count as full chapters toward AniList progress.
    // Use tolerance-based check to handle float imprecision (e.g. 238.00002f).
    val integerChapterCount = chapters.count { ch ->
        ch.chapterNumber > 0f && (ch.chapterNumber - ch.chapterNumber.toInt()) < 0.001f
    }
    android.util.Log.d("MangaChapters", "loadMangaChapters: chapters.size=${chapters.size} integerChapterCount=$integerChapterCount extTotalChapters=$extTotalChapters")
    chapters.filter { ch ->
        ch.chapterNumber > 0f && (ch.chapterNumber - ch.chapterNumber.toInt()) >= 0.001f
    }.forEach { ch ->
        android.util.Log.d("MangaChapters", "  EXCLUDED chapter: title='${ch.title}' chapterNumber=${ch.chapterNumber} diff=${ch.chapterNumber - ch.chapterNumber.toInt()}")
    }

    // Display denominator for progress (e.g. 149/354). Prefer the extension's CURRENT release
    // count: AniList's chapters field is stale for releasing manga (e.g. Blue Lock stuck at 352
    // while the extension already has 354), so it must not override the extension total.
    // extTotalChapters covers the case where the extension returns only a partial list (e.g.
    // just the latest few chapters), which would otherwise make progress look like 149/3.
    // When the extension provides only a total (no chapter entries), that total includes
    // semi-chapters, so fall back to the integer-only count from the actual chapter list.
    val displayTotalChapters = when {
        extTotalChapters > 0 && extChapters != null && extChapters.isNotEmpty() -> integerChapterCount
        extChapters != null && extChapters.isNotEmpty() -> integerChapterCount
        detail?.chapters != null && detail.chapters > 0 -> detail.chapters
        else -> integerChapterCount
    }
    android.util.Log.d("MangaChapters", "loadMangaChapters: displayTotalChapters=$displayTotalChapters")
    _mangaTotalChapters.value = displayTotalChapters
    mangaTrackManager?.updateTotalChapters(
        mangaId,
        displayTotalChapters,
        mdVolumeCount ?: detail?.volumes
    )
    loadLocalMangaTracking()
    _hasLoadedMangaChapters.value = true
    _isLoadingMangaChapters.value = false
}

// â”€â”€â”€ Chapter Images â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Load the image URL list for a single chapter. Matches oni's exact flow:
 *
 * 1. Extract the chapter number from the chapter TITLE (not the URL)
 *    Title format: "Chapter 346" or "Chapter 346.2: Some Title" â†’ "346" or "346.2"
 * 2. Resolve the manga title (extension-matched title > detail title > passed title)
 * 3. Call the extension's /scrape endpoint with manga=<title>&chapter=<number>
 * 4. Parse the {chapter:{images:[...]}} JSON response
 *
 * If no extension is selected, shows an error telling the user to pick one in Settings.
 */
fun MainViewModel.loadChapterImages(chapterId: String, useDataSaver: Boolean = false, mangaTitle: String? = null, chapterTitle: String? = null, mangaId: Int = 0) {
    chapterImagesJob?.cancel()
    chapterImagesJob = viewModelScope.launch {
        // Serve from the prefetch cache first so chapter transitions (incl. auto-advance)
        // load instantly instead of waiting on a scrape round-trip.
        _mangaChapterImagesCache.value[chapterId]?.let { cached ->
            android.util.Log.d("MangaReader", "loadChapterImages: served ${cached.size} images from cache for chapterId='$chapterId'")
            _mangaChapterImages.value = cached
            _mangaChapterImagesError.value = null
            return@launch
        }

        _mangaChapterImages.value = null
        _mangaChapterImagesError.value = null

        android.util.Log.d("MangaReader", "loadChapterImages: chapterId='$chapterId' chapterTitle='$chapterTitle' mangaTitle='$mangaTitle' mangaId=$mangaId")

        // Extract the chapter number from the chapter TITLE (matching oni's contract)
        // Title format: "Chapter 346" or "Chapter 346.2: Some Title" â†’ "346" or "346.2"
        val chapterParam = chapterTitle?.let { title ->
            title.removePrefix("Chapter ").substringBefore(":").trim()
        } ?: chapterId.substringAfterLast("_ch_").trim()

        android.util.Log.d("MangaReader", "loadChapterImages: extracted chapterParam='$chapterParam'")

        // Resolve the manga title for the extension
        val extTitle = _mangaExtensionTitle.value
            ?: _mangaDetail.value?.titleEnglish?.takeIf { it.isNotBlank() }
            ?: _mangaDetail.value?.title?.takeIf { it.isNotBlank() }
            ?: mangaTitle
            ?: ""

        if (extTitle.isBlank()) {
            android.util.Log.w("MangaReader", "loadChapterImages: no manga title available")
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = "Could not determine the manga title for the extension."
            return@launch
        }

        val authority = _selectedExtensionAuthority.value
        if (authority == null) {
            android.util.Log.w("MangaReader", "loadChapterImages: no extension selected")
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = "No manga extension selected. Pick one in Settings â†’ Extensions to read this chapter."
            return@launch
        }

        android.util.Log.d("MangaReader", "loadChapterImages: calling extension scrape with title='$extTitle' chapter='$chapterParam' authority='$authority'")

        val images = withContext(Dispatchers.IO) {
            fetchExtensionChapterImages(extTitle, chapterParam, authority)
        }

        android.util.Log.d("MangaReader", "loadChapterImages: extension returned ${images?.size ?: 0} images")
        images?.take(3)?.forEachIndexed { i, url -> android.util.Log.d("MangaReader", "  image[$i]: ${url.take(200)}") }
        images?.lastOrNull()?.let { android.util.Log.d("MangaReader", "  image[last]: ${it.take(200)}") }

        if (images == null) {
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = "Failed to load chapter images. Check your connection and try again."
        } else if (images.isEmpty()) {
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = "This chapter has no pages."
        } else {
            _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (chapterId to images)
            _mangaChapterImages.value = images
        }
    }
}

/**
 * Scrape the NEXT chapter's image list in the background and cache it, so that advancing to it
 * (manually or via auto-advance) is instant. Never touches [mangaChapterImages], so the reader
 * keeps showing the current chapter. Skips chapters that are already cached or in flight.
 */
fun MainViewModel.prefetchMangaChapterImages(chapter: MangaChapter?, mangaTitle: String? = null, mangaId: Int = 0) {
    val next = chapter ?: return
    if (_mangaChapterImagesCache.value.containsKey(next.chapterId)) return
    if (!_prefetchingChapterIds.add(next.chapterId)) return

    viewModelScope.launch {
        try {
            val authority = _selectedExtensionAuthority.value
            if (authority == null) return@launch
            val extTitle = _mangaExtensionTitle.value
                ?: _mangaDetail.value?.titleEnglish?.takeIf { it.isNotBlank() }
                ?: _mangaDetail.value?.title?.takeIf { it.isNotBlank() }
                ?: mangaTitle
                ?: return@launch
            if (extTitle.isBlank()) return@launch

            val chapterParam = next.title.removePrefix("Chapter ").substringBefore(":").trim()

            android.util.Log.d("MangaReader", "prefetchMangaChapterImages: chapterId='${next.chapterId}' chapter='$chapterParam' title='$extTitle'")

            val images = withContext(Dispatchers.IO) {
                fetchExtensionChapterImages(extTitle, chapterParam, authority)
            }
            if (images != null && images.isNotEmpty()) {
                _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (next.chapterId to images)
                android.util.Log.d("MangaReader", "prefetchMangaChapterImages: cached ${images.size} images for chapterId='${next.chapterId}'")
            }
        } finally {
            _prefetchingChapterIds.remove(next.chapterId)
        }
    }
}

fun MainViewModel.clearChapterImages() {
    _mangaChapterImages.value = null
    _mangaChapterImagesError.value = null
}

// â”€â”€â”€ Tracking â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Mark a chapter as read locally. If the user has no track for this manga yet, one is auto-created
 * with status CURRENT (matching oni's behavior).
 *
 * The AniList progress push is NOT done here â€” [onMangaScrollProgress] schedules a single
 * debounced (3s) AniList update when the sync threshold is reached. This function is called on
 * every scroll frame above the threshold, so pushing here would fire one mutation per frame and
 * spam/rate-limit the API (which silently breaks later updates, including manual status changes).
 */
fun MainViewModel.markMangaChapterRead(mangaId: Int, chapter: MangaChapter, mangaTitle: String = "", mangaCover: String = "") {
    android.util.Log.d("MangaSyncDebug", "markMangaChapterRead mangaId=$mangaId chapterNumber=${chapter.chapterNumber} title='$mangaTitle'")
    // The track writes and the tracking-list refresh are pure SharedPreferences work. Run them
    // off the main thread so the threshold-crossing frame (the scroll/page callback that
    // triggered this) doesn't hitch the reader. The reader's own "read" checkmark updates
    // immediately via markChapterReadInListUi (in-memory), so local-first UX is unaffected.
    viewModelScope.launch(Dispatchers.Default) {
        // Ensure a track exists so progress is recorded for first-time readers
        mangaTrackManager?.ensureTrack(mangaId, mangaTitle, mangaCover)
        mangaTrackManager?.markChapterComplete(mangaId, chapter)
        loadLocalMangaTracking()
    }
}

/**
 * Called when a chapter is opened in the reader. NOTE: this intentionally does nothing â€” a local
 * track is created lazily only once the user actually reads (see updateMangaScrollProgress), so
 * merely opening a chapter does not add the manga to tracking. Retained (empty) as a stable API
 * so the reader's "opened" call site still documents the intended entry point.
 */
fun MainViewModel.startMangaChapter(mangaId: Int, mangaTitle: String = "", mangaCover: String = "") {
}

/** Reload the local manga tracking lists (Continue Reading / Planning / Completed). */
fun MainViewModel.refreshMangaTracking() {
    loadLocalMangaTracking()
}

fun MainViewModel.updateMangaProgress(mangaId: Int, progress: Float) {
    android.util.Log.d("MangaSyncDebug", "updateMangaProgress mangaId=$mangaId progress=$progress")
    mangaTrackManager?.updateChapterProgress(mangaId, progress)
    loadLocalMangaTracking()
}

// â”€â”€â”€ Manga â†’ AniList sync queue (local-first, debounced) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Mirrors the anime queueSync/executePendingSyncs pattern: the UI is updated
// immediately from local tracking and the AniList mutation is pushed in the
// background after a short debounce. Rapid changes for the same manga coalesce
// into a single mutation (status/progress fields merge, latest non-null wins).
// Failed pushes are re-queued so they retry on the next debounce cycle.

private data class PendingMangaSync(
    val type: String,
    val mediaId: Int,
    val status: String? = null,
    val progress: Int? = null,
    val entryId: Int? = null,
    val score: Int? = null,
    val malId: Int? = null
)

private val pendingMangaSyncs = mutableMapOf<Int, PendingMangaSync>()
private var mangaSyncJob: Job? = null

// Scroll-progress persistence is throttled so the reader doesn't write SharedPreferences
// (full JSON encode of every track) on every scroll frame â€” that per-frame write, plus the
// repeated threshold-crossing work, was the source of jank once the autosync threshold hit.
private const val MANGA_SCROLL_PERSIST_INTERVAL_MS = 500L
private const val MANGA_SYNC_RETRY_MS = 30_000L
private var lastMangaScrollPersistTime = 0L

// Mangas whose local track was already ensured this session. Lets the first real scroll
// progress create the Continue Reading track without decoding all tracks from prefs on
// every frame just to check existence.
private val mangaTrackEnsured = mutableSetOf<Int>()

// Chapters already marked read + pushed to AniList this session, keyed "$mangaId:$chapterId".
// The threshold-crossing path (track writes, tracking-list refresh, debounced sync re-queue)
// must run once per chapter â€” otherwise every scroll frame past the threshold re-runs it.
private val mangaReadSyncedChapters = mutableSetOf<String>()

private fun MainViewModel.queueMangaSync(
    mediaId: Int,
    type: String,
    status: String? = null,
    progress: Int? = null,
    entryId: Int? = null,
    score: Int? = null,
    malId: Int? = null
) {
    val existing = pendingMangaSyncs[mediaId]
    pendingMangaSyncs[mediaId] = PendingMangaSync(
        type = type,
        mediaId = mediaId,
        status = status ?: existing?.status,
        progress = progress ?: existing?.progress,
        entryId = entryId ?: existing?.entryId,
        score = score ?: existing?.score,
        malId = malId ?: existing?.malId
    )
    mangaSyncJob?.cancel()
    // Run the push on a background dispatcher. The mutation is a network call, and the
    // post-push refresh (fetchMangaLists) merges every AniList row into local tracking with
    // per-item SharedPreferences reads/writes â€” none of that may run on the main thread, or
    // the reader janks ~2s after progress crosses the sync threshold.
    mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
        delay(MainViewModel.SYNC_DEBOUNCE_MS)
        // Run the push to completion even if a newer sync is queued mid-flight.
        // Cancelling the executing job would abort the network call in progress and
        // skip the re-queue for failed pushes â€” rapid chapter flips (next-chapter
        // button) hit this constantly, silently dropping AniList updates.
        withContext(NonCancellable) { executeMangaPendingSyncs() }
    }
}

/**
 * Flush any pending manga â†’ AniList syncs immediately, skipping the debounce.
 * Called when the reader closes so progress read in the last few seconds isn't
 * lost if the app is killed within the debounce window.
 */
fun MainViewModel.flushMangaSync() {
    if (pendingMangaSyncs.isEmpty()) return
    mangaSyncJob?.cancel()
    // Background dispatcher â€” see queueMangaSync for why the sync must not run on main.
    mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
        withContext(NonCancellable) { executeMangaPendingSyncs() }
    }
}

private suspend fun MainViewModel.executeMangaPendingSyncs() {
    val syncs = pendingMangaSyncs.toMap()
    pendingMangaSyncs.clear()
    if (syncs.isEmpty()) return

    val token = authToken.value
    var didPush = false
    for ((_, sync) in syncs) {
        val malMangaId = resolveMalMangaId(sync.mediaId)
        val ok = when (sync.type) {
            "status", "progress" -> {
                val status = sync.status
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.status
                    ?: "CURRENT"
                var result = true
                // Push to AniList whenever AniList is active (alone or as part of BOTH).
                if (isAniListActive && token != null) {
                    val aniListId = resolveMangaAniListIdForPush(sync)
                    if (aniListId != sync.mediaId) rekeyMangaTrackIfNeeded(sync.mediaId, aniListId, sync.malId)
                    val r = mangaRepository?.updateMangaStatus(aniListId, status, token, sync.progress, score = sync.score)
                    result = result && (r == true)
                }
                // Push to MAL whenever MAL is active (using the AniList manga's idMal).
                if (isMalActive && malMangaId != null) {
                    val malStatus = mapMangaStatusToMal(status)
                    val r = malApiService.updateMangaStatus(
                        malMangaId,
                        malStatus?.takeIf { sync.type != "progress" },
                        sync.score?.let { (it / 10f).roundToInt().coerceIn(0, 10) },
                        sync.progress
                    )
                    result = result && r
                }
                result
            }
            "score" -> {
                val score = sync.score
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.score
                    ?: 0
                val status = sync.status
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.status
                    ?: "CURRENT"
                var result = true
                if (isAniListActive && token != null) {
                    val aniListId = resolveMangaAniListIdForPush(sync)
                    if (aniListId != sync.mediaId) rekeyMangaTrackIfNeeded(sync.mediaId, aniListId, sync.malId)
                    val r = mangaRepository?.updateMangaStatus(aniListId, status, token, sync.progress, score = score)
                    result = result && (r == true)
                }
                if (isMalActive && malMangaId != null) {
                    val r = malApiService.updateMangaStatus(
                        malMangaId,
                        mapMangaStatusToMal(status),
                        (score / 10f).roundToInt().coerceIn(0, 10),
                        sync.progress
                    )
                    result = result && r
                }
                result
            }
            "delete" -> {
                var result = true
                if (isAniListActive && token != null) {
                    val entryId = sync.entryId
                    if (entryId != null) {
                        val r = mangaRepository?.deleteMangaListEntry(entryId, token)
                        result = result && (r == true)
                    }
                }
                if (isMalActive) {
                    // Prefer the MAL id captured at queue time: after a delete the manga is removed
                    // from the local lists, so on-demand resolution (resolveMalMangaId) can no longer
                    // find it. Fall back to resolution for syncs that predate the new field.
                    val malMangaId = sync.malId ?: resolveMalMangaId(sync.mediaId)
                    if (malMangaId != null) {
                        result = result && malApiService.deleteMangaFromList(malMangaId)
                    }
                }
                result
            }
            else -> null
        }
        android.util.Log.d("MangaSyncDebug", "executeMangaPendingSyncs: type=${sync.type} mediaId=${sync.mediaId} malId=$malMangaId ok=$ok")
        if (ok == true) {
            didPush = true
        } else {
            // Re-queue the failed sync so it retries on the next debounce cycle
            pendingMangaSyncs[sync.mediaId] = sync
        }
    }

    if (didPush) {
        // Refresh the lists so local + remote stay in sync. Prefer AniList; fall back to MAL.
        if (isAniListActive && _userId.value != null) {
            fetchMangaLists()
        } else if (isMalActive) {
            fetchMalMangaList()
        }
        loadLocalMangaTracking()
        // After any manga change (status, progress, or delete), reconcile MAL to keep it in exact
        // parity with AniList — prunes MAL-only entries and mirrors status/score/progress.
        if (isBothActive && !userPreferences.malAsMainProvider.value) {
            runCrossProviderDiffSync()
        }
    }

    // Retry anything that failed (e.g. the AniList API being down during the debounce) on a
    // short cycle so the change lands automatically once the API recovers — a failed push is
    // otherwise only retried when the user makes another change.
    if (pendingMangaSyncs.isNotEmpty()) {
        mangaSyncJob?.cancel()
        mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(MANGA_SYNC_RETRY_MS)
            withContext(NonCancellable) { executeMangaPendingSyncs() }
        }
    }
}

/** Resolve the MAL manga id for an AniList manga (the manga's `idMal`). */
private fun MainViewModel.resolveMalMangaId(animeMangaId: Int): Int? {
    _mangaDetail.value?.takeIf { it.id == animeMangaId }?.malId?.let { return it }
    val inLists = _mangaContinueReading.value + _mangaCurrentlyReading.value +
        _mangaPlanningToRead.value + _mangaCompleted.value +
        _mangaPaused.value + _mangaDropped.value
    return inLists.firstOrNull { it.id == animeMangaId }?.malId
}

/**
 * Resolve the real AniList manga id for a pending AniList push. Manga opened/added through a
 * MAL fallback (search/explore/detail while AniList was down) carry the MAL id as their own
 * id, and AniList has no Media under that number — pushing it unchanged would fail forever and
 * the manga would never land on AniList. Whenever the local id is really a MAL id
 * (idMal == id, as threaded through [MainViewModel.updateMangaStatus]), the id is translated
 * to AniList's id via the idMal lookup. Genuine AniList ids are returned unchanged.
 */
private suspend fun MainViewModel.resolveMangaAniListIdForPush(sync: PendingMangaSync): Int {
    val mediaId = sync.mediaId
    val known = _mangaContinueReading.value + _mangaCurrentlyReading.value +
        _mangaPlanningToRead.value + _mangaCompleted.value +
        _mangaPaused.value + _mangaDropped.value
    // A list entry whose id is mediaId with a DIFFERENT malId → genuine AniList id.
    known.firstOrNull { it.id == mediaId && it.malId != null && it.malId != mediaId }?.let { return mediaId }
    // A list entry that holds mediaId as its MAL id → that entry's own id is the AniList id
    // (covers reader/progress pushes for a manga that was already re-keyed).
    known.firstOrNull { it.malId != null && it.malId == mediaId }?.let { e ->
        if (e.id != mediaId) return e.id
    }
    // Id verbatim from a caller that carried the manga's detail context (falls back to the
    // track's stored malId). Null → no way to translate, keep the id as-is.
    val track = mangaTrackManager?.getTrack(mediaId)
    val malId = sync.malId ?: track?.malId
    if (malId == null || malId <= 0) return mediaId
    if (malId != mediaId) return mediaId // genuine AniList id with a different MAL id.
    // mediaId == malId → MAL-origin entry added through a fallback (its local id IS the MAL
    // id). Present in the lists under this id does NOT make it genuine — translate by idMal so
    // the add actually lands on AniList's entry for this manga.
    val resolved = mangaRepository?.findMangaByMalId(malId, authToken.value)
    val aniListId = resolved?.id?.takeIf { it > 0 } ?: mediaId
    if (aniListId != mediaId) {
        android.util.Log.d("MangaSyncDebug", "resolveMangaAniListIdForPush: mediaId=$mediaId is the MAL id → AniList id=$aniListId")
    }
    return aniListId
}

/**
 * Re-key a local manga track from its MAL-origin id to its real AniList id once the id is
 * known, so the AniList list merge can't create a duplicate track and future pushes reuse the
 * correct id. Read-chapter bookkeeping moves with the id so already-synced chapters are not
 * pushed twice after the re-key.
 */
private fun MainViewModel.rekeyMangaTrackIfNeeded(oldId: Int, newId: Int, malId: Int?) {
    if (oldId == newId) return
    val tracker = mangaTrackManager ?: return
    val track = tracker.getTrack(oldId) ?: return
    android.util.Log.d("MangaSyncDebug", "rekeyMangaTrackIfNeeded: $oldId -> $newId title='${track.title}'")
    tracker.removeTrack(oldId)
    tracker.addTrack(track.copy(mangaId = newId, malId = malId ?: track.malId))
    mangaTrackEnsured.remove(oldId)
    mangaTrackEnsured.add(newId)
    val prefixOld = "$oldId:"
    val moved = mangaReadSyncedChapters.filter { it.startsWith(prefixOld) }
    mangaReadSyncedChapters.removeAll { it.startsWith(prefixOld) }
    mangaReadSyncedChapters.addAll(moved.map { newId.toString() + it.removePrefix(prefixOld) })
    loadLocalMangaTracking()
}

/** Map an AniList manga status (CURRENT/PLANNING/COMPLETED/PAUSED/DROPPED) to MAL. */
internal fun mapMangaStatusToMal(status: String): String? {
    return when (status) {
        "CURRENT" -> "reading"
        "PLANNING" -> "plan_to_read"
        "COMPLETED" -> "completed"
        "PAUSED" -> "on_hold"
        "DROPPED" -> "dropped"
        else -> null
    }
}

/**
 * Handle scroll progress from the reader. This mirrors oni's onChapterScrollProgress flow:
 * - Always save scroll progress locally
 * - When scroll reaches the threshold (default 90%), mark the chapter as read
 * - If the chapter number is an integer (not partial like 12.5), push the new progress
 *   to AniList through the debounced background sync queue (rapid chapter flips coalesce
 *   into a single mutation)
 *
 * @param mangaId The AniList manga ID
 * @param chapter The chapter being read
 * @param scrollPercent 0.0 - 1.0 scroll progress
 * @param mangaTitle Title for track creation
 * @param mangaCover Cover URL for track creation
 */
fun MainViewModel.onMangaScrollProgress(
    mangaId: Int,
    chapter: MangaChapter?,
    scrollPercent: Float,
    mangaTitle: String = "",
    mangaCover: String = ""
): Boolean {
    if (chapter == null) return false
    if (!scrollPercent.isFinite()) return false

    // Always save scroll progress locally (this also lazily creates the track on first progress)
    updateMangaScrollProgress(mangaId, scrollPercent, mangaTitle, mangaCover, chapter)

    val threshold = userPreferences.mangaSyncThreshold.value / 100f
    if (scrollPercent >= threshold) {
        // Mark the chapter read (and push to AniList) only ONCE per chapter â€” on the first frame
        // that crosses the threshold. Without this guard every subsequent scroll frame re-runs the
        // track writes, the tracking-list StateFlow refresh, and the debounced sync re-queue,
        // which janks the reader once the threshold is reached.
        val readKey = "$mangaId:${chapter.chapterId}"
        if (mangaReadSyncedChapters.add(readKey)) {
            android.util.Log.d("MangaSyncDebug", "THRESHOLD CROSSED mangaId=$mangaId scrollPercent=$scrollPercent chapterNumber=${chapter.chapterNumber}")
            // Mark chapter as read (creates track if needed, updates local progress)
            markMangaChapterRead(mangaId, chapter, mangaTitle, mangaCover)
            // The chapter is now consumed by the auto-sync (progress pushed to AniList). Rule:
            // it must NOT leave a Continue Reading card behind — and any pre-threshold scroll
            // saved for it is dropped here so no stale card lingers.
            mangaTrackManager?.updateScrollProgress(mangaId, 0f)
            // A released (FINISHED) manga that reaches its final chapter is completed
            // automatically; on-going manga never is — new chapters may still arrive.
            reconcileCompletion(mangaId, chapter.chapterNumber, reading = true)

            // Schedule the AniList progress push through the debounced sync queue.
            // Only for integer chapter numbers (skip partial chapters like 12.5)
            val chapterNum = chapter.chapterNumber
            val isIntegerChapter = chapterNum > 0f && chapterNum == chapterNum.toInt().toFloat()
            if (isIntegerChapter) {
                queueMangaSync(mangaId, "progress", progress = chapterNum.toInt())
            }
            return true
        }
    }
    return false
}

/**
 * Complete a manga automatically whenever a RELEASED (media status FINISHED) manga reaches
 * its final chapter: flip the local track to COMPLETED and push the status change. This is the
 * one shared rule — the reader sync threshold, the change-status dialog, and the startup sweep
 * all funnel into it, so a finished manga is never silently left at 100% under CURRENT while a
 * still-running (RELEASING) series caught up to its latest release is never falsely completed.
 */
private fun MainViewModel.reconcileCompletion(mangaId: Int, progressOverride: Float = -1f, reading: Boolean = false) {
    val track = mangaTrackManager?.getTrack(mangaId) ?: return
    if (track.status == "COMPLETED") return
    val finished = track.mediaStatus == "FINISHED" || _mangaDetail.value?.status == "FINISHED"
    if (!finished) return
    if (track.totalChapters <= 0) return
    val effectiveProgress = if (progressOverride >= 0f) progressOverride else track.progress
    if (effectiveProgress < track.totalChapters) return
    // Reading a chapter implies an active CURRENT manga. For dialog-driven changes only a
    // CURRENT manga is auto-completed, so an explicit PAUSED/DROPPED/PLANNING choice is never
    // overridden.
    if (!reading && track.status != "CURRENT") return
    android.util.Log.d("MangaSyncDebug", "AUTO-COMPLETE: mangaId=$mangaId progress=${effectiveProgress} totalChapters=${track.totalChapters} mediaStatus=${track.mediaStatus} reading=$reading")
    mangaTrackManager?.updateTrackingStatus(mangaId, "COMPLETED")
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "status", status = "COMPLETED", progress = track.totalChapters)
}

/** Startup/refresh safety net: sweep every locally-tracked CURRENT manga for the completion rule. */
private fun MainViewModel.reconcileAllCompletions() {
    val tracker = mangaTrackManager ?: return
    var changed = false
    for (track in tracker.allTracks()) {
        if (track.status != "CURRENT") continue
        if (track.mediaStatus != "FINISHED") continue
        if (track.totalChapters <= 0 || track.progress < track.totalChapters) continue
        tracker.updateTrackingStatus(track.mangaId, "COMPLETED")
        queueMangaSync(track.mangaId, "status", status = "COMPLETED", progress = track.totalChapters)
        changed = true
    }
    if (changed) {
        loadLocalMangaTracking()
        android.util.Log.d("MangaSyncDebug", "reconcileAllCompletions: completed $changed manga")
    }
}

/** Set the AniList sync threshold (75-100%). Persists across restarts. */
fun MainViewModel.setMangaSyncThreshold(percent: Int) {
    userPreferences.setMangaSyncThreshold(percent)
}

fun MainViewModel.updateMangaScrollProgress(mangaId: Int, scrollProgress: Float, mangaTitle: String = "", mangaCover: String = "", chapter: MangaChapter? = null) {
    // Guard against NaN/Infinity — same defensive pattern as oni's TrackingManager
    val safe = if (scrollProgress.isNaN() || scrollProgress.isInfinite()) 0f else scrollProgress
    // In-chapter resume card: created lazily on the FIRST real scroll (past page 1), so closing
    // the reader mid-chapter BEFORE the sync threshold leaves a "continue at this spot" card.
    // Opening a chapter alone (scroll stays 0) still never tracks. Once a chapter is consumed by
    // the auto-sync — the sync threshold crossed this session, or progress already covers it from
    // an earlier session — it must NOT produce a card again, even if the user scrolls back below
    // the threshold; only the status/progress update applies from then on.
    var suppressCard = false
    val chapterKey = if (chapter != null) "$mangaId:${chapter.chapterId}" else null
    if (chapterKey != null && chapterKey in mangaReadSyncedChapters) {
        suppressCard = true
    } else if (safe > 0f && chapter != null && mangaId !in mangaTrackEnsured) {
        // First real scroll for this manga this session: decide once (with a single track
        // decode) whether this chapter can card. Progress-covered chapters are treated like
        // already-synced ones; otherwise create the track so the stop position can be saved.
        val track = mangaTrackManager?.ensureTrack(mangaId, mangaTitle, mangaCover)
        if (track != null && chapter.chapterNumber > 0f && chapter.chapterNumber <= track.progress) {
            mangaReadSyncedChapters.add(chapterKey!!)
            suppressCard = true
        } else {
            mangaTrackEnsured.add(mangaId)
        }
    }
    // Persist scroll progress — throttled, because writing SharedPreferences (full JSON encode of
    // all tracks) on every scroll frame is the jank source once the reader is at/over the sync
    // threshold. Resets to 0 (opening a non-resume chapter) are rare and must land immediately so
    // stale Continue Reading cards clear correctly; the final persisted value of a scroll gesture
    // stays within one interval of the actual position, which is plenty for resume. The chapter is
    // stored alongside the fraction so Continue Reading stays attached to the chapter the scroll
    // was made in.
    val now = SystemClock.elapsedRealtime()
    if (!suppressCard && (safe <= 0f || now - lastMangaScrollPersistTime >= MANGA_SCROLL_PERSIST_INTERVAL_MS)) {
        mangaTrackManager?.updateScrollProgress(mangaId, safe, chapter)
        lastMangaScrollPersistTime = now
    }
}

/** Persist the page count of the chapter currently being read so home can show "pages left". */
fun MainViewModel.updateMangaChapterPages(mangaId: Int, pages: Int) {
    if (pages <= 0) return
    mangaTrackManager?.updateChapterPages(mangaId, pages)
}

fun MainViewModel.updateMangaStatus(mangaId: Int, status: String, progress: Int? = null, score: Int? = null, malId: Int? = null, title: String = "", cover: String = "") {
    val effectiveStatus = status.ifBlank { "CURRENT" }
    android.util.Log.d("MangaSyncDebug", "updateMangaStatus mangaId=$mangaId status='$status' effectiveStatus='$effectiveStatus' progress=$progress score=$score malId=$malId title='$title'")
    // Local-first: apply the change immediately so the UI reacts instantly, then
    // queue the AniList push for the background debounced sync. Create the track WITH the
    // manga's title/cover so the home/library cards render real data instead of an empty
    // placeholder (a bare status change used to mint a blank track).
    mangaTrackManager?.ensureTrack(mangaId, title = title, cover = cover, malId = malId)
    mangaTrackManager?.updateTrackingStatus(mangaId, effectiveStatus)
    if (progress != null) {
        mangaTrackManager?.updateChapterProgress(mangaId, progress.toFloat())
    }
    if (score != null) {
        mangaTrackManager?.updateScore(mangaId, score)
    }
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "status", status = effectiveStatus, progress = progress, score = score, malId = malId)
    // Re-apply the auto-complete rule after the dialog: a CURRENT manga whose progress now
    // sits on its final chapter of a FINISHED (released) series becomes COMPLETED. An explicit
    // PAUSED/DROPPED/PLANNING choice is untouched.
    reconcileCompletion(mangaId, progress?.toFloat() ?: -1f, reading = false)
}

/** Set the AniList score (0-100) for a manga, local-first with a debounced remote push. */
fun MainViewModel.updateMangaScore(mangaId: Int, score: Int, malId: Int? = null) {
    android.util.Log.d("MangaSyncDebug", "updateMangaScore mangaId=$mangaId score=$score malId=$malId")
    mangaTrackManager?.updateScore(mangaId, score)
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "score", score = score, malId = malId)
}

fun MainViewModel.removeMangaTracking(mangaId: Int) {
    android.util.Log.d("MangaSyncDebug", "removeMangaTracking mangaId=$mangaId")
    // Resolve the AniList list-entry id and the MAL id (needed by the remote deletes) from the
    // local track or the in-memory lists BEFORE the track is removed. Without the entry id the
    // AniList entry survives, and without the MAL id the MAL entry survives (deletion happens on a
    // debounced background queue, by which point the manga is gone from the local lists and can no
    // longer be resolved) — each would be silently resurrected / left behind.
    val track = mangaTrackManager?.getTrack(mangaId)
    val knownEntryId = track?.listEntryId
        ?: listOf(
            _mangaContinueReading.value,
            _mangaCurrentlyReading.value,
            _mangaPlanningToRead.value,
            _mangaCompleted.value,
            _mangaPaused.value,
            _mangaDropped.value
        ).flatten().firstOrNull { it.id == mangaId }?.listEntryId
    val knownMalId = track?.malId
        ?: listOf(
            _mangaContinueReading.value,
            _mangaCurrentlyReading.value,
            _mangaPlanningToRead.value,
            _mangaCompleted.value,
            _mangaPaused.value,
            _mangaDropped.value
        ).flatten().firstOrNull { it.id == mangaId }?.malId
    mangaTrackManager?.removeTrack(mangaId)
    // Forget the ensure-track flag so re-reading this manga later recreates its local track.
    mangaTrackEnsured.remove(mangaId)
    mangaReadSyncedChapters.removeAll { it.startsWith("$mangaId:") }
    loadLocalMangaTracking()

    // Both AniList and MAL need their own id for the remote delete. If we resolved both, queue
    // immediately. If either is missing (older track without malId, or track never saw a list
    // sync), fetch the AniList list once to fill in the blanks before deleting from each provider.
    // We must always know the MAL id — on-demand resolution after removal can't find it in the
    // (now empty) local lists, so the MAL entry would be left behind.
    if (knownEntryId != null && knownMalId != null) {
        android.util.Log.d("MangaSyncDebug", "removeMangaTracking: queuing delete mediaId=$mangaId entryId=$knownEntryId malId=$knownMalId")
        queueMangaSync(mangaId, "delete", entryId = knownEntryId, malId = knownMalId)
    } else {
        viewModelScope.launch {
            val token = authToken.value
            val userId = _userId.value
            if (token != null && userId != null) {
                val fetched = mangaRepository?.fetchUserMangaLists(userId, token)
                val listEntry = fetched?.values?.flatten()?.firstOrNull { it.id == mangaId }
                val entryId = listEntry?.listEntryId ?: knownEntryId
                val malId = listEntry?.malId ?: knownMalId
                if (entryId != null || malId != null) {
                    android.util.Log.d("MangaSyncDebug", "removeMangaTracking: queuing delete (from fetch) mediaId=$mangaId entryId=$entryId malId=$malId")
                    queueMangaSync(mangaId, "delete", entryId = entryId, malId = malId)
                }
            }
        }
    }
}

/**
 * Dismiss a manga from the Continue Reading row only. Clears the in-chapter reading
 * state so the resume card disappears, but keeps the manga in its status list
 * (mirrors anime's removeContinueWatchingEntry, which doesn't untrack the anime).
 */
fun MainViewModel.dismissMangaContinueReading(mangaId: Int) {
    android.util.Log.d("MangaSyncDebug", "dismissMangaContinueReading mangaId=$mangaId")
    mangaTrackManager?.clearChapterProgress(mangaId)
    loadLocalMangaTracking()
}

// â”€â”€â”€ MAL manga sync & favorites â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/** MAL manga favorites held locally (MAL has no favorite-write API) keyed by AniList/manga id. */
private val _malMangaFavorites = MutableStateFlow<Set<Int>>(emptySet())
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
    android.util.Log.d("MalSync", "fetchMalMangaList: got ${entries.size} manga entries (isMalActive=$isMalActive)")
    val localTracker = mangaTrackManager ?: return

    for (entry in entries) {
        val malId = entry.node.id
        val status = entry.list_status?.status
        val progress = entry.list_status?.num_chapters_read ?: 0
        val score = entry.list_status?.score

        // Resolve an AniList manga key via idMal when possible; else fall back to the MAL id.
        val mangaKey = resolveMangaIdForMal(malId) ?: malId

        val title = entry.node.alternative_titles?.en ?: entry.node.title

        localTracker.ensureTrack(mangaKey, title, entry.node.main_picture?.large ?: entry.node.main_picture?.medium ?: "", entry.node.num_chapters, null, null, null, malId, mediaStatus = mapMangaMediaStatusFromMal(entry.node.status))
        localTracker.updateTrackingStatus(mangaKey, mapMangaStatusFromMal(status))
        if (progress > 0) localTracker.updateChapterProgressKeepMax(mangaKey, progress.toFloat())
        if (score != null && score > 0) localTracker.updateScore(mangaKey, (score * 10).coerceAtMost(100))
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

