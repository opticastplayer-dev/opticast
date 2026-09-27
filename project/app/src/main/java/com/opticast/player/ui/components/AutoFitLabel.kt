package com.opticast.player.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/** Bounded one-line fitting for short UI labels; never shrinks below 11sp. */
@Composable
internal fun AutoFitLabel(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.labelLarge, maxSp: Int = 14, minSp: Int = 11) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val fontSize = remember(text, widthPx, style, maxSp, minSp, density) {
            var size = maxSp.coerceAtLeast(11)
            val floor = minSp.coerceIn(11, size)
            while (size > floor && measurer.measure(text, style.copy(fontSize = size.sp), softWrap = false).size.width > widthPx) size--
            size.sp
        }
        Text(text, Modifier.fillMaxWidth(), color = color, style = style.copy(fontSize = fontSize),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
