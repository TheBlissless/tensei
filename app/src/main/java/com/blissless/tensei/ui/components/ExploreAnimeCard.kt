package com.blissless.tensei.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.theme.artworkGradient
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.ui.theme.StatusLabels
import java.util.Locale
import kotlin.math.absoluteValue

data class AnimeCardBounds(
    val animeId: Int,
    val coverUrl: String,
    val bounds: android.graphics.RectF
)

@Composable
fun ExploreAnimeHorizontalList(
    animeList: List<ExploreAnime>,
    animeStatusMap: Map<Int, String>,
    showStatusColors: Boolean,
    showAnimeCardButtons: Boolean = true,
    preferEnglishTitles: Boolean = true,
    onAnimeClick: (ExploreAnime, AnimeCardBounds?) -> Unit,
    onBookmarkClick: (ExploreAnime) -> Unit,
    onPlayClick: (ExploreAnime) -> Unit = { _ -> },
    isLoggedIn: Boolean = false,
    isOled: Boolean = false,
    localAnimeStatus: Map<Int, com.blissless.tensei.data.models.LocalAnimeEntry> = emptyMap(),
    onAddToLocalPlanning: (ExploreAnime) -> Unit = {},
    onRemoveFromLocalStatus: (ExploreAnime) -> Unit = {},
    listIndex: Int = 0,
    isVisible: Boolean = true,
    viewModel: com.blissless.tensei.MainViewModel? = null
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val cameraDistancePx = with(density) { 12.dp.toPx() }
    val translationYOffset = with(density) { (-40).dp.toPx() }
    
    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }
    
    val cinematicProgress = rememberCinematicAnimation("explore", isVisible, true)
    // Normalised stagger (see MangaScreen): progress still reaches 1 at the end,
    // so later sections don't sit permanently at 1 - delay/1000.
    val staggerMs = minOf(listIndex, 18) * 50f / 1000f
    val effectiveProgress = ((cinematicProgress - staggerMs) / (1f - staggerMs)).coerceIn(0f, 1f)
    val easedProgress = easeOutCubic(effectiveProgress)
    
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = Spacing.railGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem)
    ) {
        itemsIndexed(
            items = animeList,
            key = { _, anime -> anime.id }
        ) { index, anime ->
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

            val localStatus = localAnimeStatus[anime.id]?.status
            val handleAddLocalPlanning: () -> Unit = { onAddToLocalPlanning(anime) }
            val handleRemoveLocalStatus: () -> Unit = { onRemoveFromLocalStatus(anime) }
            
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
                        viewModel?.setExploreAnimeCardBounds(anime.id, anime.cover, bounds)
                    }
            ) {
                ExploreAnimeCard(
                    anime = anime,
                    currentStatus = animeStatusMap[anime.id],
                    showStatusColors = showStatusColors,
                    showAnimeCardButtons = showAnimeCardButtons,
                    preferEnglishTitles = preferEnglishTitles,
                    onClick = { bounds ->
                        viewModel?.setExploreAnimeCardBounds(anime.id, anime.cover, bounds?.bounds)
                        onAnimeClick(anime, bounds)
                    },
                    onBookmarkClick = { onBookmarkClick(anime) },
                    onPlayClick = { onPlayClick(anime) },
                    isLoggedIn = isLoggedIn,
                    isOled = isOled,
                    localStatus = localStatus,
                    onAddToLocalPlanning = handleAddLocalPlanning,
                    onRemoveFromLocalStatus = handleRemoveLocalStatus
                )
            }
        }
    }
}

private fun easeOutCubic(t: Float): Float {
    val t1 = t - 1
    return t1 * t1 * t1 + 1
}

