package com.blissless.tensei.extensions.repo

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Which extension ecosystem a repository belongs to. */
enum class RepoKind { ANIME, MANGA }

/**
 * Works out whether a repository URL points at an anime repository or at a Mihon-style manga
 * repository, so the user never has to pick the type by hand.
 *
 * The two ecosystems are told apart by their payload:
 *  - Anime repos are JSON with an "extensions" array (or a bare array of extension entries,
 *    each of which may declare a "type").
 *  - Manga repos are the Mihon store: "repo.json" (meta/index_v2), "index.min.json" (a flat
 *    array whose entries carry a numeric "nsfw" flag and "Tachiyomi: " names) or the protobuf
 *    "index.pb" index, which isn't JSON at all.
 *
 * When the payload is ambiguous the URL is used as a tiebreaker.
 */
object RepoKindDetector {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Detect the repository kind from its payload.
     *
     * @param url the repository URL, used as a tiebreaker for ambiguous payloads
     * @param body the raw (already decompressed) repository payload
     */
    fun detect(url: String, body: ByteArray): RepoKind {
        val text = String(body, Charsets.UTF_8).trimStart()
        if (!text.startsWith("{") && !text.startsWith("[")) {
            // An HTML page (someone pasted a github.com repo URL) is not a payload we
            // can parse either way; fall back to the URL hint instead of assuming the
            // Mihon protobuf index.
            if (text.startsWith("<")) return urlHint(url)
            // Binary payload — the Mihon protobuf index.
            return RepoKind.MANGA
        }
        val element = runCatching { json.parseToJsonElement(text) }.getOrNull()
        return when (element) {
            is JsonObject -> detectObject(url, element)
            is JsonArray -> detectArray(url, element)
            else -> urlHint(url)
        }
    }

    private fun detectObject(url: String, obj: JsonObject): RepoKind {
        // The anime repo format keeps its entries under "extensions".
        if (obj["extensions"] is JsonArray) return RepoKind.ANIME
        // The Mihon store root ("repo.json") and the store format ("meta"/"index_v2").
        if (obj.containsKey("meta") || obj.containsKey("index_v2") || obj.containsKey("extensionList")) {
            return RepoKind.MANGA
        }
        return urlHint(url)
    }

    private fun detectArray(url: String, arr: JsonArray): RepoKind {
        val entries = arr.take(20).mapNotNull { it as? JsonObject }
        if (entries.isEmpty()) return urlHint(url)

        // Aniyomi-style anime repositories reuse the Mihon index.min.json payload, so
        // the format alone can't tell them apart — the ENTRIES can. A repository whose
        // entries live under "eu.kanade.tachiyomi.animeextension.*" packages or are
        // named "Aniyomi: ..." is an anime repo (e.g. yuzono/anime-repo) even though
        // it carries the numeric nsfw flags and flat array of a Mihon store.
        val animeMarkers = entries.count { entry ->
            val pkg = (entry["pkg"] as? JsonPrimitive)?.content.orEmpty()
            val name = (entry["name"] as? JsonPrimitive)?.content.orEmpty()
            pkg.contains("animeextension", ignoreCase = true) ||
                pkg.contains("aniyomi", ignoreCase = true) ||
                name.startsWith("Aniyomi", ignoreCase = true)
        }
        if (animeMarkers > 0 && animeMarkers >= entries.size / 2) return RepoKind.ANIME

        val first = entries.first()
        // Anime entries declare their own type.
        if (first.containsKey("type")) return RepoKind.ANIME
        // Mihon's legacy index flags NSFW numerically (0/1) and names extensions "Tachiyomi: X".
        if ((first["nsfw"] as? JsonPrimitive)?.content?.toIntOrNull() != null) return RepoKind.MANGA
        val name = (first["name"] as? JsonPrimitive)?.content.orEmpty()
        if (name.startsWith("Tachiyomi", ignoreCase = true)) return RepoKind.MANGA
        return urlHint(url)
    }

    private fun urlHint(url: String): RepoKind {
        val lower = url.lowercase()
        val file = lower.substringAfterLast('/')
        return when {
            file.endsWith("index.pb") || file.endsWith("index.min.json") || file.endsWith("repo.json") -> RepoKind.MANGA
            file.endsWith("index.json") -> RepoKind.ANIME
            lower.contains("manga") -> RepoKind.MANGA
            else -> RepoKind.ANIME
        }
    }
}
