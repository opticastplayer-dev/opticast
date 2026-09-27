package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.NetworkSource
import com.opticast.player.data.remote.NetworkLibraryTools
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun NetworkToolsPanel(onOpenNetwork: () -> Unit) {
    val context = LocalContext.current
    val tools = remember { NetworkLibraryTools(context) }
    val scope = rememberCoroutineScope()
    val sources by AppContainer.networkSources.sources.collectAsStateWithLifecycle()
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val results = remember { mutableStateMapOf<String, String>() }
    val discovered = remember { mutableStateListOf<NetworkSource>() }
    var configure by remember { mutableStateOf<NetworkSource?>(null) }

    Text("Check saved servers or find advertised SMB, WebDAV and HTTP services on your local network. Runs only when requested; no background scanning.",
        style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
    OutlinedButton(enabled = !busy, onClick = {
        val snapshot = sources.toList()
        results.clear()
        if (snapshot.isEmpty()) status = "No saved libraries. Discover services or open network libraries to add one."
        else {
            busy = true
            status = "Checking saved libraries…"
            job = scope.launch {
                try {
                    for (source in snapshot) results[source.id] = "${source.label}: ${tools.check(source)}"
                    status = "Saved-library checks finished."
                } finally { busy = false }
            }
        }
    }) { Text("Check saved libraries") }
    OutlinedButton(enabled = !busy, onClick = {
        discovered.clear()
        status = "Looking for advertised services for 12 seconds…"
        busy = true
        job = scope.launch {
            try {
                tools.discover(onFound = { discovered.add(it) }, onStatus = { status = it })
                status = if (discovered.isEmpty()) "No advertised services found. A server may still be available—add its address manually."
                    else "Found ${discovered.size} advertised service(s). Configure the path/share and credentials before browsing."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { status = "Discovery could not run. Check your network or add a library manually." }
            finally { busy = false }
        }
    }) { Text("Find libraries on my network") }
    if (busy) TextButton(onClick = { job?.cancel(); status = "Check cancelled." }) { Text("Cancel check") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)) }
    results.values.forEach { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp)) }
    discovered.forEach { source ->
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(source.label, style = MaterialTheme.typography.titleSmall)
                Text("${source.typeLabel} · ${source.host}:${source.port}", style = MaterialTheme.typography.bodySmall)
                if (source.type == "http") Text("Advertised web service; a video folder is not guaranteed.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { configure = source }) { Text("Configure library") }
            }
        }
    }
    Text("Discovery is limited to services your network advertises. Guest Wi-Fi, routers and server settings may block discovery. HTTP is unencrypted; prefer HTTPS for passwords.",
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    OutlinedButton(onClick = onOpenNetwork) { Text("Open network libraries") }
    configure?.let { source ->
        SourceEditorDialog(initial = source, onDismiss = { configure = null }, onSave = {
            AppContainer.networkSources.save(it)
            configure = null
            status = "Library saved. Use Open network libraries to browse."
        })
    }
}
