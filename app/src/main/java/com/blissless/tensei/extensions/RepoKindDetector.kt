package com.blissless.tensei.extensions

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
        val first = arr.firstOrNull() as? JsonObject ?: return urlHint(url)
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
