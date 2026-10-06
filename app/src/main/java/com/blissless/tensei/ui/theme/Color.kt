package com.blissless.tensei.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Raw status hues. Used only where a plain Map<Color> is required (library
// breakdowns). Prefer [statusColor], which is theme-aware.
val StatusCurrent = Color(0xFF60A5FA)
val StatusPlanning = Color(0xFFC084FC)
val StatusCompleted = Color(0xFF34D399)
val StatusPaused = Color(0xFFFBBF24)
val StatusDropped = Color(0xFFF87171)

val StatusColors = mapOf(
    "CURRENT" to StatusCurrent,
    "PLANNING" to StatusPlanning,
    "COMPLETED" to StatusCompleted,
    "PAUSED" to StatusPaused,
    "DROPPED" to StatusDropped
)

val StatusLabels = mapOf(
    "CURRENT" to "Watching",
    "PLANNING" to "Planning",
    "COMPLETED" to "Completed",
    "PAUSED" to "On Hold",
    "DROPPED" to "Dropped"
)

// Manga-specific status labels (same colors as anime, just different text)
val MangaStatusLabels = mapOf(
    "CURRENT" to "Reading",
    "PLANNING" to "Planning",
    "COMPLETED" to "Completed",
    "PAUSED" to "On Hold",
    "DROPPED" to "Dropped"
)

/** Canonical ordering of the five list statuses. */
val StatusOrder = listOf("CURRENT", "PLANNING", "COMPLETED", "PAUSED", "DROPPED")

/**
 * Theme-aware accent for a list status key.
 *
 * In the monochrome theme every status collapses onto the *same* neutral
 * `onSurface` tone. Distinguishing them with slightly different greys still
 * reads as "coloured lists" against a greyscale UI, so monochrome deliberately
 * gives up per-status differentiation and relies on the label text instead.
 */
@Composable
@ReadOnlyComposable
fun statusColor(status: String?): Color {
    val scheme = MaterialTheme.colorScheme
    if (tenseiColors.monochrome) {
        return scheme.onSurface
    }
    return StatusColors[status] ?: scheme.outline
}

/**
 * [statusColor] for badges drawn on top of cover art.
 *
 * Monochrome collapses to white rather than `onSurface`, because a dark grey
 * accent would be unreadable against unpredictable artwork.
 */
@Composable
@ReadOnlyComposable
fun statusColorOnArtwork(status: String?): Color =
    if (tenseiColors.monochrome) onArtworkColor() else statusColor(status)

/**
 * Rating accent for readouts drawn on top of cover art (carousel hero, grid
 * badges).
 *
 * Monochrome collapses to white instead of `onSurface`, because a dark grey
 * star would be unreadable against a dark scrim in the light theme.
 */
@Composable
@ReadOnlyComposable
fun ratingColorOnArtwork(): Color =
    if (tenseiColors.monochrome) onArtworkColor() else tenseiColors.rating

/** Low-alpha background for a badge built from [statusColor]. */
@Composable
@ReadOnlyComposable
fun statusContainerColor(status: String?, strength: Float = 0.16f): Color =
    statusColor(status).copy(alpha = strength)

/**
 * The five list statuses resolved through [statusColor].
 *
 * For the few components that need a plain `Map<String, Color>` (the library
 * breakdowns on the profile screen) instead of a per-call lookup.
 */
@Composable
@ReadOnlyComposable
fun themeStatusColors(): Map<String, Color> =
    StatusOrder.associateWith { statusColor(it) }

/** Human-readable label for a status key, respecting the anime/manga wording. */
fun statusLabel(status: String?, isManga: Boolean = false): String {
    val labels = if (isManga) MangaStatusLabels else StatusLabels
    return labels[status] ?: status.orEmpty()
}

val SurfaceWhite = Color(0xFFF8F8F8)

/** The jigsaw piece blue of the app icon — brand accent for on-dark surfaces. */
val AppIconBlue = Color(0xFF1B6BA4)

val GlassWhite = Color(0x1AFFFFFF)
val GlassBlack = Color(0x1A000000)

val SurfaceElevatedLight = Color(0xFFFAFAFA)
val SurfaceElevatedDark = Color(0xFF1E1E1E)