package com.opticast.player.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import com.opticast.player.player.PipActions
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import java.lang.ref.WeakReference
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.ui.theme.OptiCastTheme
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hosts the built-in player in its own activity.
 *
 * Keeping the player out of the library activity is what makes the PiP flow
 * work without tying its lifecycle to the library. PiP entry is explicit;
 * backgrounding pauses the existing session rather than destroying it.
 * Fixed for 32-bit: expanding PiP automatically resumes playback.
 */
class PlayerActivity : ComponentActivity() {

    var subtitlePickerActive: Boolean = false
    private var usedPip = false
    internal fun ownsPlaybackSession(): Boolean = activePlayer.get() === this

    /** Keep activity intent routing and the in-player movie picker on one identity. */
    fun selectLocalMovie(id: Long) {
        if (id <= 0L) return
        acceptPlaybackIntent(intent(this, id))
    }

    fun enterPipFromControls(): Boolean {
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false
        // Save playing state before entering PiP so we can auto-resume on expand
        PiPController.wasPlayingBeforePip = PiPController.isPlaying()
        PiPController.onEnterPip?.invoke()
        val entered = runCatching { enterPictureInPictureMode(pipParams()) }.getOrDefault(false)
        if (entered) usedPip = true
        return entered
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("subtitlePickerActive", subtitlePickerActive)
        outState.putBoolean("usedPip", usedPip)
        outState.putLong("playingVideoId", currentVideoId.longValue)
        outState.putString("playingRemoteUri", remoteUri.value)
        outState.putString("playingRemoteTitle", remoteTitle.value)
        super.onSaveInstanceState(outState)
    }

