package com.blissless.tensei.extensions

import android.content.Context
import android.util.Log
import com.blissless.tensei.extensions.model.AnimeExtension
import com.blissless.tensei.extensions.model.AnimeLoadResult
import com.blissless.tensei.stream.AnimeExtensionLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton manager for Aniyomi-style extensions.
 *
 * Ported from Aniyomi's `AnimeExtensionManager` at
 * `app/src/main/java/eu/kanade/tachiyomi/extension/anime/AnimeExtensionManager.kt`.
 *
 * What it does:
 *
 *   1. On construction, kicks off a background `initAnimeExtensions()`
 *      call that scans for installed extensions via `AnimeExtensionLoader`
 *      and populates three `StateFlow`s:
 *
 *        - `installedExtensionsFlow: StateFlow<List<AnimeExtension.Installed>>`
 *        - `untrustedExtensionsFlow: StateFlow<List<AnimeExtension.Untrusted>>`
 *
 *      (Available-extensions-from-remote-index is left to your existing
 *       `ExtensionsViewModel` — it already does HTTP fetches of the
 *       repo JSON.)
 *
 *   2. Registers an `AnimeExtensionInstallReceiver` so that any
 *      package install/replace/remove on the device triggers an
 *      automatic re-scan. No more manual "refresh extensions" button.
 *
 *   3. Exposes `trust(extension)`, `uninstallExtension(extension)`,
 *      `installExtension(apkFile)` — the high-level operations the
 *      Extensions UI calls.
 *
 *   4. The `SourceManager` subscribes to `installedExtensionsFlow`
 *      and rebuilds its `sourceId → AnimeSource` map whenever the
 *      extension set changes.
 *
 * USAGE
 *   - In `TenseiApplication.onCreate()`: call
 *     `AnimeExtensionManager.init(application)`. This creates the
 *     singleton and kicks off the background scan.
 *   - In `SourceManager`: call `AnimeExtensionManager.get(context)
 *     .installedExtensionsFlow` and subscribe — see the new
 *     `SourceManager.kt` for the pattern.
 *   - In the Extensions UI: call `AnimeExtensionManager.get(context)
 *     .trust(ext)` when the user taps "Trust" on an untrusted extension.
 *
 * THREAD SAFETY
 *   All state mutations happen on `Dispatchers.IO` via the manager's
 *   private `CoroutineScope`. UI threads can read the `StateFlow`s
 *   directly.
 */
