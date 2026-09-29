package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.PosterCard

/**
 * Optimized library grid — smooth like settings, fast startup
 * - Grid changeable: remember outside LazyVerticalGrid triggers recomposition
 * - Stable keys: video.id prevents reordering jank
 * - ContentType: helps Compose skip recomposition
 * - AnimateItem: smooth animations when grid changes
 * - Offline-first: uses cached posters, no network while scrolling
 */
@Composable
fun LibraryGrid(
    entries: List<LibraryEntry>,
    libraryGrid: String,
    gridState: LazyGridState,
    chromeScroll: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    padding: PaddingValues,
    onPosterClick: (LibraryEntry) -> Unit,
    onPosterLongClick: (LibraryEntry) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    filterContent: @Composable () -> Unit = {},
    statsContent: @Composable () -> Unit = {},
    discoveryContent: @Composable () -> Unit = {}
) {
    // Stability: Ensure grid changeable works — compute outside and use key
    // Remember libraryGrid to trigger recomposition when user changes grid in settings
    val gridCells = remember(libraryGrid) {
        GridCells.Adaptive(libraryPosterMinimumDp(libraryGrid).dp)
    }
    
    LazyVerticalGrid(
        state = gridState,
        columns = gridCells,
        modifier = modifier.fillMaxSize().nestedScroll(chromeScroll),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "header", contentType = "header") { headerContent() }
        item(span = { GridItemSpan(maxLineSpan) }, key = "search", contentType = "search") { filterContent() }
        item(span = { GridItemSpan(maxLineSpan) }, key = "stats", contentType = "stats") { statsContent() }
        item(span = { GridItemSpan(maxLineSpan) }, key = "discovery", contentType = "discovery") { discoveryContent() }
        items(
            items = entries,
            key = { it.video.id },
            contentType = { "poster" }
        ) { entry ->
            // Stability: animateItem for smooth grid changes, stable key prevents jank
            PosterCard(
                entry = entry,
                onClick = { onPosterClick(entry) },
                onLongClick = { onPosterLongClick(entry) },
                modifier = Modifier.animateItem()
            )
        }
    }
}
