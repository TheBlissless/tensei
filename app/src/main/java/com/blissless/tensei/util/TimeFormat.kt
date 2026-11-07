package com.blissless.tensei.util

import java.util.Locale

/**
 * Time and duration formatting helpers shared across the app.
 */

/**
 * Formats a duration in milliseconds as a clock-style string:
 * `M:SS` below one hour, `H:MM:SS` above — e.g. `65000 -> "1:05"`,
 * `3725000 -> "1:02:05"`. Used by the player's timestamp indicators.
 */
fun formatPlaybackTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
