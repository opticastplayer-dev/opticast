package com.opticast.player.player

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.opticast.player.data.AppContainer
import com.opticast.player.data.AppSettings

/**
 * Handles orientation for player.
 * Starts in landscape when autoLandscape is enabled, respects orientation mode.
 * Default mode is sensor per user request.
 */
@Composable
fun HandlePlayerOrientation(
    activity: Activity?,
    appSettings: AppSettings
) {
    // Instant landscape on entry when toggle is on - uses SENSOR_LANDSCAPE to allow rotation
    LaunchedEffect(Unit) {
        if (AppContainer.initialSettings.autoLandscape) {
            activity?.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Orientation mode handling - respects autoLandscape toggle
    LaunchedEffect(appSettings.orientationMode, appSettings.autoLandscape) {
        val act = activity ?: return@LaunchedEffect
        val autoLand = appSettings.autoLandscape
        act.requestedOrientation = when (appSettings.orientationMode) {
            "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "sensor" -> if (autoLand) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "locked" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
            else -> if (autoLand) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
