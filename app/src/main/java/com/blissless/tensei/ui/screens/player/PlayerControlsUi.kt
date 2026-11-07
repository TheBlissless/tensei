package com.blissless.tensei.ui.screens.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.blissless.tensei.R
import com.blissless.tensei.data.models.EpisodeTimestamps
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.ui.theme.AppIconBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.player.engine.PlayerEngine
import androidx.compose.foundation.layout.BoxScope
import kotlinx.coroutines.CoroutineScope

/**
 * The player's on-video controls: top/bottom bars, gradients, the scrub bar,
 * server/quality/speed/subtitle menus, skip buttons and the volume/brightness
 * overlays plus the subtitle-settings dialog.
 *
 * Extracted verbatim from the [PlayerScreen] composable (the region below the
 * gesture zones); operates on the shared [PlayerScreenState] holder and the
 * player's local operations passed in as functions.
 */
@Composable
fun BoxScope.PlayerControlsUi(
    s: PlayerScreenState,
    context: android.content.Context,
    scope: CoroutineScope,
    engine: PlayerEngine,
    videoUrl: String,
    animeName: String,
    episodeTitle: String?,
    currentEpisode: Int,
    totalEpisodes: Int,
    currentServerName: String,
    currentCategory: String,
    extensionServers: List<ServerInfo>,
    extensionName: String,
    isFullscreen: Boolean,
    isCompact: Boolean,
    isLatestEpisode: Boolean,
    isLoadingStream: Boolean,
    isOled: Boolean,
    showBufferIndicator: Boolean,
    supportsPiP: Boolean,
    autoPlayNextEpisode: Boolean,
    swipeVolume: Boolean,
    swipeBrightness: Boolean,
    swipeSwap: Boolean,
    subtitleUrl: String?,
    subtitleTracks: List<eu.kanade.tachiyomi.animesource.model.Track>,
    resizeModes: List<Pair<Int, String>>,
    effectiveTimestamps: EpisodeTimestamps,
    subServers: List<ServerInfo>,
    dubServers: List<ServerInfo>,
    introStartRatio: Float?,
    introEndRatio: Float?,
    creditsStartRatio: Float?,
    creditsAtEnd: Boolean,
    disableMaterialColors: Boolean,
    onBackClick: (() -> Unit)?,
    onPreviousEpisode: (() -> Unit)?,
    onNextEpisode: (() -> Unit)?,
    onPiPToggle: ((Boolean) -> Unit)?,
    onServerChange: ((serverName: String, category: String) -> Unit)?,
    onExtensionServerChange: ((hosterName: String) -> Unit)?,
    onAutoPlayNextEpisodeChanged: ((Boolean) -> Unit)?,
    onSwipeVolumeChange: ((Boolean) -> Unit)?,
    onSwipeBrightnessChange: ((Boolean) -> Unit)?,
    onSwipeSwapChange: ((Boolean) -> Unit)?,
    toggleFullscreen: () -> Unit,
    exitFullscreen: () -> Unit,
    seekToPosition: (Long) -> Unit,
    performManualSeek: (Long) -> Unit,
    handleServerChange: (serverName: String, category: String) -> Unit,
    handlePlaybackError: () -> Unit,
    rebuildWithSubtitles: (Boolean) -> Unit,
    getActiveSubtitleSettings: () -> SubtitleSettings,
    saveSubtitleProfileData: (SubtitleProfileData) -> Unit,
    formatTime: (Long) -> String,
) {
    // Controls UI with darkening overlay (drawn first)
    AnimatedVisibility(
        visible = s.controlsVisible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(100)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Darkening overlay when controls are visible
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (s.controlsVisible) 0.3f else 0f))
            )

            // Top gradient - slides from top
            AnimatedVisibility(
                visible = s.controlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(200)),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(100)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)))
                        .padding(if (isCompact) 4.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (isCompact) Modifier else Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    exitFullscreen()
                                    onBackClick?.invoke()
                                },
                                modifier = Modifier.size(if (isCompact) 32.dp else 40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.player_back),
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            if (!isCompact) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    if (animeName.isNotEmpty()) {
                                        Text(
                                            text = animeName,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!episodeTitle.isNullOrEmpty()) {
                                            Text(
                                                text = episodeTitle,
                                                color = Color.White.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text("·", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(
                                            text = stringResource(R.string.player_episode_label, currentEpisode) + if (totalEpisodes > 0) "/$totalEpisodes" else "",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        if (s.isFetchingTimestamps || s.isChangingServer) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = if (disableMaterialColors) Color.White else playerAccentColor()
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Compact (portrait phone) layout: mirrors the non-compact
                                // (fullscreen) layout — a 2-line Column with the anime name
                                // on top, then a Row with the episode title + "·" + "Episode X/Y".
                                // Previously this was a single condensed line that only showed
                                // one of (episode title | anime name), which was less informative
                                // than the fullscreen layout. Now both layouts show the same
                                // information, just with smaller typography in compact mode to
                                // fit the narrower top bar.
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    if (animeName.isNotEmpty()) {
                                        Text(
                                            text = animeName,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!episodeTitle.isNullOrEmpty()) {
                                            Text(
                                                text = episodeTitle,
                                                color = Color.White.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Text("·", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                                        }
                                        Text(
                                            text = stringResource(R.string.player_episode_label, currentEpisode) + if (totalEpisodes > 0) "/$totalEpisodes" else "",
                                            color = Color.White.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                        if (s.isFetchingTimestamps || s.isChangingServer) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.5.dp,
                                                color = if (disableMaterialColors) Color.White else playerAccentColor()
                                            )
                                        }
                                    }
                                }
                            }

                        }

                        fun catFromName(name: String): String = when {
                            name.contains("dub", ignoreCase = true) -> "DUB"
                            name.contains("sub", ignoreCase = true) -> "SUB"
                            extensionServers.isNotEmpty() -> extensionName.ifEmpty { "EXT" }
                            else -> "EXT"
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 8.dp), modifier = Modifier.width(IntrinsicSize.Max)) {
                            // Server selector
                            if (!isCompact && ((onServerChange != null && (subServers.isNotEmpty() || dubServers.isNotEmpty())) || extensionServers.isNotEmpty())) {
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                                        color = if (isCompact) Color.Transparent else Color.Black.copy(alpha = 0.5f),
                                        onClick = { s.showServerMenu = true }
                                    ) {
                                    Row(
                                        modifier = Modifier
                                            .defaultMinSize(minWidth = if (isCompact) 28.dp else 44.dp)
                                            .padding(horizontal = if (isCompact) 6.dp else 12.dp, vertical = if (isCompact) 2.dp else 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = currentServerName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val serverCat = if (extensionServers.isNotEmpty()) catFromName(currentServerName) else currentCategory.uppercase()
                                            Text(
                                                text = serverCat,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = s.showServerMenu,
                                        onDismissRequest = { s.showServerMenu = false },
                                        modifier = Modifier.width(playerMenuWidth(280.dp))
                                    ) {
                                        val headerCat = if (extensionServers.isNotEmpty()) catFromName(currentServerName) else currentCategory.uppercase()
                                        Text(
                                            text = "${currentServerName.uppercase()} ($headerCat)",
                                            color = playerMenuAccentColor(),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        if (extensionServers.isNotEmpty()) {
                                            val extSubServers = extensionServers.filter { it.name.contains("sub", ignoreCase = true) || !it.name.contains("dub", ignoreCase = true) }
                                            val extDubServers = extensionServers.filter { it.name.contains("dub", ignoreCase = true) && !it.name.contains("sub", ignoreCase = true) }
                                            if (extSubServers.isNotEmpty()) {
                                                Text("SUB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                                 extSubServers.forEach { server ->
                                                    ServerSelectorButton(
                                                        serverName = server.name,
                                                        isSelected = server.name == currentServerName,
                                                        qualities = server.qualities.map { it.quality },
                                                        onClick = {
                                                            s.showServerMenu = false
                                                            s.autoRetryServers.clear()
                                                            s.pendingAutoRetry = null
                                                            onExtensionServerChange?.invoke(server.url)
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            if (extDubServers.isNotEmpty()) {
                                                Text("DUB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                                 extDubServers.forEach { server ->
                                                    ServerSelectorButton(
                                                        serverName = server.name,
                                                        isSelected = server.name == currentServerName,
                                                        qualities = server.qualities.map { it.quality },
                                                        onClick = {
                                                            s.showServerMenu = false
                                                            s.autoRetryServers.clear()
                                                            s.pendingAutoRetry = null
                                                            onExtensionServerChange?.invoke(server.url)
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            if (extSubServers.isEmpty() && extDubServers.isEmpty()) {
                                                Text(extensionName.ifEmpty { "EXT" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                                 extensionServers.forEach { server ->
                                                    ServerSelectorButton(
                                                        serverName = server.name,
                                                        isSelected = server.name == currentServerName,
                                                        qualities = server.qualities.map { it.quality },
                                                        onClick = {
                                                            s.showServerMenu = false
                                                            s.autoRetryServers.clear()
                                                            s.pendingAutoRetry = null
                                                            onExtensionServerChange?.invoke(server.url)
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                        if (subServers.isNotEmpty()) {
                                            Text("SUB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                            subServers.forEach { server ->
                                                ServerSelectorButton(
                                                    serverName = server.name,
                                                    isSelected = server.name == currentServerName && currentCategory == "sub",
                                                    onClick = {
                                                        s.showServerMenu = false
                                                        handleServerChange(server.name, "sub")
                                                    }
                                                )
                                            }
                                        }
                                        if (dubServers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("DUB", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                                            dubServers.forEach { server ->
                                                ServerSelectorButton(
                                                    serverName = server.name,
                                                    isSelected = server.name == currentServerName && currentCategory == "dub",
                                                    onClick = {
                                                        s.showServerMenu = false
                                                        handleServerChange(server.name, "dub")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // CC/Subtitles button
                            val hasExternalSubs = subtitleTracks.isNotEmpty() || subtitleUrl != null
                            if (!isCompact && hasExternalSubs) {
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(if (isCompact) 10.dp else 14.dp),
                                        color = if (isCompact) Color.Transparent else Color.Black.copy(alpha = 0.5f),
                                        onClick = { s.showSubtitleMenu = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = if (isCompact) 6.dp else 12.dp, vertical = if (isCompact) 2.dp else 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.ClosedCaption,
                                                contentDescription = stringResource(R.string.player_subtitles),
                                                tint = if (s.subtitlesEnabled) Color.White else Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                                            )
                                        }
                                    }

                                    var subtitleSettingsView by remember(s.showSubtitleMenu) { mutableStateOf(false) }

                                    DropdownMenu(
                                        expanded = s.showSubtitleMenu,
                                        onDismissRequest = { s.showSubtitleMenu = false },
                                        modifier = Modifier.width(playerMenuWidth(220.dp))
                                    ) {
                                        if (subtitleSettingsView) {
                                            Text(
                                                stringResource(R.string.player_translation_profiles),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                            s.subtitleProfileData.profiles.forEachIndexed { index, profile ->
                                                val isActive = index == s.subtitleProfileData.activeProfileIndex
                                                DropdownMenuItem(
                                                    text = { Text(profile.profileName, color = if (isActive) playerMenuAccentColor() else MaterialTheme.colorScheme.onSurface) },
                                                    onClick = {
                                                        val data = s.subtitleProfileData
                                                        saveSubtitleProfileData(data.copy(activeProfileIndex = index))
                                                        subtitleSettingsView = false
                                                        s.showSubtitleMenu = false
                                                    },
                                                    leadingIcon = if (isActive) { { Icon(Icons.Default.Check, null, tint = playerMenuAccentColor()) } } else null
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(stringResource(R.string.player_edit_subtitles), color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                },
                                                onClick = {
                                                    s.showSubtitleMenu = false
                                                    subtitleSettingsView = false
                                                    s.showSubtitleSettings = true
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.player_off), color = if (!s.subtitlesEnabled) playerMenuAccentColor() else MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    if (s.subtitlesEnabled) rebuildWithSubtitles(false)
                                                    s.selectedEmbeddedTrackIndex = -1
                                                    engine.disableSubtitles()
                                                    s.showSubtitleMenu = false
                                                },
                                                leadingIcon = if (!s.subtitlesEnabled) { { Icon(Icons.Default.Check, null, tint = playerMenuAccentColor()) } } else null
                                            )
                                            val externalTrackList = subtitleTracks.ifEmpty {
                                                if (subtitleUrl != null) listOf(eu.kanade.tachiyomi.animesource.model.Track(subtitleUrl, "en"))
                                                else emptyList()
                                            }
                                            if (externalTrackList.isNotEmpty()) {
                                                Text(stringResource(R.string.player_external), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                                externalTrackList.forEachIndexed { index, track ->
                                                    val isSelected = s.subtitlesEnabled && s.selectedSubtitleIndex == index && s.selectedEmbeddedTrackIndex < 0
                                                    DropdownMenuItem(
                                                        text = { Text(track.lang.uppercase(), color = if (isSelected) playerMenuAccentColor() else MaterialTheme.colorScheme.onSurface) },
                                                        onClick = {
                                                            s.selectedSubtitleIndex = index
                                                            s.selectedEmbeddedTrackIndex = -1
                                                            rebuildWithSubtitles(true)
                                                            s.showSubtitleMenu = false
                                                        },
                                                        leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, tint = playerMenuAccentColor()) } } else null
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            DropdownMenuItem(
                                                text = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(stringResource(R.string.player_settings), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                },
                                                onClick = { subtitleSettingsView = true }
                                            )
                                        }
                                    }
                                }
                            }

                            // Resize button
                            if (!isCompact) {
                                ResizeButton(
                                    onClick = { s.resizeModeIndex = (s.resizeModeIndex + 1) % resizeModes.size },
                                )
                            }

                            // Player settings button
                            PlayerSettingsButton(
                                showMenu = s.showPlayerSettings,
                                onShowMenuChange = { s.showPlayerSettings = it },
                                swipeVolume = swipeVolume,
                                swipeBrightness = swipeBrightness,
                                swipeSwap = swipeSwap,
                                onSwipeVolumeChange = { onSwipeVolumeChange?.invoke(it) },
                                onSwipeBrightnessChange = { onSwipeBrightnessChange?.invoke(it) },
                                onSwipeSwapChange = { onSwipeSwapChange?.invoke(it) },
                                isCompact = isCompact,
                                autoPlayNextEpisode = autoPlayNextEpisode,
                                onAutoPlayChange = { onAutoPlayNextEpisodeChanged?.invoke(it) },
                                supportsPiP = supportsPiP,
                                onPiPToggle = { onPiPToggle?.invoke(it) },
                            )

                        }
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.Center)) {
                AnimatedVisibility(
                    visible = s.controlsVisible,
                    enter = scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                    exit = scaleOut(targetScale = 0.8f, animationSpec = tween(100)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 32.dp else 40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onPreviousEpisode?.invoke() },
                            modifier = Modifier.size(if (isCompact) 40.dp else 56.dp).then(if (!isCompact) Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape) else Modifier).alpha(if (onPreviousEpisode != null && !isLoadingStream && !s.isChangingServer) 1f else 0.3f),
                            enabled = onPreviousEpisode != null && !isLoadingStream && !s.isChangingServer
                        ) {
                            Icon(Icons.Default.SkipPrevious, stringResource(R.string.player_previous_episode), tint = Color.White, modifier = Modifier.size(if (isCompact) 24.dp else 32.dp))
                        }

                        IconButton(
                            onClick = {
                                if (s.hasError) {
                                    handlePlaybackError()
                                    engine.prepare()
                                    engine.playWhenReady = true
                                } else {
                                    if (s.isPlaying) engine.pause() else engine.play()
                                }
                            },
                            modifier = Modifier.size(if (isCompact) 52.dp else 72.dp).then(if (!isCompact) Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape) else Modifier)
                        ) {
                            if (s.isBuffering || s.isOffline) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(if (isCompact) 28.dp else 42.dp),
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (s.hasError) Icons.Default.Refresh else if (s.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (s.hasError) stringResource(R.string.player_retry) else if (s.isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isCompact) 30.dp else 42.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onNextEpisode?.invoke()
                            },
                            modifier = Modifier.size(if (isCompact) 40.dp else 56.dp).then(if (!isCompact) Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape) else Modifier).alpha(if (onNextEpisode != null && !isLatestEpisode && !isLoadingStream && !s.isChangingServer) 1f else 0.3f),
                            enabled = onNextEpisode != null && !isLatestEpisode && !isLoadingStream && !s.isChangingServer
                        ) {
                            Icon(Icons.Default.SkipNext, stringResource(R.string.player_next_episode), tint = Color.White, modifier = Modifier.size(if (isCompact) 24.dp else 32.dp))
                        }
                    }
                }

                SkipIndicatorOverlay(
                    visible = s.showSkipIndicator && !s.skipIsForward,
                    isForward = false,
                    text = s.skipIndicatorText,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-120).dp),
                )

                SkipIndicatorOverlay(
                    visible = s.showSkipIndicator && s.skipIsForward,
                    isForward = true,
                    text = s.skipIndicatorText,
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = 120.dp),
                )
            }

            if (isLoadingStream || s.isChangingServer) {
                PlayerLoadingIndicator(modifier = Modifier.align(Alignment.Center).offset(y = 64.dp))
            }

            if (s.hasError && s.playbackError != null) {
                val hasMoreServers = if (extensionServers.isNotEmpty()) {
                    extensionServers.any { it.name !in s.autoRetryServers && it.url != videoUrl }
                } else {
                    onServerChange != null && let {
                        val servers = if (currentCategory == "sub") subServers else dubServers
                        servers.size > 1
                    }
                }
                StreamErrorOverlay(
                    errorMessage = s.playbackError ?: stringResource(R.string.player_unknown_error),
                    showTryNextServer = hasMoreServers,
                    onTryNextServer = {
                        if (extensionServers.isNotEmpty()) {
                            s.autoRetryServers.add(currentServerName)
                            extensionServers.find { it.url == videoUrl }?.let { s.autoRetryServers.add(it.name) }
                            fun srvCat2(name: String): String = when {
                                name.contains("dub", ignoreCase = true) -> "dub"
                                name.contains("sub", ignoreCase = true) -> "sub"
                                else -> "other"
                            }
                            val curCat = srvCat2(currentServerName)
                            val remaining = extensionServers.filter {
                                it.name !in s.autoRetryServers && it.url != videoUrl
                            }
                            val sameCat = remaining.filter { srvCat2(it.name) == curCat }
                            val otherCat = remaining.filter { srvCat2(it.name) != curCat }
                            val next = (sameCat + otherCat).firstOrNull()
                            if (next != null) {
                                // Pass URL (not name) — matches the
                                // URL-based lookup in handleExtensionServerChange.
                                s.pendingAutoRetry = next.url
                            }
                        } else {
                            val servers = if (currentCategory == "sub") subServers else dubServers
                            val currentIndex = servers.indexOfFirst { it.name == currentServerName }
                            val nextIndex = (currentIndex + 1) % servers.size
                            handleServerChange(servers[nextIndex].name, currentCategory)
                        }
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // Bottom gradient - slides from bottom
            AnimatedVisibility(
                visible = s.controlsVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(100)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                        .then(if (isCompact) Modifier else Modifier.navigationBarsPadding())
                        .pointerInput(Unit) { detectTapGestures(onTap = { s.showControls = !s.showControls }) }
                        .padding(horizontal = if (isCompact) 10.dp else 16.dp)
                        .padding(bottom = if (isCompact) 2.dp else 12.dp, top = if (isCompact) 12.dp else 12.dp)
                ) {
                    // YouTube-style timestamp, all on the LEFT: elapsed / total by default, and
                    // pressing it swaps the elapsed part to the time left (time left / total).
                    // Nothing on the right.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timestampText = if (s.duration > 0) {
                            val firstPart = if (s.showTimeLeft) {
                                "-${formatTime((s.duration - s.currentPosition).coerceAtLeast(0L))}"
                            } else {
                                formatTime(s.currentPosition)
                            }
                            "$firstPart / ${formatTime(s.duration)}"
                        } else {
                            formatTime(s.currentPosition)
                        }
                        Text(
                            text = timestampText,
                            color = Color.White,
                            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { s.showTimeLeft = !s.showTimeLeft }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

                    // YouTube-style scrub bar: a thin line whose thickness and dot grow
                    // while scrubbing and shrink the moment the finger lifts.
                    val isScrubbing = s.isDragging
                    val scrubTrackHeight by animateDpAsState(
                        targetValue = if (isScrubbing) (if (isCompact) 4.dp else 6.dp) else (if (isCompact) 2.dp else 3.dp),
                        animationSpec = tween(150),
                        label = "scrubTrackHeight"
                    )
                    val scrubThumbRadius by animateDpAsState(
                        targetValue = if (isScrubbing) (if (isCompact) 6.dp else 8.dp) else (if (isCompact) 3.dp else 4.dp),
                        animationSpec = tween(150),
                        label = "scrubThumbRadius"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 14.dp else 24.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { offset ->
                                        if (s.duration > 0) {
                                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                            val seekPosition = (ratio * s.duration).toLong()
                                            performManualSeek(seekPosition)
                                        }
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        s.isDragging = true
                                        s.isManuallySeeking = true
                                        s.wasPlayingBeforeScrub = s.isPlaying
                                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                                        s.sliderValue = ratio * (if (s.duration > 0) s.duration.toFloat() else 1000f)
                                        s.currentPosition = s.sliderValue.toLong()
                                    },
                                    onDragEnd = {
                                        s.isDragging = false
                                        s.fastHideUntil = System.currentTimeMillis() + 3500
                                        val seekPos = s.sliderValue.toLong()
                                        seekToPosition(seekPos)
                                        s.skipResetJob?.cancel()
                                        s.skipResetJob = scope.launch {
                                            delay(1500.milliseconds)
                                            s.isManuallySeeking = false
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        val currentRatio = s.sliderValue / (if (s.duration > 0) s.duration.toFloat() else 1000f)
                                        val newRatio = (currentRatio + dragAmount / size.width).coerceIn(0f, 1f)
                                        s.sliderValue = newRatio * (if (s.duration > 0) s.duration.toFloat() else 1000f)
                                        s.currentPosition = s.sliderValue.toLong()
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sliderWidth = size.width
                            val trackHeight = scrubTrackHeight.toPx()
                            val trackTop = (size.height - trackHeight) / 2f
                            val cornerRadius = trackHeight / 2f
                            val thumbRadiusPx = scrubThumbRadius.toPx()

                            if (s.duration > 0) {
                                val progressRatio = s.currentPosition.toFloat() / s.duration
                                val bufferedRatio = s.maxBufferedPosition.toFloat() / s.duration

                                // Draw inactive track background
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.3f),
                                    topLeft = Offset(0f, trackTop),
                                    size = Size(sliderWidth, trackHeight),
                                    cornerRadius = CornerRadius(cornerRadius)
                                )

                                // Draw buffer indicator
                                val bufferWidth = (bufferedRatio - progressRatio) * sliderWidth
                                if (showBufferIndicator && bufferWidth >= 2.dp.toPx()) {
                                    val bufferStartX = progressRatio * sliderWidth
                                    val bufferEndX = bufferedRatio * sliderWidth
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.5f),
                                        topLeft = Offset(bufferStartX, trackTop),
                                        size = Size(bufferEndX - bufferStartX, trackHeight),
                                        cornerRadius = CornerRadius(cornerRadius)
                                    )
                                }

                                // Draw active track (played portion) in the app icon blue
                                val progressX = progressRatio * sliderWidth
                                drawRoundRect(
                                    color = AppIconBlue,
                                    topLeft = Offset(0f, trackTop),
                                    size = Size(progressX.coerceAtLeast(thumbRadiusPx), trackHeight),
                                    cornerRadius = CornerRadius(cornerRadius)
                                )

                                // Intro/credits markers, drawn only over the part the playhead
                                // has not reached — the blue fill already covers the watched part,
                                // so a second "watched" shade would only muddy it.
                                val markerColor = Color(0xFFFF9800)
                                if (introStartRatio != null && introEndRatio != null) {
                                    val markerEndX = (introEndRatio * sliderWidth).coerceAtMost(sliderWidth)
                                    val visibleStartX = (introStartRatio * sliderWidth).coerceAtLeast(progressX)
                                    if (markerEndX - visibleStartX > 0f) {
                                        drawRoundRect(
                                            color = markerColor.copy(alpha = 0.85f),
                                            topLeft = Offset(visibleStartX, trackTop),
                                            size = Size(markerEndX - visibleStartX, trackHeight),
                                            cornerRadius = CornerRadius(cornerRadius)
                                        )
                                    }
                                }

                                if (creditsStartRatio != null) {
                                    val creditsStartX = creditsStartRatio * sliderWidth
                                    if (creditsStartX > 0f && creditsStartX < sliderWidth) {
                                        val visibleStartX = creditsStartX.coerceAtLeast(progressX)
                                        drawRoundRect(
                                            color = markerColor.copy(alpha = 0.85f),
                                            topLeft = Offset(visibleStartX, trackTop),
                                            size = Size((sliderWidth - visibleStartX).coerceAtLeast(0f), trackHeight),
                                            cornerRadius = CornerRadius(cornerRadius)
                                        )
                                    }
                                }

                                // Draw the thumb as a circle
                                drawCircle(
                                    color = AppIconBlue,
                                    radius = thumbRadiusPx,
                                    center = Offset(progressX, size.height / 2)
                                )
                            }
                        }
                    }

                    // Remaining time lives in the tappable left indicator above the bar (YouTube-style),
                    // so no second "-mm:ss" line here.

                    // Bottom row with speed selector on left and time on right
                    var currentSpeed by rememberSaveable { mutableFloatStateOf(1f) }
                    val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = if (isCompact) 2.dp else 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback speed selector on the left
                        PlaybackSpeedSelector(
                            currentSpeed = currentSpeed,
                            showMenu = s.showSpeedMenu,
                            onShowMenuChange = { s.showSpeedMenu = it },
                            onSpeedChange = { speed ->
                                currentSpeed = speed
                                engine.setPlaybackSpeed(speed)
                            },
                            isCompact = isCompact,
                        )

                        // Autoplay + Fullscreen (linked)
                        if (isCompact) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.Transparent,
                                onClick = { toggleFullscreen() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullscreen) stringResource(R.string.player_exit_fullscreen) else stringResource(R.string.player_fullscreen),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            AutoplayFullscreenRow(
                                autoPlayNextEpisode = autoPlayNextEpisode,
                                isFullscreen = isFullscreen,
                                onAutoPlayChange = { onAutoPlayNextEpisodeChanged?.invoke(it) },
                                onFullscreenToggle = { toggleFullscreen() },
                                isCompact = isCompact,
                                isOled = isOled,
                            )
                        }
                    }
                    if (isCompact) {
                        // The compact bottom title row (episode title + Ep X/Y) has been
                        // removed — the same information is now shown in the compact top
                        // bar (which mirrors the fullscreen 2-line layout: anime name on
                        // top, episode title + "·" + "Episode X/Y" on the bottom). Keeping
                        // it in both places was redundant and crowded the bottom controls
                        // area. A small spacer is kept so the speed/fullscreen row doesn't
                        // sit flush against the scrubber.
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    }

    // Skip Opening/Ending buttons - outside controls AnimatedVisibility so they're visible even when UI is hidden
    SkipButtonsOverlay(
        showSkipOpening = s.showSkipOpeningButton,
        showSkipEnding = s.showSkipEndingButton,
        isLatestEpisode = isLatestEpisode,
        creditsAtEnd = creditsAtEnd,
        isChangingServer = s.isChangingServer,
        onSkipOpening = {
            val ts = effectiveTimestamps
            if (ts.introEnd != null) {
                engine.seekTo(ts.introEnd * 1000L)
                if (engine.isPlaying) {
                    engine.play()
                }
                s.hasSkippedIntro = true
            }
        },
        onSkipEnding = {
            if (isLatestEpisode || !creditsAtEnd) {
                if (engine.duration > 0) {
                    engine.seekTo(engine.duration)
                }
            } else if (!s.isChangingServer) {
                onNextEpisode?.invoke()
            }
        },
        isCompact = isCompact,
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = if (isCompact) 8.dp else 16.dp),
    )

    // Volume Overlay Indicator (on top of controls)
    // When swipeSwap is true, volume shows on the right; otherwise left
    VolumeOverlay(
        visible = s.showVolumeOverlay,
        volume = s.playerVolume,
        disableMaterialColors = disableMaterialColors,
        modifier = Modifier.align(if (swipeSwap) Alignment.CenterEnd else Alignment.CenterStart),
    )

    // Brightness Overlay Indicator (on top of controls)
    // When swipeSwap is true, brightness shows on the left; otherwise right
    BrightnessOverlay(
        visible = s.showBrightnessOverlay,
        brightness = s.currentBrightness,
        disableMaterialColors = disableMaterialColors,
        modifier = Modifier.align(if (swipeSwap) Alignment.CenterStart else Alignment.CenterEnd),
    )

    // Subtitle Settings full-screen overlay
    if (s.showSubtitleSettings) {
        SubtitleSettingsDialog(
            currentSettings = getActiveSubtitleSettings(),
            profiles = s.subtitleProfileData.profiles,
            activeProfileIndex = s.subtitleProfileData.activeProfileIndex,
            onSettingsChange = { newSettings ->
                val data = s.subtitleProfileData
                val updatedProfiles = data.profiles.toMutableList().also {
                    it[data.activeProfileIndex] = newSettings
                }
                saveSubtitleProfileData(data.copy(profiles = updatedProfiles))
            },
            onProfileSelect = { index ->
                val data = s.subtitleProfileData
                saveSubtitleProfileData(data.copy(activeProfileIndex = index))
            },
            onResetProfile = { index ->
                val data = s.subtitleProfileData
                val updatedProfiles = data.profiles.toMutableList().also {
                    it[index] = SubtitleSettings(profileName = context.getString(R.string.player_default_profile_name, index + 1))
                }
                saveSubtitleProfileData(data.copy(profiles = updatedProfiles))
            },
            onRenameProfile = { index, name ->
                val data = s.subtitleProfileData
                val updated = data.profiles[index].copy(profileName = name)
                val updatedProfiles = data.profiles.toMutableList().also {
                    it[index] = updated
                }
                saveSubtitleProfileData(data.copy(profiles = updatedProfiles))
            },
            onDismiss = { s.showSubtitleSettings = false },
            onSave = {
                saveSubtitleProfileData(s.subtitleProfileData)
            }
        )
    }
}
