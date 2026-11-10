package com.blissless.tensei.data.models

import kotlinx.serialization.Serializable

// AniList GraphQL wire models (users, lists, explore, airing schedule,
// favorites, activity, detail/relations/characters/staff, tags).



// ============================================
// STREAM FETCH RESULT
// ============================================

// ============================================
// API RESPONSE MODELS (AniList)
// ============================================

@Serializable
data class ViewerResponse(val data: ViewerData)


@Serializable
data class ViewerData(val Viewer: Viewer)


@Serializable
data class Viewer(
    val id: Int,
    val name: String,
    val about: String? = null,
    val avatar: Avatar? = null,
    val bannerImage: String? = null,
    val siteUrl: String? = null,
    val createdAt: Long? = null,
    val statistics: ViewerStatistics? = null
)


@Serializable
data class ViewerStatistics(
    val anime: UserAnimeStats? = null
)


@Serializable
data class Avatar(val medium: String, val large: String? = null)


@Serializable
data class MediaListResponse(val data: MediaListData)


@Serializable
data class MediaListData(val MediaListCollection: MediaListCollection)


@Serializable
data class MediaListCollection(val lists: List<MediaList>)


@Serializable
data class MediaList(
    val name: String,
    val status: String?,
    val entries: List<MediaListEntry>
)


@Serializable
data class MediaListEntry(
    val id: Int,
    val mediaId: Int,
    val progress: Int?,
    val status: String?,
    val score: Int? = null,
    // Unix seconds; AniList sorts each list by UPDATED_TIME_DESC when we request it.
    val updatedAt: Long? = null,
    val media: MediaEntryMedia
)


@Serializable
data class MediaEntryMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MediaTitle,
    val coverImage: MediaCoverImage?,
    val bannerImage: String?,
    val episodes: Int?,
    val nextAiringEpisode: NextAiringEpisode?,
    val status: String?,
    val averageScore: Int?,
    val genres: List<String>?,
    val seasonYear: Int? = null
)


@Serializable
data class ExploreResponse(val data: ExploreData)


@Serializable
data class ExploreData(val Page: ExplorePage)


@Serializable
data class ExplorePage(val media: List<ExploreMedia>)


@Serializable
data class ExploreMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MediaTitle,
    val coverImage: MediaCoverImage?,
    val bannerImage: String?,
    val episodes: Int?,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val status: String?,
    val averageScore: Int?,
    val genres: List<String>?,
    val tags: List<TagData>? = null,
    val seasonYear: Int? = null,
    val startDate: FuzzyDate? = null,
    val isAdult: Boolean = false,
    val format: String? = null
)


@Serializable
data class BatchedExploreResponse(val data: BatchedExploreData)


@Serializable
data class BatchedExploreData(
    val featured: ExplorePage,
    val seasonal: ExplorePage,
    val topSeries: ExplorePage,
    val topMovies: ExplorePage,
    val action: ExplorePage,
    val romance: ExplorePage,
    val comedy: ExplorePage,
    val fantasy: ExplorePage,
    val scifi: ExplorePage
)


@Serializable
data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null
)


@Serializable
data class MediaTitle(
    val romaji: String?,
    val english: String?
)


@Serializable
data class MediaCoverImage(
    val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null
)


@Serializable
data class NextAiringEpisode(
    val episode: Int? = null,
    val airingAt: Long? = null,
    val timeUntilAiring: Long? = null
)


@Serializable
data class AiringScheduleResponse(val data: AiringScheduleData)


@Serializable
data class AiringScheduleData(val Page: AiringSchedulePage)


@Serializable
data class AiringSchedulePage(val airingSchedules: List<AiringScheduleEntry>)


@Serializable
data class AiringScheduleEntry(
    val id: Int,
    val airingAt: Long,
    val episode: Int,
    val timeUntilAiring: Long? = null,
    val mediaId: Int,
    val media: AiringScheduleMedia?
)


@Serializable
data class AiringScheduleMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: MediaTitle,
    val coverImage: MediaCoverImage?,
    val episodes: Int?,
    val status: String?,
    val averageScore: Int?,
    val genres: List<String>?,
    val tags: List<TagData>? = null,
    val seasonYear: Int? = null,
    val isAdult: Boolean = false
)


