package com.opticast.player.player

import android.os.Debug
import java.util.Locale

internal fun diagnosticNonNegativeLong(value: String?): Long? = value?.toLongOrNull()?.takeIf { it >= 0 }
internal fun diagnosticNonNegativeDouble(value: String?): Double? = value?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
internal fun diagnosticMiB(bytes: Long?): String = bytes?.takeIf { it >= 0 }?.let { String.format(Locale.ROOT, "%.1f MiB", it / 1048576.0) } ?: "Not reported"
internal fun positionRefreshDelayMs(visible: Boolean, timedOverlay: Boolean, pendingResume: Boolean): Long =
    if (visible || timedOverlay || pendingResume) 400L else 2000L

internal fun diagnosticPlaybackState(hasItem: Boolean, state: Int, playing: Boolean, playWhenReady: Boolean, suppression: Int): String = when {
    !hasItem -> "Idle (no item)"
    state == 1 -> "Idle (item loaded)"
    state == 2 -> "Buffering · play requested: $playWhenReady"
    state == 4 -> "Ended"
    state == 3 && playing -> "Ready · playing"
    state == 3 && !playWhenReady -> "Ready · paused"
    state == 3 -> "Ready · not advancing (suppression $suppression)"
    else -> "Unknown state $state"
}

/** Deliberately has no URI, filename, title, account or credential fields. */
internal data class MemorySnapshot(
    val capturedAt: Long,
    val totalPssKiB: Long,
    val javaPrivateKiB: Long?,
    val nativePrivateKiB: Long?,
    val graphicsKiB: Long?,
    val engine: String = "No active session",
    val state: String = "Idle",
    val hardware: String = "Not reported",
    val width: Int = 0,
    val height: Int = 0,
    val pip: Boolean = false,
    val demuxerBytes: Long? = null,
    val forwardBytes: Long? = null,
    val cacheSeconds: Double? = null,
    val forwardLimit: Long? = null,
    val backLimit: Long? = null,
    val donation: String? = null,
    val trialApplied: Boolean? = null,
    val autoBufferStatus: String? = null,
) {
    fun report(): String = buildString {
        appendLine("OptiCast memory snapshot")
        appendLine("Captured: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(java.util.Date(capturedAt)))
        appendLine("Process PSS: ${diagnosticMiB(totalPssKiB * 1024)}")
        appendLine("Java heap (private): ${diagnosticMiB(javaPrivateKiB?.times(1024))}")
        appendLine("Native heap (private): ${diagnosticMiB(nativePrivateKiB?.times(1024))}")
        appendLine("Graphics (attributed): ${diagnosticMiB(graphicsKiB?.times(1024))}")
        appendLine("Engine: $engine · $state")
        appendLine("Hardware mode: $hardware")
        appendLine("Resolution: ${if (width > 0 && height > 0) "$width × $height" else "Not reported"}")
        appendLine("PiP: ${if (pip) "Yes" else "No"}")
        if (engine == "mpv") {
            appendLine("Local buffer trial: ${when (trialApplied) { true -> "Applied"; false -> "Not applied (off, ineligible source or automatic adjustment)"; null -> "Not reported" }}")
            appendLine("Automatic buffer fallback: ${autoBufferStatus ?: "Not reported"}")
            appendLine("Effective forward limit: ${diagnosticMiB(forwardLimit)}")
            appendLine("Effective backward limit: ${diagnosticMiB(backLimit)}")
            appendLine("Backward buffer donation: ${donation ?: "Not reported"}")
            appendLine("Demuxer cache: ${diagnosticMiB(demuxerBytes)}")
            appendLine("Forward packets: ${diagnosticMiB(forwardBytes)}")
            appendLine("Cache ahead (estimate): ${cacheSeconds?.let { String.format(Locale.ROOT, "%.2f s", it) } ?: "Not reported"}")
        }
        append("One sample, not an average. App process only. Cache duration is an unreliable estimate; buffer limits are not total RAM caps.")
    }
}

/** Called on IO only, exclusively following an explicit user action. */
internal fun readProcessMemorySnapshot(): MemorySnapshot {
    val info = Debug.MemoryInfo()
    Debug.getMemoryInfo(info)
    fun stat(key: String) = diagnosticNonNegativeLong(runCatching { info.getMemoryStat(key) }.getOrNull())
    return MemorySnapshot(System.currentTimeMillis(), info.totalPss.toLong(),
        stat("summary.java-heap"), stat("summary.native-heap"), stat("summary.graphics"))
}
