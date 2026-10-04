package com.blissless.tensei.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.MainViewModel
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

data class HomeAnimeCardBounds(
    val animeId: Int,
    val coverUrl: String,
    val bounds: android.graphics.RectF
)

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .shimmer(),
        verticalArrangement = Arrangement.spacedBy(Spacing.section),
    ) {
        repeat(3) { sectionIndex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter)
            ) {
                Box(
                    modifier = Modifier
                        .width(SectionAccentWidth)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, SectionStripeShape)
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                SkeletonBlock(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Spacing.sm))
                SkeletonBlock(modifier = Modifier.width(140.dp).height(14.dp))
                Spacer(modifier = Modifier.weight(1f))
                SkeletonBlock(
                    modifier = Modifier
                        .width(32.dp)
                        .height(18.dp),
                    cornerRadius = Radius.chip
                )
            }

            TenseiRailSkeleton(itemCount = 4)
        }
    }
}

/**
 * Home's section header.
 *
 * Now a thin wrapper over [TenseiSectionHeader] so the Home rails and the
 * Explore rails render the same label instead of looking like two features.
 */
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    count: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit = {}
) {
    TenseiSectionHeader(
        title = title,
        icon = icon,
        count = count,
        accent = iconTint,
        onClick = if (count > 0) onClick else null,
    )
}

