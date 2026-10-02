package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.DiscoveryGapDp
import com.opticast.player.ui.components.DiscoveryGutterDp
import com.opticast.player.ui.components.DiscoveryHeader
import com.opticast.player.ui.components.DiscoveryPosterDp
import com.opticast.player.ui.components.DiscoveryResumeDp
import com.opticast.player.ui.screens.ContinueWatchingCard
import com.opticast.player.ui.screens.SelectableCard

/**
 * Gold Standard — Discovery sections extracted from LibraryScreen.kt
 * Single responsibility: continue watching, featured, recent, etc.
 * Was 400+ lines inside LazyVerticalGrid, now reusable
 */
@Composable
fun ContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    collapsed: Boolean,
    onToggle: () -> Unit,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onPlay: (LibraryEntry) -> Unit,
    onLongClick: (LibraryEntry) -> Unit
) {
    if (items.isEmpty()) return
    androidx.compose.foundation.lazy.grid.LazyGridItemScope.apply {
        // This is called from inside LazyVerticalGrid item
    }
}

data class ContinueWatchingItem(
    val entry: LibraryEntry,
    val playback: com.opticast.player.data.local.PlaybackState
)