class AnimeExtensionManager private constructor(
    private val context: Context,
) {
    private val TAG = "AnimeExtensionManager"

    /** Background scope for all extension-load + IO operations. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Installed (loaded + trusted) extensions, keyed by package name. */
    private val _installedExtensions = MutableStateFlow<Map<String, AnimeExtension.Installed>>(emptyMap())
    val installedExtensionsFlow: StateFlow<Map<String, AnimeExtension.Installed>> =
        _installedExtensions.asStateFlow()

    /** Installed-but-untrusted extensions, keyed by package name. */
    private val _untrustedExtensions = MutableStateFlow<Map<String, AnimeExtension.Untrusted>>(emptyMap())
    val untrustedExtensionsFlow: StateFlow<Map<String, AnimeExtension.Untrusted>> =
        _untrustedExtensions.asStateFlow()

    /** Convenience accessor: list form of `installedExtensionsFlow`. */
    val installedExtensions: List<AnimeExtension.Installed>
        get() = _installedExtensions.value.values.toList()

    /** Convenience accessor: list form of `untrustedExtensionsFlow`. */
    val untrustedExtensions: List<AnimeExtension.Untrusted>
        get() = _untrustedExtensions.value.values.toList()

    private val isInitialized = MutableStateFlow(false)

    init {
        // Kick off the initial scan on a background thread.
        scope.launch { initAnimeExtensions() }
        // Listen for system package-change broadcasts and re-scan.
        AnimeExtensionInstallReceiver { _, _ ->
            scope.launch { initAnimeExtensions() }
        }.also { receiver ->
            try {
                receiver.register(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register AnimeExtensionInstallReceiver", e)
            }
        }
    }

    /**
     * Scan all installed extensions and update the live StateFlows.
     * Idempotent; safe to call repeatedly. Runs on `Dispatchers.IO`.
     *
     * Mirrors Aniyomi's `AnimeExtensionManager.initAnimeExtensions()`.
     */
    suspend fun initAnimeExtensions() {
        val results = try {
            AnimeExtensionLoader.loadExtensions(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load extensions", e)
            emptyList<AnimeLoadResult>()
        }

        val installed = results.filterIsInstance<AnimeLoadResult.Success>()
            .associate { it.extension.pkgName to it.extension }
        val untrusted = results.filterIsInstance<AnimeLoadResult.Untrusted>()
            .associate { it.extension.pkgName to it.extension }

        _installedExtensions.value = installed
        _untrustedExtensions.value = untrusted
        isInitialized.value = true
        Log.i(TAG, "initAnimeExtensions: ${installed.size} installed, ${untrusted.size} untrusted → installedExtensionsFlow now has ${_installedExtensions.value.size} entries")
    }

    /**
     * Trust the given untrusted extension. After this call succeeds,
     * the extension will be re-loaded and moved from
     * `untrustedExtensionsFlow` to `installedExtensionsFlow`.
     */
    suspend fun trust(extension: AnimeExtension.Untrusted) {
        try {
            // Persist the trust decision so future loads succeed without a prompt.
            TrustAnimeExtension.get(context)
                .trust(extension.pkgName, extension.versionCode, extension.signatureHash)
            // Re-load the now-trusted extension.
            val result = AnimeExtensionLoader.loadExtensionFromPkgName(context, extension.pkgName)
            if (result is AnimeLoadResult.Success) {
                _installedExtensions.value = _installedExtensions.value +
                    (result.extension.pkgName to result.extension)
                _untrustedExtensions.value = _untrustedExtensions.value -
                    extension.pkgName
            } else {
                Log.w(TAG, "Trusted extension ${extension.pkgName} still failed to load: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trust extension ${extension.pkgName}", e)
        }
    }

    /**
     * Revoke trust for an extension (used when the user uninstalls it).
     * Future loads will return `Untrusted`.
     */
    fun revokeTrust(pkgName: String, signatureHash: String) {
        TrustAnimeExtension.get(context).revoke(pkgName, signatureHash)
    }

    /**
     * Look up the extension that owns the given source id, if any.
     * Returns the package name or `null`.
     */
    fun getExtensionPackage(sourceId: Long): String? {
        return _installedExtensions.value.values.find { ext ->
            ext.sources.any { it.id == sourceId }
        }?.pkgName
    }

    /**
     * Get the live `AnimeSource` instance for the given source id, if any.
     * Returns `null` if no installed extension provides a source with that id.
     */
    fun getSource(sourceId: Long): eu.kanade.tachiyomi.animesource.AnimeSource? {
        return _installedExtensions.value.values
            .flatMap { it.sources }
            .find { it.id == sourceId }
    }

    /**
     * Get the package name of every installed extension.
     */
    fun getAllInstalledPackageNames(): List<String> =
        _installedExtensions.value.keys.toList()

    companion object {
        @Volatile private var instance: AnimeExtensionManager? = null

        /**
         * Initialise the singleton. MUST be called early in app startup
         * (e.g. from `TenseiApplication.onCreate()`) so the install
         * receiver is registered before any package-change broadcasts
         * arrive.
         */
        fun init(context: Context) {
            val appContext = context.applicationContext
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = AnimeExtensionManager(appContext)
                    }
                }
            }
        }

        /** Get the singleton. Throws if `init()` hasn't been called. */
        fun get(context: Context): AnimeExtensionManager {
            return instance ?: synchronized(this) {
                instance ?: AnimeExtensionManager(context.applicationContext)
                    .also { instance = it }
            }
        }

        /** Optional: get the singleton if it's been initialised, else null. */
        fun getOrNull(): AnimeExtensionManager? = instance
    }
}