    private val currentVideoId = mutableLongStateOf(0L)
    private val remoteUri = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val remoteTitle = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    /** Guards only the final position save - never the stop itself. */
    private val closingHookFired = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activePlayer = WeakReference(this)
        PiPController.inPictureInPicture = isInPictureInPictureMode
        usedPip = savedInstanceState?.getBoolean("usedPip") == true
        onBackPressedDispatcher.addCallback(this) { closeFromBack() }
        subtitlePickerActive = savedInstanceState?.getBoolean("subtitlePickerActive") ?: false
        enableEdgeToEdge()
        window.attributes = window.attributes.apply {
            rotationAnimation = android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE
        }
        val restoredSelection = savedInstanceState?.containsKey("playingVideoId") == true
        currentVideoId.longValue = if (restoredSelection) savedInstanceState?.getLong("playingVideoId") ?: 0L else intent.getLongExtra(EXTRA_VIDEO_ID, 0L)
        remoteUri.value = if (restoredSelection) savedInstanceState?.getString("playingRemoteUri") else intent.getStringExtra(EXTRA_REMOTE_URI)
        remoteTitle.value = if (restoredSelection) savedInstanceState?.getString("playingRemoteTitle") else intent.getStringExtra(EXTRA_REMOTE_TITLE)
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            PipActions.filter(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        PiPController.onPlayingChanged = { playing -> refreshPip(); playing }
        PiPController.onPipAspectChanged = { refreshPip() }

        setContent {
            val settings by AppContainer.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppContainer.initialSettings)
            OptiCastTheme(
                theme = settings.appTheme,
                useDeviceColors = settings.useDeviceColors,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    PlayerScreen(
                        videoId = currentVideoId.longValue,
                        remoteUri = remoteUri.value?.takeIf { remoteUri.value != null && currentVideoId.longValue <= 0L },
                        remoteTitle = remoteTitle.value,
                        onBack = { closeFromBack() },
                        onSystemBack = { closeFromBack() },
                        onClosePlayer = { finish() },
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(0, com.opticast.player.R.anim.player_fade_out)
    }

    /** Tapping another video while the floating window is open switches it. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        acceptPlaybackIntent(intent)
    }

    /** Main-thread route replacement, without startActivity bringing PiP to fullscreen. */
    private fun acceptPlaybackIntent(incoming: Intent) {
        val id = incoming.getLongExtra(EXTRA_VIDEO_ID, 0L)
        val uri = incoming.getStringExtra(EXTRA_REMOTE_URI)
        if (!validPlaybackRequest(id, uri) || !ownsPlaybackSession()) return
        if (!samePlaybackRequest(currentVideoId.longValue, remoteUri.value, id, uri)) {
            // Persist the service's own outgoing identity BEFORE changing the Compose route.
            OptiCastPlaybackService.prepareForMediaSwitch()
        }
        setIntent(incoming)
        closingHookFired.set(false)
        currentVideoId.longValue = if (id > 0L) id else 0L
        remoteUri.value = if (id > 0L) null else uri
        remoteTitle.value = if (id > 0L) null else incoming.getStringExtra(EXTRA_REMOTE_TITLE)
    }

    private fun closeFromBack() {
        if (isInPictureInPictureMode || !ownsPlaybackSession()) return
        val route = expandedPipBackRoute(usedPip, currentVideoId.longValue)
        requestStop()
        if (route != null) {
            val destination = Intent(this, com.opticast.player.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (currentVideoId.longValue > 0L) destination.putExtra(com.opticast.player.MainActivity.EXTRA_SHOW_VIDEO_DETAILS, currentVideoId.longValue)
            else destination.putExtra(com.opticast.player.MainActivity.EXTRA_SHOW_LIBRARY, true)
            startActivity(destination)
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Fix for 32-bit: when returning from PiP via expand, auto-resume if it was playing before
        if (usedPip && !isInPictureInPictureMode && PiPController.wasPlayingBeforePip) {
            handler.postDelayed({
                if (ownsPlaybackSession() && !isInPictureInPictureMode) {
                    PiPController.onReturnFromPip?.invoke()
                }
            }, 150L)
        }
    }

    // PiP is entered only from the explicit player control, never Back/Home.
    /**
     * The screen is going away from the user. Playback is only allowed to
     * continue when the video floats in PiP (or the screen simply turned off),
     * so closing the PiP window - or leaving the player any other way - stops
     * it instead of leaking audio in the background.
     */
    override fun onStop() {
        super.onStop()
        if (!ownsPlaybackSession()) return
        // No longer visible and not floating in a PiP window -> nothing should
        // still be audible.
        when (playerBackgroundAction(isFinishing, isChangingConfigurations, isInPictureInPictureMode, subtitlePickerActive)) {
            BackgroundAction.STOP -> requestStop()
            BackgroundAction.PAUSE -> PiPController.onBackground?.invoke()
            BackgroundAction.KEEP -> Unit
        }
    }

    /**
     * Leaving the floating window: either the user tapped back into the app or
     * the window was dismissed. Deferred, because at mode-change time Android
     * has not settled the activity state yet.
     * Fixed for 32-bit: longer delay and auto-resume on expand.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!ownsPlaybackSession()) return
        PiPController.inPictureInPicture = isInPictureInPictureMode
        if (isInPictureInPictureMode) { usedPip = true; refreshPip() }
        if (!isInPictureInPictureMode) {
            // Fix: Slow to cut sound when PiP closed, should be instant
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                if (isFinishing || isDestroyed) requestStop() else PiPController.onBackground?.invoke()
            } else {
                handler.postDelayed({
                    if (ownsPlaybackSession() && !this@PlayerActivity.isInPictureInPictureMode) {
                        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                            if (PiPController.wasPlayingBeforePip) {
                                PiPController.onReturnFromPip?.invoke()
                            }
                        }
                    }
                }, 150L)
            }
        }
    }

    /**
     * Buttons inside the floating window. Android supplies close and expand;
     * pause, play and skip have to come from here or the PiP window has no
     * controls at all.
     */
    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!ownsPlaybackSession()) return
            when (intent?.action) {
                PipActions.ACTION_PLAY_PAUSE -> PiPController.onTogglePlay?.invoke()
                PipActions.ACTION_REWIND -> PiPController.onSeekBy?.invoke(-10_000L)
                PipActions.ACTION_FORWARD -> PiPController.onSeekBy?.invoke(10_000L)
            }
            refreshPip()
        }
    }

    private fun refreshPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // While not in PiP we still want the next enterPip to use the right aspect,
        // so update the params even when not yet floating — Android ignores it until needed.
        runCatching {
            setPictureInPictureParams(
                PipActions.params(this, PiPController.isPlaying(), PiPController.videoWidth, PiPController.videoHeight)
            )
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        // Unconditional: the activity is only destroyed when the player is
        // really going away (floating in PiP keeps it alive, and rotation does
        // not recreate it). Closing the PiP window destroys it WITHOUT setting
        // isFinishing, and sometimes while isInPictureInPictureMode still reads
        // true - both of which used to leave the audio playing.
        handler.removeCallbacksAndMessages(null)
        if (!isChangingConfigurations) requestStop()
        super.onDestroy()
        if (ownsPlaybackSession()) {
            PiPController.reset()
            activePlayer.clear()
        }
    }

    /**
     * Stops playback for good.
     *
     * The stop is deliberately NOT one-shot: an earlier version latched a flag
     * after the first call, so any later close (including tapping X on the PiP
     * window) was ignored and the audio kept running. Only the position save is
     * guarded, so it happens once.
     */
    private fun requestStop() {
        if (!ownsPlaybackSession()) return
        if (closingHookFired.compareAndSet(false, true)) {
            PiPController.onPlayerClosing?.invoke()
        }
        OptiCastPlaybackService.stopPlayback(this)
    }

    private fun pipParams(): PictureInPictureParams =
        PipActions.params(this, PiPController.isPlaying(), PiPController.videoWidth, PiPController.videoHeight)

    companion object {
        private var activePlayer = WeakReference<PlayerActivity>(null)

        /** All in-app launches use this gate; never create a second player for an active PiP. */
        fun launch(context: Context, incoming: Intent) {
            if (!validPlaybackRequest(incoming.getLongExtra(EXTRA_VIDEO_ID, 0L), incoming.getStringExtra(EXTRA_REMOTE_URI))) return
            val owner = activePlayer.get()
            if (reusePipPlayer(owner != null, owner?.isInPictureInPictureMode == true,
                    owner?.isFinishing == true, owner?.isDestroyed == true)) {
                owner?.acceptPlaybackIntent(incoming)
            } else context.startActivity(incoming)
        }

        const val EXTRA_REMOTE_URI = "opticast.remote.uri"
        const val EXTRA_REMOTE_TITLE = "opticast.remote.title"
        const val EXTRA_VIDEO_ID = "videoId"

        fun intent(context: Context, videoId: Long): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_ID, videoId)
    }
}
