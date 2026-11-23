package com.blissless.tensei.discord

import android.util.Log

object DiscordNative {
    private const val TAG = "DiscordNative"
    private var loaded = false

    init {
        try {
            System.loadLibrary("tensei_discord")
            loaded = true
            Log.i(TAG, "Native library loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
            loaded = false
        }
    }

    fun isAvailable(): Boolean = loaded

    external fun nativeInitialize(appId: String)

    /**
     * Sets the rich presence. [name] is the activity name shown next to the verb
     * ("Watching Tensei"); [button1Label]/[button1Url] and [button2Label]/[button2Url]
     * add the up-to-two clickable buttons under the presence (e.g. AniList and
     * MyAnimeList links). Null/blank button fields skip that button; a null
     * [smallImage] hides the small asset.
     */
    external fun nativeSetPresence(
        name: String?,
        details: String?,
        state: String?,
        type: Int,
        startTimestamp: Long,
        endTimestamp: Long,
        largeImage: String?,
        largeText: String?,
        smallImage: String?,
        smallText: String?,
        largeUrl: String?,
        smallUrl: String?,
        button1Label: String?,
        button1Url: String?,
        button2Label: String?,
        button2Url: String?,
    )
    external fun nativeClearPresence()
    external fun nativeRunCallbacks()
    external fun nativeDestroy()
}
