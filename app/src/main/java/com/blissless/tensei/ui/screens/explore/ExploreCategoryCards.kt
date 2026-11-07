package com.blissless.tensei.ui.screens.explore

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.R
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.ui.components.AnimeCardBounds
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.screens.status.SortOption
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.ratingColorOnArtwork
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.blissless.tensei.ui.components.easeOutCubic

// Poster cards (with shared-element transform) for the explore category
// lists.


@Composable
internal fun rememberCategoryCardTransform(
    index: Int,
    gridState: LazyGridState,
    isScrolling: Boolean,
    cinematicProgress: Float,
    translationYOffset: Float
): Modifier {
    val staggerDelay = minOf(index, 20) * 30f
    val staggerMs = staggerDelay / 1000f
    val rawProgress = ((cinematicProgress - staggerMs) / (1f - staggerMs))
    val easedProgress = easeOutCubic(rawProgress.coerceAtLeast(0f).coerceAtMost(1f))

    val introScale = 0.3f + easedProgress * 0.7f
    val introAlpha = easedProgress.coerceAtLeast(0f)
    val introTranslationY = translationYOffset * (1f - easedProgress)

    val centerOffset by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val itemInfo = visibleItems.find { it.index == index }
            if (itemInfo != null) {
                val itemCenter = itemInfo.offset.y + itemInfo.size.height / 2
                val screenCenter = (layoutInfo.viewportSize.height / 2).toFloat()
                (itemCenter - screenCenter) / screenCenter
            } else 0f
        }
    }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isScrolling) centerOffset.coerceIn(-2f, 2f) else 0f,
        animationSpec = if (isScrolling) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "centerOffset"
    )

    val scrollScale = 1f - (animatedOffset.absoluteValue * 0.15f).coerceAtMost(0.15f)
    val scrollAlpha = 1f - (animatedOffset.absoluteValue * 0.3f).coerceAtMost(0.4f)
    val scrollParallax = animatedOffset * 20f
    val finalScale = scrollScale * introScale
    val finalAlpha = (scrollAlpha * introAlpha).coerceIn(0f, 1f)
    val finalTranslationY = scrollParallax + introTranslationY

    return Modifier.graphicsLayer {
        scaleX = finalScale
        scaleY = finalScale
        alpha = finalAlpha
        translationY = finalTranslationY
    }
}


@Composable
internal fun ExploreCategoryAnimeCard(
    anime: ExploreAnime,
    currentStatus: String?,
    showStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    onClick: (AnimeCardBounds?) -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    var cardBounds by remember { mutableStateOf<android.graphics.RectF?>(null) }
    val hasStatus = currentStatus != null
    // No status -> no top stripe; a null status would otherwise resolve to the theme outline.
    val listStatusColor = if (showStatusColors && hasStatus) statusColor(currentStatus) else Color.Transparent

    val episodeText = remember(anime.latestEpisode, anime.episodes) {
        when {
            anime.latestEpisode != null && anime.latestEpisode > 0 -> context.getString(R.string.explore_episode_short, anime.latestEpisode)
            anime.episodes > 0 -> context.getString(
                if (anime.episodes == 1) R.string.explore_episode_count_one else R.string.explore_episode_count_other,
                anime.episodes
            )
            else -> ""
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

    val buttonContainerColor = if (hasStatus && showStatusColors) {
        listStatusColor.copy(alpha = 0.8f)
    } else {
        Color.Black.copy(alpha = 0.6f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = Radius.posterShape,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(140f / 195f)
                .clip(Radius.posterShape)
                .clickable {
                    onClick(
                        if (cardBounds != null && cardBounds!!.width() > 0 && cardBounds!!.height() > 0) {
                            AnimeCardBounds(anime.id, anime.cover, cardBounds!!)
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
                AsyncImage(
                    model = imageRequest,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                // Top Row: Episode Counter (left) + Info Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    if (episodeText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = episodeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { onClick(cardBounds?.let { AnimeCardBounds(anime.id, anime.cover, it) }) },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.explore_info),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showStatusColors && listStatusColor != Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(top = 52.dp)
                            .background(listStatusColor)
                    )
                }

                // Bottom Row: Save/Bookmark Button (left) + Play/Info Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = buttonContainerColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (hasStatus) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                            contentDescription = if (hasStatus) stringResource(R.string.explore_in_list) else stringResource(R.string.explore_add_to_list),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalIconButton(
                        onClick = { onClick(null) },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.explore_open),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        val displayTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(top = 6.dp)
        ) {
            Text(
                text = displayTitle,
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
internal fun ExploreCategoryMangaCard(
    manga: MangaExploreMedia,
    preferEnglishTitles: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    val title = if (preferEnglishTitles && !manga.title.english.isNullOrBlank()) manga.title.english
               else manga.title.romaji ?: stringResource(R.string.explore_unknown)
    val coverUrl = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: manga.coverImage?.medium ?: ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = Radius.posterShape,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(140f / 195f)
                .clip(Radius.posterShape)
                .clickable(onClick = onClick)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(coverUrl).build(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) { Text(stringResource(R.string.explore_na), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )
                manga.chapters?.let { ch ->
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = stringResource(R.string.explore_chapter_short, ch),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                manga.averageScore?.let { score ->
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "★ ${String.format(Locale.US, "%.1f", score / 10.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ratingColorOnArtwork(),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bottom Row: Bookmark Button (left) + Open Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                            contentDescription = stringResource(R.string.explore_add_to_list),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalIconButton(
                        onClick = onClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.explore_open),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).padding(top = 6.dp)
        ) {
            Text(
                text = title,
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
