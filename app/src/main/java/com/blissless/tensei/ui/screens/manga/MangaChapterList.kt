package com.blissless.tensei.ui.screens.manga

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.Surface
import android.view.View
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.countDistinctChapters
import com.blissless.tensei.viewmodel.clearMangaChapterImagesCache
import com.blissless.tensei.viewmodel.cancelChapterImageLoad
import com.blissless.tensei.viewmodel.cancelMangaChapterLoad
import com.blissless.tensei.viewmodel.fetchMangaDetail
import com.blissless.tensei.viewmodel.flushMangaSync
import com.blissless.tensei.viewmodel.hasLoadedMangaChapters
import com.blissless.tensei.viewmodel.isLoadingMangaChapters
import com.blissless.tensei.viewmodel.loadChapterImages
import com.blissless.tensei.viewmodel.loadMangaChapters
import com.blissless.tensei.viewmodel.mangaChapterImages
import com.blissless.tensei.viewmodel.mangaChapterImagesError
import com.blissless.tensei.viewmodel.mangaImageHeaders
import com.blissless.tensei.viewmodel.mangaChapters
import com.blissless.tensei.viewmodel.mangaTrackManager
import com.blissless.tensei.viewmodel.onMangaScrollProgress
import com.blissless.tensei.viewmodel.prefetchMangaChapterImages
import com.blissless.tensei.viewmodel.refreshMangaTracking
import com.blissless.tensei.viewmodel.selectedExtensionAuthority
import com.blissless.tensei.viewmodel.mangaSourceMethod
import com.blissless.tensei.viewmodel.defaultExternalMangaExtension
import com.blissless.tensei.viewmodel.setMangaAutoAdvance
import com.blissless.tensei.viewmodel.setMangaFullscreen
import com.blissless.tensei.viewmodel.setMangaPageIndicator
import com.blissless.tensei.viewmodel.setMangaReaderMode
import com.blissless.tensei.viewmodel.setMangaLockRotation
import com.blissless.tensei.viewmodel.updateMangaChapterPages
import com.blissless.tensei.viewmodel.updateMangaScrollProgress
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.Headers

// The chapter list sheet: grouped chapters with header, search bar and rows.



// ─── Chapter list (Oni-style) ──────────────────────────────────────────

// Estimated heights (dp) used to center the current chapter inside its expanded group:
// the group header (title + subtitle + paddings) and one chapter row.
private val MangaChapterRowHeightDp = 56.dp


