package com.blissless.tensei

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.blissless.tensei.extensions.AnimeExtensionManager
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import com.blissless.tensei.torrent.TorrentEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.fullType

class TenseiApplication : Application(), ImageLoaderFactory {

    lateinit var torrentEngine: TorrentEngine
        private set

    override fun onCreate() {
        super.onCreate()
        Injekt.addSingleton(fullType<Application>(), this)
        Injekt.addSingleton(fullType<Context>(), this)
        Injekt.addSingleton(fullType<Json>(), Json { ignoreUnknownKeys = true; explicitNulls = false })
        // Mihon-style manga extensions resolve `NetworkHelper` via Injekt (see HttpSource),
        // so the shared network helper must be registered before any extension is loaded.
        Injekt.addSingleton(fullType<NetworkHelper>(), NetworkHelper.getInstance(this))
        torrentEngine = TorrentEngine(this)

        // ── Aniyomi-style extension manager: kicks off a background scan of
        //   installed extensions (via AnimeExtensionLoader.loadExtensions) and
        //   registers the AnimeExtensionInstallReceiver so future
        //   install/remove broadcasts trigger automatic re-scans. Must be
        //   called early in app startup so the receiver is registered before
        //   any PACKAGE_ADDED/REPLACED/REMOVED broadcast can arrive.
        AnimeExtensionManager.init(this)

        // ── Mihon manga extension manager: scans for installed manga extensions
        //   (.apk shared installs and private .ext files) and exposes them to the
        //   external manga source flow.
        MangaExtensionManager.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Route image fetches through the shared OkHttp client so they get the same
            // browser User-Agent, cookie jar, and Cloudflare interception as the extension
            // network calls — many manga image CDNs 403 on Coil's default anonymous client.
            .okHttpClient(NetworkHelper.getInstance(this).client)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache images even if server says not to
            .crossfade(false) // Disable crossfade globally for performance
            .build()
    }
}
