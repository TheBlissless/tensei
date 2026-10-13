package com.blissless.tensei.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.R
import com.blissless.tensei.ui.theme.PillShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing

/**
 * Modern cross-provider sync prompt. The user first picks a direction (AniList → MAL or
 * MAL → AniList), the header flips to visualize that direction, then the primary action runs
 * the diff-based sync: the target is made to mirror the source exactly — entries are added,
 * updated, or removed when the source no longer has them. The provider logos are the same
 * favicons used by the settings login buttons.
 */
@Composable
fun CrossProviderSyncDialog(
    aniToMalAnime: Int,
    aniToMalManga: Int,
    malToAniAnime: Int,
    malToAniManga: Int,
    onDismiss: () -> Unit,
    onSyncAniListToMal: () -> Unit,
    onSyncMalToAniList: () -> Unit,
    useMonochrome: Boolean = false,
    isOled: Boolean = false,
) {
    var direction by remember { mutableStateOf<Boolean?>(null) }
    val aniToMal = direction == true
    val malToAni = direction == false

    // Brand colors for the two providers; monochrome mode maps them to grayscale scheme colors
    // so nothing stays colored. In OLED mode the dialog keeps a pure-black surface with subtle
    // near-black chips/cards instead of gray material surfaces.
    val aniListAccent = if (useMonochrome) MaterialTheme.colorScheme.primary else Color(0xFF07A9FF)
    val malAccent = if (useMonochrome) MaterialTheme.colorScheme.secondary else Color(0xFF2E51A2)

    Dialog(
        onDismissRequest = onDismiss,
        // Closed only through the buttons inside the dialog: an accidental tap on
        // the scrim (or a back press) must not discard the chosen sync direction.
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Surface(
            shape = Radius.sheetShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isOled) 0.dp else 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    Radius.sheetShape
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xl)) {
                // Header: the two provider logos with an animated direction arrow, centered.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = direction,
                        modifier = Modifier.fillMaxWidth(),
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally { it / 3 })
                                .togetherWith(fadeOut() + slideOutHorizontally { -it / 3 })
                        },
                        label = "direction"
                    ) { current ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (current) {
                                true -> {
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.AniList.FAVICON,
                                        contentDescription = "AniList",
                                        isOled = isOled
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    DirectionArrow(active = true, isOled = isOled)
                                    Spacer(Modifier.width(10.dp))
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.Mal.FAVICON,
                                        contentDescription = "MyAnimeList",
                                        isOled = isOled
                                    )
                                }
                                false -> {
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.Mal.FAVICON,
                                        contentDescription = "MyAnimeList",
                                        isOled = isOled
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    DirectionArrow(active = true, isOled = isOled)
                                    Spacer(Modifier.width(10.dp))
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.AniList.FAVICON,
                                        contentDescription = "AniList",
                                        isOled = isOled
                                    )
                                }
                                null -> {
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.Mal.FAVICON,
                                        contentDescription = "MyAnimeList",
                                        dimmed = true,
                                        isOled = isOled
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    DirectionArrow(active = false, isOled = isOled)
                                    Spacer(Modifier.width(10.dp))
                                    ProviderLogo(
                                        url = com.blissless.tensei.network.Endpoints.AniList.FAVICON,
                                        contentDescription = "AniList",
                                        dimmed = true,
                                        isOled = isOled
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.comp_sync_libraries_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.comp_sync_libraries_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))
                DirectionOption(
                    from = "AniList",
                    to = "MyAnimeList",
                    description = stringResource(R.string.comp_sync_ani_to_mal_desc),
                    accent = aniListAccent,
                    changesAnime = aniToMalAnime,
                    changesManga = aniToMalManga,
                    isOled = isOled,
                    selected = aniToMal,
                    onClick = { direction = true }
                )
                Spacer(Modifier.height(10.dp))
                DirectionOption(
                    from = "MyAnimeList",
                    to = "AniList",
                    description = stringResource(R.string.comp_sync_mal_to_ani_desc),
                    accent = malAccent,
                    changesAnime = malToAniAnime,
                    changesManga = malToAniManga,
                    isOled = isOled,
                    selected = malToAni,
                    onClick = { direction = false }
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.comp_sync_status_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (direction == true) onSyncAniListToMal() else onSyncMalToAniList()
                    },
                    enabled = direction != null,
                    modifier = Modifier
                        .clip(Radius.controlShape)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = Radius.controlShape
                ) {
                    Text(
                        when (direction) {
                            true -> stringResource(R.string.comp_sync_button_ani_to_mal)
                            false -> stringResource(R.string.comp_sync_button_mal_to_ani)
                            null -> stringResource(R.string.comp_sync_select_direction)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(Radius.controlShape)
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = Radius.controlShape
                ) {
                    Text(stringResource(R.string.comp_not_now), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProviderLogo(url: String, contentDescription: String, dimmed: Boolean = false, isOled: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .alpha(if (dimmed) 0.4f else 1f)
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
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun DirectionArrow(active: Boolean, isOled: Boolean = false) {
    val arrowColor by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (isOled) Color(0xFF1A1A1A)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = arrowColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DirectionOption(
    from: String,
    to: String,
    description: String,
    accent: Color,
    changesAnime: Int,
    changesManga: Int,
    isOled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val borderColor by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        label = "border"
    )
    val containerColor by animateColorAsState(
        when {
            selected -> accent.copy(alpha = 0.10f)
            isOled -> Color(0xFF111111)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        label = "container"
    )
    val total = changesAnime + changesManga
    Surface(
        shape = Radius.cardShape,
        color = containerColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius.cardShape)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$from → $to",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        CountPill(count = total, accent = accent, isOled = isOled)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (total > 0) {
                        Spacer(Modifier.height(2.dp))
                        val parts = buildList {
                            if (changesAnime > 0) add(context.getString(R.string.comp_sync_changes_anime, changesAnime))
                            if (changesManga > 0) add(context.getString(R.string.comp_sync_changes_manga, changesManga))
                        }
                        Text(
                            parts.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = accent
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (selected) accent else Color.Transparent)
                        .border(2.dp, borderColor, CircleShape)
                ) {
                    if (selected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountPill(count: Int, accent: Color, isOled: Boolean) {
    val inSync = count == 0
    val bg = when {
        inSync -> if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> accent.copy(alpha = 0.12f)
    }
    val fg = if (inSync) MaterialTheme.colorScheme.onSurfaceVariant else accent
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            if (inSync) stringResource(R.string.comp_sync_in_sync)
            else stringResource(if (count == 1) R.string.comp_sync_change_one else R.string.comp_sync_change_other, count),
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}