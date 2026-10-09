package com.opticast.player.player

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.opticast.player.MainActivity
import com.opticast.player.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Owns the ExoPlayer + MediaSession so playback gets:
 *  - a media notification with play/pause/seek controls,
 *  - lock-screen controls,
 *  - Bluetooth / media-button handling,
 *  - audio focus management,
 * even while the app is in the background or Picture-in-Picture.
 *
 * Playback is only ever audible while a player screen exists (fullscreen or its
 * floating PiP window). Several independent mechanisms enforce that, because a
 * single one is not enough on modern Android - see [stopPlayback].
 */
class OptiCastPlaybackService : MediaSessionService() {

    private var player: Player? = null
    private var session: MediaSession? = null
    private val audioEffects = AudioEffects()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instanceRef = this
        com.opticast.player.data.PlaybackWorkBudget.setSessionActive(true)

        val exoPlayer = createMedia3Player()
        // Tapping the notification brings the app back to the front.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        session = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    if (controller.packageName == packageName && controller.connectionHints.containsKey(ENGINE_HINT)) {
                        com.opticast.player.data.PlaybackWorkBudget.setSessionActive(true)
                        selectEngine(controller.connectionHints.getString(ENGINE_HINT) == "mpv")
                    }
                    return super.onConnect(session, controller)
                }
            })
            .build()
        player = exoPlayer
        serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2_000L)
                if (player?.isPlaying == true) persistPosition()
            }
        }

        // Audio boost - simplified, no presets, only dialogue + volume boost
        serviceScope.launch {
            AppContainer.settings.settings.collect { prefs ->
                audioEffects.apply(prefs)
            }
        }
    }

    private fun createMedia3Player(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // The player reads local files exactly as before, but understands
        // smb:// links and gains credentials for saved WebDAV/HTTP libraries.
        val dataSourceFactory = RemoteDataSource.Factory(this, AppContainer.networkSources)
        val exoPlayer = ExoPlayer.Builder(this, CompatibleRenderersFactory(this).setEnableDecoderFallback(true))
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setLoadControl(StartupLoadControl())
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Analytics feed the playback-info overlay (decoder names, dropped
        // frames, bandwidth). Purely observational - it never influences
        // playback or the stop paths.
        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            private fun eventMediaId(eventTime: AnalyticsListener.EventTime): String? {
                if(eventTime.timeline.isEmpty || eventTime.windowIndex !in 0 until eventTime.timeline.windowCount) return null
                return eventTime.timeline.getWindow(eventTime.windowIndex,androidx.media3.common.Timeline.Window()).mediaItem.mediaId
                    .takeIf { it == exoPlayer.currentMediaItem?.mediaId }
            }
            private fun probeForEvent(eventTime: AnalyticsListener.EventTime): ResumeProbe? {
                val probe=ResumeProbes.forId(exoPlayer.currentMediaItem?.mediaId) ?: return null
                if(eventTime.realtimeMs < (probe.submittedMs ?: return null)) return null
                if(!eventTime.timeline.isEmpty && eventTime.windowIndex in 0 until eventTime.timeline.windowCount && eventMediaId(eventTime)!=probe.mediaId) return null
                return probe
            }
            override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, tracks: androidx.media3.common.Tracks) {
                if(!tracks.isEmpty) probeForEvent(eventTime)?.event(ProbeStage.TRACKS,eventTime.realtimeMs,inferred=eventMediaId(eventTime)==null)
            }
            override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: androidx.media3.common.Format, decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?) {
                probeForEvent(eventTime)?.event(ProbeStage.VIDEO_INPUT,eventTime.realtimeMs,inferred=eventMediaId(eventTime)==null)
            }
            override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: androidx.media3.common.Format, decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?) {
                probeForEvent(eventTime)?.event(ProbeStage.AUDIO_INPUT,eventTime.realtimeMs,inferred=eventMediaId(eventTime)==null)
            }
            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                if(!eventTime.timeline.isEmpty && eventTime.windowIndex in 0 until eventTime.timeline.windowCount) {
                    val id=eventTime.timeline.getWindow(eventTime.windowIndex,androidx.media3.common.Timeline.Window()).mediaItem.mediaId
                    if(id==exoPlayer.currentMediaItem?.mediaId) {
                        PlayerStats.markStartup(id,StartupStage.FRAME,renderTimeMs)
                        ResumeProbes.forId(id)?.event(ProbeStage.FRAME,renderTimeMs)
                    }
                }
            }

            override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
                PlayerStats.onBuffering(eventMediaId(eventTime),state==Player.STATE_BUFFERING,eventTime.realtimeMs)
            }

            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                PlayerStats.onVideoDecoder(decoderName,eventMediaId(eventTime))
                probeForEvent(eventTime)?.decoder(true,decoderName,initializedTimestampMs,initializationDurationMs,eventMediaId(eventTime)==null)
            }

            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                PlayerStats.onAudioDecoder(decoderName,eventMediaId(eventTime))
                probeForEvent(eventTime)?.decoder(false,decoderName,initializedTimestampMs,initializationDurationMs,eventMediaId(eventTime)==null)
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long,
            ) {
                PlayerStats.onDroppedFrames(droppedFrames,eventMediaId(eventTime))
            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long,
            ) {
                PlayerStats.onBandwidth(bitrateEstimate)
            }
        })

        // Effects live on the audio session, which the device can rebuild at any
        // time (output switch, Bluetooth connect). Re-attach whenever it changes.
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if(isPlaying) {
                    PlayerStats.markStartup(exoPlayer.currentMediaItem?.mediaId,StartupStage.PLAYING)
                    ResumeProbes.forId(exoPlayer.currentMediaItem?.mediaId)?.event(ProbeStage.PLAYING,android.os.SystemClock.elapsedRealtime())
                }
                persistPosition()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if(playbackState==Player.STATE_READY) {
                    PlayerStats.markStartup(exoPlayer.currentMediaItem?.mediaId,StartupStage.READY)
                    ResumeProbes.forId(exoPlayer.currentMediaItem?.mediaId)?.event(ProbeStage.READY,android.os.SystemClock.elapsedRealtime())
                }
                if (playbackState == Player.STATE_ENDED) persistPosition()
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                ResumeProbes.forId(exoPlayer.currentMediaItem?.mediaId)?.event(ProbeStage.ERROR,android.os.SystemClock.elapsedRealtime(),error.errorCode.toLong())
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo, reason: Int) {
                // PositionInfo binds the outgoing position to its OWN item even after the
                // engine has changed currentMediaItem. Never label it using the next route.
                val oldId = oldPosition.mediaItem?.mediaId?.toLongOrNull()
                val newId = newPosition.mediaItem?.mediaId?.toLongOrNull()
                if (shouldSaveOutgoing(oldId, newId, oldPosition.positionMs)) {
                    val id = oldId ?: return
                    val duration = knownDurations[id]
                        ?: AppContainer.playbackState.state(id)?.durationMs?.takeIf { it > 0L }
                        ?: 0L
                    AppContainer.playbackState.save(id, oldPosition.positionMs, duration)
                }
                if(reason==Player.DISCONTINUITY_REASON_SEEK || reason==Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    ResumeProbes.forId(newPosition.mediaItem?.mediaId)?.event(if(reason==Player.DISCONTINUITY_REASON_SEEK) ProbeStage.SEEK else ProbeStage.SEEK_ADJUSTED,android.os.SystemClock.elapsedRealtime(),newPosition.positionMs)
                }
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    persistPosition(allowReset = newPosition.positionMs == 0L)
                }
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects.attach(audioSessionId)
            }
        })
        audioEffects.attach(exoPlayer.audioSessionId)

        return exoPlayer
    }

    private fun selectEngine(native: Boolean) {
        if ((player is com.opticast.player.player.mpv.MpvPlayer) == native) return
        persistPosition()
        val outgoing = player
        outgoing?.pause(); outgoing?.stop()
        audioEffects.release()
        val replacement: Player = if (native) com.opticast.player.player.mpv.MpvPlayer(this).also { mpv ->
            mpv.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { persistPosition() }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) persistPosition()
                }
                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    val oldId = oldPosition.mediaItem?.mediaId?.toLongOrNull()
                    val newId = newPosition.mediaItem?.mediaId?.toLongOrNull()
                    if (shouldSaveOutgoing(oldId, newId, oldPosition.positionMs)) {
                        val id = oldId ?: return
                        AppContainer.playbackState.save(id, oldPosition.positionMs,
                            knownDurations[id] ?: AppContainer.playbackState.state(id)?.durationMs ?: 0L)
                    }
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) persistPosition(allowReset = newPosition.positionMs == 0L)
                }
            })
        } else createMedia3Player()
        player = replacement
        PlayerStats.reset()
        // Detach the session from the old player BEFORE releasing it.
        session?.setPlayer(replacement)
        outgoing?.release()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * Explicit stop request from the UI (e.g. the PiP window was closed).
     * Binder-independent on purpose: a MediaController.pause() sent right
     * before the controller is released can be dropped, which used to leave
     * the audio running after the floating window was dismissed.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_PLAYBACK) {
            stopPlaybackInternal()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // Updated only from the engine's application thread; scoped per media identity.
    private val knownDurations = linkedMapOf<Long, Long>()

    private fun persistPosition(allowReset: Boolean = false) {
        val p = player ?: return
        val id = p.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        if (id == 0L) return
        val duration = p.duration.takeIf { it > 0L }
            ?: knownDurations[id]
            ?: AppContainer.playbackState.state(id)?.durationMs ?: 0L
        if (duration > 0L) {
            knownDurations[id] = duration
            if (knownDurations.size > 64) knownDurations.remove(knownDurations.keys.first())
        }
        val position = if (p.playbackState == Player.STATE_ENDED && duration > 0L) duration
            else p.currentPosition.coerceAtLeast(0L)
        if (position > 0L || allowReset) {
            AppContainer.playbackState.save(id, position, duration, allowReset = allowReset)
        }
    }

    private fun stopPlaybackInternal() {
        persistPosition()
        val p = player
        PlayerStats.reset()
        runCatching {
            p?.pause()
            p?.stop()
            // Emptying the session is what actually removes the media
            // notification: media3 keeps a paused notification in the shade for
            // as long as the session still holds media items. Without this the
            // audio stopped but the notification stayed behind (reported on
            // 1.9.2 when the PiP window's X was tapped).
            if (p is com.opticast.player.player.mpv.MpvPlayer) p.clearForService() else p?.clearMediaItems()
        }
        // Belt and braces, in case the notification was posted by an older
        // service instance that is no longer bound to this player.
        runCatching { stopForeground(Service.STOP_FOREGROUND_REMOVE) }
        runCatching { NotificationManagerCompat.from(this).cancelAll() }
        runCatching { stopSelf() }
        com.opticast.player.external.ExternalVideoGrants.release(this)
        knownDurations.clear()
        com.opticast.player.data.PlaybackWorkBudget.setSessionActive(false)
    }

    /**
     * OptiCast only keeps audio alive while the player screen (or its floating
     * PiP window) is on screen, so removing the task always stops playback.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopPlaybackInternal()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        persistPosition()
        if (instanceRef === this) com.opticast.player.external.ExternalVideoGrants.release(this)
        instanceRef = null
        com.opticast.player.data.PlaybackWorkBudget.setSessionActive(false)
        PiPController.inPictureInPicture = false
        serviceScope.cancel()
        audioEffects.release()
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        /** Invoked from the UI on main. Returns no sample if the owning session/item changed. */
        internal suspend fun captureMemorySnapshot(): MemorySnapshot? {
            val owner = instanceRef
            val active = owner?.player
            val mediaId = active?.currentMediaItem?.mediaId
            val engine = when {
                mediaId == null -> "No active session"
                active is com.opticast.player.player.mpv.MpvPlayer -> "mpv"
                else -> "Media3"
            }
            val cache = (active as? com.opticast.player.player.mpv.MpvPlayer)?.captureCacheSnapshot()
            val memory = kotlinx.coroutines.withContext(Dispatchers.IO) { readProcessMemorySnapshot() }
            if (instanceRef !== owner || owner?.player !== active || active?.currentMediaItem?.mediaId != mediaId) return null
            val native = com.opticast.player.player.mpv.MpvRuntimeInfo.current?.takeIf { it.mediaId == mediaId }
            val size = active?.videoSize
            val matchingCache = cache?.takeIf { it.mediaId == mediaId }
            return memory.copy(engine = engine,
                state = diagnosticPlaybackState(mediaId != null, active?.playbackState ?: Player.STATE_IDLE,
                    active?.isPlaying == true, active?.playWhenReady == true, active?.playbackSuppressionReason ?: 0),
                hardware = if (engine == "mpv") native?.hardwareMode ?: "Not reported" else "Not reported",
                width = size?.width ?: 0, height = size?.height ?: 0,
                pip = PiPController.inPictureInPicture,
                demuxerBytes = matchingCache?.totalBytes, forwardBytes = matchingCache?.forwardBytes,
                cacheSeconds = matchingCache?.seconds, forwardLimit = matchingCache?.forwardLimit,
                backLimit = matchingCache?.backLimit, donation = matchingCache?.donation, trialApplied = matchingCache?.trialApplied, autoBufferStatus = matchingCache?.autoBufferStatus)
        }

        const val ENGINE_HINT = "opticast.playback.engine"
        const val ACTION_STOP_PLAYBACK = "com.opticast.player.action.STOP_PLAYBACK"

        /** Live instance so the UI can silence playback without any IPC. */
        @Volatile
        private var instanceRef: OptiCastPlaybackService? = null

        /**
         * Tears playback down. Three independent mechanisms, because relying on
         * startService() alone is exactly what left audio playing after the PiP
         * window was closed: Android refuses background service starts, and
         * closing PiP puts the app in the background.
         */
        /** Main-thread UI scrubbing: render nearby keyframes, then restore exact seeking. */
        fun setNativePresentation(presentation: NativePresentation) {
            (instanceRef?.player as? com.opticast.player.player.mpv.MpvPlayer)?.setPresentation(presentation)
        }

        fun setFastSeeking(enabled: Boolean) {
            (instanceRef?.player as? com.opticast.player.player.mpv.MpvPlayer)?.fastSeeking = enabled
            (instanceRef?.player as? ExoPlayer)?.setSeekParameters(if (enabled) androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC
                else androidx.media3.exoplayer.SeekParameters.EXACT)
        }

        /** Keep the service/window alive while saving and pausing its outgoing media identity. */
        fun prepareForMediaSwitch() {
            instanceRef?.let { service ->
                service.persistPosition()
                service.player?.pause()
            }
        }

        fun stopPlayback(context: Context) {
            // 1. Direct, same-process call - immediate, and immune to the
            //    background-service-start restrictions that defeated the old
            //    startService()-only version.
            runCatching { instanceRef?.stopPlaybackInternal() }
            // Never start a fresh service just to stop it: a queued STOP intent can
            // race with the next video's connection and tear that new session down.
            runCatching {
                context.stopService(Intent(context, OptiCastPlaybackService::class.java))
            }
        }
    }
}
