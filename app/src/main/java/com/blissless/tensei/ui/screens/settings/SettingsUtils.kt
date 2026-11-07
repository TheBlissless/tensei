package com.blissless.tensei.ui.screens.settings

import com.blissless.tensei.util.compareVersions

import android.content.Context

/**
 * Utility functions for the Settings screen.
 *
 * Extracted from SettingsScreen.kt. Pure functions with no Compose
 * dependencies, used by various settings pages.
 */

internal fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

