package com.opticast.player.ui

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

/**
 * Edge-to-edge support for Android 15+ - for 9/10 rating
 * - Content behind status bar and navigation bar
 * - No black bars
 * - Feels modern, more screen space
 * - Required for Google Play target SDK 35
 */
object EdgeToEdgeHelper {
    
    fun enableEdgeToEdge(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = Color.Transparent.toArgb()
            activity.window.navigationBarColor = Color.Transparent.toArgb()
        } else {
            @Suppress("DEPRECATION")
            activity.window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            activity.window.navigationBarColor = Color.Transparent.toArgb()
        }
    }
    
    fun enableForActivity(activity: ComponentActivity) {
        activity.enableEdgeToEdge()
    }
}

/**
 * Modifier for edge-to-edge content
 */
@Composable
fun Modifier.edgeToEdgePadding(): Modifier {
    return this
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .windowInsetsPadding(WindowInsets.ime)
}

/**
 * Predictive back gesture support for Android 14+
 * - Shows preview of previous screen when back gesture
 * - Feels like system app
 */
object PredictiveBackHelper {
    
    fun isPredictiveBackAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }
}
