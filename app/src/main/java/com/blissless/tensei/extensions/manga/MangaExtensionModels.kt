package com.blissless.tensei.extensions.manga

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.source.Source

/**
 * An extension apk present on this device whose sources were loaded successfully.
 */
data class LoadedExtension(
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val libVersion: Double,
    val lang: String,
    val nsfw: Boolean,
    val isShared: Boolean,
    val icon: Drawable? = null,
    val sources: List<Source>,
)

/**
 * A listing from an extension store (Keiyoushi).
 */
data class AvailableExtension(
    val name: String,
    val pkgName: String,
    val apkUrl: String,
    val iconUrl: String,
    val versionName: String,
    val versionCode: Long,
    val lang: String,
    val libVersion: Double,
    val nsfw: Boolean,
    val sourceNames: List<String>,
    val repoUrl: String,
)
