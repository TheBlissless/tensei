package com.blissless.tensei.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.SectionAccentWidth
import com.blissless.tensei.ui.theme.SectionStripeShape
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType

/**
 * The app's one section header.
 *
 * There used to be three unrelated implementations — a raised `Surface` on
 * Home, a bare stripe on Explore, and an icon tile on the profile — which is
 * why the same "Continue Watching" label looked like three different features.
 * They now all render this.
 *
 * @param title section name
 * @param count optional trailing count pill
 * @param icon optional leading icon, tinted with [accent]
 * @param accent colour of the stripe / icon / count pill
 * @param onClick when non-null the row becomes tappable and shows a chevron
 * @param subtitle optional second line under the title
 */
@Composable
fun TenseiSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    count: Int? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (hovered && onClick != null) 1.02f else 1f,
        label = "sectionHeaderScale",
    )
    val stripeColor by animateColorAsState(accent, label = "sectionHeaderStripe")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(Radius.chipShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                } else Modifier
            )
            .padding(horizontal = Spacing.railGutter, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (subtitle != null) 32.dp else 20.dp)
                .background(stripeColor, SectionStripeShape)
        )
        Spacer(Modifier.width(Spacing.md))
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TenseiType.sectionLabel,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = TenseiType.cardMeta,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (count != null) {
            CountPill(count = count, accent = accent)
            if (onClick != null && showChevron) {
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Small tinted pill that renders a section's item count. */
@Composable
fun CountPill(
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Radius.chipShape,
        color = accent.copy(alpha = 0.14f),
    ) {
        Text(
            text = count.toString(),
            style = TenseiType.badge,
            color = accent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
        )
    }
}

/**
 * Disclosure header for expand/collapse sections (profile library, favourites).
 *
 * Shares the stripe + count-pill language of [TenseiSectionHeader] but signals
 * a different affordance (expand/collapse rather than "see all"), so it gets
 * its own chevron direction.
 */
@Composable
fun TenseiDisclosureHeader(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    count: Int? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Radius.chipShape)
            .clickable(onClick = onClick)
            .padding(start = Spacing.xs, top = Spacing.sm, end = 0.dp, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(SectionAccentWidth)
                .height(18.dp)
                .background(accent, SectionStripeShape)
        )
        Spacer(Modifier.width(Spacing.sm))
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = title,
            style = TenseiType.sectionLabel,
            color = scheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Spacer(Modifier.width(Spacing.sm))
            CountPill(count = count, accent = accent)
        }
        Spacer(Modifier.width(Spacing.sm))
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 0f else 90f,
            label = "disclosureRotation",
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = scheme.onSurfaceVariant,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

/** Compact label used inside dense lists where a full header is too heavy. */
@Composable
fun TenseiInlineHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(accent, SectionStripeShape)
        )
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Text(
            text = title,
            style = TenseiType.sectionLabel,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}