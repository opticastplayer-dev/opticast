package com.opticast.player.player

import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl

/** Retain Media3 loading/allocator/network/rebuffer defaults; relax only known-local start/seek gating. */
internal class StartupLoadControl : DefaultLoadControl() {
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
