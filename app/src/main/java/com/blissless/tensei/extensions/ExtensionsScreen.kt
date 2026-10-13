package com.blissless.tensei.extensions

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import coil.compose.AsyncImage
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

/**
 * Extension management, laid out the way every other extension-based app does it
 * (Mihon/Tachiyomi, Swift, Hasty): a compact screen title owned by the host
 * scaffold, a short searchable list of installed extensions with an inline
 * update action, and a separate group of extension repositories you tap into.
 *
 * What this replaced, and why it read badly:
 *  - The screen drew its own 44dp icon tile + bold headline + refresh button
 *    while the settings scaffold already renders an "Extensions" title bar with
 *    its own refresh action, so every visit showed the title twice.
 *  - It re-applied a 20dp horizontal gutter even though the scaffold already
 *    insets its content, so this page was indented further than every other
 *    settings page.
 *  - Every repository and every extension was its own bordered card with three
 *    competing trailing controls (copy, delete, chevron), which made a list of
 *    plain settings rows look like a stack of promotional tiles.
 *  - Expand/collapse disclosure arrows on repositories and installed items hid
 *    content behind chevrons that had no menu to open.
 *
 * Now: one grouped card per section, one action per row (tap = primary action,
 * overflow = secondary), and an "Add repository" row that reads as a row rather
 * than a floating call-to-action card.
 */
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
    val mangaAvailable by mangaManager.available.collectAsState()
    val mangaAvailableError by mangaManager.availableError.collectAsState()
    val mangaLoadErrors by mangaManager.loadErrors.collectAsState()
    val mangaBusy by mangaManager.busy.collectAsState()
    val mangaMessage by mangaManager.message.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var repoPendingRemoval by remember { mutableStateOf<RepoRow?>(null) }
    var extensionPendingUninstall by remember { mutableStateOf<Extension?>(null) }

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
    // though they now share one list.
    val installedItems = remember(uiState.extensions, mangaExtensions) {
        buildList {
            uiState.extensions.forEach { add(InstalledItem.Anime(it)) }
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

        item {
            ExtensionsGroupHeader(title = stringResource(R.string.extensions_available), count = mangaAvailable?.size ?: 0)
        }
        item {
            ExtensionsGroupCard {
                mangaAvailableError?.let { err ->
                    Text(
                        err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                    )
                }
                val list = mangaAvailable
                when {
                    list == null -> {
                        ExtensionsRowSkeleton()
                    }
                    list.isEmpty() -> {
                        ExtensionsHintRow(
                            icon = Icons.Default.Extension,
                            title = stringResource(R.string.extensions_no_extensions_available),
                            message = stringResource(R.string.extensions_no_extensions_available_hint)
                        )
                    }
                    else -> {
                        val installedPkgs = mangaExtensions.map { it.pkgName }.toSet()
                        list.forEachIndexed { index, avail ->
                            MangaAvailableRow(
                                ext = avail,
                                installed = avail.pkgName in installedPkgs,
                                busy = mangaBusy,
                                onInstall = { mangaManager.install(avail.pkgName, avail.apkUrl) }
                            )
                            if (index < list.lastIndex) ExtensionsGroupDivider()
                        }
                    }
                }
            }
        }
    }

    if (showAddRepoDialog) {
        ExtensionsAddRepoDialog(
            onDismiss = { showAddRepoDialog = false },
            onConfirm = { url, kind ->
                when (kind) {
                    ExtensionKind.ANIME -> viewModel.addRepo(url)
                    ExtensionKind.MANGA -> if (mangaManager.addRepo(url)) {
                        mangaManager.fetchAvailable()
                    }
                }
                showAddRepoDialog = false
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
private enum class ExtensionKind(val label: String) {
    ANIME("Anime"),
    MANGA("Manga")
}

/** A unified installed-extension entry, either an Aniyomi anime ext or a Mihon manga ext. */
private sealed interface InstalledItem {
    val sortName: String
    val pkgName: String

    data class Anime(val ext: Extension) : InstalledItem {
        override val sortName get() = ext.name
        override val pkgName get() = ext.packageName
    }

    data class Manga(val ext: LoadedExtension) : InstalledItem {
        override val sortName get() = ext.name
        override val pkgName get() = ext.pkgName
    }
}

/** A repository row, combining an anime repo state (if any) with the manga repo listing. */
private data class RepoRow(
    val url: String,
    val anime: RepoState?,
    val isManga: Boolean,
)

@Composable
private fun MangaInstalledRow(
    ext: LoadedExtension,
    update: AvailableExtension?,
    busy: Boolean,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val iconBitmap = remember(ext.pkgName, ext.icon) {
        ext.icon?.toBitmap(64, 64)
    }

    ExtensionsRow(
        title = ext.name,
        subtitle = buildString {
            append("v${ext.versionName}")
            append(" (${ext.versionCode})")
            if (ext.lang.isNotBlank()) append(" - ${ext.lang}")
        },
        badge = if (ext.nsfw) "NSFW" else null,
        kindLabel = ExtensionKind.MANGA.label,
        leading = {
            if (iconBitmap != null) {
                Image(
                    painter = BitmapPainter(iconBitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(Radius.controlShape)
                )
            } else {
                ExtensionsIconTile(
                    icon = Icons.Default.Extension,
                    tint = scheme.primary,
                    containerColor = scheme.primary.copy(alpha = 0.08f)
                )
            }
        },
        trailing = {
            if (update != null) {
                FilledTonalButton(
                    onClick = onUpdate,
                    enabled = !busy,
                    shape = Radius.chipShape,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.extensions_update), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(Spacing.xs))
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(Sizes.iconButton)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.extensions_options, ext.name),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_uninstall), color = scheme.error) },
                        onClick = {
                            menuExpanded = false
                            onUninstall()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = scheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MangaAvailableRow(
    ext: AvailableExtension,
    installed: Boolean,
    busy: Boolean,
    onInstall: () -> Unit
) {
    ExtensionsRow(
        title = ext.name,
        subtitle = buildString {
            append("v${ext.versionName}")
            append(" (${ext.versionCode})")
            if (ext.lang.isNotBlank()) append(" - ${ext.lang}")
        },
        kindLabel = ExtensionKind.MANGA.label,
        leading = {
            if (ext.iconUrl.isNotBlank()) {
                AsyncImage(
                    model = ext.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(Radius.controlShape)
                )
            } else {
                ExtensionsIconTile(
                    icon = Icons.Default.Extension,
                    tint = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            }
        },
        trailing = {
            FilledTonalButton(
                onClick = onInstall,
                enabled = !busy,
                shape = Radius.chipShape,
                contentPadding = PaddingValues(horizontal = Spacing.sm),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    if (installed) stringResource(R.string.extensions_reinstall) else stringResource(R.string.extensions_install),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    )
}

/**
 * Section label with an optional trailing action, matching the label style the
 * settings screen already uses so this page doesn't invent a fourth one.
 */
@Composable
private fun ExtensionsGroupHeader(
    title: String,
    count: Int,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    actionEnabled: Boolean = true,
    actionProgress: Boolean = false,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.xs, end = Spacing.xs, top = Spacing.lg, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        if (actionLabel == null) {
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                enabled = actionEnabled,
                contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs)
            ) {
                if (actionProgress) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                } else if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(Spacing.xs))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * One card per section, rows separated by hairlines.
 *
 * The container colour matches `SettingsCard` so extensions pages read as part
 * of Settings rather than as a separate product.
 */
@Composable
private fun ExtensionsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = Radius.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.xxs), content = content)
    }
}

/** Hairline between rows, inset so it starts after the leading icon. */
@Composable
private fun ExtensionsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
        thickness = 0.5.dp
    )
}

// --- Rows --------------------------------------------------------------------

/**
 * The single row shape used by every entry on this screen.
 *
 * [leading] is a 40dp slot so all rows line up regardless of whether they show
 * an extension icon, a folder, or a hint, and only one control is ever exposed
 * directly: secondary actions live behind [trailing] overflow.
 */
@Composable
private fun ExtensionsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    subtitleMaxLines: Int = 1,
    badge: String? = null,
    kindLabel: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (kindLabel != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = Radius.chipShape
                    ) {
                        Text(
                            text = kindLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                if (badge != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = Radius.chipShape
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor
                        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = subtitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing()
    }
}

/** Rounded tile that hosts a row's leading glyph. */
@Composable
private fun ExtensionsIconTile(
    icon: ImageVector,
    tint: Color,
    containerColor: Color
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(Radius.controlShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ExtensionsRepoRow(
    row: RepoRow,
    onBrowse: () -> Unit,
    onCopyUrl: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val animeState = row.anime
    val repo = animeState?.repo
    val isReady = repo != null

    ExtensionsRow(
        title = repo?.name ?: row.url,
        subtitle = if (animeState?.error != null) {
            animeState.error
        } else if (animeState?.isLoading == true) {
            stringResource(R.string.extensions_loading)
        } else if (isReady) {
            repo.description.takeIf { it.isNotBlank() } ?: stringResource(R.string.extensions_available_count, repo.extensions.size)
        } else if (row.isManga) {
            stringResource(R.string.extensions_manga_repo_subtitle)
        } else {
            stringResource(R.string.extensions_tap_to_retry)
        },
        subtitleColor = if (animeState?.error != null) scheme.error else null,
        kindLabel = if (row.anime != null) ExtensionKind.ANIME.label else ExtensionKind.MANGA.label,
        onClick = if (isReady) onBrowse else null,
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(Radius.controlShape)
                    .background(
                        if (isReady) {
                            scheme.tertiary.copy(alpha = 0.1f)
                        } else {
                            scheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (animeState?.isLoading == true) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = if (isReady) {
                            scheme.tertiary
                        } else {
                            scheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        trailing = {
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(Sizes.iconButton)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.extensions_repo_options),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_copy_url)) },
                        onClick = {
                            menuExpanded = false
                            onCopyUrl()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_remove), color = scheme.error) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = scheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ExtensionsAddRepoRow(onClick: () -> Unit) {
    ExtensionsRow(
        title = stringResource(R.string.extensions_add_repo),
        subtitle = stringResource(R.string.extensions_add_repo_subtitle),
        subtitleMaxLines = 2,
        onClick = onClick,
        leading = {
            ExtensionsIconTile(
                icon = Icons.Default.Add,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        },
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun ExtensionsInstalledRow(
    extension: Extension,
    hasUpdate: Boolean,
    onUpdate: () -> Unit,
    onAppInfo: () -> Unit,
    onCopyPackage: () -> Unit,
    onUninstall: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val iconBitmap = remember(extension.packageName) {
        extension.icon?.toBitmap(64, 64)
    }
    val languages = remember(extension.packageName) {
        extension.sources
            .map { it.lang.uppercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
    }
    val sourceCount = extension.sources.size
    val subtitle = buildString {
        append("v${extension.versionName} (${extension.versionCode})")
        if (sourceCount > 0) {
            append(" - ")
            append(
                if (sourceCount == 1) stringResource(R.string.extensions_source_count_one)
                else stringResource(R.string.extensions_source_count_many, sourceCount)
            )
        }
        if (languages.isNotEmpty()) {
            append(" - ${languages.joinToString(", ")}")
        }
    }

    ExtensionsRow(
        title = extension.name,
        subtitle = subtitle,
        badge = if (extension.isNsfw) "NSFW" else null,
        kindLabel = ExtensionKind.ANIME.label,
        onClick = onAppInfo,
        leading = {
            if (iconBitmap != null) {
                Image(
                    painter = BitmapPainter(iconBitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(Radius.controlShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(Radius.controlShape)
                        .background(scheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = extension.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                }
            }
        },
        trailing = {
            if (hasUpdate) {
                FilledTonalButton(
                    onClick = onUpdate,
                    shape = Radius.chipShape,
                    contentPadding = PaddingValues(horizontal = Spacing.sm),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(R.string.extensions_update), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(Spacing.xs))
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(Sizes.iconButton)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.extensions_options, extension.name),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_app_info)) },
                        onClick = {
                            menuExpanded = false
                            onAppInfo()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_copy_package_name)) },
                        onClick = {
                            menuExpanded = false
                            onCopyPackage()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.extensions_uninstall), color = scheme.error) },
                        onClick = {
                            menuExpanded = false
                            onUninstall()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = scheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    )
}

/** Non-interactive row used for empty and filtered-out states. */
@Composable
private fun ExtensionsHintRow(
    icon: ImageVector,
    title: String,
    message: String
) {
    ExtensionsRow(
        title = title,
        subtitle = message,
        subtitleMaxLines = 2,
        leading = {
            ExtensionsIconTile(
                icon = icon,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
    )
}

@Composable
private fun ExtensionsErrorRow(message: String, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.controlShape)
            .background(scheme.errorContainer.copy(alpha = 0.55f))
            .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = scheme.onErrorContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onErrorContainer.copy(alpha = 0.9f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRetry) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(stringResource(R.string.extensions_retry))
            }
    }
}

@Composable
private fun ExtensionsRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBlock(
            modifier = Modifier.size(40.dp),
            cornerRadius = Radius.control
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBlock(
                modifier = Modifier
                    .width(140.dp)
                    .height(13.dp)
            )
            Spacer(Modifier.height(Spacing.xs))
            SkeletonBlock(
                modifier = Modifier
                    .width(96.dp)
                    .height(10.dp)
            )
        }
    }
}

// --- Dialogs and search ------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionsSearchField(query: String, onQueryChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.extensions_search_installed)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.extensions_clear),
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = Radius.controlShape,
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = scheme.outline.copy(alpha = 0.25f),
            focusedBorderColor = scheme.primary.copy(alpha = 0.6f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionsAddRepoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ExtensionKind) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ExtensionKind.MANGA) }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (url.isNotBlank()) {
            onConfirm(url.trim(), kind)
            url = ""
            keyboard?.hide()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.extensions_add_repo)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    ExtensionKind.entries.forEach { candidate ->
                        FilterChip(
                            selected = kind == candidate,
                            onClick = { kind = candidate },
                            label = { Text(candidate.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = Radius.chipShape
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = stringResource(R.string.extensions_add_repo_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://example.com/index.json") },
                    singleLine = true,
                    shape = Radius.controlShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = url.isNotBlank()) { Text(stringResource(R.string.extensions_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun ExtensionsConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

// --- Helpers -----------------------------------------------------------------

private const val EXTENSIONS_SEARCH_KEY = "extensions_search"

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun openAppSettings(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:$packageName".toUri()
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}