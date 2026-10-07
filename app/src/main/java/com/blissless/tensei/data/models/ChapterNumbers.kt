package com.blissless.tensei.data.models

/**
 * The chapter a (possibly partial) chapter number belongs to: 314.1f and 314.2f both
 * belong to chapter 314.
 *
 * A subchapter therefore counts as an extra chapter only when it opens a new number
 * before the separator (314.1 when 314 itself never shipped), while a second subchapter
 * of a number that is already there (314.2, or 313.2 after 313) does not add one.
 *
 * Returns 0 for chapter numbers at or below 0; those are never counted.
 */
fun chapterBaseNumber(chapterNumber: Float): Int {
    if (chapterNumber <= 0f) return 0
    val floored = chapterNumber.toInt()
    // Float noise can leave 237.99999f behind; snap anything within 0.001 of the next
    // integer onto it so that chapter lands on the same base as its neighbours.
    return if (chapterNumber - floored >= 0.999f) floored + 1 else floored
}

/**
 * How many chapters [chapterNumbers] really are: every distinct base counts once.
 * 313f, 314.1f, 314.2f, 315f is three chapters.
 */
fun countDistinctChapters(chapterNumbers: Iterable<Float>): Int =
    chapterNumbers.asSequence()
        .map(::chapterBaseNumber)
        .filter { it > 0 }
        .distinct()
        .count()
