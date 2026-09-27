package com.opticast.player.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Live decoder/engine facts for the playback-info overlay.
 *
 * Media3 exposes most of what the overlay shows through the Player interface
 * (formats, video size, buffer), but the decoder names and the dropped-frame
 * counter only exist on the analytics callbacks. Those arrive on the app's main
 * thread from the service's ExoPlayer, so this is a tiny observable holder
 * rather than a full state machine.
 */
object PlayerStats {
    internal var startup: StartupTrace? by mutableStateOf(null)
        private set
    internal fun beginStartup(mediaId: String, requestedAtMs: Long) {
        reset()
        startup=StartupTrace(mediaId,requestedAtMs)
        ResumeProbes.begin(mediaId,requestedAtMs)
    }
    internal fun markStartup(mediaId: String?, stage: StartupStage, nowMs: Long = android.os.SystemClock.elapsedRealtime()) {
        startup=recordStartup(startup,mediaId,stage,nowMs)
    }


    internal var bufferingHealth: BufferingHealth by mutableStateOf(BufferingHealth())
        private set
    internal fun onBuffering(mediaId: String?, buffering: Boolean, nowMs: Long = android.os.SystemClock.elapsedRealtime()) {
        if(mediaId==null || startup?.mediaId!=mediaId) return
        bufferingHealth=bufferingHealth.observe(buffering,nowMs)
    }
    internal var fullVideoDecoder: String? by mutableStateOf(null)
        private set
    internal var fullAudioDecoder: String? by mutableStateOf(null)
        private set
    var videoDecoder: String? by mutableStateOf(null)
        private set
    var audioDecoder: String? by mutableStateOf(null)
        private set
    var droppedFrames: Long by mutableStateOf(0L)
        private set
    var bandwidthEstimate: Long by mutableStateOf(0L)
        private set

    fun onVideoDecoder(name: String, mediaId: String?) {
        if(mediaId==null || startup?.mediaId!=mediaId) return
        fullVideoDecoder=safeDecoderName(name)
        videoDecoder = name.substringAfterLast('.')
    }

    fun onAudioDecoder(name: String, mediaId: String?) {
        if(mediaId==null || startup?.mediaId!=mediaId) return
        fullAudioDecoder=safeDecoderName(name)
        audioDecoder = name.substringAfterLast('.')
    }

    fun onDroppedFrames(count: Int, mediaId: String?) {
        if(mediaId==null || startup?.mediaId!=mediaId) return
        droppedFrames += count.coerceAtLeast(0)
    }

    fun onBandwidth(bytesPerSecond: Long) {
        bandwidthEstimate = bytesPerSecond
    }

    /** Called when a new item is prepared so stale numbers never linger. */
    fun reset() {
        startup=null
        ResumeProbes.clear()
        bufferingHealth=BufferingHealth()
        fullVideoDecoder=null
        fullAudioDecoder=null
        videoDecoder = null
        audioDecoder = null
        droppedFrames = 0L
        bandwidthEstimate = 0L
    }
}
