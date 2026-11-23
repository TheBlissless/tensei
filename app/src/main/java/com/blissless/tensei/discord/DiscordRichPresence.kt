package com.blissless.tensei.discord

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.blissless.tensei.BuildConfig

object DiscordRichPresence {
    // Fallback artwork: the app icon hosted on GitHub, used whenever no anime/manga artwork
    // is available (browsing, or metadata temporarily missing).
    private const val ICON_URL = "https://github.com/TheBlissless/tensei/blob/main/assets/app_icon/icon.png?raw=true"
    private const val PROFILE_URL = "https://avatars.githubusercontent.com/u/89277156?v=4&size=64"
    private const val REPO_URL = "https://github.com/TheBlissless/tensei"
    private const val APP_NAME = "Tensei"
    private const val TAG = "DiscordRPC"

    /** Button labels for the tracker links shown under the presence. */
    private const val BUTTON_ANILIST_LABEL = "AniList"
    private const val BUTTON_MAL_LABEL = "MyAnimeList"

    private var initialized = false
    private val handler = Handler(Looper.getMainLooper())
    private const val CALLBACK_INTERVAL_MS = 1000L

    /**
     * When true the small asset (the app icon / author avatar badge) is left off the
     * presence — mirrors the "hide the small image" setting. Read on every presence
     * update so toggling the setting takes effect on the next refresh.
     */
    @Volatile
    var hideSmallImage: Boolean = false

    /**
     * When true the tracker link buttons (AniList / MyAnimeList) are left off the
     * presence — mirrors the "hide the rich presence buttons" setting. Read on every
     * presence update so toggling the setting takes effect on the next refresh.
     */
    @Volatile
    var hideButtons: Boolean = false

    private val callbackRunnable = object : Runnable {
        override fun run() {
            if (initialized) {
                try {
                    DiscordNative.nativeRunCallbacks()
                } catch (_: Exception) {}
                handler.postDelayed(this, CALLBACK_INTERVAL_MS)
            }
        }
    }

    fun connect() {
        if (initialized) return
        if (!DiscordNative.isAvailable()) {
            Log.w(TAG, "Discord native SDK not available")
            return
        }
        try {
            DiscordNative.nativeInitialize(BuildConfig.DISCORD_APP_ID)
            initialized = true
            handler.post(callbackRunnable)
            Log.i(TAG, "Discord SDK initialized")
            handler.postDelayed({ setBrowsingPresence() }, 2000)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Discord SDK", e)
        }
    }

    fun disconnect() {
        if (!initialized) return
        handler.removeCallbacks(callbackRunnable)
        try {
            DiscordNative.nativeDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to destroy Discord SDK", e)
        }
        initialized = false
        Log.i(TAG, "Discord SDK destroyed")
    }

