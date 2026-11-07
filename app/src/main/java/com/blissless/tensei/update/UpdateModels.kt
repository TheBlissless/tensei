package com.blissless.tensei.update

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<ReleaseAsset> = emptyList()
)

@Serializable
data class ReleaseAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0
)

/**
 * The ABI this device should download an APK for, normalised to the naming
 * used by the release assets (arm64-v8a, armeabi-v7a, x86_64, x86).
 */
internal fun deviceAbi(): String {
    val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    return when {
        abi.startsWith("arm64") -> "arm64-v8a"
        abi.startsWith("armeabi") -> "armeabi-v7a"
        abi.startsWith("x86_64") -> "x86_64"
        abi.startsWith("x86") -> "x86"
        else -> abi
    }
}