@Composable
fun HomeAnimeHorizontalList(
    animeList: List<AnimeMedia>,
    listType: String,
    showStatusColors: Boolean = false,
    preferEnglishTitles: Boolean = true,
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    disableMaterialColors: Boolean = false,
    showProgressBar: Boolean = true,
    onAnimeClick: (AnimeMedia, HomeAnimeCardBounds?) -> Unit,
    onInfoClick: (AnimeMedia, HomeAnimeCardBounds?) -> Unit = { _, _ -> },
    listIndex: Int = 0,
    screenKey: String = "home",
    isVisible: Boolean = true,
    viewModel: MainViewModel? = null
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val cameraDistancePx = with(density) { 12.dp.toPx() }
    val translationYOffset = with(density) { (-40).dp.toPx() }

    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    val cinematicProgress = rememberCinematicAnimation(screenKey, isVisible, true)
    val staggerDelay = listIndex * 50f
    val effectiveProgress = ((cinematicProgress * 1000f - staggerDelay) / 1000f).coerceIn(0f, 1f)
    val easedProgress = easeOutCubic(effectiveProgress)

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
            contentPadding = PaddingValues(horizontal = Spacing.railGutter)
        ) {
            itemsIndexed(items = animeList, key = { _, anime -> "${listType}_${anime.id}" }) { index, anime ->
                val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
                val visibleItems = layoutInfo.visibleItemsInfo
                val itemInfo = visibleItems.find { it.index == index }

                val centerOffset = if (itemInfo != null) {
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    val screenCenter = (layoutInfo.viewportSize.width / 2).toFloat()
                    (itemCenter - screenCenter) / screenCenter
                } else {
                    0f
                }

                val animatedOffset by animateFloatAsState(
                    targetValue = if (isScrolling) centerOffset.coerceIn(-1.5f, 1.5f) else 0f,
                    animationSpec = if (isScrolling) {
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        )
                    } else {
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        )
                    },
                    label = "centerOffset"
                )

                val baseScale = 1f - (animatedOffset.absoluteValue * 0.25f).coerceAtMost(0.25f)
                val baseAlpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f)
                val translationXVal = animatedOffset * -20f
                val rotationYVal = (animatedOffset * 15f).coerceIn(-15f, 15f)

                val introScale = 0.3f + easedProgress * 0.7f
                val introTranslationY = translationYOffset * (1f - easedProgress)

                val finalScale = baseScale * introScale
                val finalAlpha = baseAlpha * easedProgress

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = finalScale
                            scaleY = finalScale
                            alpha = finalAlpha
                            translationX = translationXVal
                            translationY = introTranslationY
                            rotationY = rotationYVal
                            cameraDistance = cameraDistancePx
                        }
                        .onGloballyPositioned { coordinates ->
                            val position = coordinates.positionInRoot()
                            val size = coordinates.size
                            val bounds = android.graphics.RectF(
                                position.x,
                                position.y,
                                position.x + size.width,
                                position.y + size.height
                            )
                            viewModel?.setHomeAnimeCardBounds(anime.id, anime.cover, bounds)
                        }
                ) {
                    HomeAnimeCard(
                        anime = anime,
                        listType = listType,
                        showStatusColors = showStatusColors,
                        preferEnglishTitles = preferEnglishTitles,
                        playbackPositions = playbackPositions,
                        playbackDurations = playbackDurations,
                        disableMaterialColors = disableMaterialColors,
                        showProgressBar = showProgressBar,
                        onClick = { bounds ->
                            viewModel?.setHomeAnimeCardBounds(anime.id, anime.cover, bounds?.bounds)
                            onAnimeClick(anime, bounds)
                        },
                        onInfoClick = { bounds ->
                            viewModel?.setHomeAnimeCardBounds(anime.id, anime.cover, bounds?.bounds)
                            onInfoClick(anime, bounds)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeAnimeCard(
    anime: AnimeMedia,
    listType: String,
    showStatusColors: Boolean = false,
    preferEnglishTitles: Boolean = true,
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    disableMaterialColors: Boolean = false,
    showProgressBar: Boolean = true,
    onClick: (HomeAnimeCardBounds?) -> Unit,
    onInfoClick: (HomeAnimeCardBounds?) -> Unit = {}
) {
    val context = LocalContext.current
    var cardBounds by remember { mutableStateOf<android.graphics.RectF?>(null) }
    val statusColor = HomeStatusColors.getColor(listType)

    // Progress bar color: bright white for monochrome, bright user-defined primary for material colors
    val progressColor = if (disableMaterialColors) Color.White else MaterialTheme.colorScheme.primary

    val total = anime.totalEpisodes
    val released = anime.latestEpisode ?: total

    val nextEpisode = anime.progress + 1
    val playbackKey = "${anime.id}_$nextEpisode"
    val savedPosition = playbackPositions[playbackKey] ?: 0L
    val savedDuration = playbackDurations[playbackKey] ?: 0L
    val effectiveDuration = if (savedDuration > 0L) savedDuration else 24 * 60 * 1000L
    val progressPercent = if (savedPosition > 0) (savedPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f) else 0f

    val progressText = when (listType) {
        "CURRENT" -> {
            when {
                total > 0 && released < total -> "${anime.progress} / $released / $total"
                total > 0 -> "${anime.progress} / $total"
                released > 0 -> "${anime.progress} / $released"
                else -> "${anime.progress}"
            }
        }
        "COMPLETED" -> { if (total > 0) "$total ${if (total == 1) "ep" else "eps"}" else "${anime.progress} ${if (anime.progress == 1) "ep" else "eps"}" }
        "PAUSED", "DROPPED" -> {
            when {
                total > 0 && released < total -> "${anime.progress} / $released / $total"
                total > 0 -> "${anime.progress} / $total"
                released > 0 -> "${anime.progress} / $released"
                else -> if (anime.progress > 0) "${anime.progress}" else "??"
            }
        }
        else -> {
            when {
                total > 0 -> "$released / $total"
                released > 0 -> "$released"
                else -> "??"
            }
        }
    }

    val imageRequest = remember(anime.cover) {
        ImageRequest.Builder(context)
            .data(anime.cover)
            .memoryCacheKey(anime.cover)
            .diskCacheKey(anime.cover)
            .crossfade(false)
            .build()
    }

    Column(modifier = Modifier.width(Sizes.railPosterWidth)) {
        Card(
            shape = Radius.posterShape,
            modifier = Modifier
                .height(Sizes.railPosterHeight)
                .clip(Radius.posterShape)
                .clickable {
                    onClick(
                        if (cardBounds != null && cardBounds!!.width() > 0 && cardBounds!!.height() > 0) {
                            HomeAnimeCardBounds(anime.id, anime.cover, cardBounds!!)
                        } else null
                    )
                }
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    val size = coordinates.size
                    cardBounds = android.graphics.RectF(
                        position.x,
                        position.y,
                        position.x + size.width,
                        position.y + size.height
                    )
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(model = imageRequest, contentDescription = anime.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

                // Scrims keep the badge and progress bar legible over any cover.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(artworkGradient(0.55f, 0f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(artworkGradient(0f, 0.85f))
                )

                // Top Row: Episode Counter (left) + Info Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TenseiScrimChip(text = progressText)

                    TenseiOverlayAction(
                        icon = Icons.Outlined.Info,
                        contentDescription = "Anime info",
                        onClick = {
                            onInfoClick(cardBounds?.let { HomeAnimeCardBounds(anime.id, anime.cover, it) })
                        },
                    )
                }

                // Bottom progress bar
                if (showProgressBar && progressPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(progressColor)
                        )
                    }
                }
            }
        }
        val displayTitle = when {
            preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
            anime.title.isNotEmpty() -> anime.title
            !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
            else -> "Unknown"
        }
        RailCardTitle(title = displayTitle)
    }
}

