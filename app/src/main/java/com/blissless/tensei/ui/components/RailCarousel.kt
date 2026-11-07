package com.blissless.tensei.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.math.absoluteValue

/**
 * The scroll-driven "carousel" effect shared by the horizontal rail rows
 * (characters/staff/relations/recommendations cards in the detail screens
 * and character/staff media rails).
 *
 * Cards at the row's center render at full size; cards moving toward either
 * edge shrink, fade, drift inward and rotate away — see
 * [railScrollTransforms]. The offsets animate with a stiff, non-bouncy spring
 * while the row is scrolling and settle back with a bouncy spring when it
 * stops — see [animateRailItemOffset].
 *
 * Previously this ~30-line block was duplicated verbatim in eight places.
 */

/**
 * Computes the normalized horizontal center offset of the rail item at
 * [index] in [listState]: `0f` at the viewport center, approaching `±1f`
 * (or beyond, clipped by callers) at the edges. Returns `0f` when the item
 * is not laid out yet.
 */
@Composable
fun rememberRailItemCenterOffset(listState: LazyListState, index: Int): Float {
    val layoutInfo: State<androidx.compose.foundation.lazy.LazyListLayoutInfo> =
        remember { derivedStateOf { listState.layoutInfo } }
    val info by layoutInfo
    val itemInfo = info.visibleItemsInfo.find { it.index == index }
    return if (itemInfo != null) {
        val itemCenter = itemInfo.offset + itemInfo.size / 2
        val screenCenter = (info.viewportSize.width / 2).toFloat()
        (itemCenter - screenCenter) / screenCenter
    } else {
        0f
    }
}

/**
 * Animates a rail card's center offset: while the row is scrolling the
 * offset tracks tightly (stiff, no bounce); once scrolling stops it settles
 * back with a soft, bouncy spring.
 */
@Composable
fun animateRailItemOffset(centerOffset: Float, isScrolling: Boolean, label: String): Float {
    val animated by animateFloatAsState(
        targetValue = if (isScrolling) centerOffset.coerceIn(-1.5f, 1.5f) else 0f,
        animationSpec = if (isScrolling) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = label,
    )
    return animated
}

/** The scroll-driven transforms applied to a rail card. */
data class RailScrollTransforms(
    val scale: Float,
    val alpha: Float,
    val translationX: Float,
    val rotationY: Float,
)

/**
 * Maps a (possibly animated) rail center offset to card transforms:
 * up to 25% shrink, 60% fade, a 20px inward drift and a 15° rotation
 * toward the screen edge.
 */
fun railScrollTransforms(animatedOffset: Float): RailScrollTransforms =
    RailScrollTransforms(
        scale = 1f - (animatedOffset.absoluteValue * 0.25f).coerceAtMost(0.25f),
        alpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f),
        translationX = animatedOffset * -20f,
        rotationY = (animatedOffset * 15f).coerceIn(-15f, 15f),
    )
