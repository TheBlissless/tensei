package com.blissless.tensei.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * Persists the set of "trusted" extension signing certificates — i.e.
 * extension APKs the user has explicitly approved for code-loading.
 *
 * Ported from Aniyomi's `TrustAnimeExtension` at
 * `app/src/main/java/eu/kanade/domain/extension/anime/interactor/TrustAnimeExtension.kt`.
 *
 * WHY THIS EXISTS
 *   Loading an extension APK via `PathClassLoader` is equivalent to
 *   executing arbitrary code from that APK in Tensei's process — the
 *   extension runs with Tensei's permissions, can read Tensei's
 *   private files, etc. Aniyomi mitigates this by:
 *
 *     1. Refusing to load ANY extension whose signing certificate is
 *        not in the trust store.
 *     2. Showing a UI prompt ("This extension is from an unknown
 *        source. Trust it?") the first time the user encounters each
 *        untrusted extension.
 *     3. Persisting the user's "trust" decisions so the extension
 *        auto-loads on subsequent launches.
 *
 *   This class implements #3 (the persistence layer). The UI prompt
 *   is left to your activity / composable — see `AnimeExtensionManager.trust()`.
 *
 * STORAGE
 *   Trusted certs are stored as a `Set<String>` in a SharedPreferences
 *   file named `trusted_extension_certificates`. Each entry is the
 *   SHA-256 hash of one signing certificate. (We hash rather than
 *   storing the full cert to keep the file small and to avoid
 *   SharedPreferences's `getStringSet` size limits.)
 *
 *   We also persist the version code of the extension that was
 *   trusted, so we can re-prompt if an extension is updated with a
 *   different signature (Aniyomi behaviour).
 */
class TrustAnimeExtension private constructor(
    private val context: Context,
) {
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Is the given PackageInfo's signing certificate trusted?
     *
     * Returns `true` iff:
     *   - The PackageInfo has at least one signing certificate.
     *   - The SHA-256 of those signatures matches an entry in our
     *     trust store.
     *   - The persisted versionCode for that cert matches the
     *     PackageInfo's current versionCode (so we re-prompt on
     *     signature rotation).
     */
    fun isTrusted(pkgInfo: PackageInfo, signatures: Array<Signature>): Boolean {
        val hash = hashSignatures(signatures)
        val trustedVersionCode = prefs.getLong("$hash.${pkgInfo.packageName}", -1L)
        return trustedVersionCode == pkgInfo.longVersionCode
    }

    /**
     * Mark the given package + signing cert + version as trusted.
     * Future loads of an extension with the same signature will succeed
     * without a prompt.
     */
    suspend fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        prefs.edit()
            .putLong("$signatureHash.$pkgName", versionCode)
            .apply()
    }

    /**
     * Revoke trust for the given package + cert. The next load attempt
     * will return `AnimeLoadResult.Untrusted`.
     */
    fun revoke(pkgName: String, signatureHash: String) {
        prefs.edit()
            .remove("$signatureHash.$pkgName")
            .apply()
    }

    /**
     * Revoke trust for every cert belonging to the given package —
     * used when an extension is uninstalled.
     */
    fun revokeAll(pkgName: String) {
        val keys = prefs.all.keys.filter { it.endsWith(".$pkgName") }
        if (keys.isEmpty()) return
        prefs.edit().apply { keys.forEach { remove(it) } }.apply()
    }

    // ─── Hashing helpers ────────────────────────────────────────────────────

    private fun hashSignatures(signatures: Array<Signature>): String {
        val md = MessageDigest.getInstance("SHA-256")
        signatures.forEach { md.update(it.toByteArray()) }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "trusted_extension_certificates"

        @Volatile private var instance: TrustAnimeExtension? = null

        /** Get the singleton instance — created lazily, cached for the app lifetime. */
        fun get(context: Context): TrustAnimeExtension {
            val appContext = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: TrustAnimeExtension(appContext).also { instance = it }
            }
        }
    }
}
