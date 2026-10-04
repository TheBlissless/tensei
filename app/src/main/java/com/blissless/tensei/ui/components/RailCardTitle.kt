package com.blissless.tensei.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType

/**
 * The title block under a rail poster.
 *
 * Shared by the Home, Anime explore and Manga explore rails so the three screens
 * have identical title typography and, more importantly, identical space below
 * the title before the next section header.
 *
 * The height is a *minimum*, never a fixed height: two lines of [TenseiType.cardTitle]
 * plus the top padding do not fit inside [Sizes.railTitleBlock], so a fixed height
 * clipped the second line of long titles on small screens.
 */
@Composable
fun RailCardTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = TenseiType.cardTitle,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.railTitleBlock)
            .padding(top = Spacing.sm),
    )
}