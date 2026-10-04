package com.blissless.tensei.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.blissless.tensei.ui.theme.statusColor

/**
 * Backwards-compatible bridge to [statusColor].
 *
 * Kept so the many existing call sites keep working, but it now resolves
 * through the theme so monochrome/OLED modes no longer leak raw hues.
 */
object HomeStatusColors {
    @Composable
    fun getColor(status: String?): Color = statusColor(status)

    @Composable
    fun getContainerColor(status: String?): Color =
        statusColor(status).copy(alpha = 0.16f)
}

@Deprecated(
    "Use statusColor() from com.blissless.tensei.ui.theme",
    ReplaceWith("com.blissless.tensei.ui.theme.statusColor(status)")
)
@Composable
fun getStatusColor(status: String?): Color = statusColor(status)

@Deprecated(
    "Use statusContainerColor() from com.blissless.tensei.ui.theme",
    ReplaceWith("com.blissless.tensei.ui.theme.statusContainerColor(status)")
)
@Composable
fun getStatusContainerColor(status: String?): Color =
    statusColor(status).copy(alpha = 0.16f)