@Composable
fun ExploreAnimeCard(
    anime: ExploreAnime,
    currentStatus: String?,
    showStatusColors: Boolean,
    showAnimeCardButtons: Boolean = true,
    preferEnglishTitles: Boolean = true,
    onClick: (AnimeCardBounds?) -> Unit,
    onBookmarkClick: () -> Unit,
    onPlayClick: () -> Unit = {},
    isLoggedIn: Boolean = false,
    isOled: Boolean = false,
    localStatus: String? = null,
    onAddToLocalPlanning: () -> Unit = {},
    onRemoveFromLocalStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    var cardBounds by remember { mutableStateOf<android.graphics.RectF?>(null) }

    var showAnimation by remember { mutableStateOf(false) }
    val bookmarkScale by animateFloatAsState(
        targetValue = if (showAnimation) 1.3f else 1f,
        animationSpec = tween(200),
        finishedListener = {
            if (showAnimation) {
                showAnimation = false
            }
        },
        label = "bookmarkScale"
    )

    val effectiveHasStatus = if (isLoggedIn) currentStatus != null else localStatus != null

    val imageRequest = remember(anime.cover) {
        ImageRequest.Builder(context)
            .data(anime.cover)
            .memoryCacheKey(anime.cover)
            .diskCacheKey(anime.cover)
            .crossfade(false)
            .build()
    }

    val effectiveStatus = if (isLoggedIn) currentStatus else localStatus

    val resolvedStatusColor = statusColor(effectiveStatus)

    val statusIndicatorColor = if (showStatusColors) resolvedStatusColor else Color.Transparent

    val displayScore = remember(anime.averageScore) {
        anime.averageScore?.let { it / 10.0 }
    }

    val episodeText = remember(anime.latestEpisode, anime.episodes) {
        when {
            anime.latestEpisode != null && anime.latestEpisode > 0 -> "Ep ${anime.latestEpisode}"
            anime.episodes > 0 -> "${anime.episodes} ${if (anime.episodes == 1) "ep" else "eps"}"
            else -> ""
        }
    }

    val buttonContainerColor = if (showStatusColors) {
        resolvedStatusColor.copy(alpha = 0.8f)
    } else {
        Color.Black.copy(alpha = 0.6f)
    }

    Column(modifier = Modifier.width(Sizes.railPosterWidth)) {
        Card(
            shape = Radius.posterShape,
            modifier = Modifier
                .height(Sizes.railPosterHeight)
                .clip(Radius.posterShape)
                .clickable(onClick = {
                    val bounds = cardBounds
                    onClick(
                        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                            AnimeCardBounds(anime.id, anime.cover, bounds)
                        } else null
                    )
                })
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
                AsyncImage(
                    model = imageRequest,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(artworkGradient(0.5f, 0f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(artworkGradient(0f, 0.85f))
                )

                if (statusIndicatorColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(statusIndicatorColor)
                    )
                }

                displayScore?.let { score ->
                    TenseiScrimChip(
                        text = "★ ${String.format(Locale.US, "%.1f", score)}",
                        color = tenseiColors.rating,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm),
                    )
                }

                if (episodeText.isNotEmpty()) {
                    TenseiScrimChip(
                        text = episodeText,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(Spacing.sm),
                    )
                }

                if (showAnimeCardButtons) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                showAnimation = true
                                if (isLoggedIn) {
                                    onBookmarkClick()
                                } else {
                                    if (localStatus != null) {
                                        onRemoveFromLocalStatus()
                                    } else {
                                        onAddToLocalPlanning()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .scale(if (showAnimation) bookmarkScale else 1f),
                            shape = RoundedCornerShape(4.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = buttonContainerColor,
                                contentColor = Color.White
                            )
                        ) {
                            AnimatedContent(
                                targetState = effectiveHasStatus,
                                transitionSpec = {
                                    (scaleIn(animationSpec = tween(200)) + fadeIn())
                                        .togetherWith(scaleOut(animationSpec = tween(200)) + fadeOut())
                                },
                                label = "bookmarkIcon"
                            ) { hasStatus ->
                                Icon(
                                    imageVector = if (hasStatus) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                                    contentDescription = if (hasStatus) "Remove from list" else "Add to planning",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        FilledTonalIconButton(
                            onClick = onPlayClick,
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(18.dp)
                            )
                        }
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

/**
 * Explore rail placeholder. Now renders the same geometry as the real rail so
 * the list doesn't jump when data lands (it previously used 120x170 with a 12dp
 * radius against a real 140x195 card with a 4dp radius).
 */
@Composable
internal fun LoadingPlaceholder() {
    TenseiRailSkeleton(itemCount = 3, showTitleBlock = true)
}

/**
 * Explore's section header.
 *
 * Delegates to [TenseiSectionHeader] so Explore rails look identical to Home
 * rails instead of being a second, plainer variant.
 */
@Composable
internal fun SectionTitle(
    title: String,
    count: Int? = null,
    isOled: Boolean = false,
    onClick: (() -> Unit)? = null,
    accent: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    TenseiSectionHeader(
        title = title,
        count = count,
        accent = accent,
        onClick = onClick,
    )
}


