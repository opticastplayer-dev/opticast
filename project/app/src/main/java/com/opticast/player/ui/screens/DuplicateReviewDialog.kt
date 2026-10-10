package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.ui.components.formatDuration

@Composable
internal fun DuplicateReviewDialog(group: List<LocalVideo>, onDelete: (List<LocalVideo>) -> Unit, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(listOf<Long>()) }
    val deletion = group.filter { it.id in selected }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = .92f),
        title = { Text("Possible duplicates") },
        confirmButton = { TextButton(onClick = { onDelete(deletion) }, enabled = deletion.isNotEmpty()) { Text("Delete ${deletion.size}") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .7f).dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Same size is a clue, not proof. Check only files you want to delete.", style = MaterialTheme.typography.bodySmall)
                group.forEach { video ->
                    HorizontalDivider()
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(video.id in selected, { selected = if (it) selected + video.id else selected - video.id })
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(video.name, style = MaterialTheme.typography.titleSmall)
                    Text("${video.relativePath.ifBlank { "Unknown" }} • ${video.sizeBytes} bytes • ${video.durationMs.formatDuration()}", style = MaterialTheme.typography.bodySmall)
                }
                if (selected.size >= group.size) Text("Keep at least one.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    )
}
