package com.blissless.tensei.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.blissless.tensei.mal.MalMangaListEntry
import com.blissless.tensei.mal.LoginProvider
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.manga.MangaDexManager
import com.blissless.tensei.data.manga.MangaRepository
import com.blissless.tensei.data.manga.MangaTrackManager
import com.blissless.tensei.extensions.manga.MangaExtensionManager
import com.blissless.tensei.extensions.manga.MangaSourceApi
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import com.blissless.tensei.data.models.MangaActivityNode
import com.blissless.tensei.data.models.MangaChapter
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaExploreMedia
import com.blissless.tensei.data.models.MangaFavorite
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaTrack
import com.blissless.tensei.data.models.chapterBaseNumber
import com.blissless.tensei.data.models.countDistinctChapters
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.json.JSONObject

// Manga tracking and AniList sync: debounced sync queue, scroll-progress
// persistence, completion reconciliation, status/score/progress updates and
// MAL id resolution.



/** Reload the local manga tracking lists (Continue Reading / Planning / Completed). */
fun MainViewModel.refreshMangaTracking() {
    loadLocalMangaTracking()
}


fun MainViewModel.updateMangaProgress(mangaId: Int, progress: Float) {
    mangaTrackManager?.updateChapterProgress(mangaId, progress)
    loadLocalMangaTracking()
}


// â”€â”€â”€ Manga â†’ AniList sync queue (local-first, debounced) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Mirrors the anime queueSync/executePendingSyncs pattern: the UI is updated
// immediately from local tracking and the AniList mutation is pushed in the
// background after a short debounce. Rapid changes for the same manga coalesce
// into a single mutation (status/progress fields merge, latest non-null wins).
// Failed pushes are re-queued so they retry on the next debounce cycle.

private data class PendingMangaSync(
    val type: String,
    val mediaId: Int,
    val status: String? = null,
    val progress: Int? = null,
    val entryId: Int? = null,
    val score: Int? = null,
    val malId: Int? = null
)


private val pendingMangaSyncs = mutableMapOf<Int, PendingMangaSync>()

private var mangaSyncJob: Job? = null


// Scroll-progress persistence is throttled so the reader doesn't write SharedPreferences
// (full JSON encode of every track) on every scroll frame â€” that per-frame write, plus the
// repeated threshold-crossing work, was the source of jank once the autosync threshold hit.
private const val MANGA_SCROLL_PERSIST_INTERVAL_MS = 500L

private const val MANGA_SYNC_RETRY_MS = 30_000L

private var lastMangaScrollPersistTime = 0L


// Mangas whose local track was already ensured this session. Lets the first real scroll
// progress create the Continue Reading track without decoding all tracks from prefs on
// every frame just to check existence.
private val mangaTrackEnsured = mutableSetOf<Int>()


// Chapters already marked read + pushed to AniList this session, keyed "$mangaId:$chapterId".
// The threshold-crossing path (track writes, tracking-list refresh, debounced sync re-queue)
// must run once per chapter â€” otherwise every scroll frame past the threshold re-runs it.
private val mangaReadSyncedChapters = mutableSetOf<String>()


private fun MainViewModel.queueMangaSync(
    mediaId: Int,
    type: String,
    status: String? = null,
    progress: Int? = null,
    entryId: Int? = null,
    score: Int? = null,
    malId: Int? = null
) {
    val existing = pendingMangaSyncs[mediaId]
    pendingMangaSyncs[mediaId] = PendingMangaSync(
        type = type,
        mediaId = mediaId,
        status = status ?: existing?.status,
        progress = progress ?: existing?.progress,
        entryId = entryId ?: existing?.entryId,
        score = score ?: existing?.score,
        malId = malId ?: existing?.malId
    )
    mangaSyncJob?.cancel()
    // Run the push on a background dispatcher. The mutation is a network call, and the
    // post-push refresh (fetchMangaLists) merges every AniList row into local tracking with
    // per-item SharedPreferences reads/writes â€” none of that may run on the main thread, or
    // the reader janks ~2s after progress crosses the sync threshold.
    mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
        delay(MainViewModel.SYNC_DEBOUNCE_MS)
        // Run the push to completion even if a newer sync is queued mid-flight.
        // Cancelling the executing job would abort the network call in progress and
        // skip the re-queue for failed pushes â€” rapid chapter flips (next-chapter
        // button) hit this constantly, silently dropping AniList updates.
        withContext(NonCancellable) { executeMangaPendingSyncs() }
    }
}