@Serializable
data class AnimeScheduleTimetableEntry(
    val title: String? = null,
    val route: String = "",
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null,
    val status: String? = null,
    val episodeDate: String? = null,
    val episodeNumber: Int? = null,
    val episodes: Int? = null,
    val lengthMin: Int? = null,
    val donghua: Boolean = false,
    val airType: String? = null,
    val airingStatus: String? = null,
    val imageVersionRoute: String? = null,
    val delayedFrom: String? = null,
    val delayedUntil: String? = null
)


@Serializable
data class UserFavoritesResponse(val data: UserFavoritesData)


@Serializable
data class UserFavoritesData(val User: UserFavoritesUser)


@Serializable
data class UserFavoritesUser(val favourites: UserFavorites)


@Serializable
data class UserFavorites(val anime: UserFavoritesAnime)


@Serializable
data class UserFavoritesAnime(val nodes: List<UserFavoriteAnime>)


@Serializable
data class UserFavoriteAnime(
    val id: Int,
    val title: MediaTitle,
    val coverImage: MediaCoverImage?,
    val episodes: Int?,
    val averageScore: Int?,
    val genres: List<String>?,
    val seasonYear: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val userScore: Int? = null,
    val idMal: Int? = null
)


@Serializable
data class UserStatsResponse(val data: UserStatsData)


@Serializable
data class UserStatsData(val User: UserStats)


@Serializable
data class UserStats(val statistics: UserStatistics)


@Serializable
data class UserStatistics(val anime: UserAnimeStats)


@Serializable
data class UserAnimeStats(
    val count: Int = 0,
    val episodesWatched: Int = 0,
    val minutesWatched: Int = 0,
    val meanScore: Double? = null
)


@Serializable
data class SimpleActivityResponse(val data: SimpleActivityData)


@Serializable
data class SimpleActivityData(val Page: SimpleActivityPage)


@Serializable
data class SimpleActivityPage(val activities: List<SimpleActivityEntry>)


@Serializable
data class SimpleActivityEntry(
    val createdAt: Long,
    val status: String?,
    val progress: String?,
    val media: SimpleActivityMedia?
)


@Serializable
data class SimpleActivityMedia(
    val id: Int,
    val title: SimpleActivityTitle,
    val coverImage: MediaCoverImage?,
    val idMal: Int? = null
)


@Serializable
data class SimpleActivityTitle(
    val romaji: String?,
    val english: String?
)


@Serializable
data class DetailedAnimeResponse(val data: DetailedAnimeDataWrapper)


@Serializable
data class DetailedAnimeDataWrapper(val Media: DetailedAnimeMedia)


@Serializable
data class DetailedAnimeMedia(
    val id: Int,
    val idMal: Int? = null,
    val title: DetailedAnimeTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val bannerImage: String? = null,
    val description: String? = null,
    val episodes: Int? = null,
    val duration: Int? = null,
    val status: String? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val favourites: Int? = null,
    val genres: List<String>? = null,
    val tags: List<TagData>? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val format: String? = null,
    val source: String? = null,
    val studios: DetailedAnimeStudios? = null,
    val startDate: FuzzyDate? = null,
    val endDate: FuzzyDate? = null,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val relations: AnimeRelations? = null,
    val isAdult: Boolean = false,
    val characters: DetailedAnimeCharacters? = null,
    val trailer: MediaTrailer? = null,
    val staff: DetailedAnimeStaff? = null,
    val recommendations: DetailedAnimeRecommendations? = null
)


@Serializable
data class DetailedAnimeRecommendations(
    val nodes: List<DetailedAnimeRecommendationNode> = emptyList()
)


@Serializable
data class DetailedAnimeRecommendationNode(
    val mediaRecommendation: DetailedAnimeMedia? = null
)


@Serializable
data class DetailedAnimeTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)


@Serializable
data class DetailedAnimeStudios(
    val nodes: List<DetailedAnimeStudioNode> = emptyList()
)


@Serializable
data class DetailedAnimeStudioNode(
    val id: Int? = null,
    val name: String? = null
)


// ============================================
// ANIME RELATIONS
// ============================================

@Serializable
data class AnimeRelationsResponse(val data: AnimeRelationsData)


@Serializable
data class AnimeRelationsData(val Media: AnimeRelationsMedia)


@Serializable
data class AnimeRelationsMedia(
    val id: Int,
    val title: MediaTitle? = null,
    val episodes: Int? = null,
    val format: String? = null,
    val nextAiringEpisode: NextAiringEpisode? = null,
    val relations: AnimeRelations? = null
)


