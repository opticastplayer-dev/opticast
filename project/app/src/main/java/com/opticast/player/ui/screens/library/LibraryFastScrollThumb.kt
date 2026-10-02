package com.opticast.player.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Gold Standard — Fast-scroll thumb extracted from LibraryScreen.kt
 * Low-RAM safe with derivedStateOf + graphicsLayer, like Infuse
 */
@Composable
fun LibraryFastScrollThumb(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val showThumb by remember { derivedStateOf { gridState.isScrollInProgress } }
    val firstVisible by remember { derivedStateOf { gridState.firstVisibleItemIndex } }
    val totalItems by remember { derivedStateOf { gridState.layoutInfo.totalItemsCount } }
    AnimatedVisibility(
        visible = showThumb && totalItems > 20,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(32.dp)
                .padding(vertical = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val progress = if (totalItems > 0) firstVisible.toFloat() / totalItems else 0f
            Box(
                Modifier
                    .fillMaxHeight(0.1f)
                    .width(4.dp)
                    .graphicsLayer { translationY = progress * 200f }
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
}
