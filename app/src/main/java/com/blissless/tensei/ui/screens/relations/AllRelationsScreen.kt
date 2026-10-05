package com.blissless.tensei.ui.screens.relations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.ui.components.RelatedMediaCard
import com.blissless.tensei.ui.components.asRelatedMedia
import kotlinx.coroutines.delay

@Composable
fun AllRelationsScreen(
    animeId: Int,
    animeTitle: String,
    animeTitleEnglish: String? = null,
    preferEnglishTitles: Boolean = true,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit = onDismiss,
    onRelationClick: (AnimeRelation) -> Unit
) {
    var relations by remember { mutableStateOf<List<AnimeRelation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    val displayTitle = if (preferEnglishTitles && !animeTitleEnglish.isNullOrBlank()) animeTitleEnglish else animeTitle

    LaunchedEffect(animeId) {
        isLoading = true
        relations = try {
            viewModel.fetchAnimeRelations(animeId) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        isLoading = false
    }

    // Auto-recovery: while the list is MAL-fallback data (AniList down), re-check AniList
    // every 60s (force = bypass the stored cache). When AniList answers, the fresh list
    // replaces the MAL one and the source flips back to "anilist".
    val animeDetailSource by viewModel.animeDetailSource.collectAsState()
    LaunchedEffect(animeId, animeDetailSource) {
        while (animeDetailSource == "mal") {
            delay(30_000)
            isLoading = true
            relations = try {
                viewModel.fetchAnimeRelations(animeId, force = true) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onNavigateBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = statusBarsPadding.calculateTopPadding() + 8.dp, bottom = 12.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .zIndex(10f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                        Text(
                            text = "Relations",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 60.dp)
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (relations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No relations found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp,
                            bottom = 16.dp + navigationBarsPadding.calculateBottomPadding()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(relations) { relation ->
                            RelatedMediaCard(
                                media = relation.asRelatedMedia(),
                                preferEnglishTitle = preferEnglishTitles,
                                titleAlign = TextAlign.Center,
                                placeholderColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                onClick = { onRelationClick(relation) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

