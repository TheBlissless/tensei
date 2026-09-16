package com.blissless.tensei.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * BroadcastReceiver that listens for package install / replace / remove
 * events on the system and re-loads the corresponding extension (or
 * removes it from the live extension map).
 *
 * Ported from Aniyomi's `AnimeExtensionInstallReceiver` at
 * `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionInstallReceiver.kt`.
 *
 * This version has a **no-arg constructor** (required for manifest-declared
 * receivers — the system instantiates the class using reflection on the
 * default constructor). When `onReceive` fires, it directly calls
 * `AnimeExtensionManager.get(context).initAnimeExtensions()` — no listener
 * callback needed. This works for BOTH system-created instances (from the
 * manifest declaration) and runtime-registered instances (via [register]).
 *
 * The runtime registration via [register] is still useful for immediate
 * delivery — manifest-declared receivers have a delivery delay on Android 8+.
 */
class AnimeExtensionInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkgName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return
        when (action) {
            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.i(TAG, "Package change: action=$action pkg=$pkgName — triggering re-scan")
                try {
                    // Directly trigger re-scan via the manager singleton.
                    // Uses a background Thread (not coroutine) to avoid import
                    // complexity — the manager's initAnimeExtensions() is
                    // a suspend fun that handles its own dispatching.
                    val manager = AnimeExtensionManager.get(context)
                    Thread {
                        try {
                            kotlinx.coroutines.runBlocking {
                                manager.initAnimeExtensions()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Re-scan failed for $pkgName", e)
                        }
                    }.apply { isDaemon = true; name = "ExtReScan" }.start()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to trigger re-scan for $pkgName", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AnimeExtensionInstallReceiver"

        /**
         * Register this receiver for system package-change broadcasts.
         * Use `ContextCompat.registerReceiver` so we get the right receiver
         * export flag on Android 12+ (where the default is now "not exported").
         *
         * The manifest declaration handles cold-start delivery (when the
         * app process isn't running). This runtime registration provides
         * immediate delivery while the app is running.
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
                AnimeExtensionInstallReceiver(),
                filter,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.RECEIVER_NOT_EXPORTED
                else 0,
            )
        }

        /** Unregister a previously-registered receiver (best-effort). */
        fun unregister(context: Context, receiver: BroadcastReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Receiver wasn't registered — ignore.
            }
        }
    }
}
