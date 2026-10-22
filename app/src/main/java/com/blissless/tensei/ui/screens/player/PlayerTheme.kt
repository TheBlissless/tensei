package com.blissless.tensei.ui.screens.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blissless.tensei.ui.theme.tenseiColors

/** The app's accent blue, for controls drawn on the player's dark panels. */
internal val PlayerAccentBlue = Color(0xFF60A5FA)

/** Darker variant of the accent blue, readable as text on a light panel. */
internal val PlayerAccentBlueDark = Color(0xFF1B6BA4)

/**
 * Width of a player menu, never wider than the screen.
 *
 * On small devices the popup would otherwise be clipped by the screen edge, so the menu keeps a
 * margin and the labels wrap onto a second line instead of being cut off.
 */
@Composable
internal fun playerMenuWidth(preferred: Dp): Dp {
    val available = LocalConfiguration.current.screenWidthDp.dp - 24.dp
    return minOf(preferred, available).coerceAtLeast(160.dp)
}

/**
 * Accent for the player's floating controls (buttons, sliders, badges over the video).
 *
 * They always sit on dark translucent panels over arbitrary video content, so the accent has
 * to stay light: the theme's primary is a dark tone in the light theme and a neutral grey in
 * the semi-monochrome mode, so the app's accent blue is used there.
 */
@Composable
internal fun playerAccentColor(): Color {
    val colors = tenseiColors
    return when {
        colors.monochrome -> Color(0xFFCFCFCF)
        colors.semiMonochrome -> PlayerAccentBlue
        else -> {
            val primary = MaterialTheme.colorScheme.primary
            if (primary.luminance() > 0.35f) primary else PlayerAccentBlue
        }
    }
}

/**
 * Accent for the player's menus, which follow the app theme (a light panel with dark text in
 * the light theme).
 *
 * The theme's primary is used in MATERIAL mode, the app's accent blue in semi-monochrome mode
 * — darkened when the panel is light so it stays readable as text — and plain onSurface in
 * monochrome mode, where no hue is allowed.
 */
@Composable
internal fun playerMenuAccentColor(): Color {
    val colors = tenseiColors
    return when {
        colors.monochrome -> MaterialTheme.colorScheme.onSurface
        colors.semiMonochrome ->
            if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) PlayerAccentBlueDark else PlayerAccentBlue
        else -> MaterialTheme.colorScheme.primary
    }
}
