package com.blissless.tensei.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Colours that carry meaning but are not part of the Material colour scheme.
 *
 * These used to be inline `Color(0xFFFFD700)` / `Color(0xFFFF1744)` literals
 * scattered across a dozen screens, which meant they ignored dark mode, OLED
 * and the monochrome theme. They are resolved once in [AppTheme] and exposed
 * through a [CompositionLocal] so every screen stays in sync.
 */
@Immutable
data class TenseiColors(
    /** True when the monochrome theme is active; accents collapse to greys. */
    val monochrome: Boolean = false,
    /** True when only the surfaces are neutral and semantic accents keep their hue. */
    val semiMonochrome: Boolean = false,
    /** Score stars, "8.5" readouts. */
    val rating: Color,
    /** Favourite hearts and favourite-filled states. */
    val favorite: Color,
    /** Airing-status accent used on schedule rows. */
    val airing: Color,
    /** Trailer / play affordances. */
    val play: Color,
    /** "Completed" accent. */
    val completed: Color,
    /** Warning strip (ongoing, on hold). */
    val warning: Color,
    /** Destructive strip (cancelled, dropped). */
    val destructive: Color,
) {
    /** Tinted background for a badge/chip built from [accent]. */
    fun container(accent: Color, strength: Float = 0.16f): Color = accent.copy(alpha = strength)
}

/**
 * The colour modes offered in Appearance settings.
 *
 * [MATERIAL] keeps the dynamic Material palette, [SEMI_MONOCHROME] swaps that
 * palette for neutral greys but keeps the semantic accents (ratings stay
 * yellow, favourite hearts red, status badges coloured), and [MONOCHROME] drops
 * every hue so the whole UI reads neutral.
 */
enum class ColorMode(val value: String) {
    MATERIAL("material"),
    SEMI_MONOCHROME("semi_monochrome"),
    MONOCHROME("monochrome");

    /** True when the mode replaces Material's palette with neutral surfaces. */
    val usesNeutralSurfaces: Boolean get() = this != MATERIAL

    companion object {
        fun fromValue(value: String?): ColorMode =
            entries.find { it.value == value } ?: SEMI_MONOCHROME
    }
}

/** The default (colourful) palette, matching the app's existing accent hues. */
val DefaultTenseiColors = TenseiColors(
    rating = Color(0xFFFFC107),
    favorite = Color(0xFFFF1744),
    airing = Color(0xFF2196F3),
    play = Color(0xFFFFC107),
    completed = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    destructive = Color(0xFFF44336),
)

/**
 * Greyscale palette for the monochrome theme — accents keep their *role* but
 * drop their hue so the whole UI reads as neutral.
 */
fun monochromeTenseiColors(scheme: ColorScheme): TenseiColors = TenseiColors(
    monochrome = true,
    rating = scheme.onSurface,
    favorite = scheme.onSurface,
    airing = scheme.onSurfaceVariant,
    play = scheme.onSurface,
    completed = scheme.onSurfaceVariant,
    warning = scheme.onSurfaceVariant,
    destructive = scheme.error,
)

/**
 * Semi-monochrome palette: the surfaces come from the neutral greyscale schemes
 * while the accents keep their hue, so ratings stay yellow, favourite hearts
 * red and airing / finished / planned badges keep their status colours.
 */
fun semiMonochromeTenseiColors(): TenseiColors = TenseiColors(
    semiMonochrome = true,
    rating = DefaultTenseiColors.rating,
    favorite = DefaultTenseiColors.favorite,
    airing = DefaultTenseiColors.airing,
    play = DefaultTenseiColors.play,
    completed = DefaultTenseiColors.completed,
    warning = DefaultTenseiColors.warning,
    destructive = DefaultTenseiColors.destructive,
)

private fun darken(color: Color, amount: Float): Color =
    lerp(color, Color.Black, amount)

/**
 * OLED variant: accents are dimmed slightly so pure black dominates and the
 * screen doesn't glow on an OLED panel.
 */
fun oledTenseiColors(scheme: ColorScheme): TenseiColors = TenseiColors(
    rating = darken(DefaultTenseiColors.rating, 0.15f),
    favorite = darken(DefaultTenseiColors.favorite, 0.10f),
    airing = darken(DefaultTenseiColors.airing, 0.10f),
    play = darken(DefaultTenseiColors.play, 0.15f),
    completed = darken(DefaultTenseiColors.completed, 0.10f),
    warning = darken(DefaultTenseiColors.warning, 0.20f),
    destructive = darken(DefaultTenseiColors.destructive, 0.05f),
)

private val LocalTenseiColors = staticCompositionLocalOf { DefaultTenseiColors }

/** Provides [TenseiColors] to the subtree. Called by [AppTheme]. */
@Composable
fun ProvideTenseiColors(colors: TenseiColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTenseiColors provides colors, content = content)
}

/** Access the semantic accent colours. */
val tenseiColors: TenseiColors
    @Composable
    @ReadOnlyComposable
    get() = LocalTenseiColors.current

/**
 * Resolves the on-surface colour for text drawn over artwork.
 *
 * Artwork is unpredictable, so every badge/chip placed on a cover uses this
 * instead of an unconditional [Color.White].
 */
@Composable
@ReadOnlyComposable
fun onArtworkColor(): Color = Color.White

/** Translucent scrim used behind text/icons drawn over artwork. */
@Composable
@ReadOnlyComposable
fun artworkScrim(alpha: Float = 0.6f): Color = Color.Black.copy(alpha = alpha)

/** Vertical gradient that guarantees text legibility over cover art. */
@Composable
@ReadOnlyComposable
fun artworkGradient(topAlpha: Float, bottomAlpha: Float): androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(Color.Black.copy(alpha = topAlpha), Color.Black.copy(alpha = bottomAlpha))
    )

/** Accent for an AniList media status string (RELEASING / FINISHED / ...). */
@Composable
fun statusAccentFor(status: String?): Color {
    val colors = tenseiColors
    return when (status) {
        "RELEASING" -> colors.airing
        "FINISHED" -> colors.completed
        "NOT_YET_RELEASED" -> colors.warning
        "CANCELLED" -> colors.destructive
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}