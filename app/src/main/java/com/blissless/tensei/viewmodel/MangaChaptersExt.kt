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

// Chapter loading for manga: extension chapter lists and images, built-in
// MangaDex fallback, external (Mihon-style) sources, prefetching and
// cancellation.



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
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor == null) {
                android.util.Log.e("MangaDebug", "RESULT: cursor is NULL â€” content provider not found at authority='$authority'")
                android.util.Log.e("MangaDebug", "Check manifest: android:authorities should be '$authority'")
                return@withContext null
            }
            cursor.use { c ->
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
                val json = JSONObject(jsonData)
                if (json.has("error")) {
                    android.util.Log.e("MangaDebug", "Extension error: ${json.getString("error")}")
                    return@withContext null
                }
                val totalChapters = json.optInt("totalChapters", 0)
                val chaptersArr = json.optJSONArray("chapters")
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
                if (chapters.isNotEmpty()) {
                }
                Pair(chapters, totalChapters)
            }
        } catch (e: Exception) {
            android.util.Log.e("MangaDebug", "EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}


private fun MainViewModel.fetchExtensionChapterImages(mangaTitle: String, chapterParam: String, authority: String): List<String>? {
    return try {
        val uri = Uri.parse("content://$authority/scrape")
            .buildUpon()
            .appendQueryParameter("manga", mangaTitle)
            .appendQueryParameter("anime", mangaTitle)
            .appendQueryParameter("chapter", chapterParam)
            .build()
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
            images
        }
    } catch (e: Exception) {
        android.util.Log.w("MangaExt", "fetchExtensionChapterImages failed: ${e.message}", e)
        null
    }
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
fun MainViewModel.loadMangaChapters(mangaId: Int, title: String) {
    mangaChaptersJob?.cancel()
    // A fresh load clears any previous cancellation so the loading state shows again.
    _mangaChaptersLoadCancelled.value = false
    mangaChaptersJob = viewModelScope.launch {
        loadMangaChaptersInternal(mangaId, title)
    }
}


private suspend fun MainViewModel.loadMangaChaptersInternal(mangaId: Int, title: String) {
    if (userPreferences.mangaSourceMethod.value == "external") {
        loadMangaChaptersFromExternal(mangaId, title)
        return
    }
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
    var chapters = emptyList<MangaChapter>()

    // Resolve the title â€” prefer English, then Romaji, then the passed-in title
    val resolvedTitle = detail?.titleEnglish?.takeIf { it.isNotBlank() }
        ?: detail?.title?.takeIf { it.isNotBlank() }
        ?: title

    // --- 1. Fetch MangaDex aggregate for count metadata (NOT for the chapter list) ---
    var mdLatestChapter: Int? = null
    var mdVolumeCount: Int? = null
    val mangaDexId = mangaDexManager?.findMangaByAniListId(resolvedTitle, mangaId)
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

    var extChapters: List<ExtensionChapter>? = null
    var extTotalChapters = 0
    var matchedTitle: String? = null
    for (t in titlesToTry) {
        val extResult = fetchExtensionChapterList(t)
        val resultChapters = extResult?.first
        val resultTotal = extResult?.second ?: 0
        // Keep the highest totalChapters seen across attempts â€” a later failed/null result
        // must not wipe the count reported by an earlier successful query.
        if (resultTotal > extTotalChapters) extTotalChapters = resultTotal
        if (resultChapters != null && resultChapters.isNotEmpty()) {
            extChapters = resultChapters
            matchedTitle = t
            break
        }
    }

    if (extChapters != null && extChapters.isNotEmpty()) {
        _mangaExtensionTitle.value = matchedTitle
        // Build ChapterInfo list with oni's URL scheme: anilist_${mediaId}_ch_${number}
        //
        // Filter + sort fix (Blue Lock ch 367 → "chapter2" bug):
        // Extensions often return special/extras/omake chapters mixed in with the main
        // chapter list. These specials typically have titles like "Volume 2, Chapter 2",
        // "Side Story", "Bonus", "Extra", "Prologue", "Oneshot" — and their `number`
        // field may be 0, a fraction (367.5), a duplicate of a main chapter, or plain
        // text the toFloatOrNull() parses to null. Sorted by extension `index`, these
        // specials ended up AFTER the last main chapter (e.g. ch 367), so the reader's
        // "Next Chapter" button at the bottom would pick the special's title, and
        // extractChapterNum() would fall back to extracting ANY number from the title
        // (often "2" from "Volume 2, Chapter 2") → "chapter2" shown as the next chapter
        // even though 367 was the actual last main chapter.
        //
        // The fix has two parts:
        //   1. Filter out chapters whose title matches obvious special/extras markers
        //      (case-insensitive). These are NOT main chapters and shouldn't be in the
        //      reader's sequential navigation.
        //   2. Sort the remaining chapters by their parsed chapter NUMBER (with the
        //      extension's `index` as a stable tiebreaker) instead of by `index` alone.
        //      This way ch 367 (the highest-numbered main chapter) lands at the end of
        //      the list → isLastChapter == true → the Next Chapter button doesn't show.
        val specialMarkers = listOf(
            "volume", "special", "side story", "bonus", "extra", "omake",
            "prologue", "epilogue", "oneshot", "one-shot", "doujinshi",
            "extra chapter", "bonus chapter", "side chapter"
        )
        val filteredExtChapters = extChapters.filter { ch ->
            val combined = "${ch.number} ${ch.title}".lowercase()
            specialMarkers.none { combined.contains(it) }
        }.ifEmpty { extChapters } // never return an empty list — fall back to original

        chapters = filteredExtChapters
            .sortedWith(compareBy({ it.number.toFloatOrNull() ?: Float.MAX_VALUE }, { it.index }))
            .mapIndexed { idx, ch ->
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

    _mangaChapters.value = chapters

    // Count chapters per base number for the display total (e.g. 149/354): a subchapter that
    // opens a new number (314.1) is a chapter of its own, one that only continues an existing
    // number (314.2, or 313.2 after 313) is not, so 313/314.1/314.2/315 is three chapters.
    // The old integer-only count dropped every subchapter and undercounted series numbered
    // 314.1 instead of 314.
    val countedChapters = countDistinctChapters(chapters.map { it.chapterNumber })
    chapters.filter { ch ->
        ch.chapterNumber > 0f && (ch.chapterNumber - ch.chapterNumber.toInt()) >= 0.001f
    }.forEach { ch ->
    }

    // Display denominator for progress (e.g. 149/354). Prefer the extension's CURRENT release
    // count: AniList's chapters field is stale for releasing manga (e.g. Blue Lock stuck at 352
    // while the extension already has 354), so it must not override the extension total.
    // extTotalChapters covers the case where the extension returns only a partial list (e.g.
    // just the latest few chapters), which would otherwise make progress look like 149/3.
    // When the extension provides only a total (no chapter entries), that total may cover
    // subchapters too, so fall back to the base-number count from the actual chapter list.
    val displayTotalChapters = when {
        extTotalChapters > 0 && extChapters != null && extChapters.isNotEmpty() -> countedChapters
        extChapters != null && extChapters.isNotEmpty() -> countedChapters
        detail?.chapters != null && detail.chapters > 0 -> detail.chapters
        else -> countedChapters
    }
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

// ─── External (Mihon) manga extension chapter loading ────────────────────────────────────

/** Resolve the source to use for a package, honoring the per-extension language preference. */
private fun MainViewModel.resolveExternalSource(pkgName: String): Source? {
    val manager = MangaExtensionManager.getOrNull() ?: return null
    val ext = manager.installed(pkgName) ?: return null
    val sources = ext.sources
    if (sources.isEmpty()) return null
    val preferred = userPreferences.getMangaExtensionLanguage(pkgName)
    if (!preferred.isNullOrBlank()) {
        sources.firstOrNull { it.lang.equals(preferred, ignoreCase = true) }?.let { return it }
    }
    // Default to English when available (requirement), else the first source.
    sources.firstOrNull { it.lang.equals("en", ignoreCase = true) }?.let { return it }
    return sources.first()
}


/** Languages an installed external extension can serve (for the settings picker). */
fun MainViewModel.externalExtensionLanguages(pkgName: String): List<String> {
    val manager = MangaExtensionManager.getOrNull() ?: return emptyList()
    val ext = manager.installed(pkgName) ?: return emptyList()
    val langs = ext.sources.map { it.lang }.filter { it.isNotBlank() }.distinct()
    return if (langs.isEmpty()) listOf(ext.lang) else langs.sorted()
}


private fun normalizeMangaTitle(title: String): String =
    title.lowercase().filter { it.isLetterOrDigit() }


/** Pick the search result whose title is the closest match to [title]. */
private fun pickBestMangaMatch(title: String, results: List<SManga>): SManga? {
    if (results.isEmpty()) return null
    val target = normalizeMangaTitle(title)
    return results.maxByOrNull { candidate ->
        val name = normalizeMangaTitle(candidate.title)
        when {
            name == target -> 1000
            name.startsWith(target) || target.startsWith(name) -> 500 - kotlin.math.abs(name.length - target.length)
            name.contains(target) || target.contains(name) -> 250
            else -> 0
        }
    }
}


private suspend fun MainViewModel.loadMangaChaptersFromExternal(mangaId: Int, title: String) {
    _isLoadingMangaChapters.value = true
    _hasLoadedMangaChapters.value = false
    _mangaTotalChapters.value = 0
    _mangaExtensionTitle.value = null
    mangaTrackManager?.resetTotalChapters(mangaId)

    val detail = _mangaDetail.value
    val resolvedTitle = detail?.titleEnglish?.takeIf { it.isNotBlank() }
        ?: detail?.title?.takeIf { it.isNotBlank() }
        ?: title

    val pkgName = userPreferences.defaultExternalMangaExtension.value
    if (pkgName.isNullOrBlank()) {
        android.util.Log.w("MangaChapters", "external: no default manga extension selected")
        _mangaChapters.value = emptyList()
        _hasLoadedMangaChapters.value = true
        _isLoadingMangaChapters.value = false
        return
    }

    val chapters = withContext(Dispatchers.IO) {
        runCatching { fetchExternalMangaChapters(mangaId, pkgName, resolvedTitle) }
            .onFailure { android.util.Log.e("MangaChapters", "external chapter load failed", it) }
            .getOrDefault(emptyList())
    }

    _mangaChapters.value = chapters
    val counted = countDistinctChapters(chapters.map { it.chapterNumber })
    _mangaTotalChapters.value = counted
    mangaTrackManager?.updateTotalChapters(mangaId, counted, detail?.volumes)
    loadLocalMangaTracking()
    _hasLoadedMangaChapters.value = true
    _isLoadingMangaChapters.value = false
}


private suspend fun MainViewModel.fetchExternalMangaChapters(
    mangaId: Int,
    pkgName: String,
    title: String,
): List<MangaChapter> {
    val source = resolveExternalSource(pkgName) ?: return emptyList()

    var sManga: SManga? = null
    var sChapters: List<SChapter> = emptyList()

    // Prefer a search hit (gives the real source URL), then load details + chapters.
    val searchResult = runCatching { MangaSourceApi.search(source, 1, title) }.getOrNull()
    val match = searchResult?.let { pickBestMangaMatch(title, it.mangas) }
    if (match != null) {
        runCatching { MangaSourceApi.details(source, match) }.getOrNull()?.let { update ->
            sManga = update.manga
            sChapters = update.chapters
        }
    }

    // Fall back to building an SManga from the title alone (some sources search poorly).
    if (sChapters.isEmpty()) {
        val fallback = SManga.create().apply {
            this.title = title
            url = "/"
        }
        runCatching { MangaSourceApi.details(source, fallback) }.getOrNull()?.let { update ->
            sManga = update.manga
            sChapters = update.chapters
        }
    }

    val manga = sManga ?: return emptyList()
    _externalMangaCache[mangaId] = source to manga

    return normalizeExternalChapters(sChapters)
        .map { norm ->
            val number = norm.number
            val chapterId = "ext_${mangaId}_${norm.sourceIndex}_${norm.chapter.url.hashCode()}"
            _externalChapterCache[chapterId] = source to norm.chapter
            MangaChapter(
                url = chapterId,
                title = norm.chapter.name.takeIf { it.isNotBlank() } ?: "Chapter $number",
                chapterId = chapterId,
                chapterNumber = number,
            )
        }
}

// ─── External chapter-list normalization ─────────────────────────────────────────────

/** An [SChapter] paired with its resolved number and position in the source list. */
private data class NormalizedExternalChapter(
    val chapter: SChapter,
    val number: Float,
    val sourceIndex: Int,
)

/** Chapter number parsed from a chapter NAME: "Ch. 300", "Chapter 12.5", "12.5 …". */
private val EXTERNAL_CHAPTER_NUMBER_REGEX = Regex("""(?i)(?:chapter|ch\.?)\s*(\d+(?:\.\d+)?)""")

private fun chapterNumberFromName(name: String): Float? =
    EXTERNAL_CHAPTER_NUMBER_REGEX.find(name)?.groupValues?.get(1)?.toFloatOrNull()

/** True when [candidate] should win the slot over [current] when merging same-numbered chapters. */
private fun isMorePreferredChapter(candidate: SChapter, current: SChapter): Boolean {
    // Official scanlations beat fan groups (mirrors the MangaFire store's own preference).
    val candidateOfficial = candidate.scanlator?.contains("official", ignoreCase = true) == true
    val currentOfficial = current.scanlator?.contains("official", ignoreCase = true) == true
    return candidateOfficial && !currentOfficial
}

/**
 * Normalizes a raw Mihon-source chapter list into reading order with usable chapter numbers.
 *
 * Sources return chapters in their own order — usually newest-first (Mihon convention) — and
 * many leave `chapter_number` unset (-1f):
 *
 *  - WeebCentral lists chapters DESCENDING and only sets `chapter_number` for season-indexed
 *    comics. With every number at -1f, the old `sortedBy { chapter_number }` kept the
 *    descending order and the `idx + 1` fallback numbered the NEWEST chapter "1": the list
 *    read bottom-up and the next-chapter logic selected the wrong row.
 *  - MangaFire numbers every chapter but lists one entry per scanlator group, so each chapter
 *    appears twice (different title translations).
 *
 * Normalization therefore:
 *  1. resolves a number per chapter — the source-declared number wins, otherwise one is
 *     parsed from the chapter name;
 *  2. when numbers are usable (majority of the list), keeps ONE chapter per number —
 *     preferring official scanlations, then source order — and sorts ascending so the list
 *     reads top-down and numbers match the real chapter progression;
 *  3. when no numbers are usable at all, orders by upload-date trend (falling back to the
 *     newest-first Mihon convention) and numbers the list sequentially in that order.
 */
private fun normalizeExternalChapters(chapters: List<SChapter>): List<NormalizedExternalChapter> {
    if (chapters.isEmpty()) return emptyList()

    // 1. Best number per chapter: declared number, then name-parsed number, else unusable.
    val resolved = chapters.mapIndexed { idx, ch ->
        val declared = ch.chapter_number.takeIf { it > 0f }
        NormalizedExternalChapter(
            chapter = ch,
            number = declared ?: chapterNumberFromName(ch.name) ?: Float.NaN,
            sourceIndex = idx,
        )
    }

    val numbered = resolved.filter { !it.number.isNaN() }
    val numbersUsable = numbered.size * 2 >= resolved.size

    if (numbersUsable) {
        // 2. One entry per chapter number (dedupe across scanlator groups), then ascending
        //    reading order. Duplicates that never carried a number keep their relative slot
        //    after the highest resolved number so nothing is silently dropped.
        val byNumber = LinkedHashMap<Float, NormalizedExternalChapter>()
        for (entry in numbered) {
            val existing = byNumber[entry.number]
            if (existing == null || isMorePreferredChapter(entry.chapter, existing.chapter)) {
                byNumber[entry.number] = entry
            }
        }
        val maxNumber = byNumber.keys.maxOrNull() ?: 0f
        val unnumbered = resolved.filter { it.number.isNaN() }
            .mapIndexed { i, entry -> entry.copy(number = maxNumber + i + 1f) }
        return (byNumber.values + unnumbered).sortedBy { it.number }
    }

    // 3. No usable numbers: order by upload-date trend; Mihon sources default to newest-first,
    //    so a flat/absent date trend is treated as descending and reversed into reading order.
    val dated = resolved.filter { it.chapter.date_upload > 0L }
    val descendingPairs = dated.zipWithNext().count { (a, b) -> a.chapter.date_upload > b.chapter.date_upload }
    val ascendingPairs = dated.zipWithNext().count { (a, b) -> a.chapter.date_upload < b.chapter.date_upload }
    val isDescending = descendingPairs >= ascendingPairs
    val ordered = if (isDescending) resolved.reversed() else resolved
    return ordered.mapIndexed { pos, entry -> entry.copy(number = (pos + 1).toFloat()) }
}


/** Load page URLs for an external chapter from its cached SChapter handle. */
private suspend fun MainViewModel.loadExternalChapterImages(chapterId: String) {
    val pair = _externalChapterCache[chapterId]
    if (pair == null) {
        _mangaChapterImages.value = emptyList()
        _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_chapter_data_unavailable)
        return
    }
    val (source, sChapter) = pair
    val pages = runCatching { MangaSourceApi.pages(source, sChapter) }.getOrNull()
    if (pages == null) {
        _mangaChapterImages.value = emptyList()
        _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_load_failed)
        return
    }
    val urls = MangaSourceApi.resolveImages(source, pages).filterNotNull()
    if (urls.isEmpty()) {
        _mangaChapterImages.value = emptyList()
        _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_no_pages)
    } else {
        _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (chapterId to urls)
        _mangaChapterImages.value = urls
    }
}


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
        // Attach the active extension source's headers (Referer/User-Agent) to the reader's
        // image requests; null for the built-in scrape path. Looked up before the cache
        // serve so cached chapter transitions keep the right headers.
        _mangaImageHeaders.value = if (userPreferences.mangaSourceMethod.value == "external") {
            _externalChapterCache[chapterId]?.let { (source, _) -> MangaSourceApi.headers(source) }
        } else null

        // Serve from the prefetch cache first so chapter transitions (incl. auto-advance)
        // load instantly instead of waiting on a scrape round-trip.
        _mangaChapterImagesCache.value[chapterId]?.let { cached ->
            _mangaChapterImages.value = cached
            _mangaChapterImagesError.value = null
            return@launch
        }

        _mangaChapterImages.value = null
        _mangaChapterImagesError.value = null

        if (userPreferences.mangaSourceMethod.value == "external") {
            loadExternalChapterImages(chapterId)
            return@launch
        }

        // Extract the chapter number from the chapter TITLE (matching oni's contract)
        // Title format: "Chapter 346" or "Chapter 346.2: Some Title" â†’ "346" or "346.2"
        val chapterParam = chapterTitle?.let { title ->
            title.removePrefix("Chapter ").substringBefore(":").trim()
        } ?: chapterId.substringAfterLast("_ch_").trim()

        // Resolve the manga title for the extension
        val extTitle = _mangaExtensionTitle.value
            ?: _mangaDetail.value?.titleEnglish?.takeIf { it.isNotBlank() }
            ?: _mangaDetail.value?.title?.takeIf { it.isNotBlank() }
            ?: mangaTitle
            ?: ""

        if (extTitle.isBlank()) {
            android.util.Log.w("MangaReader", "loadChapterImages: no manga title available")
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_no_title)
            return@launch
        }

        val authority = _selectedExtensionAuthority.value
        if (authority == null) {
            android.util.Log.w("MangaReader", "loadChapterImages: no extension selected")
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_no_extension)
            return@launch
        }

        val images = withContext(Dispatchers.IO) {
            fetchExtensionChapterImages(extTitle, chapterParam, authority)
        }

        if (images == null) {
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_load_failed)
        } else if (images.isEmpty()) {
            _mangaChapterImages.value = emptyList()
            _mangaChapterImagesError.value = context.getString(R.string.runtime_manga_error_no_pages)
        } else {
            _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (chapterId to images)
            _mangaChapterImages.value = images
        }
    }
}


