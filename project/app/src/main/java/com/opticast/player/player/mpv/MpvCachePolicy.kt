package com.opticast.player.player.mpv

import kotlinx.serialization.json.*

internal data class PacketCacheMetrics(val totalBytes: Long?, val forwardBytes: Long?, val estimatedSeconds: Double?, val underrun: Boolean? = null, val eof: Boolean? = null, val idle: Boolean? = null)

/** mpv exposes this map as JSON through its whole-property string conversion. */
internal fun parsePacketCacheMetrics(text: String?): PacketCacheMetrics? {
    if (text == null || text.length > 131072) return null
    val map = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
    fun bytes(key: String) = (map[key] as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 }
    val seconds = (map["cache-duration"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it.isFinite() && it >= 0 }
    fun flag(key: String) = (map[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
    return PacketCacheMetrics(bytes("total-bytes"), bytes("fw-bytes"), seconds, flag("underrun"), flag("eof"), flag("idle"))
}

internal val localPacketBufferOptions = linkedMapOf(
    "demuxer-max-bytes" to (32L * 1024 * 1024).toString(),
    "demuxer-max-back-bytes" to (8L * 1024 * 1024).toString(),
    "demuxer-donate-buffer" to "no",
)

/** Unknown document/cloud providers are not assumed local merely because they expose a seekable FD. */
internal fun eligibleLocalBufferTrial(enabled: Boolean, scheme: String?, authority: String?, regular: Boolean, seekable: Boolean): Boolean {
    if (!enabled || !regular || !seekable) return false
    return when (scheme) {
        null, "file" -> authority.isNullOrBlank() || authority == "localhost"
        "content" -> authority in setOf("media", "com.android.providers.media.documents", "com.android.externalstorage.documents")
        else -> false
    }
}
