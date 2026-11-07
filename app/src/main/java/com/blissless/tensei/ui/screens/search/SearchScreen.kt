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
import com.blissless.tensei.viewmodel.getAllTags
import com.blissless.tensei.viewmodel.searchAnimeAdvanced
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.addExploreAnimeToList
import com.blissless.tensei.viewmodel.removeAnimeFromList


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

    // When the user switches between simple and advanced search, the type selection (anime /
    // manga / both) is preserved rather than reset to "both". Previously, switching to simple
    // search forced searchType = "both" and hid the type switch — meaning a simple-search user
    // couldn't search anime-only or manga-only. Now the SearchTypeSegmented control is rendered
    // on both simple and advanced search screens, and the user's selection persists across the
    // mode switch.
    LaunchedEffect(advancedSearch) {
        // No-op — preserve the existing searchType selection.
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
    // PAUSED and DROPPED manga were previously missing from the manga track maps, so a
    // manga the user had put On Hold or Dropped would render in search results without a
    // status badge (while anime with the same status DID get a badge — anime's savedAnimeMap
    // at line 289 already includes all 5 statuses). Including them here makes the search
    // result badges symmetric between anime and manga.
    val mangaPausedList by viewModel.mangaPaused.collectAsState()
    val mangaDroppedList by viewModel.mangaDropped.collectAsState()

    val mangaTrackMap = remember(mangaCurrentlyReadingList, mangaPlanningToReadList, mangaCompletedList, mangaPausedList, mangaDroppedList) {
        val map = mutableMapOf<Int, String>()
        mangaCurrentlyReadingList.forEach { map[it.id] = "CURRENT" }
        mangaPlanningToReadList.forEach { map[it.id] = "PLANNING" }
        mangaCompletedList.forEach { map[it.id] = "COMPLETED" }
        mangaPausedList.forEach { map[it.id] = "PAUSED" }
        mangaDroppedList.forEach { map[it.id] = "DROPPED" }
        map
    }

    val mangaTrackByMalMap = remember(mangaCurrentlyReadingList, mangaPlanningToReadList, mangaCompletedList, mangaPausedList, mangaDroppedList) {
        val map = mutableMapOf<Int, String>()
        mangaCurrentlyReadingList.forEach { it.malId?.let { m -> map[m] = "CURRENT" } }
        mangaPlanningToReadList.forEach { it.malId?.let { m -> map[m] = "PLANNING" } }
        mangaCompletedList.forEach { it.malId?.let { m -> map[m] = "COMPLETED" } }
        // Match the mangaTrackMap fix above: include PAUSED and DROPPED so MAL-fallback
        // cards (used when AniList search/detail is down) also resolve their status badge.
        mangaPausedList.forEach { it.malId?.let { m -> map[m] = "PAUSED" } }
        mangaDroppedList.forEach { it.malId?.let { m -> map[m] = "DROPPED" } }
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
                placeholder = when (searchType) {
                    "anime" -> stringResource(R.string.search_query_hint_anime)
                    "manga" -> stringResource(R.string.search_query_hint_manga)
                    else -> stringResource(R.string.search_query_hint_both)
                },
            )

            // SearchTypeSegmented is now rendered for BOTH simple and advanced search. The user
            // can switch between anime / manga / both regardless of which search mode they're in.
            // The filter chips below remain advanced-only because they're tied to the
            // SearchFilterSheet UI that doesn't fit on the simple search screen.
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

            if (advancedSearch) {
                Spacer(Modifier.height(Spacing.sm))

                SearchFilterChipRow(
                    filters = filters,
                    activeFilterCount = activeFilterCount,
                    isOled = isOled,
                    onOpenSheet = { activeSheet = it },
                    onReset = { clearAllFilters() },
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
                            savedAnimeMap = savedAnimeMap,
                            savedAnimeByMalMap = savedAnimeByMalMap,
                            mangaTrackMap = mangaTrackMap,
                            mangaTrackByMalMap = mangaTrackByMalMap,
                            showAnimeStatusColors = showAnimeStatusColors,
                            showMangaStatusColors = showMangaStatusColors,
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
                    } else {
                        // Simple search pre-search view: surface the user's last 3 searches as
                        // tappable pills. Mirrors the advanced search's recent-searches row
                        // (SearchDiscovery) but stripped down — no trending rails, no genre
                        // chips, no clear button (the user can clear from the advanced search
                        // screen). Limited to 3 entries so the screen stays focused for users
                        // who only want to repeat a recent query.
                        val recentThree = recentSearches.take(3)
                        if (recentThree.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = Spacing.gutter,
                                    end = Spacing.gutter,
                                    top = Spacing.md,
                                    bottom = navigationBarBottomPadding() + Spacing.lg,
                                ),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                item {
                                    TenseiSectionHeader(
                                        title = stringResource(R.string.search_recent),
                                        icon = Icons.Default.History,
                                    )
                                }
                                item {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    ) {
                                        recentThree.forEach { query ->
                                            DiscoveryPill(
                                                icon = Icons.Default.History,
                                                label = query,
                                                isOled = isOled,
                                                onClick = {
                                                    filters = filters.copy(query = query)
                                                    triggerSearchOrDiscovery()
                                                },
                                            )
                                        }
                                        // Always provide a clear-all affordance so the user can
                                        // wipe their search history without having to switch to
                                        // advanced search.
                                        DiscoveryPill(
                                            icon = Icons.Default.Clear,
                                            label = stringResource(R.string.search_clear_recent),
                                            isOled = isOled,
                                            onClick = {
                                                recentStore.clear()
                                                recentSearches = emptyList()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                filteredResults.isEmpty() && filteredMangaResults.isEmpty() -> SearchEmptyState(onClearFilters = { clearAllFilters() })

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
                                statusKey = trackStatus,
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
                                        statusKey = trackStatus,
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
                                        statusKey = trackStatus,
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
                                statusKey = trackStatus,
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
                            totalChapters = rec.episodes,
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
