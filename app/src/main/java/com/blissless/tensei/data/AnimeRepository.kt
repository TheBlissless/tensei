package com.blissless.tensei.data

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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Handles all API calls and data fetching.
 * Optimized to use GraphQLClient for high-performance AniList requests.
 */

private val CLIENT_IDS = listOf(BuildConfig.CLIENT_ID_ANILIST)
private const val MAX_AIRING_PAGES = 5
private const val ANIME_SCHEDULE_TIMEOUT_MS = 15000

private val animeScheduleClient = OkHttpClient.Builder()
    .connectTimeout(ANIME_SCHEDULE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
    .readTimeout(ANIME_SCHEDULE_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
    .build()

internal val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

internal val tmdbBearerToken = BuildConfig.TMDB_API_KEY
internal val visitedOffsetIds = mutableSetOf<Int>()

// High-performance GraphQL client
private val graphQLClient = GraphQLClient(
    config = GraphQLConfig(
        maxConcurrentRequests = 5,
        minRequestIntervalMs = 100L,
        cacheDurationMs = 60 * 60 * 1000L, // 1 hour for public data
        userDataCacheDurationMs = 60 * 60 * 1000L // 1 hour for user data
    )
)

// Use longer cache for authenticated requests
private val authCacheDuration get() = graphQLClient.getConfig().userDataCacheDurationMs

class AnimeRepository(
    private val userPreferences: UserPreferences,
    private val cacheManager: CacheManager
) {

    // ============================================
    // GraphQL Requests (Optimized via GraphQLClient)
    // ============================================

    suspend fun graphqlRequest(query: String, variables: Map<String, Any?>): String? {
        val token = userPreferences.authToken.value ?: return null

        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = true,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = true,
            cacheDurationMs = authCacheDuration, // Use longer cache for user data
            parser = { it } // Return raw string for existing parsing logic
        )

        return result.data
    }

    suspend fun graphqlMutation(query: String, variables: Map<String, Any?>): String? {
        val token = userPreferences.authToken.value ?: return null

        Log.d("AniListScoreDebug", "graphqlMutation variables=$variables")

        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = true,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = false, // Mutations should never be cached
            parser = { it }
        )

        Log.d("AniListScoreDebug", "graphqlMutation result data=${result.data?.take(200)} error=${result.error?.message}")

        return result.data
    }

    suspend fun publicGraphqlRequest(query: String, variables: Map<String, Any?>): String? {
        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = false,
            clientIds = CLIENT_IDS,
            useCache = true,
            cacheDurationMs = 60 * 60 * 1000L, // 1 hour for public data
            parser = { it }
        )

        if (result.data == null) {
            Log.e(
                "GraphQLDebug",
                "Error code=${result.error?.code} message=${result.error?.message}" +
                    " isRateLimit=${result.error?.isRateLimit} retryCount=${result.retryCount}"
            )
        } else {
            Log.d("GraphQLDebug", "Success: ${result.data.take(200)}")
        }
        return result.data
    }

    // ============================================
    // User Operations
    // ============================================

    suspend fun fetchUser(): ViewerResponse? {
        val query = """
            query {
                Viewer {
                    id
                    name
                    about
                    avatar { medium large }
                    bannerImage
                    siteUrl
                    createdAt
                    statistics {
                        anime {
                            count
                            episodesWatched
                            minutesWatched
                            meanScore
                        }
                    }
                }
            }
        """.trimIndent()

        return graphqlRequest(query, emptyMap())?.let {
            try {
                json.decodeFromString<ViewerResponse>(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun fetchUserStats(userId: Int): UserStatsResponse? {
        val query = $$"""
            query ($userId: Int) {
                User(id: $userId) {
                    statistics {
                        anime {
                            count
                            episodesWatched
                            minutesWatched
                            meanScore
                        }
                    }
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("userId" to userId))?.let {
            try {
                json.decodeFromString<UserStatsResponse>(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    // ============================================
    // Anime Lists
    // ============================================

    suspend fun fetchMediaLists(userId: Int): MediaListResponse? {
        val query = $$"""
            query ($userId: Int) {
                MediaListCollection(userId: $userId, type: ANIME) {
                    lists {
                        name
                        status
                        entries {
                            id
                            mediaId
                            progress
                            status
                            score
                            media {
                                id
                                idMal
                                title { romaji english }
                                coverImage { extraLarge }
                                bannerImage
                                episodes
                                nextAiringEpisode { episode airingAt }
                                status
                                averageScore
                                genres
                                seasonYear
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("userId" to userId))?.let {
            try {
                json.decodeFromString<MediaListResponse>(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    // ============================================
    // Explore Data
    // ============================================

    data class ExploreResult(val response: BatchedExploreResponse?, val error: String?)

    suspend fun fetchBatchedExploreWithError(useCache: Boolean = true): ExploreResult {
        val query = """
            query {
                featured: Page(page: 1, perPage: 10) {
                    media(type: ANIME, status: RELEASING, sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                seasonal: Page(page: 1, perPage: 50) {
                    media(type: ANIME, sort: POPULARITY_DESC, status: RELEASING) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                topSeries: Page(page: 1, perPage: 50) {
                    media(type: ANIME, format: TV, sort: SCORE_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                topMovies: Page(page: 1, perPage: 50) {
                    media(type: ANIME, format: MOVIE, sort: SCORE_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                action: Page(page: 1, perPage: 50) {
                    media(type: ANIME, genre: "Action", sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                romance: Page(page: 1, perPage: 50) {
                    media(type: ANIME, genre: "Romance", sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                comedy: Page(page: 1, perPage: 50) {
                    media(type: ANIME, genre: "Comedy", sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                fantasy: Page(page: 1, perPage: 50) {
                    media(type: ANIME, genre: "Fantasy", sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
                scifi: Page(page: 1, perPage: 50) {
                    media(type: ANIME, genre: "Sci-Fi", sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        startDate { year }
                    }
                }
            }
        """.trimIndent()

        val rawResult = publicGraphqlRequestWithError(query, emptyMap(), useCache)
        return if (rawResult.data != null) {
            try {
                val response = json.decodeFromString<BatchedExploreResponse>(rawResult.data)
                ExploreResult(response, null)
            } catch (e: Exception) {
                ExploreResult(null, "JSON parse error: ${e.message}")
            }
        } else {
            ExploreResult(null, rawResult.error ?: "Unknown error")
        }
    }

    suspend fun publicGraphqlRequestWithError(query: String, variables: Map<String, Any?> = emptyMap(), useCache: Boolean = true): PublicGraphqlResult {
        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = false,
            clientIds = CLIENT_IDS,
            useCache = useCache,
            parser = { it }
        )

        return if (result.data != null) {
            PublicGraphqlResult(result.data, null)
        } else {
            PublicGraphqlResult(null, result.error?.message ?: "Unknown GraphQL error")
        }
    }

    data class PublicGraphqlResult(val data: String?, val error: String?)

    // ============================================
    // Airing Schedule
    // ============================================

    suspend fun fetchAiringSchedule(): List<AiringScheduleEntry> = withContext(Dispatchers.IO) {
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
        Log.d("AiringDebug", "fetchAiringSchedule: pages=${pages.size} nonNullPages=${pages.count { it != null }} sizes=${pages.map { it?.size }}")
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
    suspend fun fetchAiringScheduleAnimeScheduleFallback(): List<AiringScheduleAnime> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis() / 1000
            val results = requestAnimeScheduleWeek()
            if (results.isNullOrEmpty()) {
                Log.e("AiringDebug", "AnimeSchedule fallback: weekly timetable empty — schedule unreachable")
                throw AnimeScheduleUnavailableException()
            }
            Log.d("AiringDebug", "AnimeSchedule raw entries=${results.size}")
            val mapped = mapAnimeScheduleToAiring(results, now)
            Log.d("AiringDebug", "AnimeSchedule mapped=${mapped.size}")
            mapped
        } catch (e: AnimeScheduleUnavailableException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiringDebug", "AnimeSchedule fallback failed unexpectedly: ${e.message}", e)
            throw AnimeScheduleUnavailableException()
        }
    }

    private suspend fun requestAnimeScheduleWeek(): List<AnimeScheduleTimetableEntry>? = try {
        val today = LocalDate.now()
        val year = today.get(IsoFields.WEEK_BASED_YEAR)
        val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val tz = ZoneId.systemDefault().id
        val url = Endpoints.AnimeSchedule.timetableUrl(year, week, "raw", tz)
        Log.d("AiringDebug", "AnimeSchedule URL: $url")
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer ${BuildConfig.ANIME_SCHEDULE_API_KEY}")
            .header("Accept", "application/json")
            .header("User-Agent", "Tensei/1.0")
            .build()
        val response = withContext(Dispatchers.IO) { animeScheduleClient.newCall(request).execute() }
        response.use {
            val code = it.code
            if (code == 200) {
                val body = it.body?.string() ?: ""
                Log.d("AiringDebug", "AnimeSchedule HTTP $code bodyLen=${body.length} preview=${body.take(120)}")
                json.decodeFromString<List<AnimeScheduleTimetableEntry>>(body)
            } else {
                val errBody = it.body?.string().orEmpty()
                Log.w("AiringDebug", "AnimeSchedule HTTP $code body=${errBody.take(300)}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("AiringDebug", "AnimeSchedule request failed: ${e.message}", e)
        null
    }

    fun mapAnimeScheduleToAiring(entries: List<AnimeScheduleTimetableEntry>, now: Long): List<AiringScheduleAnime> {
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
        // Only took entries that haven't aired yet this week are matched first so a series
        // with a later episode this week keeps its countdown over an already-aired one.
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
        Log.d("AiringDebug", "AnimeSchedule map summary: total=${entries.size} kept=${result.size} dropped(seen=$droppedSeen, noDate=$droppedNoDate, badDate=$droppedBadDate) airedShown=$airedShown airedSkipped=$airedSkipped")
        return result.sortedBy { it.airingAt }
    }

    private fun parseAnimeScheduleDate(iso: String): Long? {
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

    suspend fun searchAnime(searchQuery: String): List<ExploreMedia> {
        if (searchQuery.isBlank()) return emptyList()

        val query = $$"""
            query ($search: String) {
                Page(page: 1, perPage: 20) {
                    media(search: $search, type: ANIME, sort: POPULARITY_DESC) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        isAdult
                        startDate { year }
                        format
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("search" to searchQuery))?.let {
            try {
                val data = json.decodeFromString<ExploreResponse>(it)
                data.data.Page.media
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun searchAnimeAdvanced(
        search: String? = null,
        genres: List<String>? = null,
        tags: List<String>? = null,
        format: String? = null,
        status: String? = null,
        season: String? = null,
        seasonYear: Int? = null,
        sort: String = "POPULARITY_DESC",
        isAdult: Boolean? = null,
        page: Int = 1,
        perPage: Int = 30
    ): List<ExploreMedia> {
        val varDeclarations = mutableListOf($$"$sort: [MediaSort]", $$"$page: Int",
            $$"$perPage: Int"
        )
        val varValues = mutableMapOf<String, Any?>(
            "sort" to listOf(sort),
            "page" to page,
            "perPage" to perPage
        )
        val mediaArgs = mutableListOf("type: ANIME", $$"sort: $sort")
        if (search != null) {
            varDeclarations.add(0, $$"$search: String")
            mediaArgs.add(0, $$"search: $search")
            varValues["search"] = search
        }

        fun addFilter(varName: String, varType: String, argName: String, value: Any?) {
            if (value != null) {
                varDeclarations.add($$"$$$varName: $$varType")
                mediaArgs.add($$"$$argName: $$$varName")
                varValues[varName] = value
            }
        }

        addFilter("genre_in", "[String]", "genre_in", genres)
        addFilter("tag_in", "[String]", "tag_in", tags)
        addFilter("season", "MediaSeason", "season", season)
        addFilter("seasonYear", "Int", "seasonYear", seasonYear)
        addFilter("format", "MediaFormat", "format", format)
        addFilter("status", "MediaStatus", "status", status)
        addFilter("isAdult", "Boolean", "isAdult", isAdult)

        val query = $$"""
            query ($${varDeclarations.joinToString(", ")}) {
                Page(page: $page, perPage: $perPage) {
                    media($${mediaArgs.joinToString("\n                        ")}) {
                        id
                        idMal
                        title { romaji english }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        isAdult
                        startDate { year }
                        format
                    }
                }
            }
        """.trimIndent()

        Log.d("SearchDebug", "Query: $query")
        Log.d("SearchDebug", "Variables: $varValues")
        val response = publicGraphqlRequest(query, varValues)
        Log.d("SearchDebug", "Response: ${response?.take(500)}")
        val parsed = response?.let {
            try {
                val data = json.decodeFromString<ExploreResponse>(it)
                Log.d("SearchDebug", "Parsed ${data.data.Page.media.size} results")
                data.data.Page.media
            } catch (e: Exception) {
                Log.e("SearchDebug", "Parse error: ${e.message}")
                Log.e("SearchDebug", "Response: ${it.take(1000)}")
                emptyList()
            }
        } ?: emptyList<ExploreMedia>().also { Log.e("SearchDebug", "Response was null") }

        // AniList unavailable (down/blocked): fall back to the official MyAnimeList
        // API so the search screen still returns results. MAL's /anime search only
        // honors the text query — genre/status/year/format params are ignored
        // server-side — so those filters are applied locally against each returned
        // node's metadata. Tags have no MAL equivalent; if tags are the ONLY active
        // filter there is nothing meaningful to query, so we degrade to empty.
        if (parsed.isEmpty()) {
            if (search != null || genres != null || format != null || status != null || seasonYear != null) {
                if (genres == null && format == null && status == null && seasonYear == null && !tags.isNullOrEmpty()) {
                    Log.w("SearchDebug", "AniList search failed — MAL cannot filter by tags alone")
                    return emptyList()
                }
                Log.w("SearchDebug", "AniList search failed/empty — using MAL filtered fallback for '$search'")
                return searchAnimeMalFiltered(search, genres, format, status, seasonYear, page, perPage)
            }
            Log.w("SearchDebug", "AniList default search failed/empty — using MAL ranking fallback")
            return searchAnimeMalRankingFallback(page, perPage)
        }
        return parsed
    }

    /**
     * MAL search fallback that also honors year/format/genre/status filters.
     * MAL's v2 search endpoint silently ignores every filter except the text query,
     * so they are applied client-side against each returned node's metadata. Blank
     * queries use a ranking pool derived from the format/status filters so a
     * filtered browse still gets sensible seed data. AniList "tag" filters have no
     * MAL equivalent and are dropped here (caller guards tags-only queries).
     */
    suspend fun searchAnimeMalFiltered(
        query: String?,
        genres: List<String>?,
        format: String?,
        status: String?,
        year: Int?,
        page: Int = 1,
        perPage: Int = 30
    ): List<ExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
            val limit = perPage.coerceIn(1, 100)
            val offset = (page - 1).coerceAtLeast(0) * limit
            val url = if (!query.isNullOrBlank()) {
                Endpoints.Mal.searchAnimeUrl(query, limit, offset, fields)
            } else {
                Endpoints.Mal.rankingAnimeUrl(malAnimeRankingType(format, status), limit, offset, fields)
            }
            Log.d("SearchDebug", "MAL filtered fallback URL: $url")
            requestMalNodes(url) { node ->
                node.matchesSearchFilters(year, format, status, genres)
            }
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL filtered fallback failed: ${e.message}", e)
            emptyList()
        }
    }

    /** Ranking pool for blank-query filtered anime searches: format/status pick the closest MAL ranking type. */
    private fun malAnimeRankingType(format: String?, status: String?): String = when {
        status == "RELEASING" -> "airing"
        status == "NOT_YET_RELEASED" -> "upcoming"
        format == "MOVIE" -> "movie"
        format == "OVA" -> "ova"
        format == "SPECIAL" -> "special"
        else -> "all"
    }

    private fun MalAnimeNode.matchesSearchFilters(
        year: Int?,
        format: String?,
        status: String?,
        genres: List<String>?
    ): Boolean {
        if (year != null && (start_date?.take(4)?.toIntOrNull() ?: 0) != year) return false
        if (format != null && malFormat()?.equals(format, ignoreCase = true) != true) return false
        if (status != null && malStatus() != status) return false
        genres?.takeIf { it.isNotEmpty() }?.let { wanted ->
            val own = this.genres?.mapNotNull { it.name } ?: emptyList()
            if (wanted.any { w -> own.none { it.equals(w, ignoreCase = true) } }) return false
        }
        return true
    }

    /**
     * Searches the official MyAnimeList v2 API for anime by title.
     * Used as a fallback when the AniList GraphQL API is unavailable.
     * Only text queries can be mirrored; MAL has no genre/tag/filter search.
     */
    suspend fun searchAnimeMalFallback(query: String, page: Int = 1, perPage: Int = 30): List<ExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
            val limit = perPage.coerceIn(1, 100)
            val offset = (page - 1).coerceAtLeast(0) * limit
            val url = Endpoints.Mal.searchAnimeUrl(query, limit, offset, fields)
            Log.d("SearchDebug", "MAL fallback URL: $url")
            requestMalNodes(url)
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL fallback failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Returns MAL's top-anime ranking for the search screen's default (blank-query)
     * result set when AniList is unavailable.
     */
    suspend fun searchAnimeMalRankingFallback(page: Int = 1, perPage: Int = 30): List<ExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
            val limit = perPage.coerceIn(1, 100)
            val offset = (page - 1).coerceAtLeast(0) * limit
            val url = Endpoints.Mal.rankingAnimeUrl("all", limit, offset, fields)
            Log.d("SearchDebug", "MAL ranking fallback URL: $url")
            requestMalNodes(url)
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL ranking fallback failed: ${e.message}", e)
            emptyList()
        }
    }

    /** Shared MAL request/parse for search & ranking responses (both use data[].node).
     *  Optional [include] predicate lets filtered searches drop nodes that don't match. */
    private fun requestMalNodes(url: String, include: (MalAnimeNode) -> Boolean = { true }): List<ExploreMedia> {
        val request = Request.Builder().url(url)
            .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
            .header("Accept", "application/json")
            .header("User-Agent", "Tensei/1.0")
            .build()
        animeScheduleClient.newCall(request).execute().use { response ->
            val code = response.code
            if (code != 200) {
                Log.w("SearchDebug", "MAL fallback HTTP $code body=${response.body?.string()?.take(300)}")
                return emptyList()
            }
            val body = response.body?.string() ?: return emptyList()
            Log.d("SearchDebug", "MAL fallback HTTP 200 bodyLen=${body.length}")
            val parsed = try {
                json.decodeFromString<MalSearchResponse>(body)
            } catch (e: Exception) {
                Log.e("SearchDebug", "MAL fallback parse error: ${e.message}")
                return emptyList()
            }
            return parsed.data.mapNotNull { node ->
                if (include(node.node)) node.node.toExploreMedia() else null
            }
        }
    }

    /** Shared MAL request returning a single detail node (anime or manga detail endpoint). */
    private inline fun <reified T> requestMalDetail(url: String): T? {
        val request = Request.Builder().url(url)
            .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
            .header("Accept", "application/json")
            .header("User-Agent", "Tensei/1.0")
            .build()
        animeScheduleClient.newCall(request).execute().use { response ->
            if (response.code != 200) {
                Log.w("SearchDebug", "MAL detail HTTP ${response.code} url=$url")
                return null
            }
            val body = response.body?.string() ?: return null
            return try {
                json.decodeFromString<T>(body)
            } catch (e: Exception) {
                Log.e("SearchDebug", "MAL detail parse error: ${e.message}")
                null
            }
        }
    }

    /**
     * Detailed anime fallback from the official MAL v2 API (used when AniList is
     * unavailable). Returns a mapping of everything the detail screen can render
     * without needing the AniList GraphQL API.
     */
    suspend fun fetchDetailedAnimeFromMal(malId: Int): DetailedAnimeData? = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,synopsis,background,num_episodes,mean,rank," +
                "popularity,num_list_users,num_scoring_users,status,media_type,start_date,end_date,source,rating," +
                "studios,genres,related_anime{node{id,title,main_picture,media_type,num_episodes,status,start_date}}," +
                "recommendations{node{id,title,main_picture,media_type,num_episodes,status,start_date}}"
            val url = Endpoints.Mal.detailAnimeUrl(malId, fields)
            val node = requestMalDetail<MalAnimeNode>(url) ?: return@withContext null
            Log.d("SearchDebug", "MAL anime detail OK id=$malId title=${node.title}")
            node.toDetailedAnimeData()
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL anime detail failed: ${e.message}", e)
            null
        }
    }

    /**
     * Anime explore fallback: fills the main home rows from MAL ranking when AniList
     * is unavailable. Genre rows can't be mirrored (MAL ranking has no genre filter).
     */
    suspend fun fetchAnimeExploreFromMal(): Map<String, List<ExploreMedia>> = withContext(Dispatchers.IO) {
        val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
        val rankingDefs = listOf(
            "featured" to "bypopularity",
            "seasonal" to "airing",
            "topSeries" to "all",
            "topMovies" to "movie"
        )
        val sections = mutableMapOf<String, List<ExploreMedia>>()
        for ((key, rankingType) in rankingDefs) {
            try {
                val url = Endpoints.Mal.rankingAnimeUrl(rankingType, 30, 0, fields)
                val nodes = requestMalNodes(url)
                Log.d("SearchDebug", "MAL explore '$key': ${nodes.size} entries")
                if (nodes.isNotEmpty()) sections[key] = nodes
            } catch (e: Exception) {
                Log.e("SearchDebug", "MAL explore '$key' failed: ${e.message}")
            }
        }
        sections
    }

    private fun MalAnimeNode.toDetailedAnimeData(): DetailedAnimeData = DetailedAnimeData(
        id = id,
        malId = id,
        title = title ?: alternative_titles?.en ?: "Unknown",
        titleRomaji = title,
        titleEnglish = alternative_titles?.en,
        titleNative = null,
        cover = main_picture?.large ?: main_picture?.medium ?: "",
        banner = null,
        description = synopsis ?: background,
        episodes = num_episodes ?: 0,
        duration = null,
        status = malStatus(),
        averageScore = malMeanScore(),
        meanScore = malMeanScore(),
        popularity = num_list_users ?: popularity,
        favourites = num_scoring_users ?: rank,
        genres = genres?.mapNotNull { it.name } ?: emptyList(),
        tags = emptyList(),
        season = null,
        year = start_date?.take(4)?.toIntOrNull(),
        format = malFormat(),
        source = source,
        studios = studios?.map { StudioData(it.id ?: 0, it.name ?: "") } ?: emptyList(),
        startDate = start_date,
        endDate = end_date,
        nextAiringEpisode = null,
        nextAiringTime = null,
        isAdult = nsfw == "black",
        staff = null,
        recommendations = recommendations?.mapNotNull { rec ->
            rec.node?.let { n ->
                ExploreAnime(
                    id = n.id, title = n.title ?: n.alternative_titles?.en ?: "Unknown",
                    titleEnglish = n.alternative_titles?.en,
                    cover = n.main_picture?.large ?: n.main_picture?.medium ?: "",
                    banner = null,
                    episodes = n.num_episodes ?: 0,
                    latestEpisode = null,
                    averageScore = n.malMeanScore(),
                    genres = n.genres?.mapNotNull { it.name } ?: emptyList(),
                    year = n.start_date?.take(4)?.toIntOrNull(),
                    malId = n.id,
                    format = n.malFormat(),
                    isAdult = n.nsfw == "black"
                )
            }
        } ?: emptyList(),
        latestEpisode = null,
        relations = related_anime?.mapNotNull { rel ->
            rel.node?.let { n ->
                AnimeRelation(
                    id = n.id, title = n.title ?: n.alternative_titles?.en ?: "Unknown",
                    titleRomaji = n.title,
                    cover = n.main_picture?.large ?: n.main_picture?.medium ?: "",
                    episodes = n.num_episodes,
                    latestEpisode = null,
                    averageScore = n.malMeanScore(),
                    format = n.malFormat(),
                    relationType = rel.relation_type?.uppercase() ?: "UNKNOWN"
                )
            }
        } ?: emptyList(),
        characters = null
    )

    private fun MalAnimeNode.malStatus(): String? = when (status) {
        "currently_airing" -> "RELEASING"
        "finished_airing" -> "FINISHED"
        "not_yet_aired" -> "NOT_YET_RELEASED"
        else -> status
    }

    private fun MalAnimeNode.malMeanScore(): Int? =
        mean?.let { (it * 10).roundToInt() }?.coerceIn(0, 100)

    private fun MalAnimeNode.malFormat(): String? = media_type?.let {
        when (it.lowercase()) {
            "tv" -> "TV"
            "movie" -> "MOVIE"
            "ova" -> "OVA"
            "ona" -> "ONA"
            "special" -> "SPECIAL"
            "music" -> "MUSIC"
            else -> it.uppercase()
        }
    }

    private fun MalAnimeNode.toExploreMedia(): ExploreMedia? {
        if (id <= 0) return null
        val picture = main_picture
        return ExploreMedia(
            id = id,
            idMal = id,
            title = MediaTitle(romaji = title, english = alternative_titles?.en),
            coverImage = MediaCoverImage(
                extraLarge = picture?.large,
                large = picture?.large,
                medium = picture?.medium
            ),
            bannerImage = null,
            episodes = num_episodes,
            status = when (status) {
                "currently_airing" -> "RELEASING"
                "finished_airing" -> "FINISHED"
                "not_yet_aired" -> "NOT_YET_RELEASED"
                else -> status
            },
            averageScore = mean?.let { (it * 10).roundToInt() }?.coerceIn(0, 100),
            genres = genres?.mapNotNull { it.name },
            seasonYear = start_date?.take(4)?.toIntOrNull(),
            startDate = start_date?.split("-")?.let {
                FuzzyDate(year = it.getOrNull(0)?.toIntOrNull(), month = it.getOrNull(1)?.toIntOrNull(), day = it.getOrNull(2)?.toIntOrNull())
            },
            isAdult = nsfw == "black",
            format = media_type?.let {
                when (it.lowercase()) {
                    "tv" -> "TV"
                    "movie" -> "MOVIE"
                    "ova" -> "OVA"
                    "ona" -> "ONA"
                    "special" -> "SPECIAL"
                    "music" -> "MUSIC"
                    else -> it.uppercase()
                }
            }
        )
    }

    suspend fun fetchAllTags(): List<MediaTag> {
        val response = publicGraphqlRequest(GraphqlQueries.GET_ALL_TAGS, emptyMap())
        return response?.let {
            try {
                json.decodeFromString<MediaTagCollectionResponse>(it).data.MediaTagCollection
            } catch (e: Exception) {
                Log.e("TagDebug", "Parse error: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
    }
    
    suspend fun findAnimeByMalId(malId: Int): ExploreMedia? {
        val query = $$"""
            query ($malId: Int) {
                Page(page: 1, perPage: 1) {
                    media(type: ANIME, idMal: $malId) {
                        id
                        idMal
                        title { romaji english native }
                        coverImage { extraLarge }
                        bannerImage
                        episodes
                        nextAiringEpisode { episode airingAt }
                        status
                        averageScore
                        genres
                        seasonYear
                        isAdult
                        startDate { year }
                        format
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("malId" to malId))?.let {
            try {
                val data = json.decodeFromString<ExploreResponse>(it)
                data.data.Page.media.firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
    }

    // ============================================
    // Detailed Anime
    // ============================================

    suspend fun fetchDetailedAnime(animeId: Int): DetailedAnimeMedia? {
        Log.d("AnimeDetailDebug", "fetchDetailedAnime START id=$animeId")
        val query = $$"""
            query ($id: Int) {
                Media(id: $id, type: ANIME) {
                    id
                    idMal
                    title { romaji english native }
                    coverImage { extraLarge }
                    bannerImage
                    description(asHtml: false)
                    episodes
                    duration
                    status
                    averageScore
                    popularity
                    favourites
                    genres
                    tags {
                        name
                        rank
                        isMediaSpoiler
                        description
                        isAdult
                    }
                    season
                    seasonYear
                    format
                    source
                    studios(isMain: true) { nodes { id name } }
                    startDate { year month day }
                    endDate { year month day }
                    nextAiringEpisode { episode airingAt }
                    isAdult
                    characters(perPage: 10) {
                        nodes {
                            id
                            name { full }
                            image { large }
                        }
                    }
                    trailer {
                        id
                        site
                    }
                    staff(perPage: 10) {
                        edges {
                            node {
                                id
                                name { full }
                                image { large }
                            }
                            role
                        }
                    }
                    relations {
                        edges {
                            relationType
                            node {
                                id
                                title { romaji english }
                                coverImage { extraLarge }
                                episodes
                                averageScore
                                format
                                nextAiringEpisode { episode }
                            }
                        }
                    }
                    recommendations(perPage: 20) {
                        nodes {
                            mediaRecommendation {
                                id
                                idMal
                                title { romaji english }
                                coverImage { extraLarge }
                                bannerImage
                                episodes
                                nextAiringEpisode { episode }
                                averageScore
                                genres
                                seasonYear
                                format
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("id" to animeId))?.let { response ->
            try {
                val data = json.decodeFromString<DetailedAnimeResponse>(response)
                val media = data.data.Media
                Log.d(
                    "AnimeDetailDebug",
                    "fetchDetailedAnime PARSED id=${media.id} title=${media.title?.romaji ?: media.title?.english}" +
                        " chars=${media.characters?.nodes?.size ?: 0}" +
                        " staff=${media.staff?.edges?.size ?: 0}" +
                        " relations=${media.relations?.edges?.size ?: 0}" +
                        " studios=${media.studios?.nodes?.size ?: 0}" +
                        " genres=${media.genres?.size ?: 0}" +
                        " desc=${media.description?.length ?: 0}" +
                        " recs=${media.recommendations?.nodes?.size ?: 0}"
                )
                media
            } catch (e: Exception) {
                Log.e("AnimeDetailDebug", "fetchDetailedAnime PARSE FAILED id=$animeId: ${e::class.simpleName}: ${e.message}", e)
                Log.d("AnimeDetailDebug", "fetchDetailedAnime RAW head=${response.take(500)}")
                null
            }
        }
    }

    suspend fun fetchAnimeRelationsForOffset(animeId: Int): AnimeRelationsMedia? {
        val query = $$"""
            query ($id: Int!) {
                Media(id: $id, type: ANIME) {
                    id
                    title { romaji english }
                    episodes
                    format
                    nextAiringEpisode { episode }
                    relations {
                        edges {
                            relationType
                            node {
                                id
                                title { romaji english }
                                episodes
                                type
                                format
                                nextAiringEpisode { episode }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("id" to animeId))?.let {
            try {
                json.decodeFromString<AnimeRelationsResponse>(it).data.Media
            } catch (e: Exception) { ErrorHandler.report("AnimeRepository", "operation failed, returning null", e); null }
        }
    }

    // ============================================
    // Mutations
    // ============================================

    suspend fun fetchCharacter(characterId: Int): CharacterData? {
        val query = $$"""
            query ($id: Int!) {
                Character(id: $id) {
                    id
                    name { full native }
                    image { large medium }
                    description(asHtml: false)
                    anime: media(perPage: 10, sort: POPULARITY_DESC) {
                        nodes {
                            id
                            title { romaji english }
                            coverImage { extraLarge }
                            format
                        }
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("id" to characterId))?.let { response ->
            try {
                val data = json.decodeFromString<CharacterResponse>(response)
                data.data.Character
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun fetchStaff(staffId: Int): StaffData? {
        val query = $$"""
            query ($id: Int!) {
                Staff(id: $id) {
                    id
                    name { full native }
                    image { large medium }
                    description(asHtml: false)
                    anime: staffMedia(perPage: 15, sort: POPULARITY_DESC) {
                        edges {
                            node {
                                id
                                title { romaji english }
                                coverImage { extraLarge }
                                format
                            }
                            staffRole
                        }
                    }
                }
            }
        """.trimIndent()

        val response = publicGraphqlRequest(query, mapOf("id" to staffId)) ?: return null
        return try {
            val data = json.decodeFromString<StaffResponse>(response)
            data.data.Staff
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAllCharacters(animeId: Int): List<CharacterData>? {
        val query = $$"""
            query ($id: Int!) {
                Media(id: $id, type: ANIME) {
                    characters(perPage: 50) {
                        nodes {
                            id
                            name { full native }
                            image { large medium }
                        }
                    }
                }
            }
        """.trimIndent()

        return publicGraphqlRequest(query, mapOf("id" to animeId))?.let { response ->
            try {
                val data = json.decodeFromString<AllCharactersResponse>(response)
                val characters = data.data.Media?.characters?.nodes
                characters?.distinctBy { it.id }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun fetchAllStaff(animeId: Int): List<StaffData>? {
        val query = $$"""
            query ($id: Int!) {
                Media(id: $id, type: ANIME) {
                    staff(perPage: 50) {
                        nodes {
                            id
                            name { full native }
                            image { large }
                            primaryOccupations
                        }
                    }
                }
            }
        """.trimIndent()

        val response = publicGraphqlRequest(query, mapOf("id" to animeId))
        return response?.let { resp ->
            try {
                val data = json.decodeFromString<AllStaffResponse>(resp)
                val staff = data.data.Media?.staff?.nodes
                staff
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun updateProgress(mediaId: Int, progress: Int): Boolean {
        cacheManager.invalidateUserCache()
        graphQLClient.clearCache() // Invalidate high-performance client cache too

        val query = $$"""
            mutation ($mediaId: Int, $progress: Int) {
                SaveMediaListEntry(mediaId: $mediaId, progress: $progress) {
                    id
                    progress
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("mediaId" to mediaId, "progress" to progress)) != null
    }

    suspend fun updateStatus(mediaId: Int, status: String, progress: Int? = null, score: Int? = null): Boolean {
        cacheManager.invalidateUserCache()
        graphQLClient.clearCache()

        Log.d("AniListScoreDebug", "updateStatus mediaId=$mediaId status=$status progress=$progress score=$score")

        val query = $$"""
            mutation ($mediaId: Int, $status: MediaListStatus$${if (progress != null) $$", $progress: Int" else ""}$${if (score != null) $$", $score: Float" else ""}) {
                SaveMediaListEntry(mediaId: $mediaId, status: $status$${if (progress != null) $$", progress: $progress" else ""}$${if (score != null) $$", score: $score" else ""}) {
                    id
                    status
                    score
                }
            }
        """.trimIndent()

        val variables = mutableMapOf<String, Any?>("mediaId" to mediaId, "status" to status)
        if (progress != null) variables["progress"] = progress
        if (score != null) variables["score"] = score

        return graphqlMutation(query, variables) != null
    }

    suspend fun deleteListEntry(entryId: Int): Boolean {
        cacheManager.invalidateUserCache()
        graphQLClient.clearCache()

        val query = $$"""
            mutation ($id: Int) {
                DeleteMediaListEntry(id: $id) {
                    deleted
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("id" to entryId)) != null
    }

    suspend fun updateScore(mediaId: Int, score: Int): Boolean {
        cacheManager.invalidateUserCache()
        graphQLClient.clearCache()

        val query = $$"""
            mutation ($mediaId: Int, $score: Float) {
                SaveMediaListEntry(mediaId: $mediaId, score: $score) {
                    id
                    score
                }
            }
        """.trimIndent()

        return graphqlMutation(query, mapOf("mediaId" to mediaId, "score" to score)) != null
    }

    // ============================================
    // Stream Operations
    // ============================================

    // ============================================
    // TMDB Operations
    // ============================================

    // TMDB episode fetching logic — implementations live in AnimeRepositoryTmdbExt.kt
    // (suspend fun fetchTmdbEpisodes, searchTmdb, fetchTvDetails, fetchSeason,
    //  buildEpisodesFromPool, calculateEpisodeOffset, calculateRecursiveOffset,
    //  getPrequelEpisodesSum, fetchEpisodeOffsetFromAniwatch,
    //  findTmdbEpisodeOffsetByTitle, wordOverlapScore, findBestMatch,
    //  normalizeTitle, detectFormatFromTitle, extractBaseTitle)

    suspend fun fetchUserActivity(userId: Int, perPage: Int = 50): List<UserActivity>? {
        val query = $$"""
            query ($userId: Int) {
                Page(page: 1, perPage: $$perPage) {
                    activities(userId: $userId, type: ANIME_LIST, sort: ID_DESC) {
                        ... on ListActivity {
                            createdAt
                            status
                            progress
                            media {
                                id
                                idMal
                                title { romaji english }
                                coverImage { extraLarge }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("userId" to userId))?.let {
            try {
                val data = json.decodeFromString<SimpleActivityResponse>(it)
                data.data.Page.activities.mapIndexedNotNull { index, activity ->
                    if (activity.media != null) {
                        UserActivity(
                            id = index,
                            type = "ANIME_LIST",
                            status = activity.status ?: "",
                            progress = activity.progress,
                            createdAt = activity.createdAt,
                            mediaId = activity.media.id,
                            mediaIdMal = activity.media.idMal,
                            mediaTitle = activity.media.title.romaji ?: activity.media.title.english
                            ?: "Unknown",
                            mediaTitleEnglish = activity.media.title.english,
                            mediaCover = activity.media.coverImage?.extraLarge ?: "",
                            episodes = null,
                            averageScore = null,
                            year = null
                        )
                    } else null
                }
            } catch (e: Exception) { ErrorHandler.report("AnimeRepository", "operation failed, returning null", e); null }
        }
    }

    suspend fun fetchUserFavorites(userId: Int): UserFavoritesResponse? {
        val query = $$"""
            query ($userId: Int) {
                User(id: $userId) {
                    favourites {
                        anime(page: 1, perPage: 30) {
                            nodes {
                                id
                                idMal
                                title { romaji english }
                                coverImage { extraLarge }
                                episodes
                                averageScore
                                genres
                                seasonYear
                                format
                                status
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return graphqlRequest(query, mapOf("userId" to userId))?.let {
            try {
                val response = json.decodeFromString<UserFavoritesResponse>(it)
                response
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun toggleAniListFavorite(mediaId: Int): Boolean {
        graphQLClient.clearCache() // Clear cache to ensure fresh data
        
        val mutation = $$"""
            mutation ($mediaId: Int) {
                ToggleFavourite(animeId: $mediaId) {
                    anime { nodes { id } }
                }
            }
        """.trimIndent()

        val response = graphqlMutation(mutation, mapOf("mediaId" to mediaId))
        return !response.isNullOrEmpty()
    }
    
    suspend fun addAniListFavorite(mediaId: Int): Boolean {
        val mutation = $$"""
            mutation ($mediaId: Int) {
                ToggleFavourite(animeId: $mediaId) {
                    anime { nodes { id } }
                }
            }
        """.trimIndent()
        
        // Check if already favorited first
        val checkQuery = $$"""
            query ($mediaId: Int) {
                Media(id: $mediaId) {
                    id
                    isFavourite
                }
            }
        """.trimIndent()
        
        val result = graphqlRequest(checkQuery, mapOf("mediaId" to mediaId))
        if (result?.contains("\"isFavourite\":true") == true || result?.contains("\"isFavourite\": true") == true) {
            return true // Already favorited
        }
        
        val success = graphqlMutation(mutation, mapOf("mediaId" to mediaId)) != null
        return success
    }
    
    suspend fun removeAniListFavorite(mediaId: Int): Boolean {
        val mutation = $$"""
            mutation ($mediaId: Int) {
                ToggleFavourite(animeId: $mediaId) {
                    anime { nodes { id } }
                }
            }
        """.trimIndent()
        
        // Check if not favorited first
        val checkQuery = $$"""
            query ($mediaId: Int) {
                Media(id: $mediaId) {
                    id
                    isFavourite
                }
            }
        """.trimIndent()
        
        val result = graphqlRequest(checkQuery, mapOf("mediaId" to mediaId))
        if (result?.contains("\"isFavourite\":false") == true || result?.contains("\"isFavourite\": false") == true) {
            return true // Already not favorited
        }
        
        val success = graphqlMutation(mutation, mapOf("mediaId" to mediaId)) != null
        return success
    }
    
    suspend fun toggleAniListFavorite(mediaId: Int, addFavorite: Boolean): Boolean {
        if (addFavorite) {
            // Check if already favorited
            val checkQuery = $$"""
                query ($mediaId: Int) {
                    Media(id: $mediaId) {
                        id
                        isFavourite
                    }
                }
            """.trimIndent()
            
            val result = graphqlRequest(checkQuery, mapOf("mediaId" to mediaId))
            
            if (result?.contains("\"isFavourite\":true") == true || result?.contains("\"isFavourite\": true") == true) {
                return true // Already favorited
            }
        }
        
        val mutation = $$"""
            mutation ($mediaId: Int) {
                ToggleFavourite(animeId: $mediaId) {
                    anime { nodes { id } }
                }
            }
        """.trimIndent()
        
        val success = graphqlMutation(mutation, mapOf("mediaId" to mediaId)) != null
        return success
    }
}


class AiringScheduleApiDownException : Exception()
class AnimeScheduleUnavailableException : Exception()


