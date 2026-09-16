package com.blissless.tensei.stream

import dalvik.system.PathClassLoader

/**
 * Hybrid parent-first / parent-last ClassLoader for loading Aniyomi-style
 * extension APKs.
 *
 * ## Why hybrid (not pure parent-last like Aniyomi's `ChildFirstPathClassLoader`)
 *
 * The pure parent-last strategy that Aniyomi uses works for them because
 * Aniyomi extensions are compiled with `compileOnly` for shared dependencies
 * like `kotlinx-coroutines-core` — so the extension's dex doesn't actually
 * bundle those classes; they're provided by the host at runtime.
 *
 * In practice, many Aniyomi extensions DO bundle `kotlin.coroutines.Continuation`
 * (and other shared classes) as a transitive dependency. When Tensei loads
 * such an extension with a pure parent-last classloader, the extension's
 * bundled `Continuation` is found first — and that's a DIFFERENT class
 * identity from the host's `Continuation` (which lives in the host's
 * `PathClassLoader`). The JVM then refuses to link the extension's
 * `AniWave.getAnimeDetails(SAnime, Continuation_ext)` against the host's
 * `AnimeSource.getAnimeDetails(SAnime, Continuation_host)` interface —
 * throwing `java.lang.LinkageError: Parameter 1 type mismatch`.
 *
 * The fix: load shared platform/stdlib classes from the HOST first (parent-first)
 * so class identities always match. Only fall back to the extension's own
 * dex for classes that the host doesn't have — which is exactly the
 * extension's own package code.
 *
 * ## The packages loaded parent-first
 *
 *   - `java.`, `javax.`, `android.`, `androidx.` — platform
 *   - `kotlin.`, `kotlinx.` — Kotlin stdlib + coroutines + serialization
 *   - `okhttp3.`, `okio.` — networking
 *   - `org.jsoup.` — HTML parser
 *   - `uy.kohesive.injekt.` — DI framework
 *   - `eu.kanade.tachiyomi.animesource.` — the source API (vendored in Tensei)
 *   - `eu.kanade.tachiyomi.network.` — network helpers (vendored)
 *   - `eu.kanade.tachiyomi.util.` — utility helpers (vendored)
 *
 * For everything else (e.g., `eu.kanade.tachiyomi.animeextension.en.aniwave.*`
 * — the extension's own code), we look in the extension's dex FIRST, then
 * fall back to the parent. This is parent-last for the extension's own
 * classes, which is correct because the host doesn't have them.
 *
 * ## Drop-in replacement
 *
 * This file replaces the previous pure parent-last version that caused
 * `LinkageError` on extensions bundling `kotlin.coroutines.Continuation`
 * (and similar shared classes). Same file path, same class name, same
 * constructor signature — only the loadClass logic changes.
 */
class ChildFirstPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
    private val parentFirstPackages: Set<String> = DEFAULT_PARENT_FIRST_PACKAGES,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    private val systemClassLoader: ClassLoader? = getSystemClassLoader()

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        // 1. Already loaded by THIS classloader? Use it.
        var c = findLoadedClass(name)

        // 2. Always try the system classloader first for platform classes
        //    (java.*, javax.*, android.* — these MUST come from the platform,
        //    never from an extension's dex).
        if (c == null && systemClassLoader != null) {
            c = try {
                systemClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                null
            }
        }

        // 3. For shared library packages (kotlin.*, kotlinx.*, okhttp3.*,
        //    eu.kanade.tachiyomi.animesource.*, etc.), delegate to the PARENT
        //    (host app's classloader) BEFORE trying the extension's own dex.
        //    This is the key fix for `LinkageError`: it guarantees the host's
        //    copy of `kotlin.coroutines.Continuation` (and similar shared
        //    classes) is the one used by both the host's interface methods
        //    AND the extension's overriding methods, so the JVM sees them
        //    as the same class identity.
        if (c == null && name != null && parentFirstPackages.any { name.startsWith(it) }) {
            c = try {
                parent.loadClass(name)
            } catch (_: ClassNotFoundException) {
                null
            }
        }

        // 4. Otherwise (or if parent didn't have it — e.g., the extension's
        //    own package code), try the EXTENSION's dex.
        if (c == null) {
            c = try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                // 5. Final fallback: parent classloader.
                super.loadClass(name, resolve)
            }
        }

        if (resolve) {
            resolveClass(c)
        }
        return c
    }

    companion object {
        /**
         * Packages that MUST be loaded parent-first to avoid class identity
         * mismatches between the host app's bundled copy and the extension's
         * bundled copy. Without parent-first, you get `LinkageError` because
         * e.g. `kotlin.coroutines.Continuation` from the extension's
         * classloader is a different class identity from
         * `kotlin.coroutines.Continuation` from the host's classloader.
         *
         * Add to this set if you encounter new `LinkageError`s on classes
         * from other shared libraries.
         */
        val DEFAULT_PARENT_FIRST_PACKAGES: Set<String> = setOf(
            // Platform / framework
            "java.",
            "javax.",
            "android.",
            "androidx.",
            // Kotlin stdlib + kotlinx (coroutines, serialization)
            "kotlin.",
            "kotlinx.",
            // Networking
            "okhttp3.",
            "okio.",
            // HTML parsing
            "org.jsoup.",
            // DI
            "uy.kohesive.injekt.",
            // Vendored Aniyomi source API surface — MUST match host's copy
            "eu.kanade.tachiyomi.animesource.",
            "eu.kanade.tachiyomi.network.",
            "eu.kanade.tachiyomi.util.",
            "eu.kanade.tachiyomi.extension.anime.", // host's extension-loader package
        )
    }
}
