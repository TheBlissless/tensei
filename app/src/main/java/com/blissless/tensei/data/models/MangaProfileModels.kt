package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// AniList manga profile wire models (favorites and activity feed for the
// user profile screen).




// ─── Manga Favorites & Activity (Profile) ────────────────────────────

@Serializable
data class MangaFavorite(
    val id: Int,
    val title: MangaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val format: String? = null,
    val status: String? = null,
    val startDate: MangaFuzzyDate? = null,
    val chapters: Int? = null,
    val averageScore: Int? = null,
    val siteUrl: String? = null,
    val userScore: Int? = null,
    val idMal: Int? = null
)


@Serializable
data class MangaFavoritesResponse(val data: MangaFavoritesData)


@Serializable
data class MangaFavoritesData(val Viewer: MangaFavoritesViewer)


@Serializable
data class MangaFavoritesViewer(val favourites: MangaFavoritesFavourites? = null)


@Serializable
data class MangaFavoritesFavourites(val manga: MangaFavoritesPage? = null)


@Serializable
data class MangaFavoritesPage(val nodes: List<MangaFavorite> = emptyList())


@Serializable
data class MangaActivity(
    val id: Int,
    val createdAt: Long,
    val status: String? = null,
    val progress: String? = null,
    val media: MangaActivityMedia? = null
)


@Serializable
data class MangaActivityMedia(
    val id: Int,
    val title: MangaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val chapters: Int? = null,
    val siteUrl: String? = null,
    val idMal: Int? = null
)


@Serializable
data class MangaActivityResponse(val data: MangaActivityData)


@Serializable
data class MangaActivityData(val Page: MangaActivityPage)


@Serializable
data class MangaActivityPage(
    val activities: List<MangaActivityNode> = emptyList()
)


@Serializable
data class MangaActivityNode(
    val id: Int = 0,
    val createdAt: Long = 0,
    val status: String? = null,
    val progress: String? = null,
    val media: MangaActivityMedia? = null
)
