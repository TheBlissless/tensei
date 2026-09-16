package com.blissless.tensei.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Server selector dropdown item.
 *
 * Displays the server name (uppercase) as the main text, and the
 * available qualities (e.g. "1080p, 720p, 480p") as a smaller
 * subtext line below.
 *
 * The qualities subtext helps the user distinguish between duplicate
 * server names (e.g. two "VIDSTREAM-2" entries with different
 * available qualities) and gives a quick visual on what resolutions
 * each server offers.
 *
 * @param serverName the server's display name (already includes
 *   quality suffix for non-lazy duplicates, e.g. "Vidstream-2 (1080p)",
 *   or index suffix for lazy duplicates, e.g. "Vidstream-2 #2").
 * @param qualities the list of quality labels available on this
 *   server (e.g. ["1080p", "720p", "480p"]). Empty list = no
 *   quality info available (lazy hoster or single-quality server).
 * @param isSelected whether this server is currently selected.
 * @param onClick invoked when the user taps this menu item.
 */
@Composable
fun ServerSelectorButton(
    serverName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    qualities: List<String> = emptyList(),
) {
    DropdownMenuItem(
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSelected) Icon(
                        Icons.Default.Check,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        serverName.uppercase(),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                // Qualities subtext — only shown if there are 2+ qualities
                // (single-quality or lazy hosters don't benefit from subtext
                // since the quality is already in the name for duplicates).
                if (qualities.size > 1) {
                    Text(
                        qualities.joinToString(" · "),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = if (isSelected) 24.dp else 0.dp)
                    )
                }
            }
        },
        onClick = {
            android.util.Log.d("ServerSelectorClick", "serverName=\"$serverName\" isSelected=$isSelected qualities=$qualities")
            onClick()
        }
    )
}