@Serializable
data class AnimeRelations(
    val edges: List<AnimeRelationEdge> = emptyList()
)


@Serializable
data class AnimeRelationEdge(
    val relationType: String? = null,
    val node: AnimeRelationNode? = null
)


@Serializable
data class AnimeRelationNode(
    val id: Int,
    val title: MediaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val episodes: Int? = null,
    val averageScore: Int? = null,
    val type: String? = null,
    val format: String? = null,
    val nextAiringEpisode: NextAiringEpisode? = null
)


@Serializable
data class AnimeRecommendationsResponse(val data: AnimeRecommendationsData)


@Serializable
data class AnimeRecommendationsData(val Media: AnimeRecommendationsMedia)


@Serializable
data class AnimeRecommendationsMedia(
    val id: Int,
    val recommendations: AnimeRecommendations? = null
)


@Serializable
data class AnimeRecommendations(
    val nodes: List<AnimeRecommendationNode> = emptyList()
)


@Serializable
data class AnimeRecommendationNode(
    val mediaRecommendation: AnimeRelationNode? = null
)


@Serializable
data class DetailedAnimeCharacters(
    val nodes: List<DetailedAnimeCharacterNode> = emptyList()
)


@Serializable
data class DetailedAnimeCharacterNode(
    val id: Int,
    val name: DetailedCharacterName? = null,
    val image: MediaCoverImage? = null
)


@Serializable
data class DetailedCharacterName(
    val full: String? = null,
    val native: String? = null
)


@Serializable
data class DetailedAnimeStaff(
    val edges: List<DetailedAnimeStaffEdge> = emptyList()
)


@Serializable
data class DetailedAnimeStaffEdge(
    val node: DetailedAnimeStaffNode? = null,
    val role: String? = null
)


@Serializable
data class DetailedAnimeStaffNode(
    val id: Int,
    val name: DetailedCharacterName? = null,
    val image: MediaCoverImage? = null
)


@Serializable
data class MediaTrailer(
    val id: String? = null,
    val site: String? = null
)


@Serializable
data class CharacterData(
    val id: Int,
    val name: DetailedCharacterName? = null,
    val image: MediaCoverImage? = null,
    val description: String? = null,
    val anime: CharacterAnimeConnection? = null
)


@Serializable
data class CharacterAnimeConnection(
    val nodes: List<CharacterAnimeNode> = emptyList()
)


@Serializable
data class CharacterAnimeNode(
    val id: Int,
    val title: MediaTitle? = null,
    val coverImage: MediaCoverImage? = null,
    val format: String? = null,
    val episodes: Int? = null,
    val averageScore: Int? = null
)


@Serializable
data class StaffData(
    val id: Int,
    val name: DetailedCharacterName? = null,
    val image: MediaCoverImage? = null,
    val description: String? = null,
    val anime: StaffAnimeConnection? = null,
    val primaryOccupations: List<String>? = null
)


@Serializable
data class StaffAnimeConnection(
    val edges: List<StaffAnimeEdge> = emptyList()
)


@Serializable
data class StaffAnimeEdge(
    val node: CharacterAnimeNode? = null,
    val staffRole: String? = null
)


@Serializable
data class CharacterResponse(val data: CharacterResponseData)

@Serializable
data class CharacterResponseData(val Character: CharacterData?)


@Serializable
data class StaffResponse(val data: StaffResponseData)

@Serializable
data class StaffResponseData(val Staff: StaffData?)


@Serializable
data class AllCharactersResponse(val data: AllCharactersData)

@Serializable
data class AllCharactersData(val Media: AllCharactersMedia?)

@Serializable
data class AllCharactersMedia(val characters: AllCharactersConnection?)

@Serializable
data class AllCharactersConnection(val nodes: List<CharacterData> = emptyList())


@Serializable
data class AllStaffResponse(val data: AllStaffData)

@Serializable
data class AllStaffData(val Media: AllStaffMedia?)

@Serializable
data class AllStaffMedia(val staff: AllStaffConnection?)

@Serializable
data class AllStaffConnection(val edges: List<DetailedAnimeStaffEdge> = emptyList())


@Serializable
data class MediaTagCollectionResponse(val data: MediaTagCollectionData)

@Serializable
data class MediaTagCollectionData(val MediaTagCollection: List<MediaTag>)

@Serializable
data class MediaTag(
    val id: Int,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val rank: Int? = null,
    val isAdult: Boolean = false
)
