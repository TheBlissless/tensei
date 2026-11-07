package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────────────────
// Anime domain models — the app-facing representation of anime entries,
// used by repositories, ViewModels and UI. Wire-format DTOs live in
// AniListDtos.kt / MalDtos.kt / TmdbModels.kt.
// ─────────────────────────────────────────────────────────────────────────────


@Serializable
data class ExploreAnime(
    val id: Int,
    val title: String,
    val titleEnglish: String? = null,
    val cover: String,
    val banner: String?,
    val episodes: Int,
    val latestEpisode: Int?,
    val averageScore: Int?,
    val genres: List<String>,
    val year: Int? = null,
    val malId: Int? = null,
    val format: String? = null,
    val isAdult: Boolean = false,
    val status: String? = null
)


@Serializable
data class AnimeMedia(
    val id: Int,
    val title: String,
    val titleEnglish: String? = null,
    val cover: String,
    val banner: String? = null,
    val progress: Int = 0,
    val totalEpisodes: Int = 0,
    val latestEpisode: Int? = null,
    val status: String = "",
    val averageScore: Int? = null,
    val genres: List<String> = emptyList(),
    val listStatus: String = "",
    val listEntryId: Int? = null,
    val year: Int? = null,
    val malId: Int? = null,
    val format: String? = null,
    val userScore: Int? = null,
    val siteUrl: String? = null
)


@Serializable
data class AiringScheduleAnime(
    val id: Int,
    val title: String,
    val titleEnglish: String? = null,
    val cover: String,
    val episodes: Int = 0,
    val airingEpisode: Int = 0,
    val airingAt: Long = 0,
    val timeUntilAiring: Long? = null,
    val averageScore: Int? = null,
    val genres: List<String> = emptyList(),
    val year: Int? = null,
    val malId: Int? = null,
    val isAdult: Boolean = false
)


@Serializable
data class UserActivity(
    val id: Int,
    val type: String,
    val status: String,
    val progress: String?,
    val createdAt: Long,
    val mediaId: Int,
    val mediaTitle: String,
    val mediaTitleEnglish: String? = null,
    val mediaCover: String,
    val episodes: Int?,
    val averageScore: Int?,
    val year: Int? = null,
    val mediaIdMal: Int? = null
)


// ============================================
// DETAILED ANIME DATA
// ============================================

@Serializable
data class DetailedAnimeData(
    val id: Int,
    val title: String,
    val titleRomaji: String? = null,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val cover: String,
    val banner: String? = null,
    val description: String? = null,
    val episodes: Int = 0,
    val duration: Int? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val tags: List<TagData> = emptyList(),
    val season: String? = null,
    val year: Int? = null,
    val format: String? = null,
    val source: String? = null,
    val studios: List<StudioData> = emptyList(),
    val startDate: String? = null,
    val endDate: String? = null,
    val nextAiringEpisode: Int? = null,
    val nextAiringTime: Long? = null,
    val isAdult: Boolean = false,
    val trailerUrl: String? = null,
    val trailerThumbnail: String? = null,
    val staff: DetailedAnimeStaff? = null,
    val recommendations: List<ExploreAnime> = emptyList(),
    val latestEpisode: Int? = null,
    val malId: Int? = null,
    val relations: List<AnimeRelation> = emptyList(),
    val characters: DetailedAnimeCharacters? = null,
    val siteUrl: String? = null
)


@Serializable
data class TagData(
    val name: String,
    val rank: Int? = null,
    val isMediaSpoiler: Boolean = false,
    val description: String? = null,
    val isAdult: Boolean = false
)


@Serializable
data class StudioData(
    val id: Int,
    val name: String,
    val isAnimationStudio: Boolean = true
)


@Serializable
data class AnimeRelation(
    val id: Int,
    val title: String,
    val titleRomaji: String? = null,
    val cover: String,
    val episodes: Int?,
    val latestEpisode: Int? = null,
    val averageScore: Int?,
    val format: String?,
    val relationType: String
)


// ============================================
// EXTENSION FUNCTIONS
// ============================================

fun ExploreAnime.toDetailedAnimeData(): DetailedAnimeData {
    return DetailedAnimeData(
        id = id,
        title = title,
        titleRomaji = title,
        titleEnglish = titleEnglish,
        cover = cover,
        banner = banner,
        episodes = episodes,
        averageScore = averageScore,
        genres = genres,
        year = year,
        latestEpisode = latestEpisode,
        malId = malId,
        siteUrl = com.blissless.tensei.network.Endpoints.AniList.animePageUrl(id)
    )
}


fun AnimeMedia.toDetailedAnimeData(): DetailedAnimeData {
    return DetailedAnimeData(
        id = id,
        title = title,
        titleRomaji = title,
        titleEnglish = titleEnglish,
        cover = cover,
        banner = banner,
        episodes = totalEpisodes,
        averageScore = averageScore,
        genres = genres,
        year = year,
        latestEpisode = latestEpisode,
        malId = malId,
        siteUrl = siteUrl ?: com.blissless.tensei.network.Endpoints.AniList.animePageUrl(id)
    )
}


// ============================================
// LOCAL FAVORITES
// ============================================

@Serializable
data class StoredFavorite(
    val id: Int,
    val title: String,
    val cover: String,
    val banner: String? = null,
    val year: Int? = null,
    val averageScore: Int? = null,
    // English title — stored alongside `title` (which is the romaji title from AniList) so
    // the "Prefer English titles" setting can be honored for local favorites when the user
    // is logged out. null for legacy favorites persisted before this field existed; the
    // dialog falls back to `title` in that case.
    val titleEnglish: String? = null
)


@Serializable
data class LocalAnimeEntry(
    val id: Int,
    val status: String,
    val progress: Int = 0,
    val totalEpisodes: Int = 0,
    val title: String = "",
    val cover: String = "",
    val banner: String? = null,
    val year: Int? = null,
    val averageScore: Int? = null,
    val score: Int? = null
)
