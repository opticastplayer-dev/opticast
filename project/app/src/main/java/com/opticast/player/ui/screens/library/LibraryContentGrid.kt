package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry

/**
 * Gold Standard — Content grid helpers extracted from LibraryScreen.kt
 * Each section is a LazyGridScope extension, single responsibility
 */

fun LazyGridScope.libraryFileAvailabilitySection(
    missingCount: Int,
    fileScanError: String?,
    checkingFiles: Boolean,
    onReview: () -> Unit,
    onRecheck: () -> Unit
) {
    if (missingCount > 0 || fileScanError != null) {
        item(key = "file-availability", span = { GridItemSpan(maxLineSpan) }) {
            Surface(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
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
                        androidx.compose.material3.TextButton(onClick = onReview) { Text("Review files") }
                        androidx.compose.material3.TextButton(onClick = onRecheck, enabled = !checkingFiles) { Text("Recheck storage") }
                    }
                }
            }
        }
    }
}

fun LazyGridScope.libraryMatchingSection(
    isMatching: Boolean,
    matchingDone: Int,
    matchingTotal: Int
) {
    if (isMatching) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                )
            }
        }
    }
}
