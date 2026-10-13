package com.blissless.tensei.extensions.manga.index

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Wire models for the Keiyoushi/Mihon extension index.
 *
 * Field numbers match Mihon's `NetworkExtensionStore` so the protobuf index can be decoded.
 * Every field has a default so partial payloads still decode.
 */
@Serializable
data class NetworkExtensionStore(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val badgeLabel: String = "",
    @ProtoNumber(3) val signingKey: String = "",
    @ProtoNumber(101) val extensionList: ExtensionList? = null,
    @ProtoNumber(102) val extensionListUrl: String? = null,
) {
    @Serializable
    data class ExtensionList(
        @ProtoNumber(1) val extensions: List<Extension> = emptyList(),
    )

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String = "",
        @ProtoNumber(2) val packageName: String = "",
        @ProtoNumber(3) val resources: Resources = Resources(),
        @ProtoNumber(4) val extensionLib: String = "",
        @ProtoNumber(5) val versionCode: Long = 0L,
        @ProtoNumber(6) val versionName: String = "",
        @ProtoNumber(7) val contentWarning: ContentWarning = ContentWarning.UNSPECIFIED,
        @ProtoNumber(8) val sources: List<Source> = emptyList(),
    )

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String = "",
        @ProtoNumber(2) val iconUrl: String = "",
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long = 0L,
        @ProtoNumber(2) val name: String = "",
        @ProtoNumber(3) val language: String = "",
        @ProtoNumber(4) val homeUrl: String = "",
    )

    @Serializable
    enum class ContentWarning {
        @ProtoNumber(0)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }
}

/**
 * `repo.json`, the store root. Points at the protobuf index when present.
 */
@Serializable
data class NetworkLegacyExtensionRepo(
    @SerialName("index_v2")
    val indexV2: String? = null,
    val meta: Meta,
) {
    @Serializable
    data class Meta(
        val name: String = "",
        val shortName: String? = null,
        val website: String = "",
        val signingKeyFingerprint: String = "",
    )
}

/**
 * `index.min.json`, the legacy flat list.
 */
@Serializable
data class NetworkLegacyExtension(
    val name: String = "",
    val pkg: String = "",
    val apk: String = "",
    val lang: String = "",
    val code: Long = 0L,
    val version: String = "",
    val nsfw: Int = 0,
    val sources: List<Source>? = null,
) {
    @Serializable
    data class Source(
        val id: Long = 0L,
        val lang: String = "",
        val name: String = "",
        val baseUrl: String = "",
    )
}