@Composable
fun MangaChapterListWithGroups(
    chapters: List<MangaChapter>,
    readIndices: Set<Int>,
    nextChapterToRead: Int,
    onChapterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingChapters: Boolean = true,
    hasLoadedChapters: Boolean = false,
    onContinueReading: (() -> Unit)? = null,
    onRetryLoadChapters: (() -> Unit)? = null,
    onCancelLoadChapters: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    if (chapters.isEmpty()) {
        var lastEmptySig by remember { mutableStateOf("") }
        val emptySig = "empty|$isLoadingChapters|$hasLoadedChapters"
        if (lastEmptySig != emptySig) {
            lastEmptySig = emptySig
        }
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingChapters || !hasLoadedChapters) {
                // Same transparent overlay design as the stream-fetching loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ReaderTextStrong)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.reader_loading_chapters), color = ReaderTextStrong)
                        if (onCancelLoadChapters != null) {
                            Spacer(modifier = Modifier.height(24.dp))
                            // Explicit light colors: the overlay is always dark, so the theme's
                            // default outlined-button colors would be unreadable in the light
                            // app theme.
                            OutlinedButton(
                                onClick = onCancelLoadChapters,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ReaderTextStrong),
                                border = BorderStroke(1.dp, ReaderTextStrong.copy(alpha = 0.7f))
                            ) {
                                Text(stringResource(R.string.common_cancel), color = ReaderTextStrong)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.reader_no_chapters_found), color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.reader_chapters_load_error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (onRetryLoadChapters != null) {
                            Button(onClick = onRetryLoadChapters) {
                                Text(stringResource(R.string.reader_retry))
                            }
                        }
                    }
                }
            }
        }
    } else {
        var searchQuery by remember { mutableStateOf("") }
        val groupedChapters = remember(chapters) {
            runCatching {
                groupChaptersByMainChapter(chapters)
            }.onFailure { e ->
                android.util.Log.e("MangaChapterList", "groupChaptersByMainChapter CRASHED: ${e.message}", e)
            }.getOrElse { emptyList() }
        }
        var lastListSig by remember { mutableStateOf("") }
        val listSig = "list|${chapters.size}|${groupedChapters.size}|${readIndices.size}|$nextChapterToRead"
        if (lastListSig != listSig) {
            lastListSig = listSig
        }

        val filteredGroups = remember(groupedChapters, searchQuery) {
            if (searchQuery.isBlank()) groupedChapters
            else groupedChapters.mapNotNull { (key, list) ->
                val filtered = list.filter { (_, chapter) ->
                    chapter.title.contains(searchQuery, ignoreCase = true)
                }
                if (filtered.isNotEmpty()) key to filtered else null
            }
        }

        val listState = rememberLazyListState()
        // Count per base number, not per whole number: 313, 314.1, 314.2, 315 is three
        // chapters, because 314.1 opens chapter 314 and 314.2 rides along with it (a 313.2
        // after 313 would not be a fourth). The old integer-only count reported 2.
        val totalCount = countDistinctChapters(chapters.map { it.chapterNumber })
        val readCount = countDistinctChapters(
            readIndices.mapNotNull { idx -> chapters.getOrNull(idx)?.chapterNumber }
        )
        val progress = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val listDensity = LocalDensity.current

        LaunchedEffect(chapters, nextChapterToRead) {
            // chapters.size is the "everything is read" marker, and -1 means the caller
            // does not want an autoscroll: either way there is no row to jump to, so
            // the list stays where it is (on top).
            if (nextChapterToRead !in chapters.indices) return@LaunchedEffect
            val targetGroupIndex = filteredGroups.indexOfFirst { (_, groupList) ->
                groupList.any { it.first == nextChapterToRead }
            }
            if (targetGroupIndex < 0) {
                android.util.Log.w("MangaChapterList", "AUTOSCROLL no group holds index $nextChapterToRead (chapters=${chapters.size})")
                return@LaunchedEffect
            }
            val groupKey = filteredGroups[targetGroupIndex].first
            val groupList = filteredGroups[targetGroupIndex].second
            val targetChapterTitle = chapters.getOrNull(nextChapterToRead)?.title ?: "?"
            // Let the list finish its first layout before jumping.
            delay(100)
            // LazyColumn item index: 0=header, 1=search, then one item per group.
            val targetItem = targetGroupIndex + 2
            val rowInGroup = groupList.indexOfFirst { it.first == nextChapterToRead }
            if (rowInGroup < 0) {
                android.util.Log.w("MangaChapterList", "AUTOSCROLL row not found in group $groupKey for index $nextChapterToRead")
                return@LaunchedEffect
            }
            // Jump straight to the target chapter's group (instant, single frame — no
            // scroll-down-then-scroll-back-up dance), then glide it to roughly the
            // vertical center. The target row sits inside the auto-expanded group, so
            // measure the group's REAL viewport offset and size, then derive the row's
            // offset from the measured group height (header + N rows). contentPadding is
            // already included in the measured offset, so the centering is accurate.
            listState.scrollToItem(targetItem)
            // Wait until the jump is reflected in the layout before measuring.
            var attempts = 0
            while (attempts < 10 && listState.layoutInfo.visibleItemsInfo.none { it.index == targetItem }) {
                attempts++
                delay(16)
            }
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportSize.height
            val groupItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetItem }
            if (viewportHeight <= 0 || groupItem == null) {
                android.util.Log.w("MangaChapterList", "AUTOSCROLL measure failed viewport=$viewportHeight item=${groupItem?.index}")
                return@LaunchedEffect
            }
            val rowHeightPx = with(listDensity) { MangaChapterRowHeightDp.toPx() }
            // Non-row portion of the group (header + padding) comes straight from the
            // MEASURED group height, so a wrong header constant can't throw it off.
            val nonRowHeightPx = (groupItem.size - groupList.size * rowHeightPx).coerceAtLeast(0f)
            val rowCenterInGroup = nonRowHeightPx + rowInGroup * rowHeightPx + rowHeightPx / 2f
            // scrollBy() is forward-positive: content moves UP the screen as delta grows, so
            // to bring the row center UP to the viewport center we scroll forward by the
            // row's current distance below center.
            val delta = (groupItem.offset + rowCenterInGroup) - viewportHeight / 2f
            if (delta != 0f) listState.animateScrollBy(delta)
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = statusBarPadding.calculateTopPadding() + 12.dp,
                bottom = statusBarPadding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                MangaChapterListHeader(
                    readCount = readCount,
                    totalCount = totalCount,
                    progress = progress,
                    nextChapterToRead = nextChapterToRead,
                    chapters = chapters,
                    onContinueReading = onContinueReading ?: {
                        if (nextChapterToRead in chapters.indices) onChapterClick(nextChapterToRead)
                    },
                    onBack = onBack
                )
            }

            item(key = "search") {
                MangaChapterSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            if (filteredGroups.isEmpty() && searchQuery.isNotBlank()) {
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.reader_no_chapters_match, searchQuery),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                filteredGroups.forEachIndexed { index, (groupKey, groupList) ->
                    val containsTarget = groupList.any { it.first == nextChapterToRead }
                    item(key = "chapter_group_$index") {
                        MangaChapterGroup(
                            groupKey = groupKey,
                            groupChapters = groupList,
                            readIndices = readIndices,
                            nextChapterToRead = nextChapterToRead,
                            initiallyExpanded = containsTarget,
                            onChapterClick = onChapterClick
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MangaChapterListHeader(
    readCount: Int,
    totalCount: Int,
    progress: Float,
    nextChapterToRead: Int,
    chapters: List<MangaChapter>,
    onContinueReading: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.reader_chapters),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.reader_read_count, readCount, totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress section — ring + bar + continue button in a card-like surface
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.width(14.dp))

                // Linear progress bar
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                if (nextChapterToRead < totalCount) {
                    Spacer(Modifier.width(14.dp))
                    FilledTonalButton(
                        onClick = onContinueReading,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        val nextChapterDisplay = chapters.getOrNull(nextChapterToRead)?.let { extractChapterNum(it.title) }
                            ?: "${nextChapterToRead + 1}"
                        Text(stringResource(R.string.reader_chapter_short, nextChapterDisplay), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


@Composable
private fun MangaChapterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (query.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        tween(200),
        label = "searchBorder"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(if (query.isNotEmpty()) 1.dp else 0.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(R.string.reader_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.reader_search_chapters),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.reader_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun MangaChapterGroup(
    groupKey: String,
    groupChapters: List<Pair<Int, MangaChapter>>,
    readIndices: Set<Int>,
    nextChapterToRead: Int,
    initiallyExpanded: Boolean = false,
    onChapterClick: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    LaunchedEffect(initiallyExpanded) {
        expanded = initiallyExpanded
    }

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "rotation"
    )

    // Same base-number rule as the header: a group weighs one chapter per main chapter, so
    // 314.1 and 314.2 count once together and a group that is only subchapters still fills
    // its ring instead of dividing by a whole-chapter count it does not have.
    val totalInGroup = countDistinctChapters(groupChapters.map { it.second.chapterNumber })
    val readInGroup = countDistinctChapters(
        groupChapters.filter { (index, _) -> index in readIndices }.map { it.second.chapterNumber }
    )
    val readRatio = if (totalInGroup > 0) readInGroup.toFloat() / totalInGroup else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Group header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact circular progress ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { readRatio },
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "${(readRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupKey,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (readInGroup > 0) stringResource(R.string.reader_group_chapters_read, totalInGroup, readInGroup)
                               else stringResource(R.string.reader_group_chapters, totalInGroup),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (readInGroup > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) stringResource(R.string.reader_collapse) else stringResource(R.string.reader_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    groupChapters.forEachIndexed { localIndex, (absoluteIndex, chapter) ->
                        MangaChapterRow(
                            chapter = chapter,
                            isRead = absoluteIndex in readIndices,
                            isNextToRead = absoluteIndex == nextChapterToRead,
                            isLast = localIndex == groupChapters.lastIndex,
                            onClick = { onChapterClick(absoluteIndex) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun MangaChapterRow(
    chapter: MangaChapter,
    isRead: Boolean,
    isNextToRead: Boolean,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    val chNum = extractChapterNum(chapter.title)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            isNextToRead && !isRead -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
            else -> Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Inline status indicator
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).then(
                    when {
                        isRead -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        isNextToRead -> Modifier.background(MaterialTheme.colorScheme.primary)
                        else -> Modifier.border(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    }
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Chapter number badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when {
                    isNextToRead -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    isRead -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(width = 38.dp, height = 24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = chNum,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = when {
                            isNextToRead -> MaterialTheme.colorScheme.primary
                            isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (chapter.title.length > 10 && !chapter.title.startsWith("Chapter")) {
                        chapter.title
                    } else {
                        // Chapter titles are built as "Chapter N: <name>". Show the name
                        // part (e.g. "Dragon Ball 194") next to the number badge, falling
                        // back to "Chapter N" when there's no actual name.
                        val namePart = Regex("^Chapter\\s*${Regex.escape(chNum)}\\s*:\\s*")
                            .replaceFirst(chapter.title, "").trim()
                        if (namePart.isNotBlank()) namePart else stringResource(R.string.reader_chapter_fallback, chNum)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isNextToRead -> MaterialTheme.colorScheme.onBackground
                        isRead -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    },
                    fontWeight = if (isNextToRead) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isNextToRead && !isRead) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.reader_next),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
