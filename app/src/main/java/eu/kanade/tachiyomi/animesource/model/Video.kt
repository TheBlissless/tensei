package eu.kanade.tachiyomi.animesource.model

import android.net.Uri
import kotlin.jvm.internal.DefaultConstructorMarker
import okhttp3.Headers

data class Track(val url: String, val lang: String)

enum class ChapterType {
    Opening, Ending, Recap, MixedOp, Other,
}

data class TimeStamp(
    val start: Double,
    val end: Double,
    val name: String,
    val type: ChapterType = ChapterType.Other,
)

/**
 * Video data class — one playable video returned from
 * `AnimeSource.getVideoList(hoster)` or `getVideoList(episode)`.
 *
 * Ported from Aniyomi's `Video` at
 * `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/Video.kt`.
 *
 * NEW in this port vs the previous Tensei version:
 *
 *   - `State` enum: runtime status of a video — QUEUE, LOAD_VIDEO,
 *     READY, ERROR. The player uses this to render the per-video
 *     status icon in the Quality sheet and to drive the "auto-advance
 *     to next-best video on failure" logic.
 *
 *   - `@Transient @Volatile var status: State`: runtime status field,
 *     marked transient because it's not part of the serialisable
 *     shape (it's used by the player's UI only).
 *
 *   - `usesHttpServer(): Boolean`: returns true if the video's
 *     `videoUrl` matches the localhost:1 sentinel pattern that
 *     Aniyomi sources use to signal "I need my source's local
 *     HttpServer to proxy this request."
 *
 *   - `copyHttpServer(port): Video`: returns a copy of this video
 *     with the `videoUrl` rewritten from `localhost:1` to
 *     `localhost:$port`. Called by the player after starting the
 *     source's `HttpServer` and reading its actual listening port.
 *
 *   - `MPV_ARGS_TAG`: string constant sources use to label
 *     `mpvArgs` pairs that should be passed through to the MPV
 *     command line (kept for API parity with Aniyomi even though
 *     Tensei uses ExoPlayer, not MPV).
 *
 *   - `localUrl` regex: matches `http://localhost:1<path>` (note the
 *     negative lookahead on the second digit so we don't match
 *     `localhost:12345`).
 *
 * All the existing deprecated constructors are kept so existing
 * extension code keeps working.
 */