@Composable
fun ContinueWatchingRow(
    animeList: List<AnimeMedia>,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    tmdbEpisodeCache: Map<Int, List<TmdbEpisode>> = emptyMap(),
    preferEnglishTitles: Boolean = true,
    disableMaterialColors: Boolean = false,
    playbackKeySuffix: String = "",
    onPlayClick: (AnimeMedia, Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
        contentPadding = PaddingValues(horizontal = Spacing.railGutter)
    ) {
        itemsIndexed(items = animeList, key = { _, anime -> "continue_${anime.id}" }) { _, anime ->
            ContinueWatchingCard(
                anime = anime,
                playbackPositions = playbackPositions,
                playbackDurations = playbackDurations,
                tmdbEpisodeCache = tmdbEpisodeCache,
                preferEnglishTitles = preferEnglishTitles,
                disableMaterialColors = disableMaterialColors,
                playbackKeySuffix = playbackKeySuffix,
                onPlayClick = {
                    val nextEp = anime.progress + 1
                    onPlayClick(anime, nextEp)
                }
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    anime: AnimeMedia,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    tmdbEpisodeCache: Map<Int, List<TmdbEpisode>> = emptyMap(),
    preferEnglishTitles: Boolean = true,
    disableMaterialColors: Boolean = false,
    playbackKeySuffix: String = "",
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current

    val nextEpisode = anime.progress + 1
    val playbackKey = "${anime.id}_${nextEpisode}$playbackKeySuffix"
    val savedPosition = playbackPositions[playbackKey] ?: 0L
    val savedDuration = playbackDurations[playbackKey] ?: 0L
    val effectiveDuration = if (savedDuration > 0L) savedDuration else 24 * 60 * 1000L
    val progressPercent = (savedPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = effectiveDuration - savedPosition

    val tmdbStillUrl = tmdbEpisodeCache[anime.id]?.find { it.episode == nextEpisode }?.image
    val backgroundImageModel: Any = when {
        tmdbStillUrl != null -> tmdbStillUrl
        anime.banner != null -> anime.banner
        else -> anime.cover
    }

    val progressColor = if (disableMaterialColors) Color.White else MaterialTheme.colorScheme.primary

    val displayTitle = when {
        preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
        anime.title.isNotEmpty() -> anime.title
        !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
        else -> "Unknown"
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
                    .background(artworkGradient(0.3f, 0.88f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TenseiScrimChip(text = "Ep. $nextEpisode")
                    TenseiOverlayAction(
                        icon = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        onClick = onPlayClick,
                        size = 34.dp,
                    )
                }

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(Radius.chipShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (remainingMs > 0 && savedDuration > 0L) {
                        Text(
                            text = formatTimeRemaining(remainingMs),
                            style = TenseiType.cardMeta,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = displayTitle,
                        style = TenseiType.cardTitle,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

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
        entry.animeBanner != null -> entry.animeBanner!!
        else -> entry.animeCover
    }

    val displayTitle = when {
        preferEnglishTitles && !entry.animeTitleEnglish.isNullOrEmpty() -> entry.animeTitleEnglish
        entry.animeTitle.isNotEmpty() -> entry.animeTitle
        !entry.animeTitleEnglish.isNullOrEmpty() -> entry.animeTitleEnglish
        else -> "Unknown"
    }

    val progressColor = if (disableMaterialColors) Color.White else MaterialTheme.colorScheme.primary

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
                    .padding(Spacing.cardPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    TenseiScrimChip(text = "Ep. ${entry.episode}")
                    TenseiCircleAction(
                        icon = Icons.Default.Close,
                        contentDescription = "Remove from continue watching",
                        onClick = onDismissClick,
                        size = 32.dp,
                    )
                }

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(Radius.chipShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(progressColor)
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (remainingMs > 0 && savedDuration > 0L) {
                        Text(
                            text = formatTimeRemaining(remainingMs),
                            style = TenseiType.cardMeta,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Text(
                        text = displayTitle,
                        style = TenseiType.cardTitle,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatTimeRemaining(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "${min}:${sec.toString().padStart(2, '0')} remaining"
}

/**
 * Status pill used by the list-status dialogs.
 *
 * Previously the unselected state was hardcoded to `Color.White.copy(0.08f)`,
 * which meant these pills were invisible in light mode. Both states now derive
 * from the colour scheme.
 */
@Composable
fun StatusButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.height(Sizes.controlHeight),
        shape = Radius.controlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedColor else scheme.surfaceVariant,
            contentColor = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun easeOutCubic(t: Float): Float {
    val t1 = t - 1
    return t1 * t1 * t1 + 1
}

