package com.blissless.tensei.data

import android.util.Log
import com.blissless.tensei.BuildConfig
import com.blissless.tensei.data.models.AiringScheduleAnime
import com.blissless.tensei.data.models.AiringScheduleEntry
import com.blissless.tensei.data.models.AiringScheduleResponse
import com.blissless.tensei.data.models.AnimeScheduleTimetableEntry
import com.blissless.tensei.data.models.AllCharactersResponse
import com.blissless.tensei.data.models.AllStaffResponse
import com.blissless.tensei.data.models.AnimeRelationsMedia
import com.blissless.tensei.data.models.AnimeRelationsResponse
import com.blissless.tensei.data.models.BatchedExploreResponse
import com.blissless.tensei.data.models.CharacterData
import com.blissless.tensei.data.models.CharacterResponse
import com.blissless.tensei.data.models.DetailedAnimeMedia
import com.blissless.tensei.data.models.DetailedAnimeResponse
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
        var droppedAired = 0
        var droppedNoDate = 0
        var droppedBadDate = 0
        var droppedSeen = 0
        val seenRoutes = mutableSetOf<String>()
        for (entry in entries) {
            if (entry.airingStatus == "aired") {
                droppedAired++
                continue
            }
            if (!seenRoutes.add(entry.route)) {
                droppedSeen++
                Log.w("AiringDebug", "AnimeSchedule duplicate route '${entry.route}' skipped")
                continue
            }
            val dateStr = entry.episodeDate
            if (dateStr.isNullOrBlank()) {
                droppedNoDate++
                Log.w("AiringDebug", "AnimeSchedule '${entry.title ?: entry.route}' dropped: no episodeDate")
                continue
            }
            val airingAt = parseAnimeScheduleDate(dateStr)
            if (airingAt == null) {
                droppedBadDate++
                continue
            }
            val title = entry.english ?: entry.romaji ?: entry.title ?: "Unknown"
            result.add(
                AiringScheduleAnime(
                    id = entry.route.hashCode(),
                    title = title,
                    titleEnglish = entry.english ?: entry.romaji,
                    cover = entry.imageVersionRoute?.let {
                        "${Endpoints.AnimeSchedule.IMAGE_BASE_URL}/${it.trimStart('/')}"
                    } ?: "",
                    episodes = entry.episodes ?: 0,
                    airingEpisode = entry.episodeNumber ?: 0,
                    airingAt = airingAt,
                    timeUntilAiring = airingAt - now,
                    year = dateStr.take(4).toIntOrNull(),
                    isAdult = entry.donghua
                )
            )
        }
        Log.d("AiringDebug", "AnimeSchedule map summary: total=${entries.size} kept=${result.size} dropped(aired=$droppedAired, seen=$droppedSeen, noDate=$droppedNoDate, badDate=$droppedBadDate)")
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
        // search API so the search screen still returns results for text queries.
        if (parsed.isEmpty() && search != null) {
            Log.w("SearchDebug", "AniList search failed/empty — using MAL fallback for '$search'")
            return searchAnimeMalFallback(search, page, perPage)
        }
        return parsed
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
            val request = Request.Builder().url(url)
                .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
                .header("Accept", "application/json")
                .header("User-Agent", "Tensei/1.0")
                .build()
            animeScheduleClient.newCall(request).execute().use { response ->
                val code = response.code
                if (code != 200) {
                    Log.w("SearchDebug", "MAL fallback HTTP $code body=${response.body?.string()?.take(300)}")
                    return@use emptyList()
                }
                val body = response.body?.string() ?: return@use emptyList()
                Log.d("SearchDebug", "MAL fallback HTTP 200 bodyLen=${body.length}")
                val parsed = try {
                    json.decodeFromString<MalSearchResponse>(body)
                } catch (e: Exception) {
                    Log.e("SearchDebug", "MAL fallback parse error: ${e.message}")
                    return@use emptyList()
                }
                parsed.data.mapNotNull { node -> node.node.toExploreMedia() }
            }
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL fallback failed: ${e.message}", e)
            emptyList()
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


