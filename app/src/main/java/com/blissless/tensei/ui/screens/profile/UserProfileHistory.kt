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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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

// Activity history tab content.



@Composable
internal fun HistoryContent(
    history: List<HistoryEntry>,
    mangaHistory: List<MangaActivityNode> = emptyList(),
    preferEnglishTitles: Boolean,
    onAnimeClick: (HistoryEntry) -> Unit,
    onMangaClick: (MangaActivityNode) -> Unit = {},
    statuses: List<String> = emptyList(),
    progressList: List<String> = emptyList()
) {
    if (history.isEmpty() && mangaHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.profile_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        var animeExpanded by remember { mutableStateOf(true) }
        var mangaExpanded by remember { mutableStateOf(true) }

        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            // Must mirror the favourites grid's horizontal inset, otherwise the
            // disclosure headers shift sideways when switching tabs.
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp)
        ) {
            if (history.isNotEmpty()) {
                item {
                    ProfileSectionHeader(
                        title = stringResource(R.string.profile_anime),
                        icon = Icons.Rounded.Movie,
                        count = history.size,
                        expanded = animeExpanded,
                        onClick = { animeExpanded = !animeExpanded }
                    )
                }
                itemsIndexed(
                    items = if (animeExpanded) history else emptyList(),
                    key = { index, entry -> "hist_anime_${entry.malId}_$index" }
                ) { index, entry ->
                    Box(modifier = Modifier.animateItem()) {
                        HistoryItem(
                            entry = entry,
                            preferEnglishTitles = preferEnglishTitles,
                            onClick = { onAnimeClick(entry) },
                            status = statuses.getOrNull(index),
                            progress = progressList.getOrNull(index)
                        )
                    }
                }
            }
            if (mangaHistory.isNotEmpty()) {
                item {
                    ProfileSectionHeader(
                        title = stringResource(R.string.profile_manga),
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        count = mangaHistory.size,
                        expanded = mangaExpanded,
                        onClick = { mangaExpanded = !mangaExpanded }
                    )
                }
                itemsIndexed(
                    items = if (mangaExpanded) mangaHistory else emptyList(),
                    key = { _, node -> "hist_manga_${node.id}" }
                ) { index, node ->
                    Box(modifier = Modifier.animateItem()) {
                        MangaActivityItem(node = node, preferEnglishTitles = preferEnglishTitles, onClick = { onMangaClick(node) })
                    }
                }
            }
        }
    }
}


@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    preferEnglishTitles: Boolean, onClick: () -> Unit,
    status: String? = null, progress: String? = null
) {
    val (statusIcon, statusColor, statusLabel) = when {
        status?.contains("completed", ignoreCase = true) == true || status?.contains("finished", ignoreCase = true) == true ->
            Triple(Icons.Default.Check, statusColor("COMPLETED"), stringResource(R.string.profile_completed))
        status?.contains("paused", ignoreCase = true) == true || status?.contains("hold", ignoreCase = true) == true ->
            Triple(Icons.Default.Pause, statusColor("PAUSED"), stringResource(R.string.profile_on_hold))
        status?.contains("dropped", ignoreCase = true) == true ->
            Triple(Icons.Default.Delete, statusColor("DROPPED"), stringResource(R.string.profile_dropped))
        status?.contains("plan", ignoreCase = true) == true ->
            Triple(Icons.Default.Bookmark, statusColor("PLANNING"), stringResource(R.string.profile_planning_to_watch))
        status?.contains("watching", ignoreCase = true) == true || status?.contains("watched", ignoreCase = true) == true ||
            status?.contains("repeating", ignoreCase = true) == true || status?.contains("rewatched", ignoreCase = true) == true ->
            Triple(Icons.Default.PlayArrow, statusColor("CURRENT"), stringResource(R.string.profile_watched))
        else -> Triple(Icons.Default.PlayArrow, statusColor("CURRENT"), status ?: "")
    }

    val displayTitle = if (preferEnglishTitles && !entry.titleEnglish.isNullOrEmpty()) entry.titleEnglish else entry.title

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
                model = entry.cover, contentDescription = displayTitle,
                modifier = Modifier.width(56.dp).height(80.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    displayTitle, color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(12.dp))
                    Text(
                        if (progress.isNullOrBlank()) statusLabel else "$statusLabel · $progress",
                        color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium
                    )
                }
                entry.date?.let { date ->
                    Text(
                        date, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
