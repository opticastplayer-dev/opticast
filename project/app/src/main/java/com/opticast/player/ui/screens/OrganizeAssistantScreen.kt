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
import com.opticast.player.data.AppContainer
import com.opticast.player.data.local.FileSuggestion
import com.opticast.player.data.model.LibraryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeAssistantScreen(
    entries: List<LibraryEntry>,
    onBack: () -> Unit
) {
    var report by remember { mutableStateOf<com.opticast.player.data.local.FileHealthReport?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var selectedSuggestions by remember { mutableStateOf(setOf<String>()) }
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organize Assistant - Beta") },
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
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Safe Mode - Suggest Only", style = MaterialTheme.typography.titleSmall)
                        Text("Never auto-renames. Only shows suggestions. You check what you want. All through Trash with Undo. Virtual organization by default.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isScanning = true
                        report = AppContainer.organizeAssistant.scanLibrary(entries)
                        isScanning = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (report == null) "Scan Library - Health Report" else "Rescan Library")
                }
            }

            report?.let { r ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Health Report - ${r.totalIssues} issues", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Messy names: ${r.messyNames.size}")
                            Text("Misplaced movies: ${r.misplacedMovies.size}")
                            Text("Misplaced shows: ${r.misplacedShows.size}")
                            Text("Empty folders: ${r.emptyFolders.size}")
                        }
                    }
                }

                if (r.messyNames.isNotEmpty()) {
                    item { Text("Messy Names - Rename in same folder", style = MaterialTheme.typography.titleSmall) }
                    items(r.messyNames.take(20)) { suggestion ->
                        SuggestionCard(suggestion, selectedSuggestions.contains(suggestion.id), onChecked = { checked ->
                            selectedSuggestions = if (checked) selectedSuggestions + suggestion.id else selectedSuggestions - suggestion.id
                        })
                    }
                }

                if (r.misplacedMovies.isNotEmpty()) {
                    item { Text("Misplaced Movies - Move to Movies folder", style = MaterialTheme.typography.titleSmall) }
                    items(r.misplacedMovies.take(20)) { suggestion ->
                        SuggestionCard(suggestion, selectedSuggestions.contains(suggestion.id), onChecked = { checked ->
                            selectedSuggestions = if (checked) selectedSuggestions + suggestion.id else selectedSuggestions - suggestion.id
                        })
                    }
                }

                if (r.misplacedShows.isNotEmpty()) {
                    item { Text("Misplaced Shows - Move to TV Shows", style = MaterialTheme.typography.titleSmall) }
                    items(r.misplacedShows.take(20)) { suggestion ->
                        SuggestionCard(suggestion, selectedSuggestions.contains(suggestion.id), onChecked = { checked ->
                            selectedSuggestions = if (checked) selectedSuggestions + suggestion.id else selectedSuggestions - suggestion.id
                        })
                    }
                }

                if (selectedSuggestions.isNotEmpty()) {
                    item {
                        Button(
                            onClick = { showPreview = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Preview ${selectedSuggestions.size} changes")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Safety Levels", style = MaterialTheme.typography.titleSmall)
                        Text("Level 1 - Suggest Only (Default): Only show suggestions, no file ops", style = MaterialTheme.typography.bodySmall)
                        Text("Level 2 - Rename in Same Folder Only: Clean filename in same folder", style = MaterialTheme.typography.bodySmall)
                        Text("Level 3 - Allow Organized Moves (Advanced): Move to Movies/TV Shows, requires typing MOVE", style = MaterialTheme.typography.bodySmall)
                        Text("All operations: Copy-Verify-Trash, log + undo, one-by-one with progress, cancel anytime", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (showPreview) {
            AlertDialog(
                onDismissRequest = { showPreview = false },
                title = { Text("Preview ${selectedSuggestions.size} changes") },
                text = {
                    Column {
                        Text("Dry run - no changes yet")
                        Spacer(Modifier.height(8.dp))
                        Text("Selected operations will be: Copy → Verify size → Move old to Trash → Log + Undo available")
                        Spacer(Modifier.height(8.dp))
                        Text("This is beta behind flag organizeAssistantEnabled. Enable in Settings → Tools to allow actual file operations.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPreview = false }) {
                        Text("Close Preview")
                    }
                }
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: FileSuggestion,
    isChecked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = isChecked, onCheckedChange = onChecked)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Before: ${suggestion.beforePath}", maxLines = 2, style = MaterialTheme.typography.bodySmall)
                    Text("After: ${suggestion.afterPath}", maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Reason: ${suggestion.reason} • ${com.opticast.player.data.StorageAnalyzer.formatSize(suggestion.size)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
