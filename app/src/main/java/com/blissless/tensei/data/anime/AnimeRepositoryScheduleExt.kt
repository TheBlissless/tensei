package com.blissless.tensei.data.anime

import android.util.Log
import com.blissless.tensei.BuildConfig
import com.blissless.tensei.data.models.AiringScheduleAnime
import com.blissless.tensei.data.models.AiringScheduleEntry
import com.blissless.tensei.data.models.AiringScheduleResponse
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.data.models.AnimeScheduleTimetableEntry
import com.blissless.tensei.data.models.AllCharactersResponse
import com.blissless.tensei.data.models.AllStaffResponse
import com.blissless.tensei.data.models.AnimeRelationsMedia
import com.blissless.tensei.data.models.AnimeRelationsResponse
import com.blissless.tensei.data.models.BatchedExploreResponse
import com.blissless.tensei.data.models.CharacterData
import com.blissless.tensei.data.models.CharacterResponse
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.DetailedAnimeMedia
import com.blissless.tensei.data.models.DetailedAnimeResponse
import com.blissless.tensei.data.models.DetailedAnimeStaffEdge
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.data.models.ExploreMedia
import com.blissless.tensei.data.models.ExploreResponse
import com.blissless.tensei.data.models.FuzzyDate
import com.blissless.tensei.data.models.MalAnimeNode
import com.blissless.tensei.data.models.MalSearchResponse
import com.blissless.tensei.data.models.MediaCoverImage
import com.blissless.tensei.data.models.MediaListResponse
import com.blissless.tensei.data.models.MediaTag
import com.blissless.tensei.data.models.MediaTagCollectionResponse
import com.blissless.tensei.data.models.MediaTitle
import com.blissless.tensei.data.models.SimpleActivityResponse
import com.blissless.tensei.data.models.StaffData
import com.blissless.tensei.data.models.StaffResponse
import com.blissless.tensei.data.models.StudioData
import com.blissless.tensei.data.models.UserActivity
import com.blissless.tensei.data.models.UserFavoritesResponse
import com.blissless.tensei.data.models.UserStatsResponse
import com.blissless.tensei.data.models.ViewerResponse
import com.blissless.tensei.network.Endpoints
import com.blissless.tensei.network.GraphQLClient
import com.blissless.tensei.network.GraphQLConfig
import kotlinx.serialization.json.Json
import com.blissless.tensei.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import com.blissless.tensei.data.cache.CacheManager
import com.blissless.tensei.data.prefs.UserPreferences

// Airing schedule fetching: AniList primary + AnimeSchedule.org fallback
// (with mapping of its timetable format to AiringScheduleAnime).