    /**
     * Presence while watching an episode.
     *
     * Layout: the activity name is "Tensei", so with the Watching activity type the
     * header reads "Watching Tensei"; the details line is the anime name and the state
     * line is "Episode X: Episode title". Two buttons under the presence open the
     * anime's AniList and MyAnimeList pages; they are omitted when [hideButtons] is set.
     *
     * Assets: the large image is the anime's cover art (Discord renders it in its own
     * fixed aspect — the app can only choose which artwork to send), its hover text is
     * the anime title and clicking it opens the anime's page on the tracker the user is
     * logged into ([targetUrl], AniList preferred when both are active). The small image
     * is the app icon, linking to the repository; it is omitted entirely when
     * [hideSmallImage] is set. Both images use plain https URLs — Discord does not
     * resolve `mp:`-prefixed URLs or uploaded asset keys here: those rendered as two
     * white "?" squares while watching.
     */
    fun setAnimePresence(
        animeName: String,
        episode: Int,
        totalEpisodes: Int,
        durationMs: Long = 0L,
        currentPositionMs: Long = 0L,
        imageUrl: String? = null,
        targetUrl: String? = null,
        episodeTitle: String? = null,
        anilistUrl: String? = null,
        malUrl: String? = null,
    ) {
        if (!initialized) return
        val epText = when {
            !episodeTitle.isNullOrBlank() -> "Episode $episode: ${episodeTitle.trim()}"
            totalEpisodes > 0 -> "Episode $episode / $totalEpisodes"
            else -> "Episode $episode"
        }
        val nowSec = System.currentTimeMillis() / 1000
        val endTs: Long
        val startTs: Long
        if (durationMs > 0 && currentPositionMs >= 0) {
            val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0)
            endTs = nowSec + (remainingMs / 1000)
            startTs = nowSec - (currentPositionMs / 1000)
        } else {
            endTs = 0L
            startTs = 0L
        }
        Log.i(TAG, "setAnimePresence: $animeName - $epText, startTs=$startTs endTs=$endTs")
        DiscordNative.nativeSetPresence(
            name = APP_NAME,
            details = animeName,
            state = epText,
            type = 3,
            startTimestamp = startTs,
            endTimestamp = endTs,
            largeImage = artwork(imageUrl),
            largeText = animeName.ifBlank { APP_NAME },
            smallImage = if (hideSmallImage) null else ICON_URL,
            smallText = if (hideSmallImage) null else APP_NAME,
            largeUrl = targetUrl?.takeIf { it.isNotBlank() } ?: REPO_URL,
            smallUrl = if (hideSmallImage) null else REPO_URL,
            button1Label = if (hideButtons) null else BUTTON_ANILIST_LABEL,
            button1Url = if (hideButtons) null else anilistUrl?.takeIf { it.isNotBlank() },
            button2Label = if (hideButtons) null else BUTTON_MAL_LABEL,
            button2Url = if (hideButtons) null else malUrl?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Presence while reading a manga chapter. Same layout as [setAnimePresence]:
     * "Watching Tensei" header, manga title on the details line, chapter label on the
     * state line, manga cover + tracker page link, and the AniList / MyAnimeList
     * buttons underneath.
     */
    fun setMangaPresence(
        mangaTitle: String,
        chapterLabel: String,
        imageUrl: String? = null,
        targetUrl: String? = null,
        anilistUrl: String? = null,
        malUrl: String? = null,
    ) {
        if (!initialized) return
        val startTs = System.currentTimeMillis() / 1000
        Log.i(TAG, "setMangaPresence: $mangaTitle - $chapterLabel")
        DiscordNative.nativeSetPresence(
            name = APP_NAME,
            details = mangaTitle,
            state = chapterLabel,
            type = 3,
            startTimestamp = startTs,
            endTimestamp = 0,
            largeImage = artwork(imageUrl),
            largeText = mangaTitle.ifBlank { APP_NAME },
            smallImage = if (hideSmallImage) null else ICON_URL,
            smallText = if (hideSmallImage) null else APP_NAME,
            largeUrl = targetUrl?.takeIf { it.isNotBlank() } ?: REPO_URL,
            smallUrl = if (hideSmallImage) null else REPO_URL,
            button1Label = if (hideButtons) null else BUTTON_ANILIST_LABEL,
            button1Url = if (hideButtons) null else anilistUrl?.takeIf { it.isNotBlank() },
            button2Label = if (hideButtons) null else BUTTON_MAL_LABEL,
            button2Url = if (hideButtons) null else malUrl?.takeIf { it.isNotBlank() },
        )
    }

    fun setBrowsingPresence() {
        if (!initialized) return
        Log.i(TAG, "setBrowsingPresence")
        DiscordNative.nativeSetPresence(
            name = APP_NAME,
            details = null,
            state = "Exploring Anime & Manga",
            type = 3,
            startTimestamp = 0,
            endTimestamp = 0,
            largeImage = ICON_URL,
            largeText = APP_NAME,
            smallImage = if (hideSmallImage) null else PROFILE_URL,
            smallText = if (hideSmallImage) null else "TheBlissless",
            largeUrl = REPO_URL,
            smallUrl = if (hideSmallImage) null else "https://github.com/TheBlissless",
            button1Label = if (hideButtons) null else BUTTON_ANILIST_LABEL,
            button1Url = if (hideButtons) null else "https://anilist.co",
            button2Label = if (hideButtons) null else BUTTON_MAL_LABEL,
            button2Url = if (hideButtons) null else "https://myanimelist.net",
        )
    }

    fun clearPresence() {
        if (!initialized) return
        Log.i(TAG, "clearPresence")
        setBrowsingPresence()
    }

    fun isConnected(): Boolean = initialized

    /** Pass the cover URL straight through (plain https works with this Discord SDK),
     *  falling back to the hosted app icon when no artwork is available. */
    private fun artwork(url: String?): String? =
        url?.takeIf { it.isNotBlank() && it.startsWith("http", ignoreCase = true) } ?: ICON_URL
}
