package com.blissless.tensei.ui.screens.episode

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.TmdbEpisode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.clearMagnetEpisodes
import com.blissless.tensei.viewmodel.fetchMagnetEpisodes
import com.blissless.tensei.viewmodel.loadAvailableMagnetExtensions
import com.blissless.tensei.viewmodel.loadAvailableStreamExtensions
import com.blissless.tensei.viewmodel.setDefaultExtensionPackage
import com.blissless.tensei.viewmodel.setDefaultMagnetExtension
import com.blissless.tensei.viewmodel.setDefaultStreamExtension
import com.blissless.tensei.viewmodel.setStreamMethod
import com.blissless.tensei.util.toast

// Episode list and the two episode card styles (simple + TMDB with
// titles/descriptions).



/**
 * Compact rich episode list shown below the video player when it is not in fullscreen.
 * Mirrors the rich episode screen styling but drops the anime banner header — the video
 * itself acts as the header.
 */
@Composable
fun RichEpisodeList(
    episodeCount: Int,
    releasedCount: Int,
    currentEpisode: Int,
    currentProgress: Int = 0,
    isOled: Boolean = false,
    tmdbEpisodes: List<TmdbEpisode> = emptyList(),
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    animeId: Int = 0,
    animeTitle: String = "",
    episodeTitle: String? = null,
    onEpisodeSelect: (Int) -> Unit,
    onClose: (() -> Unit)? = null,
    onEnterFullscreen: (() -> Unit)? = null,
    hideDescription: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentEpisode - 1).coerceAtLeast(0))

    // Keep the list anchored at the currently playing episode when it changes
    LaunchedEffect(currentEpisode) {
        val targetIndex = (currentEpisode - 1).coerceIn(0, (episodeCount - 1).coerceAtLeast(0))
        listState.scrollToItem(targetIndex)
    }

    Column(modifier.fillMaxSize().background(if (isOled) Color.Black else MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onClose != null) {
                IconButton(onClick = onClose) {
                     Icon(Icons.Default.Close, contentDescription = stringResource(R.string.lists_close_player), tint = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (animeTitle.isNotEmpty()) {
                    Text(
                        animeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!episodeTitle.isNullOrEmpty()) {
                    Text(
                        episodeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onEnterFullscreen != null) {
                IconButton(onClick = onEnterFullscreen) {
                     Icon(Icons.Default.Fullscreen, contentDescription = stringResource(R.string.lists_enter_fullscreen), tint = Color.White)
                }
            }
        }
        HorizontalDivider(color = if (isOled) Color(0xFF333333) else MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(episodeCount) { index ->
                val ep = index + 1
                val tmdb = tmdbEpisodes.find { it.episode == ep }
                val isWatched = ep <= currentProgress
                val isCurrent = ep == currentEpisode
                val hasAired = ep <= releasedCount
                if (tmdb != null) {
                    RichTmdbEpisodeCard(
                        episodeNumber = ep,
                        title = tmdb.title,
                        description = tmdb.description,
                        image = tmdb.image,
                        isWatched = isWatched,
                        isCurrent = isCurrent,
                        hasAired = hasAired,
                        isOled = isOled,
                        hideDescription = hideDescription,
                        isSelected = isCurrent,
                        playbackPositions = playbackPositions,
                        playbackDurations = playbackDurations,
                        animeId = animeId,
                        onSelect = {},
                        onPlay = { onEpisodeSelect(ep) }
                    )
                } else {
                    SimpleRichEpisodeCard(
                        episodeNumber = ep,
                        isWatched = isWatched,
                        isCurrent = isCurrent,
                        hasAired = hasAired,
                        isOled = isOled,
                        isSelected = isCurrent,
                        playbackPositions = playbackPositions,
                        playbackDurations = playbackDurations,
                        animeId = animeId,
                        onSelect = {},
                        onPlay = { onEpisodeSelect(ep) }
                    )
                }
            }
        }
    }
}


@Composable
internal fun SimpleRichEpisodeCard(
    episodeNumber: Int,
    isWatched: Boolean,
    isCurrent: Boolean,
    hasAired: Boolean,
    isOled: Boolean,
    isSelected: Boolean,
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    animeId: Int = 0,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = when {
        isWatched -> if (isOled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        hasAired -> if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else -> if (isOled) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    }

    // Keep outline indicator for current episode
    val borderColor = when {
        isCurrent -> MaterialTheme.colorScheme.secondary
        isSelected -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    val contentAlpha = if (hasAired) 1f else 0.4f

    // Consistent dark style for badges and icons
    val badgeBg = Color.Black.copy(alpha = 0.7f)
    val badgeText = Color.White

    // Playback progress
    val epPlaybackKey = "${animeId}_$episodeNumber"
    val savedPos = playbackPositions[epPlaybackKey] ?: 0L
    val epDuration = playbackDurations[epPlaybackKey] ?: 0L
    val progressRatio = if (savedPos > 0 && epDuration > 0) (savedPos.toFloat() / epDuration).coerceIn(0f, 1f) else 0f
    val hasProgress = savedPos > 5000L

    AnimatedVisibility(visible = true, enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f), exit = fadeOut(tween(200))) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)
                .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(14.dp)) else Modifier),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = {
                if (hasAired) {
                    onSelect()
                    onPlay()
                } else {
                                     context.toast(context.getString(R.string.lists_episode_has_not_aired_yet))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).alpha(if (isWatched) 0.55f else contentAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(badgeBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isCurrent -> Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.lists_current), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            else -> Text(
                                text = "$episodeNumber",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                             Text(
                                 text = stringResource(R.string.lists_episode, episodeNumber),
                                 style = MaterialTheme.typography.titleSmall,
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.onSurface
                             )
                        }
                    }
                }
                // Progress bar only
                if (hasProgress) {
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = if (isOled) Color(0xFF333333) else MaterialTheme.colorScheme.surfaceVariant,
                        gapSize = 0.dp
                    )
                }
            }
        }
    }
}


