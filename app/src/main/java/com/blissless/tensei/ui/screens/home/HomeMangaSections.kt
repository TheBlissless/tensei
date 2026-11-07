package com.blissless.tensei.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ContinueWatchingEntry
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.dialogs.HomeAnimeStatusDialog
import com.blissless.tensei.ui.dialogs.OfflineFavoritesDialog
import com.blissless.tensei.ui.screens.manga.MangaStatusDialog
import com.blissless.tensei.ui.screens.episode.RichEpisodeScreen
import com.blissless.tensei.ui.components.HomeAnimeCardBounds
import com.blissless.tensei.ui.components.HomeAnimeHorizontalList
import com.blissless.tensei.ui.components.HomeStatusColors
import com.blissless.tensei.ui.components.LoadingSkeleton
import com.blissless.tensei.ui.components.SectionHeader
import com.blissless.tensei.ui.components.TenseiCircleAction
import com.blissless.tensei.ui.components.TenseiErrorBanner
import com.blissless.tensei.ui.components.TenseiScrimChip
import com.blissless.tensei.ui.theme.PillShape
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.theme.artworkGradient
import com.blissless.tensei.ui.components.ContinueWatchingEpisodeRow
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.components.AppIconCircle
import com.blissless.tensei.ui.components.ExploreTopBarRow
import com.blissless.tensei.ui.components.RailCardTitle
import com.blissless.tensei.ui.components.SearchCircleAction
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.ui.screens.status.StatusListScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaContinueReading
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaDropped
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.removeContinueWatchingEntry
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.updateMangaStatus
import com.blissless.tensei.util.toast
import com.blissless.tensei.util.longToast

// Manga sections shown inside the Home screen.



@Composable
internal fun MangaHorizontalRow(
    mangaList: List<MangaMedia>,
    isOled: Boolean,
    preferEnglishTitles: Boolean = true,
    onMangaClick: (MangaMedia) -> Unit,
    onMangaInfoClick: (MangaMedia) -> Unit = {}
) {
    val context = LocalContext.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.railGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(mangaList) { _, manga ->
            val displayTitle = if (preferEnglishTitles && !manga.titleEnglish.isNullOrEmpty()) manga.titleEnglish else manga.title
            val progressText = when {
                manga.totalChapters > 0 && manga.progress > 0 -> "${manga.progress} / ${manga.totalChapters}"
                manga.totalChapters > 0 -> stringResource(R.string.home_chapter_count, manga.totalChapters)
                manga.progress > 0 -> stringResource(R.string.home_chapter_number, manga.progress)
                else -> null
            }
            Column(modifier = Modifier.width(140.dp)) {
                Card(
                    // 4dp read as a near-square poster next to the Anime rail cards,
                    // which use the shared poster radius.
                    shape = Radius.posterShape,
                    modifier = Modifier
                        .height(195.dp)
                        .clip(Radius.posterShape)
                        .clickable { onMangaClick(manga) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(manga.cover)
                                .build(),
                            contentDescription = manga.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(50.dp)
                            .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))))
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(80.dp)
                            .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))

                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            if (progressText != null) {
                                Surface(
                                    shape = Radius.chipShape,
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = progressText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            FilledTonalIconButton(
                                onClick = { onMangaInfoClick(manga) },
                                modifier = Modifier.align(Alignment.TopEnd).size(30.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f), contentColor = Color.White)
                            ) { Icon(imageVector = Icons.Outlined.Info, contentDescription = stringResource(R.string.home_info_content_description), modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                RailCardTitle(title = displayTitle, modifier = Modifier.width(140.dp))
            }
        }
    }
}


@Composable
internal fun MangaContinueReadingRow(
    mangaList: List<MangaMedia>,
    isOled: Boolean,
    preferEnglishTitles: Boolean = true,
    onResumeClick: (MangaMedia) -> Unit,
    onDismissClick: (MangaMedia) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
        contentPadding = PaddingValues(horizontal = Spacing.railGutter)
    ) {
        itemsIndexed(mangaList, key = { _, manga -> "manga_continue_${manga.id}" }) { _, manga ->
            MangaContinueReadingCard(
                manga = manga,
                isOled = isOled,
                preferEnglishTitles = preferEnglishTitles,
                onResumeClick = { onResumeClick(manga) },
                onDismissClick = { onDismissClick(manga) }
            )
        }
    }
}


@Composable
private fun MangaContinueReadingCard(
    manga: MangaMedia,
    isOled: Boolean,
    preferEnglishTitles: Boolean = true,
    onResumeClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollProgress = manga.scrollProgress.coerceIn(0f, 1f)
    val hasScrollProgress = scrollProgress > 0f
    // Badge carries the chapter number and it is the last *tracked* chapter: the
    // chapter picked in the reader's chapter selection (scrollChapterNumber, which
    // can be a fraction like "20.5") never reaches the home screen.
    val trackedChapter = manga.progress.coerceAtLeast(1)
    val pageTotal = manga.currentChapterPages
    // The saved scroll fraction maps to the item index the reader restores to
    // (0-based); the on-screen page number is that index + 1.
    val currentPage = if (pageTotal > 0) {
        // scrollProgress is stored as currentScroll / maxScroll (top-of-viewport fraction).
        // Multiplying by (total - 1) and rounding to nearest recovers the best-overlap page
        // index — the page the reader was actually showing when the user exited. See
        // ReaderScreen vertical restore for the full derivation. Without this, a chapter
        // scrolled to show page 4 would display "page 3" on the card (off-by-one).
        ((scrollProgress * (pageTotal - 1) + 0.5f).toInt() + 1).coerceIn(1, pageTotal)
    } else {
        null
    }
    val overallProgress = if (manga.totalChapters > 0) {
        (manga.progress.toFloat() / manga.totalChapters.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val barFraction = if (hasScrollProgress) scrollProgress else overallProgress
    val progressLabel = when {
        currentPage != null -> stringResource(R.string.home_page_of, currentPage, pageTotal)
        hasScrollProgress -> stringResource(R.string.home_percent_read, (scrollProgress * 100).toInt())
        manga.totalChapters > 0 -> stringResource(R.string.home_chapters_progress, manga.progress, manga.totalChapters)
        else -> stringResource(R.string.home_reading)
    }
    val progressColor = if (isOled) Color.White else MaterialTheme.colorScheme.primary
    val displayMangaTitle = if (preferEnglishTitles && !manga.titleEnglish.isNullOrEmpty()) manga.titleEnglish else manga.title

    Card(
        // Same geometry and inner layout as the Anime continue-watching card;
        // both rails used to be built from their own numbers and read as
        // different card families.
        shape = Radius.cardShape,
        modifier = Modifier
            .width(Sizes.continueCardWidth)
            .height(Sizes.continueCardHeight)
            .clip(Radius.cardShape)
            .clickable { onResumeClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(manga.banner ?: manga.cover)
                    .build(),
                contentDescription = displayMangaTitle,
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
                    TenseiScrimChip(text = stringResource(R.string.home_chapter_number, trackedChapter))
                    TenseiCircleAction(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.home_remove_from_continue_reading),
                        onClick = onDismissClick,
                        size = Sizes.continueCardDismissSize,
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                }

                Column {
                    Text(
                        text = displayMangaTitle,
                        style = TenseiType.cardTitle,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

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
                                .fillMaxWidth(barFraction)
                                .clip(Radius.chipShape)
                                .background(progressColor)
                        )
                    }
                }
            }
        }
    }
}
