package com.opticast.player.player.mpv

/** Worker-owned, monotonic-time policy. A single attempt per native playback session. */
internal class AdaptiveBufferPolicy {
    private var graceUntil = Long.MAX_VALUE
    private var lastSample: Long? = null
    private var starvingSince: Long? = null
    var attempted = false
        private set

    fun defer(now: Long) {
        graceUntil = now + 8000L
        starvingSince = null
        lastSample = null
    }

    fun observe(now: Long, underrun: Boolean?, eof: Boolean?, idle: Boolean?): Boolean {
        if (attempted || now < graceUntil) return false
        val last = lastSample
        if (last != null && (now < last || now - last > 2500L)) starvingSince = null
        lastSample = now
        if (underrun != true || eof != false || idle != false) {
            starvingSince = null
            return false
        }
        val since = starvingSince ?: now.also { starvingSince = it }
        if (now - since < 4000L) return false
        attempted = true
        return true
    }
}

// Defaults of the pinned, controlled mpv build; not a total process-memory budget.
internal val normalPacketBufferOptions = linkedMapOf(
    "demuxer-max-bytes" to (150L * 1024 * 1024).toString(),
    "demuxer-max-back-bytes" to (50L * 1024 * 1024).toString(),
    "demuxer-donate-buffer" to "yes",
)
internal enum class BufferExpansion { EXPANDED, SMALL_RESTORED, INCOMPLETE }

/** Never restart playback or throw an engine error for an optional buffer adjustment. */
internal fun expandPacketBuffer(set: (String, String) -> Int, get: (String) -> String?): BufferExpansion {
    fun apply(options: Map<String, String>): Boolean {
        var ok = true
        for ((key, value) in options) if (runCatching { set(key, value) >= 0 }.getOrDefault(false).not()) ok = false
        for ((key, value) in options) if (runCatching { get("options/$key") == value }.getOrDefault(false).not()) ok = false
        return ok
    }
    if (apply(normalPacketBufferOptions)) return BufferExpansion.EXPANDED
    return if (apply(localPacketBufferOptions)) BufferExpansion.SMALL_RESTORED else BufferExpansion.INCOMPLETE
}
