package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var trashEntries by remember { mutableStateOf(AppContainer.trashStore.getAll()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        trashEntries = AppContainer.trashStore.getAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash - Recently Deleted") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (trashEntries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("Empty Trash")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (trashEntries.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("Trash is empty", style = MaterialTheme.typography.titleMedium)
                    Text("Deleted videos will appear here for 30 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${trashEntries.size} items in trash", style = MaterialTheme.typography.titleSmall)
                            Text("Total: ${com.opticast.player.data.StorageAnalyzer.formatSize(trashEntries.sumOf { it.size })} • Auto-deleted after 30 days", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(trashEntries.sortedByDescending { it.deletedAt }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                                Text("Deleted ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.deletedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(entry.originalPath, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row {
                                IconButton(onClick = {
                                    scope.launch {
                                        if (AppContainer.trashStore.restore(entry)) {
                                            trashEntries = AppContainer.trashStore.getAll()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Restore, "Restore")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        if (AppContainer.trashStore.deletePermanently(entry)) {
                                            trashEntries = AppContainer.trashStore.getAll()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, "Delete permanently")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Empty Trash?") },
                text = { Text("Permanently delete ${trashEntries.size} items? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            trashEntries.forEach { AppContainer.trashStore.deletePermanently(it) }
                            trashEntries = emptyList()
                            showClearConfirm = false
                        }
                    }) {
                        Text("Delete All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
