package com.blissless.tensei.stream

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import com.blissless.tensei.extensions.ANIME_EXTENSION_FEATURE
import com.blissless.tensei.extensions.METADATA_ANIME_NSFW
import com.blissless.tensei.extensions.METADATA_ANIME_SOURCE_CLASS
import com.blissless.tensei.extensions.METADATA_NSFW
import com.blissless.tensei.extensions.METADATA_SOURCE_CLASS
import com.blissless.tensei.extensions.METADATA_SOURCE_FACTORY
import com.blissless.tensei.extensions.TrustAnimeExtension
import com.blissless.tensei.extensions.model.AnimeExtension
import com.blissless.tensei.extensions.model.AnimeLoadResult
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.fullType
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Aniyomi-style extension loader.
 *
 * This file is a direct port of Aniyomi's `AnimeExtensionLoader` from
 * `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionLoader.kt`.
 *
 * It replaces the previous Tensei `ExtensionLoader` which only supported
 * the older parent-first classloader pattern. The new loader:
 *
 *   1. Reads BOTH shared (system-installed) extensions AND private `.ext`
 *      APK files dropped into `context.filesDir/exts/` (Aniyomi's PRIVATE
 *      installer format — lets users sideload extensions without going
 *      through Android's package installer).
 *   2. Parses the full Aniyomi metadata block:
 *        - `tachiyomi.animeextension`        (uses-feature)
 *        - `tachiyomi.animeextension.class`  (source class name)
 *        - `tachiyomi.animeextension.factory` (source factory class name)
 *        - `tachiyomi.animeextension.nsfw`   (boolean)
 *        - `tachiyomi.extension.class`       (legacy manga-style fallback)
 *        - `tachiyomi.extension.factory`     (legacy manga-style fallback)
 *        - `tachiyomi.extension.nsfw`        (legacy)
 *        - `aniyomix.extensionLib`           (lib version: 14, 16, or 17)
 *        - `aniyomix.name`                  (display name)
 *        - `aniyomix.torrent`               (boolean — torrent-type source)
 *   3. Validates the lib version against `SUPPORTED_LIB_VERSIONS`. Extensions
 *      built against an unsupported lib version return `AnimeLoadResult.Error`
 *      so the UI can show "obsolete extension, please update" instead of
 *      crashing at runtime.
 *   4. Verifies the extension's signing certificate against the persisted
 *      trusted-signature set in `TrustAnimeExtension`. Untrusted extensions
 *      return `AnimeLoadResult.Untrusted` so the UI can prompt the user to
 *      trust or uninstall — instead of silently loading arbitrary code.
 *   5. Loads the source classes via `ChildFirstPathClassLoader` (parent-last)
 *      so each extension runs against its own bundled copy of the source API.
 *   6. Registers per-extension `SharedPreferences` via Injekt so
 *      `ConfigurableAnimeSource.getSourcePreferences()` works.
 *
 * The API surface mirrors Aniyomi's: callers receive a `List<AnimeLoadResult>`
 * where each entry is `Success`, `Untrusted`, or `Error`. The
 * `AnimeExtensionManager` consumes these and exposes the live set of
 * `AnimeExtension.Installed` via a `StateFlow`.
 */
@Suppress("MemberVisibilityCanBePrivate")
internal object AnimeExtensionLoader {

    private const val TAG = "AnimeExtensionLoader"

    // ─── Metadata keys ────────────────────────────────────────────────────────
    private const val METADATA_ANIYOMIX_NAME = "aniyomix.name"
    private const val METADATA_ANIYOMIX_EXTENSION_LIB = "aniyomix.extensionLib"
    private const val METADATA_ANIYOMIX_TORRENT = "aniyomix.torrent"

    /**
     * The Aniyomi extension-lib ABI versions Tensei's loader currently
     * understands. Mirrors Aniyomi's `SUPPORTED_LIB_VERSIONS`.
     *
     *   - 14.0 → old `fetchEpisodeList` / `fetchVideoList` RxJava API.
     *   - 16.0 → `getHosterList(episode)` + `getVideoList(hoster)`.
     *   - 17.0 → combined `getAnimeEpisodeUpdate` API (parallel fetch).
     *
     * Aniyomi supports all three simultaneously because the runtime inspects
     * the source's class via reflection to decide which API to call.
     * Tensei does the same.
     */
    private val SUPPORTED_LIB_VERSIONS = listOf(14.0, 16.0, 17.0)

    /** File extension for private (sideloaded) extension APKs. */
    private const val PRIVATE_EXTENSION_EXTENSION = "ext"

    /** Directory under `filesDir` where private extensions live. */
    private const val PRIVATE_EXTENSION_DIR = "exts"

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS: Int = PackageManager.GET_CONFIGURATIONS or
            PackageManager.GET_META_DATA or
            PackageManager.GET_SIGNING_CERTIFICATES or
            PackageManager.GET_PERMISSIONS

    /** Per-APK-path ClassLoader cache so we don't reload the same dex twice. */
    private val loaderCache = ConcurrentHashMap<String, ChildFirstPathClassLoader>()

    // ─── Public entry points ─────────────────────────────────────────────────

    /**
     * Load every installed extension on the device. Mirrors Aniyomi's
     * `AnimeExtensionLoader.loadExtensions(context): List<AnimeLoadResult>`.
     *
     * Iterates over BOTH shared (system-installed) APKs that declare the
     * `tachiyomi.animeextension` feature AND private `.ext` files dropped
     * into `context.filesDir/exts/`. De-duplicates by package name (a
     * package that exists in both forms is loaded from whichever location
     * has the higher versionCode — same rule as Aniyomi).
     */
    fun loadExtensions(context: Context): List<AnimeLoadResult> {
        val pm = context.packageManager

        // ── Diagnostic: count all installed packages ─────────────────────
        val allInstalled = try {
            getInstalledPackages(pm)
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing QUERY_ALL_PACKAGES permission", e)
            emptyList()
        }
        Log.i(TAG, "loadExtensions: PackageManager returned ${allInstalled.size} installed packages")

        // ── Diagnostic: log every package that "looks like" an extension,
        //    even ones isPackageAnExtension() might reject, so we can see
        //    what's actually present on the device.
        val candidatePkgs = allInstalled.filter {
            val n = it.packageName
            n.startsWith("eu.kanade.tachiyomi.animeextension") ||
            n.startsWith("eu.kanade.tachiyomi.extension") ||
            n.startsWith("com.blissless.")
        }
        Log.i(TAG, "loadExtensions: ${candidatePkgs.size} candidate package(s) by name:")
        candidatePkgs.forEach { p ->
            val feats = p.reqFeatures.orEmpty().mapNotNull { it.name }.toSet()
            val md = p.applicationInfo?.metaData
            val mdKeys = md?.keySet()?.toList().orEmpty()
            Log.i(TAG, "  pkg=${p.packageName} features=$feats smells=$mdKeys hasAnimeFeature=${ANIME_EXTENSION_FEATURE in feats}")
        }

        val sharedExtPkgs = allInstalled.asSequence()
            .filter { isPackageAnExtension(it) }
            .map { AnimeExtensionInfo(packageInfo = it, isShared = true) }
            .toList()
        Log.i(TAG, "loadExtensions: ${sharedExtPkgs.size} shared extension(s) passed isPackageAnExtension() filter")

        val privateExtDir = getPrivateExtensionDir(context)
        val privateFiles = privateExtDir.listFiles().orEmpty()
        Log.i(TAG, "loadExtensions: private ext dir ${privateExtDir.absolutePath} has ${privateFiles.size} file(s)")
        val privateExtPkgs = privateFiles.asSequence()
            .filter { it.isFile && it.extension == PRIVATE_EXTENSION_EXTENSION }
            .mapNotNull { apkFile ->
                try {
                    pm.getPackageArchiveInfo(apkFile.absolutePath, PACKAGE_FLAGS)
                        ?.apply { applicationInfo!!.fixBasePaths(apkFile.absolutePath) }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read private extension file ${apkFile.absolutePath}", e)
                    null
                }
            }
            .filter { isPackageAnExtension(it) }
            .map { AnimeExtensionInfo(it, isShared = false) }
            .toList()
        Log.i(TAG, "loadExtensions: ${privateExtPkgs.size} private extension(s) passed filter")

        // De-dup: if a package exists in BOTH shared and private forms, pick
        // whichever has the higher versionCode (matches Aniyomi behaviour).
        val extPkgs = (sharedExtPkgs.asSequence() + privateExtPkgs.asSequence())
            .groupBy { it.packageInfo.packageName }
            .mapValues { (_, infos) -> infos.maxByOrNull { it.packageInfo.longVersionCode }!! }
            .values
            .toList()
        Log.i(TAG, "loadExtensions: ${extPkgs.size} unique extension package(s) to load: ${extPkgs.map { it.packageInfo.packageName }}")

        return runBlocking {
            coroutineScope {
                extPkgs.map { async { loadExtension(context, it) } }.awaitAll()
            }
        }.also { results ->
            Log.i(TAG, "loadExtensions: DONE — ${results.size} result(s): " +
                    "${results.count { it is AnimeLoadResult.Success }} ok, " +
                    "${results.count { it is AnimeLoadResult.Untrusted }} untrusted, " +
                    "${results.count { it is AnimeLoadResult.Error }} errored")
        }
    }

    /**
     * Load a single extension by package name. Useful for re-loading an
     * extension after the user trusts it (see `AnimeExtensionManager.trust()`).
     */
    suspend fun loadExtensionFromPkgName(
        context: Context,
        pkgName: String,
    ): AnimeLoadResult {
        val pm = context.packageManager
        val pkgInfo = try {
            getPackageInfo(pm, pkgName)
        } catch (e: Exception) {
            return AnimeLoadResult.Error
        }
        val info = AnimeExtensionInfo(pkgInfo, isShared = true)
        return loadExtension(context, info)
    }

    // ─── Core loader ─────────────────────────────────────────────────────────

    private suspend fun loadExtension(
        context: Context,
        extensionInfo: AnimeExtensionInfo,
    ): AnimeLoadResult {
        val pkgInfo = extensionInfo.packageInfo
        val appInfo = pkgInfo.applicationInfo ?: return AnimeLoadResult.Error.also {
            Log.w(TAG, "loadExtension: ${pkgInfo.packageName} has no ApplicationInfo")
        }
        val pm = context.packageManager
        Log.d(TAG, "loadExtension: pkg=${pkgInfo.packageName} versionName=${pkgInfo.versionName} versionCode=${pkgInfo.longVersionCode} isShared=${extensionInfo.isShared}")

        // ── 1. Read lib version from manifest meta-data ──────────────────────
        //   Aniyomi extensions put <meta-data android:name="aniyomix.extensionLib"
        //   android:value="16"/> in their AndroidManifest. If it's missing, fall
        //   back to deriving from the versionName's first segment (older exts).
        val libVersion = appInfo.metaData?.getInt(METADATA_ANIYOMIX_EXTENSION_LIB)
            ?.takeUnless { it == 0 }?.toString()?.toDoubleOrNull()
            ?: pkgInfo.versionName?.substringBeforeLast('.')?.toDoubleOrNull()
            ?: 1.0
        Log.d(TAG, "loadExtension: ${pkgInfo.packageName} libVersion=$libVersion (supported=$SUPPORTED_LIB_VERSIONS)")
        if (libVersion !in SUPPORTED_LIB_VERSIONS) {
            Log.w(TAG, "loadExtension: ${pkgInfo.packageName} has unsupported libVersion=$libVersion — returning Error")
            return AnimeLoadResult.Error
        }

        // ── 2. Read signing certificate(s) and check trust ───────────────────
        val signatures = getSignatures(pkgInfo)
        if (signatures.isNullOrEmpty()) {
            Log.w(TAG, "loadExtension: ${pkgInfo.packageName} has no signatures — returning Error")
            return AnimeLoadResult.Error
        }
        val signatureHash = hashSignatures(signatures)
        val trustExtension = TrustAnimeExtension.get(context)
        val isTrusted = trustExtension.isTrusted(pkgInfo, signatures)
        Log.d(TAG, "loadExtension: ${pkgInfo.packageName} isTrusted=$isTrusted signatureHash=$signatureHash")
        if (!isTrusted) {
            // Don't load untrusted extension code — return Untrusted so the
            // UI can prompt the user to trust or uninstall.
            val extName = readExtensionName(appInfo, pm) ?: pkgInfo.packageName
            val extension = AnimeExtension.Untrusted(
                name = extName,
                pkgName = pkgInfo.packageName,
                versionName = pkgInfo.versionName ?: "",
                versionCode = pkgInfo.longVersionCode,
                libVersion = libVersion,
                lang = null,
                isNsfw = isNsfw(appInfo),
                isTorrent = isTorrent(appInfo),
                signatureHash = signatureHash,
                icon = try { appInfo.loadIcon(pm) } catch (_: Exception) { null },
            )
            Log.w(TAG, "loadExtension: ${pkgInfo.packageName} returning Untrusted — user must trust via Extensions UI")
            return AnimeLoadResult.Untrusted(extension)
        }

        // ── 3. Class-load the extension APK ──────────────────────────────────
        val sourceDir = appInfo.sourceDir
        val classLoader = try {
            loaderCache.getOrPut(sourceDir) {
                ChildFirstPathClassLoader(sourceDir, appInfo.nativeLibraryDir, context.classLoader)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadExtension: Failed to create ClassLoader for ${pkgInfo.packageName}", e)
            return AnimeLoadResult.Error
        }

        // ── 4. Register per-source SharedPreferences (so ConfigurableAnimeSource works) ─
        //   Aniyomi keys prefs by `source_$id`; we mirror that. But because we
        //   haven't instantiated the source yet (so we don't know the id),
        //   we also register a package-level fallback keyed by `source_$pkgName`
        //   that extensions compiled against Tensei's older Extension API
        //   expect.
        try {
            Injekt.addSingleton(
                fullType<android.content.SharedPreferences>(),
                context.getSharedPreferences(
                    "source_${pkgInfo.packageName}",
                    Context.MODE_PRIVATE,
                ),
            )
        } catch (_: Throwable) {
            // Injekt's addSingleton may throw if the type is already
            // registered — that's fine, we just keep the existing one.
        }

        // ── 5. Instantiate the source class(es) ──────────────────────────────
        val sourceClass = appInfo.metaData?.getString(METADATA_ANIME_SOURCE_CLASS)
            ?: appInfo.metaData?.getString(METADATA_SOURCE_CLASS)
            ?: appInfo.metaData?.getString(METADATA_SOURCE_FACTORY)
        if (sourceClass.isNullOrBlank()) {
            Log.w(TAG, "loadExtension: ${pkgInfo.packageName} has no source class metadata — returning Error")
            return AnimeLoadResult.Error
        }
        Log.d(TAG, "loadExtension: ${pkgInfo.packageName} sourceClass=$sourceClass")

        val sources: List<AnimeSource> = try {
            sourceClass.split(";").map { it.trim() }.filter { it.isNotEmpty() }.flatMap {
                val className = if (it.startsWith(".")) "${pkgInfo.packageName}$it" else it
                Log.d(TAG, "loadExtension: loading class $className …")
                val clazz = classLoader.loadClass(className)
                when (val obj = clazz.getDeclaredConstructor().newInstance()) {
                    is AnimeSource -> listOf(obj)
                    is AnimeSourceFactory -> obj.createSources()
                    else -> {
                        Log.w(TAG, "loadExtension: $className is neither AnimeSource nor AnimeSourceFactory")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate source class from ${pkgInfo.packageName}", e)
            return AnimeLoadResult.Error
        }
        if (sources.isEmpty()) {
            return AnimeLoadResult.Error
        }

        // ── 6. Build the Installed extension descriptor ───────────────────────
        val extName = readExtensionName(appInfo, pm) ?: pkgInfo.packageName
        val extension = AnimeExtension.Installed(
            name = extName,
            pkgName = pkgInfo.packageName,
            versionName = pkgInfo.versionName ?: "",
            versionCode = pkgInfo.longVersionCode,
            libVersion = libVersion,
            lang = sources.first().lang.ifBlank { "all" },
            isNsfw = isNsfw(appInfo),
            isTorrent = isTorrent(appInfo),
            pkgFactory = appInfo.metaData?.getString(METADATA_SOURCE_FACTORY),
            sources = sources,
            icon = try { appInfo.loadIcon(pm) } catch (_: Exception) { null },
            isShared = extensionInfo.isShared,
        )
        Log.i(TAG, "loadExtension: SUCCESS ${pkgInfo.packageName} → ${sources.size} source(s): ${sources.map { "${it.name} (${it.lang})" }}")
        return AnimeLoadResult.Success(extension)
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private data class AnimeExtensionInfo(
        val packageInfo: PackageInfo,
        val isShared: Boolean,
    )

    /** File where private `.ext` APKs are stored. */
    private fun getPrivateExtensionDir(context: Context): File =
        File(context.filesDir, PRIVATE_EXTENSION_EXTENSION).also {
            if (!it.exists()) it.mkdirs()
        }

    /**
     * Install a private extension file by copying the APK into `filesDir/exts/`.
     * Mirrors Aniyomi's `AnimeExtensionLoader.installPrivateExtensionFile`.
     *
     * Returns `true` if the file was successfully written, `false` otherwise.
     * The caller is expected to then re-call `loadExtensions(context)` so the
     * new private extension is picked up.
     */
    fun installPrivateExtensionFile(context: Context, tempApkFile: File): Boolean {
        val destDir = getPrivateExtensionDir(context)
        if (!destDir.exists() && !destDir.mkdirs()) return false
        val destFile = File(destDir, "${tempApkFile.nameWithoutExtension}.$PRIVATE_EXTENSION_EXTENSION")
        return try {
            tempApkFile.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install private extension from ${tempApkFile.absolutePath}", e)
            false
        }
    }

    /**
     * Remove a private extension file. No-op if the extension is shared
     * (system-installed) — in that case, the system package manager owns it.
     */
    fun uninstallPrivateExtensionFile(context: Context, pkgName: String): Boolean {
        val destDir = getPrivateExtensionDir(context)
        val target = File(destDir, "$pkgName.$PRIVATE_EXTENSION_EXTENSION")
        return target.exists() && target.delete()
    }

    /** Quick test: does this PackageInfo declare the aniyomi extension feature? */
    private fun isPackageAnExtension(pkgInfo: PackageInfo): Boolean {
        // Blissless/Tensei-specific package-name patterns (legacy).
        val pkgName = pkgInfo.packageName
        val isBlisslessExtension =
            (pkgName.startsWith("com.blissless.") && pkgName.endsWith(".anime.stream")) ||
            (pkgName.startsWith("com.blissless.") && pkgName.endsWith(".anime.torrent")) ||
            (pkgName.startsWith("com.blissless.") && pkgName.endsWith(".manga"))
        if (isBlisslessExtension) return true

        // Aniyomi-style: declared via <uses-feature android:name="tachiyomi.animeextension">.
        val features = pkgInfo.reqFeatures.orEmpty().map { it.name }.toSet()
        if (ANIME_EXTENSION_FEATURE in features) return true

        // Fall back to meta-data presence (older extensions may not declare a feature).
        val metaData = pkgInfo.applicationInfo?.metaData
        if (metaData?.containsKey(METADATA_ANIME_SOURCE_CLASS) == true) return true
        if (metaData?.containsKey(METADATA_SOURCE_FACTORY) == true) return true
        if (metaData?.containsKey(METADATA_SOURCE_CLASS) == true) return true

        return false
    }

    private fun readExtensionName(
        appInfo: ApplicationInfo,
        pm: PackageManager,
    ): String? {
        val metaDataName = appInfo.metaData?.getString(METADATA_ANIYOMIX_NAME)
        if (!metaDataName.isNullOrBlank()) return metaDataName
        return try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun isNsfw(appInfo: ApplicationInfo): Boolean {
        val md = appInfo.metaData ?: return false
        return isMetadataTrue(md, METADATA_ANIME_NSFW) || isMetadataTrue(md, METADATA_NSFW)
    }

    private fun isTorrent(appInfo: ApplicationInfo): Boolean =
        isMetadataTrue(appInfo.metaData, METADATA_ANIYOMIX_TORRENT)

    private fun isMetadataTrue(metaData: android.os.Bundle?, key: String): Boolean {
        if (metaData == null) return false
        @Suppress("DEPRECATION")
        return when (val value = metaData.get(key)) {
            is Boolean -> value
            is Int -> value != 0
            is String -> value.toBooleanStrictOrNull() ?: false
            else -> false
        }
    }

    @Suppress("DEPRECATION")
    private fun getSignatures(pkgInfo: PackageInfo): Array<Signature>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = pkgInfo.signingInfo
            if (signing == null) return null
            // Always pull the most recent signing certificate set.
            signing.apkContentsSigners
        } else {
            pkgInfo.signatures
        }
    }

    private fun hashSignatures(signatures: Array<Signature>): String {
        val md = MessageDigest.getInstance("SHA-256")
        signatures.forEach { sig -> md.update(sig.toByteArray()) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun getInstalledPackages(pm: PackageManager): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pm.getInstalledPackages(PACKAGE_FLAGS)
        }
    }

    private fun getPackageInfo(pm: PackageManager, pkgName: String): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pm.getPackageInfo(pkgName, PACKAGE_FLAGS)
        }
    }

    /**
     * For private `.ext` APKs loaded via `PackageManager.getPackageArchiveInfo`,
     * the returned ApplicationInfo has its `sourceDir` and `nativeLibraryDir`
     * pointing at the archive path — but with a `file://` prefix and no
     * proper zip-alignment flags. This fixes those paths so `PathClassLoader`
     * can read them.
     *
     * Mirrors Aniyomi's `ApplicationInfo.fixBasePaths(...)` extension.
     */
    private fun ApplicationInfo.fixBasePaths(apkPath: String) {
        // Set sourceDir to the actual file path (no file:// prefix).
        sourceDir = apkPath
        // nativeLibraryDir: derive from the APK's lib/ directory if present.
        // For most extensions, the native libraries live next to the APK.
        val parentLib = File(apkPath).parentFile?.let { File(it, "lib") }
        if (parentLib != null && parentLib.exists()) {
            nativeLibraryDir = parentLib.absolutePath
        }
    }
}
