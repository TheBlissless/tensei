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

// Skeleton (loading placeholder) layouts for the profile screen.



@Composable
internal fun UserProfileNavButton(
    icon: ImageVector, title: String, isSelected: Boolean,
    onClick: () -> Unit, badge: Int? = null
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                icon, contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            if (badge != null && badge > 0) {
                androidx.compose.material3.Badge(
                    modifier = Modifier.offset(x = 10.dp, y = (-4).dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}


/**
 * Mirrors the geometry of [TenseiDisclosureHeader] so switching tabs doesn't
 * reflow once the real headers land.
 */
@Composable
private fun ProfileHeaderSkeleton(modifier: Modifier = Modifier) {
    SectionHeaderSkeleton(
        modifier = modifier.padding(horizontal = Spacing.xs, vertical = Spacing.sm)
    )
}


@Composable
internal fun ProfileAboutSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shimmer()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            cornerRadius = 14.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(
                modifier = Modifier.size(64.dp),
                cornerRadius = 32.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(140.dp)
                        .height(16.dp)
                )
                SkeletonBlock(
                    modifier = Modifier
                        .width(90.dp)
                        .height(11.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SkeletonBlock(
                        modifier = Modifier
                            .width(56.dp)
                            .height(12.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .width(34.dp)
                            .height(20.dp)
                    )
                }
            }
        }
        ProfileHeaderSkeleton()
        repeat(3) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                cornerRadius = 12.dp,
            )
        }
        ProfileHeaderSkeleton()
        repeat(3) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                cornerRadius = 12.dp,
            )
        }
    }
}


@Composable
internal fun ProfileFavoritesSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shimmer()
            // Mirrors the favourites grid's inset so the headers don't shift.
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ProfileHeaderSkeleton()
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                            cornerRadius = 12.dp,
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(12.dp)
                        )
                        SkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(9.dp)
                        )
                    }
                }
            }
        }
        ProfileHeaderSkeleton()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                SkeletonBlock(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(2f / 3f),
                    cornerRadius = 12.dp,
                )
            }
        }
    }
}


@Composable
internal fun ProfileHistorySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .shimmer()
            // Mirrors the history list's inset so the headers don't shift.
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProfileHeaderSkeleton()
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(56.dp)
                        .height(80.dp),
                    cornerRadius = 10.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(12.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .width(84.dp)
                            .height(16.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .width(56.dp)
                            .height(9.dp)
                    )
                }
            }
        }
        ProfileHeaderSkeleton()
        repeat(2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(56.dp)
                        .height(80.dp),
                    cornerRadius = 10.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                    )
                    SkeletonBlock(
                        modifier = Modifier
                            .width(72.dp)
                            .height(16.dp)
                    )
                }
            }
        }
    }
}
