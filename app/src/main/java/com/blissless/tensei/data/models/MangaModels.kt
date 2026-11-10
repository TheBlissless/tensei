package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// Manga domain models and AniList manga wire models (explore, detail,
// chapters, tracking, user lists).



// ============================================
// MANGA MODELS
// ============================================

@Serializable
data class MangaMedia(
    val id: Int,
    val title: String,
    val titleEnglish: String? = null,
    val cover: String,
    val banner: String? = null,
    val progress: Int = 0,
    val totalChapters: Int = 0,
    val totalVolumes: Int? = null,
    val latestChapter: Float? = null,
    val status: String = "",
    val averageScore: Int? = null,
    val genres: List<String> = emptyList(),
    val listStatus: String = "",
    val listEntryId: Int? = null,
    val year: Int? = null,
    val malId: Int? = null,
    val format: String? = null,
    val userScore: Int? = null,
    val siteUrl: String? = null,
    val mangaDexId: String? = null,
    val scrollProgress: Float = 0f,
    val currentChapterPages: Int = 0,
    val scrollChapterId: String? = null,
    val scrollChapterNumber: Float = 0f,
    // Epoch millis of the last list update (provider timestamp or local edit). Used to
    // order the Home manga lists newest-first, like the provider's "Last Updated" sort.
    val updatedAt: Long = 0L
)


@Serializable
data class MangaChapter(
    val url: String,
    val title: String,
    val chapterId: String,
    val volume: Float? = null,
    val chapterNumber: Float = 0f,
    val groups: List<String> = emptyList()
)


@Serializable
data class ChapterImages(
    val chapterUrl: String,
    val images: List<String>
)


@Serializable
data class MangaDetail(
    val id: Int,
    val title: String,
    val titleEnglish: String? = null,
    val titleNative: String? = null,
    val cover: String,
    val banner: String? = null,
    val description: String? = null,
    val chapters: Int = 0,
    val volumes: Int? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val genres: List<String> = emptyList(),
    val tags: List<TagData> = emptyList(),
    val year: Int? = null,
    val endYear: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val format: String? = null,
    val source: String? = null,
    val isAdult: Boolean = false,
    val staff: MangaStaff? = null,
    val recommendations: List<MangaMedia> = emptyList(),
    val malId: Int? = null,
    val relations: List<MangaRelation> = emptyList(),
    val characters: MangaCharacters? = null,
    val siteUrl: String? = null,
    val mangaDexId: String? = null,
    val synonymTitles: List<String> = emptyList(),
    val rankings: List<MangaRankingEntry> = emptyList(),
    val externalLinks: List<MangaExternalLink> = emptyList()
)


@Serializable
data class MangaRelation(
    val id: Int,
    val title: String,
    val titleRomaji: String? = null,
    val cover: String,
    val chapters: Int?,
    val episodes: Int? = null,
    val averageScore: Int?,
    val format: String?,
    val relationType: String
)


@Serializable
data class MangaStaff(
    val edges: List<MangaStaffEdge> = emptyList()
)


@Serializable
data class MangaStaffEdge(
    val node: MangaStaffNode? = null,
    val role: String? = null
)


@Serializable
data class MangaStaffNode(
    val id: Int,
    val name: MangaCharacterName? = null,
    val image: MediaCoverImage? = null
)


@Serializable
data class MangaCharacters(
    val nodes: List<MangaCharacterNode> = emptyList()
)


@Serializable
data class MangaCharacterNode(
    val id: Int,
    val name: MangaCharacterName? = null,
    val image: MediaCoverImage? = null
)


@Serializable
data class MangaCharacterName(
    val full: String? = null,
    val native: String? = null
)


@Serializable
data class MangaExternalLink(
    val url: String? = null,
    val site: String? = null
)


@Serializable
data class MangaRankingEntry(
    val id: Int? = null,
    val rank: Int? = null,
    val type: String? = null,
    val context: String? = null,
    val allTime: Boolean? = null,
    val season: String? = null,
    val year: Int? = null
)


@Serializable
data class MangaTrack(
    val mangaId: Int,
    val title: String = "",
    val titleEnglish: String? = null,
    val cover: String = "",
    val progress: Float = 0f,
    val scrollProgress: Float = 0f,
    val scrollChapterId: String? = null,
    val scrollChapterNumber: Float = 0f,
    val totalChapters: Int = 0,
    val totalVolumes: Int? = null,
    val listEntryId: Int? = null,
    val mediaStatus: String? = null,
    val status: String = "PLANNING",
    val mangaDexId: String? = null,
    val lastReadChapter: MangaChapter? = null,
    val currentChapterPages: Int = 0,
    val malId: Int? = null,
    val score: Int? = null,
    val averageScore: Int? = null,
    // Epoch millis of the last list update; legacy tracks default to 0.
    val updatedAt: Long = 0L
)