/**
 * Flush any pending manga â†’ AniList syncs immediately, skipping the debounce.
 * Called when the reader closes so progress read in the last few seconds isn't
 * lost if the app is killed within the debounce window.
 */
fun MainViewModel.flushMangaSync() {
    if (pendingMangaSyncs.isEmpty()) return
    mangaSyncJob?.cancel()
    // Background dispatcher â€” see queueMangaSync for why the sync must not run on main.
    mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
        withContext(NonCancellable) { executeMangaPendingSyncs() }
    }
}


private suspend fun MainViewModel.executeMangaPendingSyncs() {
    val syncs = pendingMangaSyncs.toMap()
    pendingMangaSyncs.clear()
    if (syncs.isEmpty()) return

    val token = authToken.value
    var didPush = false
    for ((_, sync) in syncs) {
        val malMangaId = resolveMalMangaId(sync.mediaId)
        val ok = when (sync.type) {
            "status", "progress" -> {
                val status = sync.status
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.status
                    ?: "CURRENT"
                var result = true
                // Push to AniList whenever AniList is active (alone or as part of BOTH).
                if (isAniListActive && token != null) {
                    val aniListId = resolveMangaAniListIdForPush(sync)
                    if (aniListId != sync.mediaId) rekeyMangaTrackIfNeeded(sync.mediaId, aniListId, sync.malId)
                    val r = mangaRepository?.updateMangaStatus(aniListId, status, token, sync.progress, score = sync.score)
                    result = result && (r == true)
                }
                // Push to MAL whenever MAL is active (using the AniList manga's idMal).
                if (isMalActive && malMangaId != null) {
                    val malStatus = mapMangaStatusToMal(status)
                    val r = malApiService.updateMangaStatus(
                        malMangaId,
                        malStatus?.takeIf { sync.type != "progress" },
                        sync.score?.let { (it / 10f).roundToInt().coerceIn(0, 10) },
                        sync.progress
                    )
                    result = result && r
                }
                result
            }
            "score" -> {
                val score = sync.score
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.score
                    ?: 0
                val status = sync.status
                    ?: mangaTrackManager?.getTrack(sync.mediaId)?.status
                    ?: "CURRENT"
                var result = true
                if (isAniListActive && token != null) {
                    val aniListId = resolveMangaAniListIdForPush(sync)
                    if (aniListId != sync.mediaId) rekeyMangaTrackIfNeeded(sync.mediaId, aniListId, sync.malId)
                    val r = mangaRepository?.updateMangaStatus(aniListId, status, token, sync.progress, score = score)
                    result = result && (r == true)
                }
                if (isMalActive && malMangaId != null) {
                    val r = malApiService.updateMangaStatus(
                        malMangaId,
                        mapMangaStatusToMal(status),
                        (score / 10f).roundToInt().coerceIn(0, 10),
                        sync.progress
                    )
                    result = result && r
                }
                result
            }
            "delete" -> {
                var result = true
                if (isAniListActive && token != null) {
                    val entryId = sync.entryId
                    if (entryId != null) {
                        val r = mangaRepository?.deleteMangaListEntry(entryId, token)
                        result = result && (r == true)
                    }
                }
                if (isMalActive) {
                    // Prefer the MAL id captured at queue time: after a delete the manga is removed
                    // from the local lists, so on-demand resolution (resolveMalMangaId) can no longer
                    // find it. Fall back to resolution for syncs that predate the new field.
                    val malMangaId = sync.malId ?: resolveMalMangaId(sync.mediaId)
                    if (malMangaId != null) {
                        result = result && malApiService.deleteMangaFromList(malMangaId)
                    }
                }
                result
            }
            else -> null
        }
        if (ok == true) {
            didPush = true
        } else {
            // Re-queue the failed sync so it retries on the next debounce cycle
            pendingMangaSyncs[sync.mediaId] = sync
        }
    }

    if (didPush) {
        // Refresh the lists so local + remote stay in sync. Prefer AniList; fall back to MAL.
        if (isAniListActive && _userId.value != null) {
            fetchMangaLists()
        } else if (isMalActive) {
            fetchMalMangaList()
        }
        loadLocalMangaTracking()
        // After any manga change (status, progress, or delete), reconcile MAL to keep it in exact
        // parity with AniList — prunes MAL-only entries and mirrors status/score/progress.
        if (isBothActive && !userPreferences.malAsMainProvider.value) {
            runCrossProviderDiffSync()
        }
    }

    // Retry anything that failed (e.g. the AniList API being down during the debounce) on a
    // short cycle so the change lands automatically once the API recovers — a failed push is
    // otherwise only retried when the user makes another change.
    if (pendingMangaSyncs.isNotEmpty()) {
        mangaSyncJob?.cancel()
        mangaSyncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(MANGA_SYNC_RETRY_MS)
            withContext(NonCancellable) { executeMangaPendingSyncs() }
        }
    }
}


