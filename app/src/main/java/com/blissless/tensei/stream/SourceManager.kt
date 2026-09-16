package com.blissless.tensei.stream

import android.content.Context
import android.util.Log
import com.blissless.tensei.extensions.AnimeExtensionManager
import com.blissless.tensei.extensions.ExtensionDetector
import com.blissless.tensei.extensions.model.AnimeExtension
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Upgraded SourceManager that subscribes to the new
 * `AnimeExtensionManager.installedExtensionsFlow` and maintains a live
 * `sourceId → SourceWithExt` map.
 *
 * REPLACES the old `com/blissless/tensei/stream/SourceManager.kt` that
 * loaded extensions ad-hoc via a synchronous `loadSources()` call.
 *
 * What's new vs the old version:
 *
 *   1. Constructor calls `AnimeExtensionManager.init(context)` to make
 *      sure the singleton is created (it's idempotent).
 *   2. `sources: StateFlow<List<SourceWithExt>>` is now reactive —
 *      whenever the `AnimeExtensionManager` installs/removes/trusts
 *      an extension, this manager's `sources` flow automatically
 *      re-emits with the new list. UIs that `.collectLatest` this
 *      flow will redraw without any manual refresh.
 *   3. The legacy synchronous `loadSources()` suspend function is kept
 *      for backwards compatibility — it delegates to
 *      `AnimeExtensionManager.initAnimeExtensions()` and then syncs
 *      the local state.
 *   4. All the existing methods (`getEpisodes`, `getAnimeDetails`,
 *      `getHosters`, `getVideosFromHoster`, `getVideosDirect`,
 *      `search`) are kept with identical signatures so the rest of
 *      Tensei's code keeps working unchanged.
 */
class SourceManager(private val context: Context) {
    private val detector = ExtensionDetector(context)
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Backed by the new AnimeExtensionManager.installedExtensionsFlow. */
    private val _sources = MutableStateFlow<List<SourceWithExt>>(emptyList())
    val sources: StateFlow<List<SourceWithExt>> = _sources.asStateFlow()

    init {
        // Ensure the singleton is initialised (idempotent).
        AnimeExtensionManager.init(context)
        // Reactively rebuild the local source list whenever the
        // installed-extensions set changes.
        managerScope.launch {
            AnimeExtensionManager.get(context).installedExtensionsFlow
                .collectLatest { installedMap ->
                    val list = installedMap.values.flatMap { ext ->
                        ext.sources.filterIsInstance<AnimeCatalogueSource>().map { src ->
                            SourceWithExt(src, ext)
                        }
                    }
                    _sources.value = list
                    Log.i(TAG, "Rebuilt source list: ${list.size} source(s) from ${installedMap.size} extension(s)")
                }
        }
    }

    /**
     * A loaded source paired with the extension that provides it.
     * Kept as a data class to preserve API compatibility with the
     * old SourceManager.
     */
    data class SourceWithExt(
        val source: AnimeCatalogueSource,
        val extension: AnimeExtension.Installed,
    )

    /** Synchronous accessor for the current source list. */
    fun getSources(): List<SourceWithExt> = _sources.value

    /**
     * Force a re-scan of installed extensions. Mostly a no-op now —
     * the manager auto-refreshes on package install/remove broadcasts.
     * Kept for backwards compatibility with Tensei's existing UI.
     */
    fun reloadSources() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            AnimeExtensionManager.get(context).initAnimeExtensions()
        }
    }

    /**
     * Initial load. Kept for backwards compatibility — the manager is
     * already initialised in the constructor, so this just waits for
     * the first scan to complete.
     */
    suspend fun loadSources() {
        withContext(Dispatchers.IO) {
            // If the manager isn't initialised yet, this will block until
            // the first scan completes (which is fast — PackageManager
            // calls are synchronous).
            val manager = AnimeExtensionManager.get(context)
            if (manager.installedExtensions.isEmpty()) {
                manager.initAnimeExtensions()
            }
        }
    }

    // ─── Source-querying methods (unchanged from the old SourceManager) ────

    suspend fun search(
        query: String,
        sourceFilter: SourceWithExt? = null,
        onProgress: (SourceWithExt, List<SAnime>) -> Unit,
    ) {
        Log.d(TAG, "search() query=\"$query\" sourceFilter=${sourceFilter?.source?.name}")
        withContext(Dispatchers.IO) {
            val targets = if (sourceFilter != null) listOf(sourceFilter) else _sources.value
            for (sw in targets) {
                try {
                    val filters = sw.source.getFilterList()
                    val page = sw.source.getSearchAnime(1, query, filters)
                    if (page.animes.isNotEmpty()) {
                        onProgress(sw, page.animes)
                        page.animes.forEach { anime ->
                            Log.i(TAG, "  -> [${sw.source.name}] ${anime.title} (${anime.url})")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Search failed for ${sw.source.name}", e)
                }
            }
        }
    }

    suspend fun getEpisodes(source: AnimeCatalogueSource, anime: SAnime): List<SEpisode> =
        withContext(Dispatchers.IO) { source.getEpisodeList(anime) }

    suspend fun getAnimeDetails(source: AnimeCatalogueSource, anime: SAnime): SAnime =
        withContext(Dispatchers.IO) { source.getAnimeDetails(anime) }

    suspend fun getHosters(
        source: AnimeCatalogueSource,
        episode: SEpisode,
        anime: SAnime? = null,
    ): List<Hoster>? {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "getHosters: source=${source.name} ep=${episode.url.take(80)}")
            if (anime != null && source is AnimeHttpSource) {
                source.prepareNewEpisode(episode, anime)
            }
            try {
                val hosters = source.getHosterList(episode)
                Log.d(TAG, "  got ${hosters.size} hosters: ${hosters.map { "${it.hosterName}: ${it.hosterUrl.take(80)} (lazy=${it.lazy})" }}")
                hosters
            } catch (e: Throwable) {
                Log.d(TAG, "  getHosterList failed: ${e.message}", e)
                try {
                    val videos = source.getVideoList(episode)
                    Log.d(TAG, "  got ${videos.size} videos from fallback getVideoList")
                    if (videos.isNotEmpty()) {
                        videos.forEach { v ->
                            Log.d(TAG, "    video: \"${v.videoTitle}\" res=${v.resolution}p url=${v.videoUrl.take(100)} headers=${v.headers?.let { h -> (0 until h.size).associate { h.name(it) to h.value(it) } }}")
                        }
                        val derivedHosters = videos.map { video ->
                            Hoster(
                                hosterUrl = video.videoUrl,
                                hosterName = video.videoTitle.take(50),
                                videoList = listOf(video),
                                lazy = false,
                            )
                        }
                        return@withContext derivedHosters.distinctBy { it.hosterName }
                    }
                } catch (e2: Throwable) {
                    Log.d(TAG, "  fallback getVideoList also failed: ${e2.message}")
                }
                null
            }
        }
    }

    suspend fun getVideosFromHoster(source: AnimeCatalogueSource, hoster: Hoster): List<Video> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "getVideosFromHoster: source=${source.name} hoster=${hoster.hosterName} url=${hoster.hosterUrl.take(80)} lazy=${hoster.lazy}")
            val videos = if (hoster.lazy) {
                source.getVideoList(hoster)
            } else {
                hoster.videoList ?: source.getVideoList(hoster)
            }
            Log.d(TAG, "  returned ${videos.size} videos")
            videos.forEach { v ->
                Log.d(TAG, "    video: \"${v.videoTitle}\" res=${v.resolution}p url=${v.videoUrl.take(100)}")
            }
            videos
        }
    }

    suspend fun getVideosDirect(
        source: AnimeCatalogueSource,
        episode: SEpisode,
        anime: SAnime? = null,
    ): List<Video> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "getVideosDirect: source=${source.name} ep=${episode.url.take(80)}")
            if (anime != null && source is AnimeHttpSource) {
                source.prepareNewEpisode(episode, anime)
            }
            try {
                val videos = source.getVideoList(episode)
                Log.d(TAG, "  got ${videos.size} videos")
                videos.forEach { v ->
                    Log.d(TAG, "    video: \"${v.videoTitle}\" res=${v.resolution}p url=${v.videoUrl.take(100)} headers=${v.headers?.let { h -> (0 until h.size).associate { h.name(it) to h.value(it) } }}")
                }
                videos
            } catch (e: Throwable) {
                Log.d(TAG, "  getVideoList failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Look up a source by id. Returns null if no installed extension
     * provides it. Useful for the player: given an anime's `sourceId`,
     * resolve to the live `AnimeCatalogueSource` to call
     * `getVideoList(hoster)` etc.
     */
    fun getSource(sourceId: Long): AnimeCatalogueSource? {
        return _sources.value.find { it.source.id == sourceId }?.source
            ?: AnimeExtensionManager.get(context).getSource(sourceId) as? AnimeCatalogueSource
    }

    /**
     * Look up the extension that owns the given source id.
     */
    fun getExtensionForSource(sourceId: Long): AnimeExtension.Installed? {
        return _sources.value.find { it.source.id == sourceId }?.extension
            ?: AnimeExtensionManager.get(context).installedExtensions.find { ext ->
                ext.sources.any { it.id == sourceId }
            }
    }

    companion object {
        private const val TAG = "SourceManager"
    }
}
