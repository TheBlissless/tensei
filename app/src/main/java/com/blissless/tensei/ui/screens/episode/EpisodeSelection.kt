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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.blissless.tensei.MainViewModel
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

    val sortedExtensions = remember(availableExtensions, selectedExtensionPkg) {
        val selected = selectedExtensionPkg
        availableExtensions.sortedWith(compareBy<Pair<String, String>> { if (it.second == selected) 0 else 1 }.thenBy { it.first })
    }

    val sortedMagnetExtensions = remember(availableMagnetExtensions, selectedMagnetAuthority) {
        val selected = selectedMagnetAuthority
        availableMagnetExtensions.sortedWith(compareBy<Pair<String, String>> { if (it.second == selected) 0 else 1 }.thenBy { it.first })
    }

    val sortedStreamExtensions = remember(availableStreamExtensions, selectedStreamAuthority) {
        val selected = selectedStreamAuthority
        availableStreamExtensions.sortedWith(compareBy<Pair<String, String>> { if (it.second == selected) 0 else 1 }.thenBy { it.first })
    }

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
                        magnetError = "Source not found via extension"
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

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val currentOffset = offsetY.value

                if (currentOffset > 0) {
                    if (available.y < 0) {
                        scope.launch {
                            offsetY.snapTo((currentOffset + available.y).coerceAtLeast(0f))
                        }
                        return available
                    }
                    if (available.y > 0) {
                        scope.launch {
                            offsetY.snapTo(currentOffset + available.y)
                        }
                        return available
                    }
                }

                if (isAtTop && currentOffset <= 10f && available.y > 0) {
                    scope.launch { offsetY.snapTo(available.y) }
                    return available
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val currentOffset = offsetY.value

                if (currentOffset == 0f) return Velocity.Zero

                val shouldDismiss = currentOffset > dismissThreshold || available.y > 300f

                if (shouldDismiss) {
                    dismissWithAnimation()
                } else {
                    scope.launch {
                        offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                }

                return available
            }
        }
    }

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
        Log.d("TmdbDebug", "Fetching TMDB: id=${anime.id} title='${anime.title}' eng='${anime.titleEnglish}' year=${anime.year} format=${anime.format}")
        val cached = viewModel.getCachedTmdbEpisodes(anime.id, anime.status)
        if (cached != null && hasRealTmdbData(cached)) {
            Log.d("TmdbDebug", "Using cached TMDB data for anime=${anime.id}: ${cached.size} episodes")
            tmdbEpisodes = cached
            isLoadingEpisodes = false
        } else {
            Log.d("TmdbDebug", "No cache hit for anime=${anime.id}, fetching...")
            var retries = 0
            while (retries < 2) {
                try {
                    val episodes = viewModel.fetchTmdbEpisodes(anime.title, anime.id, anime.year, anime.format)
                    Log.d("TmdbDebug", "fetchTmdbEpisodes returned ${episodes.size} episodes for anime=${anime.id} (attempt ${retries + 1})")
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
                    extensionError = "Source not found via extension"
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
                        modifier = Modifier.fillMaxSize()
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
                                    Text(text = "Progress: $currentProgress / ${if (total > 0) total else "??"}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                                if (released > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.Black.copy(alpha = 0.45f)
                                    ) {
                                        Text(text = if (released == 1) "$released episode" else "$released episodes", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }
                    }
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
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
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
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Extension selector - show only the active category
                        if (currentStreamMethod == "magnet" && sortedStreamExtensions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Tensei Stream:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                                sortedStreamExtensions.forEach { (extName, authority) ->
                                    FilterChip(
                                        selected = authority == selectedStreamAuthority && isStreamActive,
                                        onClick = {
                                            if (authority != selectedStreamAuthority || !isStreamActive) {
                                                selectedStreamAuthority = authority
                                                isStreamActive = true
                                                selectedExtensionPkg = null
                                                isMagnetActive = false
                                                selectedMagnetAuthority = null
                                                viewModel.setDefaultStreamExtension(authority)
                                                viewModel.setDefaultExtensionPackage("")
                                            }
                                        },
                                        label = { Text(extName, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        if (currentStreamMethod == "magnet" && availableMagnetExtensions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Torrent:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                sortedMagnetExtensions.forEach { (extName, authority) ->
                                    FilterChip(
                                        selected = authority == selectedMagnetAuthority && isMagnetActive,
                                        onClick = {
                                            if (authority != selectedMagnetAuthority || !isMagnetActive) {
                                                selectedMagnetAuthority = authority
                                                isMagnetActive = true
                                                isStreamActive = false
                                                selectedExtensionPkg = null
                                                selectedStreamAuthority = null
                                                isLoadingMagnetEpisodes = true
                                                magnetError = null
                                                isMagnetReady = false
                                                viewModel.clearMagnetEpisodes(anime.id)
                                                viewModel.fetchMagnetEpisodes(anime, authority)
                                            }
                                        },
                                        label = { Text(extName, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (currentStreamMethod == "direct" && availableExtensions.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("External:", style = MaterialTheme.typography.labelSmall, color = if (isOled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                sortedExtensions.forEach { (extName, extPkg) ->
                                    FilterChip(
                                        selected = extPkg == selectedExtensionPkg && !isMagnetActive && !isStreamActive,
                                        onClick = {
                                            if (extPkg != selectedExtensionPkg) {
                                                selectedExtensionPkg = extPkg
                                                isMagnetActive = false
                                                isStreamActive = false
                                                selectedMagnetAuthority = null
                                                selectedStreamAuthority = null
                                                extensionError = null
                                                isExtensionReady = false
                                            }
                                        },
                                        label = { Text(extName, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        // Navigation chips
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val nextEp = currentProgress + 1
                            if (nextEp <= released) {
                                FilterChip(
                                    selected = true,
                                    onClick = { onEpisodeSelect(nextEp, null) },
                                    label = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Resume Ep $nextEp") } },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            FilterChip(
                                selected = false,
onClick = { scope.launch { listState.animateScrollToItem(1) } },
                                        label = { Text("Ep 1") },
                                        shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            if (released > 1) {
                                FilterChip(
                                    selected = false,
onClick = { scope.launch { listState.animateScrollToItem(released) } },
                                        label = { Text("Latest: Ep $released") },
                                        shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = if (isOled) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
                                    context.toast("Episode not aired yet")
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
                                    context.toast("Episode not aired yet")
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
                                Text("Fetching Tensei sources...", style = MaterialTheme.typography.bodyMedium, color = if (isOled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text("Fetching sources...", style = MaterialTheme.typography.bodyMedium, color = if (isOled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Icon(Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
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
                    Icon(Icons.Default.Fullscreen, contentDescription = "Enter fullscreen", tint = Color.White)
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
    val remainingText = if (savedPos in 1..<epDuration) {
        val remaining = epDuration - savedPos
        val mins = (remaining / 60000).toInt()
        val secs = ((remaining % 60000) / 1000).toInt()
        "${mins}:${"%02d".format(secs)} left"
    } else null
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
                    context.toast("Episode has not aired yet")
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).alpha(contentAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(badgeBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isWatched -> Icon(Icons.Default.Check, contentDescription = "Watched", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            isCurrent -> Icon(Icons.Default.PlayArrow, contentDescription = "Current", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                                text = "Episode $episodeNumber",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOled) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                // Progress bar and remaining time
                if (hasProgress) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = if (isOled) Color(0xFF333333) else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTimeFromMs(savedPos),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOled) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (remainingText != null) {
                                Text(
                                    text = remainingText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RichTmdbEpisodeCard(
    episodeNumber: Int,
    title: String?,
    description: String?,
    image: String?,
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

    // Inline expandable description (keeps cards compact until the user taps "More")
    var descriptionExpanded by remember { mutableStateOf(false) }
    var descriptionTruncated by remember { mutableStateOf(false) }

    // Consistent dark style for badges and icons in rich menu
    val badgeBg = Color.Black.copy(alpha = 0.7f)
    val badgeText = Color.White

    // Playback progress
    val epPlaybackKey = "${animeId}_$episodeNumber"
    val savedPos = playbackPositions[epPlaybackKey] ?: 0L
    val epDuration = playbackDurations[epPlaybackKey] ?: 0L
    val progressRatio = if (savedPos > 0 && epDuration > 0) (savedPos.toFloat() / epDuration).coerceIn(0f, 1f) else 0f
    val remainingText = if (savedPos in 1..<epDuration) {
        val remaining = epDuration - savedPos
        val mins = (remaining / 60000).toInt()
        val secs = ((remaining % 60000) / 1000).toInt()
        "${mins}:${"%02d".format(secs)} left"
    } else null
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
                    context.toast("Episode has not aired yet")
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().alpha(contentAlpha).padding(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Netflix-mobile-style horizontal thumbnail (left)
                Box(
                    modifier = Modifier
                        .width(148.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    if (!image.isNullOrEmpty()) {
                        AsyncImage(model = image, contentDescription = "Episode $episodeNumber", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                    // Episode Badge - Standardized Dark Style
                    Surface(
                        modifier = Modifier.padding(5.dp).align(Alignment.TopStart),
                        shape = RoundedCornerShape(5.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = "EP $episodeNumber",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    // Watched Icon - Standardized Dark Style with primary color check
                    if (isWatched) {
                        Surface(
                            modifier = Modifier.padding(5.dp).align(Alignment.TopEnd),
                            shape = CircleShape,
                            color = badgeBg
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Watched",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(3.dp).size(13.dp)
                            )
                        }
                    }
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
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(22.dp))
                        }
                    }
                    // YouTube-style thin progress bar along the bottom edge of the thumbnail
                    if (hasProgress) {
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
                // Right side: title, status dot, progress, description
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                    Text(
                        text = title?.ifEmpty { "Episode $episodeNumber" } ?: "Episode $episodeNumber",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isOled) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    if (hasProgress) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = if (isOled) Color(0xFF333333) else MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTimeFromMs(savedPos),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOled) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (remainingText != null) {
                                Text(
                                    text = remainingText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (!description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = if (descriptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                descriptionTruncated = result.isLineEllipsized((result.lineCount - 1).coerceAtLeast(0))
                            },
                            color = if (isOled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (descriptionExpanded) {
                            Text(
                                text = "Less",
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
                                text = "More",
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

private fun formatTimeFromMs(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.ROOT, "%d:%02d", minutes, seconds)
}