suspend fun AnimeRepository.fetchAiringSchedule(): List<AiringScheduleEntry> = withContext(Dispatchers.IO) {
    val currentTime = System.currentTimeMillis() / 1000
    val startTime = currentTime - (24 * 60 * 60)
    val endTime = currentTime + (8 * 24 * 60 * 60)
    val query = $$"""
        query ($page: Int, $startTime: Int, $endTime: Int) {
            Page(page: $page, perPage: 50) {
                airingSchedules(airingAt_greater: $startTime, airingAt_lesser: $endTime, sort: TIME) {
                    id
                    airingAt
                    episode
                    timeUntilAiring
                    mediaId
                    media {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        episodes
                        status
                        averageScore
                        genres
                        seasonYear
                        isAdult
                    }
                }
            }
        }
    """.trimIndent()
    // Fetch all pages concurrently instead of sequentially. The shared GraphQL client
    // queue already enforces concurrency + rate limits, so parallelizing here only
    // reduces wall-clock time (3-5 round trips → ~1). Parsing also moves off the main thread.
    val pages = coroutineScope {
        (1..MAX_AIRING_PAGES).map { page ->
            async {
                try {
                    val result = publicGraphqlRequestWithError(
                        query,
                        mapOf("page" to page, "startTime" to startTime, "endTime" to endTime)
                    )
                    if (result.data == null) {
                        null
                    } else {
                        json.decodeFromString<AiringScheduleResponse>(result.data).data.Page.airingSchedules
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }.awaitAll()
    }
    // Every page failed to reach the AniList API (not just an empty window) — signal
    // the caller so it can fall back to the AnimeSchedule weekly timetable.
    if (pages.all { it == null }) {
        Log.e("AiringDebug", "fetchAiringSchedule: ALL pages failed — throwing AiringScheduleApiDownException")
        throw AiringScheduleApiDownException()
    }
    // Merge pages in order, stopping at the first page that returned < 50 entries
    // (empty out-of-range pages are simply not consumed; failed pages are skipped).
    buildList {
        for (pageSchedules in pages) {
            if (pageSchedules != null) {
                addAll(pageSchedules)
                if (pageSchedules.size < 50) break
            }
        }
    }
}
/**
 * Fallback airing schedule sourced from AnimeSchedule.net's weekly timetable
 * (https://animeschedule.net/api/v3/timetables/raw) when the AniList API is unavailable.
 * A single request returns the whole current ISO week, with real air times, episode
 * numbers, and cover images — no pagination or day synthesis required.
 */

suspend fun AnimeRepository.fetchAiringScheduleAnimeScheduleFallback(): List<AiringScheduleAnime> = withContext(Dispatchers.IO) {
    try {
        val now = System.currentTimeMillis() / 1000
        val results = requestAnimeScheduleWeek()
        if (results.isNullOrEmpty()) {
            Log.e("AiringDebug", "AnimeSchedule fallback: weekly timetable empty — schedule unreachable")
            throw AnimeScheduleUnavailableException()
        }
        val mapped = mapAnimeScheduleToAiring(results, now)
        mapped
    } catch (e: AnimeScheduleUnavailableException) {
        throw e
    } catch (e: Exception) {
        Log.e("AiringDebug", "AnimeSchedule fallback failed unexpectedly: ${e.message}", e)
        throw AnimeScheduleUnavailableException()
    }
}

suspend fun AnimeRepository.requestAnimeScheduleWeek(): List<AnimeScheduleTimetableEntry>? = try {
    val today = LocalDate.now()
    val mondayOfCurrentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val tz = ZoneId.systemDefault().id
    val combined = mutableListOf<AnimeScheduleTimetableEntry>()
    // Fetch the current week AND the following week. The 7-day schedule grid starts
    // today and extends into next week (e.g. Mon/Tue/Wed columns when today is
    // Thursday), so the following week's timetable fills those trailing columns.
    // mapAnimeScheduleToAiring dedupes by route, keeping the earlier airing.
    for (offset in 0L..1L) {
        val mondayOfWeek = mondayOfCurrentWeek.plusWeeks(offset)
        val year = mondayOfWeek.get(IsoFields.WEEK_BASED_YEAR)
        val week = mondayOfWeek.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val url = Endpoints.AnimeSchedule.timetableUrl(year, week, "raw", tz)
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer ${BuildConfig.ANIME_SCHEDULE_API_KEY}")
            .header("Accept", "application/json")
            .header("User-Agent", "Tensei/1.0")
            .build()
        val response = withContext(Dispatchers.IO) { animeScheduleClient.newCall(request).execute() }
        response.use {
            val code = it.code
            if (code == 200) {
                val body = it.body.string()
                combined.addAll(json.decodeFromString<List<AnimeScheduleTimetableEntry>>(body))
            } else {
                val errBody = it.body.string()
                Log.w("AiringDebug", "AnimeSchedule HTTP $code body=${errBody.take(300)}")
            }
        }
    }
    combined.ifEmpty { null }
} catch (e: Exception) {
    Log.e("AiringDebug", "AnimeSchedule request failed: ${e.message}", e)
    null
}

fun AnimeRepository.mapAnimeScheduleToAiring(entries: List<AnimeScheduleTimetableEntry>, now: Long): List<AiringScheduleAnime> {
    val result = mutableListOf<AiringScheduleAnime>()
    var droppedNoDate = 0
    var droppedBadDate = 0
    var droppedSeen = 0
    var airedShown = 0
    var airedSkipped = 0
    val seenRoutes = mutableSetOf<String>()
    fun parseDate(entry: AnimeScheduleTimetableEntry): Long? {
        val dateStr = entry.episodeDate
        if (dateStr.isNullOrBlank()) {
            droppedNoDate++
            Log.w("AiringDebug", "AnimeSchedule '${entry.title ?: entry.route}' dropped: no episodeDate")
            return null
        }
        return parseAnimeScheduleDate(dateStr).also { if (it == null) droppedBadDate++ }
    }
    fun build(entry: AnimeScheduleTimetableEntry, airingAt: Long) = AiringScheduleAnime(
        id = entry.route.hashCode(),
        title = entry.english ?: entry.romaji ?: entry.title ?: "Unknown",
        titleEnglish = entry.english ?: entry.romaji,
        cover = entry.imageVersionRoute?.let {
            "${Endpoints.AnimeSchedule.IMAGE_BASE_URL}/${it.trimStart('/')}"
        } ?: "",
        episodes = entry.episodes ?: 0,
        airingEpisode = entry.episodeNumber ?: 0,
        airingAt = airingAt,
        timeUntilAiring = airingAt - now,
        year = entry.episodeDate?.take(4)?.toIntOrNull(),
        isAdult = false
    )
    // Entries that haven't aired yet are matched first so a series with a later episode
    // in the fetched weeks keeps its countdown over an already-aired one.
    val upcoming = entries.filter { it.airingStatus != "aired" }
    for (entry in upcoming) {
        if (!seenRoutes.add(entry.route)) {
            droppedSeen++
            continue
        }
        val airingAt = parseDate(entry) ?: continue
        result.add(build(entry, airingAt))
    }
    // Already-aired episodes (e.g. a show that ran earlier today) are kept too so they
    // still appear in the timeline; the card shows how long ago they aired.
    for (entry in entries) {
        if (entry.airingStatus != "aired") continue
        if (!seenRoutes.add(entry.route)) {
            airedSkipped++
            continue
        }
        val airingAt = parseDate(entry) ?: continue
        result.add(build(entry, airingAt))
        airedShown++
    }
    return result.sortedWith(compareBy<AiringScheduleAnime> { it.airingAt }.thenBy { it.title.lowercase() })
}

fun AnimeRepository.parseAnimeScheduleDate(iso: String): Long? {
    return try {
        OffsetDateTime.parse(iso).toInstant().epochSecond
    } catch (e: Exception) {
        Log.w("AiringDebug", "AnimeSchedule bad episodeDate: '$iso' (${e::class.simpleName}: ${e.message})")
        null
    }
}
// ============================================
// Search
// ============================================
