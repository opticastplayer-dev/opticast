package com.opticast.player.player.mpv
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
internal data class MpvInfo(val mediaId: String, val hardwareMode: String?, val videoCodec: String?, val audioCodec: String?, val droppedFrames: String?)
internal object MpvRuntimeInfo { var current: MpvInfo? by mutableStateOf(null) }

internal data class MpvCacheSnapshot(val mediaId: String, val totalBytes: Long?, val forwardBytes: Long?, val seconds: Double?, val forwardLimit: Long? = null, val backLimit: Long? = null, val donation: String? = null, val trialApplied: Boolean = false, val autoBufferStatus: String = "Inactive")
