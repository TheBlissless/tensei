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

// Character and staff data (single entries and full cast/staff lists).

suspend fun AnimeRepository.fetchCharacter(characterId: Int): CharacterData? {
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
                        episodes
                        averageScore
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

suspend fun AnimeRepository.fetchStaff(staffId: Int): StaffData? {
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
                            episodes
                            averageScore
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

suspend fun AnimeRepository.fetchAllCharacters(animeId: Int): List<CharacterData>? {
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
/**
 * Full staff list for one anime, one entry per person per job.
 *
 * This asks for `edges { role }` rather than `nodes { primaryOccupations }`:
 * `primaryOccupations` is a person's global occupation ("Story & Art"), not
 * their job on this anime, so the "All staff" grid could only ever show the
 * same generic job. Edges carry the per-title role, and the UI groups the
 * repeated nodes back into one card per person.
 */

suspend fun AnimeRepository.fetchAllStaff(animeId: Int): List<DetailedAnimeStaffEdge>? {
    val query = $$"""
        query ($id: Int!) {
            Media(id: $id, type: ANIME) {
                staff(perPage: 50) {
                    edges {
                        node {
                            id
                            name { full native }
                            image { large }
                        }
                        role
                    }
                }
            }
        }
    """.trimIndent()
    val response = publicGraphqlRequest(query, mapOf("id" to animeId))
    return response?.let { resp ->
        try {
            val data = json.decodeFromString<AllStaffResponse>(resp)
            val staff = data.data.Media?.staff?.edges
            staff
        } catch (_: Exception) {
            null
        }
    }
}
