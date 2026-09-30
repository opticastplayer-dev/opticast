package com.opticast.player.player

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import com.opticast.player.data.AppContainer

/** Retain Media3 defaults but reduce buffer for low-RAM to save RAM during playback — 10/10 adaptive.
 *  Low-RAM: min 1s, max 10s, playback 0.5s, rebuffer 0.5s (was 50s max)
 *  Normal: min 1.5s, max 30s (was 50s), playback 1s, rebuffer 1s — saves RAM while keeping smooth local playback
 */
internal class StartupLoadControl : DefaultLoadControl(
    DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            if (AppContainer.lowRamMode) 1000 else 1500,
            if (AppContainer.lowRamMode) 10000 else 30000,
            if (AppContainer.lowRamMode) 500 else 1000,
            if (AppContainer.lowRamMode) 500 else 1000
        )
        .setBackBuffer(0, false)
        .build()
) {
    private val window=Timeline.Window()
    private val period=Timeline.Period()
    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean {
        if(super.shouldStartPlayback(parameters)) return true
        if(parameters.rebuffering || parameters.timeline.isEmpty) return false
        val index=parameters.timeline.getIndexOfPeriod(parameters.mediaPeriodId.periodUid)
        if(index==C.INDEX_UNSET) return false
        parameters.timeline.getPeriod(index,period)
        parameters.timeline.getWindow(period.windowIndex,window)
        val uri=window.mediaItem.localConfiguration?.uri ?: return false
        return localStartReady(uri.scheme,uri.authority,parameters.bufferedDurationUs,parameters.playbackSpeed,parameters.rebuffering,window.isLive)
    }
}
