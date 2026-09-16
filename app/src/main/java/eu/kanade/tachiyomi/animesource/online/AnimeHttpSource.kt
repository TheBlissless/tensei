package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.HttpServer
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.ThumbnailInfo
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.Locale

/**
 * Abstract OkHttp-backed `AnimeSource`. Extensions subclass this to
 * implement a typical "fetch HTML from a server, parse with Jsoup"
 * source.
 *
 * Ported from Aniyomi's `AnimeHttpSource` at
 * `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/online/AnimeHttpSource.kt`.
 *
 * NEW in this port vs the previous Tensei version:
 *
 *   - `getAnimeEpisodeUpdate(...)` — the combined API: fetches anime
 *     details + episode list in PARALLEL via `coroutineScope { async {…} }`
 *     when both flags are true. Older sources that override only
 *     `getAnimeDetails` / `getEpisodeList` get the parallel speedup
 *     for free without source changes.
 *
 *   - `getAnimeSeasonUpdate(...)` — same idea for seasons.
 *
 *   - `createHttpServer(): HttpServer?` — sources that need a local
 *     NanoHTTPD proxy (e.g., to inject subtitles into HLS streams,
 *     bypass CORS, or proxy range requests) override this and return
 *     a configured HttpServer. The player activity is responsible for
 *     calling `start()` on it and rewriting the video URL via
 *     `Video.copyHttpServer(port)` before playback.
 *
 *   - `getVideoThumbnails(video: Video): ThumbnailInfo?` — sources
 *     can return seekbar thumbnail metadata for the player to render
 *     preview thumbnails on the seekbar. Default returns null.
 *
 * All the existing hooks (`hosterListRequest`, `hosterListParse`,
 * `videoListRequest(hoster)`, `videoListParse(response, hoster)`,
 * `videoListRequest(episode)`, `videoListParse(response)`,
 * `videoUrlRequest`, `videoUrlParse`, `resolveVideo`,
 * `prepareNewEpisode`, `sortHosters`, `sortVideos`,
 * `headersBuilder`, `client`, `headers`, `baseUrl`, `id`,
 * `versionId`, `generateId`, `resolveUrl`, `getAnimeUrl`,
 * `getEpisodeUrl`, `setUrlWithoutDomain`) are unchanged so existing
 * extension code keeps working.
 */
abstract class AnimeHttpSource : AnimeCatalogueSource {

    protected val network: NetworkHelper by lazy { NetworkHelper.getInstance() }

    abstract val baseUrl: String

    open val versionId = 1

    override val id by lazy { generateId(name, lang, versionId) }

