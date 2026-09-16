package com.blissless.tensei.ui.screens.player

import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.QualityOption
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.MainViewModel
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Video

/**
 * Pure helper functions for episode playback.
 *
 * Extracted from MainActivity.kt. These functions do NOT touch Compose
 * state — they are pure transformations or data builders that can be
 * unit-tested in isolation.
 *
 * The stateful playback methods (loadAndPlayEpisode, playTorrent, etc.)
 * remain in MainActivity because they close over ~30 remember{} state
 * holders and would require a state-holder class extraction to move.
 * That extraction is a larger architectural change tracked separately.
 */

/**
 * Strips the "Episode N:" or "Ep. N:" prefix from an episode title.
 *
 * Returns null if the input is null. Returns the cleaned title otherwise.
 *
 * Example: "Episode 12: The Final Battle" -> "The Final Battle"
 *          "Ep. 3 - Confrontation" -> "Confrontation"
 *          "12" -> "12" (no prefix to strip)
 */
fun sanitizeEpisodeTitle(title: String?): String? {
    if (title == null) return null
    return title.replaceFirst(
        Regex("^Ep\\.?(?:isode)?\\s*\\d+[\\s:\\-–—]+", RegexOption.IGNORE_CASE),
        ""
    ).trim()
}

/**
 * Builds a list of [ServerInfo] from a list of [Hoster] objects.
 *
 * Each hoster becomes a server entry with its name and URL. The
 * `qualities` field is populated from `hoster.videoList` (the list of
 * `Video` objects inside the hoster) so the server selector can display
 * the available resolutions for each server.
 *
 * ## Handling duplicate hoster names
 *
 * When two hosters share the same `hosterName` (e.g. "Vidstream-2" is a
 * common mirror name across multiple sources), the server selector
 * previously showed both entries identically and the user couldn't
 * tell them apart. Clicking either one looked up the hoster by name
 * and always matched the first one — so the second duplicate was
 * unselectable.
 *
 * To fix this, when a hoster name is duplicated in the input list, we
 * produce distinguishable [ServerInfo] entries via two strategies:
 *
 *   1. **If the hoster has a non-empty `videoList`** (non-lazy hoster):
 *      explode into one [ServerInfo] per video. Name = `"$hosterName
 *      ($videoTitle)"`, URL = `v.videoUrl` (unique per video).
 *
 *   2. **If the hoster is lazy** (`videoList == null` or empty): we
 *      can't explode per-video because we don't have the video info
 *      yet. Instead, append a 1-based index to the name:
 *      `"$hosterName #$n"` (where n is the duplicate's position among
 *      same-named hosters). URL stays as `hosterUrl` (also unique per
 *      hoster).
 *
 * For non-duplicate hosters (the common case), behaviour is unchanged:
 * one [ServerInfo] per hoster, `name = hosterName`, `url = hosterUrl`.
 *
 * Diagnostic logging under the `buildServerList` logcat tag shows the
 * input hoster list + output ServerInfo list so you can verify the
 * disambiguation is happening as expected.
 */
fun buildServerList(hosters: List<Hoster>?): List<ServerInfo> {
    if (hosters.isNullOrEmpty()) {
        android.util.Log.i("buildServerList", "input: null/empty hosters → output: emptyList()")
        return emptyList()
    }

    // First pass: count duplicate hoster names.
    val nameCount = mutableMapOf<String, Int>()
    hosters.forEach { h ->
        val key = h.hosterName.lowercase()
        nameCount[key] = (nameCount[key] ?: 0) + 1
    }

    // Per-name running index for lazy-hosters disambiguation.
    val lazyIndex = mutableMapOf<String, Int>()

    val result = mutableListOf<ServerInfo>()
    for (hoster in hosters) {
        val videos = hoster.videoList.orEmpty()
        val isDuplicate = (nameCount[hoster.hosterName.lowercase()] ?: 1) > 1

        if (isDuplicate && videos.isNotEmpty()) {
            // Strategy 1: explode non-lazy duplicate hosters per video.
            for (v in videos) {
                val qualityLabel = v.videoTitle.ifBlank {
                    if (v.resolution != null && v.resolution > 0) "${v.resolution}p" else "Video"
                }
                result.add(
                    ServerInfo(
                        name = "${hoster.hosterName} ($qualityLabel)",
                        url = v.videoUrl,
                        qualities = listOf(
                            QualityOption(
                                quality = v.videoTitle.ifBlank { qualityLabel },
                                url = v.videoUrl,
                                width = v.resolution ?: 0,
                            ),
                        ),
                    ),
                )
            }
        } else if (isDuplicate) {
            // Strategy 2: lazy duplicate hosters — append a 1-based
            // index so each entry is uniquely identifiable in the
            // dropdown. URL stays as hosterUrl (unique per hoster).
            val key = hoster.hosterName.lowercase()
            val idx = (lazyIndex[key] ?: 0) + 1
            lazyIndex[key] = idx
            result.add(
                ServerInfo(
                    name = "${hoster.hosterName} #$idx",
                    url = hoster.hosterUrl,
                    qualities = emptyList(),  // Lazy — unknown until fetched.
                ),
            )
        } else {
            // Non-duplicate hoster: single entry, populate qualities
            // from videoList (may be empty for lazy hosters).
            result.add(
                ServerInfo(
                    name = hoster.hosterName,
                    url = hoster.hosterUrl,
                    qualities = videos.map { v ->
                        QualityOption(
                            quality = v.videoTitle,
                            url = v.videoUrl,
                            width = v.resolution ?: 0,
                        )
                    },
                ),
            )
        }
    }

    // Diagnostic logging — lets you verify in logcat that the
    // disambiguation actually happened.
    android.util.Log.i("buildServerList", "input: ${hosters.size} hoster(s): ${hosters.map { "${it.hosterName}(lazy=${it.lazy},videos=${it.videoList?.size ?: 0})" }}")
    android.util.Log.i("buildServerList", "output: ${result.size} ServerInfo(s): ${result.map { "${it.name} <- ${it.url.take(60)}" }}")

    return result
}

