package com.blissless.tensei.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ContinueWatchingEntry
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.ui.components.HomeAnimeCardBounds
import com.blissless.tensei.ui.components.HomeAnimeHorizontalList
import com.blissless.tensei.ui.components.HomeStatusColors
import com.blissless.tensei.ui.components.SectionHeader
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.screens.status.StatusListScreen

/**
 * The five anime and five manga status-list sections of the home screen —
 * "Currently Watching/Reading", "Planning", "Completed", "On Hold" and
 * "Dropped" — extracted verbatim from [HomeScreen].
 *
 * All ten sections previously repeated the same five status-list state
 * assignments in their header click handlers; they now report through a
 * single [onOpenStatusList] callback.
 */

@Composable
internal fun HomeAnimeStatusSections(
    effectiveCurrentlyWatching: List<AnimeMedia>,
    effectivePlanningToWatch: List<AnimeMedia>,
    effectiveCompleted: List<AnimeMedia>,
    effectiveOnHold: List<AnimeMedia>,
    effectiveDropped: List<AnimeMedia>,
    showStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    playbackPositions: Map<String, Long>,
    playbackDurations: Map<String, Long>,
    disableMaterialColors: Boolean,
    currentScreenIndex: Int,
    viewModel: MainViewModel,
    onAnimeClick: (AnimeMedia, com.blissless.tensei.ui.components.HomeAnimeCardBounds?) -> Unit,
    onInfoClick: (AnimeMedia, com.blissless.tensei.ui.components.HomeAnimeCardBounds?) -> Unit,
    onOpenStatusList: (title: String, icon: ImageVector, type: String, isManga: Boolean) -> Unit,
) {
    if (effectiveCurrentlyWatching.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_currently_watching)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.PlayArrow,
                count = effectiveCurrentlyWatching.size,
                iconTint = HomeStatusColors.getColor("CURRENT"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.PlayArrow, "CURRENT", false)
                }
            )
        HomeAnimeHorizontalList(
            animeList = effectiveCurrentlyWatching,
            listType = "CURRENT",
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            disableMaterialColors = disableMaterialColors,
            showProgressBar = false,
            onAnimeClick = onAnimeClick,
            onInfoClick = onInfoClick,
            listIndex = 0,
            screenKey = "home",
            isVisible = currentScreenIndex == 0,
            viewModel = viewModel
        )
    }

    if (effectivePlanningToWatch.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_planning_to_watch)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Bookmark,
                count = effectivePlanningToWatch.size,
                iconTint = HomeStatusColors.getColor("PLANNING"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Bookmark, "PLANNING", false)
                }
            )
        HomeAnimeHorizontalList(
            animeList = effectivePlanningToWatch,
            listType = "PLANNING",
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            disableMaterialColors = disableMaterialColors,
            onAnimeClick = onAnimeClick,
            onInfoClick = onInfoClick,
            listIndex = 1,
            screenKey = "home",
            isVisible = currentScreenIndex == 0,
            viewModel = viewModel
        )
    }

    if (effectiveCompleted.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_completed)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Check,
                count = effectiveCompleted.size,
                iconTint = HomeStatusColors.getColor("COMPLETED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Check, "COMPLETED", false)
                }
            )
        HomeAnimeHorizontalList(
            animeList = effectiveCompleted,
            listType = "COMPLETED",
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            disableMaterialColors = disableMaterialColors,
            onAnimeClick = onAnimeClick,
            onInfoClick = onInfoClick,
            listIndex = 2,
            screenKey = "home",
            isVisible = currentScreenIndex == 0,
            viewModel = viewModel
        )
    }

    if (effectiveOnHold.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_on_hold)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Pause,
                count = effectiveOnHold.size,
                iconTint = HomeStatusColors.getColor("PAUSED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Pause, "PAUSED", false)
                }
            )
        HomeAnimeHorizontalList(
            animeList = effectiveOnHold,
            listType = "PAUSED",
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            disableMaterialColors = disableMaterialColors,
            onAnimeClick = onAnimeClick,
            onInfoClick = onInfoClick,
            listIndex = 3,
            screenKey = "home",
            isVisible = currentScreenIndex == 0,
            viewModel = viewModel
        )
    }

    if (effectiveDropped.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_dropped)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Delete,
                count = effectiveDropped.size,
                iconTint = HomeStatusColors.getColor("DROPPED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Delete, "DROPPED", false)
                }
            )
        HomeAnimeHorizontalList(
            animeList = effectiveDropped,
            listType = "DROPPED",
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            playbackPositions = playbackPositions,
            playbackDurations = playbackDurations,
            disableMaterialColors = disableMaterialColors,
            onAnimeClick = onAnimeClick,
            onInfoClick = onInfoClick,
            listIndex = 4,
            screenKey = "home",
            isVisible = currentScreenIndex == 0,
            viewModel = viewModel
        )
    }
}

