package com.blissless.tensei.extensions.model

import com.blissless.tensei.extensions.model.AnimeExtension

/**
 * Result of attempting to load a single extension APK.
 *
 * Ported from Aniyomi's `app/src/main/java/eu/kanade/tachiyomi/extension/anime/model/AnimeLoadResult.kt`.
 *
 * Three possible outcomes, mirroring the three `AnimeExtension` subtypes:
 *
 *   - `Success`: extension loaded; pass back the `Installed` descriptor.
 *   - `Untrusted`: extension's signing cert is not trusted; pass back the
 *     `Untrusted` descriptor (so the UI can prompt).
 *   - `Error`: extension could not be loaded (lib-version mismatch,
 *     missing metadata, ClassLoader failure, source class instantiate
 *     failure). No descriptor — just log + drop.
 */
sealed class AnimeLoadResult {
    data class Success(val extension: AnimeExtension.Installed) : AnimeLoadResult()
    data class Untrusted(val extension: AnimeExtension.Untrusted) : AnimeLoadResult()
    data object Error : AnimeLoadResult()
}
