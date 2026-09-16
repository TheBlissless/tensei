package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.AnimeRelation
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SAnimeEpisodeUpdate
import eu.kanade.tachiyomi.animesource.model.SAnimeSeasonUpdate
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video

/**
 * The `AnimeSource` interface — the contract an extension's source class
 * implements. Mirrors Aniyomi's `AnimeSource` at
 * `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/AnimeSource.kt`.
 *
 * NEW in this port vs the previous Tensei version:
 *
 *   - `getAnimeEpisodeUpdate(anime, episodes, fetchDetails, fetchEpisodes)`:
 *     the combined suspend API Aniyomi added in ext-lib 17. Lets the
 *     source fetch the anime details AND the episode list in parallel
 *     from a single network round-trip (or at least, atomically).
 *     Default implementation falls back to calling `getAnimeDetails`
 *     + `getEpisodeList` separately.
 *
 *   - `getAnimeSeasonUpdate(anime, seasons, fetchDetails, fetchSeasons)`:
 *     same idea but for "seasons" (some sources model multi-season anime
 *     as a list of SAnime children).
 *
 *   - `getRelatedAnimeList(anime)`: returns related-anime relations for
 *     the details page's "Related" section. Optional; default throws.
 *
 *   - `supportsRelatedAnime`: capability flag. Default false.
 *
 * The old `getAnimeDetails`, `getEpisodeList`, `getSeasonList`,
 * `getHosterList`, `getVideoList` methods are all kept so existing
 * Tensei code keeps working — the combined API is opt-in.
 */
interface AnimeSource {
    val id: Long
    val name: String
    val lang: String
        get() = ""

    /**
     * Whether this source supports the "Latest" tab in the browser UI.
     * Anime sources may set this to false if they have no concept of
     * "recently added" anime.
     */
    val supportsLatest: Boolean
        get() = false

    /**
     * Whether this source can return a list of related anime for any
     * given anime. If `false`, the UI hides the "Related" section.
     */
    val supportsRelatedAnime: Boolean
        get() = false

    // ─── Legacy single-call API (still supported) ───────────────────────────

    suspend fun getAnimeDetails(anime: SAnime): SAnime
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode>
    suspend fun getSeasonList(anime: SAnime): List<SAnime>

    suspend fun getHosterList(episode: SEpisode): List<Hoster> =
        throw IllegalStateException("Not used")

    suspend fun getVideoList(hoster: Hoster): List<Video> =
        throw IllegalStateException("Not used")

    suspend fun getVideoList(episode: SEpisode): List<Video>

    // ─── NEW combined API (Aniyomi ext-lib 17+) ────────────────────────────

    /**
     * Fetch the anime details and/or episode list in a single call.
     *
     * Implementations should run the two fetches in parallel via
     * `coroutineScope { … async { … } … }` when both flags are true;
     * when only one flag is true, only the corresponding fetch should
     * run.
     *
     * Default implementation here just calls the legacy single-call
     * methods in sequence. Extensions compiled against ext-lib 17 will
     * override this with a real parallel implementation.
     *
     * @param anime the partial SAnime (only `url` populated)
     * @param episodes the previously-known episode list (may be empty
     *   if `fetchEpisodes = true`)
     * @param fetchDetails if true, refresh `getAnimeDetails`
     * @param fetchEpisodes if true, refresh `getEpisodeList`
     * @return the refreshed anime + episodes in a `SAnimeEpisodeUpdate`
     */
    suspend fun getAnimeEpisodeUpdate(
        anime: SAnime,
        episodes: List<SEpisode>,
        fetchDetails: Boolean,
        fetchEpisodes: Boolean,
    ): SAnimeEpisodeUpdate {
        val refreshedAnime = if (fetchDetails) getAnimeDetails(anime) else anime
        val refreshedEpisodes = if (fetchEpisodes) getEpisodeList(refreshedAnime) else episodes
        return SAnimeEpisodeUpdate(refreshedAnime, refreshedEpisodes)
    }

    /**
     * Same idea but for seasons. Default implementation calls
     * `getAnimeDetails` + `getSeasonList` in sequence.
     */
    suspend fun getAnimeSeasonUpdate(
        anime: SAnime,
        seasons: List<SAnime>,
        fetchDetails: Boolean,
        fetchSeasons: Boolean,
    ): SAnimeSeasonUpdate {
        val refreshedAnime = if (fetchDetails) getAnimeDetails(anime) else anime
        val refreshedSeasons = if (fetchSeasons) getSeasonList(refreshedAnime) else seasons
        return SAnimeSeasonUpdate(refreshedAnime, refreshedSeasons)
    }

    /**
     * Return a list of related anime for the given anime (sequels,
     * prequels, side stories, etc.). Default throws — sources that
     * don't support related anime should not be called.
     */
    suspend fun getRelatedAnimeList(anime: SAnime): List<AnimeRelation> =
        throw UnsupportedOperationException("getRelatedAnimeList not implemented")
}
