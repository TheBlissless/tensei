package com.blissless.tensei.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material shape scale, mapped onto the app's [Radius] tokens so anything that
 * asks Material for a shape gets the same corner the app uses by hand.
 */
val TenseiShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.chip),
    small = RoundedCornerShape(Radius.control),
    medium = RoundedCornerShape(Radius.control),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.feature),
)

/** Rounded pill used by chips, segmented controls and the nav bar indicator. */
val PillShape = RoundedCornerShape(percent = 50)

/** Radius for the small coloured strip that sits to the left of a section title. */
val SectionAccentWidth = 3.dp
val SectionAccentShape = RoundedCornerShape(2.dp)

/** Alias kept short because it is referenced from every section header. */
val SectionStripeShape = SectionAccentShape