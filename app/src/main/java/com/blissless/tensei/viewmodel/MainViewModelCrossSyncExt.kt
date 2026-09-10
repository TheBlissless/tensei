package com.blissless.tensei.viewmodel

import com.blissless.tensei.MainViewModel
import com.blissless.tensei.data.models.MangaMedia
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import com.blissless.tensei.R

/** State of the cross-provider copy prompt. When [visible] is true, the UI shows an overlay
 * dialog asking the user to pick a copy direction. [markDoneOnDismiss] says whether dismissing
 * the prompt counts as the one-time post-login question being handled: the app-startup ask uses
 * false so it re-appears on the next start, while the one-time post-login prompt uses true.
 * [aniToMalChanges]/[malToAniChanges] are the number of entries that would change on each
 * provider, so the dialog can show how far out of sync each side is. The ...Anime/...Manga
 * fields break that total down by type so the dialog can show exactly what differs. */
data class CrossProviderCopyPrompt(
    val visible: Boolean,
    val markDoneOnDismiss: Boolean = true,
    val aniToMalChanges: Int = 0,
    val malToAniChanges: Int = 0,
    val aniToMalAnime: Int = 0,
    val aniToMalManga: Int = 0,
    val malToAniAnime: Int = 0,
    val malToAniManga: Int = 0
)

/**
 * Frozen prompt for the one-time cross-provider copy. When [visible] is true, the UI shows an
 * overlay dialog asking the user to pick a copy direction. Null until both providers are logged in.
 */
private val _crossProviderCopyPrompt = MutableStateFlow<CrossProviderCopyPrompt?>(null)
val MainViewModel.crossProviderCopyPrompt: StateFlow<CrossProviderCopyPrompt?>
    get() = _crossProviderCopyPrompt.asStateFlow()

/** Lightweight (status, score, progress) snapshot of a user's list entry on one provider. */
private data class ListSnapshot(
    val status: String?,
    val score: Int,
    val progress: Int
)

/** Live progress of a running cross-provider copy. [isRunning] false means idle. */
data class CrossProviderCopyProgress(
    val isRunning: Boolean = false,
    val text: String = "",
    val processed: Int = 0,
    val total: Int = 0
)

private val _crossProviderCopyProgress = MutableStateFlow<CrossProviderCopyProgress>(CrossProviderCopyProgress())
val MainViewModel.crossProviderCopyProgress: StateFlow<CrossProviderCopyProgress>
    get() = _crossProviderCopyProgress.asStateFlow()

private const val SYNC_NOTIFICATION_CHANNEL = "cross_provider_sync"
private const val SYNC_NOTIFICATION_ID = 2002

private fun ensureSyncNotificationChannel(context: Context) {
    try {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(SYNC_NOTIFICATION_CHANNEL, "Cross-provider sync", NotificationManager.IMPORTANCE_DEFAULT)
        )
    } catch (_: Exception) {
    }
}

@SuppressLint("MissingPermission")
private fun showSyncProgressNotification(context: Context, text: String, processed: Int, total: Int) {
    try {
        if (!hasNotificationPermission(context)) return
        ensureSyncNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val indeterminate = total <= 0
        val notification = NotificationCompat.Builder(context, SYNC_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_large))
            .setContentTitle("Syncing across providers")
            .setContentText(if (indeterminate) text else "$text  ($processed / $total)")
            .setProgress(if (indeterminate) 100 else total, if (indeterminate) 0 else processed, indeterminate)
            .setOngoing(true)
            .setSilent(true)
            .build()
        nm.notify(SYNC_NOTIFICATION_ID, notification)
    } catch (_: Exception) {
    }
}

@SuppressLint("MissingPermission")
private fun showSyncCompleteNotification(context: Context, text: String) {
    try {
        if (!hasNotificationPermission(context)) return
        ensureSyncNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(SYNC_NOTIFICATION_ID)
        val notification = NotificationCompat.Builder(context, SYNC_NOTIFICATION_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification_large))
            .setContentTitle("Sync complete")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        nm.notify(SYNC_NOTIFICATION_ID, notification)
    } catch (_: Exception) {
    }
}

