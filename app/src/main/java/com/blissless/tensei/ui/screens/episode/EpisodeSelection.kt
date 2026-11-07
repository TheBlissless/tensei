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
import com.blissless.tensei.viewmodel.getCachedTmdbEpisodes
import com.blissless.tensei.viewmodel.fetchTmdbEpisodes
import com.blissless.tensei.viewmodel.cacheTmdbEpisodes

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichEpisodeScreen(
    anime: AnimeMedia,
    viewModel: MainViewModel,
    isOled: Boolean,
    onDismiss: () -> Unit,
    onEpisodeSelect: (Int, String?) -> Unit,
    preferEnglishTitles: Boolean = true,
) {
    val context = LocalContext.current
    val total = anime.totalEpisodes
    val released = anime.latestEpisode ?: total
    val episodeCount = if (total > 0) total else released.coerceAtLeast(1)
    val currentProgress = anime.progress
    val displayTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
    val playbackPositions by viewModel.playbackPositions.collectAsState()
    val playbackDurations by viewModel.playbackDurations.collectAsState()
    val hideEpisodeDescriptions by viewModel.hideEpisodeDescription.collectAsState(initial = true)

    var tmdbEpisodes by remember { mutableStateOf<List<TmdbEpisode>>(emptyList()) }
    var isLoadingEpisodes by remember { mutableStateOf(true) }
    var selectedEpisode by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Extension matching state
    val availableExtensions by viewModel.availableExtensions.collectAsState()
    val defaultPkg by viewModel.defaultExtensionPackage.collectAsState()
    var selectedExtensionPkg by remember { mutableStateOf(defaultPkg.ifEmpty { null }) }
    var extensionEpisodesNumbers by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var hasExtensionData by remember { mutableStateOf(false) }
    var isLoadingExtensionEpisodes by remember { mutableStateOf(false) }
    var isExtensionReady by remember { mutableStateOf(false) }
    var extensionError by remember { mutableStateOf<String?>(null) }

    // Magnet extension state
    val availableMagnetExtensions by viewModel.availableMagnetExtensions.collectAsState()
    val magnetEpisodesData by viewModel.magnetEpisodes.collectAsState()
    val defaultMagnetExtension by viewModel.defaultMagnetExtension.collectAsState()
    val currentStreamMethod by viewModel.streamMethod.collectAsState()
    var selectedMagnetAuthority by remember { mutableStateOf<String?>(null) }
    var isMagnetActive by remember { mutableStateOf(false) }
    var isLoadingMagnetEpisodes by remember { mutableStateOf(false) }
    var magnetError by remember { mutableStateOf<String?>(null) }
    var isMagnetReady by remember { mutableStateOf(false) }

    // Tensei stream extension state
    val availableStreamExtensions by viewModel.availableStreamExtensions.collectAsState()
    val defaultStreamExt by viewModel.defaultStreamExtension.collectAsState()
    var selectedStreamAuthority by remember { mutableStateOf<String?>(null) }
    var isStreamActive by remember { mutableStateOf(false) }

    // Auto-select state flags (declared before LaunchedEffects that reference them)
    var hasAutoSelectedMagnet by remember { mutableStateOf(false) }
    var hasAutoSelectedStream by remember { mutableStateOf(false) }

    // Sync selectedExtensionPkg with default when it becomes available
    // Also re-evaluate when stream method changes
    LaunchedEffect(defaultPkg, currentStreamMethod, defaultStreamExt) {
        if (currentStreamMethod == "direct") {
            if (defaultPkg.isNotEmpty() && selectedExtensionPkg == null && selectedStreamAuthority == null) {
                selectedExtensionPkg = defaultPkg
            }
        } else {
            // magnet / tensei mode: auto-select stream extension if saved
            if (defaultStreamExt != null && selectedStreamAuthority == null && !hasAutoSelectedStream && !hasAutoSelectedMagnet) {
                hasAutoSelectedStream = true
                selectedStreamAuthority = defaultStreamExt
                isStreamActive = true
                selectedExtensionPkg = null
                isMagnetActive = false
            }
        }
    }

    // Auto-select magnet extension when stream method is magnet
    LaunchedEffect(defaultMagnetExtension, currentStreamMethod) {
        val ext = defaultMagnetExtension
        if (currentStreamMethod != "magnet") {
            hasAutoSelectedMagnet = false
            hasAutoSelectedStream = false
        } else if (!hasAutoSelectedMagnet && !hasAutoSelectedStream && ext != null) {
            hasAutoSelectedMagnet = true
            selectedMagnetAuthority = ext
            isMagnetActive = true
            selectedExtensionPkg = null
            isLoadingMagnetEpisodes = true
            magnetError = null
            isMagnetReady = false
            viewModel.clearMagnetEpisodes(anime.id)
            viewModel.fetchMagnetEpisodes(anime, ext)
        }
    }

    // Animation states for entry
    var isVisible by remember { mutableStateOf(false) }
    val slideOffset = remember { Animatable(1000f) }
    val dismissSlideOffset = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        slideOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(200, easing = LinearEasing)
        )
    }
    
    fun dismissWithAnimation() {
        scope.launch {
            dismissSlideOffset.snapTo(0f)
            dismissSlideOffset.animateTo(
                targetValue = 1000f,
                animationSpec = tween(150, easing = LinearEasing)
            )
            onDismiss()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (slideOffset.value > 0 || dismissSlideOffset.value > 0) 0f else 1f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val magnetEpisodeNumbers = remember(magnetEpisodesData, anime.id) {
        val data = magnetEpisodesData[anime.id] ?: return@remember emptySet()
        if (data.isSingleTorrent) return@remember emptySet()
        data.episodes.map { it.episode }.toSet()
    }

    val hasMagnetData = remember(magnetEpisodeNumbers, selectedMagnetAuthority) {
        selectedMagnetAuthority != null && magnetEpisodeNumbers.isNotEmpty()
    }

    LaunchedEffect(magnetEpisodesData, anime.id, isLoadingMagnetEpisodes) {
        if (isLoadingMagnetEpisodes) {
            val data = magnetEpisodesData[anime.id]
            if (data != null) {
                isLoadingMagnetEpisodes = false
                if (data.isSingleTorrent) {
                    magnetError = null
                    isMagnetReady = true
                } else {
                    val count = data.episodes.size
                    if (count <= 0 || count < released) {
                        magnetError = context.getString(R.string.lists_source_not_found)
                        isMagnetReady = false
                    } else {
                        magnetError = null
                        isMagnetReady = true
                    }
                }
            }
        }
    }

    // Filter episodes based on active source
    val displayEpisodes = remember(tmdbEpisodes, episodeCount, isMagnetActive, isMagnetReady, isExtensionReady, selectedExtensionPkg, isStreamActive) {
        val base = tmdbEpisodes.filter { it.episode <= episodeCount }
        when {
            isMagnetActive && isMagnetReady -> base
            isStreamActive -> base
            isExtensionReady -> base
            selectedExtensionPkg == null && !isMagnetActive && !isStreamActive -> base
            else -> emptyList()
        }
    }

    val availableEpisodeNumbers = remember(episodeCount, isMagnetActive, isMagnetReady, isExtensionReady, selectedExtensionPkg, isStreamActive) {
        when {
            isMagnetActive && isMagnetReady -> (1..episodeCount).toList()
            isStreamActive -> (1..episodeCount).toList()
            isExtensionReady -> (1..episodeCount).toList()
            selectedExtensionPkg == null && !isMagnetActive && !isStreamActive -> (1..episodeCount).toList()
            else -> emptyList()
        }
    }

    val windowInfo = LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val screenHeightPx = containerSize.height.toFloat()
    val dismissThreshold = screenHeightPx / 2f

    val offsetY = remember { Animatable(0f) }

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
        }
    }

    // Shared drag-to-dismiss behavior — see ui/components/SheetDismissGestures.kt
    val nestedScrollConnection = com.blissless.tensei.ui.components.rememberSheetDismissScrollConnection(
        offsetY = offsetY,
        isAtTop = { isAtTop },
        dismissThreshold = dismissThreshold,
        onDismiss = { dismissWithAnimation() },
    )

    suspend fun handleDragEnd() {
        val currentOffset = offsetY.value
        if (currentOffset == 0f) return

        val shouldDismiss = currentOffset > dismissThreshold
        if (shouldDismiss) {
            dismissWithAnimation()
        } else {
            offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    // Fetch TMDB episodes when screen opens
    fun hasRealTmdbData(episodes: List<TmdbEpisode>): Boolean =
        episodes.isNotEmpty() && episodes.any { !it.title.startsWith("Episode ") }

    LaunchedEffect(anime.id) {
        val cached = viewModel.getCachedTmdbEpisodes(anime.id, anime.status)
        if (cached != null && hasRealTmdbData(cached)) {
            tmdbEpisodes = cached
            isLoadingEpisodes = false
        } else {
            var retries = 0
            while (retries < 2) {
                try {
                    val episodes = viewModel.fetchTmdbEpisodes(anime.title, anime.id, anime.year, anime.format, animeEpisodes = anime.totalEpisodes.takeIf { it > 0 })
                    viewModel.cacheTmdbEpisodes(anime.id, episodes)
                    tmdbEpisodes = episodes
                    break
                } catch (e: Exception) {
                    Log.e("TmdbDebug", "fetchTmdbEpisodes threw for anime=${anime.id}", e)
                }
                retries++
                if (retries < 2) delay(3000)
            }
            isLoadingEpisodes = false
        }
    }

    // Load available extensions, magnet extensions, and stream extensions
    LaunchedEffect(anime.id) {
        viewModel.loadAvailableExtensions()
        viewModel.loadAvailableMagnetExtensions()
        viewModel.loadAvailableStreamExtensions()
    }

    // Scroll to current episode (next to watch or last watched) with smooth animation
    // Re-triggers when TMDB, extension, or magnet episodes finish loading.
    val isCompleted = anime.listStatus == "COMPLETED" || (anime.listStatus.isBlank() && anime.progress >= anime.totalEpisodes && anime.totalEpisodes > 0)
    LaunchedEffect(currentProgress, episodeCount, isLoadingEpisodes, isLoadingExtensionEpisodes, isLoadingMagnetEpisodes, isCompleted) {
        if (!isCompleted && !isLoadingEpisodes && !isLoadingExtensionEpisodes && !isLoadingMagnetEpisodes && currentProgress > 0) {
            delay(300.milliseconds)
            val scrollIndex = if (currentProgress < episodeCount) currentProgress else currentProgress - 1
            listState.animateScrollToItem(scrollIndex + 1)
        }
    }

    // Watch for extension episode numbers
    val preFetchedNumbers by viewModel.preFetchedEpisodeNumbers.collectAsState()
    LaunchedEffect(preFetchedNumbers, anime.id, isLoadingExtensionEpisodes) {
        if (isLoadingExtensionEpisodes) {
            val numbers = preFetchedNumbers[anime.id]
            if (numbers != null) {
                isLoadingExtensionEpisodes = false
                extensionEpisodesNumbers = numbers
                if (numbers.isEmpty() || numbers.size < released) {
                    extensionError = context.getString(R.string.lists_source_not_found)
                    isExtensionReady = false
                    hasExtensionData = false
                } else {
                    extensionError = null
                    isExtensionReady = true
                    hasExtensionData = true
                }
            }
        }
    }

    // Re-fetch when user selects a specific extension
    LaunchedEffect(selectedExtensionPkg) {
        if (selectedExtensionPkg != null) {
            isLoadingExtensionEpisodes = true
            extensionError = null
            isExtensionReady = false
            hasExtensionData = false
            extensionEpisodesNumbers = emptySet()
            viewModel.preFetchExtensionEpisodes(anime, selectedExtensionPkg)
        } else {
            extensionEpisodesNumbers = emptySet()
            hasExtensionData = false
            isLoadingExtensionEpisodes = false
            extensionError = null
            isExtensionReady = false
        }
    }

    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = true, dismissOnClickOutside = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (slideOffset.value + dismissSlideOffset.value).roundToInt()) }
                .graphicsLayer {
                    this.alpha = alpha
                }
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .background(if (isOled) Color.Black else MaterialTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Netflix-style hero with anime info
            val hasBanner = !anime.banner.isNullOrEmpty() || anime.cover.isNotEmpty()
            if (hasBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = { scope.launch { handleDragEnd() } },
                                onVerticalDrag = { _, dragAmount ->
                                    scope.launch {
                                        val multiplier = 3.0f
                                        offsetY.snapTo((offsetY.value + (dragAmount * multiplier)).coerceAtLeast(0f))
                                    }
                                }
                            )
                        }
                ) {
                    AsyncImage(
                        model = anime.banner.takeIf { !it.isNullOrEmpty() } ?: anime.cover,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().blur(radius = 8.dp).scale(1.04f)
                    )
                    // Top scrim for close-button legibility
                    Box(modifier = Modifier.fillMaxWidth().height(110.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))))
                    // Bottom scrim for text legibility
                    Box(modifier = Modifier.fillMaxWidth().height(170.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(colors = listOf(Color.Transparent, if (isOled) Color.Black else MaterialTheme.colorScheme.background.copy(alpha = 0.92f)))))
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        AsyncImage(
                            model = anime.cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.width(76.dp).height(106.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = displayTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                         Text(text = stringResource(R.string.lists_progress), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "$currentProgress / ${if (total > 0) total else "??"}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                if (released > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.45f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                             Text(
                                                 text = if (released == 1) stringResource(R.string.lists_one_episode_aired) else stringResource(R.string.lists_episodes_aired, released),
                                                 style = MaterialTheme.typography.labelSmall,
                                                 fontWeight = FontWeight.SemiBold,
                                                 color = Color.White.copy(alpha = 0.9f)
                                             )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Watch progress bar along the bottom edge of the banner
                    val heroProgress = if (total > 0) (currentProgress.toFloat() / total).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { heroProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                        color = Color.White,
                        trackColor = Color.Transparent,
                        gapSize = 0.dp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = statusBarsPadding.calculateTopPadding() + 12.dp, start = 16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .zIndex(10f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.lists_back), tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Box(modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarsPadding.calculateTopPadding() + 12.dp)
                .width(36.dp).height(4.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)).zIndex(5f))

            // Scrollable content: chips + episodes in one LazyColumn
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(
                    top = (if (hasBanner) 216.dp + statusBarsPadding.calculateTopPadding() else statusBarsPadding.calculateTopPadding()),
                    bottom = navigationBarsPadding.calculateBottomPadding()
                ),
                state = listState
            ) {
                // Header section (scrolls away)
                item {
                    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Navigation actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val nextEp = currentProgress + 1
                            if (nextEp <= released) {
                                Button(
                                    onClick = { onEpisodeSelect(nextEp, null) },
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                     Text(stringResource(R.string.lists_resume_ep, nextEp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            OutlinedButton(
                                onClick = { scope.launch { listState.animateScrollToItem(1) } },
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                 Text(stringResource(R.string.lists_ep_1), maxLines = 1)
                            }
                            if (released > 1) {
                                OutlinedButton(
                                    onClick = { scope.launch { listState.animateScrollToItem(released) } },
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                     Text(stringResource(R.string.lists_latest), maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = if (isOled) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    }
                }

                // Episode cards
                if (displayEpisodes.isNotEmpty()) {
                    items(displayEpisodes.size) { index ->
                        val ep = displayEpisodes[index]
                        val episodeNum = ep.episode
                        val isWatched = episodeNum <= currentProgress
                        val isCurrent = episodeNum == currentProgress + 1
                        val hasAired = episodeNum <= released
                        RichTmdbEpisodeCard(
                            episodeNumber = episodeNum,
                            title = ep.title,
                            description = ep.description,
                            image = ep.image,
                            isWatched = isWatched,
                            isCurrent = isCurrent,
                            hasAired = hasAired,
                            isOled = isOled,
                            hideDescription = hideEpisodeDescriptions,
                            isSelected = selectedEpisode == episodeNum,
                            playbackPositions = playbackPositions,
                            playbackDurations = playbackDurations,
                            animeId = anime.id,
                            onSelect = { selectedEpisode = episodeNum },
                            onPlay = {
                                if (hasAired) {
                                    val title = if (ep.title.isNotEmpty() && !ep.title.startsWith("Episode", ignoreCase = true)) ep.title else "Episode $episodeNum"
                                    if (isMagnetActive && selectedMagnetAuthority != null) {
                                        viewModel.setStreamMethod("magnet")
                                        viewModel.setDefaultMagnetExtension(selectedMagnetAuthority!!)
                                    }
                                    if (isStreamActive && selectedStreamAuthority != null) {
                                        viewModel.setDefaultStreamExtension(selectedStreamAuthority!!)
                                    }
                                    onEpisodeSelect(episodeNum, title)
                                 } else {
                                     context.toast(context.getString(R.string.lists_episode_not_aired_yet))
                                 }
                             }
                         )
                     }
                 } else if (!isLoadingEpisodes) {
                     items(availableEpisodeNumbers.size) { index ->
                         val episodeNum = availableEpisodeNumbers[index]
                         val isWatched = episodeNum <= currentProgress
                         val isCurrent = episodeNum == currentProgress + 1
                         val hasAired = episodeNum <= released
                         SimpleRichEpisodeCard(
                             episodeNumber = episodeNum,
                             isWatched = isWatched,
                             isCurrent = isCurrent,
                             hasAired = hasAired,
                             isOled = isOled,
                             isSelected = selectedEpisode == episodeNum,
                             playbackPositions = playbackPositions,
                             playbackDurations = playbackDurations,
                             animeId = anime.id,
                             onSelect = { selectedEpisode = episodeNum },
                             onPlay = {
                                 if (hasAired) {
                                     if (isMagnetActive && selectedMagnetAuthority != null) {
                                         viewModel.setStreamMethod("magnet")
                                         viewModel.setDefaultMagnetExtension(selectedMagnetAuthority!!)
                                     }
                                     onEpisodeSelect(episodeNum, "Episode $episodeNum")
                                 } else {
                                     context.toast(context.getString(R.string.lists_episode_not_aired_yet))
                                 }
                             }
                         )
                     }
                 }

                // Loading and error states at the bottom
                if (isLoadingEpisodes) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (!isLoadingEpisodes && isLoadingMagnetEpisodes) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(12.dp))
                                 Text(stringResource(R.string.lists_fetching_tensei_sources), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (magnetError != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\u26A0", style = MaterialTheme.typography.headlineLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(magnetError ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (!isLoadingEpisodes && isLoadingExtensionEpisodes && selectedExtensionPkg != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                 Text(stringResource(R.string.lists_fetching_sources), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (extensionError != null && selectedExtensionPkg != null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("\u26A0", style = MaterialTheme.typography.headlineLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(extensionError ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}
