package com.blissless.tensei.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing

/**
 * The app's canonical loading / empty / error presentation.
 *
 * Before this existed, screens hand-rolled four different spinners, six
 * different empty states (some bare text, some a bare icon, some a card) and
 * four near-duplicate error banners. Every screen now routes through these so
 * a user sees the same shape of feedback everywhere.
 */

/**
 * Inline, dismissable-width banner used at the top of a screen to report a
 * non-fatal condition (offline, upstream outage).
 *
 * Unlike the old per-screen copies this always sits below the status bar and
 * always offers a retry when [onRetry] is supplied.
 */
@Composable
fun TenseiErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.CloudOff,
    onRetry: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm)
            .clip(Radius.controlShape)
            .background(scheme.errorContainer.copy(alpha = 0.7f))
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onErrorContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onErrorContainer.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Full-bleed centred message used when a list has nothing to show.
 *
 * [actionLabel] + [onAction] render a single primary call to action so empty
 * states are actionable rather than dead ends.
 */
@Composable
fun TenseiEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(scheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.md))
            FilledTonalButton(
                onClick = onAction,
                shape = Radius.chipShape,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Centred progress indicator for a screen with no skeleton to show. */
@Composable
fun TenseiLoadingState(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp),
        )
        if (label != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Small inline spinner used as a grid/list footer ("loading more"). */
@Composable
fun TenseiInlineLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Placeholder block used by skeleton loaders. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.chip,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    )
}

/**
 * Wraps content in a travelling highlight so any subtree can shimmer.
 *
 * Used by [LoadingSkeleton] and the per-rail placeholders so every loading
 * state in the app shares one animation.
 */
@Composable
fun Modifier.shimmer(durationMillis: Int = 1400): Modifier {
    val shineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return this.drawWithContent {
        drawContent()
        val sweepHalf = size.width * 0.6f
        val centerX = -sweepHalf + progress * (size.width + 2 * sweepHalf)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, shineColor, Color.Transparent),
                start = Offset(centerX - sweepHalf, 0f),
                end = Offset(centerX + sweepHalf, size.height),
            )
        )
    }
}

/**
 * Placeholder that mirrors the geometry of a section header (accent stripe, icon,
 * title line, trailing count pill) so skeleton states don't reflow when the real
 * header lands.
 */
@Composable
fun SectionHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        SkeletonBlock(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Spacing.sm))
        SkeletonBlock(
            modifier = Modifier
                .width(120.dp)
                .height(14.dp)
        )
        Spacer(Modifier.weight(1f))
        SkeletonBlock(
            modifier = Modifier
                .width(32.dp)
                .height(18.dp)
        )
    }
}

/**
 * Loading placeholder for a horizontal rail. Mirrors the geometry of the real
 * rail (same poster size, same title block) so the list doesn't reflow when
 * data arrives.
 */
@Composable
fun TenseiRailSkeleton(
    itemCount: Int = 4,
    showTitleBlock: Boolean = true,
) {
    LazyRow(
        modifier = Modifier.shimmer(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
        contentPadding = PaddingValues(horizontal = Spacing.railGutter),
        userScrollEnabled = false,
    ) {
        items(itemCount) {
            Column(modifier = Modifier.width(Sizes.railPosterWidth)) {
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.railPosterHeight),
                    cornerRadius = Radius.poster,
                )
                if (showTitleBlock) {
                    Spacer(Modifier.height(Spacing.sm))
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(12.dp),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(9.dp),
                    )
                }
            }
        }
    }
}