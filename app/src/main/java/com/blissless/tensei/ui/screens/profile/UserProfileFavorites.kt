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

// Favorites tab content and cards.



@Composable
internal fun FavoritesContent(
    favorites: List<ProfileFavorite>,
    mangaFavorites: List<MangaFavorite> = emptyList(),
    preferEnglishTitles: Boolean,
    onAnimeClick: (ProfileFavorite) -> Unit,
    onRemoveFavorite: ((ProfileFavorite) -> Unit)? = null,
    onMangaClick: (MangaFavorite) -> Unit = {},
    onRemoveMangaFavorite: ((MangaFavorite) -> Unit)? = null
) {
    if (favorites.isEmpty() && mangaFavorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.profile_no_favorites), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        var animeExpanded by remember { mutableStateOf(true) }
        var mangaExpanded by remember { mutableStateOf(true) }
        val listState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp)
        ) {
            if (favorites.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileSectionHeader(
                        title = stringResource(R.string.profile_anime),
                        icon = Icons.Rounded.Movie,
                        count = favorites.size,
                        expanded = animeExpanded,
                        onClick = { animeExpanded = !animeExpanded }
                    )
                }
                if (animeExpanded) {
                    items(favorites, key = { it.id }) { anime ->
                        Box(modifier = Modifier.animateItem()) {
                            FavoriteItem(
                                anime = anime,
                                preferEnglishTitles = preferEnglishTitles,
                                onClick = { onAnimeClick(anime) },
                                onRemove = { onRemoveFavorite?.invoke(anime) }
                            )
                        }
                    }
                }
            }
            if (mangaFavorites.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileSectionHeader(
                        title = stringResource(R.string.profile_manga),
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        count = mangaFavorites.size,
                        expanded = mangaExpanded,
                        onClick = { mangaExpanded = !mangaExpanded }
                    )
                }
                if (mangaExpanded) {
                    items(mangaFavorites, key = { it.id }) { manga ->
                        Box(modifier = Modifier.animateItem()) {
                            MangaFavoriteItem(
                                manga = manga,
                                preferEnglishTitles = preferEnglishTitles,
                                onClick = { onMangaClick(manga) },
                                onRemove = { onRemoveMangaFavorite?.invoke(manga) }
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * Expand/collapse header used by the favourites and history sections.
 *
 * Now delegates to [TenseiDisclosureHeader] so it shares the stripe + count
 * pill language of every other section header in the app.
 */
@Composable
internal fun ProfileSectionHeader(
    title: String,
    icon: ImageVector,
    count: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    TenseiDisclosureHeader(
        title = title,
        icon = icon,
        count = count,
        expanded = expanded,
        onClick = onClick,
    )
}


@Composable
private fun FavoriteItem(
    anime: ProfileFavorite,
    preferEnglishTitles: Boolean,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    val displayTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
    val displayScore = anime.userScore?.let { com.blissless.tensei.ui.dialogs.userScoreToDisplay(it) }?.takeIf { it > 0 }
    val format = anime.format
    val year = anime.year

    if (showRemoveDialog && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.profile_remove_favorite_title)) },
            text = { Text(stringResource(R.string.profile_remove_favorite_message, anime.title)) },
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
                model = anime.cover, contentDescription = displayTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            if (year != null) {
                FavoriteCoverBadge(
                    text = year.toString(),
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
                    icon = Icons.Default.PlayArrow,
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
internal fun FavoriteCoverBadge(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.White,
    scrim: Color = Color.Black.copy(alpha = 0.6f)
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = scrim
    ) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(
                text,
                color = tintColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
