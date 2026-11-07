package com.blissless.tensei.player.playback

import android.content.Context
import android.util.Log
import com.blissless.tensei.R
import com.blissless.tensei.data.anime.calculateRecursiveOffset
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.player.stream.PlayerData
import com.blissless.tensei.torrent.TorrentEngine
import com.blissless.tensei.torrent.TorrentStreamServer
import com.blissless.tensei.util.ErrorHandler
import com.blissless.tensei.util.toast
import com.blissless.tensei.viewmodel.ExtensionStreamResult
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.viewmodel.clearAnimeExtensionStreamCaches
import com.blissless.tensei.viewmodel.fetchExtensionHosterVideos
import com.blissless.tensei.viewmodel.fetchMagnetForEpisode
import com.blissless.tensei.viewmodel.fetchStreamUrlForEpisode
import com.blissless.tensei.viewmodel.getCachedTmdbEpisodes
import com.blissless.tensei.viewmodel.fetchTmdbEpisodes
import com.blissless.tensei.viewmodel.getMagnetForEpisode
import com.blissless.tensei.viewmodel.getPlaybackPosition
import com.blissless.tensei.viewmodel.invalidateStreamCache
import com.blissless.tensei.viewmodel.lastExtensionPlaybackError
import com.blissless.tensei.viewmodel.playEpisodeWithExtension
import com.blissless.tensei.viewmodel.removeFromVideoCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

/**
 * Orchestrates episode playback for the root screen.
 *
 * Owns no state itself — all mutable playback state lives in [state] (a
 * [PlaybackState] created once per composition), while long-lived data
 * operations delegate to [viewModel]. This is the class form of what used
 * to be ~25 local functions inside the [com.blissless.tensei.MainScreen]
 * composable; moving them here keeps the composable readable and makes the
 * three playback paths independently testable:
 *
 *  - **Tensei path** ([loadAndPlayEpisodeTensei]): magnet / ContentProvider
 *    extensions resolved via [com.blissless.tensei.viewmodel.MainViewModel]'s
 *    magnet extension client.
 *  - **Stream path** ([loadAndPlayEpisodeStream]): direct-URL stream
 *    extensions (`*.anime.stream`) that serve no magnets.
 *  - **Aniyomi path** ([loadAndPlayEpisodeAniyomi]): DexClassLoader-based
 *    AnimeCatalogueSource extensions.
 *
 * [loadAndPlayEpisode] dispatches between them based on the user's stream
 * method; [handleExtensionServerChange] dispatches mid-playback server
 * switches the same way.
 *
 * @param onMissingExtension invoked when playback cannot start because no
 *   usable extension/stream source is configured; the screen parks the
 *   (anime, episode) intent and redirects to Settings.
 */