@Composable
internal fun HomeMangaStatusSections(
    mangaCurrentlyReading: List<MangaMedia>,
    mangaPlanningToRead: List<MangaMedia>,
    mangaCompleted: List<MangaMedia>,
    mangaPaused: List<MangaMedia>,
    mangaDropped: List<MangaMedia>,
    isOled: Boolean,
    preferEnglishTitles: Boolean,
    onMangaClick: (MangaMedia) -> Unit,
    onMangaInfoClick: (MangaMedia) -> Unit,
    onOpenStatusList: (title: String, icon: ImageVector, type: String, isManga: Boolean) -> Unit,
) {
    // â”€â”€â”€ Manga sections â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Manga "Currently Reading" â€” full CURRENT-status poster row, mirroring the
    // anime "Currently Watching" section. Kept separate from "Continue Reading"
    // (which stays at the top and only shows manga with a saved reading position).
    if (mangaCurrentlyReading.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_currently_reading)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.PlayArrow,
                count = mangaCurrentlyReading.size,
                iconTint = HomeStatusColors.getColor("CURRENT"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.PlayArrow, "CURRENT", true)
                }
            )
        MangaHorizontalRow(
            mangaList = mangaCurrentlyReading,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick
        )
    }

    if (mangaPlanningToRead.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_planning_to_read)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Bookmark,
                count = mangaPlanningToRead.size,
                iconTint = HomeStatusColors.getColor("PLANNING"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Bookmark, "PLANNING", true)
                }
            )
        MangaHorizontalRow(
            mangaList = mangaPlanningToRead,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick
        )
    }

    if (mangaCompleted.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_completed)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Check,
                count = mangaCompleted.size,
                iconTint = HomeStatusColors.getColor("COMPLETED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Check, "COMPLETED", true)
                }
            )
        MangaHorizontalRow(
            mangaList = mangaCompleted,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick
        )
    }

    if (mangaPaused.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_on_hold)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Pause,
                count = mangaPaused.size,
                iconTint = HomeStatusColors.getColor("PAUSED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Pause, "PAUSED", true)
                }
            )
        MangaHorizontalRow(
            mangaList = mangaPaused,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick
        )
    }

    if (mangaDropped.isNotEmpty()) {
                    val sectionTitle = stringResource(R.string.home_dropped)
            SectionHeader(
                title = sectionTitle,
                icon = Icons.Default.Delete,
                count = mangaDropped.size,
                iconTint = HomeStatusColors.getColor("DROPPED"),
                onClick = {
                    onOpenStatusList(sectionTitle, Icons.Default.Delete, "DROPPED", true)
                }
            )
        MangaHorizontalRow(
            mangaList = mangaDropped,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            onMangaClick = onMangaClick,
            onMangaInfoClick = onMangaInfoClick
        )
    }
}

@Composable
internal fun HomeWelcomeCard(
    appIcon: String,
    onLoginClick: () -> Unit,
) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                                            )
                                        )
                                    )
                                    .padding(32.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        AsyncImage(model = appIconDrawable(appIcon), contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape))
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    stringResource(R.string.home_welcome_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.home_welcome_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onLoginClick,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(com.blissless.tensei.network.Endpoints.AniList.FAVICON)
                                            .build(),
                                        contentDescription = stringResource(R.string.home_anilist_logo_content_description),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(stringResource(R.string.settings_login_anilist), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.home_welcome_signup),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
}