/** Resolve the MAL manga id for an AniList manga (the manga's `idMal`). */
private fun MainViewModel.resolveMalMangaId(animeMangaId: Int): Int? {
    _mangaDetail.value?.takeIf { it.id == animeMangaId }?.malId?.let { return it }
    val inLists = _mangaContinueReading.value + _mangaCurrentlyReading.value +
        _mangaPlanningToRead.value + _mangaCompleted.value +
        _mangaPaused.value + _mangaDropped.value
    return inLists.firstOrNull { it.id == animeMangaId }?.malId
}


/**
 * Resolve the real AniList manga id for a pending AniList push. Manga opened/added through a
 * MAL fallback (search/explore/detail while AniList was down) carry the MAL id as their own
 * id, and AniList has no Media under that number — pushing it unchanged would fail forever and
 * the manga would never land on AniList. Whenever the local id is really a MAL id
 * (idMal == id, as threaded through [MainViewModel.updateMangaStatus]), the id is translated
 * to AniList's id via the idMal lookup. Genuine AniList ids are returned unchanged.
 */
private suspend fun MainViewModel.resolveMangaAniListIdForPush(sync: PendingMangaSync): Int {
    val mediaId = sync.mediaId
    val known = _mangaContinueReading.value + _mangaCurrentlyReading.value +
        _mangaPlanningToRead.value + _mangaCompleted.value +
        _mangaPaused.value + _mangaDropped.value
    // A list entry whose id is mediaId with a DIFFERENT malId → genuine AniList id.
    known.firstOrNull { it.id == mediaId && it.malId != null && it.malId != mediaId }?.let { return mediaId }
    // A list entry that holds mediaId as its MAL id → that entry's own id is the AniList id
    // (covers reader/progress pushes for a manga that was already re-keyed).
    known.firstOrNull { it.malId != null && it.malId == mediaId }?.let { e ->
        if (e.id != mediaId) return e.id
    }
    // Id verbatim from a caller that carried the manga's detail context (falls back to the
    // track's stored malId). Null → no way to translate, keep the id as-is.
    val track = mangaTrackManager?.getTrack(mediaId)
    val malId = sync.malId ?: track?.malId
    if (malId == null || malId <= 0) return mediaId
    if (malId != mediaId) return mediaId // genuine AniList id with a different MAL id.
    // mediaId == malId → MAL-origin entry added through a fallback (its local id IS the MAL
    // id). Present in the lists under this id does NOT make it genuine — translate by idMal so
    // the add actually lands on AniList's entry for this manga.
    val resolved = mangaRepository?.findMangaByMalId(malId, authToken.value)
    val aniListId = resolved?.id?.takeIf { it > 0 } ?: mediaId
    if (aniListId != mediaId) {
    }
    return aniListId
}


/**
 * Re-key a local manga track from its MAL-origin id to its real AniList id once the id is
 * known, so the AniList list merge can't create a duplicate track and future pushes reuse the
 * correct id. Read-chapter bookkeeping moves with the id so already-synced chapters are not
 * pushed twice after the re-key.
 */
private fun MainViewModel.rekeyMangaTrackIfNeeded(oldId: Int, newId: Int, malId: Int?) {
    if (oldId == newId) return
    val tracker = mangaTrackManager ?: return
    val track = tracker.getTrack(oldId) ?: return
    tracker.removeTrack(oldId)
    tracker.addTrack(track.copy(mangaId = newId, malId = malId ?: track.malId))
    mangaTrackEnsured.remove(oldId)
    mangaTrackEnsured.add(newId)
    val prefixOld = "$oldId:"
    val moved = mangaReadSyncedChapters.filter { it.startsWith(prefixOld) }
    mangaReadSyncedChapters.removeAll { it.startsWith(prefixOld) }
    mangaReadSyncedChapters.addAll(moved.map { newId.toString() + it.removePrefix(prefixOld) })
    loadLocalMangaTracking()
}


