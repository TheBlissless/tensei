package com.blissless.tensei.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.blissless.tensei.data.models.LocalAnimeEntry
import com.blissless.tensei.data.models.StoredFavorite
import com.blissless.tensei.data.models.SubtitleProfileData
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.ui.theme.ColorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import androidx.core.content.edit
import com.blissless.tensei.data.anime.json

// Local (offline) stores inside UserPreferences: offline favorites (with
// AniList favorites mirror), local anime status/progress, and full user-data
// clearing.

fun UserPreferences.isAniListFavorite(mediaId: Int): Boolean = _aniListFavorites.value.contains(mediaId)

fun UserPreferences.toggleAniListFavorite(mediaId: Int): Boolean {
    val current = _aniListFavorites.value.toMutableSet()
    val wasAdded = !current.contains(mediaId)
    if (wasAdded) {
        current.add(mediaId)
    } else {
        current.remove(mediaId)
    }
    _aniListFavorites.value = current
    saveAniListFavorites(current)
    return wasAdded
}

fun UserPreferences.saveAniListFavorites(favorites: Set<Int>) {
    val json = com.blissless.tensei.util.TenseiJson.default
    val encoded = json.encodeToString(kotlinx.serialization.serializer(), favorites.toList())
    sharedPreferences.edit {putString(UserPreferences.KEY_ANILIST_FAVORITES, encoded)}
}

fun UserPreferences.loadAniListFavorites() {
    val saved = sharedPreferences.getString(UserPreferences.KEY_ANILIST_FAVORITES, null)
    if (saved != null) {
        try {
            val json = com.blissless.tensei.util.TenseiJson.default
            val list = json.decodeFromString<List<Int>>(kotlinx.serialization.serializer(), saved)
            _aniListFavorites.value = list.toSet()
        } catch (_: Exception) {
            _aniListFavorites.value = emptySet()
        }
    }
}
// Local anime status (for offline users)

fun UserPreferences.isLocalFavorite(mediaId: Int): Boolean = _localFavorites.value.containsKey(mediaId)

fun UserPreferences.canAddFavorite(): Boolean = _localFavorites.value.size < 10

fun UserPreferences.getLocalFavoriteCount(): Int = _localFavorites.value.size

fun UserPreferences.toggleLocalFavorite(
    mediaId: Int,
    title: String = "",
    cover: String = "",
    banner: String? = null,
    year: Int? = null,
    averageScore: Int? = null,
    titleEnglish: String? = null
) {
    val currentFavorites = _localFavorites.value.toMutableMap()
    val existingFavorite = currentFavorites[mediaId]
    if (existingFavorite != null) {
        // Update metadata if title was empty before
        if (existingFavorite.title.isEmpty() && title.isNotEmpty()) {
            currentFavorites[mediaId] = StoredFavorite(mediaId, title, cover, banner, year, averageScore, titleEnglish)
        } else {
            // Remove favorite
            currentFavorites.remove(mediaId)
        }
    } else {
        // Add new favorite (max 10)
        if (currentFavorites.size >= 10) {
            return
        }
        currentFavorites[mediaId] = StoredFavorite(mediaId, title, cover, banner, year, averageScore, titleEnglish)
    }
    _localFavorites.value = currentFavorites
    saveLocalFavorites(currentFavorites)
}

fun UserPreferences.saveLocalFavorites(favorites: Map<Int, StoredFavorite>) {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val favoritesJson = favorites.values.map { fav ->
        json.encodeToString(StoredFavorite.serializer(), fav)
    }.toSet()
    sharedPreferences.edit {
        putStringSet(UserPreferences.KEY_LOCAL_FAVORITES_V2, favoritesJson)
    }
}

