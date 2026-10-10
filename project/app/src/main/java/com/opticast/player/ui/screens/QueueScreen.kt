package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    entries: List<LibraryEntry>,
    onBack: () -> Unit,
    onPlay: (Long) -> Unit
) {
    val queue by AppContainer.queueStore.queue.collectAsState()
    val queueEntries = remember(queue, entries) {
        queue.mapNotNull { q -> entries.find { it.video.id == q.videoId }?.let { q to it } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Queue (${queue.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (queue.isNotEmpty()) {
                        IconButton(onClick = { AppContainer.queueStore.shuffleQueue() }) {
                            Icon(Icons.Filled.Shuffle, "Shuffle")
                        }
                        TextButton(onClick = { AppContainer.queueStore.clearQueue() }) {
                            Text("Clear")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (queueEntries.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Queue is empty", style = MaterialTheme.typography.titleMedium)
                    Text("Long-press a video → Play next / Add to queue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(queueEntries) { index, (q, entry) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.video.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                    Text(entry.video.uri, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row {
                                IconButton(onClick = { onPlay(entry.video.id) }) {
                                    Icon(Icons.Filled.PlayArrow, "Play")
                                }
                                IconButton(onClick = { AppContainer.queueStore.removeFromQueue(entry.video.id) }) {
                                    Icon(Icons.Filled.Delete, "Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
