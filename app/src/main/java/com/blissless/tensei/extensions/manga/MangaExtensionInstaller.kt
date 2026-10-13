package com.blissless.tensei.extensions.manga

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient

/**
 * Installs a manga extension apk like Mihon does: as a real system package via
 * [PackageInstaller], with no user confirmation on Android 12+. If the system install can't
 * complete, the apk is instead copied into the app's private extension directory so it can
 * still be loaded.
 */
object MangaExtensionInstaller {

    private const val TAG = "MangaExtInstaller"
    private const val ACTION_INSTALL = "com.blissless.tensei.MANGA_EXTENSION_INSTALL"

    private val pendingInstalls = ConcurrentHashMap<Int, CompletableDeferred<Result<Unit>>>()

    @Volatile
    private var latestSessionId = -1

    @Volatile
    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            var sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
            if (sessionId == -1) sessionId = latestSessionId
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                val action = getParcelableExtraCompat<Intent>(intent, Intent.EXTRA_INTENT)
                if (action == null) {
                    pendingInstalls.remove(sessionId)
                        ?.complete(Result.failure(IllegalStateException("system installer gave no confirmation")))
                    return
                }
                try {
                    action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(action)
                } catch (e: Exception) {
                    pendingInstalls.remove(sessionId)?.complete(Result.failure(e))
                }
                return
            }
            val deferred = pendingInstalls.remove(sessionId) ?: return
            val result = when (status) {
                PackageInstaller.STATUS_SUCCESS -> Result.success(Unit)
                PackageInstaller.STATUS_FAILURE_ABORTED ->
                    Result.failure(InstallCancelledException())
                else -> Result.failure(
                    IllegalStateException(
                        intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            ?: "package install failed (status $status)",
                    ),
                )
            }
            deferred.complete(result)
        }
    }

    suspend fun install(context: Context, client: OkHttpClient, apkUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = client.newCall(GET(apkUrl)).awaitSuccess()
                val temp = File.createTempFile("manga_extension", ".apk", context.cacheDir)
                try {
                    temp.writeBytes(response.body.bytes())

                    val pkgInfo = MangaExtensionLoader.archivePackageInfo(context, temp)
                        ?: throw IllegalStateException("downloaded file is not a valid apk")
                    if (!MangaExtensionLoader.isExtension(pkgInfo)) {
                        throw IllegalStateException("apk is not a manga extension")
                    }

                    runCatching { installAsPackage(context, temp, pkgInfo) }
                        .fold(
                            onSuccess = {
                                "Installed ${pkgInfo.packageName}"
                            },
                            onFailure = { e ->
                                if (e is InstallCancelledException) throw e
                                Log.w(TAG, "package install unavailable (${e.message}); installing privately")
                                copyToPrivate(context, temp, pkgInfo)
                                "Installed ${pkgInfo.packageName} (private)"
                            },
                        )
                } finally {
                    temp.delete()
                }
            }
        }

    private suspend fun installAsPackage(context: Context, apk: File, pkgInfo: PackageInfo) {
        val packageInstaller = context.packageManager.packageInstaller

        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        val sessionId = packageInstaller.createSession(params)
        latestSessionId = sessionId
        params.setSize(apk.length())

        val deferred = CompletableDeferred<Result<Unit>>()
        pendingInstalls[sessionId] = deferred
        registerReceiver(context)

        try {
            packageInstaller.openSession(sessionId).use { session ->
                session.openWrite(pkgInfo.packageName, 0L, apk.length()).use { output ->
                    apk.inputStream().use { ins -> ins.copyTo(output) }
                    session.fsync(output)
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                val sender = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(ACTION_INSTALL).setPackage(context.packageName),
                    flags,
                ).intentSender
                session.commit(sender)
            }
        } catch (e: Exception) {
            pendingInstalls.remove(sessionId)
            runCatching { packageInstaller.abandonSession(sessionId) }
            throw e
        }

        val outcome = withTimeoutOrNull(INSTALL_TIMEOUT_MS) { deferred.await() }
            ?: run {
                pendingInstalls.remove(sessionId)
                runCatching { packageInstaller.abandonSession(sessionId) }
                throw IllegalStateException("package install timed out")
            }
        outcome.getOrThrow()
    }

    private fun copyToPrivate(context: Context, apk: File, pkgInfo: PackageInfo) {
        val dir = MangaExtensionLoader.privateExtensionDir(context).apply { mkdirs() }
        val target = File(dir, "${pkgInfo.packageName}.apk")
        if (target.exists() && !target.delete()) {
            throw IllegalStateException("cannot replace installed extension")
        }
        apk.copyTo(target, overwrite = true)
        if (!target.setReadOnly()) {
            target.delete()
            throw IllegalStateException("cannot mark apk as read-only")
        }
    }

    @Synchronized
    private fun registerReceiver(context: Context) {
        if (receiverRegistered) return
        receiverRegistered = true
        val appContext = context.applicationContext
        val filter = IntentFilter(ACTION_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }
    }

    suspend fun uninstall(context: Context, pkgName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Private install first.
                val file = File(MangaExtensionLoader.privateExtensionDir(context), "$pkgName.apk")
                if (file.exists() && !file.delete()) {
                    throw IllegalStateException("cannot delete $pkgName")
                }
                // Shared install: hand off to the system package installer.
                if (isPackageInstalled(context, pkgName)) {
                    val intent = Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:$pkgName"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
        }

    private fun isPackageInstalled(context: Context, pkgName: String): Boolean =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(pkgName, 0)
            true
        }.getOrDefault(false)

    private inline fun <reified T> getParcelableExtraCompat(intent: Intent, name: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name)
        }

    private const val INSTALL_TIMEOUT_MS = 300_000L
}

class InstallCancelledException : IllegalStateException("install cancelled")
