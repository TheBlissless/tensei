package com.blissless.tensei.stream

import dalvik.system.PathClassLoader

/**
 * Parent-last (child-first) ClassLoader for loading Aniyomi-style extension APKs.
 *
 * This is a direct port of Aniyomi's `ChildFirstPathClassLoader` from
 * `app/src/main/java/eu/kanade/tachiyomi/util/system/ChildFirstPathClassLoader.kt`.
 *
 * WHY PARENT-LAST?
 *   Aniyomi extensions ship their own compiled copy of the
 *   `eu.kanade.tachiyomi.animesource.*` API inside the APK. When the extension
 *   instantiates its `AnimeSource` class, that class's bytecode references the
 *   `eu.kanade.tachiyomi.animesource.AnimeSource` interface — and it MUST be
 *   the same copy the extension was compiled against, or you'll get
 *   `LinkageError` / `ClassCastException` at runtime.
 *
 *   With a parent-FIRST classloader (the old Tensei `ParentFirstClassLoader`),
 *   the JVM always loads `eu.kanade.tachiyomi.*` from the host app first,
 *   silently breaking any extension compiled against a different ABI version
 *   of the source API (ext-lib 16 vs 17, etc.).
 *
 *   With a parent-LAST classloader, the extension's own dex is searched first;
 *   only if the class is not present in the extension do we fall back to the
 *   parent (host app) classloader. This is the exact strategy Aniyomi uses,
 *   and is what allows Aniyomi to mix extensions of different lib versions
 *   in the same install.
 *
 * `PathClassLoader` is the post-API-26 successor to `DexClassLoader` and is
 * what Aniyomi uses; the only behavioural difference vs `DexClassLoader` is
 * that `PathClassLoader` is optimised for APKs already on the local filesystem
 * (which is exactly our case for installed extension APKs).
 *
 * Drop-in replacement for the old `ParentFirstClassLoader` inner class that
 * used to live in `ExtensionLoader.kt`.
 */
class ChildFirstPathClassLoader(
    dexPath: String,
    librarySearchPath: String?,
    parent: ClassLoader,
) : PathClassLoader(dexPath, librarySearchPath, parent) {

    private val systemClassLoader: ClassLoader? = getSystemClassLoader()

    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        // 1. Already loaded?
        var c = findLoadedClass(name)

        // 2. Try the system classloader (java.*, kotlin.*, etc.) first — these
        //    must always come from the host platform, never from an extension.
        if (c == null && systemClassLoader != null) {
            c = try {
                systemClassLoader.loadClass(name)
            } catch (_: ClassNotFoundException) {
                null
            }
        }

        // 3. Otherwise, look in the EXTENSION's own dex first (parent-last).
        if (c == null) {
            c = try {
                findClass(name)
            } catch (_: ClassNotFoundException) {
                // 4. Final fallback: parent classloader.
                super.loadClass(name, resolve)
            }
        }

        if (resolve) {
            resolveClass(c)
        }
        return c
    }
}
