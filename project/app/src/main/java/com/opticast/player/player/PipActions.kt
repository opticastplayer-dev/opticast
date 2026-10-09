package com.opticast.player.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational

/**
 * The buttons that belong in the floating picture-in-picture window.
 *
 * Android draws its own close and expand buttons, but pause, play and skip have
 * to come from the app. Without these the floating window is a viewer with no
 * controls at all - which is exactly what it was.
 */
object PipActions {

    const val ACTION_PLAY_PAUSE = "com.opticast.player.pip.PLAY_PAUSE"
    const val ACTION_REWIND = "com.opticast.player.pip.REWIND"
    const val ACTION_FORWARD = "com.opticast.player.pip.FORWARD"

    fun actions(context: Context, isPlaying: Boolean): List<RemoteAction> = listOf(
        action(
            context,
            ACTION_REWIND,
            android.R.drawable.ic_media_rew,
            context.getString(android.R.string.cancel).let { "Back 10 seconds" },
            REQUEST_REWIND,
        ),
        action(
            context,
            ACTION_PLAY_PAUSE,
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            REQUEST_PLAY_PAUSE,
        ),
        action(
            context,
            ACTION_FORWARD,
            android.R.drawable.ic_media_ff,
            "Forward 10 seconds",
            REQUEST_FORWARD,
        ),
    )

    fun params(
        context: Context,
        isPlaying: Boolean,
        width: Int = PiPController.videoWidth,
        height: Int = PiPController.videoHeight,
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setActions(actions(context, isPlaying))
        builder.setAspectRatio(aspectFor(width, height))
        // Android 12+ - keep the window bounded and smooth; aspect is enough.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(false)
        }
        return builder.build()
    }

    private fun aspectFor(w: Int, h: Int): Rational {
        if (w > 0 && h > 0) {
            val aspect = w.toFloat() / h.toFloat()
            // Android enforces 0.418069…2.390… - clamp rather than crash.
            return when {
                aspect < 0.42f -> Rational(9, 21) // ~0.428
                aspect > 2.39f -> Rational(239, 100)
                else -> Rational(w, h)
            }
        }
        return Rational(16, 9)
    }

    /** All three actions, so a receiver can be registered in one call. */
    fun filter(): android.content.IntentFilter = android.content.IntentFilter().apply {
        addAction(ACTION_PLAY_PAUSE)
        addAction(ACTION_REWIND)
        addAction(ACTION_FORWARD)
    }

    private const val REQUEST_REWIND = 101
    private const val REQUEST_PLAY_PAUSE = 102
    private const val REQUEST_FORWARD = 103

    private fun action(
        context: Context,
        action: String,
        iconRes: Int,
        label: String,
        requestCode: Int,
    ): RemoteAction {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(context, iconRes), label, label, pending)
    }

    /** Refresh the window's buttons when the playing state changes. */
    fun refresh(context: Context, isPlaying: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { context.setPictureInPictureParamsCompat(params(context, isPlaying)) }
    }

    private fun Context.setPictureInPictureParamsCompat(params: PictureInPictureParams) {
        if (this is android.app.Activity && isInPictureInPictureMode) {
            setPictureInPictureParams(params)
        }
    }
}
