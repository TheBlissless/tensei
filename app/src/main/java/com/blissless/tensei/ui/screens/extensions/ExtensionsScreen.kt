package com.blissless.tensei.ui.screens.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.tensei.R
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.util.longToast
import com.blissless.tensei.util.toast
import com.blissless.tensei.extensions.manga.AvailableExtension
import com.blissless.tensei.extensions.manga.LoadedExtension
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import kotlinx.coroutines.launch
import com.blissless.tensei.extensions.Extension
import com.blissless.tensei.extensions.anime.AnimeExtensionInstaller
import com.blissless.tensei.extensions.repo.ExtensionType
import com.blissless.tensei.extensions.repo.RepoKind
import com.blissless.tensei.viewmodel.ExtensionsViewModel
import com.blissless.tensei.viewmodel.RepoState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: ExtensionsViewModel = viewModel(),
    selectedRepoUrl: String? = null,
    onSelectRepo: (String?) -> Unit = {},
    onBrowseChanged: ((Boolean) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Mihon-compatible manga extensions live on the same page behind a tab, so there is a
    // single place to manage both anime and manga extensions (and their repositories).
    val mangaManager = remember { MangaExtensionManager.get(context) }
    val mangaExtensions by mangaManager.extensions.collectAsState()
    val mangaRepos by mangaManager.repos.collectAsState()
    val mangaLoadErrors by mangaManager.loadErrors.collectAsState()
    val mangaBusy by mangaManager.busy.collectAsState()
    val mangaMessage by mangaManager.message.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var repoPendingRemoval by remember { mutableStateOf<RepoRow?>(null) }
    var extensionPendingUninstall by remember { mutableStateOf<Extension?>(null) }
    var isDetectingRepoKind by remember { mutableStateOf(false) }
    var repoKindError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        mangaManager.fetchAvailable()
    }

    val installedPackages = uiState.extensions.map { it.packageName }.toSet()
    val installedNames = uiState.extensions.map { it.name }.toSet()
    val installedPackageVersions = uiState.extensions.associate {
        it.packageName to "v${it.versionName} (code ${it.versionCode})"
    }
    val installedNameVersions = uiState.extensions.associate {
        it.name.lowercase() to "v${it.versionName} (code ${it.versionCode})"
    }
    val updatableCount = uiState.updatablePackageNames.size

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { (message, duration) ->
            if (duration == android.widget.Toast.LENGTH_LONG) context.longToast(message)
            else context.toast(message)
        }
    }

    LaunchedEffect(uiState.refreshMessage) {
        uiState.refreshMessage?.let {
            context.toast(it)
            viewModel.clearRefreshMessage()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadExtensions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedRepoUrl) {
        onBrowseChanged?.invoke(selectedRepoUrl != null)
    }

    BackHandler(enabled = selectedRepoUrl != null) {
        onSelectRepo(null)
    }

    val selectedRepoState = selectedRepoUrl?.let { url ->
        uiState.repos.find { it.url == url }
    }

    if (selectedRepoState != null) {
        ExtensionBrowserScreen(
            repoState = selectedRepoState,
            installedPackages = installedPackages,
            installedNames = installedNames,
            updatablePackageNames = uiState.updatablePackageNames,
            updatableNames = uiState.updatableNames,
            installedPackageVersions = installedPackageVersions,
            installedNameVersions = installedNameVersions,
            onInstall = { viewModel.installExtension(it) },
            onBack = { onSelectRepo(null) },
            onRemoveRepo = { url -> viewModel.removeRepo(url) }
        )
        return
    }

    val mangaRepoSet = remember(mangaRepos) { mangaRepos.toSet() }

    // Anime + manga extensions merged into a single alphabetical list. Each row
    // carries a small kind badge so the two ecosystems are distinguishable even
    // though they now share one list. An extension that both sides know about (a
    // Tensei manga extension is detected as an installed app AND loadable as a manga
    // extension) is listed once, as the manga extension it really is.
    val installedItems = remember(uiState.extensions, mangaExtensions) {
        val mangaPackages = mangaExtensions.map { it.pkgName }.toSet()
        buildList {
            uiState.extensions
                .filter { it.packageName !in mangaPackages }
                .forEach { add(InstalledItem.Anime(it)) }
            mangaExtensions.forEach { add(InstalledItem.Manga(it)) }
            sortBy { it.sortName.lowercase() }
        }
    }

    val filteredInstalled = remember(installedItems, searchQuery) {
        if (searchQuery.isBlank()) {
            installedItems
        } else {
            installedItems.filter {
                it.sortName.contains(searchQuery, ignoreCase = true) ||
                    it.pkgName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Anime repositories and manga repositories merged into one list keyed by URL.
    val repoRows = remember(uiState.repos, mangaRepoSet) {
        buildList {
            val seen = mutableSetOf<String>()
            uiState.repos.forEach {
                seen += it.url
                add(RepoRow(url = it.url, anime = it, isManga = it.url in mangaRepoSet))
            }
            mangaRepoSet.filterNot { it in seen }.forEach {
                add(RepoRow(url = it, anime = null, isManga = true))
            }
        }
    }

    val showSkeleton = uiState.isLoading &&
        uiState.extensions.isEmpty() &&
        uiState.repos.isEmpty() &&
        uiState.error == null

    // The host scaffold already applies the horizontal gutter, so this list only
    // owns its vertical rhythm.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Spacing.sm, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        mangaMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                )
            }
        }

        if (mangaBusy) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.extensions_working), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (uiState.error != null) {
            item {
                ExtensionsErrorRow(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadExtensions(true) }
                )
            }
        }

        item {
            ExtensionsGroupHeader(
                title = stringResource(R.string.extensions_repositories),
                count = repoRows.size
            )
        }

        item {
            ExtensionsGroupCard(modifier = if (showSkeleton) Modifier.shimmer() else Modifier) {
                if (showSkeleton) {
                    ExtensionsRowSkeleton()
                    ExtensionsGroupDivider()
                    ExtensionsRowSkeleton()
                } else {
                    repoRows.forEachIndexed { index, row ->
                        ExtensionsRepoRow(
                            row = row,
                            onBrowse = { onSelectRepo(row.url) },
                            onCopyUrl = {
                                copyToClipboard(context, context.getString(R.string.extensions_repo_url), row.url)
                                context.toast(context.getString(R.string.extensions_repo_url_copied))
                            },
                            onRemove = { repoPendingRemoval = row }
                        )
                        if (index < repoRows.lastIndex) ExtensionsGroupDivider()
                    }
                    if (repoRows.isNotEmpty()) ExtensionsGroupDivider()
                    ExtensionsAddRepoRow(onClick = { showAddRepoDialog = true })
                }
            }
        }

        item {
            ExtensionsGroupHeader(
                title = stringResource(R.string.extensions_installed),
                count = installedItems.size,
                actionLabel = if (updatableCount > 0) stringResource(R.string.extensions_update_all) else null,
                actionIcon = Icons.Default.Download,
                actionProgress = uiState.isUpdatingAll,
                actionEnabled = !uiState.isUpdatingAll,
                onAction = { viewModel.updateAllExtensions() }
            )
        }

        // Always available as soon as anything is installed, and keyed so the
        // text field is never torn down and rebuilt by the list recomposition
        // that each keystroke causes. Without the key the field could drop the
        // first characters after an update check landed mid-typing.
        if (!showSkeleton && installedItems.isNotEmpty()) {
            item(key = EXTENSIONS_SEARCH_KEY) {
                ExtensionsSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }
        }

        item {
            ExtensionsGroupCard(modifier = if (showSkeleton) Modifier.shimmer() else Modifier) {
                when {
                    showSkeleton -> {
                        repeat(3) {
                            if (it > 0) ExtensionsGroupDivider()
                            ExtensionsRowSkeleton()
                        }
                    }

                    filteredInstalled.isEmpty() -> {
                        ExtensionsHintRow(
                            icon = Icons.Default.Extension,
                            title = if (searchQuery.isBlank()) {
                                stringResource(R.string.extensions_no_extensions_installed)
                            } else {
                                stringResource(R.string.extensions_no_matches)
                            },
                            message = if (searchQuery.isBlank()) {
                                stringResource(R.string.extensions_no_extensions_installed_hint)
                            } else {
                                stringResource(R.string.extensions_no_matches_hint, searchQuery)
                            }
                        )
                    }

                    else -> {
                        filteredInstalled.forEachIndexed { index, item ->
                            when (item) {
                                is InstalledItem.Anime -> ExtensionsInstalledRow(
                                    extension = item.ext,
                                    hasUpdate = item.ext.packageName in uiState.updatablePackageNames,
                                    onUpdate = { viewModel.updateExtension(item.ext.packageName) },
                                    onAppInfo = { openAppSettings(context, item.ext.packageName) },
                                    onCopyPackage = {
                                        copyToClipboard(context, context.getString(R.string.extensions_package_name), item.ext.packageName)
                                        context.toast(context.getString(R.string.extensions_package_name_copied))
                                    },
                                    onUninstall = { extensionPendingUninstall = item.ext }
                                )

                                is InstalledItem.Manga -> {
                                    val update = mangaManager.updateFor(item.ext.pkgName)
                                    MangaInstalledRow(
                                        ext = item.ext,
                                        update = update,
                                        busy = mangaBusy,
                                        onUpdate = { update?.let { mangaManager.install(it.pkgName, it.apkUrl) } },
                                        onUninstall = { mangaManager.uninstall(item.ext.pkgName) }
                                    )
                                }
                            }
                            if (index < filteredInstalled.lastIndex) ExtensionsGroupDivider()
                        }
                    }
                }
            }
        }

        if (updatableCount > 0) {
            item {
                Text(
                    text = if (updatableCount == 1) {
                        stringResource(R.string.extensions_updatable_one)
                    } else {
                        stringResource(R.string.extensions_updatable_many, updatableCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }

        if (mangaLoadErrors.isNotEmpty()) {
            item {
                ExtensionsGroupHeader(title = stringResource(R.string.extensions_load_errors), count = mangaLoadErrors.size)
            }
            item {
                ExtensionsGroupCard {
                    mangaLoadErrors.forEach { err ->
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        )
                    }
                }
            }
        }
    }

    if (showAddRepoDialog) {
        ExtensionsAddRepoDialog(
            isDetecting = isDetectingRepoKind,
            error = repoKindError,
            onDismiss = {
                if (!isDetectingRepoKind) {
                    repoKindError = null
                    showAddRepoDialog = false
                }
            },
            onConfirm = { url ->
                scope.launch {
                    isDetectingRepoKind = true
                    repoKindError = null
                    val kind = runCatching { viewModel.detectRepoKind(url) }.getOrNull()
                    isDetectingRepoKind = false
                    if (kind == null) {
                        repoKindError = context.getString(R.string.extensions_detect_repo_failed)
                        return@launch
                    }
                    when (kind) {
                        RepoKind.ANIME -> viewModel.addRepo(url)
                        RepoKind.MANGA -> if (mangaManager.addRepo(url)) {
                            mangaManager.fetchAvailable()
                        }
                    }
                    showAddRepoDialog = false
                }
            }
        )
    }

    repoPendingRemoval?.let { row ->
        ExtensionsConfirmDialog(
            title = stringResource(R.string.extensions_remove_repo_dialog_title),
            message = row.anime?.repo?.name?.let {
                stringResource(R.string.extensions_remove_repo_dialog_msg, it)
            } ?: stringResource(R.string.extensions_remove_repo_dialog_msg_anon),
            confirmLabel = stringResource(R.string.extensions_remove),
            onConfirm = {
                row.anime?.let { viewModel.removeRepo(it.url) }
                if (row.isManga) mangaManager.removeRepo(row.url)
                repoPendingRemoval = null
            },
            onDismiss = { repoPendingRemoval = null }
        )
    }

    extensionPendingUninstall?.let { extension ->
        ExtensionsConfirmDialog(
            title = stringResource(R.string.extensions_uninstall_dialog_title, extension.name),
            message = stringResource(R.string.extensions_uninstall_dialog_msg),
            confirmLabel = stringResource(R.string.extensions_uninstall),
            onConfirm = {
                AnimeExtensionInstaller(context).uninstall(extension.packageName)
                extensionPendingUninstall = null
            },
            onDismiss = { extensionPendingUninstall = null }
        )
    }
}


// --- Group chrome ------------------------------------------------------------

/** Which extension ecosystem a row belongs to. */
internal enum class ExtensionKind(val label: String) {
    ANIME("Anime"),
    MANGA("Manga")
}
