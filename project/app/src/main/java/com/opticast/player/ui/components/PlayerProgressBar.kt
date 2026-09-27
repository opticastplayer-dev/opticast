package com.opticast.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Interactive 48dp seek target with optional official Material 3 Expressive waves. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerProgressBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    style: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    chapterPositionsMs: List<Long> = emptyList(),
) {
    if (style == "hidden") return

    val latestChange = androidx.compose.runtime.rememberUpdatedState(onValueChange)
    val latestFinish = androidx.compose.runtime.rememberUpdatedState(onValueChangeFinished)
    val colors = MaterialTheme.colorScheme
    val trackColor = colors.onSurface.copy(alpha = 0.26f)
    val accent: Brush = if (style == "gradient") {
        Brush.horizontalGradient(listOf(colors.primary, colors.tertiary))
    } else {
        Brush.horizontalGradient(listOf(colors.primary, colors.primary))
    }
    val solidAccent = colors.primary
    val thumbColor = colors.primary

    val trackHeight = when (style) {
        "thick", "wavy", "default" -> 9.dp
        else -> 5.dp
    }
    val thumbRadius = when (style) {
        "thick", "wavy", "default" -> 10.dp
        else -> 7.5.dp
    }
    val barHeight = 48.dp

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f

    val fraction = ((value - valueRange.start) / span).let { if (it.isFinite()) it.coerceIn(0f, 1f) else 0f }
    val marks = remember(chapterPositionsMs, valueRange) {
        chapterFractions(chapterPositionsMs, valueRange.start, valueRange.endInclusive)
    }
    val stroke = with(LocalDensity.current) { androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = StrokeCap.Round) }

    fun valueAt(x: Float, width: Int): Float {
        if (width <= 0) return valueRange.start
        val fraction = (x / width).coerceIn(0f, 1f)
        return valueRange.start + fraction * span
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "Playback position"
                progressBarRangeInfo = ProgressBarRangeInfo(valueRange.start + fraction * span, valueRange)
                setProgress { target ->
                    if (!target.isFinite()) false else {
                        latestChange.value(target.coerceIn(valueRange)); latestFinish.value(); true
                    }
                }
            }
            .pointerInput(style, valueRange) {
                detectTapGestures { offset ->
                    latestChange.value(valueAt(offset.x, size.width))
                    latestFinish.value()
                }
            }
            .pointerInput(style, valueRange) {
                detectDragGestures(
                    onDragStart = { offset ->
                        latestChange.value(valueAt(offset.x, size.width))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        latestChange.value(valueAt(change.position.x, size.width))
                    },
                    onDragEnd = { latestFinish.value() },
                    onDragCancel = { latestFinish.value() },
                )
            },
    ) {
        if (style == "wavy") androidx.compose.material3.LinearWavyProgressIndicator(
            progress = { fraction }, modifier = Modifier.align(Alignment.Center).fillMaxWidth().height(24.dp)
                .clearAndSetSemantics { },
            color = solidAccent, trackColor = trackColor, stroke = stroke, trackStroke = stroke,
            amplitude = { 1f }, wavelength = 36.dp, waveSpeed = 108.dp, stopSize = 0.dp,
        )
        Canvas(Modifier.fillMaxWidth().height(barHeight)) {
            val centerY = size.height / 2f
            val track = trackHeight.toPx()
            val radius = thumbRadius.toPx()

            val played = size.width * fraction
            if (style != "wavy") {
            // Background track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, centerY - track / 2f),
                size = Size(size.width, track),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(track / 2f, track / 2f),
            )

            // Played portion
            if (played > 0f) {
                if (style == "gradient") {
                    drawRoundRect(
                        brush = accent,
                        topLeft = Offset(0f, centerY - track / 2f),
                        size = Size(played, track),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            track / 2f, track / 2f,
                        ),
                    )
                } else {
                    drawRoundRect(
                        color = solidAccent,
                        topLeft = Offset(0f, centerY - track / 2f),
                        size = Size(played, track),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            track / 2f, track / 2f,
                        ),
                    )
                }
            }

            }
            // High-contrast chapter ticks remain visible on all drawn track styles.
            marks.forEach { mark ->
                val x = size.width * mark
                drawLine(Color.Black.copy(alpha = 0.85f), Offset(x, centerY - 7.dp.toPx()),
                    Offset(x, centerY + 7.dp.toPx()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.9f), Offset(x, centerY - 5.dp.toPx()),
                    Offset(x, centerY + 5.dp.toPx()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            // Thumb.
            // coerceIn(min, max) THROWS when max < min, which happens on a very
            // narrow bar (long duration label on a cramped row) - and it would
            // throw inside the draw phase. Guard the degenerate case instead.
            val thumbX = if (size.width <= radius * 2f) {
                size.width / 2f
            } else {
                played.coerceIn(radius, size.width - radius)
            }
            drawCircle(
                color = thumbColor,
                radius = radius,
                center = Offset(thumbX, centerY),
            )
            // A soft ring so the thumb reads against light and dark frames alike.
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = radius,
                center = Offset(thumbX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
        }
    }
}

/** Human label for a progress-bar style, for the Settings summary line. */
fun progressBarStyleLabel(style: String): String = when (style) {
    "wavy", "thin" -> "Thick Wavy · Expressive"
    "thick" -> "Thick"
    "gradient" -> "Gradient"
    "hidden" -> "Hidden"
    else -> "Thick"
}
