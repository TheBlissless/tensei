package com.blissless.tensei.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text drawn over a banner image.
 *
 * The banner is darkened by a scrim, so in the light theme the themed title colour (near black)
 * would be unreadable on top of it. The text is drawn twice: underneath a stroked copy in the
 * theme's background colour, which gives the title a crisp outline against the image (no blurred
 * glow), and on top the filled copy in the themed colour. Off the banner the outline is
 * invisible, because it matches the page background.
 */
@Composable
fun BannerOverlayText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val strokeWidth = (style.fontSize.value * 0.2f).coerceIn(2f, 5f)
    Box(modifier) {
        Text(
            text = text,
            style = style.copy(
                color = MaterialTheme.colorScheme.background,
                drawStyle = Stroke(width = strokeWidth)
            ),
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            color = color
        )
    }
}