/** Map an AniList manga status (CURRENT/PLANNING/COMPLETED/PAUSED/DROPPED) to MAL. */
internal fun mapMangaStatusToMal(status: String): String? {
    return when (status) {
        "CURRENT" -> "reading"
        "PLANNING" -> "plan_to_read"
        "COMPLETED" -> "completed"
        "PAUSED" -> "on_hold"
        "DROPPED" -> "dropped"
        else -> null
    }
}


/**
 * Handle scroll progress from the reader. This mirrors oni's onChapterScrollProgress flow:
 * - Always save scroll progress locally
 * - When scroll reaches the threshold (default 90%), mark the chapter as read
 * - If the chapter number is an integer (not partial like 12.5), push the new progress
 *   to AniList through the debounced background sync queue (rapid chapter flips coalesce
 *   into a single mutation)
 *
 * @param mangaId The AniList manga ID
 * @param chapter The chapter being read
 * @param scrollPercent 0.0 - 1.0 scroll progress
 * @param mangaTitle Title for track creation
 * @param mangaCover Cover URL for track creation
 */
fun MainViewModel.onMangaScrollProgress(
    mangaId: Int,
    chapter: MangaChapter?,
    scrollPercent: Float,
    mangaTitle: String = "",
    mangaCover: String = ""
): Boolean {
    if (chapter == null) return false
    if (!scrollPercent.isFinite()) return false

    // Always save scroll progress locally (this also lazily creates the track on first progress)
    updateMangaScrollProgress(mangaId, scrollPercent, mangaTitle, mangaCover, chapter)

    val threshold = userPreferences.mangaSyncThreshold.value / 100f
    if (scrollPercent >= threshold) {
        // Mark the chapter read (and push to AniList) only ONCE per chapter â€” on the first frame
        // that crosses the threshold. Without this guard every subsequent scroll frame re-runs the
        // track writes, the tracking-list StateFlow refresh, and the debounced sync re-queue,
        // which janks the reader once the threshold is reached.
        val readKey = "$mangaId:${chapter.chapterId}"
        if (mangaReadSyncedChapters.add(readKey)) {
            // Mark chapter as read (creates track if needed, updates local progress)
            markMangaChapterRead(mangaId, chapter, mangaTitle, mangaCover)
            // The chapter's stop position is deliberately KEPT here: crossing the threshold
            // marks the chapter read, but the Continue Reading card still follows the reader
            // into it, so a finished (or re-read) chapter can still be resumed mid-way.
            // A released (FINISHED) manga that reaches its final chapter is completed
            // automatically; on-going manga never is — new chapters may still arrive.
            reconcileCompletion(mangaId, chapter.chapterNumber, reading = true)

            // Schedule the AniList progress push through the debounced sync queue.
            // Only for integer chapter numbers (skip partial chapters like 12.5)
            val chapterNum = chapter.chapterNumber
            val isIntegerChapter = chapterNum > 0f && chapterNum == chapterNum.toInt().toFloat()
            if (isIntegerChapter) {
                queueMangaSync(mangaId, "progress", progress = chapterNum.toInt())
            }
            return true
        }
    }
    return false
}


/**
 * Complete a manga automatically whenever a RELEASED (media status FINISHED) manga reaches
 * its final chapter: flip the local track to COMPLETED and push the status change. This is the
 * one shared rule — the reader sync threshold, the change-status dialog, and the startup sweep
 * all funnel into it, so a finished manga is never silently left at 100% under CURRENT while a
 * still-running (RELEASING) series caught up to its latest release is never falsely completed.
 */
private fun MainViewModel.reconcileCompletion(mangaId: Int, progressOverride: Float = -1f, reading: Boolean = false) {
    val track = mangaTrackManager?.getTrack(mangaId) ?: return
    if (track.status == "COMPLETED") return
    val finished = track.mediaStatus == "FINISHED" || _mangaDetail.value?.status == "FINISHED"
    if (!finished) return
    if (track.totalChapters <= 0) return
    val effectiveProgress = if (progressOverride >= 0f) progressOverride else track.progress
    if (effectiveProgress < track.totalChapters) return
    // Reading a chapter implies an active CURRENT manga. For dialog-driven changes only a
    // CURRENT manga is auto-completed, so an explicit PAUSED/DROPPED/PLANNING choice is never
    // overridden.
    // Already-COMPLETED is rejected above. Any other status (CURRENT / PLANNING / PAUSED /
    // DROPPED) is eligible for auto-completion when the progress reaches the final chapter of
    // a FINISHED manga — both the dialog path (reading = false) and the auto-tracking threshold
    // path (reading = true) converge here. Mirrors reconcileAnimeCompletion on the anime side.
    // (The `reading` parameter is preserved for callers that want to know the source, but no
    // longer gates the auto-complete rule.)
    mangaTrackManager?.updateTrackingStatus(mangaId, "COMPLETED")
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "status", status = "COMPLETED", progress = track.totalChapters)
}


