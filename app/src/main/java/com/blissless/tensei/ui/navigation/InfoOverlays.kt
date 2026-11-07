package com.blissless.tensei.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.ui.screens.cast.AllCastScreen
import com.blissless.tensei.ui.screens.cast.AllStaffScreen
import com.blissless.tensei.ui.screens.character.CharacterScreen
import com.blissless.tensei.ui.screens.character.StaffScreen
import com.blissless.tensei.ui.screens.relations.AllRecommendationsScreen
import com.blissless.tensei.ui.screens.relations.AllRelationsScreen
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Hosts for the "person and relation" overlay screens: a single character or
 * staff member ([CharacterOverlay] / [StaffOverlay]) and the full cast /
 * staff / relations / recommendations grids ([AllCastOverlay],
 * [AllStaffOverlay], [AllRelationsOverlay], [AllRecommendationsOverlay]).
 *
 * All six read their back-stack from the current [OverlayState] and push new
 * states through [setOverlayState]; extracted verbatim from
 * [com.blissless.tensei.MainScreen].
 */

/**
 * Shared handler for the media ("Anime roles" / "Manga roles") entries on the
 * character and staff screens: opens an anime detail overlay for anime
 * formats, or a manga detail for manga formats.
 */
private fun openMedia(
    viewModel: MainViewModel,
    setOverlayState: (OverlayState) -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
    openAsAnime: (mediaId: Int) -> Unit,
    mediaId: Int,
    format: String?,
) {
    if (format != null && !isMangaFormat(format)) {
        openAsAnime(mediaId)
    } else {
        setOverlayState(OverlayState.None)
        openMangaDetail(MangaMedia(id = mediaId, title = "", cover = ""))
        setMangaAutoShowChapters(false)
    }
}