    val headers: Headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient
        get() = network.client

    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase(Locale.ROOT)}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
        add("Referer", "${baseUrl.trimEnd('/')}/")
    }

    override fun toString() = "$name (${lang.uppercase()})"

    // ─── Browse API ──────────────────────────────────────────────────────────

    override suspend fun getPopularAnime(page: Int): AnimesPage {
        return client.newCall(popularAnimeRequest(page))
            .awaitSuccess()
            .let { response -> popularAnimeParse(response) }
    }

    protected open fun popularAnimeRequest(page: Int): Request {
        throw UnsupportedOperationException("popularAnimeRequest not implemented")
    }

    protected open fun popularAnimeParse(response: Response): AnimesPage {
        throw UnsupportedOperationException("popularAnimeParse not implemented")
    }

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        return client.newCall(searchAnimeRequest(page, query, filters))
            .awaitSuccess()
            .let { response -> searchAnimeParse(response) }
    }

    protected open fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        throw UnsupportedOperationException("searchAnimeRequest not implemented")
    }

    protected open fun searchAnimeParse(response: Response): AnimesPage {
        throw UnsupportedOperationException("searchAnimeParse not implemented")
    }

    override suspend fun getLatestUpdates(page: Int): AnimesPage {
        return client.newCall(latestUpdatesRequest(page))
            .awaitSuccess()
            .let { response -> latestUpdatesParse(response) }
    }

    protected open fun latestUpdatesRequest(page: Int): Request {
        throw UnsupportedOperationException("latestUpdatesRequest not implemented")
    }

    protected open fun latestUpdatesParse(response: Response): AnimesPage {
        throw UnsupportedOperationException("latestUpdatesParse not implemented")
    }

    // ─── Anime details ───────────────────────────────────────────────────────

    override suspend fun getAnimeDetails(anime: SAnime): SAnime {
        return client.newCall(animeDetailsRequest(anime))
            .awaitSuccess()
            .let { response ->
                animeDetailsParse(response).apply { initialized = true }
            }
    }

    open fun animeDetailsRequest(anime: SAnime): Request {
        return GET(resolveUrl(anime.url), headers)
    }

    protected open fun animeDetailsParse(response: Response): SAnime {
        throw UnsupportedOperationException("animeDetailsParse not implemented")
    }

    // ─── Episode list ───────────────────────────────────────────────────────

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
        return client.newCall(episodeListRequest(anime))
            .awaitSuccess()
            .let { response -> episodeListParse(response) }
    }

    protected open fun episodeListRequest(anime: SAnime): Request {
        return GET(resolveUrl(anime.url), headers)
    }

    protected open fun episodeListParse(response: Response): List<SEpisode> {
        return emptyList()
    }

    protected open fun episodeVideoParse(response: Response): SEpisode {
        throw UnsupportedOperationException("episodeVideoParse not implemented")
    }

    // ─── Season list ─────────────────────────────────────────────────────────

    override suspend fun getSeasonList(anime: SAnime): List<SAnime> {
        return client.newCall(seasonListRequest(anime))
            .awaitSuccess()
            .let { response -> seasonListParse(response) }
    }

    protected open fun seasonListRequest(anime: SAnime): Request {
        return GET(resolveUrl(anime.url), headers)
    }

    protected open fun seasonListParse(response: Response): List<SAnime> {
        throw UnsupportedOperationException("seasonListParse not implemented")
    }

    // ─── Hoster list (ext-lib 16+) ──────────────────────────────────────────

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> {
        return client.newCall(hosterListRequest(episode))
            .awaitSuccess()
            .let { response -> hosterListParse(response) }
    }

    protected open fun hosterListRequest(episode: SEpisode): Request {
        return GET(resolveUrl(episode.url), headers)
    }

    protected open fun hosterListParse(response: Response): List<Hoster> {
        return emptyList()
    }

    // ─── Video list per hoster (ext-lib 16+) ─────────────────────────────────

    override suspend fun getVideoList(hoster: Hoster): List<Video> {
        return client.newCall(videoListRequest(hoster))
            .awaitSuccess()
            .let { response -> videoListParse(response, hoster) }
    }

    protected open fun videoListRequest(hoster: Hoster): Request {
        return GET(hoster.hosterUrl, headers)
    }

    protected open fun videoListParse(response: Response, hoster: Hoster): List<Video> {
        return emptyList()
    }

    /**
     * Resolve a (possibly lazy) Video's final `videoUrl`. Sources
     * that return `Video(videoUrl = "")` from `videoListParse` and
     * want the player to call back for the real URL should override
     * this. Default returns the video unchanged.
     *
     * Called by the player's `HosterLoader.getResolvedVideo(...)`
     * before invoking `activity.setVideo(video)`.
     */
    open suspend fun resolveVideo(video: Video): Video? {
        return video
    }

    // ─── Legacy video list per episode (ext-lib 14) ─────────────────────────

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        return client.newCall(videoListRequest(episode))
            .awaitSuccess()
            .let { response -> videoListParse(response) }
    }

    protected open fun videoListRequest(episode: SEpisode): Request {
        return GET(resolveUrl(episode.url), headers)
    }

    protected open fun videoListParse(response: Response): List<Video> {
        return emptyList()
    }

    open fun List<Hoster>.sortHosters(): List<Hoster> {
        return this
    }

    open fun List<Video>.sortVideos(): List<Video> {
        return this
    }

    open suspend fun getVideoUrl(video: Video): String {
        return client.newCall(videoUrlRequest(video))
            .awaitSuccess()
            .let { response -> videoUrlParse(response) }
    }

    protected open fun videoUrlRequest(video: Video): Request {
        return GET(video.videoUrl, headers)
    }

    protected open fun videoUrlParse(response: Response): String {
        throw UnsupportedOperationException("videoUrlParse not implemented")
    }

    suspend fun getVideo(
        request: Request,
        listener: ProgressListener,
    ): Response {
        return client.newCachelessCallWithProgress(request, listener)
    }

    fun SEpisode.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    fun SAnime.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    protected fun resolveUrl(path: String): String {
        return when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            else -> {
                val base = baseUrl.trimEnd('/')
                val p = path.trimStart('/')
                "$base/$p"
            }
        }
    }

    open fun getAnimeUrl(anime: SAnime): String {
        return animeDetailsRequest(anime).url.toString()
    }

    open fun getEpisodeUrl(episode: SEpisode): String {
        return episode.url
    }

    open fun prepareNewEpisode(episode: SEpisode, anime: SAnime) {}

    override fun getFilterList() = AnimeFilterList()

    // ─── NEW: Combined suspend API (ext-lib 17+) ─────────────────────────────

    /**
     * Combined fetch of anime details + episode list, run in parallel.
     *
     * Default implementation runs `getAnimeDetails` and
     * `getEpisodeList` as two concurrent `async` coroutines inside
     * `coroutineScope { ... }`, then packages the results into a
     * `SAnimeEpisodeUpdate`. This is the Aniyomi default behaviour.
     *
     * Sources compiled against ext-lib 17 may override this to do
     * something smarter (single HTTP round-trip that returns both
     * pieces of data, etc.).
     */
    override suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate = coroutineScope {
        val detailsDeferred = async {
            if (fetchDetails) getAnimeDetails(anime) else anime
        }
        val episodesDeferred = async {
            if (fetchEpisodes) getEpisodeList(anime) else episodes
        }
        val refreshedAnime = detailsDeferred.await()
        val refreshedEpisodes = episodesDeferred.await()
        SAnimeEpisodeUpdate(refreshedAnime, refreshedEpisodes)
    }

    /**
     * Combined fetch of anime details + season list, run in parallel.
     */
    override suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate = coroutineScope {
        val detailsDeferred = async {
            if (fetchDetails) getAnimeDetails(anime) else anime
        }
        val seasonsDeferred = async {
            if (fetchSeasons) getSeasonList(anime) else seasons
        }
        val refreshedAnime = detailsDeferred.await()
        val refreshedSeasons = seasonsDeferred.await()
        SAnimeSeasonUpdate(refreshedAnime, refreshedSeasons)
    }

    // ─── NEW: Per-source local HTTP server (NanoHTTPD) ─────────────────────

    /**
     * Create and return a per-source local HTTP server (NanoHTTPD
     * subclass). The player activity is responsible for:
     *
     *   1. Calling `server.start()` before playback if `Video.usesHttpServer()`
     *      returns true for the selected video.
     *   2. Reading `server.listeningPort`.
     *   3. Calling `Video.copyHttpServer(port)` to rewrite the video
     *      URL's `localhost:1` placeholder with the actual port.
     *   4. Calling `server.stop()` after playback ends.
     *
     * Default returns `null` (no HTTP server). Sources that need a
     * proxy (some aniwave/gogoanime variants) override this.
     *
     * NOTE: requires the `org.nanohttpd:nanohttpd` dependency in
     * `app/build.gradle.kts`. See `patches/app.build.gradle.kts.patch`.
     */
    open fun createHttpServer(): HttpServer? = null

    // ─── NEW: Seekbar thumbnails ────────────────────────────────────────────

    /**
     * Fetch seekbar-preview thumbnail metadata for the given video.
     * Default returns null (no previews). Sources that support preview
     * thumbnails (e.g., via a storyboards endpoint) should override
     * this and return a `ThumbnailInfo` with the thumbnail sprite URL
     * and per-frame tile info.
     *
     * Called by the player's `loadThumbnails(video, source)` hook.
     */
    open suspend fun getVideoThumbnails(video: Video): ThumbnailInfo? = null
}