fun UserPreferences.loadLocalFavorites() {
    val json = com.blissless.tensei.util.TenseiJson.default
    // Try v2 format first (with metadata)
    val savedV2 = sharedPreferences.getStringSet(UserPreferences.KEY_LOCAL_FAVORITES_V2, null)
    if (savedV2 != null) {
        val favorites = mutableMapOf<Int, StoredFavorite>()
        savedV2.forEach { favJson ->
            try {
                val fav = json.decodeFromString(StoredFavorite.serializer(), favJson)
                favorites[fav.id] = fav
            } catch (_: Exception) {
            }
        }
        _localFavorites.value = favorites
        return
    }
    // Fall back to v1 format (IDs only)
    val saved = sharedPreferences.getStringSet(UserPreferences.KEY_LOCAL_FAVORITES, emptySet()) ?: emptySet()
    if (saved.isNotEmpty()) {
        val favorites = mutableMapOf<Int, StoredFavorite>()
        saved.mapNotNull { it.toIntOrNull() }.forEach { id ->
            favorites[id] = StoredFavorite(id, "", "")
        }
        _localFavorites.value = favorites
        // Save in v2 format
        saveLocalFavorites(favorites)
        // Clear old format
        sharedPreferences.edit {remove(UserPreferences.KEY_LOCAL_FAVORITES) }
    }
}
// ============================================
// Data Management
// ============================================

fun UserPreferences.clearAllUserData() {
    sharedPreferences.edit {
        remove(UserPreferences.TOKEN_KEY)
            .remove("cache_home_data")
            .remove("cache_home_time")
            .remove("cache_explore_data")
            .remove("cache_explore_time")
            .remove("cache_airing_data")
            .remove("cache_airing_time")
    }
}

fun UserPreferences.getLocalAnimeStatus(mediaId: Int): LocalAnimeEntry? = _localAnimeStatus.value[mediaId]

fun UserPreferences.getAllLocalAnimeStatus(): Map<Int, LocalAnimeEntry> = _localAnimeStatus.value

fun UserPreferences.setLocalAnimeStatus(mediaId: Int, entry: LocalAnimeEntry?) {
    val currentStatus = _localAnimeStatus.value.toMutableMap()
    if (entry != null) {
        currentStatus[mediaId] = entry
    } else {
        currentStatus.remove(mediaId)
    }
    _localAnimeStatus.value = currentStatus
    saveLocalAnimeStatus(currentStatus)
}

fun UserPreferences.updateLocalAnimeProgress(mediaId: Int, progress: Int, totalEpisodes: Int) {
    val current = _localAnimeStatus.value[mediaId]
    if (current != null) {
        setLocalAnimeStatus(mediaId, current.copy(progress = progress, totalEpisodes = totalEpisodes))
    }
}

fun UserPreferences.clearLocalAnimeStatus() {
    _localAnimeStatus.value = emptyMap()
    saveLocalAnimeStatus(emptyMap())
}

fun UserPreferences.saveLocalAnimeStatus(statusMap: Map<Int, LocalAnimeEntry>) {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val statusList = statusMap.values.map { json.encodeToString(LocalAnimeEntry.serializer(), it) }.toSet()
    sharedPreferences.edit { putStringSet(UserPreferences.KEY_LOCAL_ANIME_STATUS, statusList) }
}

fun UserPreferences.loadLocalAnimeStatus() {
    val saved = sharedPreferences.getStringSet(UserPreferences.KEY_LOCAL_ANIME_STATUS, null)
    if (!saved.isNullOrEmpty()) {
        try {
            val json = com.blissless.tensei.util.TenseiJson.default
            val statusMap = mutableMapOf<Int, LocalAnimeEntry>()
            saved.forEach { entryJson ->
                try {
                    val entry = json.decodeFromString(LocalAnimeEntry.serializer(), entryJson)
                    statusMap[entry.id] = entry
                } catch (_: Exception) {
                    // Skip invalid entries
                }
            }
            _localAnimeStatus.value = statusMap
        } catch (_: Exception) {
            _localAnimeStatus.value = emptyMap()
        }
    }
}
// Check for Updates on Start
