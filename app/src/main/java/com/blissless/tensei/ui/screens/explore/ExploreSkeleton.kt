package com.blissless.tensei.ui.screens.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.blissless.tensei.ui.components.SectionHeaderSkeleton
import com.blissless.tensei.ui.components.TenseiRailSkeleton
import com.blissless.tensei.ui.theme.Spacing

/**
 * Whole-screen loading state shared by the Anime and Manga Explore tabs.
 *
 * Both tabs open on a featured carousel and continue with a stack of rails, so
 * the skeleton mirrors that: the hero placeholder first, then a section header
 * and rail per section. The two tabs used to assemble this separately — Anime
 * showed bare rails while Manga hand-rolled a carousel plus three rails — and
 * neither one had the status-bar inset the real layout has, so the same moment
 * looked different on each tab.
 *
 * The carousel placeholder is what makes this fill the screen: it covers the
 * hero area the real screen fills, so nothing shows an empty band at the top
 * while the queries are in flight.
 */
@Composable
fun ExploreScreenSkeleton(
    modifier: Modifier = Modifier,
    sectionCount: Int = 3,
    railItemCount: Int = 4,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FeaturedCarouselSkeleton()
        Spacer(Modifier.height(Spacing.md))

        repeat(sectionCount) { index ->
            SectionHeaderSkeleton()
            TenseiRailSkeleton(itemCount = railItemCount)
            if (index < sectionCount - 1) {
                Spacer(Modifier.height(Spacing.section))
            }
        }
    }
}