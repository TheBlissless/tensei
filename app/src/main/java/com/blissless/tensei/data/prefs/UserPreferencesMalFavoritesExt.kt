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

// MAL favorites persistence (anime and manga).

fun UserPreferences.getMalFavorites(): Set<Int> {
    val saved = sharedPreferences.getStringSet(UserPreferences.KEY_MAL_FAVORITES, emptySet()) ?: emptySet()
    return saved.mapNotNull { it.toIntOrNull() }.toSet()
}

fun UserPreferences.saveMalFavorites(favorites: List<Int>) {
    sharedPreferences.edit {
        putStringSet(UserPreferences.KEY_MAL_FAVORITES, favorites.map { it.toString() }.toSet())
    }
}

fun UserPreferences.clearMalFavorites() {
    sharedPreferences.edit {
        remove(UserPreferences.KEY_MAL_FAVORITES)
    }
}
// ─── MAL Manga Favorites ──────────────────────────────────────────

fun UserPreferences.getMalMangaFavorites(): Set<Int> {
    val saved = sharedPreferences.getStringSet(UserPreferences.KEY_MAL_MANGA_FAVORITES, emptySet()) ?: emptySet()
    return saved.mapNotNull { it.toIntOrNull() }.toSet()
}

fun UserPreferences.saveMalMangaFavorites(favorites: List<Int>) {
    sharedPreferences.edit {
        putStringSet(UserPreferences.KEY_MAL_MANGA_FAVORITES, favorites.map { it.toString() }.toSet())
    }
}
