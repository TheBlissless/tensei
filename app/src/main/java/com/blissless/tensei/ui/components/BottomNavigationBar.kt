package com.blissless.tensei.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.PillShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.tenseiColors

/**
 * The app's bottom navigation.
 *
 * A floating, edge-to-edge pill that floats above content (YouTube / Netflix
 * pattern) rather than a full-width `NavigationBar`. The selected tab expands
 * into a labelled pill while the rest stay icon-only.
 *
 * Previously this component listened for raw pointer presses via
 * `awaitPointerEvent`, which meant tabs fired while you were scrolling a list
 * underneath them and no ripple was ever drawn. It now uses `clickable` with a
 * real interaction source, and derives its colours from the theme instead of
 * taking `isOled` / `disableMaterialColors` flags.
 *
 * @param selectedIndex currently selected tab index
 * @param hidden when true nothing is rendered (player loading, full-screen overlays)
 * @param onSelect invoked with the new tab index
 */
private data class NavTab(val label: String, val icon: ImageVector)

private val NAV_TABS = listOf(
    NavTab("Schedule", Icons.Rounded.CalendarMonth),
    NavTab("Home", Icons.Rounded.Home),
    NavTab("Anime", Icons.Rounded.Movie),
    NavTab("Manga", Icons.Rounded.MenuBook),
)

@Composable
fun BottomNavigationBar(
    selectedIndex: Int,
    isOled: Boolean = false,
    disableMaterialColors: Boolean = false,
    hideNavbar: Boolean = false,
    isLoadingStream: Boolean = false,
    showSearchScreen: Boolean = false,
    onSelect: (Int) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope? = null,
) {
    if (hideNavbar || isLoadingStream || showSearchScreen) return

    val scheme = MaterialTheme.colorScheme
    val monochrome = tenseiColors.monochrome || disableMaterialColors

    val surfaceColor = if (isOled) Color.Black else scheme.surface
    val borderColor = scheme.outlineVariant.copy(alpha = if (isOled) 0.24f else 0.6f)
    val restingIconTint = scheme.onSurfaceVariant.copy(alpha = 0.72f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = Spacing.xl,
                end = Spacing.xl,
                bottom = Spacing.sm,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = Radius.sheetShape,
            color = surfaceColor.copy(alpha = 0.94f),
            tonalElevation = 3.dp,
            shadowElevation = 10.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .border(1.dp, borderColor, Radius.sheetShape),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NAV_TABS.forEachIndexed { index, tab ->
                    NavTabItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        monochrome = monochrome,
                        restingIconTint = restingIconTint,
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

/**
 * Duration of the tab pill's expand and collapse.
 *
 * Deliberately spring-free: a bouncy spring overshot the pill width and the
 * label alpha, so the pill visibly wobbled past its resting size and the row
 * re-measured on every frame, which read as jank.
 *
 * Expanding is slower than collapsing so the pill is easy to follow as it
 * travels to the new tab, while leaving a tab still feels immediate.
 */
private const val NAV_TAB_EXPAND_MILLIS = 220
private const val NAV_TAB_COLLAPSE_MILLIS = 140

@Composable
private fun NavTabItem(
    tab: NavTab,
    selected: Boolean,
    monochrome: Boolean,
    restingIconTint: Color,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    // One spec for every animated property of this tab, chosen by direction so
    // the pill never mixes easing speeds within a single transition.
    val animMillis = if (selected) NAV_TAB_EXPAND_MILLIS else NAV_TAB_COLLAPSE_MILLIS
    val colorSpec = tween<Color>(durationMillis = animMillis, easing = FastOutSlowInEasing)
    val floatSpec = tween<Float>(durationMillis = animMillis, easing = FastOutSlowInEasing)
    val dpSpec = tween<Dp>(durationMillis = animMillis, easing = FastOutSlowInEasing)

    val pillColor by animateColorAsState(
        targetValue = if (selected) {
            if (monochrome) Color.White.copy(alpha = 0.18f) else scheme.primaryContainer
        } else Color.Transparent,
        animationSpec = colorSpec,
        label = "navPillColor",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (monochrome) scheme.onSurface else scheme.onPrimaryContainer
        } else restingIconTint,
        animationSpec = colorSpec,
        label = "navContentColor",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = floatSpec,
        label = "navLabelAlpha",
    )
    val width by animateDpAsState(
        targetValue = if (selected) 92.dp else 44.dp,
        animationSpec = dpSpec,
        label = "navWidth",
    )
    // The label stays composed for the whole expand so it is never clipped
    // mid-flight, and is only dropped once the pill has collapsed past the
    // icon. Composing it on `selected` alone made it pop in at full width.
    val labelVisible = selected || width > 56.dp

    Row(
        modifier = Modifier
            .width(width)
            .height(44.dp)
            .clip(PillShape)
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier.size(21.dp),
        )
        if (labelVisible) {
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = labelAlpha),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}