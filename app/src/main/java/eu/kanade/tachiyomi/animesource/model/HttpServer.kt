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
 * IMPLEMENTATION NOTES
 *   NanoHTTPD 2.3.1's public API surface (relevant subset):
 *     - `NanoHTTPD(int port)` constructor (also `(String host, int port)`)
 *     - `void start() throws IOException`  ← no-arg overload (default timeout=−1, backlog=100, daemon=true)
 *     - `void start(int timeout) throws IOException`
 *     - `void start(int timeout, boolean daemon) throws IOException`  ← (int, boolean), NOT (int, int)
 *     - `void start(int timeout, int backlog, boolean daemon) throws IOException`
 *     - `final boolean isAlive()`
 *     - `final int getListeningPort()`  ← Kotlin-accessed as `listeningPort`
 *     - `void stop()`  ← overridden here to track the `isRunning` flag
 *
 *   The earlier version of this file declared its own `start()` and
 *   `stop()` (no-arg) and called `wasStarted()` + `boundPort`, neither
 *   of which exist on NanoHTTPD, AND it tried to call
 *   `start(int, int)` which NanoHTTPD doesn't have (only
 *   `start(int, int, boolean)` exists). This version is a clean fix
 *   mirroring Aniyomi's exact pattern:
 *     - `override fun start()` calls `super.start()` (no-arg overload).
 *     - `override fun stop()` calls `super.stop()`.
 *     - `@Volatile var isRunning` tracks state so we don't double-start
 *       or double-stop.
 *     - `url: String` convenience property exposing
 *       `http://localhost:$listeningPort`.
 */
open class HttpServer : NanoHTTPD(0) {

    /**
     * The full URL to the local HTTP server root, e.g. `http://localhost:8321`.
     * Valid only after [start] returns successfully.
     */
    val url: String
        get() = "http://localhost:$listeningPort"

    /**
     * Tracks whether the server is currently running. Set to `true` in
     * [start], `false` in [stop]. Safe to read from any thread.
     */
    @Volatile
    private var isRunning = false

    /**
     * Convenience accessor that mirrors the [isRunning] flag.
     * Kept for API parity with Aniyomi's `HttpServer.isRunning()`.
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Start the server. Call this before playback.
     * Safe to call multiple times — second call is a no-op if already running.
     *
     * Calls NanoHTTPD's no-arg `super.start()` which internally calls
     * `start(-1, 100, true)` (infinite timeout, default backlog, daemon thread).
     */
    override fun start() {
        if (isRunning) return
        try {
            super.start()
            isRunning = true
        } catch (e: Exception) {
            // Swallow + log — most failures here are "port already in use"
            // and the player will fail to play this video with a clearer
            // error from ExoPlayer anyway.
            android.util.Log.w("HttpServer", "Failed to start http server", e)
        }
    }

    /**
     * Stop the server. Call after playback ends.
     * Safe to call multiple times.
     */
    override fun stop() {
        if (!isRunning) return
        try {
            super.stop()
        } catch (_: Throwable) {
            // NanoHTTPD occasionally throws on stop — ignore.
        }
        isRunning = false
    }

    companion object {
        /**
         * Sentinel URL prefix used by extension sources to signal
         * "I want my source's local HttpServer to proxy this video URL".
         * Matches the regex in `Video.LOCAL_URL_REGEX`.
         */
        const val PLACEHOLDER_URL = "http://localhost:1"
    }
}
