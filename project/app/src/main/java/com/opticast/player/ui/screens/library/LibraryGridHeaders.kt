package com.opticast.player.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opticast.player.ui.screens.DiscoveryGutterDp
import com.opticast.player.ui.screens.FilterChipsRow
import com.opticast.player.ui.screens.LibraryHeader
import com.opticast.player.ui.screens.StatsCard
import com.opticast.player.ui.screens.WhatsNewCard
import com.opticast.player.ui.screens.LibraryDesignStore
import com.opticast.player.ui.screens.LibraryStats

/**
 * Gold Standard — Grid headers extracted from LibraryScreen.kt
 * Single responsibility: WhatsNew, search controls, file availability, matching, stats, filters
 * Was 150+ lines inside LazyVerticalGrid, now reusable
 */

internal fun LazyGridScope.libraryHeader(
    scanning: Boolean,
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    onScan: () -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
            LibraryHeader(
                onOpenSettings = onOpenSettings,
                onCustomize = onCustomize,
                scanning = scanning,
                onScan = onScan
            )
        }
    }
}

internal fun LazyGridScope.libraryWhatsNew(
    context: android.content.Context,
    onDismiss: (String?) -> Unit
) {
    item(span = { GridItemSpan(maxLineSpan) }, contentType = "whats-new") {
        val whatsNewVersion = com.opticast.player.data.remote.UpdateChecker.getWhatsNewVersion(context)
        if (whatsNewVersion != null) {
            WhatsNewCard(version = whatsNewVersion, onDismiss = {
                com.opticast.player.data.remote.UpdateChecker.dismissWhatsNew(context)
                onDismiss(null)
            })
        }
    }
}

internal fun LazyGridScope.librarySearchControls(
    searchScope: String,
    onSearchScopeChange: (String) -> Unit,
    query: String,
    recentQueries: List<String>,
    onQueryChange: (String) -> Unit,
    designStore: com.opticast.player.ui.screens.LibraryDesignStore,
    onDesignRevisionChange: () -> Unit,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    item(key = "focused-search-controls", span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = DiscoveryGutterDp.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", "movies" to "Movies", "tv" to "TV").forEach { (id, label) ->
                    FilterChip(selected = searchScope == id, onClick = { onSearchScopeChange(id) }, label = { Text(label) })
                }
            }
            if (query.isBlank() && recentQueries.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent searches", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { designStore.clearHistory(); onDesignRevisionChange() }) { Text("Clear") }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(recentQueries, key = { it }) { recent ->
                        SuggestionChip(
                            onClick = { onQueryChange(recent); designStore.recordQuery(recent); onDesignRevisionChange(); keyboard?.hide() },
                            label = { Text(recent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp)) }
                        )
                    }
                }
            }
        }
    }
}

internal fun LazyGridScope.libraryFileAvailability(
    missingCount: Int,
    fileScanError: String?,
    checkingFiles: Boolean,
    onReview: () -> Unit,
    onRecheck: () -> Unit
) {
    if (missingCount > 0 || fileScanError != null) {
        item(key = "file-availability", span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                Modifier.padding(horizontal = DiscoveryGutterDp.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        if (checkingFiles) "Checking local files…" else if (fileScanError != null) "Storage check needs attention"
                        else "$missingCount unavailable file${if (missingCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        fileScanError ?: "Saved posters and progress are kept. Review files that were moved, removed or are temporarily inaccessible.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row {
                        TextButton(onClick = onReview) { Text("Review files") }
                        TextButton(onClick = onRecheck, enabled = !checkingFiles) { Text("Recheck storage") }
                    }
                }
            }
        }
    }
}

internal fun LazyGridScope.libraryMatching(
    isMatching: Boolean,
    matchingDone: Int,
    matchingTotal: Int
) {
    if (isMatching) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(horizontal = DiscoveryGutterDp.dp, vertical = 10.dp)) {
                Text(
                    "Identifying titles… $matchingDone/$matchingTotal",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                com.opticast.player.ui.components.FastLoadingBar(
                    progress = if (matchingTotal == 0) null else matchingDone.toFloat() / matchingTotal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

internal fun LazyGridScope.libraryStatsAndFilters(
    showStats: Boolean,
    stats: com.opticast.player.ui.screens.LibraryStats,
    sortBy: String,
    onSortChange: (String) -> Unit,
    selectedGenre: String,
    onGenreClick: () -> Unit,
    hasEntries: Boolean
) {
    if (hasEntries) {
        if (showStats) item(span = { GridItemSpan(maxLineSpan) }) {
            StatsCard(stats)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FilterChipsRow(
                sortBy = sortBy,
                onSortChange = onSortChange,
                selectedGenre = selectedGenre,
                onGenreClick = onGenreClick
            )
        }
    }
}
