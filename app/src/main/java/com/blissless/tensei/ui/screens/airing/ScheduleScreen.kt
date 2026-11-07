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
import com.blissless.tensei.viewmodel.fetchAiringSchedule
import com.blissless.tensei.viewmodel.fetchAiringScheduleOnReopen
import com.blissless.tensei.viewmodel.tryManualRefresh
import com.blissless.tensei.viewmodel.addExploreAnimeToList
import com.blissless.tensei.viewmodel.removeAnimeFromList
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData

private fun getDaysFromCurrentDay(currentDay: Int): List<Int> {
    return (0..6).map { offset -> (currentDay + offset) % 7 }
}


internal sealed class TimelineItem {
    data class Anime(val data: AiringScheduleAnime) : TimelineItem()
    data class DayHeader(val dayIndex: Int, val dayName: String) : TimelineItem()
    data class HourHeader(val hour: Int, val timeString: String) : TimelineItem()
}


internal fun easeOutCubic(t: Float): Float {
    val t1 = t - 1; return t1 * t1 * t1 + 1
}


private fun firstUpcomingHourIndex(list: List<TimelineItem>, dayIndex: Int, currentHour: Int): Int {
    var dayHeaderIndex = -1
    var usableDayHeader = 0
    for (i in list.indices) {
        when (val it = list[i]) {
            is TimelineItem.DayHeader -> { if (it.dayIndex == dayIndex) { dayHeaderIndex = i; usableDayHeader = i } }
            is TimelineItem.HourHeader -> if (dayHeaderIndex != -1 && it.hour >= currentHour) return i
            is TimelineItem.Anime -> {}
        }
    }
    return usableDayHeader
}


