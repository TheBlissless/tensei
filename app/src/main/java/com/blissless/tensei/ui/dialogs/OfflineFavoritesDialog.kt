package com.blissless.tensei.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.blissless.tensei.R
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.StoredFavorite
import com.blissless.tensei.ui.theme.tenseiColors

@Composable
fun OfflineFavoritesDialog(
    favorites: Map<Int, StoredFavorite>,
    onDismiss: () -> Unit,
    onAnimeClick: (ExploreAnime) -> Unit,
    onRemoveFavorite: (Int) -> Unit,
    // When true, the dialog prefers the favorite's `titleEnglish` (if non-blank) over `title`.
    // Mirrors the same fallback pattern used everywhere else in the app for the
    // "Prefer English titles" setting. Default true for callers that haven't been updated.
    preferEnglishTitles: Boolean = true
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.comp_favorites_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            stringResource(R.string.comp_favorites_offline_count, favorites.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.comp_close), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FavoriteBorder, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.comp_no_favorites_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    stringResource(R.string.comp_favorites_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(favorites.values.toList()) { fav ->
                                OfflineFavoriteItem(
                                    favorite = fav,
                                    onClick = {
                                        val exploreAnime = ExploreAnime(
                                            id = fav.id, title = fav.title, cover = fav.cover,
                                            banner = fav.banner, episodes = 0, latestEpisode = null,
                                            averageScore = fav.averageScore, genres = emptyList(), year = fav.year
                                        )
                                        onAnimeClick(exploreAnime)
                                    },
                                    onRemove = { onRemoveFavorite(fav.id) },
                                    preferEnglishTitles = preferEnglishTitles
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineFavoriteItem(
    favorite: StoredFavorite,
    onClick: () -> Unit, onRemove: () -> Unit,
    preferEnglishTitles: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(50.dp).height(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                if (favorite.cover.isNotEmpty()) {
                    AsyncImage(
                        model = favorite.cover,
                        contentDescription = if (preferEnglishTitles && !favorite.titleEnglish.isNullOrBlank()) favorite.titleEnglish else favorite.title,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Apply the "Prefer English titles" setting: use titleEnglish when the
                // setting is on AND the favorite has a non-blank English title stored;
                // fall back to the romaji title (favorite.title) otherwise. Legacy
                // favorites persisted before titleEnglish existed fall through cleanly.
                val displayTitle = if (preferEnglishTitles && !favorite.titleEnglish.isNullOrBlank()) {
                    favorite.titleEnglish
                } else {
                    favorite.title
                }
                Text(
                    displayTitle.ifEmpty { stringResource(R.string.comp_anime_fallback) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                favorite.year?.let { year ->
                    Text("$year", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Favorite, stringResource(R.string.comp_remove_from_favorites),
                    tint = tenseiColors.favorite, modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

