package com.blissless.tensei.extensions.manga

import android.util.Log
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers

/**
 * Wraps the extension API so every call runs off the main thread.
 */
object MangaSourceApi {

    private const val TAG = "MangaExtSrc"

    suspend fun search(source: Source, page: Int, query: String): MangasPage =
        withContext(Dispatchers.IO) {
            call("search") { source.getSearchManga(page, query, FilterList()) }
        }

    suspend fun popular(source: Source, page: Int): MangasPage =
        withContext(Dispatchers.IO) {
            call("popular") { source.getPopularManga(page) }
        }

    suspend fun details(source: Source, manga: SManga): SMangaUpdate =
        withContext(Dispatchers.IO) {
            call("details") {
                source.getMangaUpdate(manga, emptyList(), fetchDetails = true, fetchChapters = true)
            }
        }

    suspend fun pages(source: Source, chapter: SChapter): List<Page> =
        withContext(Dispatchers.IO) {
            call("pages") { source.getPageList(chapter) }
        }

    suspend fun imageUrl(source: Source, page: Page): String? {
        page.imageUrl?.let { return it }
        if (source !is HttpSource) return null
        return withContext(Dispatchers.IO) {
            runCatching { source.getImageUrl(page) }.getOrNull()
        }
    }

    suspend fun resolveImages(source: Source, pages: List<Page>): List<String?> =
        withContext(Dispatchers.IO) {
            pages.map { page ->
                page.imageUrl ?: runCatching {
                    (source as? HttpSource)?.getImageUrl(page)
                }.getOrNull()
            }
        }

    fun headers(source: Source): Headers? = (source as? HttpSource)?.headers

    private suspend fun <T> call(name: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: Throwable) {
            Log.e(TAG, "call '$name' failed (${e.javaClass.simpleName}: ${e.message})", e)
            throw e
        }
}
