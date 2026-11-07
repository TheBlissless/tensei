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
import com.blissless.tensei.data.cache.invalidateUserCache

// AniList mutations (progress/status/score/delete) and user activity +

suspend fun AnimeRepository.updateProgress(mediaId: Int, progress: Int): Boolean {
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

suspend fun AnimeRepository.updateStatus(mediaId: Int, status: String, progress: Int? = null, score: Int? = null): Boolean {
    cacheManager.invalidateUserCache()
    graphQLClient.clearCache()
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

suspend fun AnimeRepository.deleteListEntry(entryId: Int): Boolean {
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

suspend fun AnimeRepository.updateScore(mediaId: Int, score: Int): Boolean {
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

suspend fun AnimeRepository.fetchUserActivity(userId: Int, perPage: Int = 50): List<UserActivity>? {
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

suspend fun AnimeRepository.fetchUserFavorites(userId: Int): UserFavoritesResponse? {
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

suspend fun AnimeRepository.toggleAniListFavorite(mediaId: Int): Boolean {
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

suspend fun AnimeRepository.addAniListFavorite(mediaId: Int): Boolean {
    val mutation = $$"""
        mutation ($mediaId: Int) {
            ToggleFavourite(animeId: $mediaId) {
                anime { nodes { id } }
            }
        }
    """.trimIndent()
    // Check if already favorited first. CRITICAL: this check must NOT use the cached
    // graphqlRequest path (which caches user data for 1 hour). A cached isFavourite:false
    // from a previous check would make the early return fire and skip the mutation — so
    // a user who favorited then immediately unfavorited would have the un-favorite silently
    // dropped (the cached response still said isFavourite:false from before the first toggle).
    // The check is the same authed query but with useCache = false to always hit AniList.
    val checkQuery = $$"""
        query ($mediaId: Int) {
            Media(id: $mediaId) {
                id
                isFavourite
            }
        }
    """.trimIndent()
    val token = userPreferences.authToken.value
    val checkResult = if (token != null) {
        graphQLClient.execute(
            query = checkQuery,
            variables = mapOf("mediaId" to mediaId),
            requiresAuth = true,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = false, // Bypass cache — isFavourite is per-user state that changes
            parser = { it }
        ).data
    } else null
    if (checkResult?.contains("\"isFavourite\":true") == true || checkResult?.contains("\"isFavourite\": true") == true) {
        return true // Already favorited
    }
    val success = graphqlMutation(mutation, mapOf("mediaId" to mediaId)) != null
    return success
}

suspend fun AnimeRepository.removeAniListFavorite(mediaId: Int): Boolean {
    val mutation = $$"""
        mutation ($mediaId: Int) {
            ToggleFavourite(animeId: $mediaId) {
                anime { nodes { id } }
            }
        }
    """.trimIndent()
    // Check if not favorited first. CRITICAL: this check must NOT use the cached
    // graphqlRequest path (which caches user data for 1 hour). A cached isFavourite:false
    // from a previous check would make the early return fire and skip the mutation — so
    // a user who favorited then immediately unfavorited would have the un-favorite silently
    // dropped (the cached response still said isFavourite:false from before the first toggle,
    // so the code would think "already not favorited" and skip the actual removeAniListFavorite
    // mutation, leaving the favorite stuck on the user's AniList account).
    // The check is the same authed query but with useCache = false to always hit AniList.
    val checkQuery = $$"""
        query ($mediaId: Int) {
            Media(id: $mediaId) {
                id
                isFavourite
            }
        }
    """.trimIndent()
    val token = userPreferences.authToken.value
    val checkResult = if (token != null) {
        graphQLClient.execute(
            query = checkQuery,
            variables = mapOf("mediaId" to mediaId),
            requiresAuth = true,
            authToken = token,
            clientIds = CLIENT_IDS,
            useCache = false, // Bypass cache — isFavourite is per-user state that changes
            parser = { it }
        ).data
    } else null
    if (checkResult?.contains("\"isFavourite\":false") == true || checkResult?.contains("\"isFavourite\": false") == true) {
        return true // Already not favorited
    }
    val success = graphqlMutation(mutation, mapOf("mediaId" to mediaId)) != null
    return success
}

suspend fun AnimeRepository.toggleAniListFavorite(mediaId: Int, addFavorite: Boolean): Boolean {
    if (addFavorite) {
        // Check if already favorited. CRITICAL: bypass the cache (same bug as
        // addAniListFavorite / removeAniListFavorite — the isFavourite field is per-user
        // state that changes, so the cached graphqlRequest path can return a stale
        // isFavourite:false from before a previous toggle, making the early return fire
        // and skip the mutation entirely).
        val checkQuery = $$"""
            query ($mediaId: Int) {
                Media(id: $mediaId) {
                    id
                    isFavourite
                }
            }
        """.trimIndent()
        val token = userPreferences.authToken.value
        val checkResult = if (token != null) {
            graphQLClient.execute(
                query = checkQuery,
                variables = mapOf("mediaId" to mediaId),
                requiresAuth = true,
                authToken = token,
                clientIds = CLIENT_IDS,
                useCache = false, // Bypass cache — isFavourite is per-user state that changes
                parser = { it }
            ).data
        } else null
        if (checkResult?.contains("\"isFavourite\":true") == true || checkResult?.contains("\"isFavourite\": true") == true) {
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
