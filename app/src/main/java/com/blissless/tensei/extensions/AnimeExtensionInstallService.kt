package com.blissless.tensei.extensions

import android.app.Service
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.util.Log

/**
 * Foreground service that receives the result callback for
 * `PackageInstaller`-based extension installs.
 *
 * Ported from Aniyomi's `AnimeExtensionInstallService` at
 * `app/src/main/java/eu/kanade/tachiyomi/extension/anime/util/AnimeExtensionInstallService.kt`.
 *
 * When `AnimeExtensionInstaller.installPackageInstaller()` commits a
 * `PackageInstaller.Session`, it passes this service's IntentSender as
 * the result callback. The system then delivers the install result here,
 * where we can inspect status / failure cause and trigger an
 * `AnimeExtensionManager` re-scan on success.
 *
 * NOTE: For this service to work, you must declare it in
 * `AndroidManifest.xml`:
 *
 *   <service android:name=".extensions.AnimeExtensionInstallService"
 *       android:exported="false" />
 *
 * (See `patches/AndroidManifest.xml.patch`.)
 */
class AnimeExtensionInstallService : Service() {

    override fun onCreate() {
        super.onCreate()
        // Foreground notification channel + notification setup would
        // go here. We keep this minimal — on Android 12+ the system
        // allows up to 5 seconds without a foreground notification for
        // install sessions, which is plenty for an APK install.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, -999) ?: -999
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // System needs user confirmation — launch the confirm activity.
                // `intent` is nullable in `onStartCommand` — guard with `?.let`.
                intent?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { confirmIntent ->
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Log.i(TAG, "Extension install success")
                // The AnimeExtensionInstallReceiver will pick up the
                // PACKAGE_ADDED broadcast and re-scan automatically.
            }
            else -> {
                val msg = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""
                Log.e(TAG, "Extension install failed: status=$status msg=$msg")
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        private const val TAG = "AnimeExtensionInstallService"

        fun getIntent(context: android.content.Context, sessionId: Int): Intent =
            Intent(context, AnimeExtensionInstallService::class.java)
                .putExtra(EXTRA_SESSION_ID, sessionId)
    }
}
