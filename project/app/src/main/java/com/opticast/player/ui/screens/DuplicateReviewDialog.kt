package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.local.*
import com.opticast.player.ui.components.formatDuration
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

@Composable
internal fun DuplicateReviewDialog(group: List<LocalVideo>, onDelete: (List<LocalVideo>) -> Unit, onDismiss: () -> Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var hashes by remember { mutableStateOf<Map<Long,String>>(emptyMap()) }
    var errors by remember { mutableStateOf<Map<Long,String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }
    val deletion=deletableDuplicateSelection(group,selected.toSet())
    AlertDialog(onDismissRequest={job?.cancel();onDismiss()},containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=.92f),
        title={Text("Compare possible duplicates")},confirmButton={TextButton(onClick={onDelete(deletion)},enabled=deletion.isNotEmpty() && !busy){Text("Review deletion (${deletion.size})")}},
        dismissButton={TextButton(onClick={job?.cancel();onDismiss()}){Text("Close")}},text={
            Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.7f).dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Same size is only a clue, not proof. Compare locations, quality and progress. Check only files you want to delete; keep at least one. No file is selected automatically.")
                Text("SHA-256 reads every byte locally. Large files can take time and battery. Nothing is uploaded.",style=MaterialTheme.typography.bodySmall)
                if(busy) { LinearProgressIndicator(progress={done.toFloat()/group.size},modifier=Modifier.fillMaxWidth());Text("Verified $done / ${group.size}");TextButton(onClick={job?.cancel()}){Text("Cancel verification")} }
                else TextButton(onClick={
                    busy=true;done=0;hashes=emptyMap();errors=emptyMap()
                    job=scope.launch {
                        try { for(file in group) {
                            try { hashes=hashes+(file.id to verifyLocalFileHash(context,file)) }
                            catch(e:CancellationException){throw e}
                            catch(e:Exception){errors=errors+(file.id to (e.message ?: "Could not verify"))}
                            done++
                        } } finally {busy=false}
                    }
                }){Text("Verify content hashes")}
                group.forEach { video ->
                    HorizontalDivider()
                    Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(video.id in selected,{selected=if(it) selected+video.id else selected-video.id},enabled=!busy);Text("Delete this copy",style=MaterialTheme.typography.labelLarge) }
                    Text(video.name,style=MaterialTheme.typography.titleSmall)
                    Text("Location: ${video.relativePath.ifBlank { "Location not supplied by Android" }}\n${video.width}×${video.height} · ${video.durationMs.formatDuration()} · ${video.sizeBytes} bytes",style=MaterialTheme.typography.bodySmall)
                    val progress=AppContainer.playbackState.progressOf(video.id)
                    Text(if(progress?.isWatched==true) "Watched" else if(progress?.isResumable==true) "Resume at ${progress.positionMs.formatDuration()}" else "Not started",style=MaterialTheme.typography.labelMedium)
                    hashes[video.id]?.let { hash ->
                        val matches=hashes.values.count {it==hash}-1
                        Text(if(matches>0) "Verified identical bytes to $matches other file(s) in this group" else "No matching verified hash yet",style=MaterialTheme.typography.bodySmall)
                        Text("SHA-256: $hash",style=MaterialTheme.typography.labelSmall)
                    }
                    errors[video.id]?.let{Text("Not verified: $it",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
                }
                if(selected.size>=group.size) Text("Keep at least one file. Uncheck the copy you want to retain.",color=MaterialTheme.colorScheme.error)
            }
        })
}
