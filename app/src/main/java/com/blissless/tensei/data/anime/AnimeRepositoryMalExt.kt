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

// MAL (MyAnimeList) fallback for search/explore/detail when AniList is
// unreachable: Jikan-style search, ranking fallback, detail and explore
// mapping.

suspend fun AnimeRepository.searchAnimeMalFiltered(
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
        val limit = 100
        // MAL's search/ranking API only honors the text query — every other filter
        // (genre/format/status/year) is applied client-side. A single page of 30 raw
        // nodes is usually too shallow (e.g. a genre that is rarely in the top-30
        // ranking slice), so keep paging through the pool until enough nodes match.
        val want = (page).coerceAtLeast(1) * perPage
        val collected = mutableListOf<ExploreMedia>()
        var offset = 0
        var poolPages = 0
        while (collected.size < want && poolPages < 6) {
            val url = if (!query.isNullOrBlank()) {
                Endpoints.Mal.searchAnimeUrl(query, limit, offset, fields)
            } else {
                Endpoints.Mal.rankingAnimeUrl(malAnimeRankingType(format, status), limit, offset, fields)
            }
            val batch = requestMalNodes(url) { node ->
                node.matchesSearchFilters(year, format, status, genres)
            }
            if (batch.isEmpty()) break
            collected.addAll(batch)
            offset += limit
            poolPages++
        }
        collected.drop((page - 1).coerceAtLeast(0) * perPage).take(perPage)
    } catch (e: Exception) {
        Log.e("SearchDebug", "MAL filtered fallback failed: ${e.message}", e)
        emptyList()
    }
}
/** Ranking pool for blank-query filtered anime searches: format/status pick the closest MAL ranking type. */

fun AnimeRepository.malAnimeRankingType(format: String?, status: String?): String = when {
    status == "RELEASING" -> "airing"
    status == "NOT_YET_RELEASED" -> "upcoming"
    format == "MOVIE" -> "movie"
    format == "OVA" -> "ova"
    format == "SPECIAL" -> "special"
    else -> "all"
}

fun MalAnimeNode.matchesSearchFilters(
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

suspend fun AnimeRepository.searchAnimeMalFallback(query: String, page: Int = 1, perPage: Int = 30): List<ExploreMedia> = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
        val limit = perPage.coerceIn(1, 100)
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = Endpoints.Mal.searchAnimeUrl(query, limit, offset, fields)
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

suspend fun AnimeRepository.searchAnimeMalRankingFallback(page: Int = 1, perPage: Int = 30): List<ExploreMedia> = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,num_episodes,mean,start_date,status,nsfw,media_type,genres"
        val limit = perPage.coerceIn(1, 100)
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = Endpoints.Mal.rankingAnimeUrl("all", limit, offset, fields)
        requestMalNodes(url)
    } catch (e: Exception) {
        Log.e("SearchDebug", "MAL ranking fallback failed: ${e.message}", e)
        emptyList()
    }
}
/** Shared MAL request/parse for search & ranking responses (both use data[].node).
 *  Optional [include] predicate lets filtered searches drop nodes that don't match. */

fun AnimeRepository.requestMalNodes(url: String, include: (MalAnimeNode) -> Boolean = { true }): List<ExploreMedia> {
    val request = Request.Builder().url(url)
        .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
        .header("Accept", "application/json")
        .header("User-Agent", "Tensei/1.0")
        .build()
    animeScheduleClient.newCall(request).execute().use { response ->
        val code = response.code
        if (code != 200) {
            Log.w("SearchDebug", "MAL fallback HTTP $code body=${response.body.string().take(300)}")
            return emptyList()
        }
        val body = response.body.string()
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

internal inline fun <reified T> requestMalDetail(url: String): T? {
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
        val body = response.body.string()
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

suspend fun AnimeRepository.fetchDetailedAnimeFromMal(malId: Int): DetailedAnimeData? = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,synopsis,background,num_episodes,mean,rank," +
            "popularity,num_list_users,num_scoring_users,status,media_type,start_date,end_date,source,rating," +
            "studios,genres,related_anime{node{id,title,main_picture,media_type,num_episodes,status,start_date}}," +
            "recommendations{node{id,title,main_picture,media_type,num_episodes,status,start_date}}"
        val url = Endpoints.Mal.detailAnimeUrl(malId, fields)
        val node = requestMalDetail<MalAnimeNode>(url) ?: return@withContext null
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

suspend fun AnimeRepository.fetchAnimeExploreFromMal(): Map<String, List<ExploreMedia>> = withContext(Dispatchers.IO) {
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
            if (nodes.isNotEmpty()) sections[key] = nodes
        } catch (e: Exception) {
            Log.e("SearchDebug", "MAL explore '$key' failed: ${e.message}")
        }
    }
    sections
}

fun MalAnimeNode.toDetailedAnimeData(): DetailedAnimeData = DetailedAnimeData(
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

fun MalAnimeNode.malStatus(): String? = when (status) {
    "currently_airing" -> "RELEASING"
    "finished_airing" -> "FINISHED"
    "not_yet_aired" -> "NOT_YET_RELEASED"
    else -> status
}

fun MalAnimeNode.malMeanScore(): Int? =
    mean?.let { (it * 10).roundToInt() }?.coerceIn(0, 100)

fun MalAnimeNode.malFormat(): String? = media_type?.let {
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

fun MalAnimeNode.toExploreMedia(): ExploreMedia? {
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
