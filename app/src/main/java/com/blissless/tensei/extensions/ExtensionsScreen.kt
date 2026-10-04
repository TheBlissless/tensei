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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.util.longToast
import com.blissless.tensei.util.toast

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

    var searchQuery by remember { mutableStateOf("") }
    var showAddRepoDialog by remember { mutableStateOf(false) }
    var repoPendingRemoval by remember { mutableStateOf<RepoState?>(null) }
    var extensionPendingUninstall by remember { mutableStateOf<Extension?>(null) }

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

    val filteredExtensions = remember(uiState.extensions, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.extensions
        } else {
            uiState.extensions.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
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
                title = "Repositories",
                count = uiState.repos.size
            )
        }

        item {
            ExtensionsGroupCard(modifier = if (showSkeleton) Modifier.shimmer() else Modifier) {
                if (showSkeleton) {
                    ExtensionsRowSkeleton()
                    ExtensionsGroupDivider()
                    ExtensionsRowSkeleton()
                } else {
                    uiState.repos.forEachIndexed { index, repoState ->
                        ExtensionsRepoRow(
                            repoState = repoState,
                            onClick = { onSelectRepo(repoState.url) },
                            onCopyUrl = {
                                copyToClipboard(context, "Repo URL", repoState.url)
                                context.toast("Repo URL copied")
                            },
                            onRemove = { repoPendingRemoval = repoState }
                        )
                        if (index < uiState.repos.lastIndex) ExtensionsGroupDivider()
                    }
                    if (uiState.repos.isNotEmpty()) ExtensionsGroupDivider()
                    ExtensionsAddRepoRow(onClick = { showAddRepoDialog = true })
                }
            }
        }

        item {
            ExtensionsGroupHeader(
                title = "Installed",
                count = uiState.extensions.size,
                actionLabel = if (updatableCount > 0) "Update all" else null,
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
        if (!showSkeleton && uiState.extensions.isNotEmpty()) {
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

                    filteredExtensions.isEmpty() -> {
                        ExtensionsHintRow(
                            icon = Icons.Default.Extension,
                            title = if (searchQuery.isBlank()) {
                                "No extensions installed"
                            } else {
                                "No matches"
                            },
                            message = if (searchQuery.isBlank()) {
                                "Open a repository above to install one."
                            } else {
                                "No installed extension matches \"$searchQuery\"."
                            }
                        )
                    }

                    else -> {
                        filteredExtensions.forEachIndexed { index, extension ->
                            ExtensionsInstalledRow(
                                extension = extension,
                                hasUpdate = extension.packageName in uiState.updatablePackageNames,
                                onUpdate = { viewModel.updateExtension(extension.packageName) },
                                onAppInfo = { openAppSettings(context, extension.packageName) },
                                onCopyPackage = {
                                    copyToClipboard(context, "Package name", extension.packageName)
                                    context.toast("Package name copied")
                                },
                                onUninstall = { extensionPendingUninstall = extension }
                            )
                            if (index < filteredExtensions.lastIndex) ExtensionsGroupDivider()
                        }
                    }
                }
            }
        }

        if (updatableCount > 0) {
            item {
                Text(
                    text = if (updatableCount == 1) {
                        "1 extension can be updated from a repository."
                    } else {
                        "$updatableCount extensions can be updated from a repository."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
            }
        }
    }

    if (showAddRepoDialog) {
        ExtensionsAddRepoDialog(
            onDismiss = { showAddRepoDialog = false },
            onConfirm = { url ->
                viewModel.addRepo(url)
                showAddRepoDialog = false
            }
        )
    }

    repoPendingRemoval?.let { repoState ->
        ExtensionsConfirmDialog(
            title = "Remove repository?",
            message = repoState.repo?.name?.let {
                "$it will be removed from this device. Installed extensions are not uninstalled."
            } ?: "This repository will be removed from this device.",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.removeRepo(repoState.url)
                repoPendingRemoval = null
            },
            onDismiss = { repoPendingRemoval = null }
        )
    }

    extensionPendingUninstall?.let { extension ->
        ExtensionsConfirmDialog(
            title = "Uninstall ${extension.name}?",
            message = "You will be asked to confirm in the system uninstall dialog.",
            confirmLabel = "Uninstall",
            onConfirm = {
                AnimeExtensionInstaller(context).uninstall(extension.packageName)
                extensionPendingUninstall = null
            },
            onDismiss = { extensionPendingUninstall = null }
        )
    }
}

// --- Group chrome ------------------------------------------------------------

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
    badge: String? = null,
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
                    maxLines = 1,
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
    repoState: RepoState,
    onClick: () -> Unit,
    onCopyUrl: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val repo = repoState.repo
    val isReady = repo != null

    ExtensionsRow(
        title = repo?.name ?: repoState.url,
        subtitle = when {
            repoState.error != null -> repoState.error
            repoState.isLoading -> "Loading..."
            isReady -> repo.description.takeIf { it.isNotBlank() }
                ?: "${repo.extensions.size} available"
            else -> "Tap to retry"
        },
        subtitleColor = if (repoState.error != null) scheme.error else null,
        onClick = if (isReady) onClick else null,
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
                if (repoState.isLoading) {
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
                        contentDescription = "Repository options",
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy URL") },
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
                        text = { Text("Remove", color = scheme.error) },
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
        title = "Add repository",
        subtitle = "Paste a repository index.json URL",
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
            append(" - ${sourceCount} source")
            if (sourceCount != 1) append("s")
        }
        if (languages.isNotEmpty()) {
            append(" - ${languages.joinToString(", ")}")
        }
    }

    ExtensionsRow(
        title = extension.name,
        subtitle = subtitle,
        badge = if (extension.isNsfw) "NSFW" else null,
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
                    Text("Update", style = MaterialTheme.typography.labelMedium)
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
                        contentDescription = "${extension.name} options",
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("App info") },
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
                        text = { Text("Copy package name") },
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
                        text = { Text("Uninstall", color = scheme.error) },
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
            Text("Retry")
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
        placeholder = { Text("Search installed") },
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
                        contentDescription = "Clear",
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
private fun ExtensionsAddRepoDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (url.isNotBlank()) {
            onConfirm(url.trim())
            url = ""
            keyboard?.hide()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add repository") },
        text = {
            Column {
                Text(
                    text = "Paste the repository index URL.",
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
            TextButton(onClick = submit, enabled = url.isNotBlank()) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
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