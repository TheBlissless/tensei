package com.blissless.tensei.ui.navigation

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.ui.screens.manga.DetailedMangaScreen
import com.blissless.tensei.ui.screens.manga.MangaAllCharactersScreen
import com.blissless.tensei.ui.screens.manga.MangaAllRecommendationsScreen
import com.blissless.tensei.ui.screens.manga.MangaAllRelationsScreen
import com.blissless.tensei.ui.screens.manga.MangaAllStaffScreen
import com.blissless.tensei.ui.screens.manga.MangaReaderScreen
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.clearMangaDetail
import com.blissless.tensei.viewmodel.fetchDetailedAnimeData
import com.blissless.tensei.viewmodel.isMangaFavorited
import com.blissless.tensei.viewmodel.removeMangaTracking
import com.blissless.tensei.viewmodel.updateMangaProgress
import com.blissless.tensei.viewmodel.updateMangaStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Hosts the manga navigation stack held by [MangaNavState]:
 *
 *  - the chain of [DetailedMangaScreen] detail pages,
 *  - the fullscreen reader [Dialog] layered on top of them,
 *  - the "All characters / staff / relations / recommendations" grids,
 *  - the no-manga-extension redirect dialog, and
 *  - the "resume pending read after returning from Settings" effect.
 *
 * Extracted verbatim from the [com.blissless.tensei.MainScreen] composable;
 * composition order inside matches the original source order (detail →
 * resume effect → dialogs → reader → grids).
 */
