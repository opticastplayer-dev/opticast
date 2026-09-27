package com.opticast.player.player.mpv

/** Reject startup-zero/stale native samples; manual seeks intentionally replace the target. */
internal class MpvResumeGuard(val generation: Long, val videoId: String, resumeMs: Long) {
    private var target = resumeMs.coerceAtLeast(0)
    private var settled = false
    fun manualSeek(positionMs: Long) { target = positionMs.coerceAtLeast(0); settled = false }
    fun accept(eventGeneration: Long, positionMs: Long): Boolean {
        if (eventGeneration != generation || positionMs < 0) return false
        if (!settled && kotlin.math.abs(positionMs.toDouble() - target) > 2000.0) return false
        settled = true
        return true
    }
}