@Composable
internal fun HomeStatusListOverlay(
    visible: Boolean,
    statusListTitle: String,
    statusListIcon: androidx.compose.ui.graphics.vector.ImageVector,
    statusListType: String,
    statusListIsManga: Boolean,
    continueWatchingEpisodes: List<com.blissless.tensei.data.models.ContinueWatchingEntry>,
    animeById: Map<Int, com.blissless.tensei.data.models.AnimeMedia>,
    effectiveCurrentlyWatching: List<com.blissless.tensei.data.models.AnimeMedia>,
    effectivePlanningToWatch: List<com.blissless.tensei.data.models.AnimeMedia>,
    effectiveCompleted: List<com.blissless.tensei.data.models.AnimeMedia>,
    effectiveOnHold: List<com.blissless.tensei.data.models.AnimeMedia>,
    effectiveDropped: List<com.blissless.tensei.data.models.AnimeMedia>,
    activeMangaContinueReading: List<com.blissless.tensei.data.models.MangaMedia>,
    mangaCurrentlyReading: List<com.blissless.tensei.data.models.MangaMedia>,
    mangaPlanningToRead: List<com.blissless.tensei.data.models.MangaMedia>,
    mangaCompleted: List<com.blissless.tensei.data.models.MangaMedia>,
    mangaPaused: List<com.blissless.tensei.data.models.MangaMedia>,
    mangaDropped: List<com.blissless.tensei.data.models.MangaMedia>,
    showStatusColors: Boolean,
    preferEnglishTitles: Boolean,
    onPlayEpisode: (com.blissless.tensei.data.models.AnimeMedia, Int, String?) -> Unit,
    onMangaClick: (com.blissless.tensei.data.models.MangaMedia) -> Unit,
    onMangaInfoClick: (com.blissless.tensei.data.models.MangaMedia) -> Unit,
    onMangaContinueReadingClick: (com.blissless.tensei.data.models.MangaMedia) -> Unit,
    onEpisodeSheetAnime: (com.blissless.tensei.data.models.AnimeMedia) -> Unit,
    onStatusDialogAnime: (com.blissless.tensei.data.models.AnimeMedia) -> Unit,
    onInfoClick: (com.blissless.tensei.data.models.AnimeMedia, com.blissless.tensei.ui.components.HomeAnimeCardBounds?) -> Unit,
    onMangaStatusClick: (com.blissless.tensei.data.models.MangaMedia) -> Unit,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val liveStatusListAnime: List<AnimeMedia> =
        if (!statusListIsManga) {
            when (statusListTitle) {
                context.getString(R.string.home_continue_watching) -> continueWatchingEpisodes.map { entry ->
                    animeById[entry.animeId] ?: AnimeMedia(id = entry.animeId, title = entry.animeTitle, titleEnglish = entry.animeTitleEnglish, cover = entry.animeCover, banner = entry.animeBanner)
                }
                else -> when (statusListType) {
                    "CURRENT" -> effectiveCurrentlyWatching
                    "PLANNING" -> effectivePlanningToWatch
                    "COMPLETED" -> effectiveCompleted
                    "PAUSED" -> effectiveOnHold
                    "DROPPED" -> effectiveDropped
                    else -> emptyList()
                }
            }
        } else emptyList()

    val liveStatusListManga: List<MangaMedia> =
        if (statusListIsManga) {
            when (statusListTitle) {
                context.getString(R.string.home_continue_reading) -> activeMangaContinueReading
                else -> when (statusListType) {
                    "CURRENT" -> mangaCurrentlyReading
                    "PLANNING" -> mangaPlanningToRead
                    "COMPLETED" -> mangaCompleted
                    "PAUSED" -> mangaPaused
                    "DROPPED" -> mangaDropped
                    else -> emptyList()
                }
            }
        } else emptyList()

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            ),
            targetOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        StatusListScreen(
            title = statusListTitle,
            icon = statusListIcon,
            animeList = liveStatusListAnime,
            mangaList = liveStatusListManga,
            listType = statusListType,
            isManga = statusListIsManga,
            showStatusColors = showStatusColors,
            preferEnglishTitles = preferEnglishTitles,
            onAnimeClick = { anime, _ -> onEpisodeSheetAnime(anime) },
            onPlayClick = { anime ->
                val lt = statusListType
                if (lt == "CURRENT") {
                    val nextEp = anime.progress + 1
                    val released = anime.latestEpisode ?: anime.totalEpisodes
                    if (anime.latestEpisode != null && nextEp > released) {
                        context.toast(context.getString(R.string.home_episode_not_aired))
                    } else {
                        onPlayEpisode(anime, nextEp, null)
                    }
                } else {
                    onPlayEpisode(anime, 1, null)
                }
            },
            onStatusClick = { anime -> onStatusDialogAnime(anime) },
            onInfoClick = onInfoClick,
            onMangaClick = { manga -> onMangaClick(manga) },
            onMangaInfoClick = onMangaInfoClick,
            onMangaContinueReadingClick = onMangaContinueReadingClick,
            onMangaStatusClick = onMangaStatusClick,
            onBackClick = onClose,
            onDismiss = onClose
        )
    }
}
