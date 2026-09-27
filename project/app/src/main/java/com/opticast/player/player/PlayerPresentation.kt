package com.opticast.player.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/** -100 = half size, 0 = fitted size, +100 = double size; video never vanishes. */
internal fun zoomPercent(scale: Float): Int = (((scale.coerceIn(0.5f, 2f) - 1f) *
    if (scale < 1f) 200f else 100f)).roundToInt().coerceIn(-100, 100)
internal fun clampedVideoScale(scale: Float): Float = if (scale.isFinite()) scale.coerceIn(0.5f, 2f) else 1f
internal fun aspectLabel(mode: Int): String = when (mode) {
    0 -> "Fit screen"; 1 -> "Fit width"; 2 -> "Fit height"; 3 -> "Stretch"; 4 -> "Fill / crop"; AGGRESSIVE_STRETCH -> "Stretch+ / crop"; else -> "Fit screen"
}
internal fun subtitleFileExtension(name: String): String? = name.substringAfterLast('.', "").lowercase()
    .takeIf { it in setOf("srt", "ass", "ssa", "vtt") }

/** Scrollable control card inheriting the selected app theme, including device colours. */
@Composable
internal fun PlayerMenu(onDismissRequest: () -> Unit, maxWidth: Dp = 600.dp, headerTitle: String = "Playback controls", content: @Composable () -> Unit) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.86f).dp
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.padding(horizontal = 16.dp).widthIn(max = maxWidth).fillMaxWidth()
                .heightIn(max = maxHeight), shape = RoundedCornerShape(24.dp),
                color = colors.surfaceContainer.copy(alpha = 0.90f), contentColor = colors.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(headerTitle, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        IconButton(onClick = onDismissRequest) { Icon(Icons.Filled.Close, "Close controls") }
                    }
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp)) { content() }
                }
            }
    }
}

@Composable
internal fun VerticalGestureHud(icon: ImageVector, fraction: Float, left: Boolean, label: String) {
    val level by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(90), label = "gestureLevel")
    val accent = if (fraction > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                Box(Modifier.width(9.dp).height(116.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.17f))) {
                    Box(Modifier.fillMaxWidth().fillMaxHeight(level).align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.72f)))))
                }
                Text("${(fraction.coerceAtLeast(0f) * 100).roundToInt()}%", color = accent,
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun PlayerActionButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Surface(Modifier.size(44.dp), shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
    }
}

internal fun compactPlayerHeader(widthDp: Float, fontScale: Float): Boolean = widthDp < 400f || fontScale > 1.3f

@Composable
internal fun PlayerTopBar(modifier: Modifier = Modifier,
    title: @Composable RowScope.() -> Unit, actions: @Composable RowScope.() -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (compactPlayerHeader(maxWidth.value, androidx.compose.ui.platform.LocalDensity.current.fontScale)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = title)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End), content = actions)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) { title(); actions() }
        }
    }
}
