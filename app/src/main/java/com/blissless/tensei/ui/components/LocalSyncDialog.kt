package com.blissless.tensei.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blissless.tensei.R
import com.blissless.tensei.ui.theme.tenseiColors

/**
 * Dialog shown when the user logs in after having tracked anime offline.
 *
 * Extracted from MainActivity.kt. Offers three choices:
 *   1. Discard all local changes
 *   2. Add only new anime (don't overwrite existing entries)
 *   3. Overwrite AniList entries with local changes
 *
 * @param localAnimeCount Number of anime tracked offline (shown in the message)
 * @param onDismiss       Called when the dialog should close (e.g. on backdrop tap)
 * @param onDiscard       User chose to discard local changes
 * @param onAddNewOnly    User chose to add only new anime
 * @param onOverwrite     User chose to overwrite AniList with local data
 */
@Composable
fun LocalSyncDialog(
    localAnimeCount: Int,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onAddNewOnly: () -> Unit,
    onOverwrite: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                stringResource(R.string.comp_local_sync_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.comp_local_sync_message, localAnimeCount),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(R.string.comp_local_sync_choose),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(R.string.comp_local_sync_discard_title),
                    color = tenseiColors.destructive,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.comp_local_sync_discard_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(R.string.comp_local_sync_add_title),
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.comp_local_sync_add_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    stringResource(R.string.comp_local_sync_overwrite_title),
                    color = Color(0xFF2196F3),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.comp_local_sync_overwrite_desc),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(R.string.comp_discard), color = tenseiColors.destructive)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onAddNewOnly) {
                    Text(stringResource(R.string.comp_add_new_only), color = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onOverwrite) {
                    Text(stringResource(R.string.comp_overwrite), color = Color(0xFF2196F3))
                }
            }
        }
    )
}
