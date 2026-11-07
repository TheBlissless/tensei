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

// Subtitle profile persistence (JSON-encoded profiles in the prefs file).

fun UserPreferences.getActiveSubtitleSettings(): SubtitleSettings {
    val data = _subtitleProfileData.value
    return data.profiles.getOrElse(data.activeProfileIndex) { SubtitleSettings.DEFAULT }
}

fun UserPreferences.setSubtitleProfile(index: Int, settings: SubtitleSettings) {
    val current = _subtitleProfileData.value
    if (index in current.profiles.indices) {
        val updatedProfiles = current.profiles.toMutableList().also { it[index] = settings }
        _subtitleProfileData.value = current.copy(profiles = updatedProfiles)
        saveSubtitleProfiles()
    }
}

fun UserPreferences.setActiveSubtitleProfile(index: Int) {
    val current = _subtitleProfileData.value
    if (index in current.profiles.indices) {
        _subtitleProfileData.value = current.copy(activeProfileIndex = index)
        sharedPreferences.edit { putInt(UserPreferences.KEY_SUBTITLE_ACTIVE_PROFILE, index) }
    }
}

fun UserPreferences.resetSubtitleProfile(index: Int) {
    setSubtitleProfile(index, SubtitleSettings(profileName = "Profile ${index + 1}"))
}

fun UserPreferences.renameSubtitleProfile(index: Int, name: String) {
    val current = _subtitleProfileData.value
    if (index in current.profiles.indices) {
        val updated = current.profiles[index].copy(profileName = name)
        setSubtitleProfile(index, updated)
    }
}

fun UserPreferences.saveSubtitleProfiles() {
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    val encoded = json.encodeToString(SubtitleProfileData.serializer(), _subtitleProfileData.value)
    sharedPreferences.edit { putString(UserPreferences.KEY_SUBTITLE_PROFILES, encoded) }
}

fun UserPreferences.loadSubtitleProfiles() {
    val saved = sharedPreferences.getString(UserPreferences.KEY_SUBTITLE_PROFILES, null)
    if (saved != null) {
        try {
            val json = com.blissless.tensei.util.TenseiJson.default
            val data = json.decodeFromString(SubtitleProfileData.serializer(), saved)
            _subtitleProfileData.value = data
        } catch (_: Exception) {
            _subtitleProfileData.value = SubtitleProfileData()
        }
    }
    val activeIndex = sharedPreferences.getInt(UserPreferences.KEY_SUBTITLE_ACTIVE_PROFILE, 0)
    val current = _subtitleProfileData.value
    if (activeIndex in current.profiles.indices && activeIndex != current.activeProfileIndex) {
        _subtitleProfileData.value = current.copy(activeProfileIndex = activeIndex)
    }
}