@Serializable
data class MangaExploreMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MangaTitle,
    val coverImage: MediaCoverImage? = null,
    val bannerImage: String? = null,
    val chapters: Int? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val genres: List<String>? = null,
    val seasonYear: Int? = null,
    val startDate: MangaFuzzyDate? = null,
    val isAdult: Boolean = false,
    val format: String? = null,
    val volumes: Int? = null
)


@Serializable
data class MangaTitle(
    val romaji: String? = null,
    val english: String? = null
)


@Serializable
data class MangaFuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)


@Serializable
data class MangaExploreResponse(val data: MangaExploreData)


@Serializable
data class MangaExploreData(val Page: MangaExplorePage)


@Serializable
data class MangaExplorePage(val media: List<MangaExploreMedia>)


@Serializable
data class MangaDetailResponse(val data: MangaDetailDataWrapper)


@Serializable
data class MangaDetailDataWrapper(val Media: MangaDetailMedia)


@Serializable
data class MangaDetailMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MangaDetailTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val meanScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val genres: List<String>? = null,
    val tags: List<TagData>? = null,
    val seasonYear: Int? = null,
    val startDate: StartDate? = null,
    val endDate: StartDate? = null,
    val format: String? = null,
    val source: String? = null,
    val isAdult: Boolean = false,
    val relations: MangaDetailRelations? = null,
    val characters: MangaDetailCharacters? = null,
    val staff: MangaDetailStaffWrapper? = null,
    val recommendations: MangaDetailRecommendations? = null,
    val synonyms: List<String>? = null,
    val rankings: List<MangaRankingEntry>? = null,
    val externalLinks: List<MangaExternalLink>? = null
)


@Serializable
data class MangaDetailTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)


@Serializable
data class StartDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)


@Serializable
data class MangaDetailRelations(
    val edges: List<MangaDetailRelationEdge>
)


@Serializable
data class MangaDetailRelationEdge(
    val relationType: String? = null,
    val node: MangaDetailRelationNode? = null
)


@Serializable
data class MangaDetailRelationNode(
    val id: Int,
    val title: MangaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val chapters: Int? = null,
    val episodes: Int? = null,
    val averageScore: Int? = null,
    val format: String? = null
)


@Serializable
data class MangaDetailCharacters(
    val nodes: List<MangaDetailCharacterNode> = emptyList()
)


@Serializable
data class MangaDetailCharacterNode(
    val id: Int,
    val name: MangaCharacterName? = null,
    val image: MediaCoverImage? = null
)


@Serializable
data class MangaDetailStaffWrapper(
    val edges: List<MangaStaffEdge> = emptyList()
)


@Serializable
data class MangaDetailRecommendations(
    val nodes: List<MangaDetailRecommendationNode> = emptyList()
)


@Serializable
data class MangaDetailRecommendationNode(
    val mediaRecommendation: MangaExploreMedia? = null
)


@Serializable
data class MangaUserListResponse(val data: MangaUserListData)


@Serializable
data class MangaUserListData(val MediaListCollection: MangaMediaListCollection)


@Serializable
data class MangaMediaListCollection(val lists: List<MangaMediaList>)


@Serializable
data class MangaMediaList(
    val name: String,
    val status: String?,
    val entries: List<MangaMediaListEntry>
)


@Serializable
data class MangaMediaListEntry(
    val id: Int,
    val mediaId: Int,
    val progress: Int?,
    val progressVolumes: Int?,
    val score: Int?,
    val status: String?,
    // Unix seconds; AniList sorts each list by UPDATED_TIME_DESC when we request it.
    val updatedAt: Long? = null,
    val media: MangaEntryMedia
)


@Serializable
data class MangaEntryMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MangaTitle,
    val coverImage: MediaCoverImage?,
    val bannerImage: String?,
    val chapters: Int?,
    val volumes: Int?,
    val status: String?,
    val averageScore: Int?,
    val genres: List<String>?,
    val seasonYear: Int? = null,
    val startDate: StartDate? = null,
    val format: String? = null
)


@Serializable
data class MangaUserProfileResponse(val data: MangaUserProfileData)


@Serializable
data class MangaUserProfileData(val User: MangaUserProfile? = null)


@Serializable
data class MangaUserProfile(
    val id: Int,
    val name: String,
    val avatar: Avatar? = null,
    val bannerImage: String? = null,
    val siteUrl: String? = null,
    val createdAt: Long? = null,
    val statistics: MangaUserStatistics? = null
)


@Serializable
data class MangaUserStatistics(
    val manga: MangaUserMangaStats? = null
)


@Serializable
data class MangaUserMangaStats(
    val count: Int = 0,
    val chaptersRead: Int = 0,
    val volumesRead: Int = 0,
    val meanScore: Double? = null
)