@SuppressLint("FrequentlyChangingValue")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    isOled: Boolean = false,
    isVisible: Boolean = false,
    preferEnglishTitles: Boolean = true,
    isLoggedIn: Boolean = false,
    onPlayEpisode: (AnimeMedia, Int, String?) -> Unit = { _, _, _ -> },
    onAnimeDialogOpen: (Boolean) -> Unit = {},
    onCharacterClick: (Int) -> Unit = {},
    onStaffClick: (Int) -> Unit = {},
    onViewAllCast: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllStaff: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRelations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onViewAllRecommendations: (Int, String, String?) -> Unit = { _, _, _ -> },
    onNoExtension: () -> Unit = {},
    settingsReturnVersion: Int = 0,
    onAnimeDetailMangaClick: (MangaMedia) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val airingList by viewModel.airingAnimeList.collectAsState()
    val scheduleByDay by viewModel.airingSchedule.collectAsState()
    val isLoading by viewModel.isLoadingSchedule.collectAsState()
    val localAnimeStatus by viewModel.localAnimeStatus.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val appIcon by viewModel.appIcon.collectAsState()

    LaunchedEffect(airingList, scheduleByDay, isLoading) {
        val scheduleHasData = scheduleByDay.values.any { it.isNotEmpty() }
        if ((airingList.isEmpty() || !scheduleHasData) && !isLoading) viewModel.fetchAiringSchedule()
    }

    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    var currentDayOfWeek by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_WEEK) - 1) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var lastKnownDay by remember { mutableIntStateOf(currentDayOfWeek) }
    val orderedDays = remember(currentDayOfWeek) { getDaysFromCurrentDay(currentDayOfWeek) }
    var viewMode by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var visibleDayByScroll by remember { mutableIntStateOf(currentDayOfWeek) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var isInputLocked by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableIntStateOf(currentDayOfWeek) }
    var selectedAnime by remember { mutableStateOf<ExploreAnime?>(null) }
    var showAnimeDialog by remember { mutableStateOf(false) }
    var firstOpenedAnime by remember { mutableStateOf<ExploreAnime?>(null) }
    val context = LocalContext.current
    val locale = remember(context) { LocaleDate.localeOf(context) }
    val dayNames = remember(locale) { LocaleDate.dayNames(locale) }
    val dayAbbreviations = remember(locale) { LocaleDate.dayAbbreviations(locale) }
    val aniListFavorites by viewModel.aniListFavorites.collectAsState()
    val currentlyWatching by viewModel.currentlyWatching.collectAsState()
    val planningToWatch by viewModel.planningToWatch.collectAsState()
    val completed by viewModel.completed.collectAsState()
    val onHold by viewModel.onHold.collectAsState()
    val dropped by viewModel.dropped.collectAsState()

    val animeStatusMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) {
        val map = mutableMapOf<Int, String>()
        currentlyWatching.forEach { map[it.id] = "CURRENT" }
        planningToWatch.forEach { map[it.id] = "PLANNING" }
        completed.forEach { map[it.id] = "COMPLETED" }
        onHold.forEach { map[it.id] = "PAUSED" }
        dropped.forEach { map[it.id] = "DROPPED" }
        localAnimeStatus.forEach { (id, entry) -> if (!map.containsKey(id)) map[id] = entry.status }
        map
    }

    val animeProgressMap = remember(currentlyWatching, planningToWatch, completed, onHold, dropped, localAnimeStatus) {
        val map = mutableMapOf<Int, Int>()
        currentlyWatching.forEach { if (it.progress > 0) map[it.id] = it.progress }
        planningToWatch.forEach { if (it.progress > 0) map[it.id] = it.progress }
        completed.forEach { if (it.progress > 0) map[it.id] = it.progress }
        onHold.forEach { if (it.progress > 0) map[it.id] = it.progress }
        dropped.forEach { if (it.progress > 0) map[it.id] = it.progress }
        localAnimeStatus.forEach { (id, entry) -> if (entry.progress > 0 && !map.containsKey(id)) map[id] = entry.progress }
        map
    }

    val isFavoriteRateLimited by viewModel.isFavoriteRateLimited.collectAsState()
    var listVersion by remember { mutableIntStateOf(0) }
    val favoriteIds = remember(listVersion, aniListFavorites) { aniListFavorites.map { it.id }.toSet() }

    LaunchedEffect(currentlyWatching, planningToWatch, completed, onHold, dropped, aniListFavorites) { listVersion++ }
    LaunchedEffect(isFavoriteRateLimited) {
        if (isFavoriteRateLimited) context.toast(context.getString(R.string.schedule_wait_toggling))
    }

    LaunchedEffect(isLoading) { if (!isLoading && isRefreshing) isRefreshing = false }
    LaunchedEffect(Unit) {
        delay(800.milliseconds)
        viewModel.fetchAiringScheduleOnReopen()
    }
    LaunchedEffect(isLoading) { if (!isLoading && isRefreshing) isRefreshing = false }
    LaunchedEffect(Unit) {
        while (true) { delay(300000.milliseconds); currentTime = System.currentTimeMillis() / 1000; viewModel.fetchAiringSchedule(force = true) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000.milliseconds)
            val newTime = System.currentTimeMillis() / 1000
            currentTime = newTime
            val newCalendar = Calendar.getInstance()
            val newDay = newCalendar.get(Calendar.DAY_OF_WEEK) - 1
            if (newDay != lastKnownDay) { lastKnownDay = newDay; currentDayOfWeek = newDay; selectedDay = newDay; visibleDayByScroll = newDay }
        }
    }

    val startOfToday = remember(currentDayOfWeek) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis / 1000
    }
    val endOfToday = remember(startOfToday) { startOfToday + 86400L }
    val sevenDaysFromNow = remember(startOfToday) { endOfToday + 6 * 86400L }

    val filteredScheduleByDay = remember(scheduleByDay, startOfToday, endOfToday, sevenDaysFromNow, currentDayOfWeek) {
        val result = mutableMapOf<Int, MutableList<AiringScheduleAnime>>()
        for (i in 0..6) result[i] = mutableListOf()
        scheduleByDay.values.flatten().forEach { anime ->
            val ac = Calendar.getInstance(); ac.timeInMillis = anime.airingAt * 1000L; val animeDow = ac.get(Calendar.DAY_OF_WEEK) - 1
            if (animeDow == currentDayOfWeek) { if (anime.airingAt in startOfToday..endOfToday) result[animeDow]?.add(anime) }
            else { if (anime.airingAt in endOfToday..sevenDaysFromNow) result[animeDow]?.add(anime) }
        }
        result.forEach { (_, list) -> list.sortWith(compareBy<AiringScheduleAnime> { it.airingAt }.thenBy { it.title.lowercase() }) }; result
    }

    val allUpcomingTimelineItems = remember(filteredScheduleByDay, orderedDays, currentDayOfWeek, dayNames) {
        val items = mutableListOf<TimelineItem>()
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        orderedDays.forEach { dayIndex ->
            val dayAnime = (filteredScheduleByDay[dayIndex] ?: emptyList()).sortedWith(compareBy<AiringScheduleAnime> { it.airingAt }.thenBy { it.title.lowercase() })
            items.add(TimelineItem.DayHeader(dayIndex, dayNames[dayIndex]))
            if (dayAnime.isNotEmpty()) {
                val grouped = LinkedHashMap<Int, MutableList<TimelineItem.Anime>>()
                dayAnime.forEach { a -> grouped.getOrPut((a.airingAt / 3600).toInt()) { mutableListOf() }.add(TimelineItem.Anime(a)) }
                grouped.forEach { (hour, list) ->
                    items.add(TimelineItem.HourHeader(hour, tf.format(Date(hour * 3600L * 1000L))))
                    items.addAll(list)
                }
            }
        }
        items
    }

    val byDayTimelineItems = remember(filteredScheduleByDay, selectedDay, currentDayOfWeek) {
        val items = mutableListOf<TimelineItem>()
        val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayAnime = (filteredScheduleByDay[selectedDay] ?: emptyList()).sortedBy { it.airingAt }
        if (dayAnime.isNotEmpty()) {
            val grouped = LinkedHashMap<Int, MutableList<TimelineItem.Anime>>()
            dayAnime.forEach { a -> grouped.getOrPut((a.airingAt / 3600).toInt()) { mutableListOf() }.add(TimelineItem.Anime(a)) }
            grouped.forEach { (hour, list) ->
                items.add(TimelineItem.HourHeader(hour, tf.format(Date(hour * 3600L * 1000L))))
                items.addAll(list)
            }
        }
        items
    }

    val initialScrollIndexAll = remember(allUpcomingTimelineItems, currentTime) {
        val currentHour = (currentTime / 3600).toInt()
        val idx = firstUpcomingHourIndex(allUpcomingTimelineItems, Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1, currentHour)
        if (idx >= 0) idx else 0
    }
    val initialScrollIndexByDay = remember(byDayTimelineItems, currentTime) {
        val currentHour = (currentTime / 3600).toInt()
        val idx = firstUpcomingHourIndex(byDayTimelineItems, -1, currentHour)
        if (idx >= 0) idx else 0
    }
    val listStateAllUpcoming = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndexAll)
    val listStateByDay = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndexByDay)

    val dayToItemIndexMapAll = remember(allUpcomingTimelineItems) {
        val map = mutableMapOf<Int, Int>()
        allUpcomingTimelineItems.forEachIndexed { i, item -> if (item is TimelineItem.DayHeader) map[item.dayIndex] = i }
        map
    }

    LaunchedEffect(listStateAllUpcoming.firstVisibleItemIndex, viewMode, currentDayOfWeek) {
        if (viewMode == 0 && !isProgrammaticScroll) {
            val fvi = listStateAllUpcoming.firstVisibleItemIndex
            if (fvi < allUpcomingTimelineItems.size) {
                var day = currentDayOfWeek
                for (i in fvi downTo 0) { val item = allUpcomingTimelineItems.getOrNull(i); if (item is TimelineItem.DayHeader) { day = item.dayIndex; break } }
                visibleDayByScroll = day
            }
        }
    }

    LaunchedEffect(isProgrammaticScroll) {
        if (isProgrammaticScroll) { delay(550.milliseconds); isProgrammaticScroll = false; isInputLocked = false }
    }

    var hasScrolledToCurrentTime by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            currentDayOfWeek = today; visibleDayByScroll = today; selectedDay = today; lastKnownDay = today
            val currentHour = (System.currentTimeMillis() / 1000L / 3600).toInt()
            val targetAll = firstUpcomingHourIndex(allUpcomingTimelineItems, today, currentHour)
            val targetByDay = firstUpcomingHourIndex(byDayTimelineItems, -1, currentHour)
            if (targetAll >= 0) { listStateAllUpcoming.scrollToItem(targetAll, scrollOffset = -100); hasScrolledToCurrentTime = true }
            if (targetByDay >= 0) { listStateByDay.scrollToItem(targetByDay, scrollOffset = -100); hasScrolledToCurrentTime = true }
        }
    }

    LaunchedEffect(allUpcomingTimelineItems, byDayTimelineItems, isVisible) {
        if (isVisible && !hasScrolledToCurrentTime && allUpcomingTimelineItems.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            val currentHour = (System.currentTimeMillis() / 1000L / 3600).toInt()
            val targetAll = firstUpcomingHourIndex(allUpcomingTimelineItems, today, currentHour)
            val targetByDay = firstUpcomingHourIndex(byDayTimelineItems, -1, currentHour)
            if (targetAll >= 0) { listStateAllUpcoming.scrollToItem(targetAll, scrollOffset = -100); hasScrolledToCurrentTime = true }
            if (targetByDay >= 0) { listStateByDay.scrollToItem(targetByDay, scrollOffset = -100); hasScrolledToCurrentTime = true }
        }
    }

    val todayPastCount = filteredScheduleByDay[currentDayOfWeek]?.count { it.airingAt <= currentTime } ?: 0
    val totalUpcomingThisWeek = remember(filteredScheduleByDay, currentTime) { filteredScheduleByDay.values.sumOf { dl -> dl.count { it.airingAt > currentTime } } }
    val selectedDayPastCount = filteredScheduleByDay[selectedDay]?.count { it.airingAt <= currentTime } ?: 0
    val selectedDayFutureCount = filteredScheduleByDay[selectedDay]?.count { it.airingAt > currentTime } ?: 0

    val onBg = MaterialTheme.colorScheme.onBackground

    val requestRefresh: () -> Unit = {
        if (viewModel.tryManualRefresh("schedule")) {
            isRefreshing = true
            currentTime = System.currentTimeMillis() / 1000
            viewModel.fetchAiringSchedule(force = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (apiError != null || isOffline) {
            TenseiErrorBanner(
                message = if (isOffline) stringResource(R.string.home_offline) else stringResource(R.string.home_anilist_unavailable),
                icon = if (isOffline) Icons.Default.SignalWifiOff else Icons.Default.CloudOff,
                onRetry = requestRefresh,
                isOffline = isOffline,
                isOled = isOled,
            )
        }

        val scheduleSubtitle = if (viewMode == 0) {
            stringResource(R.string.schedule_subtitle_aired_upcoming, todayPastCount, totalUpcomingThisWeek)
        } else if (selectedDayPastCount > 0) {
            stringResource(R.string.schedule_subtitle_aired_upcoming, selectedDayPastCount, selectedDayFutureCount)
        } else {
            stringResource(R.string.schedule_subtitle_upcoming, selectedDayFutureCount)
        }

        ExploreTopBarRow(
            leading = { AppIconCircle(appIcon) },
            center = {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.schedule_title),
                        style = TenseiType.screenTitle,
                        color = onBg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = scheduleSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailing = {
                SearchCircleAction(
                    onClick = onSearchClick,
                    contentDescription = stringResource(R.string.schedule_search_content_description)
                )
            }
        )

        Spacer(Modifier.height(Spacing.sm))

        val orderedDaysForSelector = if (viewMode == 0) orderedDays else orderedDays
        val currentDayForSelector = if (viewMode == 0) visibleDayByScroll else selectedDay

        // Day selector: horizontally swipeable day buttons (Day name, date, airing count)
        val daySelectorState = rememberLazyListState()
        LaunchedEffect(currentDayForSelector, orderedDaysForSelector) {
            val index = orderedDaysForSelector.indexOf(currentDayForSelector)
            if (index >= 0) {
                daySelectorState.animateScrollToItem(index)
            }
        }
        LazyRow(
            state = daySelectorState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(orderedDaysForSelector) { pos, dayIndex ->
                val isSelected = currentDayForSelector == dayIndex
                val isToday = currentDayOfWeek == dayIndex
                val dayCount = filteredScheduleByDay[dayIndex]?.size ?: 0
                val dateMillis = (startOfToday + pos * 86400L) * 1000L
                val dateText = "${LocaleDate.dayOrdinal(LocaleDate.formatDayNumber(dateMillis, locale), locale)} ${LocaleDate.formatMonthAbbrev(dateMillis, locale)}"
                Surface(
                    modifier = Modifier
                        .width(100.dp)
                        .height(56.dp)
                        .clickable {
                            if (viewMode == 0) {
                                visibleDayByScroll = dayIndex
                                val targetIndex = dayToItemIndexMapAll[dayIndex] ?: 0
                                scope.launch { listStateAllUpcoming.scrollToItem(targetIndex, scrollOffset = if (isToday) -100 else 0) }
                            } else {
                                selectedDay = dayIndex
                            }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    tonalElevation = if (isSelected) 0.dp else 2.dp,
                    border = when {
                        isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        isSelected -> null
                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            dayAbbreviations[dayIndex],
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            dateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (dayCount > 0) stringResource(R.string.schedule_day_count_airing, dayCount) else stringResource(R.string.schedule_day_count_none),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = requestRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading && airingList.isEmpty()) {
                ScheduleLoadingSkeleton()
            } else {
                val timelineItems = if (viewMode == 0) allUpcomingTimelineItems else byDayTimelineItems
                val currentListState = if (viewMode == 0) listStateAllUpcoming else listStateByDay

                if (timelineItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.schedule_empty_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (viewMode == 1) stringResource(R.string.schedule_empty_for_day) else stringResource(R.string.schedule_empty_found), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.schedule_swipe_refresh), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        }
                    }
                } else if (viewMode == 1) {
                    AnimatedContent(
                        targetState = selectedDay,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "dayCrossfade"
                    ) {
                        TimelineScheduleList(
                            timelineItems = byDayTimelineItems,
                            currentDayOfWeek = currentDayOfWeek,
                            currentTime = currentTime,
                            preferEnglishTitles = preferEnglishTitles,
                            animeStatusMap = animeStatusMap,
                            listState = listStateByDay,
                            isVisible = isVisible,
                            onAnimeClick = { anime ->
                                val exploreAnime = ExploreAnime(
                                    id = anime.id, title = anime.title, titleEnglish = anime.titleEnglish, cover = anime.cover,
                                    banner = null, episodes = anime.episodes, latestEpisode = anime.airingEpisode,
                                    averageScore = anime.averageScore, genres = anime.genres, year = anime.year, format = null, malId = anime.malId
                                )
                                firstOpenedAnime = exploreAnime; selectedAnime = exploreAnime; showAnimeDialog = true; onAnimeDialogOpen(true)
                            }
                        )
                    }
                } else {
                    TimelineScheduleList(
                        timelineItems = timelineItems,
                        currentDayOfWeek = currentDayOfWeek,
                        currentTime = currentTime,
                        preferEnglishTitles = preferEnglishTitles,
                        animeStatusMap = animeStatusMap,
                        listState = currentListState,
                        isVisible = isVisible,
                        onAnimeClick = { anime ->
                            val exploreAnime = ExploreAnime(
                                id = anime.id, title = anime.title, titleEnglish = anime.titleEnglish, cover = anime.cover,
                                banner = null, episodes = anime.episodes, latestEpisode = anime.airingEpisode,
                                averageScore = anime.averageScore, genres = anime.genres, year = anime.year, format = null, malId = anime.malId
                            )
                            firstOpenedAnime = exploreAnime; selectedAnime = exploreAnime; showAnimeDialog = true; onAnimeDialogOpen(true)
                        }
                    )
                }
            }
        }
    }

    if (showAnimeDialog && selectedAnime != null) {
        val currentStatus by remember(listVersion, selectedAnime!!.id) { derivedStateOf { animeStatusMap[selectedAnime!!.id] } }
        val currentProgress by remember(listVersion, selectedAnime!!.id) { derivedStateOf { animeProgressMap[selectedAnime!!.id] } }
        val isFavorite by remember(listVersion, favoriteIds, selectedAnime!!.id) { derivedStateOf { favoriteIds.contains(selectedAnime!!.id) } }
        DetailedAnimeScreen(
            anime = selectedAnime!!.toDetailedAnimeData(), viewModel = viewModel, isOled = isOled,
            settingsReturnVersion = settingsReturnVersion,
            currentStatus = currentStatus, currentProgress = currentProgress, isFavorite = isFavorite, isLoggedIn = isLoggedIn,
            onDismiss = { if (firstOpenedAnime != null && selectedAnime!!.id != firstOpenedAnime!!.id) selectedAnime = firstOpenedAnime else { showAnimeDialog = false; selectedAnime = null; firstOpenedAnime = null; onAnimeDialogOpen(false) } },
            onSwipeToClose = { showAnimeDialog = false; selectedAnime = null; firstOpenedAnime = null; onAnimeDialogOpen(false) },
            onPlayEpisode = { episode, _ ->
                val am = AnimeMedia(id = selectedAnime!!.id, title = selectedAnime!!.title, titleEnglish = selectedAnime!!.titleEnglish, cover = selectedAnime!!.cover, banner = selectedAnime!!.banner, progress = 0, totalEpisodes = selectedAnime!!.episodes, latestEpisode = selectedAnime!!.latestEpisode, status = "", averageScore = selectedAnime!!.averageScore, genres = selectedAnime!!.genres, listStatus = "", listEntryId = 0, year = selectedAnime!!.year, malId = selectedAnime!!.malId)
                onPlayEpisode(am, episode, null); showAnimeDialog = false; selectedAnime = null; firstOpenedAnime = null; onAnimeDialogOpen(false)
            },
            onUpdateStatus = { if (it != null) viewModel.addExploreAnimeToList(selectedAnime!!, it) },
            onRemove = { viewModel.removeAnimeFromList(selectedAnime!!.id) },
            onRelationClick = { relation ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (relation.format == null || relation.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = relation.id,
                            title = relation.title,
                            cover = relation.cover,
                            totalChapters = 0,
                            averageScore = relation.averageScore,
                            format = relation.format
                        )
                    )
                } else {
                    try {
                        scope.launch {
                            try {
                                delay(100.milliseconds)
                                val d = viewModel.fetchDetailedAnimeData(relation.id)
                                if (d != null) selectedAnime = ExploreAnime(id = relation.id, title = d.title, titleEnglish = d.titleEnglish, cover = d.cover, banner = d.banner, episodes = d.episodes, latestEpisode = d.latestEpisode, averageScore = d.averageScore, genres = d.genres, year = d.year, format = d.format)
                                else context.toast(context.getString(R.string.schedule_anime_not_found, relation.id))
                            } catch (e: Exception) { context.toast(context.getString(R.string.schedule_error, e.message)) }
                        }
                    } catch (e: Exception) { context.toast(context.getString(R.string.schedule_error, e.message)) }
                }
            },
            onRecommendationClick = { rec ->
                val mangaFormats = listOf("MANGA", "NOVEL", "ONE_SHOT", "DOUJIN", "MANHWA", "MANHUA")
                if (rec.format == null || rec.format in mangaFormats) {
                    onAnimeDetailMangaClick(
                        MangaMedia(
                            id = rec.id,
                            title = rec.title,
                            cover = rec.cover,
                            totalChapters = rec.episodes,
                            averageScore = rec.averageScore,
                            format = rec.format
                        )
                    )
                } else {
                    try {
                        scope.launch {
                            try {
                                delay(100.milliseconds)
                                val d = viewModel.fetchDetailedAnimeData(rec.id)
                                if (d != null) selectedAnime = ExploreAnime(id = rec.id, title = d.title, titleEnglish = d.titleEnglish, cover = d.cover, banner = d.banner, episodes = d.episodes, latestEpisode = d.latestEpisode, averageScore = d.averageScore, genres = d.genres, year = d.year, format = d.format)
                                else context.toast(context.getString(R.string.schedule_anime_not_found, rec.id))
                            } catch (e: Exception) { context.toast(context.getString(R.string.schedule_error, e.message)) }
                        }
                    } catch (e: Exception) { context.toast(context.getString(R.string.schedule_error, e.message)) }
                }
            },
            onCharacterClick = onCharacterClick, onStaffClick = onStaffClick,
            onViewAllCast = { onViewAllCast(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllStaff = { onViewAllStaff(selectedAnime!!.id, selectedAnime!!.title, selectedAnime!!.titleEnglish) },
            onViewAllRelations = { id, title, titleEnglish -> onViewAllRelations(id, title, titleEnglish) },
            onViewAllRecommendations = { id, title, titleEnglish -> onViewAllRecommendations(id, title, titleEnglish) },
            onNoExtension = {
                onNoExtension()
            }
        )
    }
}
