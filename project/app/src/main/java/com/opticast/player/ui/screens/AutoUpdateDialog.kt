package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opticast.player.data.remote.UpdateChecker
import kotlinx.coroutines.launch

@Composable
internal fun AutoUpdateDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateInfo by UpdateChecker.availableUpdate.collectAsState()
    val downloading by UpdateChecker.isDownloading.collectAsState()
    val progress by UpdateChecker.downloadProgress.collectAsState()
    var dismissedVersion by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    val update = updateInfo?.takeIf { it.isNewer && it.version != dismissedVersion }
    if (update != null) {
        AlertDialog(
            onDismissRequest = { dismissedVersion = update.version },
            title = { Text("Update available: v${update.version}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(update.changelog.ifBlank { "A newer version of OptiCast is available." }.take(700))
                    if (update.size > 0) Text(
                        "Download size: ${"%.1f".format(update.size / (1024.0 * 1024.0))} MB",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (downloading) {
                        LinearProgressIndicator(
                            progress = { (progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Downloading… $progress%", style = MaterialTheme.typography.bodySmall)
                    }
                    downloadError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !downloading, onClick = {
                    downloadError = null
                    scope.launch {
                        val started = UpdateChecker.downloadAndInstall(context, update.downloadUrl)
                        if (started) dismissedVersion = update.version
                        else downloadError = "Could not finish the download or install. Check your connection and install permission, then try again."
                    }
                }) { Text(if (downloading) "Downloading…" else "Download and install") }
            },
            dismissButton = {
                TextButton(onClick = { dismissedVersion = update.version }) { Text("Later") }
            }
        )
    }
}
