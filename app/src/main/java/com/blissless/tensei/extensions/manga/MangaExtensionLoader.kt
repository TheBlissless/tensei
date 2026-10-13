package com.blissless.tensei.extensions.manga

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.blissless.tensei.stream.ChildFirstPathClassLoader
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads Mihon-style manga extension APKs, either installed as regular packages (shared
 * extensions) or copied into the app's private `filesDir/exts` directory (private extensions).
 *
 * Signature/trust checks are intentionally skipped: this is a barebone host. Extensions are
 * loaded against the host's vendored `eu.kanade.tachiyomi.source` API via a hybrid
 * parent-first classloader (see [ChildFirstPathClassLoader]).
 */
object MangaExtensionLoader {

    private const val TAG = "MangaExtLoader"

    private const val EXTENSION_FEATURE = "tachiyomi.extension"
    private const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
    private const val METADATA_SOURCE_FACTORY = "tachiyomi.extension.factory"
    private const val METADATA_NSFW = "tachiyomi.extension.nsfw"
    private const val METADATA_NAME = "tachiyomix.name"
    private const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
    private const val METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"
    private const val PRIVATE_EXTENSION_EXTENSION = "apk"

    private val SUPPORTED_LIB_VERSIONS = listOf(1.4, 1.6)

    @Suppress("DEPRECATION")
    private val PACKAGE_FLAGS: Int = PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA

    /**
     * Parent-first packages for the manga extension classloader. In addition to the anime
     * loader's defaults, `eu.kanade.tachiyomi.source.` must resolve to the HOST's vendored
     * copy so class identities match between host interfaces and extension overrides.
     */
    private val PARENT_FIRST_PACKAGES: Set<String> =
        ChildFirstPathClassLoader.DEFAULT_PARENT_FIRST_PACKAGES + "eu.kanade.tachiyomi.source."

    private val loaderCache = ConcurrentHashMap<String, ChildFirstPathClassLoader>()

    fun privateExtensionDir(context: Context): File =
        File(context.filesDir, "exts").also { if (!it.exists()) it.mkdirs() }

