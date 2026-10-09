package com.opticast.player.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.opticast.player.data.model.LocalVideo

/**
 * Launches a video in the user's chosen external player (whatever third-party
 * player they have installed)
 * and captures the position the player reports when it exits, so OptiCast can
 * resume from there - in the external player or the built-in one.
 *
 * Returns a `launch(video, resumeMs, title)` callback.
 */
@Composable
fun rememberExternalPlayer(
    onProgress: (video: LocalVideo, positionMs: Long, durationMs: Long) -> Unit,
): (LocalVideo, Long, String) -> Unit {
    var pendingVideo by remember { mutableStateOf<LocalVideo?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val video = pendingVideo ?: return@rememberLauncherForActivityResult
        pendingVideo = null
        val data = result.data ?: return@rememberLauncherForActivityResult
        val position = numericExtra(data, "position")
            ?: numericExtra(data, "extra_position")
        val duration = numericExtra(data, "duration")
            ?: numericExtra(data, "extra_duration")
        if (position != null && duration != null &&
            duration > 0 && position in 0..duration
        ) {
            onProgress(video, position, duration)
        }
    }

    return remember {
        { video: LocalVideo, resumeMs: Long, title: String ->
            // Do not replace the identity awaiting an external player's result.
            if (pendingVideo == null) {
            pendingVideo = video
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(video.uri), "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("title", title)
                // External players honour this and start from the saved spot.
                if (resumeMs > 0) putExtra("position", resumeMs.toInt())
            }
            runCatching { launcher.launch(intent) }.onFailure { pendingVideo = null }
            }
        }
    }
}

/** Reads int/long/string extras defensively - players are inconsistent. */
private fun numericExtra(intent: Intent, key: String): Long? =
    runCatching {
        when (val value = intent.extras?.get(key)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }.getOrNull()
