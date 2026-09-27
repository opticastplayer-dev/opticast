package com.opticast.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal fun stackLabelValue(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 280f || fontScale > 1.3f

/** Shared baselines and bounded columns; large text/narrow windows reflow instead of clipping. */
@Composable
internal fun AlignedLabelValue(label: String, value: String, modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface, valueAtEnd: Boolean = false) {
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (stackLabelValue(maxWidth.value, LocalDensity.current.fontScale)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, Modifier.weight(0.36f).alignByBaseline(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, Modifier.weight(0.64f).alignByBaseline(), style = MaterialTheme.typography.bodyMedium,
                    color = valueColor, textAlign = if (valueAtEnd) TextAlign.End else TextAlign.Start)
            }
        }
    }
}
