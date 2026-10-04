package com.blissless.tensei.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** Shared gap above the header so every tab's bar starts at the same offset. */
@Composable
fun exploreTopBarTopPadding(): Dp = Spacing.xxxl

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
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentIndex) 16.dp else 5.dp, 5.dp)
                    .background(
                        if (index == currentIndex) activeColor else inactiveColor,
                        RoundedCornerShape(3.dp),
                    )
            )
        }
    }
}