private fun cancelSyncNotification(context: Context) {
    try {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(SYNC_NOTIFICATION_ID)
    } catch (_: Exception) {
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Offer the one-time copy when BOTH providers are logged in. Called after login callbacks.
 * Shows the overlay prompt once (guarded by a persisted flag) so the user can pick a direction,
 * or skip. After the copy, ongoing diff-sync keeps the two providers reconciled.
 */
fun MainViewModel.offerCrossProviderSync() {
    if (!isBothActive) return
    if (userPreferences.isCrossProviderCopyDone()) {
        // One-time copy already handled — ensure ongoing diff-sync is running.
        viewModelScope.launch { runCrossProviderDiffSync() }
        return
    }
    // Only ask when the two providers' lists actually differ; identical lists need no dialog.
    viewModelScope.launch {
        val counts = computeCrossProviderDiffCounts()
        if (counts != null && (counts.aniToMal > 0 || counts.malToAniList > 0)) {
            _crossProviderCopyPrompt.value = CrossProviderCopyPrompt(
                visible = true,
                aniToMalChanges = counts.aniToMal,
                malToAniChanges = counts.malToAniList,
                aniToMalAnime = counts.aniToMalAnime,
                aniToMalManga = counts.aniToMalManga,
                malToAniAnime = counts.malToAniListAnime,
                malToAniManga = counts.malToAniListManga
            )
        }
    }
}

/**
 * Force-show the copy dialog, bypassing the one-time flag. Used on app startup while both
 * providers are logged in so the user can manually decide whether to sync. [markDoneOnDismiss]
 * must be false here so dismissing on startup never suppresses the one-time post-login question.
 * The dialog only appears when the two providers' lists actually differ.
 */
fun MainViewModel.showCrossProviderCopyDialog(markDoneOnDismiss: Boolean = true) {
    if (!isBothActive) return
    viewModelScope.launch {
        val counts = computeCrossProviderDiffCounts()
        if (counts != null && (counts.aniToMal > 0 || counts.malToAniList > 0)) {
            _crossProviderCopyPrompt.value = CrossProviderCopyPrompt(
                visible = true,
                markDoneOnDismiss = markDoneOnDismiss,
                aniToMalChanges = counts.aniToMal,
                malToAniChanges = counts.malToAniList,
                aniToMalAnime = counts.aniToMalAnime,
                aniToMalManga = counts.aniToMalManga,
                malToAniAnime = counts.malToAniListAnime,
                malToAniManga = counts.malToAniListManga
            )
        }
    }
}

/** Dismiss the copy prompt without performing a copy ("Don't sync existing entries"). */
fun MainViewModel.dismissCrossProviderCopyPrompt() {
    val markDone = _crossProviderCopyPrompt.value?.markDoneOnDismiss ?: true
    _crossProviderCopyPrompt.value = CrossProviderCopyPrompt(visible = false)
    // Mark done so the one-time post-login prompt doesn't keep nagging (the app-startup ask
    // opts out of this); new changes still apply to both providers via the live write-through sync.
    if (markDone) {
        userPreferences.setCrossProviderCopyDone(true)
    }
}

/**
 * Per-direction change counts between the two providers, split by anime/manga. [aniToMal] is how
 * many entries would change on MAL if AniList were the source of truth; [malToAniList] is the
 * same measured the other way. Syncing now makes the target MIRROR the source exactly: a value
 * mismatch on an entry present on both sides counts toward BOTH directions (each provider would
 * receive the other's value), and an entry present on exactly one side also counts toward BOTH —
 * the source that has it would add it to the other provider, while the direction using the other
 * provider as source would remove it (the target no longer keeps entries the source lacks).
 */
private data class CrossProviderDiffCounts(
    val aniToMal: Int = 0,
    val malToAniList: Int = 0,
    val aniToMalAnime: Int = 0,
    val aniToMalManga: Int = 0,
    val malToAniListAnime: Int = 0,
    val malToAniListManga: Int = 0
)

/**
 * Computes how many entries differ between the two providers in each direction (anime + manga).
 * Returns null when the lists cannot be compared (either provider unreachable), in which case
 * the cross-provider prompt is not shown — there is nothing meaningful to sync to an unreachable
 * provider, and both directions of the copy abort in the same situation.
 */
private suspend fun MainViewModel.computeCrossProviderDiffCounts(): CrossProviderDiffCounts? {
    val tag = "CrossSync"

    // AniList truth (also refreshes the in-memory anime lists).
    if (!fetchLists()) {
        android.util.Log.w(tag, "diff-counts: AniList fetch failed — cannot compare, no dialog shown")
        return null
    }
    val aniAnime = _currentlyWatching.value + _planningToWatch.value +
        _completed.value + _onHold.value + _dropped.value
    val aniAnimeByMalId = aniAnime.mapNotNull { a ->
        a.malId?.let { m -> m to ListSnapshot(a.listStatus, a.userScore ?: 0, a.progress) }
    }.toMap()
    val aniMangaByMalId = fetchAllAniListManga().mapNotNull { m ->
        m.malId?.let { id -> id to ListSnapshot(m.listStatus, m.userScore ?: 0, m.progress) }
    }.toMap()

    // MAL truth.
    val malAnime = try {
        malApiService.getAnimeListWithWeb()
    } catch (e: Exception) {
        android.util.Log.w(tag, "diff-counts: MAL anime fetch failed — ${e.message}", e)
        return null
    }
    val malManga = try {
        malApiService.getMangaListWithWeb()
    } catch (e: Exception) {
        android.util.Log.w(tag, "diff-counts: MAL manga fetch failed — ${e.message}", e)
        return null
    }

    var toMal = 0
    var toAniList = 0
    var toMalAnime = 0
    var toAniAnime = 0
    var toMalManga = 0
    var toAniManga = 0

    // Anime.
    val malAnimeIds = malAnime.map { it.node.id }.toSet()
    for (e in malAnime) {
        val t = aniAnimeByMalId[e.node.id]
        val differs = t == null ||
            t.status != mapFromMalStatus(e.list_status?.status) ||
            t.score != (e.list_status?.score ?: 0) ||
            t.progress != (e.list_status?.num_episodes_watched ?: 0)
        if (differs) {
            // MAL → AniList: the entry is added to AniList (t == null) or updated there.
            toAniList++
            toAniAnime++
            // AniList → MAL: the entry is updated on MAL (t != null) or, having no
            // counterpart on AniList, REMOVED from MAL.
            toMal++
            toMalAnime++
            android.util.Log.d(tag, "diff-counts anime: MAL vs AniList differ (malId=${e.node.id} onAniList=${t != null} title=${e.node.title})")
        }
    }
    // Entries only on AniList affect BOTH directions: they would be added to MAL, and (being
    // absent from MAL) removed from AniList when MAL is the source of truth.
    for (malId in aniAnimeByMalId.keys) {
        if (malId !in malAnimeIds) {
            toMal++
            toMalAnime++
            toAniList++
            toAniAnime++
            android.util.Log.d(tag, "diff-counts anime: on AniList but not MAL — malId=$malId")
        }
    }

    // Manga.
    val malMangaIds = malManga.map { it.node.id }.toSet()
    for (e in malManga) {
        val t = aniMangaByMalId[e.node.id]
        val differs = t == null ||
            t.status != mapMangaStatusFromMal(e.list_status?.status) ||
            t.score != (e.list_status?.score ?: 0) ||
            t.progress != (e.list_status?.num_chapters_read ?: 0)
        if (differs) {
            toAniList++
            toAniManga++
            // Same as anime: updated on MAL, or REMOVED from MAL when absent on AniList.
            toMal++
            toMalManga++
            android.util.Log.d(tag, "diff-counts manga: MAL vs AniList differ (malId=${e.node.id} onAniList=${t != null} title=${e.node.title})")
        }
    }
    // Entries only on AniList affect BOTH directions (add to MAL / remove from AniList).
    for (malId in aniMangaByMalId.keys) {
        if (malId !in malMangaIds) {
            toMal++
            toMalManga++
            toAniList++
            toAniManga++
            android.util.Log.d(tag, "diff-counts manga: on AniList but not MAL — malId=$malId")
        }
    }

    android.util.Log.d(tag, "diff-counts: aniToMal=$toMal (anime=$toMalAnime manga=$toMalManga) malToAniList=$toAniList (anime=$toAniAnime manga=$toAniManga)")
    return CrossProviderDiffCounts(
        aniToMal = toMal,
        malToAniList = toAniList,
        aniToMalAnime = toMalAnime,
        aniToMalManga = toMalManga,
        malToAniListAnime = toAniAnime,
        malToAniListManga = toAniManga
    )
}

/**
 * Apply the chosen copy direction and run the one-time copy for anime + manga. Also marks the
 * chosen provider as the main list source for the home screen.
 * @param toMal true = AniList is main and copies AniList â†’ MAL; false = MAL is main and copies MAL â†’ AniList.
 */
fun MainViewModel.applyCrossProviderCopy(toMal: Boolean) {
    _crossProviderCopyPrompt.value = CrossProviderCopyPrompt(visible = false)
    userPreferences.setCrossProviderCopyDone(true)
    // Persist the main list provider; it drives which list populates the home screen.
    userPreferences.setMalAsMainProvider(!toMal)
    viewModelScope.launch(Dispatchers.IO) {
        android.util.Log.d("CrossSync", "applyCrossProviderCopy: direction=${if (toMal) "AniList->MAL" else "MAL->AniList"} " +
            "aniListActive=$isAniListActive malActive=$isMalActive")
        val startMessage = if (toMal) "Syncing AniList → MAL…" else "Syncing MAL → AniList…"
        _crossProviderCopyProgress.value = CrossProviderCopyProgress(
            isRunning = true,
            text = startMessage,
            processed = 0,
            total = 0
        )
        showSyncProgressNotification(context, startMessage, 0, 0)
        withContext(Dispatchers.Main.immediate) {
            viewModelScope.launch { _toastMessage.emit(startMessage) }
        }
        var pushed = 0
        var skipped = 0
        try {
            if (toMal) {
                val r = copyAniListToMal()
                pushed = r.first
                skipped = r.second
            } else {
                val r = copyMalToAniList()
                pushed = r.first
                skipped = r.second
            }
        } catch (e: Exception) {
            // Individual item failures are handled internally; swallow structural errors.
            android.util.Log.w("CrossSync", "cross-provider copy failed: ${e.message}", e)
        }
        _crossProviderCopyProgress.value = CrossProviderCopyProgress(
            isRunning = false,
            text = "",
            processed = pushed,
            total = pushed + skipped
        )
        cancelSyncNotification(context)
        if (pushed > 0) {
            val doneMessage = if (toMal) "Synced $pushed changes from AniList to MAL" else "Synced $pushed changes from MAL to AniList"
            showSyncCompleteNotification(context, doneMessage)
            withContext(Dispatchers.Main.immediate) {
                android.util.Log.d("CrossSync", "cross-provider copy complete (toMal=$toMal)")
                viewModelScope.launch { _toastMessage.emit(doneMessage) }
            }
        } else {
            android.util.Log.d("CrossSync", "cross-provider copy finished: nothing changed (pushed=$pushed skipped=$skipped), no notification shown")
        }
    }
}

// â”€â”€â”€ One-time copy: AniList â†’ MAL â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private suspend fun MainViewModel.copyAniListToMal(): Pair<Int, Int> {
    val tag = "CrossSync"
    // Anime
    val allAnime = _currentlyWatching.value + _planningToWatch.value +
        _completed.value + _onHold.value + _dropped.value
    val allManga = fetchAllAniListManga()
    val total = allAnime.size + allManga.size
    android.util.Log.d(tag, "copyAniListToMal: anime entries=${allAnime.size}, " +
        "animeListsLoaded=${_currentlyWatching.value.size + _planningToWatch.value.size + _completed.value.size + _onHold.value.size + _dropped.value.size} manga=${allManga.size}")
    var processed = 0
    var pushed = 0
    var skipped = 0
    val updateProgress: suspend () -> Unit = {
        _crossProviderCopyProgress.value = CrossProviderCopyProgress(
            isRunning = true,
            text = "Syncing AniList → MAL…",
            processed = processed,
            total = total
        )
        showSyncProgressNotification(context, "Syncing AniList → MAL…", processed, total)
    }

    // Pull MAL's current anime/manga lists (website load.json, falling back to /v2) so we only
    // overwrite entries whose status/score/progress differ, or that are new on MAL.
    val malAnimeMap = try {
        malApiService.getAnimeListWithWeb().associate { it.node.id to it }
    } catch (e: Exception) {
        android.util.Log.w(tag, "copyAniListToMal: getAnimeListWithWeb failed: ${e.message}", e)
        emptyMap()
    }
    val malMangaMap = try {
        malApiService.getMangaListWithWeb().associate { it.node.id to it }
    } catch (e: Exception) {
        android.util.Log.w(tag, "copyAniListToMal: getMangaListWithWeb failed: ${e.message}", e)
        emptyMap()
    }

    for (anime in allAnime) {
        processed++
        updateProgress()
        val malId = anime.malId
        if (malId == null) { skipped++; continue }
        val malStatus = mapToMalStatus(anime.listStatus)
        val score = anime.userScore
        val mal = malAnimeMap[malId]
        val isNew = mal == null
        val differs = isNew ||
            mal?.list_status?.status != malStatus ||
            (mal?.list_status?.score ?: 0) != (score ?: 0) ||
            (mal?.list_status?.num_episodes_watched ?: 0) != anime.progress
        if (!differs) {
            android.util.Log.d(tag, "copyAniListToMal anime UNCHANGED (skip): malId=$malId title=${anime.title}")
            skipped++
            continue
        }
        android.util.Log.d(tag, "copyAniListToMal anime${if (isNew) " (new)" else ""}: title=${anime.title} malId=$malId " +
            "AL[status=${anime.listStatus}->mal=$malStatus score=$score progress=${anime.progress}] " +
            "MAL[status=${mal?.list_status?.status} score=${mal?.list_status?.score} progress=${mal?.list_status?.num_episodes_watched}] differs=$differs")
        try {
            val updated = malApiService.updateAnimeStatus(malId, malStatus, score, anime.progress)
            if (updated) {
                pushed++
            } else {
                android.util.Log.w(tag, "copyAniListToMal anime REJECTED by MAL malId=$malId title=${anime.title}")
                skipped++
            }
        } catch (e: Exception) {
            android.util.Log.w(tag, "copyAniListToMal FAILED anime malId=$malId title=${anime.title}: ${e.message}", e)
            skipped++
        }
        delay(1000)
    }
    android.util.Log.d(tag, "copyAniListToMal: anime done pushed=$pushed skipped=$skipped")

    // Manga - use the AniList manga list directly (it carries the manga's idMal), because the
    // local-tracking flows lose `malId` when they are rebuilt from the tracking store.
    android.util.Log.d(tag, "copyAniListToMal: manga entries=${allManga.size}")
    for (manga in allManga) {
        processed++
        updateProgress()
        val malMangaId = manga.malId
        if (malMangaId == null) { skipped++; continue }
        val malStatus = mapMangaStatusToMal(manga.listStatus)
        // Scores are kept on AniList's 0-10 scale and passed through as-is, matching the
        // anime path. (AniList's GraphQL `score` returns the per-account format; for a
        // 10-point account a 10/10 is `10`, not `100`. The previous `/10` here halved
        // every rating, pushing e.g. 10 -> 1 onto MAL.)
        val malScore = manga.userScore?.coerceIn(0, 10)
        val mal = malMangaMap[malMangaId]
        val isNew = mal == null
        val differs = isNew ||
            mal?.list_status?.status != malStatus ||
            (mal?.list_status?.score ?: 0) != (malScore ?: 0) ||
            (mal?.list_status?.num_chapters_read ?: 0) != manga.progress
        if (!differs) {
            android.util.Log.d(tag, "copyAniListToMal manga UNCHANGED (skip): malId=$malMangaId title=${manga.title}")
            skipped++
            continue
        }
        android.util.Log.d(tag, "copyAniListToMal manga${if (isNew) " (new)" else ""}: title=${manga.title} malId=$malMangaId " +
            "AL[status=${manga.listStatus}->mal=$malStatus score=$malScore progress=${manga.progress}] " +
            "MAL[status=${mal?.list_status?.status} score=${mal?.list_status?.score} chapters=${mal?.list_status?.num_chapters_read}] differs=$differs")
        try {
            val updated = malApiService.updateMangaStatus(
                malMangaId,
                malStatus,
                malScore,
                manga.progress
            )
            if (updated) {
                pushed++
            } else {
                android.util.Log.w(tag, "copyAniListToMal manga REJECTED by MAL malId=$malMangaId title=${manga.title}")
                skipped++
            }
        } catch (e: Exception) {
            android.util.Log.w(tag, "copyAniListToMal FAILED manga malId=$malMangaId title=${manga.title}: ${e.message}", e)
            skipped++
        }
        delay(1000)
    }
    android.util.Log.d(tag, "copyAniListToMal: manga done pushed=$pushed skipped=$skipped")

    // Remove entries that exist on MAL but not on AniList so the target mirrors the source
    // exactly. Only prune when the AniList source came back non-empty — an empty/failed fetch
    // must never wipe MAL.
    val animeLoaded = allAnime.isNotEmpty()
    val animeSourceMalIds = allAnime.mapNotNull { it.malId }.toSet()
    if (animeLoaded) {
        for ((malId, mal) in malAnimeMap) {
            if (malId !in animeSourceMalIds) {
                android.util.Log.d(tag, "copyAniListToMal anime DELETE (not on AniList): malId=$malId title=${mal.node.title}")
                try {
                    if (malApiService.deleteAnimeFromList(malId)) pushed++ else skipped++
                } catch (e: Exception) {
                    android.util.Log.w(tag, "copyAniListToMal anime DELETE FAILED malId=$malId: ${e.message}", e)
                    skipped++
                }
                delay(1000)
            }
        }
    } else {
        android.util.Log.w(tag, "copyAniListToMal: AniList anime list empty — skipping anime parity cleanup to avoid wiping MAL")
    }

    val mangaLoaded = allManga.isNotEmpty()
    val mangaSourceMalIds = allManga.mapNotNull { it.malId }.toSet()
    if (mangaLoaded) {
        for ((malId, mal) in malMangaMap) {
            if (malId !in mangaSourceMalIds) {
                android.util.Log.d(tag, "copyAniListToMal manga DELETE (not on AniList): malId=$malId title=${mal.node.title}")
                try {
                    if (malApiService.deleteMangaFromList(malId)) pushed++ else skipped++
                } catch (e: Exception) {
                    android.util.Log.w(tag, "copyAniListToMal manga DELETE FAILED malId=$malId: ${e.message}", e)
                    skipped++
                }
                delay(1000)
            }
        }
    } else {
        android.util.Log.w(tag, "copyAniListToMal: AniList manga list empty — skipping manga parity cleanup to avoid wiping MAL")
    }

    android.util.Log.d(tag, "copyAniListToMal: complete pushed=$pushed skipped=$skipped")
    return pushed to skipped
}

/**
 * Fetch every AniList manga list entry (across all statuses) WITH its `malId` populated.
 * Unlike the local-tracking flows (which drop `malId` when rebuilt from the tracker), the
 * AniList list response carries idMal, so this is the correct source of truth for the
 * cross-provider manga copy/diff-sync. Merges the status-keyed map into a single list.
 */
private suspend fun MainViewModel.fetchAllAniListManga(): List<MangaMedia> {
    val userId = _userId.value ?: return emptyList()
    val token = authToken.value ?: return emptyList()
    val lists = mangaRepository?.fetchUserMangaLists(userId, token) ?: return emptyList()
    val current = lists["CURRENT"] ?: lists["Reading"] ?: emptyList()
    val planning = lists["PLANNING"] ?: lists["Plan to Read"] ?: emptyList()
    val completed = lists["COMPLETED"] ?: emptyList()
    val paused = lists["PAUSED"] ?: emptyList()
    val dropped = lists["DROPPED"] ?: emptyList()
    android.util.Log.d("CrossSync", "fetchAllAniListManga: current=${current.size} planning=${planning.size} " +
        "completed=${completed.size} paused=${paused.size} dropped=${dropped.size}")
    return current + planning + completed + paused + dropped
}

// â”€â”€â”€ One-time copy: MAL â†’ AniList â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private suspend fun MainViewModel.copyMalToAniList(): Pair<Int, Int> {
    val tag = "CrossSync"
    val token = authToken.value
    if (token == null) {
        android.util.Log.w(tag, "copyMalToAniList: no AniList token, aborting")
        return 0 to 0
    }

    // Source of truth for this direction: MAL's own lists.
    val malAnime = try {
        malApiService.getAnimeListWithWeb()
    } catch (e: Exception) {
        android.util.Log.w(tag, "copyMalToAniList: getAnimeList failed: ${e.message}", e)
        return 0 to 0
    }
    val malManga = try {
        malApiService.getMangaListWithWeb()
    } catch (e: Exception) {
        android.util.Log.w(tag, "copyMalToAniList: getMangaList failed: ${e.message}", e)
        return 0 to 0
    }

    // Target: refresh AniList's own lists so we only push entries whose status/score/progress
    // actually differ from the source. Without AniList's current state this sync would blindly
    // overwrite every entry — exactly what the diff-based sync is meant to avoid.
    val aniListLoaded = fetchLists()
    if (!aniListLoaded) {
        android.util.Log.w(tag, "copyMalToAniList: could not fetch AniList lists — aborting to avoid blind overwrite")
        withContext(Dispatchers.Main.immediate) {
            viewModelScope.launch { _toastMessage.emit("Couldn't reach AniList to compare — sync skipped") }
        }
        return 0 to 0
    }
    val aniAnimeMap = (_currentlyWatching.value + _planningToWatch.value +
        _completed.value + _onHold.value + _dropped.value).associateBy { it.id }
    val aniMangaAll = fetchAllAniListManga()
    val aniMangaByMalId = aniMangaAll.associateBy { it.malId }
    val malAnimeIds = malAnime.map { it.node.id }.toSet()
    val malMangaIds = malManga.map { it.node.id }.toSet()
    val total = malAnime.size + malManga.size
    android.util.Log.d(tag, "copyMalToAniList: anime entries=${malAnime.size} manga entries=${malManga.size} " +
        "target anime=${aniAnimeMap.size} target manga=${aniMangaByMalId.size}")
    var processed = 0
    var pushed = 0
    var skipped = 0
    val updateProgress: suspend () -> Unit = {
        _crossProviderCopyProgress.value = CrossProviderCopyProgress(
            isRunning = true,
            text = "Syncing MAL → AniList…",
            processed = processed,
            total = total
        )
        showSyncProgressNotification(context, "Syncing MAL → AniList…", processed, total)
    }

    // Anime: push only entries that are new on AniList or whose status/score/progress differ.
    for (entry in malAnime) {
        processed++
        updateProgress()
        val malId = entry.node.id
        val status = mapFromMalStatus(entry.list_status?.status)
        val progress = entry.list_status?.num_episodes_watched ?: 0
        val score = entry.list_status?.score ?: 0

        val animeId = resolveAnimeIdForMal(malId)
        if (animeId == null) {
            android.util.Log.d(tag, "copyMalToAniList: no AniList id resolved for malId=$malId, skipping")
            skipped++
            continue
        }
        val target = aniAnimeMap[animeId]
        val isNew = target == null
        val differs = isNew ||
            target?.listStatus != status ||
            (target?.userScore ?: 0) != score ||
            (target?.progress ?: 0) != progress
        if (!differs) {
            android.util.Log.d(tag, "copyMalToAniList anime UNCHANGED (skip): malId=$malId title=${entry.node.title}")
            skipped++
            continue
        }
        android.util.Log.d(tag, "copyMalToAniList anime${if (isNew) " (new)" else ""}: title=${entry.node.title} malId=$malId " +
            "MAL[status=$status score=$score progress=$progress] " +
            "AL[status=${target?.listStatus} score=${target?.userScore} progress=${target?.progress}] differs=$differs")
        try {
            queueSync(animeId, "status", malId = malId, status = status, progress = progress, score = score)
            pushed++
        } catch (e: Exception) {
            android.util.Log.w(tag, "copyMalToAniList FAILED queued anime malId=$malId: ${e.message}", e)
            skipped++
        }
        delay(1000)
    }
    android.util.Log.d(tag, "copyMalToAniList: anime done pushed=$pushed skipped=$skipped")

    // Manga: load MAL entries into the local tracker so new entries can resolve their AniList key,
    // then push only entries that are new on AniList or whose status/score/progress differ.
    try {
        fetchMalMangaList()
    } catch (e: Exception) {
        android.util.Log.w(tag, "copyMalToAniList: fetchMalMangaList failed: ${e.message}", e)
        return pushed to skipped
    }
    val mangaRepository = this.mangaRepository
    if (mangaRepository == null) {
        android.util.Log.w(tag, "copyMalToAniList: mangaRepository null, skipping manga")
        return pushed to skipped
    }
    for (entry in malManga) {
        processed++
        updateProgress()
        val malId = entry.node.id
        val anilistStatus = mapMangaStatusFromMal(entry.list_status?.status)
        val progress = entry.list_status?.num_chapters_read ?: 0
        val score = entry.list_status?.score?.takeIf { it > 0 } ?: 0
        val target = aniMangaByMalId[malId]
        val isNew = target == null
        val differs = isNew ||
            target?.listStatus != anilistStatus ||
            (target?.progress ?: 0) != progress ||
            (target?.userScore ?: 0) != score
        if (!differs) {
            android.util.Log.d(tag, "copyMalToAniList manga UNCHANGED (skip): malId=$malId title=${entry.node.title}")
            skipped++
            continue
        }
        val anilistId = target?.id ?: resolveMangaIdForMal(malId)
        if (anilistId == null) {
            android.util.Log.d(tag, "copyMalToAniList manga: no AniList id resolved for malId=$malId, skipping")
            skipped++
            continue
        }
        android.util.Log.d(tag, "copyMalToAniList manga${if (isNew) " (new)" else ""}: anilistId=$anilistId malId=$malId " +
            "MAL[status=$anilistStatus score=$score progress=$progress] " +
            "AL[status=${target?.listStatus} score=${target?.userScore} progress=${target?.progress}] differs=$differs")
        try {
            mangaRepository.updateMangaStatus(
                anilistId, anilistStatus, token,
                progress = progress.takeIf { it > 0 },
                score = score.takeIf { it > 0 }
            )
            pushed++
        } catch (e: Exception) {
            android.util.Log.w(tag, "copyMalToAniList FAILED manga anilistId=$anilistId malId=$malId: ${e.message}", e)
            skipped++
        }
        delay(1000)
    }
    android.util.Log.d(tag, "copyMalToAniList: manga done pushed=$pushed skipped=$skipped")

    // Remove entries that exist on AniList but not on MAL so the target mirrors the source
    // exactly. Only prune when the MAL source came back non-empty — an empty/failed fetch must
    // never wipe AniList. Entries whose MAL id could not be resolved (null) are left alone.
    if (malAnime.isNotEmpty()) {
        for (anime in aniAnimeMap.values) {
            val deleteMalId = anime.malId
            if (deleteMalId == null || deleteMalId in malAnimeIds) continue
            android.util.Log.d(tag, "copyMalToAniList anime DELETE (not on MAL): id=${anime.id} malId=$deleteMalId title=${anime.title}")
            val entryId = anime.listEntryId
            if (entryId == null) {
                android.util.Log.w(tag, "copyMalToAniList anime DELETE SKIP (no listEntryId): id=${anime.id} title=${anime.title}")
                skipped++
                continue
            }
            try {
                if (repository.deleteListEntry(entryId)) pushed++ else skipped++
            } catch (e: Exception) {
                android.util.Log.w(tag, "copyMalToAniList anime DELETE FAILED id=${anime.id} malId=$deleteMalId: ${e.message}", e)
                skipped++
            }
            delay(1000)
        }
    } else {
        android.util.Log.w(tag, "copyMalToAniList: MAL anime list empty — skipping anime parity cleanup to avoid wiping AniList")
    }

    val malMangaLoaded = malManga.isNotEmpty()
    if (malMangaLoaded) {
        for (manga in aniMangaAll) {
            val deleteMalId = manga.malId
            if (deleteMalId == null || deleteMalId in malMangaIds) continue
            android.util.Log.d(tag, "copyMalToAniList manga DELETE (not on MAL): malId=$deleteMalId title=${manga.title}")
            val entryId = manga.listEntryId
            if (entryId == null || mangaRepository == null) {
                android.util.Log.w(tag, "copyMalToAniList manga DELETE SKIP (no listEntryId): malId=$deleteMalId title=${manga.title}")
                skipped++
                continue
            }
            try {
                if (mangaRepository.deleteMangaListEntry(entryId, token)) pushed++ else skipped++
            } catch (e: Exception) {
                android.util.Log.w(tag, "copyMalToAniList manga DELETE FAILED malId=$deleteMalId: ${e.message}", e)
                skipped++
            }
            delay(1000)
        }
    } else {
        android.util.Log.w(tag, "copyMalToAniList: MAL manga list empty — skipping manga parity cleanup to avoid wiping AniList")
    }

    android.util.Log.d(tag, "copyMalToAniList: complete pushed=$pushed skipped=$skipped")
    return pushed to skipped
}

