package com.blissless.tensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType

/**
 * The app's single top-bar implementation.
 *
 * The codebase previously mixed Material [androidx.compose.material3.TopAppBar]
 * with four hand-rolled `Row` headers that each faked the status-bar inset with
 * a magic 20/32/36dp top padding. This component owns the inset properly, so
 * headers stop hiding behind the status bar.
 *
 * @param title primary line
 * @param subtitle optional second line (count, breadcrumb, status)
 * @param leadingIcon 24dp icon rendered inside a 40dp touch target
 * @param onLeadingClick back/close behaviour; omit to hide the affordance
 * @param actions trailing icon buttons
 * @param scrim when true, fades artwork into a translucent bar (used over carousels)
 */
@Composable
fun TenseiTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onLeadingClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    scrim: Boolean = false,
    bottomDivider: Boolean = false,
    topInset: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
) {
    val scheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (scrim) {
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent,
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(containerColor)
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topInset)
                .height(Sizes.topBarHeight)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null && onLeadingClick != null) {
                IconButton(
                    onClick = onLeadingClick,
                    modifier = Modifier.size(Sizes.iconButton),
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = "Back",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.xs))
            } else {
                Spacer(Modifier.width(Spacing.sm))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TenseiType.screenTitle,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                content = actions,
            )
        }

        if (bottomDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant.copy(alpha = 0.5f))
            )
        }
    }
}

/** Circular, translucent action button that sits over artwork or a bar. */
@Composable
fun TenseiCircleAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.iconButton,
    containerColor: Color = Color.Black.copy(alpha = 0.42f),
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/** Rounded, translucent action button that sits over artwork. */
@Composable
fun TenseiOverlayAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.overlayButton,
    containerColor: Color = Color.Black.copy(alpha = 0.5f),
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(Radius.chipShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Convenience for the back affordance used on every pushed screen. */
@Composable
fun TenseiBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(Sizes.iconButton)) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Chip-shaped surface used to host a small piece of metadata on artwork. */
@Composable
fun TenseiScrimChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    containerColor: Color = Color.Black.copy(alpha = 0.62f),
) {
    Surface(
        modifier = modifier,
        shape = Radius.chipShape,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = TenseiType.badge,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
        )
    }
}

/** Bottom inset helper so every screen reserves the same gesture-bar space. */
@Composable
fun navigationBarBottomPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.sm