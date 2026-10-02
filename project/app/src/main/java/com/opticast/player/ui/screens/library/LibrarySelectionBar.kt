package com.opticast.player.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer

/**
 * Gold Standard — Selection bar extracted from LibraryScreen.kt
 * Single responsibility: floating multi-select action bar
 */
@Composable
fun LibrarySelectionBar(
    selectedIds: List<Long>,
    tab: String,
    onMarkWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selectedIds.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onMarkWatched) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Mark selected as watched",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) Icons.Outlined.FavoriteBorder else Icons.Filled.Favorite,
                        contentDescription = if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) "Remove selected from favorites" else "Add selected to favorites",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onExit) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Exit selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