/** Startup/refresh safety net: sweep every locally-tracked CURRENT manga for the completion rule. */
internal fun MainViewModel.reconcileAllCompletions() {
    val tracker = mangaTrackManager ?: return
    var changed = false
    for (track in tracker.allTracks()) {
        // Sweep every non-COMPLETED track so an anime/manga the user finished elsewhere (e.g. on
        // the AniList website) under a non-CURRENT status also gets auto-completed locally.
        if (track.status == "COMPLETED") continue
        if (track.mediaStatus != "FINISHED") continue
        if (track.totalChapters <= 0 || track.progress < track.totalChapters) continue
        tracker.updateTrackingStatus(track.mangaId, "COMPLETED")
        queueMangaSync(track.mangaId, "status", status = "COMPLETED", progress = track.totalChapters)
        changed = true
    }
    if (changed) {
        loadLocalMangaTracking()
    }
}


/** Set the AniList sync threshold (75-100%). Persists across restarts. */
fun MainViewModel.setMangaSyncThreshold(percent: Int) {
    userPreferences.setMangaSyncThreshold(percent)
}


fun MainViewModel.updateMangaScrollProgress(mangaId: Int, scrollProgress: Float, mangaTitle: String = "", mangaCover: String = "", chapter: MangaChapter? = null, force: Boolean = false) {
    // Guard against NaN/Infinity — same defensive pattern as oni's TrackingManager
    val safe = if (scrollProgress.isNaN() || scrollProgress.isInfinite()) 0f else scrollProgress
    // In-chapter resume card: created lazily on the FIRST real scroll (past page 1), so closing
    // the reader mid-chapter BEFORE the sync threshold leaves a "continue at this spot" card.
    // Opening a chapter alone (scroll stays 0) still never tracks. A chapter that is ALREADY read
    // — progress covers it, or the sync threshold was crossed this session — saves its stop
    // position all the same: re-reading a chapter the user has already finished still gets a
    // Continue Reading card, exactly like an unread one. Progress/status updates are unaffected.
    if (safe > 0f && chapter != null && mangaId !in mangaTrackEnsured) {
        // First real scroll for this manga this session: create the track so the stop position
        // has somewhere to live.
        mangaTrackManager?.ensureTrack(mangaId, mangaTitle, mangaCover)
        mangaTrackEnsured.add(mangaId)
    }
    // Persist scroll progress — throttled, because writing SharedPreferences (full JSON encode of
    // all tracks) on every scroll frame is the jank source once the reader is at/over the sync
    // threshold. Resets to 0 (opening a non-resume chapter) are rare and must land immediately so
    // stale Continue Reading cards clear correctly; the final persisted value of a scroll gesture
    // stays within one interval of the actual position, which is plenty for resume. The chapter is
    // stored alongside the fraction so Continue Reading stays attached to the chapter the scroll
    // was made in.
    //
    // The throttle is bypassed when force = true, which the reader uses on dispose so the
    // Continue Reading card reflects the page the user was actually on when they exited —
    // without this, a page change followed by an exit within 500ms would leave the persisted
    // scrollProgress at the PREVIOUS page (the snapshotFlow only emits on change, so a steady
    // position never re-fires the throttled write).
    val now = SystemClock.elapsedRealtime()
    if (force || safe <= 0f || now - lastMangaScrollPersistTime >= MANGA_SCROLL_PERSIST_INTERVAL_MS) {
        mangaTrackManager?.updateScrollProgress(mangaId, safe, chapter)
        lastMangaScrollPersistTime = now
    }
}


/** Persist the page count of the chapter currently being read so home can show "pages left". */
fun MainViewModel.updateMangaChapterPages(mangaId: Int, pages: Int) {
    if (pages <= 0) return
    mangaTrackManager?.updateChapterPages(mangaId, pages)
}


