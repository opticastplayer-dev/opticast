package com.opticast.player.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.WindowMetricsCalculator
import kotlin.math.roundToInt

/** One IME owner, tolerant of both full-height edge-to-edge and OEM-resized roots. */
@Composable
internal fun KeyboardAwareViewport(content: @Composable BoxScope.() -> Unit) {
    val activity = windowActivity(LocalContext.current)
    // Configuration invalidates window bounds after rotation/split-screen changes.
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val windowHeight = activity?.let {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(it).bounds.height()
    } ?: (configuration.screenHeightDp * density.density).roundToInt()
    val ime = WindowInsets.ime
    var hostTop by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()
        .onGloballyPositioned { hostTop = it.positionInWindow().y.roundToInt() }
        .layout { measurable, constraints ->
            if (!constraints.hasBoundedHeight) {
                val child = measurable.measure(constraints)
                layout(child.width, child.height) { child.placeRelative(0, 0) }
            } else {
                // Read inset values in measurement so IME animation does not lag a frame.
                val overlap = remainingImeOverlapPx(windowHeight, hostTop, constraints.maxHeight, ime.getBottom(this))
                val available = (constraints.maxHeight - overlap).coerceAtLeast(0)
                val child = measurable.measure(constraints.copy(
                    minHeight = constraints.minHeight.coerceAtMost(available), maxHeight = available))
                layout(child.width, constraints.maxHeight) { child.placeRelative(0, 0) }
            }
        }
        // Even when Android did the resize, descendants must not apply the IME again.
        .consumeWindowInsets(ime), content = content)
}
