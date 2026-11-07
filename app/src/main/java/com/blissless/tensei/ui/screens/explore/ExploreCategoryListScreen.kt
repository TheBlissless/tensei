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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCategoryListScreen(
    title: String,
    icon: ImageVector,
    animeList: List<ExploreAnime> = emptyList(),
    mangaList: List<MangaExploreMedia> = emptyList(),
    isManga: Boolean = false,
    animeStatusMap: Map<Int, String> = emptyMap(),
    showStatusColors: Boolean = true,
    preferEnglishTitles: Boolean = true,
    onAnimeClick: (ExploreAnime, AnimeCardBounds?) -> Unit = { _, _ -> },
    onBookmarkClick: (ExploreAnime) -> Unit = {},
    onMangaClick: (MangaExploreMedia) -> Unit = {},
    onMangaBookmarkClick: (MangaExploreMedia) -> Unit = {},
    onBackClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val iconTint = MaterialTheme.colorScheme.primary
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    BackHandler(onBack = onBackClick)

    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "offsetY"
    )

    var selectedSort by remember { mutableStateOf(SortOption.LAST_UPDATED) }
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val displayAnimeList = remember(animeList, searchQuery, selectedSort) {
        val filtered = if (searchQuery.isBlank()) animeList
        else animeList.filter {
            val t = if (preferEnglishTitles && !it.titleEnglish.isNullOrEmpty()) it.titleEnglish else it.title
            t.contains(searchQuery, ignoreCase = true)
        }
        when (selectedSort) {
            SortOption.LAST_UPDATED -> filtered
            SortOption.ALPHABETICAL_A_Z -> filtered.sortedBy {
                (if (preferEnglishTitles && !it.titleEnglish.isNullOrEmpty()) it.titleEnglish else it.title).lowercase()
            }
            SortOption.ALPHABETICAL_Z_A -> filtered.sortedByDescending {
                (if (preferEnglishTitles && !it.titleEnglish.isNullOrEmpty()) it.titleEnglish else it.title).lowercase()
            }
            SortOption.YEAR_NEWEST -> filtered.sortedByDescending { it.year ?: Int.MIN_VALUE }
            SortOption.YEAR_OLDEST -> filtered.sortedBy { it.year ?: Int.MAX_VALUE }
            SortOption.EPISODES_MOST -> filtered.sortedByDescending {
                if (it.episodes > 0) it.episodes else it.latestEpisode ?: 0
            }
            SortOption.EPISODES_LEAST -> filtered.sortedBy {
                if (it.episodes > 0) it.episodes else it.latestEpisode ?: 0
            }
            SortOption.SCORE_HIGH -> filtered.sortedByDescending { it.averageScore ?: Int.MIN_VALUE }
            SortOption.SCORE_LOW -> filtered.sortedBy { it.averageScore ?: Int.MAX_VALUE }
        }
    }

    val displayMangaList = remember(mangaList, searchQuery, selectedSort) {
        fun titleOf(m: MangaExploreMedia): String =
            (if (preferEnglishTitles && !m.title.english.isNullOrBlank()) m.title.english else m.title.romaji) ?: context.getString(R.string.explore_unknown)
        val filtered = if (searchQuery.isBlank()) mangaList
        else mangaList.filter { titleOf(it).contains(searchQuery, ignoreCase = true) }
        when (selectedSort) {
            SortOption.LAST_UPDATED -> filtered
            SortOption.ALPHABETICAL_A_Z -> filtered.sortedBy { titleOf(it).lowercase() }
            SortOption.ALPHABETICAL_Z_A -> filtered.sortedByDescending { titleOf(it).lowercase() }
            SortOption.YEAR_NEWEST -> filtered.sortedByDescending {
                it.seasonYear ?: it.startDate?.year ?: Int.MIN_VALUE
            }
            SortOption.YEAR_OLDEST -> filtered.sortedBy {
                it.seasonYear ?: it.startDate?.year ?: Int.MAX_VALUE
            }
            SortOption.EPISODES_MOST -> filtered.sortedByDescending { it.chapters ?: it.volumes ?: 0 }
            SortOption.EPISODES_LEAST -> filtered.sortedBy { it.chapters ?: it.volumes ?: 0 }
            SortOption.SCORE_HIGH -> filtered.sortedByDescending { it.averageScore ?: Int.MIN_VALUE }
            SortOption.SCORE_LOW -> filtered.sortedBy { it.averageScore ?: Int.MAX_VALUE }
        }
    }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val scrollToTop = {
        scope.launch { gridState.animateScrollToItem(0) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .graphicsLayer {
                translationY = animatedOffset
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onDismiss()
                        }
                        offsetY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        offsetY = (offsetY + dragAmount).coerceAtLeast(0f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { scrollToTop() }
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isManga) stringResource(R.string.explore_count_manga, mangaList.size) else stringResource(R.string.explore_count_anime, animeList.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { focusManager.clearFocus(); showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.explore_sort),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.explore_search_in_list)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.explore_clear_search)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if ((if (isManga) mangaList.size else animeList.size) == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isManga) stringResource(R.string.explore_no_manga_in_list) else stringResource(R.string.explore_no_anime_in_list),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                val translationYOffset = with(density) { (-30).dp.toPx() }

                val isScrolling by remember {
                    derivedStateOf { gridState.isScrollInProgress }
                }

                val cinematicProgress = rememberCinematicAnimation("exploreCategory_$title",
                    isVisible = true,
                    playOncePerSession = true
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    contentPadding = PaddingValues(Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.railItem),
                    verticalArrangement = Arrangement.spacedBy(Spacing.gutter)
                ) {
                    if (isManga) {
                        itemsIndexed(items = displayMangaList, key = { _, manga -> "${title}_${manga.id}" }) { index, manga ->
                            val transform = rememberCategoryCardTransform(
                                index, gridState, isScrolling, cinematicProgress, translationYOffset
                            )
                            Box(modifier = Modifier.animateItem().then(transform)) {
                                ExploreCategoryMangaCard(
                                    manga = manga,
                                    preferEnglishTitles = preferEnglishTitles,
                                    onClick = { onMangaClick(manga) },
                                    onBookmarkClick = { onMangaBookmarkClick(manga) }
                                )
                            }
                        }
                    } else {
                        itemsIndexed(items = displayAnimeList, key = { _, anime -> "${title}_${anime.id}" }) { index, anime ->
                            val transform = rememberCategoryCardTransform(
                                index, gridState, isScrolling, cinematicProgress, translationYOffset
                            )
                            Box(modifier = Modifier.animateItem().then(transform)) {
                                ExploreCategoryAnimeCard(
                                    anime = anime,
                                    currentStatus = animeStatusMap[anime.id],
                                    showStatusColors = showStatusColors,
                                    preferEnglishTitles = preferEnglishTitles,
                                    onClick = { bounds -> onAnimeClick(anime, bounds) },
                                    onBookmarkClick = { onBookmarkClick(anime) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        stringResource(R.string.explore_sort_by),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                    SortOption.entries.forEach { option ->
                        val isSelected = option == selectedSort
                        Surface(
                            onClick = { focusManager.clearFocus(); selectedSort = option; showSortSheet = false; scrollToTop() },
                            color = if (isSelected) iconTint.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = iconTint)
                                )
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    option.displayLabel(isManga),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
