package com.blissless.tensei.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.data.models.AnimeMedia
import com.blissless.tensei.torrent.MagnetData
import com.blissless.tensei.torrent.MagnetExtensionClient
import com.blissless.tensei.torrent.StreamUrlResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Magnet extension logic for [MainViewModel].
 *
 * Extracted from MainViewModel.kt. Manages the MagnetExtensionClient lifecycle,
 * magnet link prefetching, and per-episode stream URL resolution.
 * Public signatures preserved as extension functions.
 */

/**
 * Lazily create the magnet extension client. [loadAvailableMagnetExtensions] is the
 * normal entry point, but playback (deep links, auto-refresh, widgets) can be
 * triggered before any screen calls it — without this guard the client stays null
 * and [fetchMagnetForEpisode] silently returns null ("no magnet link found").
 */
fun MainViewModel.ensureMagnetClient(): MagnetExtensionClient {
    return magnetExtensionClient ?: MagnetExtensionClient(context).also {
        magnetExtensionClient = it
    }
}

fun MainViewModel.loadAvailableMagnetExtensions() {
    val client = ensureMagnetClient()
    viewModelScope.launch(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val detected = client.detectExtensions()
        val elapsed = System.currentTimeMillis() - startTime
        _availableMagnetExtensions.value = detected.map { it.name to it.authority }
    }
}

fun MainViewModel.loadAvailableStreamExtensions() {
    val detector = com.blissless.tensei.extensions.ExtensionDetector(context)
    viewModelScope.launch(Dispatchers.IO) {
        val detected = detector.detectStreamExtensions()
        _availableStreamExtensions.value = detected
    }
}

internal fun MainViewModel.parseTitleForSearch(title: String): String {
    var cleaned = title
    cleaned = cleaned.replace(Regex("\\([^)]*\\)"), "").trim()
    cleaned = cleaned.replace(Regex("\\[[^\\]]*\\]"), "").trim()
    cleaned = cleaned.replace(Regex("\\{.*?\\}"), "").trim()
    cleaned = cleaned.replace(Regex("\\s+"), " ").trim()
    return cleaned
}

fun MainViewModel.fetchMagnetEpisodes(anime: AnimeMedia, authority: String) {
    viewModelScope.launch(Dispatchers.IO) {
        fetchMagnetEpisodesSync(anime, authority)
    }
}

internal suspend fun MainViewModel.fetchMagnetEpisodesSync(anime: AnimeMedia, authority: String): MagnetData? {
    val eng = anime.titleEnglish

    val searchTerms = mutableListOf<String>()
    if (!eng.isNullOrBlank()) {
        val cleaned = parseTitleForSearch(eng)
        searchTerms.add(cleaned)
        if (cleaned != eng) searchTerms.add(eng)
    } else {
        Log.w(MainViewModel.TAG, "fetchMagnetEpisodesSync: no English title available, falling back to romaji")
        anime.title?.let { searchTerms.add(parseTitleForSearch(it)) }
    }

    val client = ensureMagnetClient()
    var result: MagnetData? = null
    for ((i, query) in searchTerms.withIndex()) {
        val startTime = System.currentTimeMillis()
        val fetched = try {
            client.fetchMagnets(authority, anime.id, query, parseTitleForSearch(anime.title ?: query), preferredCategory.value)
        } catch (e: Exception) {
            Log.e(MainViewModel.TAG, "fetchMagnetEpisodesSync: fetchMagnets threw for term[$i]='$query'", e)
            null
        }
        val elapsed = System.currentTimeMillis() - startTime
        if (fetched != null) {
            if (fetched.episodes.isEmpty()) {
                Log.w(MainViewModel.TAG, "fetchMagnetEpisodesSync: extension returned MagnetData but 0 episodes (isSingleTorrent=${fetched.isSingleTorrent})")
            } else {
                result = fetched
                break
            }
        }
    }

    if (result != null) {
        _magnetEpisodes.value += (anime.id to result)
    } else {
        Log.w(MainViewModel.TAG, "fetchMagnetEpisodesSync: no magnet data found for anime ${anime.id} with any search term")
        _magnetEpisodes.value += (anime.id to MagnetData(emptyList(), false))
    }
    return result
}

fun MainViewModel.clearMagnetEpisodes(animeId: Int) {
    _magnetEpisodes.value -= animeId
}

fun MainViewModel.getMagnetEpisodeNumbers(animeId: Int): Set<Int> {
    val data = _magnetEpisodes.value[animeId]
    if (data == null) {
        return emptySet()
    }
    if (data.isSingleTorrent && data.episodes.size == 1) {
        return emptySet()
    }
    val eps = data.episodes.map { it.episode }.toSet()
    return eps
}

fun MainViewModel.getMagnetForEpisode(animeId: Int, episode: Int): String? {
    val data = _magnetEpisodes.value[animeId]
    if (data == null) {
        return null
    }
    val magnet = if (data.isSingleTorrent) {
        data.episodes.firstOrNull()?.magnet
    } else {
        val ep = data.episodes.find { it.episode == episode }
        ep?.magnet
    }
    return magnet
}

suspend fun MainViewModel.fetchMagnetForEpisode(anime: AnimeMedia, episode: Int): String? {
    ensureMagnetClient()
    val authority = defaultMagnetExtension.value
        ?: _availableMagnetExtensions.value.firstOrNull()?.second
    if (authority.isNullOrBlank()) {
        Log.w(MainViewModel.TAG, "fetchMagnetForEpisode: no magnet extension authority " +
                "(default=${defaultMagnetExtension.value}, detected=${_availableMagnetExtensions.value.size})")
        return null
    }
    val data = withContext(Dispatchers.IO) {
        fetchMagnetEpisodesSync(anime, authority)
    }
    if (data != null && data.isSingleTorrent) {
        val magnet = data.episodes.firstOrNull()?.magnet
        return magnet
    }
    val magnet = getMagnetForEpisode(anime.id, episode)
    return magnet
}

suspend fun MainViewModel.fetchStreamUrlForEpisode(anime: AnimeMedia, episode: Int, lang: String, overrideAuthority: String? = null): StreamUrlResult? {
    val engName = anime.titleEnglish ?: ""
    val romajiName = anime.title ?: ""
    ensureMagnetClient()
    val authority = overrideAuthority
        ?: defaultMagnetExtension.value
        ?: _availableMagnetExtensions.value.firstOrNull()?.second
        ?: defaultStreamExtension.value
        ?: _availableStreamExtensions.value.firstOrNull()?.second
    if (authority.isNullOrBlank()) {
        Log.w(MainViewModel.TAG, "fetchStreamUrlForEpisode: no extension authority")
        return null
    }
    return withContext(Dispatchers.IO) {
        magnetExtensionClient?.fetchStreamUrl(authority, anime.id, episode, lang, engName, romajiName)
    }
}
