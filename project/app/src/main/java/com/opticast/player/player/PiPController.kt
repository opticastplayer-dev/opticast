package com.opticast.player.player

/**
 * Tiny bridge between the Compose player and the Activity so the Activity can
 * enter Picture-in-Picture only from an explicit control.
 * Fixed for 32-bit devices: tracks whether playback was active before PiP
 * so expanding PiP automatically resumes instead of requiring manual tap.
 */
object PiPController {
    @Volatile var inPictureInPicture: Boolean = false
    @Volatile var isPlayerActive: Boolean = false
    @Volatile var isPlaying: () -> Boolean = { false }
    @Volatile var wasPlayingBeforePip: Boolean = false

    @Volatile var onEnterPip: (() -> Unit)? = null
    @Volatile var onBackground: (() -> Unit)? = null
    @Volatile var onReturnFromPip: (() -> Unit)? = null

    /**
     * Invoked when the player screen is really going away (PiP window closed,
     * player closed). Saves the final position and stops the playback service.
     */
    @Volatile var onPlayerClosing: (() -> Unit)? = null

    /** Transport controls for the floating window's own buttons. */
    @Volatile var onTogglePlay: (() -> Unit)? = null
    @Volatile var onSeekBy: ((Long) -> Unit)? = null

    /** Lets the activity refresh the window's pause/play icon. */
    @Volatile var onPlayingChanged: ((Boolean) -> Unit)? = null

    /** Current video dimensions — used to size the PiP window to the content. */
    @Volatile var videoWidth: Int = 0
    @Volatile var videoHeight: Int = 0

    /** Called when the video size (and thus PiP aspect) changes. */
    @Volatile var onPipAspectChanged: (() -> Unit)? = null

    /** Only the owning player activity may clear the global UI bridge. */
    internal fun reset() {
        inPictureInPicture = false
        isPlayerActive = false
        isPlaying = { false }
        wasPlayingBeforePip = false
        onEnterPip = null; onBackground = null; onPlayerClosing = null
        onReturnFromPip = null
        onTogglePlay = null; onSeekBy = null; onPlayingChanged = null
        onPipAspectChanged = null; videoWidth = 0; videoHeight = 0
    }
}
