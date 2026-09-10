package com.blissless.tensei.data.manga

import com.blissless.tensei.data.models.MangaActivity
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaActivityResponse
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaDetailMedia
import com.blissless.tensei.data.models.MangaDetailResponse
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaExploreResponse
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaFavoritesResponse
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaMediaListEntry
import com.blissless.tensei.data.models.MangaRelation
import com.blissless.tensei.data.models.MangaUserListResponse
import com.blissless.tensei.data.models.MangaUserProfileResponse
import com.blissless.tensei.data.models.MangaDetailCharacters
import com.blissless.tensei.data.models.MangaStaff
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaCharacterName
import com.blissless.tensei.data.models.MangaCharacters
import com.blissless.tensei.data.models.MangaMalSearchResponse
import com.blissless.tensei.data.models.MalMangaNode
import com.blissless.tensei.BuildConfig
import com.blissless.tensei.network.Endpoints
import com.blissless.tensei.network.GraphQLClient
import com.blissless.tensei.network.GraphQLConfig
import com.blissless.tensei.util.ErrorHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MangaRepository {

    private val graphQLClient = GraphQLClient(
        config = GraphQLConfig(
            maxConcurrentRequests = 5,
            minRequestIntervalMs = 100L,
            cacheDurationMs = 60 * 60 * 1000L,
            userDataCacheDurationMs = 60 * 60 * 1000L
        )
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    companion object {
        private val CLIENT_IDS = listOf(BuildConfig.CLIENT_ID_ANILIST)
        private const val TAG = "MangaRepository"
    }

    private suspend fun executeQuery(query: String, variables: Map<String, Any?> = emptyMap(), token: String? = null, useCache: Boolean = true): String? {
        android.util.Log.d(TAG, "executeQuery: query=${query.take(100)}... variables=$variables token=${token != null} useCache=$useCache")
        val result = graphQLClient.execute(
            query = query,
            variables = variables,
            requiresAuth = token != null,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = useCache,
            parser = { it }
        )
        android.util.Log.d(TAG, "executeQuery: result.data=${result.data != null} result.error=${result.error?.message} fromCache=${result.fromCache}")
        if (result.data != null) {
            // GraphQL APIs return HTTP 200 with an "errors" array on failure, so a non-null
            // body doesn't mean the mutation succeeded. Log whether errors are buried in the body.
            val hasGraphQLErrors = result.data.contains("\"errors\"")
            android.util.Log.d(TAG, "executeQuery: raw response (first 400 chars): ${result.data.take(400)} graphqlErrorsInBody=$hasGraphQLErrors")
        }
        return result.data
    }

    suspend fun searchManga(search: String, page: Int = 1, perPage: Int = 30): List<MangaExploreMedia> {
        val query = """
            query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int) {
                Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    media(search: ${'$'}search, type: MANGA, isAdult: false) {
                        id idMal title { romaji english }
                        coverImage { extraLarge large }
                        bannerImage chapters volumes status averageScore genres
                        seasonYear startDate { year month day } isAdult format
                    }
                }
            }
        """.trimIndent()
        val vars = mapOf("search" to search, "page" to page, "perPage" to perPage)
        val raw = executeQuery(query, vars) ?: return emptyList()
        return try {
            json.decodeFromString<MangaExploreResponse>(raw).data.Page.media
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "search parse failed", e); emptyList()
        }
    }

    suspend fun fetchExploreSections(token: String? = null): Map<String, List<MangaExploreMedia>> {
        // Try the batched query first (single HTTP request for all 8 sections)
        val batched = fetchExploreSectionsBatched(token)
        if (batched.isNotEmpty()) return batched

        // Fallback: fetch sections individually if the batched query failed
        android.util.Log.w(TAG, "Batched explore query returned empty, falling back to individual fetches")
        return fetchExploreSectionsIndividual(token)
    }

    private suspend fun fetchExploreSectionsBatched(token: String? = null): Map<String, List<MangaExploreMedia>> {
        val query = """
            query {
                trending: Page(page: 1, perPage: 50) { media(sort: TRENDING_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                popular: Page(page: 1, perPage: 50) { media(sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                topRated: Page(page: 1, perPage: 50) { media(sort: SCORE_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                favourites: Page(page: 1, perPage: 50) { media(sort: FAVOURITES_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                action: Page(page: 1, perPage: 50) { media(genre: "Action", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                romance: Page(page: 1, perPage: 50) { media(genre: "Romance", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                comedy: Page(page: 1, perPage: 50) { media(genre: "Comedy", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                fantasy: Page(page: 1, perPage: 50) { media(genre: "Fantasy", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                "sci-fi": Page(page: 1, perPage: 50) { media(genre: "Sci-Fi", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
                seinen: Page(page: 1, perPage: 50) { media(genre: "Seinen", sort: POPULARITY_DESC, type: MANGA, isAdult: false) { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear startDate { year month day } isAdult format } }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, emptyMap(), token)
        if (raw == null) {
            android.util.Log.w(TAG, "fetchExploreSectionsBatched: executeQuery returned null after retries")
            return emptyMap()
        }
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            // Check for GraphQL errors
            root["errors"]?.let { errors ->
                android.util.Log.w(TAG, "GraphQL errors in explore response: $errors")
            }
            val data = root["data"]?.jsonObject ?: run {
                android.util.Log.w(TAG, "No 'data' in explore response: ${raw.take(200)}")
                return emptyMap()
            }
            val sections = mutableMapOf<String, List<MangaExploreMedia>>()
            for ((key, value) in data) {
                val page = value.jsonObject["Page"]?.jsonObject ?: continue
                val mediaArray = page["media"]?.jsonArray ?: continue
                sections[key] = json.decodeFromJsonElement<List<MangaExploreMedia>>(mediaArray)
            }
            android.util.Log.d(TAG, "fetchExploreSectionsBatched: got ${sections.size} sections, sizes=${sections.mapValues { it.value.size }}")
            sections
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "explore parse failed", e)
            emptyMap()
        }
    }

    /**
     * Fallback: fetch each explore section as a separate request.
     * Used when the batched query fails or returns empty.
     */
    private suspend fun fetchExploreSectionsIndividual(token: String? = null): Map<String, List<MangaExploreMedia>> = withContext(Dispatchers.IO) {
        val sections = mutableMapOf<String, List<MangaExploreMedia>>()
        val sectionDefs = listOf(
            "trending" to """sort: TRENDING_DESC""",
            "popular" to """sort: POPULARITY_DESC""",
            "topRated" to """sort: SCORE_DESC""",
            "favourites" to """sort: FAVOURITES_DESC""",
            "action" to """genre: "Action", sort: POPULARITY_DESC""",
            "romance" to """genre: "Romance", sort: POPULARITY_DESC""",
            "comedy" to """genre: "Comedy", sort: POPULARITY_DESC""",
            "fantasy" to """genre: "Fantasy", sort: POPULARITY_DESC""",
            "sci-fi" to """genre: "Sci-Fi", sort: POPULARITY_DESC""",
            "seinen" to """genre: "Seinen", sort: POPULARITY_DESC"""
        )
        // Fetch every section concurrently instead of sequentially. The shared GraphQL
        // client enforces the 5-concurrent/100ms rate limit, so this only cuts wall-clock
        // time (~10 round trips → ~1). Parsing also happens off the main thread.
        val results = coroutineScope {
            sectionDefs.map { (key, filter) ->
                async {
                    val query = """
                        query {
                            Page(page: 1, perPage: 50) {
                                media($filter, type: MANGA, isAdult: false) {
                                    id idMal title { romaji english }
                                    coverImage { extraLarge large }
                                    bannerImage chapters volumes status averageScore genres
                                    seasonYear startDate { year month day } isAdult format
                                }
                            }
                        }
                    """.trimIndent()
                    val raw = executeWithRetry(query, emptyMap(), token) ?: return@async null
                    try {
                        key to json.decodeFromString<MangaExploreResponse>(raw).data.Page.media
                    } catch (e: Exception) {
                        ErrorHandler.ignore(TAG, "individual section '$key' parse failed", e)
                        null
                    }
                }
            }.awaitAll()
        }
        results.forEach { pair -> pair?.let { sections[it.first] = it.second } }
        android.util.Log.d(TAG, "fetchExploreSectionsIndividual: got ${sections.size} sections")
        sections
    }

    /**
     * Execute a GraphQL query with retry-on-403 logic. AniList sometimes returns HTTP 403
     * ("API temporarily disabled due to severe stability issues") during server-side outages.
     * We retry up to 3 times with exponential backoff (1s, 2s, 4s) before giving up.
     */
    private suspend fun executeWithRetry(
        query: String,
        variables: Map<String, Any?> = emptyMap(),
        token: String? = null,
        maxRetries: Int = 2,
        useCache: Boolean = true
    ): String? {
        var lastError: String? = null
        repeat(maxRetries) { attempt ->
            val raw = executeQuery(query, variables, token, useCache)
            if (raw != null) return raw
            // If the query failed, wait and retry with short backoff (400ms, 800ms)
            val delayMs = 400L * (1 shl attempt)
            android.util.Log.w(TAG, "executeWithRetry: attempt ${attempt + 1}/$maxRetries failed, retrying in ${delayMs}ms")
            kotlinx.coroutines.delay(delayMs)
            lastError = "Retries exhausted"
        }
        android.util.Log.w(TAG, "executeWithRetry: all $maxRetries attempts failed ($lastError)")
        return null
    }

    suspend fun fetchMangaDetail(mangaId: Int, token: String? = null): MangaDetail? {
        android.util.Log.d(TAG, "fetchMangaDetail: mangaId=$mangaId token=${token != null}")
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: MANGA) {
                    id idMal title { romaji english native }
                    coverImage { extraLarge large } bannerImage description
                    chapters volumes status averageScore meanScore popularity favourites
                    genres tags { name rank isMediaSpoiler description isAdult }
                    seasonYear startDate { year } format source isAdult
                    relations { edges { relationType node { id title { romaji english } coverImage { extraLarge } chapters averageScore format } } }
                    characters { nodes { id name { full native } image { large } } }
                    staff { edges { node { id name { full native } image { large } } role } }
                    recommendations { nodes { mediaRecommendation { id idMal title { romaji english } coverImage { extraLarge } chapters volumes averageScore format } } }
                    rankings { id rank type context allTime season year }
                    synonyms externalLinks { url site }
                }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, mapOf("id" to mangaId), token) ?: run {
            android.util.Log.w(TAG, "fetchMangaDetail: executeQuery returned null for mangaId=$mangaId after retries")
            return null
        }
        return try {
            val wrapper = json.decodeFromString<MangaDetailResponse>(raw)
            val media = wrapper.data.Media
            android.util.Log.d(TAG, "fetchMangaDetail: success, title=${media.title?.romaji} chapters=${media.chapters} " +
                "chars=${media.characters?.nodes?.size ?: 0} staff=${media.staff?.edges?.size ?: 0} " +
                "relations=${media.relations?.edges?.size ?: 0} recs=${media.recommendations?.nodes?.size ?: 0} " +
                "tags=${media.tags?.size ?: 0} genres=${media.genres?.size ?: 0} " +
                "descNull=${media.description == null} popularity=${media.popularity} favourites=${media.favourites} source=${media.source}")
            mapMangaDetail(media)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "fetchMangaDetail: parse failed for mangaId=$mangaId: ${e.message}", e)
            android.util.Log.e(TAG, "fetchMangaDetail: raw response (first 500 chars): ${raw.take(500)}")
            ErrorHandler.ignore(TAG, "detail parse failed", e); null
        }
    }

    private fun mapMangaDetail(media: MangaDetailMedia): MangaDetail {
        return MangaDetail(
            id = media.id,
            malId = media.idMal,
            title = media.title?.romaji ?: media.title?.english ?: "Unknown",
            titleEnglish = media.title?.english,
            titleNative = media.title?.native,
            cover = media.coverImage?.extraLarge ?: media.coverImage?.large ?: "",
            banner = media.bannerImage,
            description = media.description,
            chapters = media.chapters ?: 0,
            volumes = media.volumes,
            status = media.status,
            averageScore = media.averageScore,
            meanScore = media.meanScore,
            popularity = media.popularity,
            favourites = media.favourites,
            genres = media.genres ?: emptyList(),
            tags = media.tags ?: emptyList(),
            year = media.seasonYear ?: media.startDate?.year,
            format = media.format,
            source = media.source,
            isAdult = media.isAdult,
            staff = media.staff?.let { MangaStaff(it.edges.map { e ->
                MangaStaffEdge(
                    node = e.node?.let { n -> com.blissless.tensei.data.models.MangaStaffNode(n.id, n.name?.let { MangaCharacterName(it.full, it.native) }, n.image) },
                    role = e.role
                )
            })},
            recommendations = media.recommendations?.nodes?.mapNotNull { r ->
                r.mediaRecommendation?.let { m ->
                    MangaMedia(
                        id = m.id, title = m.title.romaji ?: m.title.english ?: "",
                        titleEnglish = m.title.english, cover = m.coverImage?.extraLarge ?: "",
                        totalChapters = m.chapters ?: 0, averageScore = m.averageScore
                    )
                }
            } ?: emptyList(),
            characters = media.characters?.let { MangaCharacters(it.nodes.map { n ->
                MangaCharacterNode(n.id, n.name?.let { MangaCharacterName(it.full, it.native) }, n.image)
            })},
            relations = media.relations?.edges?.mapNotNull { e ->
                e.node?.let { node ->
                    MangaRelation(
                        id = node.id,
                        title = node.title?.english ?: node.title?.romaji ?: "Unknown",
                        titleRomaji = node.title?.romaji,
                        cover = node.coverImage?.extraLarge ?: "",
                        chapters = node.chapters,
                        averageScore = node.averageScore,
                        format = node.format,
                        relationType = e.relationType ?: "UNKNOWN"
                    )
                }
            } ?: emptyList(),
            synonymTitles = media.synonyms ?: emptyList(),
            rankings = media.rankings ?: emptyList(),
            externalLinks = media.externalLinks ?: emptyList()
        )
    }

    suspend fun fetchMangaAllCharacters(mangaId: Int): List<MangaCharacterNode> {
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: MANGA) {
                    id
                    characters(perPage: 100, sort: ROLE) {
                        nodes { id name { full native } image { large } }
                    }
                }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, mapOf("id" to mangaId)) ?: return emptyList()
        return try {
            json.decodeFromString<MangaDetailResponse>(raw).data.Media.characters?.nodes
                ?.map { MangaCharacterNode(it.id, it.name, it.image) } ?: emptyList()
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "all characters parse failed", e); emptyList()
        }
    }

    suspend fun fetchMangaAllStaff(mangaId: Int): List<MangaStaffEdge> {
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: MANGA) {
                    id
                    staff(perPage: 100) {
                        edges { node { id name { full native } image { large } } role }
                    }
                }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, mapOf("id" to mangaId)) ?: return emptyList()
        return try {
            json.decodeFromString<MangaDetailResponse>(raw).data.Media.staff?.edges ?: emptyList()
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "all staff parse failed", e); emptyList()
        }
    }

    suspend fun fetchMangaAllRelations(mangaId: Int): List<MangaRelation> {
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: MANGA) {
                    id
                    relations {
                        edges {
                            relationType
                            node {
                                id title { romaji english }
                                coverImage { extraLarge }
                                chapters averageScore format
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, mapOf("id" to mangaId)) ?: return emptyList()
        return try {
            val media = json.decodeFromString<MangaDetailResponse>(raw).data.Media
            media.relations?.edges?.mapNotNull { e ->
                e.node?.let { node ->
                    MangaRelation(
                        id = node.id,
                        title = node.title?.english ?: node.title?.romaji ?: "Unknown",
                        titleRomaji = node.title?.romaji,
                        cover = node.coverImage?.extraLarge ?: "",
                        chapters = node.chapters,
                        averageScore = node.averageScore,
                        format = node.format,
                        relationType = e.relationType ?: "UNKNOWN"
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "all relations parse failed", e); emptyList()
        }
    }

    suspend fun fetchMangaAllRecommendations(mangaId: Int): List<MangaMedia> {
        val query = """
            query (${'$'}id: Int) {
                Media(id: ${'$'}id, type: MANGA) {
                    id
                    recommendations(perPage: 100) {
                        nodes {
                            mediaRecommendation {
                                id title { romaji english }
                                coverImage { extraLarge }
                                chapters averageScore format
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val raw = executeWithRetry(query, mapOf("id" to mangaId)) ?: return emptyList()
        return try {
            val media = json.decodeFromString<MangaDetailResponse>(raw).data.Media
            media.recommendations?.nodes?.mapNotNull { r ->
                r.mediaRecommendation?.let { m ->
                    MangaMedia(
                        id = m.id,
                        title = m.title?.romaji ?: m.title?.english ?: "Unknown",
                        titleEnglish = m.title?.english,
                        cover = m.coverImage?.extraLarge ?: "",
                        totalChapters = m.chapters ?: 0,
                        averageScore = m.averageScore,
                        format = m.format
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "all recommendations parse failed", e); emptyList()
        }
    }

    suspend fun fetchUserMangaLists(userId: Int, token: String): Map<String, List<MangaMedia>>? {
        val query = """
            query (${'$'}userId: Int) {
                MediaListCollection(userId: ${'$'}userId, type: MANGA) {
                    lists { name status entries { id mediaId progress progressVolumes score status media { id idMal title { romaji english } coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres seasonYear format } } }
                }
            }
        """.trimIndent()
        val raw = executeQuery(query, mapOf("userId" to userId), token) ?: return null
        return try {
            val response = json.decodeFromString<MangaUserListResponse>(raw)
            response.data.MediaListCollection.lists.associate { list ->
                (list.status ?: list.name) to list.entries.map { entry -> mapMangaMedia(entry) }
            }
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "user lists parse failed", e); null
        }
    }

    private fun mapMangaMedia(entry: MangaMediaListEntry): MangaMedia {
        val media = entry.media
        return MangaMedia(
            id = media.id,
            title = media.title.romaji ?: media.title.english ?: "Unknown",
            titleEnglish = media.title.english,
            cover = media.coverImage?.extraLarge ?: media.coverImage?.large ?: "",
            banner = media.bannerImage,
            progress = entry.progress ?: 0,
            totalChapters = media.chapters ?: 0,
            totalVolumes = media.volumes,
            status = media.status ?: "",
            averageScore = media.averageScore,
            genres = media.genres ?: emptyList(),
            listStatus = entry.status ?: "",
            listEntryId = entry.id,
            userScore = entry.score,
            year = media.seasonYear ?: media.startDate?.year,
            malId = media.idMal,
            format = media.format
        )
    }

    suspend fun searchMangaAdvanced(
        search: String? = null,
        genres: List<String> = emptyList(),
        format: String? = null,
        status: String? = null,
        sort: String = "SEARCH_MATCH",
        page: Int = 1,
        perPage: Int = 30
    ): List<MangaExploreMedia> {
        val query = """
            query (${'$'}search: String, ${'$'}genres: [String], ${'$'}format: MediaFormat, ${'$'}status: MediaStatus, ${'$'}sort: [MediaSort], ${'$'}page: Int, ${'$'}perPage: Int) {
                Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    media(search: ${'$'}search, genre_in: ${'$'}genres, format: ${'$'}format, status: ${'$'}status, sort: ${'$'}sort, type: MANGA, isAdult: false) {
                        id idMal title { romaji english }
                        coverImage { extraLarge large } bannerImage chapters volumes status averageScore genres
                        seasonYear startDate { year month day } isAdult format
                    }
                }
            }
        """.trimIndent()
        val vars = mutableMapOf<String, Any?>(
            "page" to page,
            "perPage" to perPage,
            "sort" to listOf(sort)
        )
        if (!search.isNullOrBlank()) vars["search"] = search
        if (genres.isNotEmpty()) vars["genres"] = genres
        if (!format.isNullOrBlank()) vars["format"] = format
        if (!status.isNullOrBlank()) vars["status"] = status
        val raw = executeQuery(query, vars)
        val results = raw?.let { body ->
            try {
                json.decodeFromString<MangaExploreResponse>(body).data.Page.media
            } catch (e: Exception) {
                ErrorHandler.ignore(TAG, "advanced search parse failed", e); emptyList()
            }
        } ?: emptyList()
        // AniList unavailable: fall back to MAL. MAL's /manga search silently ignores
        // genre/status/format params, so those filters are applied locally against each
        // returned node's metadata. Blank queries (default "discover" browse) fall back
        // to the MAL top-manga ranking.
        if (results.isEmpty()) {
            if (!search.isNullOrBlank() || genres.isNotEmpty() || !format.isNullOrBlank() || !status.isNullOrBlank()) {
                android.util.Log.w(TAG, "AniList manga search failed/empty — using MAL filtered fallback for '$search'")
                return searchMangaMalFiltered(search, genres, format, status, page, perPage)
            }
            android.util.Log.w(TAG, "AniList manga browse failed/empty — using MAL ranking fallback")
            return searchMangaMalRankingFallback(page, perPage)
        }
        return results
    }

    /**
     * MAL search fallback that also honors genre/format/status filters.
     * MAL's v2 search endpoint silently ignores every filter except the text query,
     * so they are applied client-side against each returned node's metadata. Blank
     * queries use a ranking pool derived from the format filter so a filtered browse
     * still gets sensible seed data. AniList "tag" filters have no MAL equivalent.
     */
    suspend fun searchMangaMalFiltered(
        query: String?,
        genres: List<String>,
        format: String?,
        status: String?,
        page: Int = 1,
        perPage: Int = 30
    ): List<MangaExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
            val limit = 100
            // MAL's search/ranking API only honors the text query — genre/format/status leaks
            // are applied client-side. A single 30-node page is usually too shallow to match
            // a genre chip (rarely present in the top-30 slice), so keep paging through the
            // pool until enough nodes match (or the pool is exhausted).
            val want = (page).coerceAtLeast(1) * perPage
            val collected = mutableListOf<MangaExploreMedia>()
            var offset = 0
            var poolPages = 0
            while (collected.size < want && poolPages < 6) {
                val url = if (!query.isNullOrBlank()) {
                    Endpoints.Mal.searchMangaUrl(query, limit, offset, fields)
                } else {
                    Endpoints.Mal.rankingMangaUrl(malMangaRankingType(format, status), limit, offset, fields)
                }
                android.util.Log.d("MangaMal", "filtered search URL: $url")
                val batch = fetchMalMangaNodes(url) { node -> node.matchesSearchFilters(format, status, genres) }
                if (batch.isEmpty()) break
                collected.addAll(batch)
                offset += limit
                poolPages++
            }
            collected.drop((page - 1).coerceAtLeast(0) * perPage).take(perPage)
        } catch (e: Exception) {
            android.util.Log.e("MangaMal", "filtered search failed: ${e.message}", e)
            emptyList()
        }
    }

    /** MAL top-manga ranking for the search screen's default blank-query browse when AniList is down. */
    suspend fun searchMangaMalRankingFallback(page: Int = 1, perPage: Int = 30): List<MangaExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
            val limit = perPage.coerceIn(1, 100)
            val offset = (page - 1).coerceAtLeast(0) * limit
            val url = Endpoints.Mal.rankingMangaUrl("all", limit, offset, fields)
            android.util.Log.d("MangaMal", "ranking fallback URL: $url")
            fetchMalMangaNodes(url)
        } catch (e: Exception) {
            android.util.Log.e("MangaMal", "ranking fallback failed: ${e.message}", e)
            emptyList()
        }
    }

    /** Ranking pool for blank-query filtered manga searches: format picks the closest MAL ranking type. */
    private fun malMangaRankingType(format: String?, status: String?): String = when {
        format == "NOVEL" -> "novels"
        format == "ONE_SHOT" -> "oneshots"
        format == "DOUJINSHI" -> "doujin"
        format == "MANHWA" -> "manhwa"
        format == "MANHUA" -> "manhua"
        format == "MANGA" -> "manga"
        else -> "all"
    }

    private fun MalMangaNode.matchesSearchFilters(
        format: String?,
        status: String?,
        genres: List<String>
    ): Boolean {
        if (format != null && malMangaFormat()?.equals(format, ignoreCase = true) != true) return false
        if (status != null && malMangaStatus() != status) return false
        if (genres.isNotEmpty()) {
            val own = this.genres?.mapNotNull { it.name } ?: emptyList()
            if (genres.any { w -> own.none { it.equals(w, ignoreCase = true) } }) return false
        }
        return true
    }

    suspend fun fetchUserMangaFavorites(token: String): List<MangaFavorite> {
        val query = """
            query {
                Viewer {
                    favourites {
                        manga {
                            nodes {
                                id
                                idMal
                                title { romaji english }
                                coverImage { extraLarge large }
                                format
                                status
                                startDate { year }
                                chapters
                                averageScore
                                siteUrl
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val raw = executeQuery(query, token = token) ?: return emptyList()
        return try {
            json.decodeFromString<MangaFavoritesResponse>(raw).data.Viewer.favourites?.manga?.nodes ?: emptyList()
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "favorites parse failed", e); emptyList()
        }
    }

    suspend fun fetchUserMangaActivity(userId: Int, token: String): List<MangaActivityNode> {
        val query = """
            query (${'$'}userId: Int) {
                Page(page: 1, perPage: 50) {
                    activities(userId: ${'$'}userId, type: MANGA_LIST, sort: ID_DESC) {
                        ... on ListActivity {
                            id
                            createdAt
                            status
                            progress
                            media {
                                id
                                idMal
                                title { romaji english }
                                coverImage { large }
                                chapters
                                siteUrl
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        val raw = executeQuery(query, mapOf("userId" to userId), token) ?: return emptyList()
        return try {
            json.decodeFromString<MangaActivityResponse>(raw).data.Page.activities
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "activity parse failed", e); emptyList()
        }
    }

    suspend fun fetchMangaUserProfile(token: String): com.blissless.tensei.data.models.MangaUserProfile? {
        val query = """
            query {
                Viewer { id name avatar { medium large } bannerImage siteUrl createdAt statistics { manga { count chaptersRead volumesRead meanScore } } }
            }
        """.trimIndent()
        val raw = executeQuery(query, token = token) ?: return null
        return try {
            json.decodeFromString<MangaUserProfileResponse>(raw).data.User
        } catch (e: Exception) {
            ErrorHandler.ignore(TAG, "user profile parse failed", e); null
        }
    }
    suspend fun updateMangaStatus(mediaId: Int, status: String, token: String, progress: Int? = null, progressVolumes: Int? = null, score: Int? = null): Boolean {
        val query = """
            mutation (${'$'}mediaId: Int, ${'$'}status: MediaListStatus, ${'$'}progress: Int, ${'$'}progressVolumes: Int, ${'$'}score: Float) {
                SaveMediaListEntry(mediaId: ${'$'}mediaId, status: ${'$'}status, progress: ${'$'}progress, progressVolumes: ${'$'}progressVolumes, score: ${'$'}score) { id }
            }
        """.trimIndent()
        val vars = mutableMapOf<String, Any?>("mediaId" to mediaId, "status" to status)
        if (progress != null) vars["progress"] = progress
        if (progressVolumes != null) vars["progressVolumes"] = progressVolumes
        if (score != null) vars["score"] = score
        val raw = executeQuery(query, vars, token, useCache = false)
        // AniList returns HTTP 200 with an "errors" array when the mutation is rejected,
        // so a non-null body alone is not success.
        val ok = raw != null && !raw.contains("\"errors\"")
        if (ok) graphQLClient.clearCache()
        return ok
    }

    suspend fun deleteMangaListEntry(entryId: Int, token: String): Boolean {
        val query = """
            mutation (${'$'}id: Int) { DeleteMediaListEntry(id: ${'$'}id) { deleted } }
        """.trimIndent()
        val raw = executeQuery(query, mapOf("id" to entryId), token, useCache = false)
        val ok = raw != null && !raw.contains("\"errors\"")
        if (ok) graphQLClient.clearCache()
        return ok
    }

    suspend fun toggleMangaFavorite(mediaId: Int, token: String): Boolean {
        val query = """
            mutation (${'$'}mediaId: Int) { ToggleFavourite(mangaId: ${'$'}mediaId) { manga { favourites } } }
        """.trimIndent()
        val raw = executeQuery(query, mapOf("mediaId" to mediaId), token, useCache = false)
        val ok = raw != null && !raw.contains("\"errors\"")
        if (ok) graphQLClient.clearCache()
        return ok
    }

    // ============================================
    // MAL fallbacks (used when AniList is unavailable)
    // ============================================

    private val malMangaClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun malMangaRequest(url: String): okhttp3.Response {
        val request = Request.Builder().url(url)
            .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
            .header("Accept", "application/json")
            .header("User-Agent", "Tensei/1.0")
            .build()
        return malMangaClient.newCall(request).execute()
    }

    /** MAL manga search fallback for the search screen (text queries only). */
    suspend fun searchMangaMalFallback(query: String, page: Int = 1, perPage: Int = 30): List<MangaExploreMedia> = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
            val limit = perPage.coerceIn(1, 100)
            val offset = (page - 1).coerceAtLeast(0) * limit
            val url = Endpoints.Mal.searchMangaUrl(query, limit, offset, fields)
            android.util.Log.d("MangaMal", "search URL: $url")
            fetchMalMangaNodes(url)
        } catch (e: Exception) {
            android.util.Log.e("MangaMal", "search failed: ${e.message}", e)
            emptyList()
        }
    }

    /** MAL manga ranking fallback for the explore screen's non-genre rows. */
    suspend fun fetchMangaExploreFromMal(): Map<String, List<MangaExploreMedia>> = withContext(Dispatchers.IO) {
        val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
        val sections = mutableMapOf<String, List<MangaExploreMedia>>()
        // MAL has no genre ranking, so we mirror the trend/popular/top-ranked rows with
        // the closest ranking types; genre rows are left for AniList. Note: MAL manga
        // has NO "airing" ranking type (that's anime-only) — it returns HTTP 400 — so
        // the trending row uses bypopularity (member count) instead.
        val rankingDefs = listOf(
            "trending" to "bypopularity",
            "popular" to "all",
            "topRated" to "manga",
            "favourites" to "favorite"
        )
        for ((key, rankingType) in rankingDefs) {
            try {
                val url = Endpoints.Mal.rankingMangaUrl(rankingType, 30, 0, fields)
                val nodes = fetchMalMangaNodes(url)
                android.util.Log.d("MangaMal", "explore '$key': ${nodes.size} entries")
                if (nodes.isNotEmpty()) sections[key] = nodes
            } catch (e: Exception) {
                android.util.Log.e("MangaMal", "explore '$key' failed: ${e.message}")
            }
        }
        sections
    }

    /** Detailed manga fallback from the official MAL v2 API (used when AniList is down). */
    suspend fun fetchMangaMalDetail(malId: Int): MangaDetail? = withContext(Dispatchers.IO) {
        try {
            val fields = "id,title,alternative_titles,main_picture,synopsis,background,num_chapters,num_volumes,mean,rank," +
                "popularity,num_list_users,num_scoring_users,status,media_type,start_date,end_date,genres," +
                "related_manga{node{id,title,main_picture,media_type,num_chapters,status,start_date}}," +
                "recommendations{node{id,title,main_picture,media_type,num_chapters,num_volumes,status}}"
            val url = Endpoints.Mal.detailMangaUrl(malId, fields)
            android.util.Log.d("MangaMal", "detail URL: $url")
            val node = malMangaRequest(url).use { response ->
                if (response.code != 200) {
                    android.util.Log.w("MangaMal", "detail HTTP ${response.code} url=$url")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                try {
                    json.decodeFromString<MalMangaNode>(body)
                } catch (e: Exception) {
                    android.util.Log.e("MangaMal", "detail parse error: ${e.message}")
                    null
                }
            }
            node?.let {
                android.util.Log.d("MangaMal", "detail OK id=$malId title=${it.title}")
                it.toMangaDetail()
            }
        } catch (e: Exception) {
            android.util.Log.e("MangaMal", "detail failed: ${e.message}", e)
            null
        }
    }

    private fun fetchMalMangaNodes(url: String, include: (MalMangaNode) -> Boolean = { true }): List<MangaExploreMedia> {
        malMangaRequest(url).use { response ->
            if (response.code != 200) {
                android.util.Log.w("MangaMal", "HTTP ${response.code} url=$url")
                return emptyList()
            }
            val body = response.body?.string() ?: return emptyList()
            val parsed = try {
                json.decodeFromString<MangaMalSearchResponse>(body)
            } catch (e: Exception) {
                android.util.Log.e("MangaMal", "parse error: ${e.message}")
                return emptyList()
            }
            return parsed.data.mapNotNull { node ->
                if (include(node.node)) node.node.toMangaExploreMedia() else null
            }
        }
    }

    private fun MalMangaNode.toMangaExploreMedia(): MangaExploreMedia? {
        if (id <= 0) return null
        val picture = main_picture
        return MangaExploreMedia(
            id = id,
            idMal = id,
            title = com.blissless.tensei.data.models.MangaTitle(romaji = title, english = alternative_titles?.en),
            coverImage = com.blissless.tensei.data.models.MediaCoverImage(
                extraLarge = picture?.large,
                large = picture?.large,
                medium = picture?.medium
            ),
            bannerImage = null,
            chapters = num_chapters,
            status = malMangaStatus(),
            averageScore = malMangaMeanScore(),
            genres = genres?.mapNotNull { it.name },
            seasonYear = start_date?.take(4)?.toIntOrNull(),
            startDate = start_date?.split("-")?.let {
                com.blissless.tensei.data.models.MangaFuzzyDate(year = it.getOrNull(0)?.toIntOrNull(), month = it.getOrNull(1)?.toIntOrNull(), day = it.getOrNull(2)?.toIntOrNull())
            },
            isAdult = nsfw == "black",
            format = malMangaFormat(),
            volumes = num_volumes
        )
    }

    private fun MalMangaNode.toMangaDetail(): MangaDetail = MangaDetail(
        id = id,
        malId = id,
        title = title ?: alternative_titles?.en ?: "Unknown",
        titleEnglish = alternative_titles?.en,
        cover = main_picture?.large ?: main_picture?.medium ?: "",
        description = synopsis ?: background,
        chapters = num_chapters ?: 0,
        volumes = num_volumes,
        status = malMangaStatus(),
        averageScore = malMangaMeanScore(),
        meanScore = malMangaMeanScore(),
        popularity = num_list_users ?: popularity,
        favourites = num_scoring_users ?: rank,
        genres = genres?.mapNotNull { it.name } ?: emptyList(),
        year = start_date?.take(4)?.toIntOrNull(),
        format = malMangaFormat(),
        isAdult = nsfw == "black",
        recommendations = recommendations?.mapNotNull { rec ->
            rec.node?.let { n ->
                MangaMedia(
                    id = n.id,
                    title = n.title ?: n.alternative_titles?.en ?: "",
                    titleEnglish = n.alternative_titles?.en,
                    cover = n.main_picture?.large ?: n.main_picture?.medium ?: "",
                    totalChapters = n.num_chapters ?: 0,
                    totalVolumes = n.num_volumes,
                    status = n.malMangaStatus() ?: "",
                    averageScore = n.malMangaMeanScore()
                )
            }
        } ?: emptyList(),
        relations = related_manga?.mapNotNull { rel ->
            rel.node?.let { n ->
                MangaRelation(
                    id = n.id,
                    title = n.title ?: n.alternative_titles?.en ?: "Unknown",
                    titleRomaji = n.title,
                    cover = n.main_picture?.large ?: n.main_picture?.medium ?: "",
                    chapters = n.num_chapters,
                    averageScore = n.malMangaMeanScore(),
                    format = n.malMangaFormat(),
                    relationType = rel.relation_type?.uppercase() ?: "UNKNOWN"
                )
            }
        } ?: emptyList()
    )

    private fun MalMangaNode.malMangaStatus(): String? = when (status) {
        "currently_publishing" -> "RELEASING"
        "finished" -> "FINISHED"
        "on_hiatus" -> "HIATUS"
        "discontinued" -> "CANCELLED"
        "not_yet_published" -> "NOT_YET_RELEASED"
        else -> status
    }

    private fun MalMangaNode.malMangaMeanScore(): Int? =
        mean?.let { (it * 10).roundToInt() }?.coerceIn(0, 100)

    private fun MalMangaNode.malMangaFormat(): String? = media_type?.let {
        when (it.lowercase()) {
            "manga" -> "MANGA"
            "novel" -> "NOVEL"
            "one_shot" -> "ONE_SHOT"
            "doujinshi" -> "DOUJINSHI"
            "manhwa" -> "MANHWA"
            "manhua" -> "MANHUA"
            "oel" -> "OEL"
            else -> it.uppercase()
        }
    }
}
