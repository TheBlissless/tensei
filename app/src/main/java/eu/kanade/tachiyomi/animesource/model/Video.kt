package eu.kanade.tachiyomi.animesource.model

import android.net.Uri
import kotlinx.serialization.json.JsonObject
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
 * ## Backwards-compatibility shims (CRITICAL)
 *
 * Aniyomi extensions in the wild are compiled against different versions
 * of the aniyomi-lib JAR. The `Video` class's primary constructor has
 * changed over time:
 *
 *   - **ext-lib 14**: 6 fields (`videoUrl, videoTitle, resolution, bitrate, headers, preferred`) + `subtitleTracks`/`audioTracks`.
 *   - **ext-lib 16**: 13 fields (added `timestamps, mpvArgs, ffmpegStreamArgs, ffmpegVideoArgs, internalData, initialized`).
 *   - **ext-lib 17** (current): 15 fields (added `memo: JsonObject`).
 *
 * Extensions compiled against ext-lib 16 call `video.copy(videoUrl = ...)` —
 * the Kotlin compiler generates a call to the synthetic `copy$default`
 * matching the 13-field constructor (no `memo`, no `ffmpegVideoArgs`).
 * If the host class only has the 15-field auto-generated `copy$default`,
 * the call fails at runtime with `NoSuchMethodError: No static method copy$default(...)`.
 *
 * The fix is the manually-defined `copy()` method below (marked
 * `@Deprecated(level = HIDDEN)` so it's invisible to Kotlin callers but
 * visible to the JVM bytecode — exactly mirroring Aniyomi's pattern).
 * Its synthetic `copy$default` has the 14-field signature (no `memo`)
 * that ext-lib 16 extensions expect.
 *
 * ## Field type matching
 *
 * Aniyomi uses `memo: JsonObject = JsonObject.EMPTY` (from
 * `kotlinx.serialization.json`). Earlier Tensei versions used
 * `memo: Int = 16300` — this caused `NoSuchMethodError` on extensions
 * compiled against ext-lib 16 (which expect NO `memo` field at all in
 * the synthetic `copy$default`).
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
    val memo: JsonObject = JsonObject(emptyMap()),
) {
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
        // Delegate directly to the PRIMARY constructor (not the first
        // deprecated constructor) — calling another ERROR-level
        // deprecated constructor from within Kotlin triggers a
        // compile error, so we go straight to the primary ctor.
        videoTitle = quality,
        videoUrl = videoUrl ?: "",
        headers = headers,
    ) {
        this.videoPageUrl = url
    }

    // Ext lib 16 ABI (maskless full-args), kept for compatibility with older extensions
    @Suppress("UNUSED_PARAMETER")
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        videoUrl: String = "",
        videoTitle: String = "",
        resolution: Int? = null,
        bitrate: Int? = null,
        headers: Headers? = null,
        preferred: Boolean = false,
        subtitleTracks: List<Track> = emptyList(),
        audioTracks: List<Track> = emptyList(),
        timestamps: List<TimeStamp> = emptyList(),
        mpvArgs: List<Pair<String, String>> = emptyList(),
        ffmpegStreamArgs: List<Pair<String, String>> = emptyList(),
        ffmpegVideoArgs: List<Pair<String, String>> = emptyList(),
        internalData: String = "",
        initialized: Boolean = false,
    ) : this(
        videoUrl, videoTitle, resolution, bitrate, headers, preferred,
        subtitleTracks, audioTracks, timestamps, mpvArgs,
        ffmpegStreamArgs, ffmpegVideoArgs, internalData, initialized,
        JsonObject(emptyMap()),
    )

    // ─── CRITICAL: Ext lib 16 `copy()` shim ─────────────────────────────────
    //
    // Extensions compiled against aniyomi-lib 16 (which had 14 fields, no
    // `memo`) call `video.copy(videoUrl = "x")` — the Kotlin compiler
    // generates a call to the synthetic `copy$default` of THIS 14-field
    // method. Without it, you get:
    //
    //   NoSuchMethodError: No static method copy$default(Video; String,
    //     String, Integer, Integer, Headers, Z, List, List, List, List,
    //     List, List, String, Z, I, Object)Video
    //
    // The synthetic `copy$default` for this method has the 14-field
    // signature (no `memo`) that ext-lib 16 extensions expect. The
    // `level = HIDDEN` hides it from Kotlin callers (which should use
    // the auto-generated 15-field `copy()` instead) but the JVM bytecode
    // is still present for ext-lib 16 extensions to call.
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    fun copy(
        videoUrl: String = this.videoUrl,
        videoTitle: String = this.videoTitle,
        resolution: Int? = this.resolution,
        bitrate: Int? = this.bitrate,
        headers: Headers? = this.headers,
        preferred: Boolean = this.preferred,
        subtitleTracks: List<Track> = this.subtitleTracks,
        audioTracks: List<Track> = this.audioTracks,
        timestamps: List<TimeStamp> = this.timestamps,
        mpvArgs: List<Pair<String, String>> = this.mpvArgs,
        ffmpegStreamArgs: List<Pair<String, String>> = this.ffmpegStreamArgs,
        ffmpegVideoArgs: List<Pair<String, String>> = this.ffmpegVideoArgs,
        internalData: String = this.internalData,
        initialized: Boolean = this.initialized,
    ): Video = Video(
        videoUrl = videoUrl,
        videoTitle = videoTitle,
        resolution = resolution,
        bitrate = bitrate,
        headers = headers,
        preferred = preferred,
        subtitleTracks = subtitleTracks,
        audioTracks = audioTracks,
        timestamps = timestamps,
        mpvArgs = mpvArgs,
        ffmpegStreamArgs = ffmpegStreamArgs,
        ffmpegVideoArgs = ffmpegVideoArgs,
        internalData = internalData,
        initialized = initialized,
        memo = JsonObject(emptyMap()),
    )

    // ─── Runtime status (transient — not part of equals/hashCode/copy) ──────

    @Transient
    @Volatile
    var status: State = State.QUEUE
        set(value) {
            field = value
        }

    enum class State {
        QUEUE,
        LOAD_VIDEO,
        READY,
        ERROR,
    }

    // ─── Local HttpServer helpers (per-source NanoHTTPD proxy) ──────────────

    /**
     * Returns `true` if any URL on this video (the main `videoUrl`,
     * audio tracks, or subtitle tracks) matches the `http://localhost:1<path>`
     * sentinel pattern that Aniyomi sources use to signal "I need my
     * source's local HttpServer to proxy this request".
     */
    fun usesHttpServer(): Boolean {
        if (LOCAL_URL_REGEX.find(videoUrl) != null) return true
        if (audioTracks.any { LOCAL_URL_REGEX.find(it.url) != null }) return true
        if (subtitleTracks.any { LOCAL_URL_REGEX.find(it.url) != null }) return true
        return false
    }

    /**
     * Returns a copy of this video with ALL `http://localhost:1` sentinel
     * URLs (on the main video URL, audio tracks, AND subtitle tracks)
     * rewritten to `http://localhost:$port`.
     *
     * Called by the player after starting the source's `HttpServer`
     * and reading its actual listening port.
     */
    fun copyHttpServer(port: Int): Video {
        val newHost = "http://localhost:$port"
        return this.copy(
            videoUrl = LOCAL_URL_REGEX.replace(videoUrl, newHost),
            subtitleTracks = subtitleTracks.map {
                it.copy(url = LOCAL_URL_REGEX.replace(it.url, newHost))
            },
            audioTracks = audioTracks.map {
                it.copy(url = LOCAL_URL_REGEX.replace(it.url, newHost))
            },
        )
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
