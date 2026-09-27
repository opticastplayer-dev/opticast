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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import com.opticast.player.data.local.BatchRenameItem
import com.opticast.player.data.local.batchRenameConflicts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Snapshot-based job. Saved cursor and permission stage survive screen recreation. */
@Composable
internal fun BatchRenameDialog(planJson: String, onDone: (String) -> Unit) {
    val context = LocalContext.current
    val plans = remember(planJson) { Json.decodeFromString<List<BatchRenameItem>>(planJson) }
    var eligibleJson by rememberSaveable { mutableStateOf("[]") }
    val eligible = remember(eligibleJson) { Json.decodeFromString<List<BatchRenameItem>>(eligibleJson) }
    var stage by rememberSaveable { mutableIntStateOf(0) } // prepare, request, apply, finished
    var cursor by rememberSaveable { mutableIntStateOf(0) }
    var chunkEnd by rememberSaveable { mutableIntStateOf(0) }
    var succeeded by rememberSaveable { mutableIntStateOf(0) }
    var skipped by rememberSaveable { mutableIntStateOf(0) }
    var failed by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf("") }
    var requested by rememberSaveable { mutableStateOf(false) }
    var legacyApprovedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var stop by rememberSaveable { mutableStateOf(false) }
    var cancelled by rememberSaveable { mutableStateOf(false) }
    val store = AppContainer.renameSuggestions
    fun record(text: String) { if (detail.length < 8000) detail += text + "\n" }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        requested = false
        if (result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT == 29) legacyApprovedId = eligible.getOrNull(cursor)?.id
            stage = 2; retry++
        }
        else { cancelled = true; stage = 3 }
    }
    LaunchedEffect(stage, retry) {
        try {
            when (stage) {
                0 -> {
                    // Preflight has no filename side effects; start clean after recreation.
                    succeeded = 0; skipped = 0; failed = 0; detail = ""
                    val conflicts = batchRenameConflicts(plans)
                    val accepted = mutableListOf<BatchRenameItem>()
                    for (item in plans) {
                        if (stop) { cancelled = true; break }
                        if (item.id in conflicts) { skipped++; record("${item.original}: duplicate proposed destination; skipped."); continue }
                        try {
                            val video = withContext(Dispatchers.IO) { store.validate(item.id, item.original, item.proposed) }
                            accepted += item.copy(uri = video.uri)
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { skipped++; record("${item.original}: ${e.message ?: "could not validate"}") }
                    }
                    eligibleJson = Json.encodeToString(accepted)
                    stage = if (stop || accepted.isEmpty()) 3 else 1
                }
                1 -> {
                    if (stop) { cancelled = true; stage = 3; return@LaunchedEffect }
                    chunkEnd = minOf(cursor + 500, eligible.size)
                    if (Build.VERSION.SDK_INT >= 30) {
                        if (!requested) {
                            requested = true
                            consent.launch(IntentSenderRequest.Builder(MediaStore.createWriteRequest(context.contentResolver,
                                eligible.subList(cursor, chunkEnd).map { Uri.parse(it.uri) }).intentSender).build())
                        }
                    } else stage = 2
                }
                2 -> {
                    while (cursor < chunkEnd) {
                        if (stop) { cancelled = true; stage = 3; return@LaunchedEffect }
                        val item = eligible[cursor]
                        try {
                            withContext(Dispatchers.IO) {
                                val current = AppContainer.mediaScanner.byId(item.id)
                                // An interrupted coroutine may have completed the provider update already.
                                if (current?.name != item.proposed) store.rename(item.id, item.original, item.proposed, rescan = false)
                            }
                            succeeded++; cursor++
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) {
                            if (Build.VERSION.SDK_INT == 29 && e is RecoverableSecurityException) {
                                if (legacyApprovedId == item.id) {
                                    failed++; cursor++; record("${item.original}: Android still denied changes after consent.")
                                    continue
                                }
                                if (!requested) {
                                    requested = true
                                    consent.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                                }
                                return@LaunchedEffect
                            }
                            failed++; cursor++; record("${item.original}: ${e.message ?: "rename failed"}")
                        }
                    }
                    stage = if (cursor >= eligible.size) 3 else 1
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { record(e.message ?: "Batch could not continue."); cancelled = true; stage = 3 }
    }
    val remaining = (plans.size - succeeded - skipped - failed).coerceAtLeast(0)
    val summary = "$succeeded renamed · $skipped skipped · $failed failed · $remaining not processed"
    AlertDialog(onDismissRequest = { if (stage == 3) onDone(summary) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        title = { Text(if (stage == 3) "Rename results" else "Renaming files") },
        text = { Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
            if (stage != 3) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(if (stage == 0) "Checking filenames and file identities…" else if (requested) "Waiting for Android permission…"
                    else "Processing ${cursor + 1} of ${eligible.size}", Modifier.padding(vertical = 10.dp))
            }
            Text(summary)
            if (cancelled) Text("Stopped. Files already renamed keep their new names.", Modifier.padding(top = 8.dp))
            if (detail.isNotBlank()) Text(detail, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = {
            if (stage == 3) TextButton(onClick = { onDone(summary) }) { Text("Done") }
            else TextButton(enabled = !stop && !requested, onClick = { stop = true }) { Text(if (stop) "Stopping…" else "Stop after current file") }
        })
}
