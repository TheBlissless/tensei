package com.blissless.tensei.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared drag-to-dismiss nested-scroll behavior for sheet-style screens.
 *
 * The sheet can be dragged down while its scrollable content is at the top;
 * releasing past [dismissThreshold] (or with a brisk downward fling) runs
 * [onDismiss], otherwise it springs back to its resting offset. Previously
 * this connection was duplicated verbatim in the anime detail screen, the
 * manga detail screen and the episode selection sheet.
 *
 * @param offsetY the sheet's vertical drag offset (resting at 0)
 * @param isAtTop read on every scroll event — true while the sheet's
 *   scrollable content is at its top, enabling the drag
 * @param dismissThreshold drag offset (px) past which a release dismisses
 * @param onDismiss invoked when the release/fling should dismiss the sheet
 */
@Composable
fun rememberSheetDismissScrollConnection(
    offsetY: Animatable<Float, AnimationVector1D>,
    isAtTop: () -> Boolean,
    dismissThreshold: Float,
    onDismiss: () -> Unit,
): NestedScrollConnection {
    val scope = rememberCoroutineScope()
    return remember(offsetY, dismissThreshold) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = offsetY.value

                if (currentOffset > 0) {
                    if (available.y < 0) {
                        scope.launch {
                            offsetY.snapTo((currentOffset + available.y).coerceAtLeast(0f))
                        }
                        return available
                    }
                    if (available.y > 0) {
                        scope.launch {
                            offsetY.snapTo(currentOffset + available.y)
                        }
                        return available
                    }
                }

                if (isAtTop() && currentOffset <= 10f && available.y > 0) {
                    scope.launch { offsetY.snapTo(available.y) }
                    return available
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val currentOffset = offsetY.value

                if (currentOffset == 0f) return Velocity.Zero

                // The sheet can only reach offsetY > 0 while the list is at the top (see
                // onPreScroll), so an "isAtTop" gate here is redundant AND flaky: right
                // after scrolling back to the top the flag lags a frame, so a dismissal
                // fling is silently ignored and the sheet snaps back. Treat any release
                // with the sheet past the threshold, or a brisk downward fling, as a
                // dismissal.
                val shouldDismiss = currentOffset > dismissThreshold || available.y > 300f

                if (shouldDismiss) {
                    onDismiss()
                } else {
                    scope.launch {
                        offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                }

                return available
            }
        }
    }
}
