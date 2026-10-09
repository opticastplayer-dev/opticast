@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.opticast.player.player.mpv

import android.content.*
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.view.*
import androidx.media3.common.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** MediaSession-compatible mpv engine. Simplified: always use smaller local buffer trial for lightness. */
class MpvPlayer(private val context: Context) : SimpleBasePlayer(Looper.getMainLooper()) {
    private val localBufferTrial: Boolean = true // always enabled, no setting for lightness
    private var localTrialApplied = false // native worker only
    private var adaptiveBuffer = AdaptiveBufferPolicy()
    private var nextBufferCheck = 0L
    private var autoBufferStatus = "Inactive"
    private val main = Handler(Looper.getMainLooper())
    private val epoch = AtomicLong()
    private val seekEpoch = AtomicLong()
    private var seekDispatched = 0L // worker only
    private var seekAfterLoad: Long? = null // worker only
    private var preparedItem: MediaItem? = null // worker only
    private var pendingSource: String? = null // worker only
    private var nativeResume = 0L // worker only
    private var revealVideo = false // main only; readiness signal, not first-frame telemetry
    var fastSeeking = false // main only
    private var hasSurface = false // worker only
    @Volatile private var videoDisabled = false
    @Volatile private var closed = false
    @Volatile private var requestedPlay = false
    private var playlist = emptyList<MediaItem>()
    private var position = 0L
    private var duration = C.TIME_UNSET
    private var status = Player.STATE_IDLE
    private var failure: PlaybackException? = null
    private var size = VideoSize.UNKNOWN
    private var nativeTracks = Tracks.EMPTY
    private var parameters = PlaybackParameters.DEFAULT
    @Volatile private var selection = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT
    @Volatile private var presentation = com.opticast.player.player.NativePresentation()
    fun setPresentation(value: com.opticast.player.player.NativePresentation) {
        presentation = value
        job { applyPresentation() }
    }
    private fun applyPresentation() {
        if (handle == 0L) return
        com.opticast.player.player.nativePresentationOptions(presentation).forEach { (key,value) -> NativeMpv.set(handle,key,value) }
    }
    private var level = 1f
    private var output: Any? = null
    private var holder: SurfaceHolder? = null
    private var activeSurface: Surface? = null
    private var handle = 0L // shared-worker only
    private val descriptors = mutableListOf<ParcelFileDescriptor>() // shared-worker only
    private var guard: MpvResumeGuard? = null // shared-worker only
    private var loaded = false // shared-worker only
    private var started = false // application looper only
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusHeld = false
    private var resumeAfterFocus = false
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE).build())
        .setOnAudioFocusChangeListener({ change ->
            if (closed) return@setOnAudioFocusChangeListener
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> { if (resumeAfterFocus) { resumeAfterFocus = false; play() } }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> suspendForFocus()
                AudioManager.AUDIOFOCUS_LOSS -> { resumeAfterFocus = false; pause() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> suspendForFocus()
            }
        }, main).build()
    private val noisy = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { if (!closed) pause() } }
    init { androidx.core.content.ContextCompat.registerReceiver(context, noisy,
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED) }
    private val poll = worker.scheduleWithFixedDelay({ sample() }, 0, 250, TimeUnit.MILLISECONDS)

    override fun getState(): State {
        val entries = playlist.map { item -> MediaItemData.Builder(item.mediaId).setMediaItem(item)
            .setDurationUs(if (duration > 0) duration * 1000 else C.TIME_UNSET)
            .setIsSeekable(true).setTracks(nativeTracks).build() }
        return State.Builder().setAvailableCommands(COMMANDS).setPlaylist(entries)
            .setPlayWhenReady(requestedPlay, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(status).setPlayerError(failure).setIsLoading(status == Player.STATE_BUFFERING)
            .setContentPositionMs(position).setContentBufferedPositionMs { position }
            .setVideoSize(size).setNewlyRenderedFirstFrame(revealVideo).setPlaybackParameters(parameters).setTrackSelectionParameters(selection)
            .setVolume(level).setSeekBackIncrementMs(10_000).setSeekForwardIncrementMs(10_000).build()
    }
    private fun done(): ListenableFuture<*> = Futures.immediateVoidFuture()
    private fun post(generation: Long, block: () -> Unit) { main.post { if (!closed && generation == epoch.get()) { block(); invalidateState(); revealVideo = false } } }
    private fun job(block: () -> Unit) {
        val generation = epoch.get()
        worker.execute {
            if (closed || generation != epoch.get()) return@execute
            try { block() } catch (e: Exception) { fail(generation, e.message ?: "Native playback failed") }
            catch (e: LinkageError) { fail(generation, "Native engine is unavailable on this device") }
        }
    }
    private fun fail(generation: Long, message: String) {
        if (handle != 0L) runCatching { NativeMpv.set(handle, "pause", "yes") }
        post(generation) {
            requestedPlay = false; status = Player.STATE_IDLE
            failure = PlaybackException("mpv: $message", null, PlaybackException.ERROR_CODE_UNSPECIFIED)
            abandonFocus()
        }
    }
    private fun check(result: Int, action: String) { check(result >= 0) { "$action failed ($result)" } }
    private fun destroyNative() {
        adaptiveBuffer = AdaptiveBufferPolicy(); nextBufferCheck = 0L
        autoBufferStatus = "Inactive"; localTrialApplied = false
        val old = handle; handle = 0L; loaded = false; guard = null; preparedItem = null; seekAfterLoad = null; pendingSource = null; hasSurface = false
        try { if (old != 0L) NativeMpv.destroy(old); NativeSessionJournal.closed(context) }
        finally { descriptors.forEach { runCatching { it.close() } }; descriptors.clear() }
    }
    private fun uriSource(uri: android.net.Uri): String = when (uri.scheme) {
        "content" -> {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open file")
            descriptors += descriptor
            try { android.system.Os.lseek(descriptor.fileDescriptor,0,android.system.OsConstants.SEEK_CUR) }
            catch (_: android.system.ErrnoException) { error("This provider does not expose a seekable file. Use Media3 or a local copy") }
            "fd://${descriptor.fd}"
        }
        "file" -> uri.path ?: error("Invalid file URI")
        null -> uri.toString()
        else -> error("Use Media3 for network sources in this first mpv build")
    }
    private fun verifiedLocalTrial(uri: android.net.Uri, enabled: Boolean): Boolean {
        return runCatching {
            fun inspect(fd: java.io.FileDescriptor): Boolean {
                val regular = android.system.OsConstants.S_ISREG(android.system.Os.fstat(fd).st_mode)
                val seekable = runCatching { android.system.Os.lseek(fd, 0, android.system.OsConstants.SEEK_CUR); true }.getOrDefault(false)
                return eligibleLocalBufferTrial(enabled, uri.scheme, uri.authority, regular, seekable)
            }
            if (uri.scheme == "content") descriptors.lastOrNull()?.let { inspect(it.fileDescriptor) } ?: false
            else if (uri.scheme == "file" || uri.scheme == null) {
                val path = if (uri.scheme == "file") uri.path else uri.toString()
                if (path == null || !java.io.File(path).isFile) false
                else java.io.FileInputStream(path).use { inspect(it.fd) }
            } else false
        }.getOrDefault(false)
    }
    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        epoch.incrementAndGet()
        MpvRuntimeInfo.current = null
        playlist = mediaItems.getOrNull(if (startIndex == C.INDEX_UNSET) 0 else startIndex)?.let { listOf(it) }.orEmpty()
        position = if (startPositionMs == C.TIME_UNSET) 0 else startPositionMs.coerceAtLeast(0)
        duration = C.TIME_UNSET; size = VideoSize.UNKNOWN; nativeTracks = Tracks.EMPTY
        failure = null; status = Player.STATE_IDLE; started = false
        job { destroyNative() }
        return done()
    }
    // The app routes collection changes itself; do not advertise unsupported queue editing.
    fun clearForService() {
        handleSetMediaItems(mutableListOf(), C.INDEX_UNSET, 0)
        invalidateState()
    }
    override fun handlePrepare(): ListenableFuture<*> {
        val item = playlist.firstOrNull() ?: return done()
        if (started && failure == null) return done()
        val generation = epoch.incrementAndGet(); val resume = position
        started = true; status = Player.STATE_BUFFERING; failure = null
        val initialSurface = activeSurface
        val speed = parameters.speed; val volume = level
        val trialEnabled = localBufferTrial
        job {
            destroyNative()
            val uri = item.localConfiguration?.uri ?: error("No video URI")
            val source = uriSource(uri)
            localTrialApplied = verifiedLocalTrial(uri, trialEnabled)
            autoBufferStatus = if (localTrialApplied) "Monitoring for sustained buffer starvation" else "Inactive"
            NativeSessionJournal.opened(context)
            handle = NativeMpv.create(context.applicationContext); check(handle != 0L) { "Could not create engine" }
            val fonts = java.io.File(context.filesDir, "mpv-fonts.conf")
            if (!fonts.exists()) fonts.writeText("""<?xml version="1.0"?><!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd"><fontconfig><dir>/system/fonts</dir><dir>/product/fonts</dir><cachedir>${context.cacheDir.absolutePath}/mpv-fonts</cachedir></fontconfig>""")
            android.system.Os.setenv("FONTCONFIG_FILE", fonts.absolutePath, true)
            // 10/10: HDR tone-mapping for HDR->SDR on low-RAM, gapless next episode support
            val isLowRam = com.opticast.player.data.AppContainer.lowRamMode
            val toneMapping = if (isLowRam) "hable" else "bt.2390"
            val options = mapOf("vo" to "gpu", "gpu-api" to "opengl", "gpu-context" to "android",
                "profile" to "fast", "keepaspect" to "no", "hwdec" to "mediacodec,mediacodec-copy,no", "hwdec-codecs" to "all",
                "hr-seek" to "yes", "idle" to "yes", "keep-open" to "yes", "force-window" to "no", "pause" to "yes",
                "terminal" to "no", "input-default-bindings" to "no", "input-vo-keyboard" to "no",
                "tone-mapping" to toneMapping, "hdr-compute-peak" to "yes", "tone-mapping-param" to "0.5",
                "gapless-audio" to "yes", "prefetch-playlist" to "yes",
                "speed" to speed.toString(), "volume" to (volume * 100).toString())
            options.forEach { (key,value) -> check(NativeMpv.option(handle,key,value),key) }
            if (localTrialApplied) localPacketBufferOptions.forEach { (key, value) ->
                check(NativeMpv.option(handle, key, value), key)
            }
            check(NativeMpv.initialize(handle), "Initialize")
            initialSurface?.takeIf { it.isValid }?.let {
                check(NativeMpv.surface(handle,it), "Attach display"); hasSurface = true
            }
            applyPresentation()
            preparedItem = item
            seekDispatched = seekEpoch.get()
            guard = MpvResumeGuard(generation,item.mediaId,resume)
            nativeResume = resume
            pendingSource = source
            loadWhenReady()
        }
        return done()
    }
    private fun loadWhenReady() {
        val source = pendingSource ?: return
        if (handle == 0L || (!hasSurface && !videoDisabled)) return
        val resume = seekAfterLoad ?: nativeResume
        NativeMpv.set(handle,"vid",if(videoDisabled) "no" else "auto")
        check(NativeMpv.command(handle,"loadfile",source,"replace","-1","start=${resume / 1000.0}"),"Open video")
        pendingSource = null
    }
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        requestedPlay = playWhenReady && (focusHeld || audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        if (requestedPlay) focusHeld = true else { resumeAfterFocus = false; abandonFocus() }
        job { if (handle != 0L && loaded) check(NativeMpv.set(handle,"pause",if(requestedPlay) "no" else "yes"),"Play/pause") }
        return done()
    }
    private fun suspendForFocus() {
        resumeAfterFocus = requestedPlay; requestedPlay = false
        job { if (handle != 0L) NativeMpv.set(handle,"pause","yes") }
        invalidateState()
    }
    private fun abandonFocus() { if (focusHeld) audio.abandonAudioFocusRequest(focusRequest); focusHeld = false }
    override fun handleStop(): ListenableFuture<*> {
        requestedPlay = false; abandonFocus(); status = Player.STATE_IDLE; failure = null; started = false
        epoch.incrementAndGet(); job { destroyNative() }; return done()
    }
    override fun handleRelease(): ListenableFuture<*> {
        MpvRuntimeInfo.current = null
        closed = true; epoch.incrementAndGet(); requestedPlay = false; abandonFocus()
        holder?.removeCallback(surfaceCallback); holder = null; output = null; activeSurface = null
        runCatching { context.unregisterReceiver(noisy) }; poll.cancel(false)
        worker.execute { runCatching { destroyNative() } }; return done()
    }
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        position = if (positionMs == C.TIME_UNSET) 0 else positionMs.coerceAtLeast(0)
        val target = position
        val seekMode = if(fastSeeking) "absolute+keyframes" else "absolute+exact"
        val requestSeek = seekEpoch.incrementAndGet()
        job {
            adaptiveBuffer.defer(SystemClock.elapsedRealtime())
            guard?.manualSeek(target)
            if (handle != 0L && loaded) {
                check(NativeMpv.command(handle,"seek",(target/1000.0).toString(),seekMode),"Seek")
                seekDispatched = requestSeek
            } else seekAfterLoad = target
        }
        return done()
    }
    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        parameters = playbackParameters
        job { if (handle != 0L) NativeMpv.set(handle,"speed",playbackParameters.speed.toString()) }; return done()
    }
    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        level = volume
        job { if (handle != 0L) NativeMpv.set(handle,"volume",(volume * 100).toString()) }; return done()
    }
    override fun handleSetTrackSelectionParameters(trackSelectionParameters: TrackSelectionParameters): ListenableFuture<*> {
        val enableText = C.TRACK_TYPE_TEXT in selection.disabledTrackTypes && C.TRACK_TYPE_TEXT !in trackSelectionParameters.disabledTrackTypes
        val resetAudio = selection.overrides.keys.any { it.type == C.TRACK_TYPE_AUDIO } &&
            trackSelectionParameters.overrides.keys.none { it.type == C.TRACK_TYPE_AUDIO }
        val resetText = selection.overrides.keys.any { it.type == C.TRACK_TYPE_TEXT } &&
            trackSelectionParameters.overrides.keys.none { it.type == C.TRACK_TYPE_TEXT }
        selection = trackSelectionParameters
        videoDisabled = C.TRACK_TYPE_VIDEO in trackSelectionParameters.disabledTrackTypes
        val generation = epoch.get()
        job {
            if (handle == 0L) return@job
            NativeMpv.set(handle,"vid",if (videoDisabled || !hasSurface) "no" else "auto")
            loadWhenReady()
            if (resetAudio) NativeMpv.set(handle,"aid","auto")
            if (enableText || resetText) NativeMpv.set(handle,"sid","auto")
            adaptiveBuffer.defer(SystemClock.elapsedRealtime())
            applyTrackSelection(trackSelectionParameters)
            val tracks = readTracks(); post(generation) { nativeTracks = tracks }
        }
        return done()
    }
    private fun applyTrackSelection(params: TrackSelectionParameters) {
        if (C.TRACK_TYPE_TEXT in params.disabledTrackTypes) NativeMpv.set(handle,"sid","no")
        for ((group, override) in params.overrides) {
            val id = group.id.substringAfterLast('-').toIntOrNull() ?: continue
            val property = if(group.type == C.TRACK_TYPE_AUDIO) "aid" else if(group.type == C.TRACK_TYPE_TEXT) "sid" else continue
            if (group.type in params.disabledTrackTypes) continue
            NativeMpv.set(handle,property,if(override.trackIndices.isEmpty()) "no" else id.toString())
        }
    }
    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(h: SurfaceHolder) { setSurface(h.surface) }
        override fun surfaceChanged(h: SurfaceHolder,f: Int,w: Int,hh: Int) { setSurface(h.surface) }
        override fun surfaceDestroyed(h: SurfaceHolder) { setSurface(null) }
    }
    private fun setSurface(surface: Surface?) {
        activeSurface = surface
        job {
            if (handle != 0L) {
                adaptiveBuffer.defer(SystemClock.elapsedRealtime())
                val usable = surface?.takeIf { it.isValid }
                check(NativeMpv.surface(handle,usable),"Display change")
                hasSurface = usable != null
                if (hasSurface && !videoDisabled) NativeMpv.set(handle,"vid","auto")
                loadWhenReady()
            }
        }
    }
    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> {
        holder?.removeCallback(surfaceCallback); output = videoOutput
        holder = when(videoOutput) { is SurfaceView -> videoOutput.holder; is SurfaceHolder -> videoOutput; else -> null }
        holder?.addCallback(surfaceCallback)
        setSurface(when(videoOutput) { is Surface -> videoOutput; else -> holder?.surface?.takeIf { it.isValid } })
        return done()
    }
    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> {
        if (videoOutput == null || videoOutput === output) {
            holder?.removeCallback(surfaceCallback); holder = null; output = null; setSurface(null)
        }
        return done()
    }
    private fun readTracks(): Tracks {
        val groups = mutableListOf<Tracks.Group>()
        val count = NativeMpv.get(handle,"track-list/count")?.toIntOrNull()?.coerceIn(0,256) ?: 0
        for(i in 0 until count) {
            val prefix = "track-list/$i/"
            val type = NativeMpv.get(handle,prefix+"type") ?: continue
            if (type !in listOf("audio", "sub", "video")) continue
            val id = NativeMpv.get(handle,prefix+"id") ?: continue
            val format = Format.Builder().setId("mpv-$type-$id")
                .setSampleMimeType(when(type) { "audio" -> "audio/x-unknown"; "video" -> "video/x-unknown"; else -> MimeTypes.APPLICATION_SUBRIP })
                .setLabel(NativeMpv.get(handle,prefix+"title") ?: if(type == "audio") "Audio $id" else "Subtitle $id")
                .setCodecs(NativeMpv.get(handle,prefix+"codec"))
                .setLanguage(NativeMpv.get(handle,prefix+"lang"))
                .setSelectionFlags((if (NativeMpv.get(handle,prefix+"forced") == "yes") C.SELECTION_FLAG_FORCED else 0) or
                    (if (NativeMpv.get(handle,prefix+"default") == "yes") C.SELECTION_FLAG_DEFAULT else 0))
                .setRoleFlags(if (NativeMpv.get(handle,prefix+"title")?.contains("commentary", true) == true) C.ROLE_FLAG_COMMENTARY else 0)
                .build()
            groups += Tracks.Group(TrackGroup("mpv-$type-$id",format), false, intArrayOf(C.FORMAT_HANDLED),
                booleanArrayOf(NativeMpv.get(handle,prefix+"selected") == "yes"))
        }
        return Tracks(groups)
    }
    private fun sample() {
        if (closed || handle == 0L) return
        val generation = guard?.generation ?: return
        val sampleSeek = seekEpoch.get()
        if (generation != epoch.get()) return
        try {
            val events = NativeMpv.events(handle)
            if (events[0] and 8 != 0) { fail(generation,"Could not decode/open this file (${events[1]}). Try Media3."); return }
            if (events[0] and 1 != 0) {
                loaded = true
                adaptiveBuffer.defer(SystemClock.elapsedRealtime())
                seekAfterLoad?.let { target ->
                    check(NativeMpv.command(handle,"seek",(target/1000.0).toString(),"absolute+exact"),"Initial seek")
                    seekAfterLoad = null; seekDispatched = seekEpoch.get()
                }
                NativeMpv.set(handle,"pause",if(requestedPlay) "no" else "yes")
                val items = preparedItem?.localConfiguration?.subtitleConfigurations.orEmpty()
                for(sub in items) runCatching { NativeMpv.command(handle,"sub-add",uriSource(sub.uri),if((sub.selectionFlags and C.SELECTION_FLAG_DEFAULT) != 0) "select" else "auto",sub.label ?: "External subtitles",sub.language ?: "") }
                applyTrackSelection(selection)
                applyPresentation()
                val tracks = readTracks(); post(generation) { nativeTracks = tracks }
            }
            if (!loaded) return
            val seconds = NativeMpv.get(handle,"time-pos")?.toDoubleOrNull()
            val sampled = seconds?.takeIf { it.isFinite() && it >= 0 && it < Long.MAX_VALUE/1000.0 }?.times(1000)?.toLong()
            val accepted = sampled != null && sampleSeek == seekDispatched && guard?.accept(generation,sampled) == true
            val length = NativeMpv.get(handle,"duration")?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 && it < Long.MAX_VALUE/1_000_000.0 }?.times(1000)?.toLong()
            val width = NativeMpv.get(handle,"video-out-params/dw")?.toIntOrNull() ?: 0
            val height = NativeMpv.get(handle,"video-out-params/dh")?.toIntOrNull() ?: 0
            val seeking = NativeMpv.get(handle,"seeking") == "yes"
            val buffering = NativeMpv.get(handle,"paused-for-cache") == "yes" || seeking
            val ended = events[0] and 4 != 0 || NativeMpv.get(handle,"eof-reached") == "yes"
            if (!ended && NativeMpv.get(handle,"idle-active") == "yes") {
                fail(generation, "mpv stopped playback without reporting a decoder error. Try Media3 or an external player.")
                return
            }
            if (ended && com.opticast.player.player.endedUnexpectedly(sampled ?: position, length ?: duration)) {
                fail(generation, "Playback ended before the expected end (${(sampled ?: position) / 1000}s of ${(length ?: duration) / 1000}s). mpv did not report a decoder error. The file may be truncated or decoding may have stopped.")
                return
            }
            runCatching { checkAdaptiveBuffer(generation, sampleSeek, seeking || sampleSeek != seekDispatched, ended) }
            val info = MpvInfo(preparedItem?.mediaId.orEmpty(), NativeMpv.get(handle,"hwdec-current"),
                NativeMpv.get(handle,"video-codec"), NativeMpv.get(handle,"audio-codec"), NativeMpv.get(handle,"frame-drop-count"))
            post(generation) {
                MpvRuntimeInfo.current = info
                // mpv readiness releases PlayerView's shutter. Do not use this as measured first-frame timing.
                revealVideo = events[0] and 2 != 0 && width > 0 && height > 0
                if (accepted && sampleSeek == seekEpoch.get()) sampled?.let { position = it }
                if (length != null) duration = length
                if (width > 0 && height > 0) size = VideoSize(width,height)
                if (failure == null) status = if(ended) Player.STATE_ENDED else if(buffering) Player.STATE_BUFFERING else Player.STATE_READY
            }
        } catch (e: Exception) { fail(generation,"Native state update failed") }
    }
    private fun checkAdaptiveBuffer(generation: Long, sampleSeek: Long, seeking: Boolean, ended: Boolean) {
        if (!localTrialApplied || adaptiveBuffer.attempted) return
        val now = SystemClock.elapsedRealtime()
        if (!requestedPlay || seeking || ended || (!hasSurface && !videoDisabled)) {
            adaptiveBuffer.defer(now)
            return
        }
        if (now < nextBufferCheck) return
        nextBufferCheck = now + 1000L
        // One bounded map per second on the existing native worker; no process-memory polling.
        val metrics = parsePacketCacheMetrics(NativeMpv.get(handle, "demuxer-cache-state"))
        if (generation != epoch.get() || sampleSeek != seekEpoch.get() || !requestedPlay) {
            adaptiveBuffer.defer(now)
            return
        }
        if (!adaptiveBuffer.observe(now, metrics?.underrun, metrics?.eof, metrics?.idle)) return
        when (expandPacketBuffer({ key, value -> NativeMpv.set(handle, key, value) }, { key -> NativeMpv.get(handle, key) })) {
            BufferExpansion.EXPANDED -> {
                localTrialApplied = false
                autoBufferStatus = "Larger buffer enabled after sustained buffer starvation (this session only)"
            }
            BufferExpansion.SMALL_RESTORED -> autoBufferStatus = "Automatic expansion failed; smaller limits restored"
            BufferExpansion.INCOMPLETE -> {
                localTrialApplied = false
                autoBufferStatus = "Automatic adjustment incomplete; check effective limits"
            }
        }
    }
    /** One worker-thread query only on explicit diagnostic capture, fenced by media identity. */
    internal suspend fun captureCacheSnapshot(): MpvCacheSnapshot? = kotlinx.coroutines.withTimeoutOrNull(1500L) {
        kotlinx.coroutines.suspendCancellableCoroutine<MpvCacheSnapshot?> { continuation ->
            val generation = epoch.get()
            worker.execute {
                val result = runCatching {
                    if (closed || !loaded || handle == 0L || epoch.get() != generation) null else {
                        // Query the whole map: this pinned mpv property does not implement slash-key reads.
                        val metrics = parsePacketCacheMetrics(NativeMpv.get(handle, "demuxer-cache-state"))
                        MpvCacheSnapshot(preparedItem?.mediaId.orEmpty(), metrics?.totalBytes, metrics?.forwardBytes,
                            metrics?.estimatedSeconds ?: com.opticast.player.player.diagnosticNonNegativeDouble(NativeMpv.get(handle, "demuxer-cache-duration")),
                            com.opticast.player.player.diagnosticNonNegativeLong(NativeMpv.get(handle, "options/demuxer-max-bytes")),
                            com.opticast.player.player.diagnosticNonNegativeLong(NativeMpv.get(handle, "options/demuxer-max-back-bytes")),
                            NativeMpv.get(handle, "options/demuxer-donate-buffer")?.takeIf { it == "yes" || it == "no" },
                            localTrialApplied, autoBufferStatus)
                    }
                }.getOrNull()
                if (continuation.isActive) continuation.resumeWith(Result.success(result))
            }
        }
    }
    companion object {
        // Serializes teardown before the next native instance, even across service replacement.
        private val worker = Executors.newSingleThreadScheduledExecutor { task -> Thread(task,"OptiCast-mpv").apply { isDaemon = true } }
        private val COMMANDS = Player.Commands.Builder().addAll(
            Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP, Player.COMMAND_RELEASE,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD, Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_TIMELINE, Player.COMMAND_GET_METADATA, Player.COMMAND_GET_TRACKS,
            Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS, Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_SET_SPEED_AND_PITCH, Player.COMMAND_SET_VOLUME, Player.COMMAND_GET_VOLUME,
            Player.COMMAND_SET_VIDEO_SURFACE, Player.COMMAND_GET_TEXT).build()
    }
}
