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

// Search screen palette helpers.



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
internal fun searchContainerColor(isOled: Boolean, raised: Boolean = false): Color {
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
internal fun searchSheetColor(isOled: Boolean): Color =
    if (isOled) MaterialTheme.colorScheme.surfaceContainerHigh
    else MaterialTheme.colorScheme.surfaceContainerLow


/**
 * Theme-aware accent for the "Browse Genres" section header (and any other accent that
 * should follow the same rule). Mirrors the player's `playerAccentColor()` logic so the
 * search screen's accent matches the scrubber / floating controls in the player:
 *   - monochrome (full)        → neutral grey (Color(0xFFCFCFCF), same as player)
 *   - semi-monochrome           → the app's accent blue (Color(0xFF60A5FA), same as player)
 *   - Material                  → MaterialTheme.colorScheme.primary when it's light enough
 *                                  to read on a surface, else falls back to the accent blue
 *
 * Local to the search screen to avoid coupling the search package to the player package —
 * the player's `playerAccentColor()` is `internal` and conceptually belongs to playback UI,
 * not search. If this logic ever needs to be shared more broadly it should be promoted to
 * a `tenseiAccentColor()` helper in the theme package.
 */
@Composable
internal fun tenseiAccentColor(): Color {
    val colors = tenseiColors
    return when {
        colors.monochrome -> Color(0xFFCFCFCF)
        colors.semiMonochrome -> Color(0xFF60A5FA)
        else -> {
            val primary = MaterialTheme.colorScheme.primary
            if (primary.luminance() > 0.35f) primary else Color(0xFF60A5FA)
        }
    }
}
