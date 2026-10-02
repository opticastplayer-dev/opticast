package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.DiscoveryCollectionDp
import com.opticast.player.ui.components.DiscoveryGapDp
import com.opticast.player.ui.components.DiscoveryGutterDp
import com.opticast.player.ui.components.DiscoveryHeader
import com.opticast.player.ui.components.DiscoveryPosterDp
import com.opticast.player.ui.components.DiscoveryResumeDp
import com.opticast.player.ui.screens.CollectionCover
import com.opticast.player.ui.screens.ContinueWatchingCard
import com.opticast.player.ui.screens.HeroPager
import com.opticast.player.ui.screens.PosterCard
import com.opticast.player.ui.screens.SelectableCard
import com.opticast.player.ui.screens.ShowCard
import com.opticast.player.ui.screens.discoveryHeading

/**
 * Gold Standard — Discovery content extracted from LibraryScreen.kt
 * Single responsibility: continue watching, featured, recently added, movies, shows, watched, collections
 * Was 300+ lines inside LazyVerticalGrid, now reusable
 */

fun LazyGridScope.continueWatchingSection(
    continueWatching: List<ContinueWatchingData>,
    collapsed: Boolean,
    onToggle: () -> Unit,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onPlay: (LibraryEntry) -> Unit,
    onLongClick: (LibraryEntry) -> Unit
) {
    if (continueWatching.isEmpty()) return
    item(key = "discovery-continue-header", span = { GridItemSpan(maxLineSpan) }) {
        DiscoveryHeader(
            "Continue Watching",
            "Pick up right where you left off.",
            Color(0xFFFFBF56),
            continueWatching.size,
            expanded = !collapsed,
            onToggle = onToggle
        )
    }
    if (!collapsed) item(key = "discovery-continue-content", span = { GridItemSpan(maxLineSpan) }) {
        val carouselState = rememberLazyListState()
        LazyRow(
            state = carouselState,
            contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
        ) {
            items(continueWatching, key = { "cw-${it.entry.video.id}" }) { item ->
                SelectableCard(
                    selectionMode, item.entry.video.id in selectedIds,
                    modifier = Modifier.width(DiscoveryResumeDp.dp),
                    onToggle = { onToggleSelect(item.entry.video.id) }
                ) {
                    ContinueWatchingCard(
                        entry = item.entry,
                        progress = item.playback.progress,
                        remainingLabel = if (item.playback.durationMs > 0) "${item.playback.remainingMs.formatDuration()} left"
                        else "Resume at ${item.playback.positionMs.formatDuration()}",
                        onClick = { if (selectionMode) onToggleSelect(item.entry.video.id) else onPlay(item.entry) },
                        modifier = Modifier,
                        compact = true,
                        selectionMode = selectionMode,
                        onLongClick = { if (selectionMode) onToggleSelect(item.entry.video.id) else onLongClick(item.entry) }
                    )
                }
            }
        }
    }
}

data class ContinueWatchingData(
    val entry: LibraryEntry,
    val playback: com.opticast.player.data.local.PlaybackState
)

fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun LazyGridScope.featuredSection(
    featured: List<LibraryEntry>,
    collapsed: Boolean,
    onToggle: () -> Unit,
    showExtras: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onPlay: (LibraryEntry) -> Unit,
    onHold: (LibraryEntry) -> Unit
) {
    if (!showExtras || featured.isEmpty()) return
    item(key = "discovery-featured-header", span = { GridItemSpan(maxLineSpan) }) {
        DiscoveryHeader("Featured", "A little cinema, from your own collection.", Color(0xFF63CFFF), expanded = !collapsed, onToggle = onToggle)
    }
    if (!collapsed) item(key = "discovery-featured-content", span = { GridItemSpan(maxLineSpan) }) {
        com.opticast.player.ui.screens.HeroPager(
            items = featured,
            onOpenDetail = onOpenDetail,
            onPlay = onPlay,
            selectionMode = selectionMode,
            selectedIds = selectedIds,
            onToggle = { onToggleSelect(it.video.id) },
            onHold = onHold
        )
    }
}

fun LazyGridScope.recentlyAddedSection(
    recentlyAdded: List<LibraryEntry>,
    collapsed: Boolean,
    onToggle: () -> Unit,
    showExtras: Boolean,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleSelect: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onLongClick: (LibraryEntry) -> Unit,
    sharedTransitionScope: com.opticast.player.ui.screens.SharedTransitionScope?,
    animatedVisibilityScope: com.opticast.player.ui.screens.AnimatedVisibilityScope?
) {
    if (!showExtras || recentlyAdded.isEmpty()) return
    item(key = "discovery-recent-header", span = { GridItemSpan(maxLineSpan) }) {
        DiscoveryHeader("Recently added", "Fresh arrivals. Ready when you are.", Color(0xFF63DAB0), recentlyAdded.size, expanded = !collapsed, onToggle = onToggle)
    }
    if (!collapsed) item(key = "discovery-recent-content", span = { GridItemSpan(maxLineSpan) }) {
        val carouselState = rememberLazyListState()
        LazyRow(
            state = carouselState,
            contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
        ) {
            items(recentlyAdded, key = { "recent-${it.video.id}" }) { entry ->
                com.opticast.player.ui.screens.SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.width(DiscoveryPosterDp.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)), onToggle = { onToggleSelect(entry.video.id) }) {
                    PosterCard(
                        entry = entry,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { if (selectionMode) onToggleSelect(entry.video.id) else onOpenDetail(entry.video.id) },
                        onLongClick = { if (selectionMode) onToggleSelect(entry.video.id) else onLongClick(entry) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

fun LazyGridScope.collectionsSection(
    allCollections: List<com.opticast.player.ui.screens.PersonalCollection>,
    libraryEntriesById: Map<Long, LibraryEntry>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onToggleMembers: (List<Long>) -> Unit,
    onOpenCollection: (String) -> Unit,
    onShowCollections: () -> Unit,
    onSelectCollection: (List<Long>) -> Unit
) {
    item(key = "discovery-collections", span = { GridItemSpan(maxLineSpan) }) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(discoveryHeading("Collections"), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onShowCollections) { Text("Manage") }
            }
            if (allCollections.isEmpty()) TextButton(
                onClick = onShowCollections,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) { Text("Create a personal or franchise collection") }
            LazyRow(
                contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
            ) {
                items(allCollections, key = { it.id }) { collection ->
                    val members = remember(collection.videoIds, libraryEntriesById) {
                        collection.videoIds.mapNotNull { libraryEntriesById[it] }
                    }
                    fun toggleMembers() {
                        val ids = members.map { it.video.id }
                        onToggleMembers(ids)
                    }
                    SelectableCard(
                        selectionMode,
                        members.isNotEmpty() && members.all { it.video.id in selectedIds },
                        Modifier.width(DiscoveryCollectionDp.dp),
                        onToggle = { toggleMembers() }
                    ) {
                        CollectionCover(
                            collection, members,
                            onClick = { if (selectionMode) toggleMembers() else onOpenCollection(collection.id) },
                            onHold = { onSelectCollection(members.map { it.video.id }.filterNot { it in selectedIds }) }
                        )
                    }
                }
            }
        }
    }
}
