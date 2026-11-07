package com.blissless.tensei.ui.screens.search

import com.blissless.tensei.ui.components.TenseiEmptyState
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MediaTag
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.TenseiScrimChip
import com.blissless.tensei.ui.components.TenseiSectionHeader
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.MangaStatusLabels
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.theme.artworkGradient
import com.blissless.tensei.ui.components.navigationBarBottomPadding
import com.blissless.tensei.ui.theme.ratingColorOnArtwork
import com.blissless.tensei.ui.theme.statusColorOnArtwork
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.util.longToast
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.searchMangaAdvanced
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import java.util.Calendar
import java.util.Locale
import com.blissless.tensei.viewmodel.mangaDropped
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.ui.theme.onArtworkColor

// Discovery view shown before any search: recent searches, trending
// rails and genre pills.



// ─────────────────────────────────────────────────────────────────────────
// Discovery (no query / no filters)
// ─────────────────────────────────────────────────────────────────────────

@Composable
internal fun SearchDiscovery(
    searchType: String,
    isOled: Boolean,
    recentSearches: List<String>,
    trendingAnime: List<ExploreAnime>,
    trendingManga: List<MangaExploreMedia>,
    preferEnglishTitles: Boolean,
    // Status tracking state — passed through from the parent so the trending cards on the
    // discovery view can render the same status badges as the actual search-result cards.
    // Without these, the discovery view's cards always showed statusLabel = null (no badge)
    // even when the user had the media in their lists.
    savedAnimeMap: Map<Int, String>,
    savedAnimeByMalMap: Map<Int, String>,
    mangaTrackMap: Map<Int, String>,
    mangaTrackByMalMap: Map<Int, String>,
    showAnimeStatusColors: Boolean,
    showMangaStatusColors: Boolean,
    onQuerySelect: (String) -> Unit,
    onClearRecent: () -> Unit,
    onGenreSelect: (String) -> Unit,
    onAnimeClick: (ExploreAnime) -> Unit,
    onMangaClick: (MangaExploreMedia) -> Unit,
) {
    val showAnimeTrending = searchType != "manga" && trendingAnime.isNotEmpty()
    val showMangaTrending = searchType != "anime" && trendingManga.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = navigationBarBottomPadding() + Spacing.lg),
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                TenseiSectionHeader(
                    title = stringResource(R.string.search_recent),
                    icon = Icons.Default.History,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    recentSearches.forEach { query ->
                        DiscoveryPill(icon = Icons.Default.History, label = query, isOled = isOled, onClick = { onQuerySelect(query) })
                    }
                    DiscoveryPill(
                        icon = Icons.Default.Clear,
                        label = stringResource(R.string.search_clear_recent),
                        isOled = isOled,
                        onClick = onClearRecent,
                    )
                }
            }
            item { Spacer(Modifier.height(Spacing.section)) }
        }

        if (showAnimeTrending) {
            item {
                TenseiSectionHeader(
                    title = stringResource(R.string.search_trending_anime),
                    icon = Icons.Default.Whatshot,
                    accent = tenseiColors.airing,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(trendingAnime, key = { it.id }) { anime ->
                        // Look up the user's list status for this anime (by AniList id, with
                        // MAL-id fallback for MAL-source results). Mirrors the actual search
                        // result grid path so the discovery trending cards render the same
                        // status badges.
                        val trackStatus = savedAnimeMap[anime.id] ?: savedAnimeByMalMap[anime.malId ?: anime.id]
                        Box(modifier = Modifier.width(132.dp)) {
                            SearchPosterCard(
                                title = animeTitle(anime, preferEnglishTitles),
                                cover = anime.cover,
                                score = anime.averageScore?.let { it / 10.0 },
                                meta = animeMeta(anime),
                                statusLabel = resolvedStatusLabel(trackStatus, isManga = false, show = showAnimeStatusColors),
                                statusTint = resolvedStatusTint(trackStatus, showAnimeStatusColors),
                                statusKey = trackStatus,
                                onClick = { onAnimeClick(anime) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.section)) }
        }

        if (showMangaTrending) {
            item {
                TenseiSectionHeader(
                    title = stringResource(R.string.search_trending_manga),
                    icon = Icons.Default.Whatshot,
                    accent = tenseiColors.favorite,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(trendingManga, key = { it.id }) { manga ->
                        val title = if (preferEnglishTitles && !manga.title.english.isNullOrBlank()) manga.title.english
                        else manga.title.romaji ?: stringResource(R.string.explore_unknown)
                        // Look up the user's list status for this manga (by AniList id, with
                        // MAL-id fallback). Mirrors the actual search result grid path so the
                        // discovery trending cards render the same status badges.
                        val trackStatus = mangaTrackMap[manga.id] ?: mangaTrackByMalMap[manga.idMal]
                        Box(modifier = Modifier.width(132.dp)) {
                            SearchPosterCard(
                                title = title,
                                cover = manga.coverImage?.large ?: manga.coverImage?.medium ?: "",
                                score = manga.averageScore?.let { it / 10.0 },
                                meta = mangaMeta(manga),
                                statusLabel = resolvedStatusLabel(trackStatus, isManga = true, show = showMangaStatusColors),
                                statusTint = resolvedStatusTint(trackStatus, showMangaStatusColors),
                                statusKey = trackStatus,
                                onClick = { onMangaClick(manga) },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.section)) }
        }

        item {
            TenseiSectionHeader(
                title = stringResource(R.string.search_browse_genres),
                icon = Icons.Default.Tune,
                accent = tenseiAccentColor(),
            )
        }
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ALL_GENRES.forEach { genre ->
                    DiscoveryPill(label = genre, isOled = isOled, onClick = { onGenreSelect(genre) })
                }
            }
        }
    }
}
