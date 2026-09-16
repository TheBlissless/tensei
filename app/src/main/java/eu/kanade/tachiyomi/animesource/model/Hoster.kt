package eu.kanade.tachiyomi.animesource.model

import kotlin.jvm.internal.DefaultConstructorMarker

class Hoster(
    val hosterUrl: String = "",
    val hosterName: String = "",
    val videoList: List<Video>? = null,
    val internalData: String = "",
    val lazy: Boolean = false,
    val memo: Int = 24,
) {
    @Suppress("UNUSED_PARAMETER")
    constructor(
        hosterUrl: String,
        hosterName: String,
        videoList: List<Video>?,
        internalData: String?,
        lazy: Boolean,
        memo: Int,
        marker: DefaultConstructorMarker?,
    ) : this(
        hosterUrl = hosterUrl,
        hosterName = hosterName,
        videoList = videoList,
        internalData = internalData.orEmpty(),
        lazy = lazy,
        memo = memo,
    )
    // Ext lib 16 ABI (maskless full-args), kept for compatibility with older extensions
    @Deprecated("Used only for compatibility with ext lib 16, do not use", level = DeprecationLevel.HIDDEN)
    constructor(
        hosterUrl: String,
        hosterName: String,
        videoList: List<Video>?,
        internalData: String,
        lazy: Boolean,
    ) : this(
        hosterUrl = hosterUrl,
        hosterName = hosterName,
        videoList = videoList,
        internalData = internalData,
        lazy = lazy,
    )
    companion object {
        const val NO_HOSTER_LIST = "no_hoster_list"

        fun List<Video>.toHosterList(): List<Hoster> {
            return listOf(
                Hoster(
                    hosterUrl = "",
                    hosterName = NO_HOSTER_LIST,
                    videoList = this,
                ),
            )
        }
    }
}
