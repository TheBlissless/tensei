package eu.kanade.tachiyomi.animesource.model

/**
 * Represents a related anime returned by `AnimeSource.getRelatedAnimeList(...)`
 * — the prequel/sequel/side-story data shown on the anime details screen.
 *
 * Ported from Aniyomi's `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/AnimeRelation.kt`.
 *
 * @param type the kind of relation (PREQUEL, SEQUEL, SIDE_STORY, etc.).
 *   Free-form string — sources may use any value but should pick from
 *   the WELL-KNOWN constants below for compatibility.
 * @param anime the partial SAnime (only `url` and `title` populated).
 * @param cover the cover URL of the related anime (optional, may be null).
 */
data class AnimeRelation(
    val type: String,
    val anime: SAnime,
) {
    companion object {
        const val PREQUEL = "PREQUEL"
        const val SEQUEL = "SEQUEL"
        const val PARENT = "PARENT"
        const val SIDE_STORY = "SIDE_STORY"
        const val CHARACTER = "CHARACTER"
        const val SUMMARY = "SUMMARY"
        const val ALTERNATIVE = "ALTERNATIVE"
        const val SPIN_OFF = "SPIN_OFF"
        const val OTHER = "OTHER"
        const val FULL_STORY = "FULL_STORY"

        val WELL_KNOWN = setOf(
            PREQUEL, SEQUEL, PARENT, SIDE_STORY, CHARACTER, SUMMARY,
            ALTERNATIVE, SPIN_OFF, OTHER, FULL_STORY,
        )
    }
}
