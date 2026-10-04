package com.blissless.tensei.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing

/**
 * Skeletons for the anime / manga detail screens.
 *
 * The detail screens always receive list data, so they render whatever already
 * arrived and only fall back to these placeholders for the fields and sections
 * the detail fetch has not filled in yet. Every skeleton mirrors the geometry of
 * the real component it stands in for, so nothing reflows when data lands.
 */

/** A single shimmering line, used for inline missing text (hero meta, chips). */
@Composable
fun SkeletonTextLine(
    width: Dp,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier,
) {
    SkeletonBlock(
        modifier = modifier
            .shimmer()
            .width(width)
            .height(height)
    )
}

/** A rounded shimmering pill, used for missing hero badges. */
@Composable
fun SkeletonPill(
    width: Dp = 64.dp,
    height: Dp = 30.dp,
    modifier: Modifier = Modifier,
) {
    SkeletonBlock(
        modifier = modifier
            .shimmer()
            .width(width)
            .height(height),
        cornerRadius = Radius.chip,
    )
}

/** Card chrome shared by every detail placeholder: same shape, tint and border. */
@Composable
private fun DetailSkeletonCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter),
        shape = RoundedCornerShape(Radius.feature),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.gutter)
                .shimmer(),
            content = content,
        )
    }
}

/** Mirrors the icon tile + title + subtitle header every detail card uses. */
@Composable
private fun DetailCardHeaderSkeleton(showAction: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            modifier = Modifier.size(38.dp),
            cornerRadius = Radius.control,
        )
        Spacer(Modifier.width(Spacing.md))
        Column {
            SkeletonTextLine(width = 120.dp, height = 16.dp)
            Spacer(Modifier.height(Spacing.xs))
            SkeletonTextLine(width = 84.dp, height = 11.dp)
        }
        Spacer(Modifier.weight(1f))
        if (showAction) {
            SkeletonTextLine(width = 64.dp, height = 26.dp)
        }
    }
}

/**
 * Placeholder for a detail rail section (relations, recommendations, cast,
 * staff). [itemWidth] and [posterAspectRatio] match the real rail so the
 * skeleton occupies the same space.
 */
@Composable
fun DetailRailSkeleton(
    itemCount: Int = 5,
    itemWidth: Dp = 110.dp,
    posterAspectRatio: Float = 3f / 4f,
    titleLines: Int = 2,
    showAction: Boolean = true,
    modifier: Modifier = Modifier,
) {
    DetailSkeletonCard(modifier = modifier) {
        DetailCardHeaderSkeleton(showAction = showAction)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Spacer(Modifier.height(Spacing.md))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            userScrollEnabled = false,
        ) {
            items(itemCount) {
                Column(modifier = Modifier.width(itemWidth)) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(posterAspectRatio),
                        cornerRadius = Radius.poster,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    repeat(titleLines) { line ->
                        SkeletonTextLine(
                            width = if (line == 0) itemWidth * 0.9f else itemWidth * 0.6f,
                            height = 11.dp,
                        )
                        if (line < titleLines - 1) {
                            Spacer(Modifier.height(Spacing.xs))
                        }
                    }
                }
            }
        }
    }
}

/** Placeholder for a prose section (synopsis, tags, genres). */
@Composable
fun DetailTextCardSkeleton(
    headerWidthFraction: Float = 0.45f,
    bodyLines: Int = 3,
    modifier: Modifier = Modifier,
) {
    DetailSkeletonCard(modifier = modifier) {
        DetailCardHeaderSkeleton(showAction = false)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Spacer(Modifier.height(Spacing.md))
        SkeletonTextLine(
            width = 140.dp,
            height = 14.dp,
            modifier = Modifier.fillMaxWidth(headerWidthFraction),
        )
        Spacer(Modifier.height(Spacing.sm))
        repeat(bodyLines) { line ->
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(if (line == bodyLines - 1) 0.6f else 0.95f)
                    .height(11.dp)
            )
            if (line < bodyLines - 1) {
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

/** Placeholder for the hero stats strip inside the information card. */
@Composable
fun DetailStatStripSkeleton(
    statCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shimmer()
            .clip(RoundedCornerShape(Radius.control))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(statCount) { index ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SkeletonTextLine(width = 42.dp, height = 18.dp)
                Spacer(Modifier.height(Spacing.sm))
                SkeletonTextLine(width = 52.dp, height = 10.dp)
            }
            if (index < statCount - 1) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                )
            }
        }
    }
}

/** Placeholder for the bento spec grid inside the information card. */
@Composable
fun DetailSpecGridSkeleton(
    rows: Int = 2,
    cellsPerRow: Int = 2,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.shimmer(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(cellsPerRow) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.control))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
                            .padding(Radius.control)
                    ) {
                        SkeletonTextLine(width = 52.dp, height = 10.dp)
                        Spacer(Modifier.height(Spacing.sm))
                        SkeletonTextLine(width = 84.dp, height = 14.dp)
                    }
                }
            }
        }
    }
}