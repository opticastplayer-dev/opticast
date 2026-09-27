package com.opticast.player.player

internal const val LOCAL_START_BUFFER_MS = 350

/** Document/cloud providers are not assumed to be local merely because they use content://. */
internal fun knownLocalPlayback(scheme: String?, authority: String?): Boolean =
    scheme.equals("file",true) || scheme.equals("android.resource",true) ||
        (scheme.equals("content",true) && authority.equals("media",true))

internal fun localStartReady(scheme: String?, authority: String?, bufferedUs: Long, speed: Float, rebuffering: Boolean, live: Boolean): Boolean =
    !rebuffering && !live && knownLocalPlayback(scheme,authority) && speed.isFinite() && speed>0f && bufferedUs>=0 &&
        bufferedUs.toDouble() >= LOCAL_START_BUFFER_MS * 1000.0 * speed

internal enum class StartupStage(val label: String) {
    RESOLVED("File verified"), CONNECTED("Session connected"), PREPARED("Item submitted"), READY("Engine ready"), PLAYING("Playback running"), FRAME("First video frame")
}
internal data class StartupTrace(val mediaId: String, val startedAtMs: Long, val elapsedMs: Map<StartupStage,Long> = emptyMap())
internal fun recordStartup(trace: StartupTrace?, mediaId: String?, stage: StartupStage, nowMs: Long): StartupTrace? =
    if(trace==null || trace.mediaId!=mediaId || nowMs<trace.startedAtMs || stage in trace.elapsedMs) trace
    else trace.copy(elapsedMs=trace.elapsedMs+(stage to (nowMs-trace.startedAtMs)))

internal fun needsPlayerCollectionScan(cachedCount: Int, explicitRefresh: Boolean): Boolean = explicitRefresh || cachedCount==0
