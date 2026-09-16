package eu.kanade.tachiyomi.animesource.model

import fi.iki.elonen.NanoHTTPD

/**
 * Per-source local HTTP server, used as a proxy between the player
 * and an extension's video URL.
 *
 * Ported from Aniyomi's `HttpServer` at
 * `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/HttpServer.kt`.
 *
 * WHY IT EXISTS
 *   Some anime sources need a local proxy server to:
 *     - Inject authentication headers (Referer, User-Agent, cookies)
 *       into HLS segment requests (because ExoPlayer's OkHttpDataSource
 *       can't dynamically swap headers per segment inside a .m3u8).
 *     - Bypass CORS restrictions for video files served with restrictive
 *       Access-Control-Allow-Origin headers.
 *     - Stitch subtitle tracks into HLS playlists that don't natively
 *       include them.
 *     - Proxy range requests for seek operations on stubborn servers.
 *
 *   Sources that need any of this override
 *   `AnimeHttpSource.createHttpServer(): HttpServer?` and return a
 *   configured subclass. The player activity is then responsible for
 *   starting it before playback and stopping it after.
 *
 * HOW IT'S USED
 *   1. Source's `videoListParse()` returns `Video(videoUrl = "http://localhost:1/stream/$id")`.
 *      The `1` in the port position is the sentinel value matching
 *      `Video.LOCAL_URL_REGEX`. (`Video.usesHttpServer()` returns true.)
 *   2. Player sees `video.usesHttpServer() == true`, calls
 *      `(source as AnimeHttpSource).createHttpServer()`, starts it,
 *      reads its actual listening port (random free port).
 *   3. Player calls `video.copyHttpServer(port)` to get a new `Video`
 *      with `videoUrl = "http://localhost:$port/stream/$id"`.
 *   4. Player hands the new videoUrl to ExoPlayer.
 *   5. ExoPlayer connects to `localhost:$port`. The `HttpServer.serve(...)`
 *      method dispatches the request to the source's `videoUrlRequest`
 *      / `videoUrlParse` machinery, fetches the actual upstream bytes,
 *      and streams them back to ExoPlayer.
 *   6. On playback end / quality change, player calls `server.stop()`.
 *
 * DEPENDENCY
 *   Requires `org.nanohttpd:nanohttpd:2.3.1` in `app/build.gradle.kts`.
 *   See `patches/app.build.gradle.kts.patch`.
 *
 * ALTERNATIVE
 *   If you don't want to pull in NanoHTTPD, you can replace this with
 *   Tensei's existing `LocalProxyServer` (port 41223) which already
 *   implements the same proxying with raw `ServerSocket`. The drawback
 *   is `LocalProxyServer` doesn't support per-source logic — every
 *   extension's videos go through the same proxy with the same headers.
 *   The NanoHTTPD approach gives each source its own subclass with
 *   custom routing logic.
 */
abstract class HttpServer(port: Int = 0) : NanoHTTPD(port) {

    /** The actual TCP port the server is bound to. Valid only after `start()`. */
    val listeningPort: Int
        get() = this.boundPort?.let { if (it > 0) it else 0 } ?: 0

    /**
     * Start the server. Call this before playback.
     * Safe to call multiple times — second call is a no-op.
     */
    fun start() {
        if (wasStarted()) return
        start(SOCKET_READ_TIMEOUT, DEFAULT_TCP_BACKLOG)
    }

    /**
     * Stop the server. Call after playback ends.
     * Safe to call multiple times.
     */
    fun stop() {
        try {
            super.stop()
        } catch (_: Throwable) {
            // NanoHTTPD throws if not started — ignore.
        }
    }

    companion object {
        private const val SOCKET_READ_TIMEOUT = -1 // infinite
        private const val DEFAULT_TCP_BACKLOG = 32
    }
}
