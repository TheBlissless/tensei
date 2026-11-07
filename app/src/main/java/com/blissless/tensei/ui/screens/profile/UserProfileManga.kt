package com.blissless.tensei.ui.screens.profile

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.blissless.tensei.viewmodel.malMangaFavorites
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.blissless.tensei.viewmodel.toggleMalFavoriteById
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.util.LocaleDate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.data.models.UserAnimeStats
import com.blissless.tensei.ui.components.SectionHeaderSkeleton
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.TenseiDisclosureHeader
import com.blissless.tensei.ui.components.TenseiTopBar
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.themeStatusColors
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.MangaStatusLabels
import com.blissless.tensei.ui.theme.tenseiColors
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.fetchAniListFavorites
import com.blissless.tensei.viewmodel.fetchUserActivity
import com.blissless.tensei.viewmodel.fetchUserStats
import com.blissless.tensei.viewmodel.loadAniListFavoritesFromStorage
import com.blissless.tensei.viewmodel.toggleAniListFavorite
import com.blissless.tensei.viewmodel.mangaContinueReading
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.viewmodel.mangaDropped
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaActivityMedia
import com.blissless.tensei.data.models.MangaTitle
import com.blissless.tensei.data.models.MediaCoverImage
import com.blissless.tensei.data.models.MangaFuzzyDate
import com.blissless.tensei.data.models.MangaUserProfile
import com.blissless.tensei.viewmodel.mangaFavorites
import com.blissless.tensei.viewmodel.mangaActivity
import com.blissless.tensei.viewmodel.mangaUserProfile
import com.blissless.tensei.viewmodel.fetchMangaUserProfile
import com.blissless.tensei.viewmodel.toggleMangaFavorite

// Manga favorites and manga activity items.



@Composable
internal fun MangaFavoriteItem(
    manga: MangaFavorite,
    preferEnglishTitles: Boolean,
    onClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog && onRemove != null) {
        val title = if (preferEnglishTitles) {
            manga.title?.english ?: manga.title?.romaji ?: stringResource(R.string.profile_unknown)
        } else {
            manga.title?.romaji ?: manga.title?.english ?: stringResource(R.string.profile_unknown)
        }
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.profile_remove_favorite_title)) },
            text = { Text(stringResource(R.string.profile_remove_favorite_message, title)) },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; onRemove() }) {
                    Text(stringResource(R.string.profile_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            }
        )
    }

    val displayTitle = if (preferEnglishTitles) {
        manga.title?.english ?: manga.title?.romaji ?: stringResource(R.string.profile_unknown)
    } else {
        manga.title?.romaji ?: manga.title?.english ?: stringResource(R.string.profile_unknown)
    }
    val startYear = manga.startDate?.year
    val format = manga.format
    val displayScore = manga.userScore?.let { com.blissless.tensei.ui.dialogs.userScoreToDisplay(it) }?.takeIf { it > 0 }

    if (showRemoveDialog && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.profile_remove_favorite_title)) },
            text = { Text(stringResource(R.string.profile_remove_favorite_message, displayTitle)) },
            confirmButton = {
                TextButton(onClick = { showRemoveDialog = false; onRemove() }) {
                    Text(stringResource(R.string.profile_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = manga.coverImage?.extraLarge ?: manga.coverImage?.large,
                contentDescription = displayTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            if (startYear != null) {
                FavoriteCoverBadge(
                    text = startYear.toString(),
                    icon = null,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                )
            }
            if (displayScore != null) {
                FavoriteCoverBadge(
                    text = displayScore.toString(),
                    icon = Icons.Default.Star,
                    tintColor = tenseiColors.rating,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )
            }
            format?.let { fmt ->
                FavoriteCoverBadge(
                    text = fmt,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                )
            }
            if (onRemove != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showRemoveDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Favorite, stringResource(R.string.profile_remove_from_favorites), tint = tenseiColors.favorite, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            displayTitle,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
internal fun MangaActivityItem(
    node: MangaActivityNode,
    preferEnglishTitles: Boolean,
    onClick: () -> Unit = {}
) {
    val media = node.media
    val displayTitle = if (preferEnglishTitles) {
        media?.title?.english ?: media?.title?.romaji ?: stringResource(R.string.profile_unknown)
    } else {
        media?.title?.romaji ?: media?.title?.english ?: stringResource(R.string.profile_unknown)
    }

    val status = node.status ?: ""
    val (statusIcon, statusColor, statusLabel) = when {
        status.contains("completed", ignoreCase = true) || status.contains("finished", ignoreCase = true) ->
            Triple(Icons.Default.Check, statusColor("COMPLETED"), stringResource(R.string.profile_completed))
        status.contains("paused", ignoreCase = true) || status.contains("hold", ignoreCase = true) ->
            Triple(Icons.Default.Pause, statusColor("PAUSED"), stringResource(R.string.profile_on_hold))
        status.contains("dropped", ignoreCase = true) ->
            Triple(Icons.Default.Delete, statusColor("DROPPED"), stringResource(R.string.profile_dropped))
        status.contains("plan", ignoreCase = true) ->
            Triple(Icons.Default.Bookmark, statusColor("PLANNING"), stringResource(R.string.profile_planning_to_read))
        status.contains("reading", ignoreCase = true) || status.contains("read", ignoreCase = true) ||
            status.contains("current", ignoreCase = true) || status.contains("repeating", ignoreCase = true) ||
            status.contains("reread", ignoreCase = true) ->
            Triple(Icons.Default.PlayArrow, statusColor("CURRENT"), stringResource(R.string.profile_read))
        else -> Triple(Icons.Default.PlayArrow, statusColor("CURRENT"), status)
    }

    val progress = node.progress
    val progressSuffix = if (progress.isNullOrBlank()) {
        null
    } else {
        val nums = Regex("\\d+").findAll(progress).map { it.value.toIntOrNull() }.filterNotNull().toList()
        when {
            nums.size >= 2 && nums[1] > nums[0] -> stringResource(R.string.profile_chapter_range, nums[0], nums[1])
            nums.isNotEmpty() -> stringResource(R.string.profile_chapter, nums[0])
            else -> stringResource(R.string.profile_chapter_progress, progress)
        }
    }
    val statusText = if (progressSuffix != null) "$statusLabel $progressSuffix" else statusLabel

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = media?.coverImage?.extraLarge ?: media?.coverImage?.large,
                contentDescription = displayTitle,
                modifier = Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    displayTitle, color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Text(statusText, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                }
                Text(
                    formatTimestamp(node.createdAt, LocaleDate.localeOf(LocalContext.current)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


internal fun formatTimestamp(timestamp: Long, locale: Locale): String =
    LocaleDate.formatLongWithTime(timestamp * 1000L, locale)
