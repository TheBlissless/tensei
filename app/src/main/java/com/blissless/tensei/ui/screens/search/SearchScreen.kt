package com.blissless.tensei.ui.screens.search

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
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
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MediaTag
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.components.SkeletonBlock
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

val ALL_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
    "Mystery", "Romance", "Sci-Fi", "Slice of Life", "Sports",
    "Thriller", "Supernatural", "Music", "Psychological",
    "Military", "Mecha", "School", "Seinen", "Shoujo", "Shounen"
)

val ALL_FORMATS = listOf("TV", "TV_SHORT", "MOVIE", "OVA", "ONA", "SPECIAL", "MUSIC")
val ALL_STATUSES = listOf("FINISHED", "RELEASING", "NOT_YET_RELEASED", "CANCELLED", "HIATUS")
val ALL_SORTS = listOf(
    "POPULARITY_DESC" to R.string.search_sort_popularity,
    "SCORE_DESC" to R.string.search_sort_score,
    "TRENDING_DESC" to R.string.search_sort_trending,
    "UPDATED_AT_DESC" to R.string.search_sort_updated,
    "START_DATE_DESC" to R.string.search_sort_release_date,
    "FAVOURITES_DESC" to R.string.search_sort_favorites,
    "EPISODES_DESC" to R.string.search_sort_episode_count,
    "TITLE_ROMAJI" to R.string.search_sort_title
)

data class SearchFilters(
    val query: String = "",
    val genres: Set<String> = emptySet(),
    val tags: List<String> = emptyList(),
    val format: String? = null,
    val status: String? = null,
    val seasonYear: String = "",
    val sort: String = "POPULARITY_DESC"
)

/**
 * Heterogeneous search result wrapper used when `searchType == "both"`.
 * Lets the unified grid render anime and manga side-by-side in a single,
 * evenly-spaced grid.
 */
sealed class UnifiedSearchItem {
    abstract val id: Int

    data class Anime(val item: ExploreAnime) : UnifiedSearchItem() {
        override val id: Int get() = item.id
    }

    data class Manga(val item: MangaExploreMedia) : UnifiedSearchItem() {
        override val id: Int get() = item.id
    }

    fun stableKey(): String = when (this) {
        is Anime -> "anime_$id"
        is Manga -> "manga_$id"
    }
}

/** Which filter bottom sheet is currently open. */
private enum class SearchSheet { NONE, SORT, FORMAT, STATUS, GENRE, TAG, YEAR }

