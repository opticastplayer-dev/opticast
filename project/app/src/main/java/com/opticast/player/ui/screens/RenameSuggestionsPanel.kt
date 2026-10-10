package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry

@Composable
internal fun RenameSuggestionsPanel(visible: Boolean, entries: List<LibraryEntry>, onClose: () -> Unit, onIdentify: (Long) -> Unit, onChanged: () -> Unit) {
    val store = AppContainer.renameSuggestions
    val version by store.version.collectAsStateWithLifecycle()
    val suggestions = remember(entries, version) { store.suggestions(entries, false) }

    if (visible) Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)) {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.86f).dp).padding(18.dp)) {
                Text("Rename suggestions", style = MaterialTheme.typography.titleLarge)
                Text("Review before renaming. No file is renamed automatically.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (suggestions.isEmpty()) item { Text("No changes needed.", Modifier.padding(vertical = 20.dp)) }
                    items(suggestions, key = { it.entry.video.id }) { suggestion ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(suggestion.entry.video.name, style = MaterialTheme.typography.bodyMedium)
                                suggestion.proposedName?.let {
                                    Text("→ $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                                Row {
                                    TextButton(onClick = { onIdentify(suggestion.entry.video.id) }) { Text("Identify") }
                                    TextButton(onClick = { store.dismiss(suggestion); onChanged() }) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
