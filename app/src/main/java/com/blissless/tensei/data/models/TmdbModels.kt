package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// TMDB wire models — TMDB supplies episode titles and season structure
// for the player (see data/anime/AnimeRepositoryTmdbExt.kt).



@Serializable
data class TmdbEpisode(
    val episode: Int,
    val title: String,
    val description: String,
    val image: String?
)


// ============================================
// TMDB API RESPONSE MODELS
// ============================================

@Serializable
data class TmdbSearchResponse(
    val page: Int = 0,
    val results: List<TmdbSearchResult> = emptyList(),
    val total_pages: Int = 0,
    val total_results: Int = 0
)


@Serializable
data class TmdbSearchResult(
    val id: Int = 0,
    val name: String? = null,
    val title: String? = null,
    val original_name: String? = null,
    val original_title: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val first_air_date: String? = null,
    val release_date: String? = null,
    val vote_average: Double? = null,
    val popularity: Double? = null,
    val original_language: String? = null,
    val origin_country: List<String>? = null,
    val genre_ids: List<Int> = emptyList()
)


@Serializable
data class TmdbTvDetails(
    val id: Int = 0,
    val name: String? = null,
    val original_name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val first_air_date: String? = null,
    val last_air_date: String? = null,
    val number_of_seasons: Int = 0,
    val number_of_episodes: Int = 0,
    val vote_average: Double? = null,
    val popularity: Double? = null,
    val status: String? = null,
    val original_language: String? = null,
    val origin_country: List<String>? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList()
)


@Serializable
data class TmdbGenre(
    val id: Int = 0,
    val name: String? = null
)


@Serializable
data class TmdbSeason(
    val id: Int = 0,
    val season_number: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val air_date: String? = null,
    val episode_count: Int = 0
)


@Serializable
data class TmdbSeasonDetails(
    val id: Int = 0,
    val season_number: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val poster_path: String? = null,
    val air_date: String? = null,
    val episodes: List<TmdbEpisodeDetails> = emptyList()
)


@Serializable
data class TmdbEpisodeDetails(
    val id: Int = 0,
    val episode_number: Int = 0,
    val season_number: Int = 0,
    val name: String? = null,
    val overview: String? = null,
    val still_path: String? = null,
    val air_date: String? = null,
    val runtime: Int? = null,
    val vote_average: Double? = null
)
