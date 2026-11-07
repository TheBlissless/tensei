package com.blissless.tensei.ui.components

// Shared cubic ease-out curve used by card and rail entrance animations
// (was duplicated privately in several screen files before).
internal fun easeOutCubic(t: Float): Float {
    val t1 = t - 1
    return t1 * t1 * t1 + 1
}
