package com.blissless.tensei.extensions.repo

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.net.URL

data class Repo(
    val name: String,
    val description: String,
    val extensions: List<RepoExtension>
) {
    companion object {
        fun fromJson(json: JsonObject): Repo {
            // "meta" is the Mihon store root format; top-level "name" is the
            // keiyoushi/anime-repo format. Both carry an extensions array, but
            // keiyoushi nests it under "extensionList".
            val meta = json["meta"] as? JsonObject
            val extensions = ((json["extensions"] as? JsonArray)
                ?: (json["extensionList"] as? JsonObject)?.get("extensions") as? JsonArray
                ?: emptyList()).map { RepoExtension.fromJson(it.jsonObject) }
            return Repo(
                name = json.string("name") ?: meta?.string("name") ?: "",
                description = json.string("description") ?: meta?.string("description") ?: "",
                extensions = extensions
            )
        }
    }
}

enum class ExtensionType(val label: String) {
    STREAM("Stream"),
    TORRENT("Torrent"),
    MANGA("Manga"),
    ANIME("Anime"),
    UNKNOWN("");

    companion object {
        fun fromString(s: String?): ExtensionType = when (s?.lowercase()) {
            "stream" -> STREAM
            "torrent" -> TORRENT
            "manga" -> MANGA
            "anime" -> ANIME
            else -> UNKNOWN
        }

        /**
         * Resolve the type from the extension package name.
         *
         * Tensei/Blissless extensions name themselves after the ecosystem they belong to:
         * `com.blissless.<name>.manga`, `com.blissless.<name>.anime.stream` and
         * `com.blissless.<name>.anime.torrent`. Mihon's manga extensions live under
         * `eu.kanade.tachiyomi.extension.*`.
         */
        fun fromPackageName(packageName: String): ExtensionType {
            if (packageName.isBlank()) return UNKNOWN
            val lower = packageName.lowercase()
            if (lower.endsWith(".manga")) return MANGA
            if (lower.endsWith(".anime.stream")) return STREAM
            if (lower.endsWith(".anime.torrent")) return TORRENT
            if (lower.startsWith("eu.kanade.tachiyomi.extension.")) return MANGA
            val segments = lower.split(".")
            if (segments.contains("manga")) return MANGA
            if (segments.contains("anime")) return ANIME
            return UNKNOWN
        }
    }
}

data class RepoExtension(
    val name: String,
    val packageName: String,
    val apk: String,
    val icon: String = "",
    val lang: String,
    val version: String,
    val code: Long,
    val nsfw: Boolean,
    val sources: List<RepoSources>,
    val type: ExtensionType = ExtensionType.UNKNOWN
) {
    companion object {
        fun fromJson(json: JsonObject): RepoExtension {
            val packageName = json.string("pkg") ?: json.string("packageName") ?: ""
            val declaredType = ExtensionType.fromString(json.string("type"))
            // Keiyoushi index keeps the artifacts in a nested "resources" object, with
            // absolute URLs; other formats use flat relative "apk"/"icon" paths.
            val resources = json["resources"] as? JsonObject
            val sources = (json["sources"] as? JsonArray)
                ?.map { RepoSources.fromJson(it.jsonObject) }
                ?: emptyList()
            return RepoExtension(
                name = json.string("name") ?: "",
                packageName = packageName,
                apk = json.string("apk") ?: json.string("url")
                    ?: resources?.string("apkUrl") ?: "",
                icon = json.string("icon") ?: json.string("icons")
                    ?: resources?.string("iconUrl") ?: "",
                lang = json.string("lang") ?: sources.firstOrNull()?.lang?.takeIf { it.isNotBlank() } ?: "en",
                version = json.string("version") ?: json.string("versionName") ?: "",
                code = json.string("code")?.toLongOrNull()
                    ?: json.string("versionCode")?.toLongOrNull() ?: 0L,
                nsfw = parseNsfw(json),
                sources = sources,
                type = resolveType(declaredType, packageName)
            )
        }

        /**
         * NSFW flags differ per format: Mihon-style repos use a boolean or a numeric
         * 0/1/2 flag, keiyoushi uses a "contentWarning" enum string. A null or "0"
         * value means SFW; anything else in the numeric case is treated as NSFW.
         */
        private fun parseNsfw(json: JsonObject): Boolean {
            json.string("nsfw")?.let { raw ->
                return raw.equals("true", ignoreCase = true) ||
                    (raw.toIntOrNull()?.let { it > 0 } ?: false)
            }
            return when (json.string("contentWarning")) {
                "CONTENT_WARNING_NSFW", "CONTENT_WARNING_MIXED" -> true
                else -> false
            }
        }

        /**
         * The package name wins over the type declared by the repository: a manga extension
         * must never be badged as anime just because the index says so.
         */
        private fun resolveType(declared: ExtensionType, packageName: String): ExtensionType {
            if (declared == ExtensionType.STREAM || declared == ExtensionType.TORRENT) return declared
            val hinted = ExtensionType.fromPackageName(packageName)
            return if (hinted != ExtensionType.UNKNOWN) hinted else declared
        }
    }
}

data class RepoSources(
    val name: String,
    val lang: String
) {
    companion object {
        fun fromJson(json: JsonObject): RepoSources {
            return RepoSources(
                name = json.string("name") ?: "",
                lang = json.string("lang") ?: json.string("language") ?: "en"
            )
        }
    }
}

fun resolveApkUrl(repoUrl: String, apkPath: String): String {
    if (apkPath.startsWith("http://") || apkPath.startsWith("https://")) {
        return apkPath
    }
    val base = URL(repoUrl)
    val path = if ("/" in apkPath) apkPath else "apk/$apkPath"
    return URL(base, path).toString()
}

fun resolveIconUrl(repoUrl: String, iconPath: String): String {
    if (iconPath.isBlank()) return ""
    if (iconPath.startsWith("http://") || iconPath.startsWith("https://")) {
        return iconPath
    }
    val base = URL(repoUrl)
    val path = if ("/" in iconPath) iconPath else "icon/$iconPath"
    return URL(base, path).toString()
}

fun parseRepoJson(repoUrl: String, jsonElement: JsonElement): Repo {
    val fallbackName = extractRepoName(repoUrl)
    return when (jsonElement) {
        is JsonObject -> {
            val repo = Repo.fromJson(jsonElement)
            if (repo.name.isBlank()) repo.copy(name = fallbackName) else repo
        }
        is JsonArray -> {
            val extensions = jsonElement.map { RepoExtension.fromJson(it.jsonObject) }
            Repo(name = fallbackName, description = "", extensions = extensions)
        }
        else -> throw Exception("Unexpected JSON type: ${jsonElement::class.simpleName}")
    }
}

/** Human-friendly repo name derived from its URL (e.g. "keiyoushi" for a raw.githubusercontent URL). */
internal fun extractRepoName(repoUrl: String): String {
    return try {
        val url = URL(repoUrl)
        val host = url.host.lowercase()
        val path = url.path.trim('/')
        when (host) {
            "raw.githubusercontent.com" -> {
                val parts = path.split('/')
                if (parts.size >= 2) parts[0] else host
            }
            "github.com" -> {
                val parts = path.split('/')
                if (parts.isNotEmpty()) parts[0] else host
            }
            else -> host
        }
    } catch (_: Exception) {
        repoUrl
    }
}

/** Content of [key] as a primitive string, or null when absent or not a primitive. */
private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.content
