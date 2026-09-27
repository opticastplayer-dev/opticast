package com.opticast.player.player

internal enum class ProbeStage(val label: String) {
    SUBMITTED("Item submitted; requested position ms (-1 = engine default)"),
    OPEN("Primary-source open; byte offset"), OPENED("Primary-source opened; call duration ms"), OPEN_FAILED("Primary-source open failed; call duration ms"),
    FIRST_BYTE("First primary-source bytes returned"), TRACKS("Tracks available"), VIDEO_INPUT("Video input format available"), AUDIO_INPUT("Audio input format available"),
    VIDEO_DECODER("Video decoder initialized; initialization duration ms"), AUDIO_DECODER("Audio decoder initialized; initialization duration ms"),
    SEEK("Engine seek discontinuity; position ms"), SEEK_ADJUSTED("Engine seek adjustment; position ms"), RESUME_CORRECTION("OptiCast resume-correction seek; target ms"),
    READY("Engine ready"), FRAME("First video frame"), PLAYING("Playback running"), ERROR("Playback error; code")
}

/** One bounded, in-memory request. No raw URI, title, exception, headers or media ID in export. */
internal class ResumeProbe(val mediaId: String, val startedMs: Long) {
    @Volatile private var source: String? = null
    @Volatile private var frozenMs: Long? = null
    @Volatile var submittedMs: Long? = null
        private set
    private val events=mutableListOf<String>()
    private var omitted=0
    private val firstStages=mutableSetOf<ProbeStage>()
    private var opens=0L
    private var reads=0L
    private var bytes=0L
    private var readNs=0L
    private var maxReadNs=0L
    private var readErrors=0L
    private var videoDecoder: String?=null
    private var audioDecoder: String?=null
    fun bind(uri: String) { source=uri }
    fun matches(uri: String): Boolean = source==uri
    fun collecting(): Boolean = ResumeProbes.current===this && frozenMs==null
    @Synchronized fun event(stage: ProbeStage, nowMs: Long, value: Long?=null, inferred: Boolean=false) {
        if(ResumeProbes.current!==this || nowMs<startedMs || frozenMs?.let { nowMs>it }==true) return
        val repeatable=stage in setOf(ProbeStage.OPEN,ProbeStage.OPENED,ProbeStage.OPEN_FAILED,ProbeStage.SEEK,ProbeStage.SEEK_ADJUSTED,ProbeStage.RESUME_CORRECTION,ProbeStage.SUBMITTED)
        if(!repeatable && !firstStages.add(stage)) return
        if(stage==ProbeStage.SUBMITTED) submittedMs=nowMs
        if(stage==ProbeStage.OPEN) opens++
        val line="${nowMs-startedMs} ms: ${stage.label}"+(value?.let { " = $it" } ?: "")+if(inferred) " [current-item inferred]" else ""
        if(events.size<96) events+=line else omitted++
        if(stage==ProbeStage.PLAYING || stage==ProbeStage.ERROR) frozenMs=nowMs
    }
    @Synchronized fun read(count: Int, durationNs: Long, nowMs: Long, failed: Boolean=false) {
        if(!collecting() || nowMs<startedMs) return
        reads++;readNs+=durationNs.coerceAtLeast(0);maxReadNs=maxOf(maxReadNs,durationNs)
        if(failed) readErrors++
        if(count>0) { bytes+=count;if(ProbeStage.FIRST_BYTE !in firstStages) event(ProbeStage.FIRST_BYTE,nowMs) }
    }
    @Synchronized fun decoder(video: Boolean, name: String, nowMs: Long, durationMs: Long, inferred: Boolean) {
        if(ResumeProbes.current!==this || nowMs<startedMs || frozenMs?.let { nowMs>it }==true) return
        if(nowMs < (submittedMs ?: return)) return
        val label=safeDecoderName(name)+(if(inferred) " [current-item inferred]" else " [timeline matched]")
        if(video) videoDecoder=label else audioDecoder=label
        event(if(video) ProbeStage.VIDEO_DECODER else ProbeStage.AUDIO_DECODER,nowMs,durationMs,inferred)
    }
    @Synchronized fun report(): String = buildString {
        appendLine("Resume preparation trace: "+(frozenMs?.let { "frozen at ${it-startedMs} ms" } ?: "collecting; refresh after playback begins"))
        appendLine("Video decoder initialization observed: ${videoDecoder ?: "not recorded; may be reused or unattributed"}")
        appendLine("Audio decoder initialization observed: ${audioDecoder ?: "not recorded; may be reused or unattributed"}")
        appendLine("Primary-source opens: $opens; read calls: $reads; bytes returned: $bytes; read failures: $readErrors")
        appendLine("Inside primary-source read calls: ${readNs/1_000_000.0} ms total; ${maxReadNs/1_000_000.0} ms longest")
        events.forEach { appendLine(it) }
        if(omitted>0) appendLine("Additional events omitted: $omitted (96-event limit)")
        appendLine("Newly created data sources in this request, primary-source URI matching only; previously reused sources may be unobserved. Excludes separate subtitle files and differently addressed stream segments. Byte offsets are not playback timestamps. Reopens alone do not prove an index scan. Read durations may overlap other work; do not subtract them as exclusive CPU time. This is not a throughput benchmark.")
    }
}
internal object ResumeProbes {
    @Volatile var current: ResumeProbe?=null
        private set
    fun begin(id: String, nowMs: Long) { current=ResumeProbe(id,nowMs) }
    fun clear() { current=null }
    fun forId(id: String?): ResumeProbe? = current?.takeIf { id!=null && it.mediaId==id }
}