@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RichTmdbEpisodeCard(
    episodeNumber: Int,
    title: String?,
    description: String?,
    image: String?,
    isWatched: Boolean,
    isCurrent: Boolean,
    hasAired: Boolean,
    isOled: Boolean,
    hideDescription: Boolean = false,
    isSelected: Boolean,
    playbackPositions: Map<String, Long> = emptyMap(),
    playbackDurations: Map<String, Long> = emptyMap(),
    animeId: Int = 0,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = when {
        isWatched -> if (isOled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        hasAired -> if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else -> if (isOled) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    }

    // Keep outline indicator for current episode
    val borderColor = when {
        isCurrent -> MaterialTheme.colorScheme.secondary
        isSelected -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    val contentAlpha = if (hasAired) 1f else 0.4f

    // Inline expandable description (keeps cards compact until the user taps "More")
    var descriptionExpanded by remember { mutableStateOf(false) }
    var descriptionTruncated by remember { mutableStateOf(false) }

    // Playback progress
    val epPlaybackKey = "${animeId}_$episodeNumber"
    val savedPos = playbackPositions[epPlaybackKey] ?: 0L
    val epDuration = playbackDurations[epPlaybackKey] ?: 0L
    val progressRatio = if (savedPos > 0 && epDuration > 0) (savedPos.toFloat() / epDuration).coerceIn(0f, 1f) else 0f
    val hasProgress = savedPos > 5000L

    AnimatedVisibility(visible = true, enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f), exit = fadeOut(tween(200))) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(14.dp)) else Modifier),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            onClick = {
                if (hasAired) {
                    onSelect()
                    onPlay()
                } else {
                                     context.toast(context.getString(R.string.lists_episode_has_not_aired_yet))
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().alpha(contentAlpha).padding(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Netflix-mobile-style horizontal thumbnail (left) with the continue-watching
                // indicator integrated into the bottom of the cover
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .alpha(if (isWatched) 0.45f else 1f)
                ) {
                        if (!image.isNullOrEmpty()) {
                            AsyncImage(model = image,                             contentDescription = stringResource(R.string.lists_episode, episodeNumber), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            // Gradient placeholder with a faint episode number watermark
                            Box(
                                modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color(0xFF2B2B2B), Color(0xFF141414))))
                            )
                            Text(
                                text = "$episodeNumber",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        // Bottom scrim
                        Box(
                            modifier = Modifier.fillMaxWidth().height(30.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        )
if (hasAired) {
                            FilledTonalIconButton(
                                onClick = onPlay,
                                modifier = Modifier.align(Alignment.Center).size(36.dp),
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.6f),
                                    contentColor = Color.White
                                )
                            ) {
                                 Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.lists_play), modifier = Modifier.size(22.dp))
                            }
                        }
                        // Continue watching bar, flush with the bottom of the cover
                        // (the cover's clip rounds its ends)
                        if (hasProgress) {
                            LinearProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f),
                                gapSize = 0.dp
                            )
                        }
                    }
                // Right side: title, status dot, progress, description
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f).padding(top = 2.dp).alpha(if (isWatched) 0.55f else 1f)
                ) {
                    if (hideDescription) {
                         Text(
                             text = stringResource(R.string.lists_episode, episodeNumber),
                             style = MaterialTheme.typography.titleSmall,
                             fontWeight = FontWeight.Bold,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Text(
                         text = title?.ifEmpty { stringResource(R.string.lists_episode, episodeNumber) } ?: stringResource(R.string.lists_episode, episodeNumber),
                         style = MaterialTheme.typography.titleMedium,
                         fontWeight = FontWeight.SemiBold,
                         maxLines = 4,
                         overflow = TextOverflow.Ellipsis,
                         color = MaterialTheme.colorScheme.onSurface
                     )
                    if (!hideDescription && !description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = if (descriptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                descriptionTruncated = result.isLineEllipsized((result.lineCount - 1).coerceAtLeast(0))
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (descriptionExpanded) {
                            Text(
                                 text = stringResource(R.string.lists_less),
                                 style = MaterialTheme.typography.labelMedium,
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.primary,
                                 modifier = Modifier
                                     .padding(top = 2.dp)
                                     .clip(RoundedCornerShape(6.dp))
                                     .clickable { descriptionExpanded = false }
                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                             )
                        } else if (descriptionTruncated) {
                            Text(
                                 text = stringResource(R.string.lists_more),
                                 style = MaterialTheme.typography.labelMedium,
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.primary,
                                 modifier = Modifier
                                     .padding(top = 2.dp)
                                     .clip(RoundedCornerShape(6.dp))
                                     .clickable { descriptionExpanded = true }
                                     .padding(horizontal = 4.dp, vertical = 2.dp)
                             )
                        }
                    }
                }
            }
        }
    }
}
