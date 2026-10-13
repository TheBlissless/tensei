package com.blissless.tensei.extensions.manga

import com.blissless.tensei.extensions.manga.index.NetworkExtensionStore
import com.blissless.tensei.extensions.manga.index.NetworkLegacyExtension
import com.blissless.tensei.extensions.manga.index.NetworkLegacyExtensionRepo
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

/**
 * Fetches and decodes a single Mihon-compatible extension index. Accepts any repo URL such as
 * `index.min.json`, `repo.json`, `index.pb` or a protobuf `index_v2`.
 */
class MangaExtensionRepository(
    private val client: OkHttpClient,
    private val json: Json,
    private val protoBuf: ProtoBuf,
    private val indexUrl: String,
) {

    private data class Store(
        val indexUrl: String,
        val legacy: Boolean,
        val extensionListUrl: String? = null,
        val embedded: List<NetworkExtensionStore.Extension>? = null,
    )

    suspend fun fetch(): Result<List<AvailableExtension>> = withContext(Dispatchers.IO) {
        runCatching {
            val store = fetchStore(indexUrl)
            fetchList(store).sortedBy { it.name.lowercase() }
        }
    }

    private suspend fun fetchStore(url: String, depth: Int = 0): Store {
        if (depth > 4) throw IllegalStateException("extension index redirect loop")
        val bytes = fetchBytes(url)
        if (bytes.isEmpty()) throw IllegalStateException("empty response from $url")

        return when (bytes[0]) {
            '['.code.toByte() -> {
                val repoUrl = if (url.endsWith("/index.min.json")) {
                    url.removeSuffix("/index.min.json") + "/repo.json"
                } else {
                    url
                }
                if (repoUrl == url) throw IllegalStateException("unexpected legacy index at $url")
                val repo = json.decodeFromString<NetworkLegacyExtensionRepo>(fetchBytes(repoUrl).decodeToString())
                if (repo.indexV2 != null) {
                    fetchStore(resolve(repo.indexV2, repoUrl), depth + 1)
                } else {
                    Store(repoUrl, legacy = true)
                }
            }
            '{'.code.toByte() -> {
                val text = bytes.decodeToString()
                val repo = runCatching { json.decodeFromString<NetworkLegacyExtensionRepo>(text) }.getOrNull()
                when {
                    repo == null -> json.decodeFromString<NetworkExtensionStore>(text).toStore(url)
                    repo.indexV2 != null -> fetchStore(resolve(repo.indexV2, url), depth + 1)
                    else -> Store(url, legacy = true)
                }
            }
            else -> protoBuf.decodeFromByteArray<NetworkExtensionStore>(bytes).toStore(url)
        }
    }

    private fun NetworkExtensionStore.toStore(url: String) = Store(
        indexUrl = url,
        legacy = false,
        extensionListUrl = extensionListUrl,
        embedded = extensionList?.extensions,
    )

    private suspend fun fetchList(store: Store): List<AvailableExtension> = when {
        store.extensionListUrl != null -> {
            val bytes = fetchBytes(store.extensionListUrl)
            val extensions = if (bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()) {
                json.decodeFromString<NetworkExtensionStore.ExtensionList>(bytes.decodeToString()).extensions
            } else {
                protoBuf.decodeFromByteArray<NetworkExtensionStore.ExtensionList>(bytes).extensions
            }
            extensions.map { it.toAvailable(store.indexUrl, indexUrl) }
        }

        store.embedded != null -> store.embedded.map { it.toAvailable(store.indexUrl, indexUrl) }

        store.legacy -> {
            val base = store.indexUrl.removeSuffix("/repo.json")
            json.decodeFromString<List<NetworkLegacyExtension>>(fetchBytes("$base/index.min.json").decodeToString())
                .map { legacy ->
                    AvailableExtension(
                        name = legacy.name.substringAfter("Tachiyomi: "),
                        pkgName = legacy.pkg,
                        apkUrl = resolve("${legacy.apk}", "$base/apk/"),
                        iconUrl = resolve("$base/icon/${legacy.pkg}.png", store.indexUrl),
                        versionName = legacy.version,
                        versionCode = legacy.code,
                        lang = legacy.lang,
                        libVersion = legacy.version.substringBeforeLast('.').toDoubleOrNull() ?: 0.0,
                        nsfw = legacy.nsfw == 1,
                        sourceNames = legacy.sources.orEmpty().map { it.name }.ifEmpty { listOf(legacy.name) },
                        repoUrl = indexUrl,
                    )
                }
        }

        else -> throw IllegalStateException("extension index has no entries")
    }

    private fun NetworkExtensionStore.Extension.toAvailable(indexUrl: String, repoUrl: String): AvailableExtension {
        val lang = sources.map { it.language }.toSet().let { langs ->
            when (langs.size) {
                0 -> ""
                1 -> langs.first()
                else -> "all"
            }
        }
        return AvailableExtension(
            name = name,
            pkgName = packageName,
            apkUrl = resolve(resources.apkUrl, indexUrl),
            iconUrl = resolve(resources.iconUrl, indexUrl),
            versionName = versionName,
            versionCode = versionCode,
            lang = lang,
            libVersion = extensionLib.toDoubleOrNull() ?: 0.0,
            nsfw = contentWarning == NetworkExtensionStore.ContentWarning.MIXED ||
                contentWarning == NetworkExtensionStore.ContentWarning.NSFW,
            sourceNames = sources.map { it.name },
            repoUrl = repoUrl,
        )
    }

    private suspend fun fetchBytes(url: String): ByteArray {
        val response = client.newCall(GET(url)).awaitSuccess()
        val raw = response.body.bytes()
        val gzipped = raw.size >= 2 && raw[0] == 0x1f.toByte() && raw[1] == 0x8b.toByte()
        if (!gzipped) return raw
        return GZIPInputStream(ByteArrayInputStream(raw)).use { it.readBytes() }
    }

    private fun resolve(url: String, base: String): String {
        if (url.isBlank()) return url
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return runCatching { base.toHttpUrl().resolve(url)?.toString() }.getOrNull() ?: url
    }
}
