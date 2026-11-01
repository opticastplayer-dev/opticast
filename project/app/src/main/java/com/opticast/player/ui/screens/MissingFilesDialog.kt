package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.posterUrlFor
import com.opticast.player.ui.components.formatDuration

@Composable
internal fun MissingFilesDialog(files: List<LocalVideo>, checking: Boolean, error: String?,
    onRecheck: () -> Unit, onDismissEntry: (Long) -> Unit, onDismissAll: () -> Unit, onClose: () -> Unit) {
    var dismissEntry by remember { mutableStateOf<LocalVideo?>(null) }
    var confirmDismissAll by remember { mutableStateOf(false) }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)) {
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(18.dp)) {
                Text("Missing files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Unavailable does not mean deleted. Reconnect storage or restore video access, then recheck. Matching local files reconnect only when the content signature is unique.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 10.dp))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (files.isEmpty()) item { Text("No unavailable files need review.", Modifier.padding(vertical = 12.dp)) }
                    items(files, key = { it.id }) { video ->
                        val metadata = AppContainer.metadataStore.get(video.id)
                        val progress = AppContainer.playbackState.progressOf(video.id)
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Local artwork only: reviewing unavailable files never fetches images.
                            val cached = posterUrlFor(LibraryEntry(video, metadata))?.takeUnless { it.startsWith("http") }
                            PosterImage(url = cached, fallbackTitle = metadata?.displayTitle ?: video.name, modifier = Modifier.width(56.dp).aspectRatio(2f / 3f))
                            Column(Modifier.weight(1f)) {
                                Text(metadata?.displayTitle ?: video.name, fontWeight = FontWeight.SemiBold)
                                Text(video.name, style = MaterialTheme.typography.bodySmall)
                                if (video.relativePath.isNotBlank()) Text("Last folder: ${video.relativePath}", style = MaterialTheme.typography.bodySmall)
                                Text(when {
                                    progress?.isWatched == true -> "Watched status retained"
                                    progress?.isResumable == true -> "Resume saved at ${progress.positionMs.formatDuration()}"
                                    else -> "Library record retained"
                                }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { dismissEntry = video }) { Text("Remove saved data") }
                            }
                        }
                    }
                }
                Text("Renamed copies, duplicate matches or changed files may remain separate to protect each video's history.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (files.isNotEmpty()) TextButton(onClick = { confirmDismissAll = true }) { Text("Dismiss all") }
                    TextButton(onClick = onClose) { Text("Close") }
                    TextButton(onClick = onRecheck, enabled = !checking) { Text(if (checking) "Checking…" else "Recheck") }
                }
            }
        }
    }
    if (confirmDismissAll) {
        AlertDialog(
            onDismissRequest = { confirmDismissAll = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            title = { Text("Remove data for all missing files?") },
            text = { Text("This permanently removes the current missing-file records and their saved metadata, artwork, subtitles, favourites, collections and playback progress. It does not delete any video files. New unavailable files can appear later.") },
            confirmButton = { TextButton(onClick = { onDismissAll(); confirmDismissAll = false; onClose() }) { Text("Remove saved data") } },
            dismissButton = { TextButton(onClick = { confirmDismissAll = false }) { Text("Cancel") } }
        )
    }
    dismissEntry?.let { entry ->
        AlertDialog(onDismissRequest = { dismissEntry = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            title = { Text("Remove saved data for this file?") },
            text = { Text("This permanently removes its missing-file record, metadata, cached artwork, downloaded subtitles, favourite/collection links and playback progress. It does not delete the video file. If it returns, OptiCast will scan it as a new library item.") },
            confirmButton = { TextButton(onClick = { onDismissEntry(entry.id); dismissEntry = null }) { Text("Remove saved data") } },
            dismissButton = { TextButton(onClick = { dismissEntry = null }) { Text("Cancel") } })
    }
}
