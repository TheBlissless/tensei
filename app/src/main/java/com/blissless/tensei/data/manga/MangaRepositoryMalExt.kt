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
import com.blissless.tensei.data.models.StartDate
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

// MAL (MyAnimeList) fallback for manga search/explore/detail when AniList
// is unreachable.

suspend fun MangaRepository.searchMangaMalFiltered(
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

suspend fun MangaRepository.searchMangaMalRankingFallback(page: Int = 1, perPage: Int = 30): List<MangaExploreMedia> = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
        val limit = perPage.coerceIn(1, 100)
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = Endpoints.Mal.rankingMangaUrl("all", limit, offset, fields)
        fetchMalMangaNodes(url)
    } catch (e: Exception) {
        android.util.Log.e("MangaMal", "ranking fallback failed: ${e.message}", e)
        emptyList()
    }
}
/** Ranking pool for blank-query filtered manga searches: format picks the closest MAL ranking type. */

fun MangaRepository.malMangaRankingType(format: String?, status: String?): String = when {
    format == "NOVEL" -> "novels"
    format == "ONE_SHOT" -> "oneshots"
    format == "DOUJINSHI" -> "doujin"
    format == "MANHWA" -> "manhwa"
    format == "MANHUA" -> "manhua"
    format == "MANGA" -> "manga"
    else -> "all"
}

fun MalMangaNode.matchesSearchFilters(
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

fun MangaRepository.malMangaRequest(url: String): okhttp3.Response {
    val request = Request.Builder().url(url)
        .header("X-MAL-CLIENT-ID", BuildConfig.MAL_CLIENT_ID)
        .header("Accept", "application/json")
        .header("User-Agent", "Tensei/1.0")
        .build()
    return malMangaClient.newCall(request).execute()
}
/** MAL manga search fallback for the search screen (text queries only). */

suspend fun MangaRepository.searchMangaMalFallback(query: String, page: Int = 1, perPage: Int = 30): List<MangaExploreMedia> = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,num_chapters,num_volumes,mean,start_date,status,nsfw,media_type,genres"
        val limit = perPage.coerceIn(1, 100)
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = Endpoints.Mal.searchMangaUrl(query, limit, offset, fields)
        fetchMalMangaNodes(url)
    } catch (e: Exception) {
        android.util.Log.e("MangaMal", "search failed: ${e.message}", e)
        emptyList()
    }
}
/** MAL manga ranking fallback for the explore screen's non-genre rows. */

suspend fun MangaRepository.fetchMangaExploreFromMal(): Map<String, List<MangaExploreMedia>> = withContext(Dispatchers.IO) {
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
            if (nodes.isNotEmpty()) sections[key] = nodes
        } catch (e: Exception) {
            android.util.Log.e("MangaMal", "explore '$key' failed: ${e.message}")
        }
    }
    sections
}
/** Detailed manga fallback from the official MAL v2 API (used when AniList is down). */

suspend fun MangaRepository.fetchMangaMalDetail(malId: Int): MangaDetail? = withContext(Dispatchers.IO) {
    try {
        val fields = "id,title,alternative_titles,main_picture,synopsis,background,num_chapters,num_volumes,mean,rank," +
            "popularity,num_list_users,num_scoring_users,status,media_type,start_date,end_date,genres," +
            "related_manga{node{id,title,main_picture,media_type,num_chapters,status,start_date}}," +
            "recommendations{node{id,title,main_picture,media_type,num_chapters,num_volumes,status}}"
        val url = Endpoints.Mal.detailMangaUrl(malId, fields)
        val node = malMangaRequest(url).use { response ->
            if (response.code != 200) {
                android.util.Log.w("MangaMal", "detail HTTP ${response.code} url=$url")
                return@withContext null
            }
            val body = response.body.string()
            try {
                json.decodeFromString<MalMangaNode>(body)
            } catch (e: Exception) {
                android.util.Log.e("MangaMal", "detail parse error: ${e.message}")
                null
            }
        }
        node?.let {
            it.toMangaDetail()
        }
    } catch (e: Exception) {
        android.util.Log.e("MangaMal", "detail failed: ${e.message}", e)
        null
    }
}

fun MangaRepository.fetchMalMangaNodes(url: String, include: (MalMangaNode) -> Boolean = { true }): List<MangaExploreMedia> {
    malMangaRequest(url).use { response ->
        if (response.code != 200) {
            android.util.Log.w("MangaMal", "HTTP ${response.code} url=$url")
            return emptyList()
        }
        val body = response.body.string()
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

fun MalMangaNode.toMangaExploreMedia(): MangaExploreMedia? {
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

fun MalMangaNode.toMangaDetail(): MangaDetail = MangaDetail(
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
    endYear = end_date?.take(4)?.toIntOrNull(),
    startDate = start_date,
    endDate = end_date,
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

fun MalMangaNode.malMangaStatus(): String? = when (status) {
    "currently_publishing" -> "RELEASING"
    "finished" -> "FINISHED"
    "on_hiatus" -> "HIATUS"
    "discontinued" -> "CANCELLED"
    "not_yet_published" -> "NOT_YET_RELEASED"
    else -> status
}

fun MalMangaNode.malMangaMeanScore(): Int? =
    mean?.let { (it * 10).roundToInt() }?.coerceIn(0, 100)

fun MalMangaNode.malMangaFormat(): String? = media_type?.let {
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
