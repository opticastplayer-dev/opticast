package com.opticast.player.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opticast.player.data.remote.UpdateChecker
import kotlinx.coroutines.launch

@Composable
internal fun UpdateCheckOption(version: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var show by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("App updates", style = MaterialTheme.typography.titleMedium)
        Text(version, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    checking = true
                    error = null
                    try {
                        val info = UpdateChecker.checkForUpdate(context, force = true)
                        if (info == null) error = "No update info. Check internet."
                        else {
                            updateInfo = info
                            show = info.isNewer
                            if (!info.isNewer) error = "You have the latest version."
                        }
                    } catch (e: Exception) {
                        error = "Check failed: ${e.message}"
                    } finally { checking = false }
                }
            }, enabled = !checking) {
                Text(if (checking) "Checking..." else "Check for updates")
            }
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/opticastplayer-dev/opticast/releases"))) }
            }) { Text("Releases") }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }

    if (show && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Update available: ${updateInfo!!.version}") },
            text = { Text(updateInfo!!.changelog.take(500)) },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    scope.launch { UpdateChecker.downloadAndInstall(context, updateInfo!!.downloadUrl) }
                }) { Text("Download") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Later") } }
        )
    }
}

@Composable
internal fun WhatsNewDialog() {}

@Composable
internal fun AutoUpdateDialog() {}

@Composable
internal fun GlobalUpdateInstallDialog() {}
