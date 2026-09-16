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
}