    fun isExtension(pkgInfo: PackageInfo): Boolean {
        val pkgName = pkgInfo.packageName
        // Never treat the app's own Blissless/Aniyomi packages as Mihon manga extensions.
        if (pkgName.endsWith(".anime.stream") || pkgName.endsWith(".anime.torrent")) return false

        if (pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }) return true
        val metaData = pkgInfo.applicationInfo?.metaData
        if (metaData?.containsKey(METADATA_SOURCE_CLASS) == true) return true
        if (metaData?.containsKey(METADATA_SOURCE_FACTORY) == true) return true
        // Legacy extensions built before the <uses-feature> switch only match by package prefix.
        return pkgName.startsWith("eu.kanade.tachiyomi.extension")
    }

    fun archivePackageInfo(context: Context, file: File): PackageInfo? {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)
        info?.applicationInfo?.let { app ->
            app.sourceDir = file.absolutePath
            app.publicSourceDir = file.absolutePath
        }
        return info
    }

    data class LoadResult(
        val extensions: List<LoadedExtension>,
        val errors: List<String>,
    )

    @Synchronized
    fun loadAll(context: Context): LoadResult {
        val pkgManager = context.packageManager

        @Suppress("DEPRECATION")
        val allPkgs = pkgManager.getInstalledPackages(PACKAGE_FLAGS)

        val sharedPkgs = allPkgs.asSequence()
            .filter { isExtension(it) }
            .map { it to true }

        val privatePkgs = privateExtensionDir(context)
            .listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == PRIVATE_EXTENSION_EXTENSION }
            ?.mapNotNull { file ->
                // Android 14+ refuses to read extension apks that aren't read-only.
                if (file.canWrite()) file.setReadOnly()
                archivePackageInfo(context, file)?.takeIf { isExtension(it) }
            }
            ?.map { it to false }
            ?: emptySequence()

        val candidates = (sharedPkgs + privatePkgs)
            .groupBy { it.first.packageName }
            .mapNotNull { (_, versions) ->
                versions.maxByOrNull { PackageInfoCompat.getLongVersionCode(it.first) }
            }

        val extensions = mutableListOf<LoadedExtension>()
        val errors = mutableListOf<String>()

        for ((pkgInfo, isShared) in candidates) {
            try {
                extensions += load(context, pkgInfo, isShared)
            } catch (e: Throwable) {
                Log.e(TAG, "load failed ${pkgInfo.packageName}: ${e.message}", e)
                errors += "${pkgInfo.packageName}: ${e.message ?: e.javaClass.simpleName}"
            }
        }

        return LoadResult(
            extensions = extensions.sortedBy { it.name.lowercase() },
            errors = errors,
        )
    }

    private fun load(context: Context, pkgInfo: PackageInfo, isShared: Boolean): LoadedExtension {
        val appInfo = pkgInfo.applicationInfo
            ?: throw IllegalArgumentException("missing application info")
        val metaData = appInfo.metaData
            ?: throw IllegalArgumentException("missing metadata")
        val pkgName = pkgInfo.packageName

        val label = runCatching { context.packageManager.getApplicationLabel(appInfo).toString() }
            .getOrNull()
        val name = metaData.getString(METADATA_NAME)
            ?: label?.substringAfter("Tachiyomi: ")
            ?: pkgName
        val versionName = pkgInfo.versionName.orEmpty().ifEmpty {
            throw IllegalArgumentException("missing versionName")
        }
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)

        val libVersion = metaData.getFloat(METADATA_EXTENSION_LIB)
            .takeUnless { it == 0.0f }
            ?.toString()
            ?.toDouble()
            ?: versionName.substringBeforeLast('.').toDoubleOrNull()
            ?: throw IllegalArgumentException("unknown lib version")
        if (libVersion !in SUPPORTED_LIB_VERSIONS) {
            throw IllegalArgumentException("unsupported lib version $libVersion")
        }

        val sourceClasses = metaData.getString(METADATA_SOURCE_CLASS)
            ?.takeIf { it.isNotBlank() }
            ?: metaData.getString(METADATA_SOURCE_FACTORY)
                ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("missing source class")

        val sourceDir = appInfo.sourceDir
            ?: throw IllegalArgumentException("missing sourceDir")
        val classLoader = loaderCache.getOrPut(sourceDir) {
            ChildFirstPathClassLoader(
                sourceDir,
                appInfo.nativeLibraryDir,
                context.classLoader,
                PARENT_FIRST_PACKAGES,
            )
        }

        val sources = sourceClasses.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { className ->
                val fqn = if (className.startsWith(".")) pkgName + className else className
                val instance = classLoader.loadClass(fqn).getDeclaredConstructor().newInstance()
                when (instance) {
                    is Source -> listOf(instance)
                    is SourceFactory -> instance.createSources()
                    else -> throw IllegalArgumentException("unknown source type: ${instance.javaClass.name}")
                }
            }
        if (sources.isEmpty()) throw IllegalArgumentException("no sources")

        val contentWarning = metaData.getInt(METADATA_CONTENT_WARNING)
            .takeIf { it != 0 }
            ?: metaData.getInt(METADATA_NSFW)
        val lang = sources.map { it.lang }.toSet().let { langs ->
            when (langs.size) {
                0 -> ""
                1 -> langs.first()
                else -> "all"
            }
        }
        val icon = runCatching { appInfo.loadIcon(context.packageManager) }.getOrNull()

        return LoadedExtension(
            pkgName = pkgName,
            name = name,
            versionName = versionName,
            versionCode = versionCode,
            libVersion = libVersion,
            lang = lang,
            nsfw = contentWarning != 0,
            isShared = isShared,
            icon = icon,
            sources = sources,
        )
    }
}
