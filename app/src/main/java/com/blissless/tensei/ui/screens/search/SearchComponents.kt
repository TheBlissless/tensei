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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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

// Reusable search UI components: header, type toggle, filter chips,
// results grid, poster card, skeleton, empty state.



// ─────────────────────────────────────────────────────────────────────────
// Header + controls
// ─────────────────────────────────────────────────────────────────────────

@Composable
internal fun SearchHeader(
    query: String,
    isOled: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
    onSearchAction: () -> Unit,
    focusRequester: FocusRequester,
    // searchType-aware placeholder — switches between "Search anime..." / "Search manga..."
    // / "Search anime & manga..." based on the active tab. Defaulted to the legacy
    // search_query_hint string so any caller that doesn't pass it still compiles.
    placeholder: String = "",
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
                                    text = placeholder.ifEmpty { stringResource(R.string.search_query_hint) },
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
internal fun SearchTypeSegmented(
    selected: String,
    isOled: Boolean,
    onSelect: (String) -> Unit,
) {
    // Each option now carries a leading icon so the user can identify Anime / Manga / Both
    // at a glance without reading the label. The selected segment's container colour
    // animates between transparent and primary via animateColorAsState for a smooth
    // transition (was previously an instant colour flip).
    //
    // Icon choices match the bottom navigation bar (Icons.Rounded.Movie for anime,
    // Icons.AutoMirrored.Rounded.MenuBook for manga) so the user sees the same icon for "Anime" / "Manga"
    // everywhere in the app — search, nav bar, and any other surface. "Both" uses the
    // Layers icon because there's no canonical "both" icon in the nav bar.
    val options = listOf(
        Triple("anime", R.string.search_tab_anime, Icons.Rounded.Movie),
        Triple("manga", R.string.search_tab_manga, Icons.AutoMirrored.Rounded.MenuBook),
        Triple("both", R.string.search_tab_both, Icons.Default.Layers),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter),
        shape = Radius.controlShape,
        color = searchContainerColor(isOled),
    ) {
        Row(modifier = Modifier.padding(Spacing.xs)) {
            options.forEach { (value, labelRes, icon) ->
                val isSelected = selected == value
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "segmentedContainer",
                )
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = Radius.chipShape,
                    color = containerColor,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelect(value) },
                ) {
                    Row(
                        modifier = Modifier.height(Sizes.controlHeight).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
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
internal fun SearchFilterChipRow(
    filters: SearchFilters,
    activeFilterCount: Int,
    isOled: Boolean,
    onOpenSheet: (SearchSheet) -> Unit,
    // Wired to clearAllFilters() at the call site. Previously the Reset chip called
    // onOpenSheet(SearchSheet.NONE), which is a no-op — the when(activeSheet) block
    // handles NONE as `Unit`, so the button did nothing. Now it actually clears the
    // active filters (sort / format / status / genres / tags / year) while preserving
    // the current query, then the calling composable re-runs the search via
    // clearAllFilters → resetResultState.
    onReset: () -> Unit,
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
                // Reset chip — wired to onReset (clears all filters). Previously this called
                // onOpenSheet(SearchSheet.NONE) which is a no-op. Now it actually clears the
                // active filters when tapped. Visually distinct as a destructive action: the
                // border + icon + text all use tenseiColors.destructive so the user knows this
                // wipes the active filter selection. The container stays transparent so it
                // doesn't compete visually with the active FilterChips next to it.
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, tenseiColors.destructive.copy(alpha = 0.5f)),
                    onClick = onReset,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = tenseiColors.destructive,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = stringResource(R.string.search_reset),
                            style = MaterialTheme.typography.labelLarge,
                            color = tenseiColors.destructive,
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
internal fun ResultsGrid(
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
internal fun SearchPosterCard(
    title: String,
    cover: String,
    score: Double?,
    meta: String,
    statusLabel: String?,
    statusTint: Color,
    // The raw status key ("CURRENT" / "PLANNING" / "COMPLETED" / "PAUSED" / "DROPPED").
    // Used to compute the chip's text color theme-awarely: full monochrome collapses to
    // white (onArtworkColor), semi-monochrome and material keep the proper status hue.
    // This is decoupled from `statusTint` (which is Color.Transparent when the show-status-
    // colors SETTING is OFF) so the chip keeps its color even when the setting is off —
    // the setting only gates the 3dp line at the top of the card, not the chip.
    statusKey: String? = null,
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

            // The status chip in the top-left corner is ALWAYS rendered when the card's media
            // is in the user's list (statusText != null), independent of the show-status-colors
            // setting. When the setting is OFF, statusTint arrives as Color.Transparent from
            // resolvedStatusTint, so we fall back to onArtworkColor() (white) so the chip text
            // is still readable against the cover art.
            //
            // The 3dp status stripe at TopCenter, on the other hand, only renders when the
            // show-status-colors setting is ON (statusTint != Color.Transparent) — that gate
            // is kept because the stripe is purely a color indication and a transparent stripe
            // would be a no-op anyway.
            val statusText = statusLabel
            val hasStatusText = statusText != null
            val hasStatusColor = hasStatusText && statusTint != Color.Transparent
            // Chip color is computed theme-awarely from the raw statusKey, NOT from statusTint.
            // statusColorOnArtwork already handles the monochrome theme internally:
            //   - full MONOCHROME → onArtworkColor() (white)
            //   - SEMI_MONOCHROME / MATERIAL → the proper status hue (green for Completed etc.)
            // This means the chip always shows the correct color regardless of the show-status-
            // colors SETTING (which only gates the 3dp line at TopCenter via hasStatusColor).
            val statusChipColor = if (statusKey != null) statusColorOnArtwork(statusKey) else onArtworkColor()

            if (hasStatusColor) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(statusTint),
                )
            }
            if (hasStatusText) {
                // Status badge in the LEFT corner (TopStart) with the same Spacing.sm padding
                // as the rating badge uses at TopEnd — symmetric corners, the status chip on
                // the left and the rating chip on the right.
                TenseiScrimChip(
                    text = statusText,
                    color = statusChipColor,
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.sm),
                )
            }

            score?.let {
                TenseiScrimChip(
                    text = "\u2605 ${String.format(Locale.US, "%.1f", it)}",
                    color = ratingColorOnArtwork(),
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm),
                )
            }

            typeMarker?.let { isAnime ->
                TenseiScrimChip(
                    text = stringResource(if (isAnime) R.string.search_tab_anime else R.string.search_tab_manga),
                    color = Color.White,
                    leadingIcon = if (isAnime) Icons.Rounded.Movie else Icons.AutoMirrored.Rounded.MenuBook,
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


// SearchArtworkChip has been removed — it was a private duplicate of TenseiScrimChip
// (in ui/components/TenseiTopBar.kt). The three call sites in SearchPosterCard above now
// call TenseiScrimChip directly, which was extended with an optional leadingIcon parameter
// to support the type-marker chip (Anime/Manga icon next to the label). All other existing
// TenseiScrimChip callers (HomeComponents, CharacterStaffScreen, RelatedMediaCard) are
// unaffected — the leadingIcon parameter defaults to null.

@Composable
internal fun SearchResultsSkeleton() {
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
internal fun SearchEmptyState(onClearFilters: () -> Unit) {
    // Uses the canonical TenseiEmptyState component (matching the rest of the app's empty
    // states) instead of a hand-rolled Box + Column + Icon + Text stack. The action button
    // lets the user clear any active filters in one tap when a search returns no results.
    TenseiEmptyState(
        icon = Icons.Default.Search,
        title = stringResource(R.string.search_no_results),
        message = stringResource(R.string.search_try_adjusting),
        actionLabel = stringResource(R.string.search_clear_filters),
        onAction = onClearFilters,
        modifier = Modifier.fillMaxSize(),
    )
}


@Composable
internal fun DiscoveryPill(
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