@OptIn(UnstableApi::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    isOled: Boolean,
    isLoggedIn: Boolean,
    preferEnglishTitles: Boolean,
    advancedSearch: Boolean = false,
    showAnimeStatusColors: Boolean = true,
    showMangaStatusColors: Boolean = true,
    currentlyWatching: List<AnimeMedia>,
    planningToWatch: List<AnimeMedia>,
    completed: List<AnimeMedia>,
    onHold: List<AnimeMedia>,
    dropped: List<AnimeMedia>,
    localAnimeStatus: Map<Int, LocalAnimeEntry>,
    favoriteIds: Set<Int>,
    onClose: () -> Unit,
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit,
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onViewAllCast: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllStaff: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRelations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRecommendations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onNoExtension: () -> Unit = {},
    settingsReturnVersion: Int = 0,
    onMangaClick: (MangaExploreMedia) -> Unit = {},
    onAnimeDetailMangaClick: (MangaMedia) -> Unit = {}
) {
    var searchType by remember { mutableStateOf("both") }

    // Non-advanced mode always searches both anime and manga and hides the
    // type switch, filters and discovery UI.
    LaunchedEffect(advancedSearch) {
        if (!advancedSearch) searchType = "both"
    }
    var filters by remember { mutableStateOf(SearchFilters()) }
    var results by remember { mutableStateOf<List<ExploreAnime>>(emptyList()) }
    var mangaResults by remember { mutableStateOf<List<MangaExploreMedia>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    // Anime and manga pages are tracked independently so "both" mode can fetch
    // and merge the next page of each type without losing state.
    var currentAnimePage by remember { mutableIntStateOf(1) }
    var currentMangaPage by remember { mutableIntStateOf(1) }
    var hasMoreAnime by remember { mutableStateOf(true) }
    var hasMoreManga by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    // Unified "has more" flag — mirrors whichever type(s) are active for the
    // current searchType. Updated at the end of performSearch().
    var hasMore by remember { mutableStateOf(true) }

    var selectedAnime by remember { mutableStateOf<ExploreAnime?>(null) }
    var firstAnime by remember { mutableStateOf<ExploreAnime?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    var allTags by remember { mutableStateOf<List<MediaTag>>(emptyList()) }
    var tagsLoading by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf(SearchSheet.NONE) }

    var trendingAnime by remember { mutableStateOf<List<ExploreAnime>>(emptyList()) }
    var trendingManga by remember { mutableStateOf<List<MangaExploreMedia>>(emptyList()) }

    val context = LocalContext.current
    val recentStore = remember(context) { RecentSearchStore(context) }
    var recentSearches by remember { mutableStateOf(recentStore.load()) }

    LaunchedEffect(Unit) {
        tagsLoading = true
        allTags = viewModel.getAllTags()
        tagsLoading = false
    }

    // Trending is only needed by the discovery state; fetch both types once.
    LaunchedEffect(Unit) {
        runCatching {
            if (trendingAnime.isEmpty()) {
                trendingAnime = viewModel.searchAnimeAdvanced(
                    search = null, sort = "TRENDING_DESC", page = 1, perPage = 12
                )
            }
        }
        runCatching {
            if (trendingManga.isEmpty()) {
                trendingManga = viewModel.searchMangaAdvanced(
                    search = null, sort = "TRENDING_DESC", page = 1, perPage = 12
                )
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val gridState = rememberLazyGridState()

    val savedAnimeMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) {
        val map = mutableMapOf<Int, String>()
        currentlyWatching.forEach { map[it.id] = "CURRENT" }
        planningToWatch.forEach { map[it.id] = "PLANNING" }
        completed.forEach { map[it.id] = "COMPLETED" }
        onHold.forEach { map[it.id] = "PAUSED" }
        dropped.forEach { map[it.id] = "DROPPED" }
        localAnimeStatus.forEach { (id, entry) -> if (!map.containsKey(id)) map[id] = entry.status }
        map
    }

    // MAL-fallback results (AniList search/detail down) carry the MAL id as their own id, so the
    // id-keyed maps above miss. Build malId-keyed mirrors of the AniList-sourced lists so those
    // cards still resolve their status badge.
    val savedAnimeByMalMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped) {
        val map = mutableMapOf<Int, String>()
        currentlyWatching.forEach { it.malId?.let { m -> map[m] = "CURRENT" } }
        planningToWatch.forEach { it.malId?.let { m -> map[m] = "PLANNING" } }
        completed.forEach { it.malId?.let { m -> map[m] = "COMPLETED" } }
        onHold.forEach { it.malId?.let { m -> map[m] = "PAUSED" } }
        dropped.forEach { it.malId?.let { m -> map[m] = "DROPPED" } }
        map
    }

    // ── Manga tracking state ───────────────────────────────────────────
    val mangaCurrentlyReadingList by viewModel.mangaCurrentlyReading.collectAsState()
    val mangaPlanningToReadList by viewModel.mangaPlanningToRead.collectAsState()
    val mangaCompletedList by viewModel.mangaCompleted.collectAsState()

    val mangaTrackMap = remember(mangaCurrentlyReadingList, mangaPlanningToReadList, mangaCompletedList) {
        val map = mutableMapOf<Int, String>()
        mangaCurrentlyReadingList.forEach { map[it.id] = "CURRENT" }
        mangaPlanningToReadList.forEach { map[it.id] = "PLANNING" }
        mangaCompletedList.forEach { map[it.id] = "COMPLETED" }
        map
    }

    val mangaTrackByMalMap = remember(mangaCurrentlyReadingList, mangaPlanningToReadList, mangaCompletedList) {
        val map = mutableMapOf<Int, String>()
        mangaCurrentlyReadingList.forEach { it.malId?.let { m -> map[m] = "CURRENT" } }
        mangaPlanningToReadList.forEach { it.malId?.let { m -> map[m] = "PLANNING" } }
        mangaCompletedList.forEach { it.malId?.let { m -> map[m] = "COMPLETED" } }
        map
    }

    val savedAnimeProgressMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) {
        val map = mutableMapOf<Int, Int>()
        currentlyWatching.forEach { if (it.progress > 0) map[it.id] = it.progress }
        planningToWatch.forEach { if (it.progress > 0) map[it.id] = it.progress }
        completed.forEach { if (it.progress > 0) map[it.id] = it.progress }
        onHold.forEach { if (it.progress > 0) map[it.id] = it.progress }
        dropped.forEach { if (it.progress > 0) map[it.id] = it.progress }
        localAnimeStatus.forEach { (id, entry) -> if (entry.progress > 0 && !map.containsKey(id)) map[id] = entry.progress }
        map
    }

    val savedAnimeProgressByMalMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped) {
        val map = mutableMapOf<Int, Int>()
        currentlyWatching.forEach { if (it.progress > 0) it.malId?.let { m -> map[m] = it.progress } }
        planningToWatch.forEach { if (it.progress > 0) it.malId?.let { m -> map[m] = it.progress } }
        completed.forEach { if (it.progress > 0) it.malId?.let { m -> map[m] = it.progress } }
        onHold.forEach { if (it.progress > 0) it.malId?.let { m -> map[m] = it.progress } }
        dropped.forEach { if (it.progress > 0) it.malId?.let { m -> map[m] = it.progress } }
        map
    }

    var listVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) { listVersion++ }

    val activeFilterCount = listOfNotNull(
        filters.genres.takeIf { it.isNotEmpty() }?.let { 1 },
        filters.tags.takeIf { it.isNotEmpty() }?.let { 1 },
        filters.format,
        filters.status,
        filters.seasonYear.takeIf { it.isNotBlank() }?.let { 1 },
        filters.sort.takeIf { it != "POPULARITY_DESC" }?.let { 1 }
    ).size

    fun resetResultState() {
        results = emptyList()
        mangaResults = emptyList()
        currentAnimePage = 1
        currentMangaPage = 1
        hasMoreAnime = true
        hasMoreManga = true
        hasMore = true
        hasSearched = false
    }

    fun commitRecent(query: String) {
        recentSearches = recentStore.add(query)
    }

    fun closeAndCommit() {
        if (filters.query.isNotBlank()) commitRecent(filters.query)
        keyboardController?.hide()
        onClose()
    }

    /**
     * Run an initial search (page 1) for whichever type(s) are active.
     * Replaces existing results for each fetched type.
     */
    fun performSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            isSearching = true
            hasSearched = true
            val seasonYearVal = filters.seasonYear.toIntOrNull()
            val genreList = filters.genres.toList().ifEmpty { null }
            val tagList = filters.tags.toList().ifEmpty { null }

            // ── Anime fetch ──────────────────────────────────────────────
            if (searchType == "anime" || searchType == "both") {
                val newResults = viewModel.searchAnimeAdvanced(
                    search = filters.query.ifBlank { null },
                    genres = genreList,
                    tags = tagList,
                    format = filters.format,
                    status = filters.status,
                    seasonYear = seasonYearVal,
                    sort = filters.sort,
                    isAdult = null,
                    page = 1,
                    perPage = 30
                )
                results = newResults
                hasMoreAnime = newResults.size >= 30
                currentAnimePage = 1
            } else {
                results = emptyList()
                hasMoreAnime = false
            }

            // ── Manga fetch ─────────────────────────────────────────────
            if (searchType == "manga" || searchType == "both") {
                // Plain text search with the default sort prefers SEARCH_MATCH for
                // relevance; an explicit sort is always honored.
                val effectiveMangaSort = if (
                    filters.query.isNotBlank() && filters.sort == "POPULARITY_DESC"
                ) "SEARCH_MATCH" else filters.sort

                val newMangaResults = viewModel.searchMangaAdvanced(
                    search = filters.query.ifBlank { null },
                    genres = genreList ?: emptyList(),
                    format = filters.format,
                    status = filters.status,
                    sort = effectiveMangaSort,
                    page = 1,
                    perPage = 30
                )
                mangaResults = newMangaResults
                hasMoreManga = newMangaResults.size >= 30
                currentMangaPage = 1
            } else {
                mangaResults = emptyList()
                hasMoreManga = false
            }

            hasMore = when (searchType) {
                "anime" -> hasMoreAnime
                "manga" -> hasMoreManga
                "both" -> hasMoreAnime || hasMoreManga
                else -> false
            }

            isSearching = false
        }
    }

    /**
     * When the query is blank and no filter is active there is nothing to
     * search — fall back to the discovery state instead of running an empty
     * query that would just return the popular page.
     */
    fun triggerSearchOrDiscovery() {
        if (filters.query.isBlank() && activeFilterCount == 0) {
            searchJob?.cancel()
            isSearching = false
            resetResultState()
        } else {
            performSearch()
        }
    }

    /** Fetch the next page of whichever active type(s) still have more results. */
    fun loadMore() {
        scope.launch {
            isLoadingMore = true
            val seasonYearVal = filters.seasonYear.toIntOrNull()
            val genreList = filters.genres.toList().ifEmpty { null }
            val tagList = filters.tags.toList().ifEmpty { null }

            if ((searchType == "anime" || searchType == "both") && hasMoreAnime) {
                val nextPage = currentAnimePage + 1
                val newResults = viewModel.searchAnimeAdvanced(
                    search = filters.query.ifBlank { null },
                    genres = genreList,
                    tags = tagList,
                    format = filters.format,
                    status = filters.status,
                    seasonYear = seasonYearVal,
                    sort = filters.sort,
                    isAdult = null,
                    page = nextPage,
                    perPage = 30
                )
                results = results + newResults
                hasMoreAnime = newResults.size >= 30
                currentAnimePage = nextPage
            }

            if ((searchType == "manga" || searchType == "both") && hasMoreManga) {
                val effectiveMangaSort = if (
                    filters.query.isNotBlank() && filters.sort == "POPULARITY_DESC"
                ) "SEARCH_MATCH" else filters.sort

                val nextPage = currentMangaPage + 1
                val newMangaResults = viewModel.searchMangaAdvanced(
                    search = filters.query.ifBlank { null },
                    genres = genreList ?: emptyList(),
                    format = filters.format,
                    status = filters.status,
                    sort = effectiveMangaSort,
                    page = nextPage,
                    perPage = 30
                )
                mangaResults = mangaResults + newMangaResults
                hasMoreManga = newMangaResults.size >= 30
                currentMangaPage = nextPage
            }

            hasMore = when (searchType) {
                "anime" -> hasMoreAnime
                "manga" -> hasMoreManga
                "both" -> hasMoreAnime || hasMoreManga
                else -> false
            }

            isLoadingMore = false
        }
    }

    // Re-run the search whenever a filter (not the query) changes. The first
    // composition is skipped so an untouched screen still shows discovery.
    var filtersInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(
        filters.genres, filters.tags,
        filters.format, filters.status, filters.seasonYear,
        filters.sort
    ) {
        if (filtersInitialized) {
            triggerSearchOrDiscovery()
        } else {
            filtersInitialized = true
        }
    }

    // Debounced auto-search as the query text or the active type changes.
    LaunchedEffect(filters.query, searchType) {
        delay(350.milliseconds)
        triggerSearchOrDiscovery()
    }

    fun clearAllFilters() {
        filters = SearchFilters(query = filters.query)
        resetResultState()
    }

    LaunchedEffect(Unit) {
        delay(200.milliseconds)
        focusRequester.requestFocus()
    }

    val filteredResults = results
    val filteredMangaResults = mangaResults

    // Interleave anime and manga (anime first, then manga) so neither type
    // dominates a long stretch of the "both" grid.
    val unifiedResults = remember(filteredResults, filteredMangaResults) {
        val animeItems = filteredResults.map { UnifiedSearchItem.Anime(it) }
        val mangaItems = filteredMangaResults.map { UnifiedSearchItem.Manga(it) }
        val merged = mutableListOf<UnifiedSearchItem>()
        val maxLen = maxOf(animeItems.size, mangaItems.size)
        for (i in 0 until maxLen) {
            if (i < animeItems.size) merged.add(animeItems[i])
            if (i < mangaItems.size) merged.add(mangaItems[i])
        }
        merged
    }

    BackHandler { closeAndCommit() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHeader(
                query = filters.query,
                isOled = isOled,
                onQueryChange = { filters = filters.copy(query = it) },
                onClear = { filters = filters.copy(query = "") },
                onCancel = { closeAndCommit() },
                onSearchAction = {
                    if (filters.query.isNotBlank()) commitRecent(filters.query)
                    keyboardController?.hide()
                },
                focusRequester = focusRequester,
            )

            if (advancedSearch) {
                SearchTypeSegmented(
                    selected = searchType,
                    isOled = isOled,
                    onSelect = { value ->
                        if (value != searchType) {
                            searchType = value
                            resetResultState()
                            triggerSearchOrDiscovery()
                        }
                    }
                )

                Spacer(Modifier.height(Spacing.sm))

                SearchFilterChipRow(
                    filters = filters,
                    activeFilterCount = activeFilterCount,
                    isOled = isOled,
                    onOpenSheet = { activeSheet = it },
                )

                Spacer(Modifier.height(Spacing.sm))
            }

            when {
                isSearching -> SearchResultsSkeleton()

                !hasSearched -> {
                    if (advancedSearch) {
                        SearchDiscovery(
                            searchType = searchType,
                            isOled = isOled,
                            recentSearches = recentSearches,
                            trendingAnime = trendingAnime,
                            trendingManga = trendingManga,
                            preferEnglishTitles = preferEnglishTitles,
                            onQuerySelect = { filters = filters.copy(query = it) },
                            onClearRecent = {
                                recentStore.clear()
                                recentSearches = emptyList()
                            },
                            onGenreSelect = { genre ->
                                filters = filters.copy(genres = filters.genres + genre)
                            },
                            onAnimeClick = { anime ->
                                keyboardController?.hide()
                                selectedAnime = anime
                                firstAnime = anime
                                showDetailDialog = true
                            },
                            onMangaClick = onMangaClick,
                        )
                    }
                }

                filteredResults.isEmpty() && filteredMangaResults.isEmpty() -> SearchEmptyState()

                searchType == "manga" -> {
                    ResultsGrid(
                        gridState = gridState,
                        hasMore = hasMoreManga && filteredMangaResults.size >= 30,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = { loadMore() },
                    ) {
                        items(filteredMangaResults, key = { it.id }) { manga ->
                            val title = if (preferEnglishTitles && !manga.title.english.isNullOrBlank()) manga.title.english
                            else manga.title.romaji ?: stringResource(R.string.explore_unknown)
                            val cover = manga.coverImage?.large ?: manga.coverImage?.medium ?: ""
                            val trackStatus = mangaTrackMap[manga.id] ?: mangaTrackByMalMap[manga.idMal]
                            SearchPosterCard(
                                title = title,
                                cover = cover,
                                score = manga.averageScore?.let { it / 10.0 },
                                meta = mangaMeta(manga),
                                statusLabel = resolvedStatusLabel(trackStatus, isManga = true, show = showMangaStatusColors),
                                statusTint = resolvedStatusTint(trackStatus, showMangaStatusColors),
                                onClick = { commitRecent(filters.query); onMangaClick(manga) }
                            )
                        }
                    }
                }

                searchType == "both" -> {
                    ResultsGrid(
                        gridState = gridState,
                        hasMore = hasMore && unifiedResults.isNotEmpty(),
                        isLoadingMore = isLoadingMore,
                        onLoadMore = { loadMore() },
                    ) {
                        items(unifiedResults, key = { it.stableKey() }) { item ->
                            when (item) {
                                is UnifiedSearchItem.Anime -> {
                                    val a = item.item
                                    val trackStatus = savedAnimeMap[a.id] ?: savedAnimeByMalMap[a.malId ?: a.id]
                                    SearchPosterCard(
                                        title = animeTitle(a, preferEnglishTitles),
                                        cover = a.cover,
                                        score = a.averageScore?.let { it / 10.0 },
                                        meta = animeMeta(a),
                                        statusLabel = resolvedStatusLabel(trackStatus, isManga = false, show = showAnimeStatusColors),
                                        statusTint = resolvedStatusTint(trackStatus, showAnimeStatusColors),
                                        typeMarker = true,
                                        onClick = {
                                            commitRecent(filters.query)
                                            keyboardController?.hide()
                                            selectedAnime = a
                                            firstAnime = a
                                            showDetailDialog = true
                                        }
                                    )
                                }
                                is UnifiedSearchItem.Manga -> {
                                    val m = item.item
                                    val trackStatus = mangaTrackMap[m.id] ?: mangaTrackByMalMap[m.idMal]
                                    val title = if (preferEnglishTitles && !m.title.english.isNullOrBlank()) m.title.english
                                    else m.title.romaji ?: stringResource(R.string.explore_unknown)
                                    SearchPosterCard(
                                        title = title,
                                        cover = m.coverImage?.large ?: m.coverImage?.medium ?: "",
                                        score = m.averageScore?.let { it / 10.0 },
                                        meta = mangaMeta(m),
                                        statusLabel = resolvedStatusLabel(trackStatus, isManga = true, show = showMangaStatusColors),
                                        statusTint = resolvedStatusTint(trackStatus, showMangaStatusColors),
                                        typeMarker = false,
                                        onClick = { commitRecent(filters.query); onMangaClick(m) }
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    ResultsGrid(
                        gridState = gridState,
                        hasMore = hasMoreAnime && filteredResults.size >= 30,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = { loadMore() },
                    ) {
                        items(filteredResults, key = { it.id }) { anime ->
                            val trackStatus = savedAnimeMap[anime.id] ?: savedAnimeByMalMap[anime.malId ?: anime.id]
                            SearchPosterCard(
                                title = animeTitle(anime, preferEnglishTitles),
                                cover = anime.cover,
                                score = anime.averageScore?.let { it / 10.0 },
                                meta = animeMeta(anime),
                                statusLabel = resolvedStatusLabel(trackStatus, isManga = false, show = showAnimeStatusColors),
                                statusTint = resolvedStatusTint(trackStatus, showAnimeStatusColors),
                                onClick = {
                                    commitRecent(filters.query)
                                    keyboardController?.hide()
                                    selectedAnime = anime
                                    firstAnime = anime
                                    showDetailDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailDialog && selectedAnime != null) {
        val currentStatus by remember(listVersion, selectedAnime!!.id) { derivedStateOf { savedAnimeMap[selectedAnime!!.id] ?: savedAnimeByMalMap[selectedAnime!!.malId ?: selectedAnime!!.id] } }
        val currentProgress by remember(listVersion, selectedAnime!!.id) { derivedStateOf { savedAnimeProgressMap[selectedAnime!!.id] ?: savedAnimeProgressByMalMap[selectedAnime!!.malId ?: selectedAnime!!.id] } }
        val isAnimeFavorite by remember(listVersion, favoriteIds, selectedAnime!!.id) { derivedStateOf { favoriteIds.contains(selectedAnime!!.id) } }

        DetailedAnimeScreen(
            anime = selectedAnime!!.toDetailedAnimeData(),
            viewModel = viewModel,
            isOled = isOled,
            settingsReturnVersion = settingsReturnVersion,
            isLoggedIn = isLoggedIn,
            currentStatus = currentStatus,
            currentProgress = currentProgress,
            isFavorite = isAnimeFavorite,
            onDismiss = {
                if (firstAnime != null && selectedAnime!!.id != firstAnime!!.id) {
                    scope.launch {
                        val detailedData = viewModel.fetchDetailedAnimeData(firstAnime!!.id)
                        if (detailedData != null) {
                            selectedAnime = ExploreAnime(
                                id = detailedData.id, title = detailedData.title,
                                titleEnglish = detailedData.titleEnglish, cover = detailedData.cover,
                                banner = detailedData.banner, episodes = detailedData.episodes,
                                latestEpisode = detailedData.latestEpisode, averageScore = detailedData.averageScore,
                                genres = detailedData.genres, year = detailedData.year, format = detailedData.format
                            )
                        }
                    }
                } else { showDetailDialog = false; firstAnime = null }
            },
            onPlayEpisode = { episode, _ ->
                onPlayEpisode(AnimeMedia(id = selectedAnime!!.id, title = selectedAnime!!.title, cover = selectedAnime!!.cover, banner = selectedAnime!!.banner, progress = 0, totalEpisodes = selectedAnime!!.episodes, latestEpisode = selectedAnime!!.latestEpisode, status = "", averageScore = selectedAnime!!.averageScore, genres = selectedAnime!!.genres, listStatus = "", listEntryId = 0), episode, null)
                showDetailDialog = false
            },
            onUpdateStatus = { status -> if (status != null) viewModel.addExploreAnimeToList(selectedAnime!!, status) },
            onRemove = { viewModel.removeAnimeFromList(selectedAnime!!.id); showDetailDialog = false },
            onRelationClick = { relation ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (relation.format == null || relation.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = relation.id,
                            title = relation.title,
                            cover = relation.cover,
                            totalChapters = 0,
                            averageScore = relation.averageScore,
                            format = relation.format
                        )
                    )
                } else {
                    scope.launch {
                        val detailedData = viewModel.fetchDetailedAnimeData(relation.id)
                        if (detailedData != null) {
                            selectedAnime = ExploreAnime(id = relation.id, title = detailedData.title, titleEnglish = detailedData.titleEnglish, cover = detailedData.cover, banner = detailedData.banner, episodes = detailedData.episodes, latestEpisode = detailedData.latestEpisode, averageScore = detailedData.averageScore, genres = detailedData.genres, year = detailedData.year, format = detailedData.format)
                        } else context.toast(context.getString(R.string.search_anime_not_found))
                    }
                }
            },
            onRecommendationClick = { rec ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (rec.format == null || rec.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = rec.id,
                            title = rec.title,
                            cover = rec.cover,
                            totalChapters = rec.episodes ?: 0,
                            averageScore = rec.averageScore,
                            format = rec.format
                        )
                    )
                } else {
                    scope.launch {
                        val detailedData = viewModel.fetchDetailedAnimeData(rec.id)
                        if (detailedData != null) {
                            selectedAnime = ExploreAnime(id = rec.id, title = detailedData.title, titleEnglish = detailedData.titleEnglish, cover = detailedData.cover, banner = detailedData.banner, episodes = detailedData.episodes, latestEpisode = detailedData.latestEpisode, averageScore = detailedData.averageScore, genres = detailedData.genres, year = detailedData.year, format = detailedData.format)
                        } else context.toast(context.getString(R.string.search_anime_not_found))
                    }
                }
            },
            onCharacterClick = onCharacterClick, onStaffClick = onStaffClick,
            onViewAllCast = { onViewAllCast(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllStaff = { onViewAllStaff(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllRelations = { animeId, title, titleEnglish -> onViewAllRelations(animeId, title, titleEnglish) },
            onViewAllRecommendations = { animeId, title, titleEnglish -> onViewAllRecommendations(animeId, title, titleEnglish) },
            onNoExtension = {
                onNoExtension()
            }
        )
    }

    // ── Filter bottom sheets ───────────────────────────────────────────
    when (activeSheet) {
        SearchSheet.SORT -> SingleSelectFilterSheet(
            title = stringResource(R.string.search_sort),
            options = ALL_SORTS.map { it.first to stringResource(it.second) },
            selectedValue = filters.sort,
            anyLabel = stringResource(R.string.search_sort_popularity),
            isOled = isOled,
            onSelect = { filters = filters.copy(sort = it ?: "POPULARITY_DESC") },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.FORMAT -> SingleSelectFilterSheet(
            title = stringResource(R.string.search_format),
            options = ALL_FORMATS.map { it to it.replace("_", " ").replaceFirstChar { c -> c.uppercase() } },
            selectedValue = filters.format,
            anyLabel = stringResource(R.string.search_any),
            isOled = isOled,
            onSelect = { filters = filters.copy(format = it) },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.STATUS -> SingleSelectFilterSheet(
            title = stringResource(R.string.search_status),
            options = ALL_STATUSES.map { it to it.replace("_", " ").replaceFirstChar { c -> c.uppercase() } },
            selectedValue = filters.status,
            anyLabel = stringResource(R.string.search_any),
            isOled = isOled,
            onSelect = { filters = filters.copy(status = it) },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.GENRE -> MultiSelectFilterSheet(
            title = stringResource(R.string.search_select_genres),
            hint = stringResource(R.string.search_placeholder_short),
            options = ALL_GENRES,
            selected = filters.genres,
            isOled = isOled,
            onToggle = { genre ->
                filters = if (genre in filters.genres) filters.copy(genres = filters.genres - genre)
                else filters.copy(genres = filters.genres + genre)
            },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.TAG -> MultiSelectFilterSheet(
            title = stringResource(R.string.search_select_tags),
            hint = stringResource(R.string.search_tags_hint),
            options = remember(allTags) { allTags.sortedBy { it.name }.map { it.name } },
            selected = filters.tags.toSet(),
            isOled = isOled,
            onToggle = { tag ->
                filters = if (tag in filters.tags) filters.copy(tags = filters.tags - tag)
                else filters.copy(tags = filters.tags + tag)
            },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.YEAR -> YearFilterSheet(
            current = filters.seasonYear,
            isOled = isOled,
            onSelect = { filters = filters.copy(seasonYear = it) },
            onDismiss = { activeSheet = SearchSheet.NONE }
        )
        SearchSheet.NONE -> Unit
    }

    // Keep the unused-state warning quiet for the legacy flag.
    if (tagsLoading) { /* tags load in the background; nothing to render */ }
}

// ─────────────────────────────────────────────────────────────────────────
// Theming helpers
// ─────────────────────────────────────────────────────────────────────────

/**
 * Filled container tone for the search screen (search field, segmented track,
 * filter chips, discovery pills).
 *
 * OLED flattens [androidx.compose.material3.ColorScheme.surfaceVariant] to pure
 * black, so an element filled with it would vanish into the OLED background.
 * OLED therefore uses the neutral surface-container tones instead, picking a
 * lighter one for elements drawn on top of a sheet ([raised]); every other theme
 * keeps the app-standard `surfaceVariant` fill.
 */
@Composable
private fun searchContainerColor(isOled: Boolean, raised: Boolean = false): Color {
    val scheme = MaterialTheme.colorScheme
    return if (isOled) {
        if (raised) scheme.surfaceContainerHighest else scheme.surfaceContainerHigh
    } else {
        scheme.surfaceVariant
    }
}

/**
 * Container tone for a filter bottom sheet. Distinct from
 * [searchContainerColor] so elements drawn inside a sheet (search field, chips)
 * still read as raised against it in both OLED and the light/dark themes.
 */
@Composable
private fun searchSheetColor(isOled: Boolean): Color =
    if (isOled) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceContainerLow

// ─────────────────────────────────────────────────────────────────────────
// Header + controls
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchHeader(
    query: String,
    isOled: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onSearchAction: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = Spacing.xs, end = Spacing.gutter, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(Sizes.iconButton),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.search_close),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        Surface(
            modifier = Modifier.weight(1f).height(50.dp),
            shape = Radius.controlShape,
            color = searchContainerColor(isOled),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = Spacing.md, end = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_query_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                AnimatedVisibility(visible = query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.search_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTypeSegmented(
    selected: String,
    isOled: Boolean,
    onSelect: (String) -> Unit,
) {
    val options = listOf(
        "anime" to R.string.search_tab_anime,
        "manga" to R.string.search_tab_manga,
        "both" to R.string.search_tab_both,
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter),
        shape = Radius.controlShape,
        color = searchContainerColor(isOled),
    ) {
        Row(modifier = Modifier.padding(Spacing.xs)) {
            options.forEach { (value, labelRes) ->
                val isSelected = selected == value
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = Radius.chipShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelect(value) },
                ) {
                    Box(modifier = Modifier.height(38.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterChipRow(
    filters: SearchFilters,
    activeFilterCount: Int,
    isOled: Boolean,
    onOpenSheet: (SearchSheet) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Spacing.gutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            FilterChip(
                label = stringResource(
                    ALL_SORTS.find { it.first == filters.sort }?.second ?: R.string.search_sort_popularity
                ),
                active = filters.sort != "POPULARITY_DESC",
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.SORT) },
            )
        }
        item {
            FilterChip(
                label = filters.format?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                    ?: stringResource(R.string.search_format),
                active = filters.format != null,
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.FORMAT) },
            )
        }
        item {
            FilterChip(
                label = filters.status?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                    ?: stringResource(R.string.search_status),
                active = filters.status != null,
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.STATUS) },
            )
        }
        item {
            FilterChip(
                label = stringResource(R.string.search_genres),
                active = filters.genres.isNotEmpty(),
                count = filters.genres.size,
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.GENRE) },
            )
        }
        item {
            FilterChip(
                label = stringResource(R.string.search_tags),
                active = filters.tags.isNotEmpty(),
                count = filters.tags.size,
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.TAG) },
            )
        }
        item {
            FilterChip(
                label = filters.seasonYear.ifBlank { stringResource(R.string.search_year) },
                active = filters.seasonYear.isNotBlank(),
                isOled = isOled,
                onClick = { onOpenSheet(SearchSheet.YEAR) },
            )
        }
        if (activeFilterCount > 0) {
            item {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { onOpenSheet(SearchSheet.NONE) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = stringResource(R.string.search_reset),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.width(Spacing.xs)) }
    }
}

/** Pill-shaped filter chip that opens a bottom sheet. */
@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    isOled: Boolean,
    onClick: () -> Unit,
    count: Int? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = CircleShape,
        color = if (active) accent.copy(alpha = 0.16f) else searchContainerColor(isOled),
        border = BorderStroke(
            1.dp,
            if (active) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.md,
                end = Spacing.sm,
                top = Spacing.sm,
                bottom = Spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null && count > 0) {
                Spacer(Modifier.width(Spacing.xs))
                Surface(shape = CircleShape, color = accent) {
                    Text(
                        text = count.toString(),
                        style = TenseiType.badge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.xxs))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Results
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultsGrid(
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.sm,
            bottom = navigationBarBottomPadding() + Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
        if (hasMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onLoadMore) {
                            Text(stringResource(R.string.search_load_more), color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(Spacing.xs))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPosterCard(
    title: String,
    cover: String,
    score: Double?,
    meta: String,
    statusLabel: String?,
    statusTint: Color,
    onClick: () -> Unit,
    typeMarker: Boolean? = null,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(Radius.posterShape),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cover)
                    .memoryCacheKey(cover)
                    .diskCacheKey(cover)
                    .crossfade(false)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(artworkGradient(0.45f, 0f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(artworkGradient(0f, 0.72f)),
            )

            // A status is only shown when the card actually has one and the user
            // enabled the setting (callers pass a transparent tint otherwise), so
            // untracked cards get neither a stripe nor a chip — never a grey line.
            val statusText = statusLabel
            val hasStatus = statusText != null && statusTint != Color.Transparent

            if (hasStatus) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(statusTint),
                )
                SearchArtworkChip(
                    text = statusText,
                    contentColor = statusTint,
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.sm),
                )
            }

            score?.let {
                SearchArtworkChip(
                    text = "\u2605 ${String.format(Locale.US, "%.1f", it)}",
                    contentColor = ratingColorOnArtwork(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm),
                )
            }

            typeMarker?.let { isAnime ->
                SearchArtworkChip(
                    text = stringResource(if (isAnime) R.string.search_tab_anime else R.string.search_tab_manga),
                    contentColor = Color.White,
                    leadingIcon = if (isAnime) Icons.Default.PlayArrow else Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.sm),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = title,
            style = TenseiType.cardTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = meta,
                style = TenseiType.cardMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchArtworkChip(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        shape = Radius.chipShape,
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(Spacing.xxs))
            }
            Text(
                text = text,
                style = TenseiType.badge,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SearchResultsSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.fillMaxSize().shimmer(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.sm,
            bottom = navigationBarBottomPadding() + Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        userScrollEnabled = false,
    ) {
        items(8) {
            Column {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                    cornerRadius = Radius.poster,
                )
                Spacer(Modifier.height(Spacing.sm))
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.85f).height(12.dp))
                Spacer(Modifier.height(Spacing.xs))
                SkeletonBlock(modifier = Modifier.width(52.dp).height(9.dp))
            }
        }
    }
}

@Composable
private fun SearchEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 96.dp, start = Spacing.xxxl, end = Spacing.xxxl),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.search_no_results),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.search_try_adjusting),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Discovery (no query / no filters)
// ─────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchDiscovery(
    searchType: String,
    isOled: Boolean,
    recentSearches: List<String>,
    trendingAnime: List<ExploreAnime>,
    trendingManga: List<MangaExploreMedia>,
    preferEnglishTitles: Boolean,
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
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(trendingAnime, key = { it.id }) { anime ->
                        Box(modifier = Modifier.width(132.dp)) {
                            SearchPosterCard(
                                title = animeTitle(anime, preferEnglishTitles),
                                cover = anime.cover,
                                score = anime.averageScore?.let { it / 10.0 },
                                meta = animeMeta(anime),
                                statusLabel = null,
                                statusTint = Color.Transparent,
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
                        Box(modifier = Modifier.width(132.dp)) {
                            SearchPosterCard(
                                title = title,
                                cover = manga.coverImage?.large ?: manga.coverImage?.medium ?: "",
                                score = manga.averageScore?.let { it / 10.0 },
                                meta = mangaMeta(manga),
                                statusLabel = null,
                                statusTint = Color.Transparent,
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

@Composable
private fun DiscoveryPill(
    label: String,
    isOled: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Surface(
        shape = CircleShape,
        color = searchContainerColor(isOled),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Filter sheets
// ─────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleSelectFilterSheet(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String?,
    anyLabel: String,
    isOled: Boolean,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = searchSheetColor(isOled),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xxl)) {
            SheetTitle(title)
            SheetOptionRow(
                label = anyLabel,
                selected = selectedValue == null,
                onClick = { onSelect(null); onDismiss() },
            )
            options.forEach { (value, label) ->
                SheetOptionRow(
                    label = label,
                    selected = selectedValue == value,
                    onClick = { onSelect(value); onDismiss() },
                )
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectFilterSheet(
    title: String,
    hint: String,
    options: List<String>,
    selected: Set<String>,
    isOled: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(options, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) options else options.filter { it.contains(q, ignoreCase = true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = searchSheetColor(isOled),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xxl)) {
            SheetTitle(
                title = title,
                subtitle = if (selected.isNotEmpty()) {
                    stringResource(R.string.search_tags_selected_count, selected.size)
                } else null,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .height(40.dp),
                shape = Radius.chipShape,
                color = searchContainerColor(isOled, raised = true),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = hint,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.search_no_matches),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        filtered.forEach { option ->
                            SelectableChip(
                                label = option,
                                selected = option in selected,
                                isOled = isOled,
                                onClick = { onToggle(option) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearFilterSheet(
    current: String,
    isOled: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(current) }
    val thisYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(thisYear) { (thisYear downTo thisYear - 25).toList() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = searchSheetColor(isOled),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xxl)) {
            SheetTitle(title = stringResource(R.string.search_year))
            BasicTextField(
                value = text,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .height(44.dp)
                    .clip(Radius.chipShape)
                    .background(searchContainerColor(isOled, raised = true))
                    .padding(horizontal = Spacing.md),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_year_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            Spacer(Modifier.height(Spacing.lg))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                years.forEach { year ->
                    SelectableChip(
                        label = year.toString(),
                        selected = text == year.toString(),
                        isOled = isOled,
                        onClick = { text = year.toString() },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                TextButton(
                    onClick = { onSelect(""); onDismiss() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.search_any))
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = Radius.controlShape,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onSelect(text); onDismiss() },
                ) {
                    Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.search_action),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    isOled: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.16f) else searchContainerColor(isOled, raised = true),
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SheetTitle(
    title: String,
    subtitle: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.md)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────

/**
 * Badge text for a tracked title, or `null` when the card should show no status
 * indicator at all — either the user disabled status colours or the title has no
 * list status. Returning `null` (rather than a grey fallback label) is what keeps
 * untracked cards free of a status stripe/chip.
 */
@Composable
private fun resolvedStatusLabel(status: String?, isManga: Boolean, show: Boolean): String? {
    if (!show || status == null) return null
    return (if (isManga) MangaStatusLabels[status] else StatusLabels[status]) ?: status
}

/** Status tint for a tracked title, or transparent when it should not be drawn. */
@Composable
private fun resolvedStatusTint(status: String?, show: Boolean): Color =
    if (show && status != null) statusColorOnArtwork(status) else Color.Transparent

private fun animeTitle(anime: ExploreAnime, preferEnglishTitles: Boolean): String = when {
    preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
    anime.title.isNotEmpty() -> anime.title
    !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
    else -> "Unknown"
}

@Composable
private fun animeMeta(anime: ExploreAnime): String = listOfNotNull(
    anime.format?.replace("_", " "),
    anime.year?.toString(),
    if (anime.episodes > 0 && anime.format?.uppercase() != "MOVIE") {
        stringResource(R.string.search_episode_count, anime.episodes)
    } else null,
).joinToString("  \u2022  ")

@Composable
private fun mangaMeta(manga: MangaExploreMedia): String = listOfNotNull(
    manga.format?.replace("_", " "),
    (manga.seasonYear ?: manga.startDate?.year)?.toString(),
    manga.chapters?.takeIf { it > 0 }?.let { stringResource(R.string.search_chapter_count, it) },
).joinToString("  \u2022  ")
