package com.blissless.tensei.extensions.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory

/**
 * Sealed-class model for an Aniyomi-style anime extension.
 *
 * Ported from Aniyomi's `domain/src/main/java/eu/kanade/tachiyomi/extension/anime/model/AnimeExtension.kt`.
 *
 * An extension can be in one of three runtime states:
 *
 *   - `Installed`: an APK we successfully loaded, signature verified, with
 *     one or more live `AnimeSource` instances ready to be queried.
 *   - `Available`: an extension listed in the remote extension index but
 *     not yet installed on this device. Used by the Extensions browser UI.
 *   - `Untrusted`: an APK whose signing certificate is not in our trust
 *     store. The UI must prompt the user to trust or uninstall before we
 *     will instantiate any code from it.
 *
 * Each state carries different fields (e.g. `Installed` has `sources`
 * and `icon`, while `Available` has `apkUrl` and `iconUrl`). The
 * `AnimeExtensionManager` exposes separate `StateFlow`s for each state
 * so the UI can render them in different sections.
 *
 * NOTE: the legacy Tensei `com.blissless.tensei.extensions.Extension`
 * data class is left untouched for backward compatibility with existing
 * UI code; this sealed class is the NEW canonical model used by the
 * `AnimeExtensionManager` + `SourceManager`.
 */
sealed class AnimeExtension {
    abstract val name: String
    abstract val pkgName: String
    abstract val versionName: String
    abstract val versionCode: Long
    abstract val libVersion: Double
    abstract val lang: String?
    abstract val isNsfw: Boolean
    abstract val isTorrent: Boolean

    /**
     * An extension that has been successfully loaded.
     *
     * @property sources the live `AnimeSource` instances loaded from the
     *   extension APK. May be more than one if the extension ships an
     *   `AnimeSourceFactory`.
     * @param pkgFactory the optional class name of the `AnimeSourceFactory`
     *   the extension declares — useful for re-instantiation without
     *   re-loading the whole APK.
     * @param icon the APK's icon, lazily loaded. May be `null` if the
     *   PackageManager couldn't read it.
     * @param hasUpdate `true` if a newer version is available in the
     *   remote extension index.
     * @param isObsolete `true` if the extension was built against a lib
     *   version we no longer support.
     * @param isShared `true` if the extension is installed via Android's
     *   package installer (system-wide); `false` if it's a private `.ext`
     *   file in Tensei's internal storage.
     */
    data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val isTorrent: Boolean,
        val pkgFactory: String?,
        val sources: List<AnimeSource>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val isShared: Boolean = true,
    ) : AnimeExtension() {

        /**
         * The first source's metadata — convenience accessor for UIs that
         * only show one source per extension.
         */
        val mainSource: AnimeSource? get() = sources.firstOrNull()
    }

    /**
     * An extension advertised in the remote extension index but not yet
     * installed. The Extensions browser UI uses this to render the list
     * of installable extensions.
     */
    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        override val isTorrent: Boolean,
        val apkUrl: String,
        val iconUrl: String,
        val sources: List<AnimeSourceInfo>,
    ) : AnimeExtension() {

        /**
         * Lightweight source descriptor for an `Available` extension. Because
         * the extension isn't installed, we can't instantiate its `AnimeSource`
         * class — but we can show its name/lang/baseUrl from the index.
         */
        data class AnimeSourceInfo(
            val id: Long,
            val name: String,
            val lang: String,
            val baseUrl: String,
        )
    }

    /**
     * An extension whose signing certificate is not in our trust store.
     * The UI must prompt the user to trust it (and re-load it) or uninstall
     * it before we will instantiate any code from it.
     */
    data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String?,
        override val isNsfw: Boolean,
        override val isTorrent: Boolean,
        val signatureHash: String,
        val icon: Drawable?,
    ) : AnimeExtension()
}
