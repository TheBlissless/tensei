package com.blissless.tensei.ui.screens.profile

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.blissless.tensei.viewmodel.malMangaFavorites
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.blissless.tensei.viewmodel.toggleMalFavoriteById
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.util.LocaleDate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.data.models.UserAnimeStats
import com.blissless.tensei.ui.components.SectionHeaderSkeleton
import com.blissless.tensei.ui.components.SkeletonBlock
import com.blissless.tensei.ui.components.TenseiDisclosureHeader
import com.blissless.tensei.ui.components.TenseiTopBar
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.shimmer
import com.blissless.tensei.ui.theme.Radius
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.themeStatusColors
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.MangaStatusLabels
import com.blissless.tensei.ui.theme.tenseiColors
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.Locale
// Extension functions on MainViewModel (defined in com.blissless.tensei.viewmodel)
import com.blissless.tensei.viewmodel.fetchAniListFavorites
import com.blissless.tensei.viewmodel.fetchUserActivity
import com.blissless.tensei.viewmodel.fetchUserStats
import com.blissless.tensei.viewmodel.loadAniListFavoritesFromStorage
import com.blissless.tensei.viewmodel.toggleAniListFavorite
import com.blissless.tensei.viewmodel.mangaContinueReading
import com.blissless.tensei.viewmodel.mangaPlanningToRead
import com.blissless.tensei.viewmodel.mangaCompleted
import com.blissless.tensei.viewmodel.mangaCurrentlyReading
import com.blissless.tensei.viewmodel.mangaPaused
import com.blissless.tensei.viewmodel.mangaDropped
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaActivityMedia
import com.blissless.tensei.data.models.MangaTitle
import com.blissless.tensei.data.models.MediaCoverImage
import com.blissless.tensei.data.models.MangaFuzzyDate
import com.blissless.tensei.data.models.MangaUserProfile
import com.blissless.tensei.viewmodel.mangaFavorites
import com.blissless.tensei.viewmodel.mangaActivity
import com.blissless.tensei.viewmodel.mangaUserProfile
import com.blissless.tensei.viewmodel.fetchMangaUserProfile
import com.blissless.tensei.viewmodel.toggleMangaFavorite


