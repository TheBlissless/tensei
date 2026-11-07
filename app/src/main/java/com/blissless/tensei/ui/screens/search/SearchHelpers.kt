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

// Small label/formatting helpers for search result cards.



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
internal fun resolvedStatusLabel(status: String?, isManga: Boolean, show: Boolean): String? {
    // The label is returned whenever the media has a tracked status, INDEPENDENTLY of the
    // show-status-colors setting. The setting only gates the colored stripe at the top of
    // the card (via resolvedStatusTint returning Color.Transparent when off); the chip
    // itself still renders with a white fallback color so the user can always see which of
    // their tracked media a search result is. Previously this helper returned null when
    // `show == false`, which made the chip disappear entirely along with the stripe.
    if (status == null) return null
    return (if (isManga) MangaStatusLabels[status] else StatusLabels[status]) ?: status
}


/** Status tint for a tracked title, or transparent when it should not be drawn. */
@Composable
internal fun resolvedStatusTint(status: String?, show: Boolean): Color =
    if (show && status != null) statusColorOnArtwork(status) else Color.Transparent


internal fun animeTitle(anime: ExploreAnime, preferEnglishTitles: Boolean): String = when {
    preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
    anime.title.isNotEmpty() -> anime.title
    !anime.titleEnglish.isNullOrEmpty() -> anime.titleEnglish
    else -> "Unknown"
}


@Composable
internal fun animeMeta(anime: ExploreAnime): String = listOfNotNull(
    anime.format?.replace("_", " "),
    anime.year?.toString(),
    if (anime.episodes > 0 && anime.format?.uppercase() != "MOVIE") {
        stringResource(R.string.search_episode_count, anime.episodes)
    } else null,
).joinToString("  \u2022  ")


@Composable
internal fun mangaMeta(manga: MangaExploreMedia): String = listOfNotNull(
    manga.format?.replace("_", " "),
    (manga.seasonYear ?: manga.startDate?.year)?.toString(),
    manga.chapters?.takeIf { it > 0 }?.let { stringResource(R.string.search_chapter_count, it) },
).joinToString("  \u2022  ")
