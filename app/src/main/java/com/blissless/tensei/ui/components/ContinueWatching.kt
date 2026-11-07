package com.blissless.tensei.ui.components

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ContinueWatchingEntry
import com.blissless.tensei.data.models.TmdbEpisode
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.SectionAccentWidth
import com.blissless.tensei.ui.theme.SectionStripeShape
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.theme.artworkGradient
import kotlin.math.absoluteValue

// Continue-watching rows and cards for the Home screen.

@Composable
fun ContinueWatchingEpisodeRow(
    entries: List<ContinueWatchingEntry>,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    tmdbEpisodeCache: Map<Int, List<TmdbEpisode>> = emptyMap(),
    preferEnglishTitles: Boolean = true,
    disableMaterialColors: Boolean = false,
    onPlayClick: (ContinueWatchingEntry) -> Unit,
    onDismissClick: (ContinueWatchingEntry) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
        contentPadding = PaddingValues(horizontal = Spacing.railGutter)
    ) {
        itemsIndexed(items = entries, key = { _, entry -> "continue_ep_${entry.animeId}_${entry.episode}" }) { _, entry ->
            ContinueWatchingEpisodeCard(
                entry = entry,
                playbackPositions = playbackPositions,
                playbackDurations = playbackDurations,
                tmdbEpisodeCache = tmdbEpisodeCache,
                preferEnglishTitles = preferEnglishTitles,
                disableMaterialColors = disableMaterialColors,
                onPlayClick = { onPlayClick(entry) },
                onDismissClick = { onDismissClick(entry) }
            )
        }
    }
}

@Composable
fun ContinueWatchingEpisodeCard(
    entry: ContinueWatchingEntry,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    tmdbEpisodeCache: Map<Int, List<TmdbEpisode>> = emptyMap(),
    preferEnglishTitles: Boolean = true,
    disableMaterialColors: Boolean = false,
    onPlayClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val context = LocalContext.current
    val playbackKey = "${entry.animeId}_${entry.episode}"
    val savedPosition = playbackPositions[playbackKey] ?: entry.position
    val savedDuration = playbackDurations[playbackKey] ?: entry.duration
    val effectiveDuration = if (savedDuration > 0L) savedDuration else 24 * 60 * 1000L
    val progressPercent = (savedPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = effectiveDuration - savedPosition

    val tmdbStillUrl = tmdbEpisodeCache[entry.animeId]?.find { it.episode == entry.episode }?.image
    val backgroundImageModel: Any = when {
        tmdbStillUrl != null -> tmdbStillUrl
        entry.animeBanner != null -> entry.animeBanner
        else -> entry.animeCover
    }

    val displayTitle = when {
        preferEnglishTitles && !entry.animeTitleEnglish.isNullOrEmpty() -> entry.animeTitleEnglish
        entry.animeTitle.isNotEmpty() -> entry.animeTitle
        !entry.animeTitleEnglish.isNullOrEmpty() -> entry.animeTitleEnglish
        else -> stringResource(R.string.comp_unknown)
    }

    val progressColor = if (disableMaterialColors) Color.White else MaterialTheme.colorScheme.primary

    // Same label slot as the Manga card: how far along the episode is, with a
    // percent fallback for entries whose duration was never recorded.
    val progressLabel = if (remainingMs > 0 && savedDuration > 0L) {
        formatTimeRemaining(context, remainingMs)
    } else {
        stringResource(R.string.comp_watched_percent, (progressPercent * 100).toInt())
    }

    Card(
        shape = Radius.cardShape,
        modifier = Modifier
            .width(Sizes.continueCardWidth)
            .height(Sizes.continueCardHeight)
            .clip(Radius.cardShape)
            .clickable { onPlayClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(backgroundImageModel)
                    .build(),
                contentDescription = displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(artworkGradient(0.25f, 0.9f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = Spacing.continueCardInset,
                        end = Spacing.continueCardInset,
                        top = Spacing.continueCardInset,
                        bottom = Spacing.continueCardInsetBottom
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TenseiScrimChip(text = stringResource(R.string.comp_ep_short, entry.episode))
                    TenseiCircleAction(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.comp_remove_from_continue_watching),
                        onClick = onDismissClick,
                        size = Sizes.continueCardDismissSize,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Manga's layout: title, then the position label, then the bar on
                // the bottom edge. The Anime card used to lead with a 4dp bar and
                // push the title to the very bottom, so the two rails on Home read
                // as different card families.
                Column {
                    Text(
                        text = displayTitle,
                        style = TenseiType.cardTitle,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Text(
                        text = progressLabel,
                        style = TenseiType.cardMeta,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.continueCardBarHeight)
                            .clip(Radius.chipShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .clip(Radius.chipShape)
                                .background(progressColor)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeRemaining(context: Context, ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return context.getString(R.string.comp_time_remaining, "${min}:${sec.toString().padStart(2, '0')}")
}