@Composable
fun UserProfileScreen(
    viewModel: MainViewModel,
    preferEnglishTitles: Boolean = true,
    onBack: () -> Unit,
    onShowDetailedAnime: (animeId: Int, malId: Int) -> Unit,
    onMangaClick: (MangaMedia) -> Unit = {}
) {
    var selectedSection by remember { mutableStateOf(UserProfileSection.ABOUT_ME) }
    val context = LocalContext.current

    val loginProvider by viewModel.loginProvider.collectAsState()
    val malFavorites by viewModel.malFavorites.collectAsState()
    val aniListFavorites by viewModel.aniListFavorites.collectAsState()
    val userActivity by viewModel.userActivity.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userBanner by viewModel.userBanner.collectAsState()
    val userBio by viewModel.userBio.collectAsState()
    val userSiteUrl by viewModel.userSiteUrl.collectAsState()
    val userCreatedAt by viewModel.userCreatedAt.collectAsState()

    val mangaFavorites by viewModel.mangaFavorites.collectAsState()
    // MAL-local manga favorites — collected so the profile's manga favorites grid can render
    // them in MAL-only mode (where _mangaFavorites is empty because no AniList sync happens).
    // In ANILIST / BOTH mode this set is typically a subset of the AniList-sourced list, so
    // merging them is safe (the distinctBy { it.id } below deduplicates).
    val malMangaFavoriteIds by viewModel.malMangaFavorites.collectAsState()
    val mangaActivity by viewModel.mangaActivity.collectAsState()
    val mangaUserProfile by viewModel.mangaUserProfile.collectAsState()

    val mangaReading by viewModel.mangaCurrentlyReading.collectAsState()
    val mangaPlanning by viewModel.mangaPlanningToRead.collectAsState()
    val mangaFinished by viewModel.mangaCompleted.collectAsState()
    val mangaHeld by viewModel.mangaPaused.collectAsState()
    val mangaAbandoned by viewModel.mangaDropped.collectAsState()

    // Nothing on this screen exposes a loading flag, so while the profile fetches
    // are in flight every tab would otherwise render its "empty" state and the
    // placeholder name "User". Track the fetches here so each tab can skeleton.
    var isProfileLoading by remember { mutableStateOf(false) }

    LaunchedEffect(loginProvider) {
        if (loginProvider == LoginProvider.ANILIST || loginProvider == LoginProvider.BOTH) {
            isProfileLoading = true
            val elapsed = measureTimeMillis {
                viewModel.loadAniListFavoritesFromStorage()
                // Refresh the Viewer profile (name/avatar/banner/bio/joined/stats) and
                // ensure _userId is populated BEFORE the child fetches that depend on it —
                // otherwise fetchUserStats / fetchUserActivity / fetchAniListFavorites
                // return early with null userId and About Me stays empty.
                viewModel.fetchUser()
                // The four remaining fetches are independent of each other, so run them
                // concurrently instead of paying for them one after another.
                coroutineScope {
                    listOf(
                        async { viewModel.fetchAniListFavorites() },
                        async { viewModel.fetchUserActivity() },
                        async { viewModel.fetchUserStats() },
                        async { viewModel.fetchMangaUserProfile() },
                    ).awaitAll()
                }
            }
            // Avoid a one-frame skeleton flash when everything resolves from cache.
            if (elapsed < ProfileSkeletonMinDurationMs) {
                delay(ProfileSkeletonMinDurationMs - elapsed)
            }
            isProfileLoading = false
        } else {
            // MAL-only profiles render from the already-loaded list state.
            isProfileLoading = false
        }
    }

    val favorites: List<ProfileFavorite> = when (loginProvider) {
        LoginProvider.ANILIST, LoginProvider.BOTH -> {
            aniListFavorites.map { aniListFavorite ->
                ProfileFavorite(
                    id = aniListFavorite.id,
                    malId = aniListFavorite.idMal ?: 0,
                    title = aniListFavorite.title.romaji ?: aniListFavorite.title.english ?: "",
                    titleEnglish = aniListFavorite.title.english,
                    cover = aniListFavorite.coverImage?.extraLarge ?: aniListFavorite.coverImage?.large ?: "",
                    year = aniListFavorite.seasonYear,
                    episodes = aniListFavorite.episodes,
                    averageScore = aniListFavorite.averageScore,
                    format = aniListFavorite.format,
                    status = aniListFavorite.status,
                    userScore = aniListFavorite.userScore
                )
            }
        }
        LoginProvider.MAL -> malFavorites.map { favorite ->
            ProfileFavorite(
                id = favorite.id,
                malId = favorite.malId ?: 0,
                title = favorite.title,
                titleEnglish = favorite.titleEnglish,
                cover = favorite.cover,
                year = favorite.year,
                episodes = favorite.totalEpisodes.takeIf { it > 0 },
                averageScore = favorite.averageScore,
                format = favorite.format,
                status = favorite.status.takeIf { it.isNotBlank() },
                userScore = favorite.userScore
            )
        }
        LoginProvider.NONE -> emptyList()
    }

    val historyData = when (loginProvider) {
        LoginProvider.ANILIST, LoginProvider.BOTH -> {
            val statuses = mutableListOf<String>()
            val progress = mutableListOf<String>()
            val entries = userActivity.take(50).map { activity ->
                val progressStr = activity.progress
                val episodeDisplay = progressStr?.let { prog ->
                    val nums =
                        Regex("\\d+").findAll(prog).map { it.value.toIntOrNull() }.filterNotNull().toList()
                    when {
                        nums.size >= 2 && nums[1] > nums[0] -> context.getString(R.string.profile_episode_range, nums[0], nums[1])
                        nums.isNotEmpty() -> context.getString(R.string.profile_episode, nums[0])
                        else -> prog.lowercase()
                    }
                }
                statuses.add(activity.status)
                progress.add(episodeDisplay ?: "")
                HistoryEntry(
                    malId = activity.mediaIdMal ?: 0,
                    aniListId = activity.mediaId,
                    title = activity.mediaTitle,
                    titleEnglish = activity.mediaTitleEnglish,
                    cover = activity.mediaCover,
                    date = formatTimestamp(activity.createdAt, LocaleDate.localeOf(context))
                )
            }
            HistoryData(entries, statuses, progress)
        }
        LoginProvider.MAL -> HistoryData(emptyList(), emptyList(), emptyList())
        LoginProvider.NONE -> HistoryData(emptyList(), emptyList(), emptyList())
    }

    val history = historyData.entries
    val statuses = historyData.statuses
    val progressDisplay = historyData.progressList

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TenseiTopBar(
            title = when (selectedSection) {
                UserProfileSection.ABOUT_ME -> stringResource(R.string.profile_about_me)
                UserProfileSection.FAVORITES -> stringResource(R.string.profile_favorites)
                UserProfileSection.HISTORY -> stringResource(R.string.profile_history)
            },
            leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            onLeadingClick = onBack,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomDivider = true,
            actions = {
                if (selectedSection == UserProfileSection.ABOUT_ME && userSiteUrl != null) {
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, userSiteUrl)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.profile_share_profile)))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.profile_share_profile_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            // Only skeleton a tab that has nothing to show yet — if data already
            // arrived (cache or a fast response) keep rendering it.
            val aboutLoading = isProfileLoading && userName == null
            val favoritesLoading = isProfileLoading &&
                favorites.isEmpty() && mangaFavorites.isEmpty()
            val historyLoading = isProfileLoading &&
                history.isEmpty() && mangaActivity.isEmpty()

            when (selectedSection) {
                UserProfileSection.ABOUT_ME -> if (aboutLoading) {
                    ProfileAboutSkeleton()
                } else AboutMeContent(
                    username = userName ?: stringResource(R.string.profile_user),
                    userAvatar = userAvatar, userBanner = userBanner,
                    userBio = userBio,
                    userCreatedAt = userCreatedAt, userStats = userStats,
                    mangaUserProfile = mangaUserProfile,
                    animeLibrary = listOf(
                        LibraryStatus("CURRENT", viewModel.currentlyWatching.value.size),
                        LibraryStatus("PLANNING", viewModel.planningToWatch.value.size),
                        LibraryStatus("COMPLETED", viewModel.completed.value.size),
                        LibraryStatus("PAUSED", viewModel.onHold.value.size),
                        LibraryStatus("DROPPED", viewModel.dropped.value.size)
                    ),
                    mangaLibrary = listOf(
                        LibraryStatus("CURRENT", mangaReading.size),
                        LibraryStatus("PLANNING", mangaPlanning.size),
                        LibraryStatus("COMPLETED", mangaFinished.size),
                        LibraryStatus("PAUSED", mangaHeld.size),
                        LibraryStatus("DROPPED", mangaAbandoned.size)
                    )
                )
                UserProfileSection.FAVORITES -> if (favoritesLoading) {
                    ProfileFavoritesSkeleton()
                } else {
                    val allManga = mangaReading + mangaPlanning + mangaFinished + mangaHeld + mangaAbandoned
                    val mangaScoreLookup = allManga.associate { it.id to it.userScore }
                    // Build MangaFavorite entries for MAL-local manga favorites by looking them
                    // up in the tracked manga lists (which carry the full metadata). Without this,
                    // MAL-only users would see an empty manga favorites grid even though they've
                    // favorited manga via the detail-screen heart. In ANILIST / BOTH mode this
                    // list is empty (or a subset of mangaFavorites), so the merge + distinctBy
                    // below is a no-op.
                    val malLocalMangaFavorites = allManga
                        .filter { it.id in malMangaFavoriteIds }
                        .map { mm ->
                            MangaFavorite(
                                id = mm.id,
                                title = MangaTitle(romaji = mm.title, english = mm.titleEnglish),
                                coverImage = MediaCoverImage(extraLarge = mm.cover),
                                chapters = mm.totalChapters.takeIf { it > 0 },
                                averageScore = mm.averageScore,
                                userScore = mm.userScore,
                                idMal = mm.malId
                            )
                        }
                    val enrichedMangaFavorites = (mangaFavorites + malLocalMangaFavorites)
                        .distinctBy { it.id }
                        .map { mf ->
                            mf.copy(userScore = mf.userScore ?: mangaScoreLookup[mf.id])
                        }
                    FavoritesContent(
                    favorites = favorites,
                    mangaFavorites = enrichedMangaFavorites,
                    preferEnglishTitles = preferEnglishTitles,
                    onAnimeClick = { anime ->
                        if (anime.id != 0) {
                            onShowDetailedAnime(anime.id, anime.malId)
                        } else if (anime.malId != 0) {
                            onShowDetailedAnime(anime.malId, anime.malId)
                        }
                    },
                        // Provide a remove callback for ALL logged-in providers:
                        //   - ANILIST / BOTH mode: route through toggleAniListFavorite, which handles
                        //     the AniList API push (and the BOTH-mode MAL-local mirror update via
                        //     Edit 2a). The favorite's `id` here is the AniList id (line 248-249).
                        //   - MAL-only mode: route through toggleMalFavoriteById, which is local-only
                        //     (no MAL favorite-write API exists). Without this branch, MAL-only users
                        //     had no way to remove their local anime favorites from the profile screen
                        //     — the remove button was suppressed entirely.
                        onRemoveFavorite = when (loginProvider) {
                            // Each branch returns a lambda (ProfileFavorite) -> Unit. The explicit
                            // `fav ->` parameter is required because a bare `{ ... }` after `->`
                            // in a when-branch is parsed as a code block (returning Unit), not a
                            // lambda — without the parameter, `it` is unresolved and the type
                            // mismatch surfaces as "actual type is Unit?".
                            LoginProvider.ANILIST, LoginProvider.BOTH -> { fav -> viewModel.toggleAniListFavorite(fav.id) }
                            LoginProvider.MAL -> { fav -> viewModel.toggleMalFavoriteById(fav.id) }
                            else -> null
                        },
                    onMangaClick = { manga ->
                        onMangaClick(
                            MangaMedia(
                                id = manga.id,
                                title = if (preferEnglishTitles) {
                                    manga.title?.english ?: manga.title?.romaji ?: context.getString(R.string.profile_unknown)
                                } else {
                                    manga.title?.romaji ?: manga.title?.english ?: context.getString(R.string.profile_unknown)
                                },
                                titleEnglish = manga.title?.english,
                                cover = manga.coverImage?.extraLarge ?: manga.coverImage?.large ?: "",
                                totalChapters = manga.chapters ?: 0,
                                averageScore = manga.averageScore,
                                siteUrl = manga.siteUrl,
                                malId = manga.idMal
                            )
                        )
                    },
                    onRemoveMangaFavorite = { manga ->
                        viewModel.toggleMangaFavorite(manga.id)
                    }
                )
                }
                UserProfileSection.HISTORY -> if (historyLoading) {
                    ProfileHistorySkeleton()
                } else HistoryContent(
                    history = history,
                    mangaHistory = mangaActivity,
                    preferEnglishTitles = preferEnglishTitles,
                    onAnimeClick = { entry ->
                        val id = entry.aniListId?.takeIf { it != 0 }
                            ?: entry.malId.takeIf { it != 0 }
                        if (id != null) {
                            onShowDetailedAnime(id, entry.malId)
                        }
                    },
                    onMangaClick = { node ->
                        node.media?.let { media ->
                            onMangaClick(
                                MangaMedia(
                                    id = media.id,
                                    title = if (preferEnglishTitles) {
                                        media.title?.english ?: media.title?.romaji ?: context.getString(R.string.profile_unknown)
                                    } else {
                                        media.title?.romaji ?: media.title?.english ?: context.getString(R.string.profile_unknown)
                                    },
                                    titleEnglish = media.title?.english,
                                    cover = media.coverImage?.extraLarge ?: media.coverImage?.large ?: "",
                                    totalChapters = media.chapters ?: 0,
                                    siteUrl = media.siteUrl,
                                    malId = media.idMal
                                )
                            )
                        }
                    },
                    statuses = statuses, progressList = progressDisplay
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                // Absorb taps in the gaps and below the section buttons (full hitbox of the
                // row + its padding) so they don't fall through to the page content behind the
                // profile overlay. Child button taps are unaffected.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(
                    start = Spacing.gutter,
                    end = Spacing.gutter,
                    top = Spacing.md,
                    bottom = navigationBarsPadding.calculateBottomPadding() + Spacing.md,
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfileNavButton(
                icon = Icons.Default.Person, title = stringResource(R.string.profile_about_me),
                isSelected = selectedSection == UserProfileSection.ABOUT_ME,
                onClick = { selectedSection = UserProfileSection.ABOUT_ME }
            )
            UserProfileNavButton(
                icon = Icons.Default.Favorite, title = stringResource(R.string.profile_favorites),
                isSelected = selectedSection == UserProfileSection.FAVORITES,
                onClick = { selectedSection = UserProfileSection.FAVORITES },
                badge = favorites.size + mangaFavorites.size
            )
            UserProfileNavButton(
                icon = Icons.Default.History, title = stringResource(R.string.profile_history),
                isSelected = selectedSection == UserProfileSection.HISTORY,
                onClick = { selectedSection = UserProfileSection.HISTORY },
                badge = history.size + mangaActivity.size
            )
        }
    }
}