data class Video(
    var videoUrl: String = "",
    val videoTitle: String = "",
    val resolution: Int? = null,
    val bitrate: Int? = null,
    val headers: Headers? = null,
    val preferred: Boolean = false,
    val subtitleTracks: List<Track> = emptyList(),
    val audioTracks: List<Track> = emptyList(),
    val timestamps: List<TimeStamp> = emptyList(),
    val mpvArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
    val ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
    val internalData: String = "",
    val initialized: Boolean = false,
    val memo: Int = 16300,
) {
    @Suppress("UNUSED_PARAMETER")
    constructor(
        videoUrl: String,
        videoTitle: String,
        resolution: Int?,
        bitrate: Int?,
        headers: Headers?,
        preferred: Boolean,
        subtitleTracks: List<Track>?,
        audioTracks: List<Track>?,
        timestamps: List<TimeStamp>?,
        mpvArgs: List<Pair<String, String>>?,
        ffmpegStreamArgs: List<Pair<String, String>>?,
        ffmpegVideoArgs: List<Pair<String, String>>?,
        internalData: String?,
        initialized: Boolean,
        memo: Int,
        marker: DefaultConstructorMarker?,
    ) : this(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks.orEmpty(),
        audioTracks = audioTracks.orEmpty(),
        timestamps = timestamps.orEmpty(),
        mpvArgs = mpvArgs.orEmpty(),
        ffmpegStreamArgs = ffmpegStreamArgs.orEmpty(),
        ffmpegVideoArgs = ffmpegVideoArgs.orEmpty(),
        internalData = internalData.orEmpty(),
        initialized = initialized,
        memo = memo,
    )

    @Deprecated("Use videoTitle instead", ReplaceWith("videoTitle"))
    val quality: String
        get() = videoTitle

    val url: String
        get() = videoPageUrl

    private var videoPageUrl: String = ""

    @Deprecated("Use new Video constructor", level = DeprecationLevel.ERROR)
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        headers: Headers? = null,
        subtitleTracks: List<Track> = emptyList(),
        audioTracks: List<Track> = emptyList(),
    ) : this(
        videoTitle = quality,
        videoUrl = videoUrl ?: "",
        headers = headers,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
    ) {
        this.videoPageUrl = url
    }

    @Deprecated("Use new Video constructor", level = DeprecationLevel.ERROR)
    constructor(
        url: String,
        quality: String,
        videoUrl: String?,
        uri: Uri? = null,
        headers: Headers? = null,
    ) : this(
        videoTitle = quality,
        videoUrl = videoUrl ?: "",
        headers = headers,
    )

    // Ext lib 16 ABI (maskless full-args), kept for compatibility with older extensions
    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        videoUrl: String,
        videoTitle: String,
        resolution: Int?,
        bitrate: Int?,
        headers: Headers?,
        preferred: Boolean,
        subtitleTracks: List<Track>?,
        audioTracks: List<Track>?,
        timestamps: List<TimeStamp>?,
        mpvArgs: List<Pair<String, String>>?,
        ffmpegStreamArgs: List<Pair<String, String>>?,
        internalData: String?,
        initialized: Boolean,
    ) : this(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks.orEmpty(),
        audioTracks = audioTracks.orEmpty(),
        timestamps = timestamps.orEmpty(),
        mpvArgs = mpvArgs.orEmpty(),
        ffmpegStreamArgs = ffmpegStreamArgs.orEmpty(),
        ffmpegVideoArgs = emptyList(),
        internalData = internalData.orEmpty(),
        initialized = initialized,
    )

    // Ext lib 16 ABI (masked synthetic with bitmask), kept for compatibility with older extensions
    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        videoUrl: String,
        videoTitle: String,
        resolution: Int?,
        bitrate: Int?,
        headers: Headers?,
        preferred: Boolean,
        subtitleTracks: List<Track>?,
        audioTracks: List<Track>?,
        timestamps: List<TimeStamp>?,
        mpvArgs: List<Pair<String, String>>?,
        ffmpegStreamArgs: List<Pair<String, String>>?,
        internalData: String?,
        initialized: Boolean,
        mask: Int,
        marker: DefaultConstructorMarker?,
    ) : this(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks.orEmpty(),
        audioTracks = audioTracks.orEmpty(),
        timestamps = timestamps.orEmpty(),
        mpvArgs = mpvArgs.orEmpty(),
        ffmpegStreamArgs = ffmpegStreamArgs.orEmpty(),
        ffmpegVideoArgs = emptyList(),
        internalData = internalData.orEmpty(),
        initialized = initialized,
    )

    // ─── NEW: Runtime status ────────────────────────────────────────────────

    /**
     * Runtime status of this video — used by the player's Quality sheet
     * to render the per-video state icon and to drive the "auto-advance
     * to next-best video on failure" logic.
     *
     * `@Transient` because it's not part of the serialisable shape.
     * `@Volatile` because it's read from the UI thread but written
     * from IO threads.
     */
    @Transient
    @Volatile
    var status: State = State.QUEUE

    /**
     * Lifecycle states a `Video` goes through during playback.
     *
     *   QUEUE       → just discovered, not yet fetched
     *   LOAD_VIDEO  → actively fetching the URL (for lazy videos)
     *   READY       → URL resolved, ready to hand to player
     *   ERROR       → fetch failed (network, parse, etc.)
     */
    enum class State {
        QUEUE,
        LOAD_VIDEO,
        READY,
        ERROR,
    }

    // ─── NEW: Local HttpServer helpers ──────────────────────────────────────

    /**
     * Returns `true` if this video's `videoUrl` matches the
     * `http://localhost:1<path>` sentinel pattern that Aniyomi sources
     * use to signal "I need my source's local HttpServer to proxy
     * this request."
     *
     * The player uses this to decide whether to call
     * `(source as AnimeHttpSource).createHttpServer()` and `start()`
     * before handing the URL to ExoPlayer.
     */
    fun usesHttpServer(): Boolean {
        return LOCAL_URL_REGEX.containsMatchIn(videoUrl)
    }

    /**
     * Returns a copy of this video with the `videoUrl` rewritten from
     * `http://localhost:1<path>` to `http://localhost:$port<path>`.
     *
     * Called by the player after starting the source's `HttpServer`
     * and reading its actual listening port.
     */
    fun copyHttpServer(port: Int): Video {
        if (!usesHttpServer()) return this
        val newUrl = LOCAL_URL_REGEX.replace(videoUrl, "http://localhost:$port")
        return copy(videoUrl = newUrl)
    }

    companion object {
        /**
         * Tag string sources use to label `mpvArgs` pairs that should
         * be passed through to the MPV command line. Kept for API
         * parity with Aniyomi even though Tensei uses ExoPlayer, not
         * MPV — extensions targeting the Aniyomi API may set this tag
         * on their `mpvArgs` entries, and we just ignore it.
         */
        const val MPV_ARGS_TAG = "ANIYOMI_MPV_ARGS"

        /**
         * Matches `http://localhost:1<path>` (NOT `http://localhost:12345`)
         * — the sentinel pattern for videos that need a source-local
         * HttpServer proxy.
         *
         * The negative lookahead `(?!\d)` on the second character
         * ensures we only match the sentinel port "1" followed by a
         * non-digit (so we don't match real ports like 1234 or 18765).
         */
        private val LOCAL_URL_REGEX = Regex("""http:\/\/localhost:1(?!\d)""")
    }
}
