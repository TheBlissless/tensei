package com.blissless.tensei.ui.screens.airing

import android.annotation.SuppressLint
import android.widget.Toast
import com.blissless.tensei.data.models.MangaMedia
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AiringScheduleAnime
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.toDetailedAnimeData
import com.blissless.tensei.ui.components.appIconDrawable
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.screens.details.DetailedAnimeScreen
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.StatusLabels
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds
import com.blissless.tensei.util.LocaleDate
import com.blissless.tensei.util.toast
import com.blissless.tensei.util.longToast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import com.blissless.tensei.ui.components.AppIconCircle
import com.blissless.tensei.ui.components.ExploreTopBarRow
import com.blissless.tensei.ui.components.SearchCircleAction
import com.blissless.tensei.ui.components.TenseiErrorBanner
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Sizes
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.TenseiType
import com.blissless.tensei.ui.components.easeOutCubic

// Airing timeline list: day/hour headers, anime items and loading skeleton.



@Composable
internal fun TimelineScheduleList(
    timelineItems: List<TimelineItem>,
    currentDayOfWeek: Int,
    currentTime: Long,
    preferEnglishTitles: Boolean,
    animeStatusMap: Map<Int, String>,
    listState: LazyListState,
    isVisible: Boolean = true,
    onAnimeClick: (AiringScheduleAnime) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val density = LocalDensity.current
    val translationYOffset = with(density) { (-30).dp.toPx() }
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    val cinematicProgress = rememberCinematicAnimation("schedule", isVisible, true)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        state = listState
    ) {
        itemsIndexed(
            items = timelineItems,
            key = { index, item -> when (item) { is TimelineItem.Anime -> "anime_${item.data.id}_${item.data.airingEpisode}_${item.data.airingAt}_$index"; is TimelineItem.DayHeader -> "day_${item.dayIndex}"; is TimelineItem.HourHeader -> "hour_${item.hour}_$index" } }
        ) { index, item ->
            when (item) {
                is TimelineItem.DayHeader -> DayHeaderItem(item.dayName, item.dayIndex == currentDayOfWeek)
                is TimelineItem.HourHeader -> HourHeaderItem(item.timeString)
                is TimelineItem.Anime -> {
                    val staggerDelay = minOf(index, 20) * 30f
                    val staggerMs = staggerDelay / 1000f
                    val rawProgress = ((cinematicProgress - staggerMs) / (1f - staggerMs))
                    val easedProgress = easeOutCubic(rawProgress.coerceAtLeast(0f).coerceAtMost(1f))
                    val introScale = 0.3f + easedProgress * 0.7f
                    val introAlpha = easedProgress.coerceAtLeast(0f)
                    val introTranslationY = translationYOffset * (1f - easedProgress)
                    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val itemInfo = visibleItems.find { it.index == index }
                    val centerOffset = if (itemInfo != null) { val ic = itemInfo.offset + itemInfo.size / 2; val sc = (layoutInfo.viewportSize.height / 2).toFloat(); (ic - sc) / sc } else 0f
                    val animatedOffset by animateFloatAsState(targetValue = if (isScrolling) centerOffset.coerceIn(-2f, 2f) else 0f, animationSpec = if (isScrolling) spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "offset")
                    val scrollScale = 1f - (animatedOffset.absoluteValue * 0.2f).coerceAtMost(0.2f)
                    val scrollAlpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f)
                    val scrollParallax = animatedOffset * 25f
                    val finalScale = scrollScale * introScale
                    val finalAlpha = (scrollAlpha * introAlpha).coerceIn(0f, 1f)
                    val finalTranslationY = scrollParallax + introTranslationY
                    TimelineAnimeItem(timeFormat.format(Date(item.data.airingAt * 1000L)), item.data, item.data.airingAt <= currentTime, currentTime,
                        preferEnglishTitles, animeStatusMap[item.data.id], finalScale, finalAlpha, finalTranslationY, onClick = { onAnimeClick(item.data) })
                }
            }
        }
        item { Spacer(Modifier.height(Sizes.listBottomNavClearance)) }
    }
}


