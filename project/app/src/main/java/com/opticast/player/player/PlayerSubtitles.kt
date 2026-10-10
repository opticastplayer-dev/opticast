package com.opticast.player.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView

/**
 * Subtitle handling extracted for maintainability.
 * No logic change, just organization.
 */

fun captionStyleFor(index: Int): CaptionStyleCompat = when (index) {
    1 -> CaptionStyleCompat(
        Color.White.toArgb(),
        0xB3000000.toInt(),
        Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.Transparent.toArgb(),
        null,
    )
    3 -> CaptionStyleCompat(
        Color.White.toArgb(), Color.Transparent.toArgb(), Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_NONE, Color.Transparent.toArgb(), null,
    )
    2 -> CaptionStyleCompat(
        Color.Yellow.toArgb(),
        Color.Transparent.toArgb(),
        Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        Color.Black.toArgb(),
        null,
    )
    else -> CaptionStyleCompat.DEFAULT
}

fun configureSubtitleView(
    subtitleView: SubtitleView?,
    captionStyle: Int,
    captionScale: Float
) {
    subtitleView?.apply {
        setStyle(captionStyleFor(captionStyle))
        setFractionalTextSize(
            SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * captionScale * 0.9f
        )
        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setBottomPaddingFraction(0.12f)
    }
}

@Composable
fun SecondarySubtitleOverlay(
    secondaryCues: List<SubtitleCue>,
    positionMs: Long,
    controlsVisible: Boolean
) {
    SubtitleCues.cueAt(secondaryCues, positionMs)?.let { line ->
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = if (controlsVisible) 128.dp else 88.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.55f),
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
