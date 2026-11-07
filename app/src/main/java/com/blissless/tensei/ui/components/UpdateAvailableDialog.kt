package com.blissless.tensei.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.blissless.tensei.R
import com.blissless.tensei.update.GitHubRelease
import com.blissless.tensei.update.deviceAbi
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * "Update Available" dialog shown on startup when a new GitHub release is found.
 *
 * The release is presented as a branded card: a gradient header with the app
 * icon and the old -> new version hop, the release notes (from the GitHub
 * release body), the size of the APK the device will actually download, and a
 * single primary action. Tapping it keeps the dialog open and streams the
 * download progress inline; the dialog closes itself when the installer opens.
 *
 * @param release          The pending GitHub release to install.
 * @param isDownloading    True while the APK download is in flight.
 * @param downloadProgress Download progress in 0..1 (only meaningful while downloading).
 * @param downloadError    Last download error, if any — switches the action to "Try again".
 * @param onDismiss        Called when the user postpones or backdrop-taps.
 * @param onUpdate         Called when the user starts (or retries) the download.
 */
@Composable
fun UpdateAvailableDialog(
    release: GitHubRelease,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadError: String? = null,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val currentVersion = remember { appVersionName(context) }
    val newVersion = remember(release.tagName) { release.tagName.removePrefix("v").removePrefix("V") }
    val notes = remember(release.body) { cleanReleaseNotes(release.body) }
    val abi = remember { deviceAbi() }
    val apk = remember(release, abi) { release.assets.firstOrNull { it.name.contains(abi, ignoreCase = true) } }
    val apkSizeText = apk?.takeIf { it.size > 0 }?.let { formatMegabytes(it.size) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = Radius.sheetShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 440.dp),
        ) {
            Column {
                Header(newVersion = newVersion, currentVersion = currentVersion)
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xl)) {
                    if (notes.isNotBlank()) {
                        ReleaseNotesSection(notes)
                        Spacer(Modifier.height(Spacing.lg))
                    }
                    if (apkSizeText != null) {
                        Text(
                            text = stringResource(R.string.comp_update_size_line, apkSizeText, abi),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.lg))
                    }
                    downloadError?.let { error ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 3,
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                    }
                    if (isDownloading) {
                        DownloadProgress(downloadProgress)
                    } else {
                        Button(
                            onClick = onUpdate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Sizes.controlHeight),
                            shape = Radius.controlShape,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(Spacing.sm))
                            Text(
                                stringResource(
                                    if (downloadError != null) R.string.comp_update_try_again
                                    else R.string.comp_update_download_install
                                )
                            )
                        }
                    }
                    if (!isDownloading) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.comp_update_not_now))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Gradient hero: app icon badge on the left, title and the version hop on the right.
 */
@Composable
private fun Header(newVersion: String, currentVersion: String) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.primaryContainer.copy(alpha = 0.6f),
                        colorScheme.surfaceContainerHigh,
                    )
                )
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_icon_default),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.comp_update_available),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.xs),
                ) {
                    VersionChip(label = currentVersion, emphasized = false)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    VersionChip(label = newVersion, emphasized = true)
                }
            }
        }
    }
}

/** Small pill showing a version number; the new version is highlighted. */
@Composable
private fun VersionChip(label: String, emphasized: Boolean) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = Radius.chipShape,
        color = if (emphasized) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            color = if (emphasized) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** Scrollable release-notes block, capped so long notes never grow the dialog. */
@Composable
private fun ReleaseNotesSection(notes: String) {
    Text(
        text = stringResource(R.string.comp_update_whats_new),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))
    Surface(
        shape = Radius.controlShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Text(
            text = notes,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
        )
    }
}

/** Determinate progress bar with a percentage label. */
@Composable
private fun DownloadProgress(progress: Float) {
    val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()
    Column {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.comp_update_downloading, percent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Version name of the installed build, or "-" if it cannot be read. */
private fun appVersionName(context: android.content.Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "-"
    } catch (_: Exception) {
        "-"
    }

/** Formats a byte count as megabytes with one decimal, e.g. "58.9 MB". */
private fun formatMegabytes(bytes: Long): String {
    val mb = bytes / 1024f / 1024f
    return if (mb >= 100f) "${mb.roundToInt()} MB" else String.format(java.util.Locale.US, "%.1f MB", mb)
}

/**
 * Reduces GitHub release markdown to the plain text the dialog can show:
 * image and link syntax are flattened, heading/bold/italic markers removed,
 * and bullet lines get a real bullet character. Blank runs collapse to one
 * newline so short notes stay compact.
 */
internal fun cleanReleaseNotes(raw: String): String = raw
    .lines()
    .mapNotNull { line ->
        line
            .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), "")          // images
            .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1")     // links -> their text
            .replace(Regex("^[#>]+\\s*"), "")                    // headings / quotes
            .replace(Regex("^[-*+]\\s+"), "• ")                   // bullets
            .replace("**", "").replace("`", "")                  // bold / inline code
            .trim()
            .ifBlank { null }
    }
    .joinToString("\n")
    .trim()
