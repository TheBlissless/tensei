package com.blissless.tensei.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.ui.components.HomeStatusColors
import com.blissless.tensei.ui.components.StatusButton
import com.blissless.tensei.ui.theme.tenseiColors

/**
 * Converts a stored AniList score (0-100 raw) to its 0-10 display value.
 * Scores <= 10 are treated as legacy 0-10 values (written by the previous 0-100
 * input before ratings were switched to the 0-10 scale) so a rating of "10" set
 * there still shows as 10.
 */
fun userScoreToDisplay(raw: Int?): Int? = raw?.let { if (it > 10) it / 10 else it }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAnimeStatusDialog(
    anime: AnimeMedia,
    isOled: Boolean,
    preferEnglishTitles: Boolean = true,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (String, Int?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(anime.listStatus) }
    var selectedProgress by remember { mutableStateOf(if (anime.progress > 0) anime.progress.toString() else "") }
    var markedForRemoval by remember { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (showAnimation) 1.05f else 1f,
        animationSpec = tween(150),
        finishedListener = { if (showAnimation) showAnimation = false },
        label = "statusScale"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isOled) Color.Black else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = anime.cover, contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.width(60.dp).height(85.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title,
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        val latestEp = anime.latestEpisode?.takeIf { it > 0 }
                        val totalEp = anime.totalEpisodes.takeIf { it > 0 }
                        val progressText = when {
                            latestEp != null && latestEp > 0 && totalEp != null -> "${anime.progress} / $latestEp / $totalEp"
                            latestEp != null && latestEp > 0 -> "${anime.progress} / $latestEp"
                            totalEp != null -> "${anime.progress} / $totalEp"
                            else -> "${anime.progress}"
                        }
                        Text(stringResource(R.string.comp_home_dialog_progress, progressText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Row 1: Watching and Planning
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusButton(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.comp_status_watching),
                        selected = selectedStatus == "CURRENT" && !markedForRemoval,
                        onClick = {
                            selectedStatus = "CURRENT"
                            markedForRemoval = false
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).scale(if (selectedStatus == "CURRENT" && showAnimation && !markedForRemoval) scale else 1f),
                        HomeStatusColors.getColor("CURRENT")
                    )
                    StatusButton(
                        icon = Icons.Default.Bookmark,
                        label = stringResource(R.string.comp_status_planning),
                        selected = selectedStatus == "PLANNING" && !markedForRemoval,
                        onClick = {
                            selectedStatus = "PLANNING"
                            markedForRemoval = false
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).scale(if (selectedStatus == "PLANNING" && showAnimation && !markedForRemoval) scale else 1f),
                        HomeStatusColors.getColor("PLANNING")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Completed and On Hold
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusButton(
                        icon = Icons.Default.Check,
                        label = stringResource(R.string.comp_status_completed),
                        selected = selectedStatus == "COMPLETED" && !markedForRemoval,
                        onClick = {
                            selectedStatus = "COMPLETED"
                            markedForRemoval = false
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).scale(if (selectedStatus == "COMPLETED" && showAnimation && !markedForRemoval) scale else 1f),
                        HomeStatusColors.getColor("COMPLETED")
                    )
                    StatusButton(
                        icon = Icons.Default.Pause,
                        label = stringResource(R.string.comp_status_on_hold),
                        selected = selectedStatus == "PAUSED" && !markedForRemoval,
                        onClick = {
                            selectedStatus = "PAUSED"
                            markedForRemoval = false
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).scale(if (selectedStatus == "PAUSED" && showAnimation && !markedForRemoval) scale else 1f),
                        HomeStatusColors.getColor("PAUSED")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: Dropped and Remove
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusButton(
                        icon = Icons.Default.Close,
                        label = stringResource(R.string.comp_status_dropped),
                        selected = selectedStatus == "DROPPED" && !markedForRemoval,
                        onClick = {
                            selectedStatus = "DROPPED"
                            markedForRemoval = false
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).scale(if (selectedStatus == "DROPPED" && showAnimation && !markedForRemoval) scale else 1f),
                        HomeStatusColors.getColor("DROPPED")
                    )
                    Button(
                        onClick = {
                            markedForRemoval = !markedForRemoval
                            showAnimation = true
                        },
                        modifier = Modifier.weight(1f).height(44.dp).scale(if (markedForRemoval && showAnimation) scale else 1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (markedForRemoval) tenseiColors.destructive.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (markedForRemoval) tenseiColors.destructive else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.comp_status_remove), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = if (markedForRemoval) tenseiColors.destructive else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.comp_home_episode_progress), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                val maxEp = anime.latestEpisode?.takeIf { it > 0 } ?: anime.totalEpisodes.takeIf { it > 0 }

                OutlinedTextField(
                    value = selectedProgress,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { c -> c.isDigit() }
                        val clamped = if (maxEp != null) {
                            filtered.toIntOrNull()?.coerceIn(0, maxEp)?.toString() ?: filtered
                        } else {
                            filtered
                        }
                        selectedProgress = clamped
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text(stringResource(R.string.comp_home_enter_episode_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = if (selectedProgress.isEmpty() || selectedProgress == "0") Color.Transparent else MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (markedForRemoval) { onRemove() }
                        else { val progress = selectedProgress.toIntOrNull(); onUpdate(selectedStatus, progress) }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (markedForRemoval) tenseiColors.destructive else MaterialTheme.colorScheme.primary, contentColor = if (markedForRemoval) Color.White else MaterialTheme.colorScheme.onPrimary)
                ) { Text(if (markedForRemoval) stringResource(R.string.comp_remove_from_list) else stringResource(R.string.comp_save_changes), fontWeight = FontWeight.Bold) }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeRatingSheet(
    anime: AnimeMedia,
    isOled: Boolean,
    preferEnglishTitles: Boolean = true,
    onDismiss: () -> Unit,
    onScoreSaved: (Int?) -> Unit
) {
    val initialScore = anime.userScore?.takeIf { it > 0 }?.let { userScoreToDisplay(it) }
    var selectedScore by remember { mutableStateOf(initialScore?.toString() ?: "") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isOled) Color.Black else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = anime.cover, contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.width(60.dp).height(85.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.comp_your_rating), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = selectedScore,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { c -> c.isDigit() }
                    val clamped = filtered.toIntOrNull()?.coerceIn(0, 10)?.toString() ?: filtered
                    selectedScore = clamped
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text(initialScore?.let { stringResource(R.string.comp_rating_current, it) } ?: stringResource(R.string.comp_rating_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                trailingIcon = {
                    if (selectedScore.isNotEmpty()) {
                        Text("/10", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(end = 12.dp))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = if (selectedScore.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onScoreSaved(selectedScore.toIntOrNull()); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) { Text(stringResource(R.string.comp_save_rating), fontWeight = FontWeight.Bold) }
        }
    }
}
