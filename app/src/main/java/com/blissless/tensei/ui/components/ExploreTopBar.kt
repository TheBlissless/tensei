package com.blissless.tensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing

/**
 * The shared triple-layout header used by the main tabs.
 *
 * Every tab renders the same skeleton: leading circular app icon, a centred
 * middle slot, and a trailing circular search action. Only the middle slot
 * differs per screen (carousel dots on Anime/Manga, title + subtitle on the
 * schedule, profile on home).
 *
 * Colours default to theme values so the bar is legible on a flat surface;
 * the carousels override them with translucent white because their header sits
 * on top of artwork.
 *
 * @param topPadding distance above the header, defaults to the shared value so
 *   every tab's header starts at the same vertical offset
 *
 * The leading app icon and trailing search action are pinned to the top of the
 * row instead of being centred: the middle slot is taller on some tabs (Home's
 * profile pill, the schedule's title + subtitle) and centring made the two
 * circles drift a few dp lower than on Explore, whose page dots are shorter
 * than the circles. Top-aligning keeps every tab's circles at exactly
 * [topPadding] from the top of the screen.
 */
@Composable
fun ExploreTopBarRow(
    modifier: Modifier = Modifier,
    topPadding: Dp = exploreTopBarTopPadding(),
    leading: @Composable () -> Unit,
    center: @Composable RowScope.() -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl)
            .padding(top = topPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.align(Alignment.Top)) { leading() }
        center()
        Box(modifier = Modifier.align(Alignment.Top)) { trailing() }
    }
}

/**
 * Shared distance between the bottom of the status bar and the header row.
 *
 * The four tab headers used to sit a hardcoded 32dp from the *window* top and
 * consumed no status-bar inset at all (`setDecorFitsSystemWindows(false)` plus
 * a Scaffold with zero content insets), so the app icon and search circle were
 * 8dp below the status bar on a 24dp-bar device and underneath the clock on a
 * 40dp+ one. This resolves the real inset and adds one token on top, so the
 * row clears the status bar by the same amount everywhere.
 *
 * The token matches the gap between the header row and the schedule's day chips
 * on purpose: the space above the app icon / search circle mirrors the space
 * below them.
 */
@Composable
fun exploreTopBarTopPadding(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Spacing.xs

/** Circular app icon matching the carousel header: 40dp circle, 32dp image. */
@Composable
fun AppIconCircle(
    appIcon: String,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.iconButton,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = appIconDrawable(appIcon),
                contentDescription = "App",
                modifier = Modifier
                    .size(size * 0.8f)
                    .clip(CircleShape),
            )
        }
    }
}

/** Circular search action matching the carousel header: 40dp circle, 22dp icon. */
@Composable
fun SearchCircleAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Search",
    size: Dp = Sizes.iconButton,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

/** Pill-shaped page dots used by the featured carousels. */
@Composable
fun CarouselPageDots(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.4f),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(CAROUSEL_DOT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (index == currentIndex) CAROUSEL_DOT_ACTIVE_WIDTH else CAROUSEL_DOT_WIDTH,
                        height = CAROUSEL_DOT_HEIGHT,
                    )
                    .background(
                        if (index == currentIndex) activeColor else inactiveColor,
                        RoundedCornerShape(CAROUSEL_DOT_HEIGHT / 2),
                    )
            )
        }
    }
}

/**
 * Page-dot geometry.
 *
 * The dots were 16x5dp active / 5x5dp inactive at 5dp spacing, which read as
 * specks under the app icon and search circle rather than as a position
 * indicator. Sized up a little without turning them into pills.
 */
private val CAROUSEL_DOT_ACTIVE_WIDTH = 20.dp
private val CAROUSEL_DOT_WIDTH = 6.dp
private val CAROUSEL_DOT_HEIGHT = 6.dp
private val CAROUSEL_DOT_GAP = 6.dp