package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// MangaDex API wire models — MangaDex supplies the built-in chapter list
// and page images (see data/manga/MangaDexManager.kt).



// MangaDex models
@Serializable
data class MangaDexAggregate(
    val result: String? = null,
    val volumes: Map<String, MangaDexVolume>? = null
)


@Serializable
data class MangaDexVolume(
    val volume: String? = null,
    val count: Int = 0,
    val chapters: Map<String, MangaDexVolumeChapter>? = null
)


@Serializable
data class MangaDexVolumeChapter(
    val chapter: String? = null,
    val id: String? = null,
    val others: List<String>? = null,
    val count: Int = 0
)


@Serializable
data class MangaDexAtHome(
    val result: String? = null,
    val baseUrl: String? = null,
    val chapter: MangaDexAtHomeChapter? = null
)


@Serializable
data class MangaDexAtHomeChapter(
    val hash: String? = null,
    val data: List<String>? = null,
    val dataSaver: List<String>? = null
)


@Serializable
data class MangaDexMangaResponse(
    val result: String? = null,
    val response: String? = null,
    val data: List<MangaDexMangaData>? = null
)


@Serializable
data class MangaDexMangaData(
    val id: String? = null,
    val type: String? = null,
    val attributes: MangaDexMangaAttributes? = null
)


@Serializable
data class MangaDexMangaAttributes(
    val title: MangaDexLocalizedString? = null,
    val altTitles: List<MangaDexLocalizedString>? = null,
    val links: MangaDexLinks? = null
)


@Serializable
data class MangaDexLocalizedString(
    val en: String? = null,
    val ja: String? = null
)


@Serializable
data class MangaDexLinks(
    val al: String? = null
)
