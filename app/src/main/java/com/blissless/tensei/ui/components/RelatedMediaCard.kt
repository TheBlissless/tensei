package com.blissless.tensei.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaRelation
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.ratingColorOnArtwork

/** Poster proportions used by every relations/recommendations card. */
private val PosterAspectRatio = 3f / 4f

/**
 * A related title (a relation or a recommendation) in one shape.
 *
 * Four different models feed these cards — [AnimeRelation], [ExploreAnime],
 * [MangaRelation] and [MangaMedia] — and each of them used to render its own
 * badges and format label inline, so the rails and the full screens drifted apart.
 */
data class RelatedMedia(
    val id: Int,
    /** Title used when English titles are preferred. */
    val title: String,
    /** The other title this model carries (romaji, or the untranslated default). */
    val alternateTitle: String? = null,
    val cover: String? = null,
    val format: String? = null,
    /** AniList `relationType`, e.g. `ADAPTATION` or `SIDE_STORY`. Absent for recommendations. */
    val relation: String? = null,
    val episodes: Int? = null,
    val latestEpisode: Int? = null,
    val chapters: Int? = null,
    val latestChapter: Float? = null,
    /** AniList average score, 0-100. */
    val score: Int? = null
)

fun AnimeRelation.asRelatedMedia(): RelatedMedia = RelatedMedia(
    id = id,
    title = title,
    alternateTitle = titleRomaji,
    cover = cover,
    format = format,
    relation = relationType,
    episodes = episodes,
    latestEpisode = latestEpisode,
    score = averageScore
)

fun ExploreAnime.asRelatedMedia(): RelatedMedia = RelatedMedia(
    id = id,
    title = titleEnglish?.takeIf { it.isNotBlank() } ?: title,
    alternateTitle = title,
    cover = cover,
    format = format,
    episodes = episodes,
    latestEpisode = latestEpisode,
    score = averageScore
)

fun MangaRelation.asRelatedMedia(): RelatedMedia = RelatedMedia(
    id = id,
    title = title,
    alternateTitle = titleRomaji,
    cover = cover,
    format = format,
    relation = relationType,
    chapters = chapters,
    score = averageScore
)

fun MangaMedia.asRelatedMedia(): RelatedMedia = RelatedMedia(
    id = id,
    title = titleEnglish?.takeIf { it.isNotBlank() } ?: title,
    alternateTitle = title,
    cover = cover,
    format = format,
    // MangaMedia counts chapters, not episodes; the badge has to say "ch".
    chapters = totalChapters,
    latestChapter = latestChapter,
    score = averageScore
)

/** "TV_SHORT" -> "TV Short"; unknown values are humanized instead of dropped. */
fun mediaFormatLabel(format: String?): String? = when {
    format.isNullOrBlank() -> null
    format == "TV" -> "TV"
    format == "TV_SHORT" -> "TV Short"
    format == "MOVIE" -> "Movie"
    format == "SPECIAL" -> "Special"
    format == "OVA" -> "OVA"
    format == "ONA" -> "ONA"
    format == "MANGA" -> "Manga"
    format == "NOVEL" -> "Novel"
    format == "ONE_SHOT" -> "One Shot"
    format == "MUSIC" -> "Music"
    format == "DOUJIN" -> "Doujin"
    format == "MANHWA" -> "Manhwa"
    format == "MANHUA" -> "Manhua"
    else -> format.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

/** "SIDE_STORY" -> "Side story", leaving already-cased values alone. */
fun relationLabel(relation: String?): String? =
    relation?.takeIf { it.isNotBlank() }
        ?.replace('_', ' ')
        ?.replaceFirstChar { c -> if (c.isLowerCase()) c.uppercase() else c.toString() }

/**
 * "TV, Adaptation" — the format with the relation beside it.
 *
 * `RECOMMENDATION` is AniList's own `relationType` for the recommendations
 * connection (see `fetchAnimeRecommendationsList`), so printing it would make
 * every recommendation read "TV, Recommendation" — the section the card is
 * already in says that, so only the format is left.
 */
fun RelatedMedia.metaLine(): String? =
    listOfNotNull(
        mediaFormatLabel(format),
        relation
            ?.takeUnless { it.equals("RECOMMENDATION", ignoreCase = true) }
            ?.let { relationLabel(it) }
    ).joinToString(", ").takeIf { it.isNotBlank() }

/** "12 eps" / "Ep 12" / "45 ch" / "Ch 12.5", or null when the count is unknown. */
fun RelatedMedia.countBadge(): String? = when {
    episodes != null && episodes > 0 -> "$episodes ${if (episodes == 1) "ep" else "eps"}"
    latestEpisode != null && latestEpisode > 0 -> "Ep $latestEpisode"
    chapters != null && chapters > 0 -> "$chapters ${if (chapters == 1) "ch" else "chs"}"
    latestChapter != null && latestChapter > 0f -> "Ch " + latestChapter.trimmedCount()
    else -> null
}

/** AniList score as "8.7"; null when unrated, so no empty badge is drawn. */
fun RelatedMedia.scoreBadge(): String? = score?.takeIf { it > 0 }?.let { "${(it / 10.0).toString().take(3)}" }

private fun Float.trimmedCount(): String =
    if (this % 1f == 0f) toInt().toString() else toString()

/**
 * Poster card shared by the relations and recommendations rails and by the
 * "All relations" / "All recommendations" grids.
 *
 * Badge layout: the count ("12 eps") sits at the top left, the rating at the top
 * right, and the format travels with the relation under the title ("TV,
 * Adaptation"). The relation used to be a badge over the artwork, which put it
 * in the same corner as the count badge and left the rating missing from relation
 * cards entirely.
 *
 * [titleHeight] lets the rails reserve a fixed title block so every card in the row
 * lines up, while the grids leave it unset and let the title grow.
 */
@Composable
fun RelatedMediaCard(
    media: RelatedMedia,
    modifier: Modifier = Modifier,
    preferEnglishTitle: Boolean = true,
    titleAlign: TextAlign? = null,
    titleHeight: Dp? = null,
    placeholderColor: Color = Color(0xFF0A0A0A),
    onClick: (() -> Unit)? = null
) {
    val displayTitle = if (preferEnglishTitle) media.title else media.alternateTitle ?: media.title

    Column(
        modifier = modifier
            .clip(Radius.posterShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(PosterAspectRatio)) {
            Card(
                shape = Radius.posterShape,
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = placeholderColor)
            ) {
                AsyncImage(
                    model = media.cover,
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            media.countBadge()?.let { badge ->
                TenseiScrimChip(
                    text = badge,
                    modifier = Modifier.padding(Spacing.sm).align(Alignment.TopStart)
                )
            }
            media.scoreBadge()?.let { badge ->
                TenseiScrimChip(
                    text = badge,
                    color = ratingColorOnArtwork(),
                    modifier = Modifier.padding(Spacing.sm).align(Alignment.TopEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = displayTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = titleAlign,
            modifier = titleHeight?.let { Modifier.height(it) } ?: Modifier
        )

        // Free height on purpose: "TV, Adaptation" wraps on a 110dp rail card, and a
        // fixed 16dp box clipped the second line off.
        media.metaLine()?.let { meta ->
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                textAlign = titleAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
