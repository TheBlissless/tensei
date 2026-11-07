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

/**
 * Handles all API calls and data fetching.
 * Optimized to use GraphQLClient for high-performance AniList requests.
 */

internal val CLIENT_IDS = listOf(BuildConfig.CLIENT_ID_ANILIST)
internal const val MAX_AIRING_PAGES = 5
internal const val ANIME_SCHEDULE_TIMEOUT_MS = 15000

internal val animeScheduleClient = OkHttpClient.Builder()
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
internal val graphQLClient = GraphQLClient(
    config = GraphQLConfig(
        maxConcurrentRequests = 5,
        minRequestIntervalMs = 100L,
        cacheDurationMs = 60 * 60 * 1000L, // 1 hour for public data
        userDataCacheDurationMs = 60 * 60 * 1000L // 1 hour for user data
    )
)

// Use longer cache for authenticated requests
internal val authCacheDuration get() = graphQLClient.getConfig().userDataCacheDurationMs

class AnimeRepository(
    internal val userPreferences: UserPreferences,
    internal val cacheManager: CacheManager
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

        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = true,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = false, // Mutations should never be cached
            parser = { it }
        )

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
        }
        return result.data
    }

    /**
     * True when AniList answers a minimal throwaway query right now. Deliberately cache-free
     * and auth-free: the fallback-recovery loop probes this every 30s while any source is
     * stranded on a MAL/AnimeSchedule fallback, so it must reflect the LIVE endpoint state
     * rather than a stale 1-hour cache or a token/session problem.
     */
    suspend fun isAniListReachable(): Boolean = withContext(Dispatchers.IO) {
        val result = graphQLClient.execute(
            query = "query { Media(type: ANIME, sort: POPULARITY_DESC, perPage: 1) { id } }",
            variables = emptyMap(),
            requiresAuth = false,
            clientIds = CLIENT_IDS,
            useCache = false,
            parser = { it }
        )
        val ok = result.data != null
        if (!ok) {
            Log.w("AniListRecovery", "probe failed: code=${result.error?.code} msg=${result.error?.message} rateLimit=${result.error?.isRateLimit}")
        }
        ok
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
            query ($userId: Int, $sort: [MediaListSort]) {
                MediaListCollection(userId: $userId, type: ANIME, sort: $sort) {
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

        return graphqlRequest(query, mapOf("userId" to userId, "sort" to listOf("UPDATED_TIME_DESC")))?.let {
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

        val response = publicGraphqlRequest(query, varValues)
        val parsed = response?.let {
            try {
                val data = json.decodeFromString<ExploreResponse>(it)
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
                media
            } catch (e: Exception) {
                Log.e("AnimeDetailDebug", "fetchDetailedAnime PARSE FAILED id=$animeId: ${e::class.simpleName}: ${e.message}", e)
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





























        
}

class AiringScheduleApiDownException : Exception()
class AnimeScheduleUnavailableException : Exception()

