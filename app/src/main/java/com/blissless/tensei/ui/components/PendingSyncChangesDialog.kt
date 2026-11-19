package com.blissless.tensei.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.R
import com.blissless.tensei.ui.theme.PillShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.viewmodel.PendingSyncEntry

/**
 * Full detail view behind the change counts in [CrossProviderSyncDialog]: every entry that
 * differs between AniList and MAL, grouped by direction. For entries present on both sides
 * the differing fields are shown as target → source (the values the sync would write);
 * entries that exist on only one side are shown as additions to / removals from the target.
 * Rendering follows the sync dialog (same surface, logos, pills; monochrome/OLED aware).
 */
@Composable
fun PendingSyncChangesDialog(
    entries: List<PendingSyncEntry>,
    aniToMalAnime: Int,
    aniToMalManga: Int,
    malToAniAnime: Int,
    malToAniManga: Int,
    onDismiss: () -> Unit,
    useMonochrome: Boolean = false,
    isOled: Boolean = false,
) {
    val aniListAccent = if (useMonochrome) MaterialTheme.colorScheme.primary else Color(0xFF07A9FF)
    val malAccent = if (useMonochrome) MaterialTheme.colorScheme.secondary else Color(0xFF2E51A2)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = Radius.sheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isOled) 0.dp else 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 620.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    Radius.sheetShape
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.lg)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.comp_sync_pending_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.comp_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        DirectionSectionHeader(
                            label = stringResource(R.string.comp_sync_pending_ani_to_mal),
                            count = aniToMalAnime + aniToMalManga,
                            aniListFirst = true,
                            aniListAccent = aniListAccent,
                            malAccent = malAccent,
                            isOled = isOled
                        )
                    }
                    if (entries.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.comp_sync_pending_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            )
                        }
                    }
                    items(entries) { entry ->
                        PendingEntryCard(entry = entry, toMal = true, isOled = isOled)
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        DirectionSectionHeader(
                            label = stringResource(R.string.comp_sync_pending_mal_to_ani),
                            count = malToAniAnime + malToAniManga,
                            aniListFirst = false,
                            aniListAccent = aniListAccent,
                            malAccent = malAccent,
                            isOled = isOled
                        )
                    }
                    items(entries) { entry ->
                        PendingEntryCard(entry = entry, toMal = false, isOled = isOled)
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(Radius.controlShape)
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = Radius.controlShape
                ) {
                    Text(stringResource(R.string.comp_close), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Section header for one direction: small provider logos, an arrow and the change count. */
@Composable
private fun DirectionSectionHeader(
    label: String,
    count: Int,
    aniListFirst: Boolean,
    aniListAccent: Color,
    malAccent: Color,
    isOled: Boolean
) {
    val accent = if (aniListFirst) aniListAccent else malAccent
    Surface(
        shape = Radius.cardShape,
        color = if (isOled) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniProviderLogo(
                url = if (aniListFirst) com.blissless.tensei.network.Endpoints.AniList.FAVICON
                else com.blissless.tensei.network.Endpoints.Mal.FAVICON,
                isOled = isOled
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            MiniProviderLogo(
                url = if (aniListFirst) com.blissless.tensei.network.Endpoints.Mal.FAVICON
                else com.blissless.tensei.network.Endpoints.AniList.FAVICON,
                isOled = isOled
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(accent.copy(alpha = 0.12f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    stringResource(
                        if (count == 1) R.string.comp_sync_change_one else R.string.comp_sync_change_other,
                        count
                    ),
                    color = accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * One pending entry inside a direction section. [toMal] selects the direction: the source's
 * values are what the sync would write, the target's current values come next to the arrow.
 */
@Composable
private fun PendingEntryCard(entry: PendingSyncEntry, toMal: Boolean, isOled: Boolean) {
    val context = LocalContext.current
    Surface(
        shape = Radius.cardShape,
        color = if (isOled) Color(0xFF161616) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Type chip: Anime / Manga
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        stringResource(if (entry.isManga) R.string.comp_sync_type_manga else R.string.comp_sync_type_anime),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            val lineColor = MaterialTheme.colorScheme.onSurfaceVariant
            val valueColor = MaterialTheme.colorScheme.onSurface
            val fromStatus = if (toMal) entry.malStatus else entry.aniStatus
            val toStatus = if (toMal) entry.aniStatus else entry.malStatus
            val fromScore = if (toMal) entry.malScore else entry.aniScore
            val toScore = if (toMal) entry.aniScore else entry.malScore
            val fromProgress = if (toMal) entry.malProgress else entry.aniProgress
            val toProgress = if (toMal) entry.aniProgress else entry.malProgress

            when {
                // Present on both providers: a value sync — show each differing field as
                // target (current) → source (what the sync writes).
                entry.onAniList && entry.onMal -> {
                    if (fromStatus != toStatus) {
                        DiffLine(
                            label = context.getString(R.string.comp_sync_field_status),
                            from = pendingStatusLabel(fromStatus),
                            to = pendingStatusLabel(toStatus),
                            lineColor = lineColor,
                            valueColor = valueColor
                        )
                    }
                    if (fromScore != toScore) {
                        DiffLine(
                            label = context.getString(R.string.comp_sync_field_score),
                            from = fromScore.toString(),
                            to = toScore.toString(),
                            lineColor = lineColor,
                            valueColor = valueColor
                        )
                    }
                    if (fromProgress != toProgress) {
                        DiffLine(
                            label = context.getString(R.string.comp_sync_field_progress),
                            from = fromProgress.toString(),
                            to = toProgress.toString(),
                            lineColor = lineColor,
                            valueColor = valueColor
                        )
                    }
                }
                // Only on the source: the target gains the entry.
                (toMal && !entry.onMal) || (!toMal && !entry.onAniList) -> ActionLine(
                    text = stringResource(
                        if (toMal) R.string.comp_sync_action_add_mal else R.string.comp_sync_action_add_anilist
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                // Only on the target: the target loses the entry (the source lacks it).
                else -> ActionLine(
                    text = stringResource(
                        if (toMal) R.string.comp_sync_action_remove_mal else R.string.comp_sync_action_remove_anilist
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DiffLine(label: String, from: String, to: String, lineColor: Color, valueColor: Color) {
    Text(
        "$label: $from → $to",
        style = MaterialTheme.typography.bodySmall,
        color = lineColor
    )
}

@Composable
private fun ActionLine(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

/** Localized label for an AniList-style status key (or "Unknown" when absent). */
@Composable
private fun pendingStatusLabel(status: String?): String = when (status) {
    "CURRENT" -> stringResource(R.string.comp_status_watching)
    "PLANNING" -> stringResource(R.string.comp_status_planning)
    "COMPLETED" -> stringResource(R.string.comp_status_completed)
    "PAUSED" -> stringResource(R.string.comp_status_on_hold)
    "DROPPED" -> stringResource(R.string.comp_status_dropped)
    null, "" -> stringResource(R.string.comp_unknown)
    else -> status
}

@Composable
private fun MiniProviderLogo(url: String, isOled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .alpha(1f)
            .clip(CircleShape)
            .background(
                if (isOled) Color(0xFF1A1A1A)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(13.dp)
        )
    }
}
