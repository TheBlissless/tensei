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

// Rows, tiles, badges and search field for the extensions screen.



/**
 * The ecosystem badges of a repository row.
 *
 * A repository can hold anime and manga extensions at the same time (e.g. a repo that ships a
 * manga extension next to its stream extensions), so every ecosystem it holds gets its own
 * badge, shown right after the "N available" text.
 */
private fun repoKindBadges(row: RepoRow): List<String> {
    val types = row.anime?.repo?.extensions?.map { it.type }?.toSet().orEmpty()
    val badges = mutableListOf<String>()
    if (types.any { it == ExtensionType.MANGA }) badges += ExtensionKind.MANGA.label
    if (types.any { it == ExtensionType.ANIME || it == ExtensionType.STREAM || it == ExtensionType.TORRENT }) {
        badges += ExtensionKind.ANIME.label
    }
    if (badges.isEmpty()) {
        // Nothing could be derived from the extensions themselves: fall back to the
        // ecosystem the repository was added to.
        badges += if (row.anime != null) ExtensionKind.ANIME.label else ExtensionKind.MANGA.label
    }
    return badges
}


/**
 * The ecosystem an installed extension belongs to, resolved from its package name.
 *
 * Tensei/Blissless extensions name themselves after the ecosystem they serve
 * (`com.blissless.<name>.manga`, `.anime.stream`, `.anime.torrent`) and Mihon's manga
 * extensions live under `eu.kanade.tachiyomi.extension.*`.
 */
private fun extensionKindFor(packageName: String): ExtensionKind =
    if (ExtensionType.fromPackageName(packageName) == ExtensionType.MANGA) {
        ExtensionKind.MANGA
    } else {
        ExtensionKind.ANIME
    }


/** A unified installed-extension entry, either an Aniyomi anime ext or a Mihon manga ext. */
internal sealed interface InstalledItem {
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
internal data class RepoRow(
    val url: String,
    val anime: RepoState?,
    val isManga: Boolean,
)


@Composable
internal fun MangaInstalledRow(
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


/**
 * Section label with an optional trailing action, matching the label style the
 * settings screen already uses so this page doesn't invent a fourth one.
 */
@Composable
internal fun ExtensionsGroupHeader(
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
internal fun ExtensionsGroupCard(
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
internal fun ExtensionsGroupDivider() {
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
    subtitleBadges: List<String> = emptyList(),
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
            if (subtitle != null || subtitleBadges.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor
                                ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = subtitleMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    // Badges that belong to the subtitle line (e.g. the ecosystem a repository
                    // holds), placed right after its text.
                    subtitleBadges.forEach { label ->
                        Spacer(Modifier.width(Spacing.sm))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = Radius.chipShape
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
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
internal fun ExtensionsRepoRow(
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
        subtitleBadges = if (animeState?.error != null) emptyList() else repoKindBadges(row),
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
internal fun ExtensionsAddRepoRow(onClick: () -> Unit) {
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
internal fun ExtensionsInstalledRow(
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
        kindLabel = extensionKindFor(extension.packageName).label,
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
internal fun ExtensionsHintRow(
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
internal fun ExtensionsErrorRow(message: String, onRetry: () -> Unit) {
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
internal fun ExtensionsRowSkeleton() {
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
internal fun ExtensionsSearchField(query: String, onQueryChange: (String) -> Unit) {
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
