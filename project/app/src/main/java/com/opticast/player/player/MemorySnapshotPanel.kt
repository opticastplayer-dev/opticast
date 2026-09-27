package com.opticast.player.player

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** No sampling until tapped. One request at a time; no automatic upload or persistent log. */
@Composable
internal fun MemorySnapshotPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<MemorySnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    Column {
        TextButton(enabled = !busy, onClick = {
            busy = true; notice = null; snapshot = null
            scope.launch {
                try {
                    snapshot = OptiCastPlaybackService.captureMemorySnapshot()
                    if (snapshot == null) notice = "Playback changed during capture. Try again."
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { notice = "Memory information is unavailable. Try again." }
                finally { busy = false }
            }
        }) { Text(if (busy) "Capturing…" else "Capture memory snapshot") }
        Text("Captured only when tapped. Nothing is uploaded automatically.", style = MaterialTheme.typography.bodySmall)
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        snapshot?.let { captured ->
            Spacer(Modifier.height(8.dp))
            Text(captured.report(), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"
                runCatching {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "App version: $version\n" + captured.report())
                    }, "Share memory snapshot"))
                }.onFailure { notice = "No sharing app is available." }
            }) { Text("Share snapshot") }
        }
    }
}