@Composable
private fun DayHeaderItem(dayName: String, isToday: Boolean) {
    val stripeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = 21.dp.toPx()
                val sw = 3.dp.toPx()
                drawLine(stripeColor, Offset(x, size.height), Offset(x, 0f), sw)
            }
            .padding(start = 40.dp, end = 8.dp, top = 24.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayName.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isToday) {
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Text(stringResource(R.string.schedule_today), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}


@Composable
private fun HourHeaderItem(timeString: String) {
    val stripeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val stripeX = 21.dp
    val stripeWidth = 3.dp
    val dotSize = 14.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val isMonochrome = primaryColor.red == primaryColor.green && primaryColor.green == primaryColor.blue
    val dotColor = if (isMonochrome && primaryColor.red > 0.5f) Color.White
        else if (isMonochrome) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.secondary

    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = stripeX.toPx()
                val sw = stripeWidth.toPx()
                val gap = 6.dp.toPx()
                val halfDot = (dotSize / 2).toPx()
                val contentTop = 10.dp.toPx()
                val dotCenter = contentTop + rowHeightPx / 2f
                val dotTop = dotCenter - halfDot
                val dotBottom = dotCenter + halfDot
                drawLine(stripeColor, Offset(x, 0f), Offset(x, dotTop - gap), sw)
                drawLine(stripeColor, Offset(x, dotBottom + gap), Offset(x, size.height), sw)
            }
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowHeightPx = it.height.toFloat() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(stripeX - dotSize / 2))
            Box(Modifier.size(dotSize).background(dotColor, CircleShape))
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                timeString,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
private fun TimelineAnimeItem(
    timeString: String,
    anime: AiringScheduleAnime,
    isPast: Boolean,
    currentTime: Long,
    preferEnglishTitles: Boolean,
    animeStatus: String?,
    cardScale: Float,
    cardAlpha: Float,
    cardTranslationY: Float,
    onClick: () -> Unit
) {
    val contentAlpha = if (isPast) 0.55f else 1f
    val stripeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val stripeX = 21.dp
    val stripeWidth = 3.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = stripeX.toPx()
                val sw = stripeWidth.toPx()
                drawLine(stripeColor, Offset(x, size.height), Offset(x, 0f), sw)
            }
            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Surface(
            modifier = Modifier.padding(start = 38.dp, bottom = 4.dp).height(112.dp).graphicsLayer { scaleX = cardScale; scaleY = cardScale; alpha = cardAlpha; translationY = cardTranslationY },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            onClick = onClick
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, end = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = anime.cover, contentDescription = anime.title, contentScale = ContentScale.Crop,
                        modifier = Modifier.width(68.dp).height(92.dp).clip(RoundedCornerShape(10.dp)).alpha(contentAlpha)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val displayTitle = if (preferEnglishTitles && !anime.titleEnglish.isNullOrEmpty()) anime.titleEnglish else anime.title
                        Text(
                            displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (anime.airingEpisode > 0) {
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                    Text(stringResource(R.string.schedule_episode, anime.airingEpisode), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            if (animeStatus != null) {
                                val listStatusColor = statusColor(animeStatus)
                                val statusLabel = StatusLabels[animeStatus] ?: animeStatus
                                Surface(shape = RoundedCornerShape(6.dp), color = listStatusColor.copy(alpha = 0.12f)) {
                                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = listStatusColor, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(start = 90.dp, end = 18.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPast) {
                        val secSince = (currentTime - anime.airingAt).coerceAtLeast(0)
                        val elapsedText = remember(currentTime) {
                            val h = secSince / 3600; val m = (secSince % 3600) / 60
                            when { h > 24 -> "${h / 24}d ${h % 24}h"; h > 0 -> "${h}h ${m}m"; else -> "${m}m" }
                        }
                        Text(
                            stringResource(R.string.schedule_aired_ago, elapsedText),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    } else {
                        val secUntil = (anime.airingAt - currentTime).coerceAtLeast(0)
                        val timeUntilText = remember(currentTime) {
                            val h = secUntil / 3600; val m = (secUntil % 3600) / 60
                            when { h > 24 -> "${h / 24}d ${h % 24}h"; h > 0 -> "${h}h ${m}m"; else -> "${m}m" }
                        }
                        Text(
                            stringResource(R.string.schedule_airing_in, timeUntilText),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPast) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


@Composable
internal fun ScheduleLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .shimmer(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 24.dp, bottom = 6.dp)
                    .width(60.dp)
                    .height(20.dp)
            ) {
                SkeletonBlock(Modifier.fillMaxSize())
            }

            Box(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(16.dp)
            ) {
                SkeletonBlock(Modifier.fillMaxSize())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.width(34.dp), contentAlignment = Alignment.TopCenter) {
                    SkeletonBlock(
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                    )
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBlock(
                            Modifier
                                .width(68.dp)
                                .height(92.dp),
                            cornerRadius = Radius.poster
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            SkeletonBlock(
                                Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(12.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            SkeletonBlock(
                                Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(12.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            SkeletonBlock(
                                Modifier
                                    .width(50.dp)
                                    .height(18.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            SkeletonBlock(
                                Modifier
                                    .width(40.dp)
                                    .height(14.dp)
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(Sizes.listBottomNavClearance)) }
    }
}
