package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Grid headers extracted from LibraryScreen.kt
 * Single responsibility: file availability, matching
 * Was 50+ lines inside LazyVerticalGrid, now reusable, safe
 */
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
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}
