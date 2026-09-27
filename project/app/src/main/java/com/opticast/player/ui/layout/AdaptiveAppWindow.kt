package com.opticast.player.ui.layout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.window.layout.WindowMetricsCalculator

internal fun windowActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val next = current.baseContext
        if (next === current) break
        current = next
    }
    return null
}

@Composable
internal fun AdaptiveAppWindow(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val activity = windowActivity(LocalContext.current)
    val bounds = activity?.let { WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(it).bounds }
    val normalized = Configuration(configuration).apply {
        // Use actual window bounds, not an inferred whole-display zoom ratio.
        // Respect Android's current display density and font scale. Layouts adapt;
        // never replace Compose/native view density or reset the user's font size.
        if (bounds != null) {
            screenWidthDp = windowPixelsToDp(bounds.width(), density.density)
            screenHeightDp = windowPixelsToDp(bounds.height(), density.density)
            smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
        }
    }
    CompositionLocalProvider(LocalConfiguration provides normalized, content = content)
}
