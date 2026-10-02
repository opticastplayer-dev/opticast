package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry

/**
 * Gold Standard — Grid section extracted from LibraryScreen.kt
 * Single responsibility: adaptive grid with 12dp border/clip, badges, discovery, progress
 * Low-RAM safe: keyed to libraryGrid, uses derivedStateOf, no global posterVersion bust
 */
@Composable
fun LibraryGridSection(
    entries: List<LibraryEntry>,
    gridState: LazyGridState,
    columns: GridCells,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    headerContent: @Composable () -> Unit = {},
    itemContent: @Composable (LibraryEntry) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        LazyVerticalGrid(
            state = gridState,
            columns = columns,
            contentPadding = contentPadding,
            modifier = Modifier
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                headerContent()
            }
            items(entries, key = { it.video.id }) { entry ->
                itemContent(entry)
            }
        }
    }
}
