package com.opticast.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy

/**
 * Optimized poster with placeholder for buttery smooth scrolling on low-RAM 32-bit devices.
 * - Placeholder shows immediately (12dp rounded, matches your design)
 * - Crossfade prevents white flash
 * - Memory cache policy reduces GC pressure
 * - ContentScale.Crop with stable key prevents recomposition
 */
@Composable
fun OptimizedPoster(
    posterUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val context = LocalContext.current
    val placeholderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2A2A),
                Color(0xFF3A3A3A),
                Color(0xFF2A2A2A)
            )
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(placeholderBrush)
    ) {
        if (posterUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(posterUrl)
                    .crossfade(true)
                    .crossfade(200)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(posterUrl)
                    .diskCacheKey(posterUrl)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Placeholder that matches your 12dp border/clip design
 * Shows immediately while real poster loads
 */
@Composable
fun PosterPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val shimmerBrush = remember(surfaceVariant, surface) {
        Brush.linearGradient(
            colors = listOf(
                surfaceVariant,
                surface,
                surfaceVariant
            )
        )
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(shimmerBrush)
    )
}
