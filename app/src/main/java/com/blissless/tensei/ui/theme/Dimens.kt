package com.blissless.tensei.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The single source of truth for every spacing / sizing value in the app.
 *
 * Previously each screen invented its own numbers, which is why horizontal
 * gutters ranged from 0dp to 32dp and card corners from 4dp to 24dp for the
 * same visual role. Everything below is a 4pt-grid multiple so layouts stay
 * optically aligned across screens.
 */
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp

    /** Horizontal inset between a screen edge and its content. */
    val gutter: Dp = lg

    /**
     * Horizontal inset between a screen edge and the first/last card of a horizontal rail.
     * Section headers share this so their titles line up with the rail's first card.
     *
     * Same value as [gutter]: the rails sit close to the edge so a poster fills
     * more of the screen, and the headers follow them to stay aligned.
     */
    val railGutter: Dp = gutter

    /** Gap between items inside a horizontal rail / grid. */
    val railItem: Dp = md

    /** Vertical gap between two top-level sections on a scrolling screen. */
    val section: Dp = xxl

    /** Vertical gap between rows inside a single card. */
    val cardItem: Dp = sm

    /** Inset from a card's edge to its content. */
    val cardPadding: Dp = md

    /** Standard list content padding (gutter on the sides, none on the ends). */
    val listVertical: Dp = sm
}

/**
 * Corner-radius scale. One radius per *role* so the same element always looks
 * the same no matter which screen renders it.
 */
object Radius {
    /** Chips, small tags, inline badges. */
    val chip: Dp = 8.dp

    /** Standard list item, search field, secondary button. */
    val control: Dp = 12.dp

    /** Poster covers, thumbnails, avatars. */
    val poster: Dp = 12.dp

    /** Standard card / list row. */
    val card: Dp = 16.dp

    /** Hero tiles, feature cards, section headers. */
    val feature: Dp = 20.dp

    /** Modal sheets and bottom sheets. */
    val sheet: Dp = 28.dp

    val posterShape: RoundedCornerShape get() = RoundedCornerShape(poster)
    val chipShape: RoundedCornerShape get() = RoundedCornerShape(chip)
    val controlShape: RoundedCornerShape get() = RoundedCornerShape(control)
    val cardShape: RoundedCornerShape get() = RoundedCornerShape(card)
    val featureShape: RoundedCornerShape get() = RoundedCornerShape(feature)
    val sheetShape: RoundedCornerShape get() = RoundedCornerShape(sheet)
}

/**
 * Fixed component dimensions. Kept in one place so a poster in a rail, a
 * poster in a grid, and a poster on a detail page all render identically.
 */
object Sizes {
    /** Width of a poster inside a horizontal rail. */
    val railPosterWidth: Dp = 140.dp

    /** Height of a poster inside a horizontal rail (2:3 ≈ 140x210, trimmed for the title block). */
    val railPosterHeight: Dp = 195.dp

/** Vertical space reserved under a rail poster for its two-line title. */
val railTitleBlock: Dp = 48.dp

    /** Width of the "continue watching" hero tile. */
    val continueCardWidth: Dp = 236.dp
    val continueCardHeight: Dp = 140.dp

    /** Standard touch target for icon-only actions. */
    val iconButton: Dp = 40.dp

    /** Small square action rendered on top of artwork. */
    val overlayButton: Dp = 30.dp

    /** Standard control height (buttons, text fields). */
    val controlHeight: Dp = 44.dp

    /**
     * Fixed width for the carousel hero action ("Watch Now" / "Read Now").
     *
     * The button cannot wrap its content: "Watch Now" is wider than "Read Now",
     * so the Anime hero row used to be visibly wider than the Manga one.
     */
    val heroActionButtonWidth: Dp = 160.dp

    /** Height of the bottom navigation bar surface. */
    val navBarHeight: Dp = 56.dp

    /** Top-bar content height, excluding window insets. */
    val topBarHeight: Dp = 56.dp
}