class PlaybackController(
    val state: PlaybackState,
    private val viewModel: MainViewModel,
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMissingExtension: (anime: AnimeMedia, episode: Int) -> Unit,
) {
    private val torrentEngine: TorrentEngine get() = state.torrentEngine

    // ─── Stream cache invalidation ─────────────────────────────────────

    fun invalidateCurrentStreamCache() {
        state.currentAnime?.let { anime ->
            viewModel.invalidateStreamCache(anime.id, state.currentEpisode, state.currentCategory)
            viewModel.clearAnimeExtensionStreamCaches(anime.id)
            state.currentVideoUrl?.let { viewModel.removeFromVideoCache(it) }
        }
    }

    fun onPlaybackError() {
        invalidateCurrentStreamCache()
    }

    // ─── TMDB episode titles ───────────────────────────────────────────

    /**
     * Fetch the TMDB episode title for a given (anime, episode). Used as the
     * source of truth for the player's episode title — preferred over the
     * anime's title (which would duplicate the animeName shown in the top
     * bar and produce e.g. "Black Clover 2nd Season . Ep 1" where the
     * per-episode title should be).
     *
     * Reads from the in-memory cache first; on a miss, fetches from TMDB and
     * caches the result. Returns a sanitized title on success, or a localized
     * "Episode N" placeholder on failure.
     */
    suspend fun getTmdbEpisodeTitle(anime: AnimeMedia, episode: Int): String {
        val cachedEpisodes = viewModel.getCachedTmdbEpisodes(anime.id)
        if (cachedEpisodes != null) {
            val title = cachedEpisodes.find { it.episode == episode }?.title
            if (!title.isNullOrEmpty()) return sanitizeEpisodeTitle(title)
                ?: context.getString(R.string.app_episode_number, episode)
        }
        return try {
            val tmdbEpisodes = viewModel.fetchTmdbEpisodes(
                anime.title, anime.id, anime.year, anime.format,
                animeEpisodes = anime.totalEpisodes.takeIf { it > 0 },
            )
            val title = tmdbEpisodes.find { it.episode == episode }?.title
            sanitizeEpisodeTitle(title) ?: context.getString(R.string.app_episode_number, episode)
        } catch (_: Exception) {
            context.getString(R.string.app_episode_number, episode)
        }
    }

    // ─── Aniyomi extension playback ────────────────────────────────────

    /** Applies a resolved [ExtensionStreamResult] to the player state. */
    fun playExtensionVideo(result: ExtensionStreamResult, index: Int) {
        val video = result.videos.find { it.videoUrl == result.url }
            ?: result.videos.getOrNull(index)
        if (video == null) {
            Log.w("Playback", "playExtensionVideo: no matching video found, returning early")
            return
        }
        state.streamError = null
        state.currentEpisodeTitle = sanitizeEpisodeTitle(result.episode?.name)
            ?: context.getString(R.string.app_episode_number, state.currentEpisode)
        state.currentVideoUrl = result.url.ifEmpty { video.videoUrl }
        state.currentReferer = result.referer
        val preferredLang = viewModel.defaultSubtitleLang.value
        val sortedTracks = sortSubtitleTracks(video.subtitleTracks, preferredLang)
        state.currentSubtitleTracks = sortedTracks
        state.currentSubtitleUrl = sortedTracks.firstOrNull()?.url
        state.extensionName = result.source?.name ?: ""
        state.currentServerName = result.hosters?.firstOrNull()?.hosterName
            ?: state.extensionName.ifEmpty { context.getString(R.string.app_extension_fallback) }
        val hasDubHoster = result.hosters?.any { it.hosterName.contains("dub", ignoreCase = true) } == true
        state.currentCategory = if (hasDubHoster || result.videoTitle.contains("dub", ignoreCase = true)) "dub" else "sub"
        state.actualCategory = state.currentCategory
        state.requestedCategory = viewModel.preferredCategory.value
        state.currentQualityOptions = emptyList()
        state.currentQuality = "Auto"
        state.currentServerIndex = 0
        state.isExtensionFlow = false
        state.extensionOkHttpClient = result.extensionClient
        state.extensionVideoHeaders = result.videoHeaders
        // Build the server dropdown list. Prefer per-video entries (from
        // `result.videos`) when available — this shows each quality/stream
        // as a separate dropdown entry (e.g. "BEEP: 1080p (SUB) [Soft Subs]",
        // "NEKO: auto (SUB) [Hard Subs]", "YUKI: 1080p (DUB)", etc.).
        //
        // Falls back to `buildServerList(result.hosters)` when `result.videos`
        // is empty — this produces one entry per hoster (the old behavior).
        state.extensionServers = if (result.videos.isNotEmpty()) {
            buildServerListFromVideos(result.videos, result.videoHosterNames)
        } else {
            buildServerList(result.hosters)
        }
        state.showPlayer = true
        if (state.currentCategory == "dub" && result.source != null && result.episode != null) {
            val src = result.source
            val ep = result.episode
            scope.launch {
                val episodeVideos = withContext(Dispatchers.IO) {
                    try {
                        src.getVideoList(ep)
                    } catch (e: Throwable) {
                        ErrorHandler.report("Playback", "getVideoList failed", e)
                        emptyList()
                    }
                }
                val subVideo = episodeVideos.find {
                    it.videoTitle.contains("sub", ignoreCase = true) &&
                        !it.videoTitle.contains("dub", ignoreCase = true) &&
                        it.subtitleTracks.isNotEmpty()
                } ?: episodeVideos.find {
                    !it.videoTitle.contains("dub", ignoreCase = true) && it.subtitleTracks.isNotEmpty()
                }
                if (subVideo != null) {
                    state.currentSubtitleTracks = state.currentSubtitleTracks + subVideo.subtitleTracks
                    if (state.currentSubtitleUrl == null) {
                        state.currentSubtitleUrl = state.currentSubtitleTracks.firstOrNull()?.url
                    }
                }
            }
        }
    }

    // ─── Torrent (magnet) playback ─────────────────────────────────────

    /**
     * Starts streaming a magnet URI through the local torrent engine.
     *
     * Selects the video file matching [episode] from the torrent metadata
     * (falling back to the largest video), waits until enough contiguous
     * data is downloaded, then points the player at the local HTTP server.
     */
    fun playTorrent(
        magnetUri: String,
        anime: AnimeMedia,
        episode: Int,
        extensionSubtitles: List<eu.kanade.tachiyomi.animesource.model.Track> = emptyList(),
        episodeOffset: Int = 0,
    ) {
        state.isLoadingStream = true
        state.streamError = null
        state.torrentStreamServer?.stop()
        state.torrentStreamServer = null

        val engine = torrentEngine
        if (!engine.isRunning.get()) engine.start()
        engine.removeCurrentTorrent()

        val server = TorrentStreamServer(engine.saveDir)
        state.torrentStreamServer = server

        state.currentTorrentListener?.let { engine.removeListener(it) }
        val listener = object : com.blissless.tensei.torrent.TorrentEngine.EngineListener {
            override fun onMetadataReceived(meta: com.blissless.tensei.torrent.TorrentMeta) {
                scope.launch {
                    try {
                        val videoExts = setOf("mkv", "mp4", "webm", "avi", "mov", "m4v")
                        val videoFiles = meta.files.filter { f -> f.name.substringAfterLast('.', "").lowercase() in videoExts }

                        val fileIndex = if (videoFiles.size == 1) {
                            videoFiles.first().index
                        } else {
                            var epPattern = Regex("(?:^|[Ee._ \\[\\]()-])0*${episode}(?:[Ee._ \\[\\]()-]|$)", RegexOption.IGNORE_CASE)
                            var nameMatched = videoFiles.filter { f -> epPattern.containsMatchIn(f.name) }
                            var matched = if (nameMatched.isNotEmpty()) nameMatched
                            else videoFiles.filter { f -> epPattern.containsMatchIn(f.path) }

                            if (matched.isEmpty() && episodeOffset > 0) {
                                val adjustedEp = episode + episodeOffset
                                Log.i("Playback", "playTorrent: trying offset-adjusted ep=$adjustedEp (original=$episode + offset=$episodeOffset)")
                                epPattern = Regex("(?:^|[Ee._ \\[\\]()-])0*${adjustedEp}(?:[Ee._ \\[\\]()-]|$)", RegexOption.IGNORE_CASE)
                                val offsetNameMatched = videoFiles.filter { f -> epPattern.containsMatchIn(f.name) }
                                matched = if (offsetNameMatched.isNotEmpty()) offsetNameMatched
                                else videoFiles.filter { f -> epPattern.containsMatchIn(f.path) }
                            }

                            if (matched.isNotEmpty()) {
                                if (matched.size > 1) {
                                    val dirs = matched.map { f -> f.path.substringBeforeLast('/', "").substringBeforeLast('\\', "") }.distinct()
                                    if (dirs.size > 1) matched.sortedBy { it.path }.first().index
                                    else matched.maxBy { it.size }.index
                                } else matched.first().index
                            } else {
                                val nameContains = videoFiles.filter { f -> f.name.contains("$episode") }
                                val containsMatched = if (nameContains.isNotEmpty()) nameContains
                                else videoFiles.filter { f -> f.path.contains("$episode") }
                                containsMatched.maxByOrNull { it.size }?.index ?: engine.getLargestVideoFileIndex()
                            }
                        }

                        engine.startDownload(fileIndex)
                        val port = server.start()
                        val filePath = engine.getFileSavePath(fileIndex)
                        if (filePath == null) {
                            state.streamError = context.getString(R.string.app_torrent_path_error)
                            state.isLoadingStream = false
                            return@launch
                        }
                        val saveDirPath = engine.saveDir.absolutePath + File.separator
                        val fileName = if (filePath.startsWith(saveDirPath)) filePath.removePrefix(saveDirPath) else filePath.substringAfterLast(File.separator)

                        val fileFirstPiece = engine.getFileFirstPiece(fileIndex)
                        val fileSize = engine.getFileSize(fileIndex)
                        server.setTotalFileSize(fileSize)
                        server.setPieceSize(engine.getPieceSize())
                        server.setPieceChecker { fileRelativePiece -> engine.havePiece(fileRelativePiece + fileFirstPiece) }
                        server.setSafeBytesProvider { engine.getContiguousDownloadedBytes() }

                        // Wait for ~2 MiB of contiguous data before exposing the stream.
                        val minBytes = 2L * 1024 * 1024
                        val waitDeadline = System.nanoTime() + 120_000_000_000L
                        while (System.nanoTime() < waitDeadline) {
                            if (engine.getContiguousDownloadedBytes() >= minBytes) break
                            delay(500)
                        }

                        state.currentVideoUrl = "http://127.0.0.1:$port/$fileName"
                        state.currentReferer = ""
                        // Don't fall back to the anime's title here — it duplicates the
                        // animeName shown in the player's top bar.
                        state.currentEpisodeTitle = context.getString(R.string.app_episode_number, episode)
                        scope.launch {
                            val tmdbTitle = getTmdbEpisodeTitle(anime, episode)
                            if (state.currentAnime?.id == anime.id && state.currentEpisode == episode) {
                                state.currentEpisodeTitle = tmdbTitle
                            }
                        }
                        state.currentSubtitleTracks = extensionSubtitles
                        state.currentSubtitleUrl = pickTenseiSubtitleUrl(extensionSubtitles)
                        state.currentQualityOptions = emptyList()
                        state.currentQuality = "Auto"
                        state.currentServerName = "Torrent"
                        state.currentServerIndex = 0
                        state.currentTorrentFileSize = fileSize
                        state.isExtensionFlow = false
                        state.showPlayer = true
                        state.isLoadingStream = false
                    } catch (e: Exception) {
                        state.streamError = context.getString(R.string.app_stream_start_failed, e.message)
                        state.isLoadingStream = false
                    }
                }
            }
            override fun onProgress(downloaded: Long, total: Long) {}
            override fun onFinished() {}
            override fun onError(message: String) {
                scope.launch {
                    state.streamError = message
                    state.isLoadingStream = false
                }
            }
        }
        engine.addListener(listener)
        state.currentTorrentListener = listener
        engine.addTorrentFromMagnet(magnetUri)
    }

    // ─── Playback paths ────────────────────────────────────────────────

    /**
     * Tensei (magnet / ContentProvider) extension playback path.
     *
     * Triggered when the user has selected the "Tensei" stream method in
     * Settings. Resolves a magnet URI via [com.blissless.tensei.player.playback]'s
     * magnet extension client, and falls back to a direct stream URL when the
     * extension reports one. All state mutations here are Tensei-specific —
     * nothing in this function touches the aniyomi
     * [eu.kanade.tachiyomi.animesource.AnimeCatalogueSource] / SourceManager
     * stack, so debugging Tensei issues never requires reading aniyomi
     * code (and vice versa).
     */
    fun loadAndPlayEpisodeTensei(anime: AnimeMedia, episode: Int, isAutoRefresh: Boolean) {
        if (isAutoRefresh && state.isAutoRefreshing) return
        if (isAutoRefresh) state.isAutoRefreshing = true
        state.isExtensionFlow = false
        state.isLoadingStream = true
        scope.launch {
            yield()
            val cached = viewModel.getMagnetForEpisode(anime.id, episode)
            val magnetUri = withContext(Dispatchers.IO) {
                cached ?: viewModel.fetchMagnetForEpisode(anime, episode)
            }
            if (magnetUri != null && magnetUri.isNotEmpty()) {
                val episodeOffset = try {
                    withContext(Dispatchers.IO) {
                        viewModel.repository.calculateRecursiveOffset(anime.id)
                    }
                } catch (e: Exception) {
                    Log.w("Playback", "loadAndPlayEpisodeTensei: failed to calculate episode offset", e)
                    0
                }
                Log.i("Playback", "loadAndPlayEpisodeTensei: episodeOffset=$episodeOffset for anime='${anime.title}' (id=${anime.id})")

                val streamResult = withContext(Dispatchers.IO) {
                    try {
                        viewModel.fetchStreamUrlForEpisode(anime, episode, viewModel.preferredCategory.value)
                    } catch (e: Exception) {
                        Log.w("Playback", "loadAndPlayEpisodeTensei: fetchStreamUrlForEpisode failed", e)
                        null
                    }
                }
                Log.i("Playback", "loadAndPlayEpisodeTensei: streamResult url=${streamResult?.url?.take(60)} subtitles=${streamResult?.subtitles?.size ?: 0}")
                playTorrent(magnetUri, anime, episode, streamResult?.subtitles ?: emptyList(), episodeOffset)
            } else if (magnetUri != null) {
                val streamResult = viewModel.fetchStreamUrlForEpisode(anime, episode, viewModel.preferredCategory.value)
                if (streamResult != null) {
                    // Use the preferred stream from the streams array if available
                    val preferredLang = viewModel.preferredCategory.value // "sub" or "dub"
                    val preferredStream = selectPreferredTenseiStream(streamResult.streams, preferredLang)
                    val playUrl = preferredStream?.url ?: streamResult.url
                    val playHeaders = preferredStream?.headers ?: streamResult.headers
                    val playSubs = preferredStream?.subtitles ?: streamResult.subtitles

                    state.currentVideoUrl = playUrl
                    state.currentReferer = playHeaders["Referer"] ?: ""
                    // Don't fall back to the anime's title here — it duplicates the
                    // animeName shown in the player's top bar.
                    state.currentEpisodeTitle = context.getString(R.string.app_episode_number, episode)
                    scope.launch {
                        val tmdbTitle = getTmdbEpisodeTitle(anime, episode)
                        if (state.currentAnime?.id == anime.id && state.currentEpisode == episode) {
                            state.currentEpisodeTitle = tmdbTitle
                        }
                    }
                    state.currentSubtitleTracks = playSubs
                    state.currentSubtitleUrl = pickTenseiSubtitleUrl(playSubs)
                    state.currentQualityOptions = emptyList()
                    state.currentQuality = "Auto"
                    state.currentServerName = preferredStream?.lang?.uppercase() ?: "Tensei"
                    state.currentServerIndex = 0
                    state.currentCategory = preferredStream?.lang ?: preferredLang
                    state.isExtensionFlow = false
                    // Pass headers for DefaultHttpDataSource (trust-all SSL via SSLContext.setDefault)
                    state.extensionVideoHeaders = playHeaders
                    state.extensionOkHttpClient = null
                    // Populate server list from streams array so the server selector shows
                    state.extensionHosters = streamResult.streams.map { s ->
                        eu.kanade.tachiyomi.animesource.model.Hoster(
                            hosterUrl = s.url,
                            hosterName = s.lang.uppercase(),
                        )
                    }
                    state.extensionServers = buildTenseiServerList(streamResult.streams)
                    state.extensionStreamEntries = streamResult.streams
                    // Fix currentServerName to match the full extensionServer name (with provider)
                    state.extensionServers.find { it.url == playUrl }?.let { state.currentServerName = it.name }
                    // Store all streams for server switching
                    PlayerData.allHosters = state.extensionHosters ?: emptyList()
                    state.showPlayer = true
                    state.isLoadingStream = false
                } else {
                    state.streamError = context.getString(R.string.app_no_stream_available, episode)
                    state.isLoadingStream = false
                    context.toast(context.getString(R.string.app_no_stream_available, episode))
                }
            } else {
                state.streamError = context.getString(R.string.app_no_tensei_link_found, episode)
                state.isLoadingStream = false
                context.toast(context.getString(R.string.app_no_tensei_link_available, episode))
            }
            if (isAutoRefresh) state.isAutoRefreshing = false
        }
    }

    /**
     * Tensei stream extension playback path.
     *
     * For ContentProvider-based stream extensions (*.anime.stream) that do not
     * provide magnet URIs — they only serve direct stream URLs via the same
     * ContentProvider contract.
     */
    fun loadAndPlayEpisodeStream(anime: AnimeMedia, episode: Int, isAutoRefresh: Boolean, streamAuthority: String) {
        if (isAutoRefresh && state.isAutoRefreshing) return
        if (isAutoRefresh) state.isAutoRefreshing = true
        state.isExtensionFlow = false
        state.isLoadingStream = true
        scope.launch {
            yield()
            Log.i("Playback", "loadAndPlayEpisodeStream: anime=${anime.id} ep=$episode authority=$streamAuthority")
            val streamResult = withContext(Dispatchers.IO) {
                try {
                    viewModel.fetchStreamUrlForEpisode(anime, episode, viewModel.preferredCategory.value, overrideAuthority = streamAuthority)
                } catch (e: Exception) {
                    Log.w("Playback", "loadAndPlayEpisodeStream: fetchStreamUrlForEpisode failed", e)
                    null
                }
            }
            Log.i("Playback", "loadAndPlayEpisodeStream: streamResult url=${streamResult?.url?.take(60)} subtitles=${streamResult?.subtitles?.size ?: 0} streams=${streamResult?.streams?.size}")
            if (streamResult != null) {
                val preferredLang = viewModel.preferredCategory.value
                val preferredStream = selectPreferredTenseiStream(streamResult.streams, preferredLang)
                val playUrl = preferredStream?.url ?: streamResult.url
                val playHeaders = preferredStream?.headers ?: streamResult.headers
                val playSubs = preferredStream?.subtitles ?: streamResult.subtitles

                state.currentVideoUrl = playUrl
                state.currentReferer = playHeaders["Referer"] ?: ""
                // Don't fall back to the anime's title here — it duplicates the
                // animeName shown in the player's top bar.
                state.currentEpisodeTitle = context.getString(R.string.app_episode_number, episode)
                scope.launch {
                    val tmdbTitle = getTmdbEpisodeTitle(anime, episode)
                    if (state.currentAnime?.id == anime.id && state.currentEpisode == episode) {
                        state.currentEpisodeTitle = tmdbTitle
                    }
                }
                state.currentSubtitleTracks = playSubs
                state.currentSubtitleUrl = pickTenseiSubtitleUrl(playSubs)
                state.currentQualityOptions = emptyList()
                state.currentQuality = "Auto"
                state.currentServerName = preferredStream?.lang?.uppercase() ?: "Tensei"
                state.currentServerIndex = 0
                state.currentCategory = preferredStream?.lang ?: preferredLang
                state.isExtensionFlow = false
                state.extensionVideoHeaders = playHeaders
                state.extensionOkHttpClient = null
                state.extensionHosters = streamResult.streams.map { s ->
                    eu.kanade.tachiyomi.animesource.model.Hoster(
                        hosterUrl = s.url,
                        hosterName = s.lang.uppercase(),
                    )
                }
                state.extensionServers = buildTenseiServerList(streamResult.streams)
                state.extensionStreamEntries = streamResult.streams
                state.extensionServers.find { it.url == playUrl }?.let { state.currentServerName = it.name }
                PlayerData.allHosters = state.extensionHosters ?: emptyList()
                state.showPlayer = true
                state.isLoadingStream = false
            } else {
                val reason = viewModel.magnetExtensionClient?.lastStreamError()
                    ?.takeIf { it.isNotBlank() }
                val msg = if (reason != null) {
                    context.getString(R.string.app_no_stream_available_reason, episode, reason)
                } else {
                    context.getString(R.string.app_no_stream_available, episode)
                }
                state.streamError = msg
                state.isLoadingStream = false
                context.toast(msg)
            }
            if (isAutoRefresh) state.isAutoRefreshing = false
        }
    }

    /**
     * Aniyomi (DexClassLoader / AnimeCatalogueSource) extension playback path.
     *
     * This is the original code path — unchanged apart from being extracted
     * into its own function so it no longer shares a function body with the
     * Tensei path. All resolution goes through the extension SourceManager /
     * [MainViewModel.playEpisodeWithExtension].
     */
    fun loadAndPlayEpisodeAniyomi(anime: AnimeMedia, episode: Int, isAutoRefresh: Boolean) {
        val extPackage = viewModel.defaultExtensionPackage.value
        if (extPackage.isNotEmpty()) {
            state.isExtensionFlow = true
            state.isLoadingStream = true
            state.extensionVideos = null
            state.extensionHosters = null
            state.pendingExtResult = null
            state.showExtHosterDialog = false
            state.showExtVideoDialog = false
            scope.launch {
                yield()
                val result = viewModel.playEpisodeWithExtension(anime, episode, extPackage)
                state.pendingExtResult = result
                if (result != null && result.videos.isNotEmpty()) {
                    state.extensionVideos = result.videos
                    state.extensionHosters = result.hosters
                    state.extensionSourcePackage = extPackage
                    state.extensionEpisodeNumber = episode
                    state.extensionEpisodeUrl = result.episode?.url ?: ""
                    PlayerData.extensionSource = result.source
                    PlayerData.extensionEpisode = result.episode
                    PlayerData.allHosters = result.hosters ?: emptyList()
                    playExtensionVideo(result, 0)
                } else {
                    val reason = viewModel.lastExtensionPlaybackError()?.takeIf { it.isNotBlank() }
                    val msg = if (reason != null) {
                        context.getString(R.string.app_extension_failed_reason, episode, reason)
                    } else {
                        context.getString(R.string.app_extension_failed, episode)
                    }
                    state.streamError = msg
                    context.toast(msg)
                }
                if (isAutoRefresh) state.isAutoRefreshing = false
                state.isLoadingStream = false
            }
            return
        }

        onMissingExtension(anime, episode)
        state.isLoadingStream = false
    }

    /**
     * True when the active stream method can start playback without user
     * intervention. Mirrors the manga home-screen check so anime redirects
     * to Settings the same way when the external (direct) method has no
     * default extension package.
     *
     * The "magnet" route auto-falls-back to any detected stream/torrent
     * extension (and is robust to detection still being in-flight on first
     * launch), so explicit selection is not required there.
     */
    fun hasUsableAnimeExtensionDefault(): Boolean {
        return viewModel.streamMethod.value == "magnet" || viewModel.defaultExtensionPackage.value.isNotEmpty()
    }

    /**
     * Dispatcher for episode playback.
     *
     * Routes to the dedicated Tensei or aniyomi path based on the user's
     * configured stream method. The paths live in separate methods so each
     * can be debugged in isolation.
     */
    fun loadAndPlayEpisode(anime: AnimeMedia, episode: Int, isAutoRefresh: Boolean = false) {
        // Anime equivalent of the manga reader's no-extension redirect: instead of
        // silently failing at playback time, park the (anime, episode) intent and
        // redirect to Settings to pick a default extension. Auto-resumes once the
        // user returns from Settings with a usable default configured.
        if (!isAutoRefresh && !hasUsableAnimeExtensionDefault()) {
            onMissingExtension(anime, episode)
            state.isLoadingStream = false
            return
        }
        val streamMethod = viewModel.streamMethod.value
        val streamExtAuthority = viewModel.defaultStreamExtension.value
        if (!isAutoRefresh) {
            state.isAutoRefreshing = false
            state.pendingSeekPosition = null
        }
        state.currentAnime = anime
        state.currentEpisode = episode
        state.totalEpisodes = anime.totalEpisodes
        state.streamError = null
        state.savedPlaybackPosition = viewModel.getPlaybackPosition(anime.id, episode)
        if (!isAutoRefresh) state.showPlayer = false

        if (streamMethod == "magnet") {
            if (streamExtAuthority != null) {
                loadAndPlayEpisodeStream(anime, episode, isAutoRefresh, streamExtAuthority)
            } else {
                loadAndPlayEpisodeTensei(anime, episode, isAutoRefresh)
            }
            return
        }
        loadAndPlayEpisodeAniyomi(anime, episode, isAutoRefresh)
    }

    /** Resolves and caches the next episode's extension result in the background. */
    fun prefetchExtensionNextEpisode() {
        val anime = state.currentAnime ?: return
        val streamMethod = viewModel.streamMethod.value
        if (streamMethod == "magnet") return
        scope.launch {
            val nextEp = state.currentEpisode + 1
            val pkg = state.extensionSourcePackage.ifEmpty { viewModel.defaultExtensionPackage.value }
            if (pkg.isEmpty()) return@launch
            val result = viewModel.playEpisodeWithExtension(anime, nextEp, pkg)
            if (result != null) {
                state.cachedExtensionNext = result
                state.episodeCache[nextEp] = result
            }
        }
    }

    // ─── Server switching ──────────────────────────────────────────────

    /**
     * Tensei (magnet / ContentProvider) server switch.
     *
     * Switches between the sub/dub streams that were resolved by
     * [loadAndPlayEpisodeTensei] and stored in [PlaybackState.extensionStreamEntries] /
     * [PlaybackState.extensionServers]. No network call is needed — all stream
     * URLs are already in memory.
     */
    fun handleTenseiServerChange(hosterName: String) {
        val serverInfo = state.extensionServers.find { it.name == hosterName }
        if (serverInfo != null) {
            state.currentVideoUrl = serverInfo.url
            state.currentServerName = hosterName
            state.currentCategory = if (hosterName.contains("DUB", ignoreCase = true)) "dub" else "sub"
            val streamEntry = state.extensionStreamEntries.find { it.url == serverInfo.url }
            if (streamEntry != null) {
                state.extensionVideoHeaders = streamEntry.headers
                state.currentReferer = streamEntry.headers["Referer"] ?: ""
            }
            state.episodeTrigger++
        } else {
            Log.w("ServerSwitch", "  serverInfo NOT found for name=$hosterName")
        }
    }

    /**
     * Aniyomi (DexClassLoader / AnimeCatalogueSource) server switch.
     *
     * Fetches the video list for the selected hoster via the aniyomi
     * extension's AnimeCatalogueSource interface.
     */
    fun handleAniyomiServerChange(
        hosterName: String,
        hoster: eu.kanade.tachiyomi.animesource.model.Hoster,
        source: eu.kanade.tachiyomi.animesource.AnimeCatalogueSource,
    ) {
        scope.launch {
            state.isLoadingStream = true
            val result = viewModel.fetchExtensionHosterVideos(source, hoster)
            if (result != null) {
                state.currentVideoUrl = result.url
                state.currentReferer = result.referer
                state.currentSubtitleUrl = result.subtitleUrl
                state.currentServerName = hosterName
                state.currentCategory = if (hosterName.contains("dub", ignoreCase = true) || result.videoTitle.contains("dub", ignoreCase = true)) "dub" else "sub"
                state.currentQualityOptions = buildQualityOptions(result.videos)
                state.currentQuality = result.videoTitle
                state.extensionOkHttpClient = result.extensionClient
                state.extensionVideoHeaders = result.videoHeaders
                state.episodeTrigger++
            } else {
                context.toast(context.getString(R.string.app_failed_to_load_hoster, hosterName))
            }
            state.isLoadingStream = false
        }
    }

    /**
     * Dispatcher for server/hoster switches in the player.
     *
     * Routes to the Tensei-specific or aniyomi-specific handler based on
     * whether the current playback was started from a Tensei stream
     * (PlayerData.extensionSource == null) or an aniyomi extension source.
     */
    fun handleExtensionServerChange(serverKey: String) {
        // ── Direct video-URL path ─────────────────────────────────────────
        //
        // When `state.extensionServers` was built from `buildServerListFromVideos`
        // (per-video entries, not per-hoster), the `serverKey` is a VIDEO
        // URL (e.g. `http://127.0.0.1:35807/playlist.m3u8?url=...`), NOT a
        // hoster URL. In that case, we can skip the hoster-based fetch
        // entirely — just use the clicked video URL directly as
        // `currentVideoUrl`. This is the common case for extensions like
        // animex that return a flat list of 12 videos with 3 "hoster"
        // labels whose `videoList` is null.
        val matchingServerInfo = state.extensionServers.find { it.url == serverKey }
        if (matchingServerInfo != null) {
            Log.i("ServerSwitch", "  direct video-URL match: ${matchingServerInfo.name} → using URL directly (skipping hoster fetch)")
            val source = PlayerData.extensionSource
            val sourceHttp = source as? eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
            val directClient = sourceHttp?.client
                ?: try {
                    eu.kanade.tachiyomi.network.NetworkHelper.getInstance().client
                } catch (e: Exception) {
                    null
                }
            // Rewrite the video URL's localhost port to our proxy port.
            var effectiveUrl = serverKey
            if (!effectiveUrl.contains("127.0.0.1:${com.blissless.tensei.player.proxy.LocalProxyServer.PROXY_PORT}") &&
                (effectiveUrl.contains("127.0.0.1") || effectiveUrl.contains("localhost"))
            ) {
                effectiveUrl = effectiveUrl
                    .replace(Regex("127\\.0\\.0\\.1:\\d+"), "127.0.0.1:${com.blissless.tensei.player.proxy.LocalProxyServer.PROXY_PORT}")
                    .replace(Regex("localhost:\\d+"), "127.0.0.1:${com.blissless.tensei.player.proxy.LocalProxyServer.PROXY_PORT}")
            }
            state.currentVideoUrl = effectiveUrl
            state.currentServerName = matchingServerInfo.name
            state.currentCategory = if (matchingServerInfo.name.contains("dub", ignoreCase = true)) "dub" else "sub"
            state.currentQualityOptions = matchingServerInfo.qualities
            state.currentQuality = matchingServerInfo.name
            state.extensionOkHttpClient = directClient
            // Use the video's headers if available, else source's headers.
            state.extensionVideoHeaders = sourceHttp?.headers?.let { h ->
                (0 until h.size).associate { h.name(it) to h.value(it) }
            } ?: emptyMap()
            state.episodeTrigger++
            return
        }

        // ── Legacy hoster-based path ─────────────────────────────────────
        //
        // Look up the hoster. We try multiple strategies because
        // `buildServerList` produces different URL types depending on
        // whether the hoster name is duplicated:
        //   - Non-duplicate hoster: `ServerInfo.url == hoster.hosterUrl`
        //     → match by `hosterUrl`.
        //   - Duplicate hoster (exploded per video): `ServerInfo.url == video.videoUrl`
        //     → match by checking if any video in the hoster's videoList
        //       has this URL.
        //   - Legacy / backwards-compat: pass the hoster's NAME as
        //     the key (the old behaviour) → match by `hosterName`.
        val hoster = state.extensionHosters?.find { it.hosterUrl == serverKey } // non-dup case
            ?: state.extensionHosters?.find { h -> // dup case: URL is a video URL
                h.videoList.orEmpty().any { it.videoUrl == serverKey }
            }
            ?: state.extensionHosters?.find { it.hosterName == serverKey } // legacy name lookup
        if (hoster == null) {
            Log.w("ServerSwitch", "  hoster not found in extensionHosters, attempting direct Tensei switch")
            handleTenseiServerChange(serverKey)
            return
        }
        Log.i("ServerSwitch", "  matched hoster: ${hoster.hosterName} (url=${hoster.hosterUrl.take(60)}, videos=${hoster.videoList?.size ?: 0})")
        val source = PlayerData.extensionSource

        if (source == null) {
            Log.w("ServerSwitch", "  source is null, falling back to Tensei switch")
            handleTenseiServerChange(serverKey)
            return
        }
        // Pass the hoster's real name (not the disambiguated display
        // name) so `currentServerName` shows the bare hoster name in
        // the player's top bar.
        handleAniyomiServerChange(hoster.hosterName, hoster, source)
    }

    // ─── Episode navigation callbacks (player + cards) ─────────────────

    /** Entry point used by schedule cards, explore rows and the detail screens. */
    fun onPlayEpisode(anime: AnimeMedia, episode: Int, title: String?) {
        if (title == null) {
            state.isLoadingStream = true
            scope.launch {
                state.currentEpisodeTitle = getTmdbEpisodeTitle(anime, episode)
                loadAndPlayEpisode(anime, episode)
            }
        } else {
            state.currentEpisodeTitle = sanitizeEpisodeTitle(title)
                ?: context.getString(R.string.app_episode_number, episode)
            loadAndPlayEpisode(anime, episode)
        }
    }

    fun onPreviousEpisode() {
        if (!state.isChangingEpisode && state.currentAnime != null && state.currentEpisode > 1) {
            state.isChangingEpisode = false
            state.isAutoRefreshing = false
            loadAndPlayEpisode(state.currentAnime!!, state.currentEpisode - 1, isAutoRefresh = true)
        }
    }

    fun onNextEpisode() {
        if (!state.isChangingEpisode && state.currentAnime != null) {
            state.isChangingEpisode = false
            state.isAutoRefreshing = false
            loadAndPlayEpisode(state.currentAnime!!, state.currentEpisode + 1, isAutoRefresh = true)
        }
    }
}
