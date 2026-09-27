package com.opticast.player.ui.screens

import android.app.Activity
import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.opticast.player.data.local.renameValidationError
import com.opticast.player.data.local.batchRenameItems
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.opticast.player.data.model.LibraryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Always composed: Android's write-consent result survives closing the review panel. */
@Composable
internal fun RenameSuggestionsPanel(visible: Boolean, entries: List<LibraryEntry>, onClose: () -> Unit,
    onIdentify: (Long) -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val store = AppContainer.renameSuggestions
    val version by store.version.collectAsStateWithLifecycle()
    var includeDismissed by rememberSaveable { mutableStateOf(false) }
    val suggestions = remember(entries, version, includeDismissed) { store.suggestions(entries, includeDismissed) }
    var confirmAll by rememberSaveable { mutableStateOf(false) }
    var batchJson by rememberSaveable { mutableStateOf<String?>(null) }
    val batchItems = remember(suggestions) { batchRenameItems(suggestions) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var original by rememberSaveable { mutableStateOf("") }
    var proposed by rememberSaveable { mutableStateOf("") }
    var pendingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var consentGiven by rememberSaveable { mutableStateOf(false) }
    var consentRequested by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) { consentGiven = true; retry++ }
        else { pendingId = null; consentRequested = false; message = "Rename cancelled. The filename was not changed." }
    }
    LaunchedEffect(pendingId, retry) {
        val id = pendingId ?: return@LaunchedEffect
        try {
            val video = withContext(Dispatchers.IO) { store.validate(id, original, proposed) }
            if (Build.VERSION.SDK_INT >= 30 && !consentGiven) {
                if (!consentRequested) {
                    consentRequested = true
                    consent.launch(IntentSenderRequest.Builder(MediaStore.createWriteRequest(context.contentResolver, listOf(Uri.parse(video.uri))).intentSender).build())
                }
                return@LaunchedEffect
            }
            try {
                withContext(Dispatchers.IO) { store.rename(id, original, proposed) }
                message = "Renamed to $proposed. Saved progress and metadata were kept."
                pendingId = null; consentRequested = false; onChanged()
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT == 29 && e is RecoverableSecurityException && !consentGiven) {
                    if (!consentRequested) {
                        consentRequested = true
                        consent.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                    }
                } else throw e
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            message = e.message ?: "Could not rename this file. Recheck storage to review its current name."
            pendingId = null; consentRequested = false; onChanged()
        }
    }
    if (visible) Dialog(onDismissRequest = { if (pendingId == null && batchJson == null) onClose() }) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)) {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.86f).dp).padding(18.dp)) {
                Text("Rename suggestions", style = MaterialTheme.typography.titleLarge)
                Text("Review each suggestion before changing the actual file. No video is renamed automatically. Your file extension and folder stay unchanged.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                Row {
                    Checkbox(checked = includeDismissed, onCheckedChange = { includeDismissed = it }, enabled = pendingId == null && batchJson == null)
                    Text("Include dismissed", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.labelMedium)
                }
                Button(enabled = batchItems.isNotEmpty() && pendingId == null && batchJson == null,
                    onClick = { confirmAll = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rename all (${batchItems.size})")
                }
                if (pendingId != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodySmall) }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (suggestions.isEmpty()) item { Text("No filename changes need review.", Modifier.padding(vertical = 20.dp)) }
                    items(suggestions, key = { it.entry.video.id }) { suggestion ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                Text("Original", style = MaterialTheme.typography.labelSmall)
                                Text(suggestion.entry.video.name, style = MaterialTheme.typography.bodyMedium)
                                if (suggestion.proposedName != null) {
                                    Text("Suggested", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(suggestion.proposedName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(suggestion.reason, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
                                Row {
                                    TextButton(enabled = pendingId == null && batchJson == null, onClick = {
                                        editId = suggestion.entry.video.id; original = suggestion.entry.video.name
                                        proposed = suggestion.proposedName ?: suggestion.entry.video.name
                                    }) { Icon(Icons.Filled.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Review name") }
                                    TextButton(enabled = pendingId == null && batchJson == null, onClick = { onIdentify(suggestion.entry.video.id) }) { Text("Identify title") }
                                }
                                TextButton(enabled = pendingId == null && batchJson == null, onClick = { store.dismiss(suggestion) }) { Text("Dismiss suggestion") }
                            }
                        }
                    }
                }
                TextButton(enabled = pendingId == null && batchJson == null, onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
    if (confirmAll) AlertDialog(onDismissRequest = { confirmAll = false },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        title = { Text("Rename all ${batchItems.size} files?") },
        text = { Text("Apply the suggested names to all ${batchItems.size} eligible files in this review list, not just the cards on screen. " +
            "Files that still need identification are excluded. Conflicts and unavailable files will be skipped. " +
            "Android may ask for permission for the batch (individually on Android 10). " +
            "Already completed renames are not undone if you stop. File extensions, folders and saved progress are preserved; external subtitle files are not renamed.") },
        confirmButton = { TextButton(enabled = batchItems.isNotEmpty(), onClick = {
            confirmAll = false; message = null; batchJson = Json.encodeToString(batchItems)
        }) { Text("Rename all") } },
        dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } })
    batchJson?.let { snapshot ->
        BatchRenameDialog(snapshot, onDone = { result -> batchJson = null; message = result; onChanged() })
    }

    editId?.let { id ->
        val error = renameValidationError(original, proposed)
        AlertDialog(onDismissRequest = { editId = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            title = { Text("Rename this video?") },
            text = { Column {
                Text("Original: $original", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = proposed, onValueChange = { proposed = it }, label = { Text("New filename") }, isError = error != null, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Only the video filename changes. External subtitle files are not renamed. Android may ask you to allow changes.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            } },
            confirmButton = { TextButton(enabled = error == null && proposed != original && pendingId == null && batchJson == null, onClick = {
                editId = null; message = null; consentGiven = false; consentRequested = false; pendingId = id
            }) { Text("Rename file") } },
            dismissButton = { TextButton(onClick = { editId = null }) { Text("Cancel") } })
    }
}