/**
 * Cancel an in-flight chapter image load (the reader's "Loading chapter…" screen).
 *
 * Stops the request so the pending result is never written; the reader keeps whatever is
 * already on screen and decides what to show next (its error/retry screen when nothing is
 * loaded yet).
 */
fun MainViewModel.cancelChapterImageLoad() {
    chapterImagesJob?.cancel()
    chapterImagesJob = null
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
            if (userPreferences.mangaSourceMethod.value == "external") {
                val pair = _externalChapterCache[next.chapterId] ?: return@launch
                val (source, sChapter) = pair
                val pages = runCatching { MangaSourceApi.pages(source, sChapter) }.getOrNull() ?: return@launch
                val urls = MangaSourceApi.resolveImages(source, pages).filterNotNull()
                if (urls.isNotEmpty()) {
                    _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (next.chapterId to urls)
                }
                return@launch
            }
            val authority = _selectedExtensionAuthority.value
            if (authority == null) return@launch
            val extTitle = _mangaExtensionTitle.value
                ?: _mangaDetail.value?.titleEnglish?.takeIf { it.isNotBlank() }
                ?: _mangaDetail.value?.title?.takeIf { it.isNotBlank() }
                ?: mangaTitle
                ?: return@launch
            if (extTitle.isBlank()) return@launch

            val chapterParam = next.title.removePrefix("Chapter ").substringBefore(":").trim()

            val images = withContext(Dispatchers.IO) {
                fetchExtensionChapterImages(extTitle, chapterParam, authority)
            }
            if (images != null && images.isNotEmpty()) {
                _mangaChapterImagesCache.value = _mangaChapterImagesCache.value + (next.chapterId to images)
            }
        } finally {
            _prefetchingChapterIds.remove(next.chapterId)
        }
    }
}


fun MainViewModel.clearChapterImages() {
    _mangaChapterImages.value = null
    _mangaChapterImagesError.value = null
    _mangaImageHeaders.value = null
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
