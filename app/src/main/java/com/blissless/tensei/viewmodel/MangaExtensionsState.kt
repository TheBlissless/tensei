package com.blissless.tensei.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.blissless.tensei.mal.MalMangaListEntry
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.manga.MangaDexManager
import com.blissless.tensei.data.manga.MangaRepository
import com.blissless.tensei.data.manga.MangaTrackManager
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import com.blissless.tensei.extensions.manga.MangaSourceApi
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaTrack
import com.blissless.tensei.data.models.chapterBaseNumber
import com.blissless.tensei.data.models.countDistinctChapters
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.json.JSONObject

// Manga extension state: installed-extension models, repository/manager
// accessors, discovery, selection and selection restore.


data class InstalledExtension(val label: String, val packageName: String) {
    val authority: String get() {
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessMangaExtension(packageName)) {
            return packageName.removeSuffix(".manga") + ".provider"
        }
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessStreamExtension(packageName)) {
            return packageName.removeSuffix(".anime.stream") + ".provider"
        }
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessTorrentExtension(packageName)) {
            return packageName.removeSuffix(".anime.torrent") + ".provider"
        }
        return "$packageName.provider"
    }
}


data class ExtensionChapter(
    val number: String,
    val title: String,
    val id: String,
    val index: Int,
    val pageCount: Int
)


// â”€â”€â”€ Managers (lazily initialized from MainViewModel.init) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
var MainViewModel.mangaRepository: MangaRepository?
    get() = _mangaRepository
    set(value) { _mangaRepository = value }

internal var _mangaRepository: MangaRepository? = null


var MainViewModel.mangaTrackManager: MangaTrackManager?
    get() = _mangaTrackManager
    set(value) { _mangaTrackManager = value }

internal var _mangaTrackManager: MangaTrackManager? = null


var MainViewModel.mangaDexManager: MangaDexManager?
    get() = _mangaDexManager
    set(value) { _mangaDexManager = value }

internal var _mangaDexManager: MangaDexManager? = null


// â”€â”€â”€ Extension State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private val _installedExtensions = MutableStateFlow<List<InstalledExtension>>(emptyList())

val MainViewModel.installedExtensions: StateFlow<List<InstalledExtension>> get() = _installedExtensions.asStateFlow()


internal val _selectedExtensionAuthority = MutableStateFlow<String?>(null)

val MainViewModel.selectedExtensionAuthority: StateFlow<String?> get() = _selectedExtensionAuthority.asStateFlow()


// ─── External (Mihon) extension preferences exposed to the UI ──────────────

val MainViewModel.mangaSourceMethod: StateFlow<String> get() = userPreferences.mangaSourceMethod

val MainViewModel.defaultExternalMangaExtension: StateFlow<String?> get() = userPreferences.defaultExternalMangaExtension

val MainViewModel.mangaExtensionLanguages: StateFlow<Map<String, String>> get() = userPreferences.mangaExtensionLanguages


fun MainViewModel.setMangaSourceMethod(method: String) {
    userPreferences.setMangaSourceMethod(method)
    _mangaTotalChapters.value = 0
    mangaTrackManager?.resetTotalChaptersForReleasing()
    loadLocalMangaTracking()
}


fun MainViewModel.setDefaultExternalMangaExtension(pkgName: String?) {
    userPreferences.setDefaultExternalMangaExtension(pkgName)
}


fun MainViewModel.setMangaExtensionLanguage(pkgName: String, language: String) {
    userPreferences.setMangaExtensionLanguage(pkgName, language)
}


/**
 * Discover installed manga extensions. Detects by package name pattern
 * (com.blissless.*.manga) and falls back to beacon labels for backward compat.
 * Safe to call repeatedly.
 */
fun MainViewModel.discoverExtensions() {
    val pm = context.packageManager
    val extensions = mutableListOf<InstalledExtension>()
    val seenPackages = mutableSetOf<String>()

    // 1) Package-name-based detection for *.manga
    val installedPkgs = try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(android.content.pm.PackageManager.PackageInfoFlags.of(
                (android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.GET_CONFIGURATIONS).toLong()
            ))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.GET_CONFIGURATIONS)
        }
    } catch (_: Exception) { emptyList() }
    for (pkg in installedPkgs) {
        val pkgName = pkg.packageName
        if (com.blissless.tensei.extensions.ExtensionDetector.isBlisslessMangaExtension(pkgName)) {
            seenPackages.add(pkgName)
            extensions.add(
                InstalledExtension(
                    label = com.blissless.tensei.extensions.ExtensionDetector.extensionDisplayName(pkgName),
                    packageName = pkgName
                )
            )
        }
    }

    // 2) Beacon fallback (backward compat)
    val beaconIntent = Intent("com.blissless.mangaclient.EXTENSION_BEACON")
    val resolveInfoList = context.packageManager.queryBroadcastReceivers(beaconIntent, 0)
    for (info in resolveInfoList) {
        val pkgName = info.activityInfo.packageName
        if (pkgName in seenPackages) continue
        val label = info.loadLabel(pm).toString()
        if (label.startsWith("Oni: ", ignoreCase = true) ||
            label.startsWith("Tensei: ", ignoreCase = true)) {
            extensions.add(InstalledExtension(label = label, packageName = pkgName))
        }
    }

    _installedExtensions.value = extensions.sortedBy { it.label }
}


/**
 * Select an extension as the active source for chapter lists and image fetching.
 * Persists across app restarts via UserPreferences.
 */
fun MainViewModel.selectExtension(authority: String?) {
    _selectedExtensionAuthority.value = authority
    userPreferences.setSelectedMangaExtensionAuthority(authority)
    _mangaTotalChapters.value = 0
    mangaTrackManager?.resetTotalChaptersForReleasing()
    loadLocalMangaTracking()
}


internal fun MainViewModel.restoreExtensionSelection() {
    val saved = userPreferences.getSelectedMangaExtensionAuthority()
    if (saved != null) {
        // Migrate stale authorities from old package names (e.g. "com.blissless.x.manga.provider" -> "com.blissless.x.provider")
        val migrated = migratedAuthority(saved)
        if (migrated != saved) {
            userPreferences.setSelectedMangaExtensionAuthority(migrated)
        }
        _selectedExtensionAuthority.value = migrated
    }
}


private fun migratedAuthority(saved: String): String {
    if (!saved.endsWith(".provider")) return saved
    val withoutProvider = saved.removeSuffix(".provider")
    if (withoutProvider.endsWith(".manga")) return withoutProvider.removeSuffix(".manga") + ".provider"
    if (withoutProvider.endsWith(".anime.stream")) return withoutProvider.removeSuffix(".anime.stream") + ".provider"
    if (withoutProvider.endsWith(".anime.torrent")) return withoutProvider.removeSuffix(".anime.torrent") + ".provider"
    return saved
}