/** Single-character overlay (opened from a character card). */
@Composable
fun CharacterOverlay(
    dialog: OverlayState.CharacterDialog,
    viewModel: MainViewModel,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    CharacterScreen(
        characterId = dialog.characterId,
        viewModel = viewModel,
        stackDepth = dialog.previousStates.size + 1,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onMediaClick = { mediaId, format ->
            openMedia(
                viewModel, setOverlayState, openMangaDetail, setMangaAutoShowChapters,
                openAsAnime = { id ->
                    scope.launch {
                        val newAnime = fetchAnimeForOverlay(viewModel, id)
                        if (newAnime != null) {
                            setOverlayState(
                                OverlayState.ExploreAnimeDialog(
                                    anime = newAnime, firstAnime = newAnime, isFirstOpen = false,
                                    previousStates = dialog.previousStates + dialog
                                )
                            )
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    }
                },
                mediaId = mediaId,
                format = format,
            )
        },
        onCharacterClick = { id ->
            setOverlayState(
                OverlayState.CharacterDialog(
                    characterId = id,
                    animeId = 0,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onStaffClick = { id ->
            setOverlayState(
                OverlayState.StaffDialog(
                    staffId = id,
                    animeId = 0,
                    previousStates = dialog.previousStates + dialog
                )
            )
        }
    )
}

/** Single-staff-member overlay (opened from a staff card). */
@Composable
fun StaffOverlay(
    dialog: OverlayState.StaffDialog,
    viewModel: MainViewModel,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    StaffScreen(
        staffId = dialog.staffId,
        viewModel = viewModel,
        stackDepth = dialog.previousStates.size + 1,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onMediaClick = { mediaId, format ->
            openMedia(
                viewModel, setOverlayState, openMangaDetail, setMangaAutoShowChapters,
                openAsAnime = { id ->
                    scope.launch {
                        val newAnime = fetchAnimeForOverlay(viewModel, id)
                        if (newAnime != null) {
                            setOverlayState(
                                OverlayState.ExploreAnimeDialog(
                                    anime = newAnime, firstAnime = newAnime, isFirstOpen = false,
                                    previousStates = dialog.previousStates + dialog
                                )
                            )
                        } else {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    }
                },
                mediaId = mediaId,
                format = format,
            )
        },
        onCharacterClick = { id ->
            setOverlayState(
                OverlayState.CharacterDialog(
                    characterId = id,
                    animeId = 0,
                    previousStates = dialog.previousStates + dialog
                )
            )
        },
        onStaffClick = { id ->
            setOverlayState(
                OverlayState.StaffDialog(
                    staffId = id,
                    animeId = 0,
                    previousStates = dialog.previousStates + dialog
                )
            )
        }
    )
}

/** Full-cast grid overlay. */
@Composable
fun AllCastOverlay(
    dialog: OverlayState.AllCastDialog,
    viewModel: MainViewModel,
    preferEnglishTitles: Boolean,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
) {
    AllCastScreen(
        animeId = dialog.animeId,
        animeTitle = dialog.animeTitle,
        animeTitleEnglish = dialog.animeTitleEnglish,
        preferEnglishTitles = preferEnglishTitles,
        viewModel = viewModel,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onCharacterClick = { characterId ->
            setOverlayState(
                OverlayState.CharacterDialog(
                    characterId = characterId,
                    animeId = dialog.animeId,
                    previousStates = dialog.previousStates + dialog
                )
            )
        }
    )
}

/** Full staff grid overlay. */
@Composable
fun AllStaffOverlay(
    dialog: OverlayState.AllStaffDialog,
    viewModel: MainViewModel,
    preferEnglishTitles: Boolean,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
) {
    AllStaffScreen(
        animeId = dialog.animeId,
        animeTitle = dialog.animeTitle,
        animeTitleEnglish = dialog.animeTitleEnglish,
        preferEnglishTitles = preferEnglishTitles,
        viewModel = viewModel,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onStaffClick = { staffId ->
            setOverlayState(
                OverlayState.StaffDialog(
                    staffId = staffId,
                    animeId = dialog.animeId,
                    previousStates = dialog.previousStates + dialog
                )
            )
        }
    )
}

/** Relations grid overlay (relations of one anime). */
@Composable
fun AllRelationsOverlay(
    dialog: OverlayState.AllRelationsDialog,
    viewModel: MainViewModel,
    preferEnglishTitles: Boolean,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AllRelationsScreen(
        animeId = dialog.animeId,
        animeTitle = dialog.animeTitle,
        animeTitleEnglish = dialog.animeTitleEnglish,
        preferEnglishTitles = preferEnglishTitles,
        viewModel = viewModel,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onRelationClick = { relation ->
            if (relation.format != null && !isMangaFormat(relation.format)) {
                scope.launch {
                    val newAnime = fetchAnimeForOverlay(viewModel, relation.id)
                    if (newAnime != null) {
                        setOverlayState(
                            OverlayState.ExploreAnimeDialog(
                                anime = newAnime,
                                firstAnime = newAnime,
                                isFirstOpen = false,
                                previousStates = dialog.previousStates + dialog
                            )
                        )
                    } else {
                        context.toast(context.getString(R.string.app_anime_not_found))
                    }
                }
            } else {
                setOverlayState(OverlayState.None)
                openMangaDetail(
                    MangaMedia(
                        id = relation.id,
                        title = relation.title,
                        cover = relation.cover,
                        averageScore = relation.averageScore,
                        format = relation.format
                    )
                )
                setMangaAutoShowChapters(false)
            }
        }
    )
}

/** Recommendations grid overlay (recommendations for one anime). */
@Composable
fun AllRecommendationsOverlay(
    dialog: OverlayState.AllRecommendationsDialog,
    viewModel: MainViewModel,
    preferEnglishTitles: Boolean,
    setOverlayState: (OverlayState) -> Unit,
    onNavigateBack: () -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    setMangaAutoShowChapters: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AllRecommendationsScreen(
        animeId = dialog.animeId,
        animeTitle = dialog.animeTitle,
        animeTitleEnglish = dialog.animeTitleEnglish,
        preferEnglishTitles = preferEnglishTitles,
        viewModel = viewModel,
        onDismiss = {
            setOverlayState(OverlayState.None)
        },
        onNavigateBack = onNavigateBack,
        onRecommendationClick = { rec ->
            if (isMangaFormat(rec.format)) {
                setOverlayState(OverlayState.None)
                openMangaDetail(
                    MangaMedia(
                        id = rec.id,
                        title = rec.title,
                        cover = rec.cover,
                        totalChapters = rec.episodes ?: 0,
                        averageScore = rec.averageScore,
                        format = rec.format
                    )
                )
                setMangaAutoShowChapters(false)
            } else {
                scope.launch {
                    val newAnime = fetchAnimeForOverlay(viewModel, rec.id)
                    if (newAnime != null) {
                        setOverlayState(
                            OverlayState.ExploreAnimeDialog(
                                anime = newAnime,
                                firstAnime = newAnime,
                                isFirstOpen = false,
                                previousStates = dialog.previousStates + dialog
                            )
                        )
                    } else {
                        context.toast(context.getString(R.string.app_anime_not_found))
                    }
                }
            }
        }
    )
}
