package com.opticast.player.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal fun unlockSlideComplete(value: Float): Boolean = value.isFinite() && value >= 0.92f
internal fun effectiveVolumeLevel(systemFraction: Float, boostPercent: Int): Float =
    (if (systemFraction.isFinite()) systemFraction.coerceIn(0f, 1f) else 0f) * boostPercent.coerceIn(100, 400) / 100f

/** Only a drag originating at the handle unlocks; tapping the far end does not. */
@Composable
internal fun SlideToUnlock(onDraggingChanged: (Boolean) -> Unit = {}, onUnlock: () -> Unit) {
    var dragPx by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val unlock = rememberUpdatedState(onUnlock)
    val draggingChanged = rememberUpdatedState(onDraggingChanged)
    DisposableEffect(Unit) { onDispose { draggingChanged.value(false) } }
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(30.dp), color = colors.surfaceContainerHigh.copy(alpha = 0.94f),
        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(0.78f)) {
        Box(Modifier.padding(6.dp).fillMaxWidth().height(48.dp).onSizeChanged { widthPx = it.width }
            .semantics(mergeDescendants = true) {
                contentDescription = "Slide right to unlock playback controls"
                customActions = listOf(CustomAccessibilityAction("Unlock playback controls") { unlock.value(); true })
            }
            .pointerInput(widthPx) {
                val travel = (widthPx - 48.dp.toPx()).coerceAtLeast(1f)
                var validStart = false
                detectHorizontalDragGestures(
                    onDragStart = { start -> validStart = start.x <= 64.dp.toPx(); dragPx = 0f; draggingChanged.value(validStart) },
                    onHorizontalDrag = { change, delta ->
                        if (validStart) { change.consume(); dragPx = (dragPx + delta).coerceIn(0f, travel) }
                    },
                    onDragEnd = { draggingChanged.value(false); if (validStart && unlockSlideComplete(dragPx / travel)) unlock.value(); dragPx = 0f },
                    onDragCancel = { draggingChanged.value(false); dragPx = 0f },
                )
            }, contentAlignment = Alignment.Center) {
            Text("Slide to unlock  →", modifier = Modifier.padding(start = 42.dp), style = MaterialTheme.typography.labelLarge)
            Box(Modifier.align(Alignment.CenterStart).offset { IntOffset(dragPx.roundToInt(), 0) }
                .size(48.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LockOpen, null, tint = colors.onPrimary)
            }
        }
    }
}