/**
 * Builds a list of [ServerInfo] from a flat list of [Video] objects.
 *
 * Use this when the extension returns all videos in a single flat list
 * (e.g. animex returns 12 videos for Soft Sub + Hard Sub + Dub all
 * mixed together, with 3 "hosters" that are just category labels
 * whose `videoList` is null). In that case, `buildServerList(hosters)`
 * only produces 3 entries (one per hoster) — but the user wants to see
 * each individual video/quality as a separate dropdown entry.
 *
 * Each video becomes a [ServerInfo] with:
 *   - `name` = the video's `videoTitle` (e.g. "BEEP: 1080p (SUB) [Soft Subs]").
 *   - `url` = the video's `videoUrl` (unique per video — used for lookup
 *     in `handleExtensionServerChange`).
 *   - `qualities` = a single-element list with the video's own quality info.
 *
 * The dropdown's SUB/DUB filter then works because the video titles
 * contain "SUB"/"DUB"/"Soft Subs"/"Hard Subs" markers.
 */
fun buildServerListFromVideos(
    videos: List<Video>?,
    videoHosterNames: List<String> = emptyList(),
): List<ServerInfo> {
    if (videos.isNullOrEmpty()) {
        android.util.Log.i("buildServerList", "buildServerListFromVideos: input null/empty → emptyList()")
        return emptyList()
    }
    val result = videos.mapIndexed { idx, v ->
        // Match by index — videoHosterNames is a list in the same order
        // as `videos`. Using a Map would deduplicate by URL and lose
        // the hoster-to-video association when multiple hosters return
        // the same video URL (common in anikoto/animex: Vidstream-2,
        // Vidstream-1 beta, and HD-1 all return the same /variant/sub/1080p.m3u8).
        val hosterName = videoHosterNames.getOrNull(idx) ?: ""
        val baseName = v.videoTitle.ifBlank {
            if (v.resolution != null && v.resolution > 0) "${v.resolution}p" else "Video ${idx + 1}"
        }
        // Include the hoster/provider name in the display name so
        // entries from different providers are distinguishable even
        // when their video titles are identical (e.g. "Vidstream-2: SUB - 1080p"
        // vs "HD-1: SUB - 1080p" — same title, different provider).
        val displayName = if (hosterName.isNotBlank()) "$hosterName: $baseName" else baseName
        ServerInfo(
            name = displayName,
            url = v.videoUrl,
            qualities = listOf(
                QualityOption(
                    quality = v.videoTitle.ifBlank { baseName },
                    url = v.videoUrl,
                    width = v.resolution ?: 0,
                ),
            ),
        )
    }
    android.util.Log.i("buildServerList", "buildServerListFromVideos: input ${videos.size} video(s) → output ${result.size} ServerInfo(s): ${result.map { it.name.take(50) }}")
    return result
}

/**
 * Builds a list of [QualityOption] from a list of [Video] objects.
 * Each video becomes a quality option with its title, URL, and resolution.
 */
fun buildQualityOptions(videos: List<Video>?): List<QualityOption> {
    if (videos.isNullOrEmpty()) return emptyList()
    return videos.map { v ->
        QualityOption(
            quality = v.videoTitle,
            url = v.videoUrl,
            width = v.resolution ?: 0
        )
    }
}

/**
 * Determines whether a video URL points to our local proxy server.
 * Used to decide whether to pass the extension's OkHttpClient to ExoPlayer.
 */
fun isOurProxyUrl(videoUrl: String, proxyPort: Int): Boolean {
    return videoUrl.contains("127.0.0.1:$proxyPort") ||
           videoUrl.contains("localhost:$proxyPort")
}

/**
 * Rewrites a localhost URL to use our proxy port instead of whatever
 * port the extension's internal server used.
 *
 * Example: "http://127.0.0.1:8080/video.mp4" -> "http://127.0.0.1:41223/video.mp4"
 */
fun rewriteToOurProxy(videoUrl: String, proxyPort: Int): String {
    return videoUrl
        .replace(Regex("127\\.0\\.0\\.1:\\d+"), "127.0.0.1:$proxyPort")
        .replace(Regex("localhost:\\d+"), "127.0.0.1:$proxyPort")
}

/**
 * Picks the best video from a list based on resolution.
 * Falls back to extracting digits from the title if resolution is null/zero.
 * Returns the last video if no resolution info is available.
 */
fun pickBestVideo(videos: List<Video>): Video {
    return videos.maxByOrNull {
        val res = it.resolution ?: 0
        if (res == 0) it.videoTitle.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
        else res
    } ?: videos.last()
}

/**
 * Sorts subtitle tracks by language preference.
 * Preferred language first, then English, then everything else.
 */
fun sortSubtitleTracks(
    tracks: List<eu.kanade.tachiyomi.animesource.model.Track>,
    preferredLang: String,
): List<eu.kanade.tachiyomi.animesource.model.Track> {
    return tracks.sortedByDescending { t ->
        when {
            t.lang.equals(preferredLang, ignoreCase = true) -> 2
            t.lang.equals("English", ignoreCase = true) -> 1
            else -> 0
        }
    }
}
