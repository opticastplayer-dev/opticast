package com.opticast.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opticast.player.R

/**
 * Library header - extracted for performance, only recomposes when scanning changes
 */
@Composable
fun LibraryHeader(
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    scanning: Boolean,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = LibraryPosterInsetDp.dp, vertical = 6.dp)) {
        val viewportWidth = maxWidth + (LibraryPosterInsetDp * 2).dp
        val rowWidth = maxOf(maxWidth, 248.dp)
        val logoWidth = minOf(viewportWidth * LibraryWordmarkWidthFraction, rowWidth - 148.dp)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).width(rowWidth),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Image(
                    painter = painterResource(R.drawable.opticast_wordmark),
                    contentDescription = "OptiCast",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.width(logoWidth).height(logoWidth * (67f / 260f))
                )
                Text(
                    "Your local cinema",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onCustomize, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Tune, "Customize Library", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onScan, enabled = !scanning, modifier = Modifier.size(48.dp)) {
                if (scanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.AutoFixHigh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (pressed) 0.95f else 0.55f),
                modifier = Modifier.size(48.dp).clickable(interactionSource = interaction, indication = null, onClick = onOpenSettings)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
