package com.blissless.tensei.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blissless.tensei.R
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.tenseiColors

/**
 * The three sync strategies offered when the user logs in after having tracked anime offline.
 *
 * Order matters: [ADD_NEW_ONLY] is the safest (default selection), [OVERWRITE] is the most
 * aggressive, and [DISCARD] is destructive. The dialog renders them in this safety order
 * rather than the old "1./2./3." numbered order — Material guidelines put destructive
 * actions last and out of thumb-reach.
 */
private enum class SyncChoice(val labelRes: Int) {
    ADD_NEW_ONLY(R.string.comp_add_new_only),
    OVERWRITE(R.string.comp_overwrite),
    DISCARD(R.string.comp_discard),
}

private data class SyncOption(
    val choice: SyncChoice,
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val accent: Color,
)

/**
 * Dialog shown when the user logs in after having tracked anime offline.
 *
 * Redesigned as a [ModalBottomSheet] (matching the rest of the app's sheets) with three
 * selectable option rows. Each row has a leading icon in a tinted container, a title,
 * a description, and a trailing radio indicator. A single confirm button at the bottom
 * applies the selected choice.
 *
 * Theme handling: every colour goes through [MaterialTheme.colorScheme] for neutrals and
 * [tenseiColors] for semantic accents. This means the dialog now renders correctly in
 * light, dark, OLED, semi-monochrome, and full-monochrome themes — the old AlertDialog
 * hardcoded `Color(0xFF1A1A1A)` + `Color.White` broke all of those.
 *
 * @param localAnimeCount Number of anime tracked offline (shown in the message)
 * @param onDismiss       Called when the dialog should close (e.g. on backdrop tap)
 * @param onDiscard       User chose to discard local changes
 * @param onAddNewOnly    User chose to add only new anime
 * @param onOverwrite     User chose to overwrite AniList with local data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalSyncDialog(
    localAnimeCount: Int,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onAddNewOnly: () -> Unit,
    onOverwrite: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Default selection is the safest option (Add New Only). The user must deliberately
    // move to Overwrite or Discard — this is the inverse of the old layout which put
    // Discard as the primary confirm button.
    var selectedChoice by remember { mutableStateOf(SyncChoice.ADD_NEW_ONLY) }

    val options = listOf(
        SyncOption(
            choice = SyncChoice.ADD_NEW_ONLY,
            titleRes = R.string.comp_local_sync_add_title,
            descRes = R.string.comp_local_sync_add_desc,
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            accent = tenseiColors.completed,
        ),
        SyncOption(
            choice = SyncChoice.OVERWRITE,
            titleRes = R.string.comp_local_sync_overwrite_title,
            descRes = R.string.comp_local_sync_overwrite_desc,
            icon = Icons.Default.Sync,
            accent = tenseiColors.airing,
        ),
        SyncOption(
            choice = SyncChoice.DISCARD,
            titleRes = R.string.comp_local_sync_discard_title,
            descRes = R.string.comp_local_sync_discard_desc,
            icon = Icons.Default.Delete,
            accent = tenseiColors.destructive,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = Radius.sheetShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
        ) {
            // Title row — gives the sheet a clear identity.
            Text(
                text = stringResource(R.string.comp_local_sync_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Message — the offline count + "choose how to sync" prompt.
            Text(
                text = stringResource(R.string.comp_local_sync_message, localAnimeCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))

            // Selectable option rows.
            options.forEach { option ->
                SyncOptionRow(
                    option = option,
                    isSelected = selectedChoice == option.choice,
                    onClick = { selectedChoice = option.choice },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Single primary confirm button — applies the selected choice. The label matches
            // the selected option so the user knows exactly what they're confirming. For the
            // destructive (Discard) choice, the button uses the destructive accent so the user
            // pauses before confirming.
            val confirmLabel = stringResource(selectedChoice.labelRes)
            val isDestructive = selectedChoice == SyncChoice.DISCARD
            val confirmColor = if (isDestructive) tenseiColors.destructive else MaterialTheme.colorScheme.primary
            val confirmContentColor = if (isDestructive) Color.White else MaterialTheme.colorScheme.onPrimary

            Button(
                onClick = {
                    when (selectedChoice) {
                        SyncChoice.ADD_NEW_ONLY -> onAddNewOnly()
                        SyncChoice.OVERWRITE -> onOverwrite()
                        SyncChoice.DISCARD -> onDiscard()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(Sizes.controlHeight),
                shape = Radius.controlShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = confirmContentColor,
                ),
            ) {
                Text(
                    text = confirmLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Secondary cancel button — always available, always neutral.
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A single selectable option row inside the sync dialog. Renders as a card with:
 *   - A leading icon in a tinted container (the option's accent colour at 16% alpha)
 *   - A title (the option's accent colour, SemiBold)
 *   - A description (onSurfaceVariant, smaller)
 *   - A trailing radio indicator (filled circle when selected, outline when not)
 *
 * The row's container animates between a subtle accent-tinted background (when selected)
 * and the surface colour (when not), so the user always knows which option is active.
 */
@Composable
private fun SyncOptionRow(
    option: SyncOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) option.accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "syncOptionContainer",
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Radius.cardShape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Leading icon in a tinted circle — mirrors CountPill / FilterChip's active state.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(option.accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = option.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))

            // Title + description column.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(option.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = option.accent,
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(option.descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(Spacing.sm))

            // Trailing radio indicator — animates between outline and filled.
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) option.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
