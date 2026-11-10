package com.blissless.tensei.ui.screens.status

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Info
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
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.ui.components.HomeAnimeCardBounds
import com.blissless.tensei.ui.components.HomeStatusColors
import com.blissless.tensei.ui.components.animeEpisodeProgressText
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.theme.tenseiColors
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Anime/manga cards for the status list screen.


@Composable
internal fun StatusListAnimeCard(
    anime: AnimeMedia,
    listType: String,
    showStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    onClick: (HomeAnimeCardBounds?) -> Unit,
    onPlayClick: () -> Unit,
    onInfoClick: (HomeAnimeCardBounds?) -> Unit,
    onStatusClick: () -> Unit
) {
    val context = LocalContext.current
    var cardBounds by remember { mutableStateOf<android.graphics.RectF?>(null) }
    val statusColor = HomeStatusColors.getColor(listType)

    val progressText = animeEpisodeProgressText(anime, listType)

    val imageRequest = remember(anime.cover) {
        ImageRequest.Builder(context)
            .data(anime.cover)
            .memoryCacheKey(anime.cover)
            .diskCacheKey(anime.cover)
            .crossfade(false)
            .build()
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

                // Top Row: Episode Counter (left) + Rating (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (anime.averageScore != null && anime.averageScore > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "★ ${String.format(java.util.Locale.US, "%.1f", anime.averageScore / 10.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tenseiColors.rating,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (showStatusColors) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(top = 52.dp)
                            .background(statusColor)
                    )
                }

                // Bottom Row: Edit Status Button (left) + Info Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onStatusClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                             contentDescription = stringResource(R.string.lists_change_status),
                             modifier = Modifier.size(18.dp)
                         )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalIconButton(
                        onClick = { onInfoClick(cardBounds?.let { HomeAnimeCardBounds(anime.id, anime.cover, it) }) },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                             imageVector = Icons.Outlined.Info,
                             contentDescription = stringResource(R.string.lists_info),
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
internal fun StatusListMangaCard(
    manga: MangaMedia,
    listType: String,
    showStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    onContinueReadingClick: () -> Unit,
    onStatusClick: () -> Unit
) {
    val context = LocalContext.current
    val statusColor = HomeStatusColors.getColor(listType)
    val displayMangaTitle = if (preferEnglishTitles && !manga.titleEnglish.isNullOrEmpty()) manga.titleEnglish else manga.title
    val progressText = when (listType) {
        "CURRENT" -> if (manga.totalChapters > 0) "${manga.progress} / ${manga.totalChapters}" else "Ch. ${manga.progress}"
        "COMPLETED" -> if (manga.totalChapters > 0) "${manga.totalChapters} ch." else "Ch. ${manga.progress}"
        else -> if (manga.totalChapters > 0) "${manga.totalChapters} ch." else null
    }
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
                AsyncImage(
                    model = ImageRequest.Builder(context).data(manga.cover).build(),
                    contentDescription = displayMangaTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )

                // Top Row: Chapter Counter (left) + Rating (right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (progressText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (manga.averageScore != null && manga.averageScore > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "★ ${String.format(java.util.Locale.US, "%.1f", manga.averageScore / 10.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tenseiColors.rating,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (showStatusColors) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .padding(top = 52.dp)
                            .background(statusColor)
                    )
                }

                // Bottom Row: Edit Status Button (left) + Info Button (right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onStatusClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkAdd,
                             contentDescription = stringResource(R.string.lists_change_status),
                             modifier = Modifier.size(18.dp)
                         )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalIconButton(
                        onClick = onInfoClick,
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                             imageVector = Icons.Outlined.Info,
                             contentDescription = stringResource(R.string.lists_info),
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
                text = displayMangaTitle,
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
