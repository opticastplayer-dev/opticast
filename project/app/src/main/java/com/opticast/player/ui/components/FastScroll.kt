package com.opticast.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Fast scroll thumb like Infuse - alphabet thumb for quick navigation
 * - Shows when scrolling, hides after 1s
 * - Drag to fast scroll through library
 * - Lightweight, no performance impact
 */
@Composable
fun FastScrollThumb(
    gridState: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var showThumb by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val alpha by animateFloatAsState(
        targetValue = if (showThumb || isDragging) 0.6f else 0f,
        label = "fastscroll_alpha"
    )
    
    // Show thumb when scrolling
    LaunchedEffect(gridState.isScrollInProgress) {
        if (gridState.isScrollInProgress) {
            showThumb = true
        } else {
            kotlinx.coroutines.delay(1000)
            if (!isDragging) showThumb = false
        }
    }
    
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .alpha(alpha)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { isDragging = true; showThumb = true },
                    onDragEnd = { 
                        isDragging = false
                        scope.launch {
                            kotlinx.coroutines.delay(1000)
                            showThumb = false
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        scope.launch {
                            val scrollAmount = (dragAmount / 2).toInt()
                            val current = gridState.firstVisibleItemIndex
                            val target = (current + scrollAmount).coerceIn(0, itemCount - 1)
                            gridState.scrollToItem(target)
                        }
                    }
                )
            }
    )
}

/**
 * Exact skeleton that matches grid cell size - no generic placeholder
 * Shows shimmer that matches real poster dimensions
 */
@Composable
fun ExactSkeleton(
    gridType: String,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 12
    val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color(0xFF2A2A2A),
            Color(0xFF3A3A3A),
            Color(0xFF2A2A2A)
        )
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(shimmerBrush)
    )
}
