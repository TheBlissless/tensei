package com.blissless.tensei.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SkipIconButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 2.dp else 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            // In compact (portrait phone) mode, drop the colored background box around the
            // icon — the icon stands on its own against the video, matching the rest of the
            // compact controls (which use Color.Transparent backgrounds). The non-compact
            // (fullscreen) layout keeps the background box for visibility against busy scenes.
            modifier = Modifier
                .size(if (isCompact) 36.dp else 48.dp)
                .then(if (isCompact) Modifier else Modifier.background(backgroundColor, shape = MaterialTheme.shapes.small))
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(if (isCompact) 20.dp else 28.dp))
        }
        Text(text = label, style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize), color = Color.White, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