/** Map a MAL anime id â†’ matching AniList anime id using cached detail, falling back to the id itself. */
private suspend fun MainViewModel.resolveAnimeIdForMal(malId: Int): Int? {
    cacheManager.detailedAnimeCache.value.values.firstOrNull { it.malId == malId }?.id?.let { return it }
    val inLists = _currentlyWatching.value + _planningToWatch.value +
        _completed.value + _onHold.value + _dropped.value
    val found = inLists.firstOrNull { it.id == malId || it.malId == malId }
    android.util.Log.d("CrossSync", "resolveAnimeIdForMal: malId=$malId cached=${cacheManager.detailedAnimeCache.value.size} " +
        "inListCount=${inLists.size} -> ${found?.id} (id=${found?.id} malId=${found?.malId})")
    return found?.id
}

// â”€â”€â”€ Ongoing diff-sync â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Reconcile diverged entries between AniList and MAL. Called on app start (when both active)
 * and on manual "Sync now" from settings. Uses AniList as the source of truth (home prefers it)
 * and pushes the AniList value to MAL whenever the two diverge. Individual failures are skipped
 * and retried on the next run.
 */
internal suspend fun MainViewModel.runCrossProviderDiffSync() {
    android.util.Log.d("CrossSync", "diff-sync: START (bothActive=$isBothActive, malAsMain=${userPreferences.malAsMainProvider.value})")
    if (!isBothActive) {
        android.util.Log.d("CrossSync", "diff-sync: return (not both active)")
        return
    }
    // Diff-sync treats AniList as the source of truth. When MAL is the user's chosen main
    // provider, skip it so it doesn't overwrite the MAL-first lists — the directional startup
    // sync handles reconciliation instead.
    if (userPreferences.malAsMainProvider.value) {
        android.util.Log.d("CrossSync", "diff-sync: return (MAL is main provider)")
        return
    }

    // Anime
    val malByAnimeId = mutableMapOf<Int, ListSnapshot>()
    try {
        malApiService.getAnimeListWithWeb().forEach { e ->
            malByAnimeId[e.node.id] = ListSnapshot(
                status = e.list_status?.status,
                score = e.list_status?.score ?: 0,
                progress = e.list_status?.num_episodes_watched ?: 0
            )
        }
    } catch (e: Exception) {
        android.util.Log.w("CrossSync", "diff-sync anime: failed to fetch MAL anime list — ${e.message}", e)
    }
    val allAnime = _currentlyWatching.value + _planningToWatch.value +
        _completed.value + _onHold.value + _dropped.value
    // Safety guard: if no anime is present locally, treat it as a load failure rather than an
    // empty list — otherwise the parity cleanup below would wipe every MAL anime entry.
    val animeListLoaded = allAnime.isNotEmpty()
    // Track every MAL id present on AniList so entries on MAL but NOT on AniList can be
    // removed below — keeping the two lists in exact parity.
    val malIdsOnAniListAnime = mutableSetOf<Int>()
    for (anime in allAnime) {
        val malId = anime.malId ?: continue
        malIdsOnAniListAnime += malId
        val mal = malByAnimeId[malId]
        // Push if missing on MAL (new entry) or any field differs.
        val differs = mal == null ||
            mal.status != mapToMalStatus(anime.listStatus) ||
            mal.score != (anime.userScore ?: 0) ||
            mal.progress != anime.progress
        if (differs) {
            android.util.Log.d("CrossSync",
                "diff-sync anime${if (mal == null) " (new)" else ""}: malId=$malId title=${anime.title} " +
                    "status=${mapToMalStatus(anime.listStatus)} score=${anime.userScore} progress=${anime.progress}")
            try {
                malApiService.updateAnimeStatus(malId, mapToMalStatus(anime.listStatus), anime.userScore, anime.progress)
            } catch (e: Exception) {
                android.util.Log.w("CrossSync",
                    "diff-sync anime PUSH FAILED malId=$malId title=${anime.title}: ${e.message}", e)
            }
            delay(1000)
        }
    }

    // Remove MAL anime entries that have no counterpart on AniList. Skips if AniList didn't load.
    if (animeListLoaded) {
        for ((malId, mal) in malByAnimeId) {
            if (malId !in malIdsOnAniListAnime) {
                android.util.Log.d("CrossSync",
                    "diff-sync anime DELETE (not on AniList): malId=$malId status=${mal.status}")
                try {
                    malApiService.deleteAnimeFromList(malId)
                } catch (e: Exception) {
                    android.util.Log.w("CrossSync",
                        "diff-sync anime DELETE FAILED malId=$malId: ${e.message}", e)
                }
                delay(1000)
            }
        }
    } else {
        android.util.Log.w("CrossSync", "diff-sync anime: AniList returned empty list — skipping parity cleanup to avoid wiping MAL")
    }

    // Manga
    val malMangaByMalId = mutableMapOf<Int, ListSnapshot>()
    try {
        malApiService.getMangaListWithWeb().forEach { e ->
            malMangaByMalId[e.node.id] = ListSnapshot(
                status = e.list_status?.status,
                score = e.list_status?.score ?: 0,
                progress = e.list_status?.num_chapters_read ?: 0
            )
        }
    } catch (e: Exception) {
        android.util.Log.w("CrossSync", "diff-sync manga: failed to fetch MAL manga list — ${e.message}", e)
    }
    android.util.Log.d("CrossSync", "diff-sync manga: MAL entries=${malMangaByMalId.size}")
    val allManga = fetchAllAniListManga()
    // Safety guard: if AniList returned nothing, treat it as a fetch failure rather than an empty
    // list — otherwise the parity cleanup below would wipe every MAL entry. Never prune on empty.
    val mangaListLoaded = allManga.isNotEmpty()
    // Track every MAL id present on AniList so entries on MAL but NOT on AniList can be
    // removed below — keeping the two lists in exact parity (items on AniList == items on MAL).
    val malIdsOnAniList = mutableSetOf<Int>()
    for (manga in allManga) {
        val malId = manga.malId
        if (malId == null) {
            android.util.Log.d("CrossSync",
                "diff-sync manga SKIP (no idMal): title=${manga.title}")
            continue
        }
        malIdsOnAniList += malId
        val mal = malMangaByMalId[malId]
        // 0-10 scale, passed through as-is (matches anime path; see copyAniListToMal).
        val anilistScore = (manga.userScore ?: 0).coerceIn(0, 10)
        val differs = mal == null ||
            mal.status != mapMangaStatusToMal(manga.listStatus) ||
            mal.score != anilistScore ||
            mal.progress != manga.progress
        if (differs) {
            android.util.Log.d("CrossSync",
                "diff-sync manga${if (mal == null) " (new)" else ""}: malId=$malId title=${manga.title}")
            try {
                malApiService.updateMangaStatus(
                    malId,
                    mapMangaStatusToMal(manga.listStatus),
                    anilistScore.coerceIn(0, 10),
                    manga.progress
                )
            } catch (e: Exception) {
                android.util.Log.w("CrossSync",
                    "diff-sync manga PUSH FAILED malId=$malId title=${manga.title}: ${e.message}", e)
            }
            delay(1000)
        }
    }

    // Remove MAL manga entries that have no counterpart on AniList (was deleted/removed
    // from AniList, or only ever created on MAL). This keeps the lists in exact parity.
    // Skips the cleanup entirely if AniList failed to load, to avoid wiping MAL on a hiccup.
    android.util.Log.d("CrossSync",
        "diff-sync manga: mangaListLoaded=$mangaListLoaded anilistMalIds=${malIdsOnAniList.size} malIds=${malMangaByMalId.size} maj=122663 in anilist=${122663 in malIdsOnAniList} in mal=${122663 in malMangaByMalId}")
    if (mangaListLoaded) {
        for ((malId, mal) in malMangaByMalId) {
            if (malId !in malIdsOnAniList) {
                android.util.Log.d("CrossSync",
                    "diff-sync manga DELETE (not on AniList): malId=$malId status=${mal.status}")
                try {
                    malApiService.deleteMangaFromList(malId)
                } catch (e: Exception) {
                    android.util.Log.w("CrossSync",
                        "diff-sync manga DELETE FAILED malId=$malId: ${e.message}", e)
                }
                delay(1000)
            }
        }
    } else {
        android.util.Log.w("CrossSync", "diff-sync manga: AniList returned empty list — skipping parity cleanup to avoid wiping MAL")
    }
    android.util.Log.d("CrossSync", "cross-provider diff-sync complete")
}

/** Bridge so the (otherwise private) manga queue is reachable from MainActivity's scope when needed. */
internal fun MainViewModel.triggerCrossProviderDiffSync() {
    viewModelScope.launch { runCrossProviderDiffSync() }
}
