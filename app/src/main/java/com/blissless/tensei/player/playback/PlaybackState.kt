package com.blissless.tensei.player.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.blissless.tensei.TenseiApplication
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.data.models.EpisodeStreams
import com.blissless.tensei.data.models.QualityOption
import com.blissless.tensei.data.models.ServerInfo
import com.blissless.tensei.network.Endpoints
import com.blissless.tensei.torrent.StreamEntry
import com.blissless.tensei.torrent.TorrentEngine
import com.blissless.tensei.torrent.TorrentStreamServer
import com.blissless.tensei.viewmodel.ExtensionStreamResult
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient

/**
 * Holder for all state that belongs to the in-app episode player pipeline.
 *
 * Previously these were ~55 individual `remember { mutableStateOf(...) }`
 * locals inside the [com.blissless.tensei.MainScreen] composable. Hoisting
 * them into a single plain class keeps the same snapshot-state semantics
 * (each property is a `mutableStateOf` delegate, so reads are still tracked
 * by the composer) while letting the playback pipeline live outside the
 * composable body — see [PlaybackController].
 *
 * The class is created once per composition via [rememberPlaybackState] and
 * is never replaced, so property identity is stable across recompositions.
 */
class PlaybackState(val torrentEngine: TorrentEngine) {

    // ─── Player visibility / lifecycle ─────────────────────────────────────
    /** True while the player region occupies the screen. */
    var showPlayer by mutableStateOf(false)

    /** True when the player fills the whole window, false for the 16:9 mini layout. */
    var playerFullscreen by mutableStateOf(true)

    /** Guards auto-refresh attempts so only one runs at a time. */
    var isAutoRefreshing by mutableStateOf(false)

    /** A seek position to apply once the player is ready, or null. */
    var pendingSeekPosition by mutableStateOf<Long?>(null)

    // ─── Current stream ────────────────────────────────────────────────────
    var currentVideoUrl by mutableStateOf<String?>(null)
    var currentReferer by mutableStateOf(Endpoints.DEFAULT_REFERER)
    var currentSubtitleUrl by mutableStateOf<String?>(null)

    var currentAnime by mutableStateOf<AnimeMedia?>(null)
    var currentEpisode by mutableIntStateOf(0)
    var totalEpisodes by mutableIntStateOf(0)

    var isLoadingStream by mutableStateOf(false)
    var loadingJob by mutableStateOf<Job?>(null)
    var streamError by mutableStateOf<String?>(null)

    /** Name of the server currently being tried, shown in the loading overlay. */
    var currentServerAttempt by mutableStateOf<String?>(null)
    var currentServerAttemptIsFallback by mutableStateOf(false)

    var currentEpisodeInfo by mutableStateOf<EpisodeStreams?>(null)
    var currentEpisodeTitle by mutableStateOf<String?>(null)

    /** True after the next episode has been prefetched post tracking-sync. */
    var hasPrefetchedNextOnTracking by mutableStateOf(false)

    // ─── Server / category selection ───────────────────────────────────────
    var currentCategory by mutableStateOf("sub")
    var currentServerName by mutableStateOf("")
    var currentServerIndex by mutableIntStateOf(0)
    var isFallbackStream by mutableStateOf(false)
    var requestedCategory by mutableStateOf("sub")
    var actualCategory by mutableStateOf("sub")
    var isManualServerChange by mutableStateOf(false)
    var isChangingEpisode by mutableStateOf(false)

    /** Bumped to force the player to reload the (possibly changed) stream URL. */
    var episodeTrigger by mutableIntStateOf(0)

    var currentQualityOptions by mutableStateOf<List<QualityOption>>(emptyList())
    var currentQuality by mutableStateOf("Auto")

    /** Position restored from the watch-progress store for the current episode. */
    var savedPlaybackPosition by mutableLongStateOf(0L)

    // ─── Aniyomi extension flow ────────────────────────────────────────────
    var extensionVideos by mutableStateOf<List<Video>?>(null)
    var extensionHosters by mutableStateOf<List<Hoster>?>(null)
    var showExtHosterDialog by mutableStateOf(false)
    var showExtVideoDialog by mutableStateOf(false)
    var pendingExtResult by mutableStateOf<ExtensionStreamResult?>(null)
    var isExtensionFlow by mutableStateOf(false)
    var extensionOkHttpClient by mutableStateOf<OkHttpClient?>(null)
    var extensionVideoHeaders by mutableStateOf<Map<String, String>>(emptyMap())
    var extensionSourcePackage by mutableStateOf("")
    var extensionEpisodeUrl by mutableStateOf("")
    var extensionEpisodeNumber by mutableIntStateOf(0)
    var extensionServers by mutableStateOf(emptyList<ServerInfo>())
    var extensionStreamEntries by mutableStateOf<List<StreamEntry>>(emptyList())
    var extensionName by mutableStateOf("")
    var currentSubtitleTracks by mutableStateOf<List<Track>>(emptyList())
    var cachedExtensionNext by mutableStateOf<ExtensionStreamResult?>(null)

    /** Per-session cache of already resolved extension results, keyed by episode. */
    val episodeCache = mutableMapOf<Int, ExtensionStreamResult>()

    // ─── Torrent (magnet) flow ─────────────────────────────────────────────
    /** The HTTP server that serves the sequentially-downloaded torrent file. */
    var torrentStreamServer by mutableStateOf<TorrentStreamServer?>(null)
    var currentTorrentListener by mutableStateOf<TorrentEngine.EngineListener?>(null)
    var currentTorrentFileSize by mutableLongStateOf(0L)

    // ─── Misc playback flags ───────────────────────────────────────────────
    /** True when playback cannot start because no extension/stream source is configured. */
    var showNoExtDialog by mutableStateOf(false)

    // Animekai intro/outro timestamps (primary source for skip ranges).
    var animekaiIntroStart by mutableStateOf<Int?>(null)
    var animekaiIntroEnd by mutableStateOf<Int?>(null)
    var animekaiOutroStart by mutableStateOf<Int?>(null)
    var animekaiOutroEnd by mutableStateOf<Int?>(null)
}

/** Creates the single [PlaybackState] instance owned by the root screen. */
@Composable
fun rememberPlaybackState(): PlaybackState {
    val context = LocalContext.current
    val app = context.applicationContext as TenseiApplication
    return androidx.compose.runtime.remember { PlaybackState(app.torrentEngine) }
}
