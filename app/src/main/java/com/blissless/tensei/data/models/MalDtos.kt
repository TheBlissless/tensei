package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// MyAnimeList Jikan-style wire models used as the AniList fallback for
// search/explore when AniList is unreachable (the MAL v2 API client lives
// in mal/MalApiService.kt and has its own models).



@Serializable
data class MalSearchResponse(val data: List<MalSearchNode>)


@Serializable
data class MalSearchNode(val node: MalAnimeNode)


@Serializable
data class MalAnimeNode(
    val id: Int = 0,
    val title: String? = null,
    val alternative_titles: MalAlternativeTitles? = null,
    val main_picture: MalPicture? = null,
    val num_episodes: Int? = null,
    val mean: Double? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = null,
    val nsfw: String? = null,
    val media_type: String? = null,
    val genres: List<MalGenre>? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val num_list_users: Int? = null,
    val num_scoring_users: Int? = null,
    val source: String? = null,
    val rating: String? = null,
    val studios: List<MalStudio>? = null,
    val related_anime: List<MalRelatedAnime>? = null,
    val recommendations: List<MalRecommendationAnime>? = null
)


@Serializable
data class MalStudio(val id: Int? = null, val name: String? = null)


@Serializable
data class MalRelatedAnime(val node: MalAnimeNode? = null, val relation_type: String? = null)


@Serializable
data class MalRecommendationAnime(val node: MalAnimeNode? = null, val num_recommendations: Int? = null)


@Serializable
data class MalMangaNode(
    val id: Int = 0,
    val title: String? = null,
    val alternative_titles: MalAlternativeTitles? = null,
    val main_picture: MalPicture? = null,
    val num_chapters: Int? = null,
    val num_volumes: Int? = null,
    val mean: Double? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val status: String? = null,
    val nsfw: String? = null,
    val media_type: String? = null,
    val genres: List<MalGenre>? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val num_list_users: Int? = null,
    val num_scoring_users: Int? = null,
    val related_manga: List<MalRelatedManga>? = null,
    val recommendations: List<MalRecommendationManga>? = null
)


@Serializable
data class MalRelatedManga(val node: MalMangaNode? = null, val relation_type: String? = null)


@Serializable
data class MalRecommendationManga(val node: MalMangaNode? = null, val num_recommendations: Int? = null)


@Serializable
data class MangaMalSearchResponse(val data: List<MangaMalSearchNode>)


@Serializable
data class MangaMalSearchNode(val node: MalMangaNode)


@Serializable
data class MalAlternativeTitles(val en: String? = null)


@Serializable
data class MalPicture(val medium: String? = null, val large: String? = null)


@Serializable
data class MalGenre(val id: Int? = null, val name: String? = null)