@Composable
fun MangaNavHost(
    state: MangaNavState,
    viewModel: MainViewModel,
    isOled: Boolean,
    preferEnglishTitles: Boolean,
    isLoggedIn: Boolean,
    settingsReturnVersion: Int,
    hasMangaSource: Boolean,
    setOverlayState: (OverlayState) -> Unit,
    openMangaDetail: (MangaMedia) -> Unit,
    onOpenAnimeDetailFromManga: (com.blissless.tensei.data.models.AnimeMedia) -> Unit,
    onOpenSettings: (group: String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ─── Manga Detail Screen ─────────────────────────────────────────
    val currentManga = state.currentManga
    if (currentManga != null && state.showDetailScreen) {
        val manga = currentManga
        DetailedMangaScreen(
            manga = manga,
            viewModel = viewModel,
            isOled = isOled,
            preferEnglishTitles = preferEnglishTitles,
            autoShowChapters = state.autoShowChapters,
            currentStatus = manga.listStatus,
            currentProgress = manga.progress,
            isLoggedIn = isLoggedIn,
            isFavorite = viewModel.isMangaFavorited(manga.id),
            stackDepth = state.detailStack.size,
            onRelationClick = { relation ->
                // If the relation is an anime format, open the anime detail screen
                if (relation.format != null && !isMangaFormat(relation.format)) {
                    scope.launch {
                        try {
                            delay(100.milliseconds)
                            val detailedData = viewModel.fetchDetailedAnimeData(relation.id)
                            if (detailedData != null) {
                                onOpenAnimeDetailFromManga(detailedData.toAnimeMedia())
                            } else {
                                context.toast(context.getString(R.string.app_anime_not_found))
                            }
                        } catch (_: Exception) {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    }
                } else {
                    // Manga relation — push the new manga detail onto the stack
                    openMangaDetail(
                        MangaMedia(
                            id = relation.id,
                            title = relation.title,
                            cover = relation.cover,
                            totalChapters = relation.chapters ?: 0,
                            averageScore = relation.averageScore,
                            format = relation.format
                        )
                    )
                    state.autoShowChapters = false
                }
            },
            onCharacterClick = { characterId ->
                setOverlayState(
                    OverlayState.CharacterDialog(
                        characterId = characterId,
                        animeId = manga.id,
                        previousStates = emptyList()
                    )
                )
            },
            onStaffClick = { staffId ->
                setOverlayState(
                    OverlayState.StaffDialog(
                        staffId = staffId,
                        animeId = manga.id,
                        previousStates = emptyList()
                    )
                )
            },
            onViewAllCharacters = {
                state.overlay = MangaOverlay.AllCharacters(manga.id, manga.title, manga.titleEnglish)
            },
            onViewAllStaff = {
                state.overlay = MangaOverlay.AllStaff(manga.id, manga.title, manga.titleEnglish)
            },
            onViewAllRelations = {
                state.overlay = MangaOverlay.AllRelations(manga.id, manga.title, manga.titleEnglish, manga.malId)
            },
            onViewAllRecommendations = {
                state.overlay = MangaOverlay.AllRecommendations(manga.id, manga.title, manga.titleEnglish, manga.malId)
            },
            navigateToMangaDetail = { mangaId ->
                // Navigate to another manga's detail — push a fresh manga onto the stack
                openMangaDetail(
                    MangaMedia(
                        id = mangaId,
                        title = "",
                        cover = ""
                    )
                )
                state.autoShowChapters = false
            },
            onDismiss = {
                if (state.showReader) {
                } else {
                    state.autoShowChapters = false
                    state.popDetail()
                    viewModel.clearMangaDetail()
                }
            },
            onNavigateBack = {
                if (state.showReader) {
                } else {
                    state.autoShowChapters = false
                    state.popDetail()
                    viewModel.clearMangaDetail()
                }
            },
            onCloseAll = {
                if (!state.showReader) {
                    state.detailStack = emptyList()
                    state.showDetailScreen = false
                    state.autoShowChapters = false
                }
            },
            onSwipeToClose = {
                if (state.showReader) {
                } else {
                    state.autoShowChapters = false
                    state.popDetail()
                    viewModel.clearMangaDetail()
                }
            },
            onUpdateStatus = { status, progress ->
                if (status != null) {
                    viewModel.updateMangaStatus(manga.id, status, progress, malId = manga.malId, title = manga.title, cover = manga.cover)
                }
            },
            onUpdateProgress = { progress ->
                viewModel.updateMangaProgress(manga.id, progress.toFloat())
            },
            onRemove = {
                viewModel.removeMangaTracking(manga.id)
                state.autoShowChapters = false
                state.popDetail()
                viewModel.clearMangaDetail()
            },
            onStartReader = { chapterIndex ->
                state.readerChapterIndex = chapterIndex
                state.showReader = true
                // NOTE: Do NOT hide the detail or clear manga detail here — the reader is a
                // Dialog layered on top, so closing the reader reveals the detail again.
            }
        )
    }

    // ─── Resume pending manga actions after returning from Settings ───
    // After returning from Settings, resume the pending manga read/open-chapters
    // action if a manga default extension was just configured.
    LaunchedEffect(settingsReturnVersion) {
        val pending = state.pendingAfterSettings
        if (settingsReturnVersion > 0 && pending != null) {
            state.pendingAfterSettings = null
            val resume = state.pendingResumeAfterSettings
            state.pendingResumeAfterSettings = false
            if (!hasMangaSource) {
                state.showNoExtensionDialog = true
            } else {
                state.autoShowChapters = !resume
                if (state.detailStack.lastOrNull()?.id != pending.id || !state.showDetailScreen) {
                    state.detailStack = state.detailStack + pending
                }
                state.readerChapterIndex = if (resume) pending.progress.coerceAtLeast(0) else -1
                state.showReader = true
            }
        }
    }

    // ─── No-manga-extension redirect dialog ───────────────────────────
    if (state.showNoExtensionDialog) {
        AlertDialog(
            onDismissRequest = {
                state.showNoExtensionDialog = false
                state.pendingAfterSettings = null
                state.pendingResumeAfterSettings = false
            },
            title = { Text(stringResource(R.string.app_no_extension_title)) },
            text = { Text(stringResource(R.string.app_no_manga_extension_message)) },
            confirmButton = {
                TextButton(onClick = {
                    state.showNoExtensionDialog = false
                    onOpenSettings("reader")
                }) {
                    Text(stringResource(R.string.app_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    state.showNoExtensionDialog = false
                    state.pendingAfterSettings = null
                    state.pendingResumeAfterSettings = false
                }) {
                    Text(stringResource(R.string.app_close))
                }
            }
        )
    }

    // ─── Manga Reader Screen ─────────────────────────────────────────
    // NOTE: Wrapped in a fullscreen Dialog so it renders on top of the main
    // Scaffold. Before this, the reader was composed directly in the main window
    // and the Scaffold (composed later, always on top) covered it — the reader
    // opened (state/logs) but was never visible.
    if (state.showReader && currentManga != null && !hasMangaSource) {
        // No default manga extension: never compose the reader here (composing it for even one
        // frame caused a flash — reader pops up then instantly closes). Surface the simple
        // no-extension dialog over whatever is behind instead.
        LaunchedEffect(state.showReader, currentManga) {
            state.autoShowChapters = false
            state.showReader = false
            // Park the reader intent so returning from Settings (after picking a
            // default) reopens the reader at exactly the same place: chapter list
            // when the chapter selection was requested, or the tapped chapter when
            // a chapter was picked directly.
            state.pendingAfterSettings = currentManga
            state.pendingResumeAfterSettings = state.readerChapterIndex >= 0
            state.showNoExtensionDialog = true
        }
    } else if (state.showReader && currentManga != null) {
        val readerManga = currentManga
        val closeReader: () -> Unit = {
            if (state.showReader) {
                state.autoShowChapters = false
                state.showReader = false
                if (!state.showDetailScreen) {
                    // Reader was opened directly (e.g. from a home card) with no detail behind.
                    state.detailStack = state.detailStack.dropLast(1)
                    if (state.detailStack.isEmpty()) {
                        state.showDetailScreen = false
                    }
                    viewModel.clearMangaDetail()
                }
            }
        }
        Dialog(
            onDismissRequest = {
                closeReader()
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                // Back navigation is owned by the reader itself (returns to the chapter
                // list first, then closes). Letting the dialog auto-dismiss on back made
                // the reader jump straight back to home.
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            MangaReaderScreen(
                manga = readerManga,
                initialChapterIndex = state.readerChapterIndex,
                viewModel = viewModel,
                isOled = isOled,
                onClose = closeReader,
                onOpenSettings = {
                    state.pendingAfterSettings = readerManga
                    state.pendingResumeAfterSettings = false
                    closeReader()
                    onOpenSettings("reader")
                }
            )
        }
    } else if (state.showReader && currentManga == null) {
        Log.e("MangaNav", "READER skipped: showMangaReader=true but currentManga is NULL! " +
            "autoShowChapters=${state.autoShowChapters}")
    }

    // ─── Manga All Characters / All Staff / All Relations overlays ────
    when (val mangaOv = state.overlay) {
        is MangaOverlay.AllCharacters -> MangaAllCharactersScreen(
            mangaId = mangaOv.mangaId,
            mangaTitle = mangaOv.mangaTitle,
            mangaTitleEnglish = mangaOv.mangaTitleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            viewModel = viewModel,
            onDismiss = { state.overlay = MangaOverlay.None },
            onNavigateBack = { state.overlay = MangaOverlay.None },
            onCharacterClick = { characterId ->
                setOverlayState(
                    OverlayState.CharacterDialog(
                        characterId = characterId,
                        animeId = mangaOv.mangaId,
                        previousStates = emptyList()
                    )
                )
            }
        )
        is MangaOverlay.AllStaff -> MangaAllStaffScreen(
            mangaId = mangaOv.mangaId,
            mangaTitle = mangaOv.mangaTitle,
            mangaTitleEnglish = mangaOv.mangaTitleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            viewModel = viewModel,
            onDismiss = { state.overlay = MangaOverlay.None },
            onNavigateBack = { state.overlay = MangaOverlay.None },
            onStaffClick = { staffId ->
                setOverlayState(
                    OverlayState.StaffDialog(
                        staffId = staffId,
                        animeId = mangaOv.mangaId,
                        previousStates = emptyList()
                    )
                )
            }
        )
        is MangaOverlay.AllRelations -> MangaAllRelationsScreen(
            mangaId = mangaOv.mangaId,
            malId = mangaOv.malId,
            mangaTitle = mangaOv.mangaTitle,
            mangaTitleEnglish = mangaOv.mangaTitleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            viewModel = viewModel,
            onDismiss = { state.overlay = MangaOverlay.None },
            onNavigateBack = { state.overlay = MangaOverlay.None },
            onRelationClick = { relation ->
                // Suspend the grid so it can be restored when back returns to this manga
                state.overlayRestoreStack = state.overlayRestoreStack +
                    MangaOverlay.AllRelations(mangaOv.mangaId, mangaOv.mangaTitle, mangaOv.mangaTitleEnglish, mangaOv.malId)
                state.overlay = MangaOverlay.None
                if (relation.format != null && !isMangaFormat(relation.format)) {
                    scope.launch {
                        try {
                            delay(100.milliseconds)
                            val detailedData = viewModel.fetchDetailedAnimeData(relation.id)
                            if (detailedData != null) {
                                onOpenAnimeDetailFromManga(detailedData.toAnimeMedia())
                            } else {
                                context.toast(context.getString(R.string.app_anime_not_found))
                            }
                        } catch (_: Exception) {
                            context.toast(context.getString(R.string.app_anime_not_found))
                        }
                    }
                } else {
                    openMangaDetail(
                        MangaMedia(
                            id = relation.id,
                            title = relation.title,
                            cover = relation.cover,
                            totalChapters = relation.chapters ?: 0,
                            averageScore = relation.averageScore,
                            format = relation.format
                        )
                    )
                    state.autoShowChapters = false
                }
            }
        )
        is MangaOverlay.AllRecommendations -> MangaAllRecommendationsScreen(
            mangaId = mangaOv.mangaId,
            malId = mangaOv.malId,
            mangaTitle = mangaOv.mangaTitle,
            mangaTitleEnglish = mangaOv.mangaTitleEnglish,
            preferEnglishTitles = preferEnglishTitles,
            viewModel = viewModel,
            onDismiss = { state.overlay = MangaOverlay.None },
            onNavigateBack = { state.overlay = MangaOverlay.None },
            onRecommendationClick = { rec ->
                // Suspend the grid so it can be restored when back returns to this manga
                state.overlayRestoreStack = state.overlayRestoreStack +
                    MangaOverlay.AllRecommendations(mangaOv.mangaId, mangaOv.mangaTitle, mangaOv.mangaTitleEnglish, mangaOv.malId)
                state.overlay = MangaOverlay.None
                openMangaDetail(rec)
                state.autoShowChapters = false
            }
        )
        else -> {}
    }
}
