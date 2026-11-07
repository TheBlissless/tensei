package com.blissless.tensei.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blissless.tensei.data.models.MangaMedia

/**
 * State holder for the manga navigation stack: the chain of open manga detail
 * screens, the reader dialog layered on top of them, and the "All characters /
 * staff / relations / recommendations" full-screen grids.
 *
 * Previously these were ~12 `remember` locals inside
 * [com.blissless.tensei.MainScreen]. Hoisting them into one class (mirroring
 * [com.blissless.tensei.player.playback.PlaybackState]) keeps snapshot-state
 * semantics while letting the manga UI live in its own host composable
 * ([MangaNavHost]) instead of the root screen body.
 *
 * The [currentManga] plus [showDetailScreen] / [showReader] flags describe the
 * full navigation layout; [overlayRestoreStack] keeps "All *" grids alive in
 * the back stack while a deeper detail screen is open (see
 * [restoreOverlayIfPending]).
 */
class MangaNavState {

    /** Stack of open manga details; the last entry is the visible one. */
    var detailStack by mutableStateOf<List<MangaMedia>>(emptyList())

    /** True while a manga detail screen is shown. */
    var showDetailScreen by mutableStateOf(false)

    /** True while the reader dialog is layered on top of the detail screen. */
    var showReader by mutableStateOf(false)

    /** Chapter the reader opens at (-1 = show the chapter list first). */
    var readerChapterIndex by mutableIntStateOf(0)

    /** True when the detail screen should start with the chapter list expanded. */
    var autoShowChapters by mutableStateOf(false)

    /** The currently open "All *" manga grid, or [MangaOverlay.None]. */
    var overlay by mutableStateOf<MangaOverlay>(MangaOverlay.None)

    /**
     * "All *" grids that were suspended while navigating deeper (e.g. tapping a
     * relation opens a new manga detail / anime detail). Restored when back
     * returns to the grid's owning manga so the grid stays in the back stack.
     */
    var overlayRestoreStack by mutableStateOf<List<MangaOverlay>>(emptyList())

    /** True when no manga source/extension is configured (drives the redirect dialog). */
    var showNoExtensionDialog by mutableStateOf(false)

    /** Manga parked while the user goes to Settings to configure a source; resumed on return. */
    var pendingAfterSettings by mutableStateOf<MangaMedia?>(null)

    /** Whether the parked [pendingAfterSettings] should resume at the last read chapter. */
    var pendingResumeAfterSettings by mutableStateOf(false)

    /** The visible (top-of-stack) manga, or null when no detail is open. */
    val currentManga: MangaMedia? get() = detailStack.lastOrNull()

    /** Pushes a manga detail onto the stack and makes it visible. */
    fun openDetail(manga: MangaMedia) {
        detailStack = detailStack + manga
        showDetailScreen = true
    }

    /** Pops the top manga detail, restoring a suspended "All *" grid if it belongs to the now-top manga. */
    fun popDetail() {
        val remaining = detailStack.dropLast(1)
        detailStack = remaining
        if (remaining.isEmpty()) {
            showDetailScreen = false
            overlayRestoreStack = emptyList()
        } else {
            restoreOverlayIfPending()
        }
    }

    /** Clears the whole manga navigation stack (details, reader and grids). */
    fun closeAll() {
        detailStack = emptyList()
        showDetailScreen = false
        showReader = false
        overlay = MangaOverlay.None
        overlayRestoreStack = emptyList()
    }

    /** Restores the top suspended "All *" grid when back returns to its owning manga. */
    fun restoreOverlayIfPending() {
        val pending = overlayRestoreStack.lastOrNull() ?: return
        val current = detailStack.lastOrNull()
        if (current != null && overlay == MangaOverlay.None && pending.mangaId == current.id) {
            overlay = pending
            overlayRestoreStack = overlayRestoreStack.dropLast(1)
        }
    }
}
