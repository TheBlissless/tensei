package com.blissless.tensei.extensions

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URL

data class Repo(
    val name: String,
    val description: String,
    val extensions: List<RepoExtension>
) {
    companion object {
        fun fromJson(json: JsonObject): Repo {
            return Repo(
                name = json["name"]?.jsonPrimitive?.content ?: "",
                description = json["description"]?.jsonPrimitive?.content ?: "",
                extensions = (json["extensions"] as? JsonArray)?.map { it.jsonObject.let(RepoExtension::fromJson) } ?: emptyList()
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
            val packageName = (json["pkg"] ?: json["packageName"])?.jsonPrimitive?.content ?: ""
            val declaredType = ExtensionType.fromString((json["type"] as? JsonPrimitive)?.content)
            return RepoExtension(
                name = json["name"]?.jsonPrimitive?.content ?: "",
                packageName = packageName,
                apk = json["apk"]?.jsonPrimitive?.content ?: json["url"]?.jsonPrimitive?.content ?: "",
                icon = (json["icon"] ?: json["icons"])?.jsonPrimitive?.content ?: "",
                lang = json["lang"]?.jsonPrimitive?.content ?: "en",
                version = json["version"]?.jsonPrimitive?.content ?: "",
                code = (json["code"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
                nsfw = (json["nsfw"] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false,
                sources = (json["sources"] as? JsonArray)?.map { it.jsonObject.let(RepoSources::fromJson) } ?: emptyList(),
                type = resolveType(declaredType, packageName)
            )
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
                name = json["name"]?.jsonPrimitive?.content ?: "",
                lang = json["lang"]?.jsonPrimitive?.content ?: "en"
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

private fun extractRepoName(repoUrl: String): String {
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
