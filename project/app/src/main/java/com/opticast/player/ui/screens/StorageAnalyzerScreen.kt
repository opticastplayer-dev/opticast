package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opticast.player.data.StorageAnalyzer
import com.opticast.player.data.model.LibraryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(
    entries: List<LibraryEntry>,
    onBack: () -> Unit
) {
    val stats = remember(entries) { StorageAnalyzer.analyze(entries) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analyzer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Storage Overview", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Movies: ${entries.count { !it.video.isEpisode }} files, ${StorageAnalyzer.formatSize(stats.totalMoviesSize)}")
                        Text("TV Shows: ${entries.count { it.video.isEpisode }} files, ${StorageAnalyzer.formatSize(stats.totalShowsSize)}")
                        Text("Total: ${StorageAnalyzer.formatSize(stats.totalSize)}")
                        if (stats.totalSpace > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("Device: ${StorageAnalyzer.formatSize(stats.availableSpace)} free / ${StorageAnalyzer.formatSize(stats.totalSpace)} total")
                            LinearProgressIndicator(
                                progress = { (stats.totalSpace - stats.availableSpace).toFloat() / stats.totalSpace.toFloat() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            item {
                Text("Biggest Files", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(stats.biggestFiles) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.video.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                            Text(entry.video.uri, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(StorageAnalyzer.formatSize(entry.video.sizeBytes), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
