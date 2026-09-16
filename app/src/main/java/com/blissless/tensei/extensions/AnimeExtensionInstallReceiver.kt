package com.blissless.tensei.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import com.blissless.tensei.extensions.AnimeExtensionManager

/**
 * BroadcastReceiver that listens for package install / replace / remove
 * events on the system and re-loads the corresponding extension (or
 * removes it from the live extension map).
 *
 * Ported from Aniyomi's `AnimeExtensionInstallReceiver` at
 * `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionInstallReceiver.kt`.
 *
 * Why this matters:
 *   Before this receiver, Tensei would only discover new extensions
 *   when the user manually pressed "refresh" in the Extensions screen.
 *   If the user installed an extension APK from the Extensions screen
 *   via `Intent.ACTION_INSTALL_PACKAGE` and then navigated back to the
 *   Home screen, the new source wouldn't appear until they manually
 *   reloaded.
 *
 *   With this receiver registered, the `AnimeExtensionManager` is
 *   notified within seconds of ANY package install/replace/remove on
 *   the device, and the live `installedExtensionsFlow` automatically
 *   reflects the change. This is what Aniyomi does.
 *
 * The receiver is declared statically in `AndroidManifest.xml` (see
 * `patches/AndroidManifest.xml.patch`) so it works even when the app
 * is not running — the system will start the app process to deliver
 * the broadcast.
 */
class AnimeExtensionInstallReceiver(
    private val listener: Listener,
) : BroadcastReceiver() {

    fun interface Listener {
        /**
         * Called when a package relevant to extensions is installed,
         * replaced, or removed. Implementations should re-scan the
         * installed package set and update their live state.
         */
        fun onExtensionInstallChanged(action: String, pkgName: String)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pkgName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED -> {
                listener.onExtensionInstallChanged(action, pkgName)
            }
        }
    }

    /**
     * Register this receiver for system package-change broadcasts.
     * Use `ContextCompat.registerReceiver` so we get the right receiver
     * export flag on Android 12+ (where the default is now "not exported").
     */
    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            this,
            filter,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.RECEIVER_NOT_EXPORTED
            else 0,
        )
    }

    /** Unregister the receiver; safe to call even if never registered. */
    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(this)
        } catch (_: IllegalArgumentException) {
            // Receiver wasn't registered — ignore.
        }
    }
}