fun MainViewModel.updateMangaStatus(mangaId: Int, status: String, progress: Int? = null, score: Int? = null, malId: Int? = null, title: String = "", cover: String = "") {
    val effectiveStatus = status.ifBlank { "CURRENT" }
    // Local-first: apply the change immediately so the UI reacts instantly, then
    // queue the AniList push for the background debounced sync. Create the track WITH the
    // manga's title/cover so the home/library cards render real data instead of an empty
    // placeholder (a bare status change used to mint a blank track).
    mangaTrackManager?.ensureTrack(mangaId, title = title, cover = cover, malId = malId)
    mangaTrackManager?.updateTrackingStatus(mangaId, effectiveStatus)
    if (progress != null) {
        mangaTrackManager?.updateChapterProgress(mangaId, progress.toFloat())
    }
    if (score != null) {
        mangaTrackManager?.updateScore(mangaId, score)
    }
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "status", status = effectiveStatus, progress = progress, score = score, malId = malId)
    // Re-apply the auto-complete rule after the dialog: a CURRENT manga whose progress now
    // sits on its final chapter of a FINISHED (released) series becomes COMPLETED. An explicit
    // PAUSED/DROPPED/PLANNING choice is untouched.
    reconcileCompletion(mangaId, progress?.toFloat() ?: -1f, reading = false)
}


/** Set the AniList score (0-100) for a manga, local-first with a debounced remote push. */
fun MainViewModel.updateMangaScore(mangaId: Int, score: Int, malId: Int? = null) {
    mangaTrackManager?.updateScore(mangaId, score)
    loadLocalMangaTracking()
    queueMangaSync(mangaId, "score", score = score, malId = malId)
}


fun MainViewModel.removeMangaTracking(mangaId: Int) {
    // Resolve the AniList list-entry id and the MAL id (needed by the remote deletes) from the
    // local track or the in-memory lists BEFORE the track is removed. Without the entry id the
    // AniList entry survives, and without the MAL id the MAL entry survives (deletion happens on a
    // debounced background queue, by which point the manga is gone from the local lists and can no
    // longer be resolved) — each would be silently resurrected / left behind.
    val track = mangaTrackManager?.getTrack(mangaId)
    val knownEntryId = track?.listEntryId
        ?: listOf(
            _mangaContinueReading.value,
            _mangaCurrentlyReading.value,
            _mangaPlanningToRead.value,
            _mangaCompleted.value,
            _mangaPaused.value,
            _mangaDropped.value
        ).flatten().firstOrNull { it.id == mangaId }?.listEntryId
    val knownMalId = track?.malId
        ?: listOf(
            _mangaContinueReading.value,
            _mangaCurrentlyReading.value,
            _mangaPlanningToRead.value,
            _mangaCompleted.value,
            _mangaPaused.value,
            _mangaDropped.value
        ).flatten().firstOrNull { it.id == mangaId }?.malId
    mangaTrackManager?.removeTrack(mangaId)
    // Forget the ensure-track flag so re-reading this manga later recreates its local track.
    mangaTrackEnsured.remove(mangaId)
    mangaReadSyncedChapters.removeAll { it.startsWith("$mangaId:") }
    loadLocalMangaTracking()

    // Both AniList and MAL need their own id for the remote delete. If we resolved both, queue
    // immediately. If either is missing (older track without malId, or track never saw a list
    // sync), fetch the AniList list once to fill in the blanks before deleting from each provider.
    // We must always know the MAL id — on-demand resolution after removal can't find it in the
    // (now empty) local lists, so the MAL entry would be left behind.
    if (knownEntryId != null && knownMalId != null) {
        queueMangaSync(mangaId, "delete", entryId = knownEntryId, malId = knownMalId)
    } else {
        viewModelScope.launch {
            val token = authToken.value
            val userId = _userId.value
            if (token != null && userId != null) {
                val fetched = mangaRepository?.fetchUserMangaLists(userId, token)
                val listEntry = fetched?.values?.flatten()?.firstOrNull { it.id == mangaId }
                val entryId = listEntry?.listEntryId ?: knownEntryId
                val malId = listEntry?.malId ?: knownMalId
                if (entryId != null || malId != null) {
                    queueMangaSync(mangaId, "delete", entryId = entryId, malId = malId)
                }
            }
        }
    }
}


/**
 * Dismiss a manga from the Continue Reading row only. Clears the in-chapter reading
 * state so the resume card disappears, but keeps the manga in its status list
 * (mirrors anime's removeContinueWatchingEntry, which doesn't untrack the anime).
 */
fun MainViewModel.dismissMangaContinueReading(mangaId: Int) {
    mangaTrackManager?.clearChapterProgress(mangaId)
    loadLocalMangaTracking()
}
