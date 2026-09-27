package com.opticast.player.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.opticast.player.data.ProviderNotices
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
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    var autoCheckEnabled by remember { mutableStateOf(UpdateChecker.isAutoCheckEnabled(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("App updates", style = MaterialTheme.typography.titleMedium)
        Text(
            "Checks GitHub releases. Downloads and installs within the app when possible. Background auto check for updates on app startup is now allowed and enabled by default (checks every 6h in background).",
            style = MaterialTheme.typography.bodySmall
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Background auto check on startup", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = autoCheckEnabled,
                onCheckedChange = { enabled ->
                    autoCheckEnabled = enabled
                    UpdateChecker.setAutoCheckEnabled(context, enabled)
                    if (enabled) {
                        scope.launch {
                            try {
                                UpdateChecker.checkAtStartup(context)
                            } catch (_: Exception) { }
                        }
                    }
                }
            )
        }
        Text(
            if (autoCheckEnabled) "✅ Auto check enabled — will check in background on every app startup (every 6h)" else "❌ Auto check disabled — only manual checks",
            style = MaterialTheme.typography.bodySmall,
            color = if (autoCheckEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        checking = true
                        error = null
                        try {
                            val info = UpdateChecker.checkForUpdate(context, force = true)
                            if (info == null) {
                                error = "No update information available. Check your internet or open releases page."
                            } else {
                                updateInfo = info
                                show = true
                            }
                        } catch (e: Exception) {
                            error = "Failed to check: ${e.message}"
                        } finally {
                            checking = false
                        }
                    }
                },
                enabled = !checking && !downloading
            ) {
                Text(if (checking) "Checking..." else "Check for updates")
            }
            OutlinedButton(onClick = { UpdateChecker.openReleasesPage(context) }) {
                Text("Open GitHub")
            }
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (downloading) {
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
            Text("Downloading: $progress%", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (show && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            onDismissRequest = { show = false },
            title = { Text(if (info.isNewer) "Update available: ${info.version}" else "Up to date") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Installed: $version")
                    Text("Latest: ${info.version} (code ${info.versionCode})")
                    if (info.changelog.isNotBlank()) {
                        Text("What's new:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            info.changelog.take(800),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 10
                        )
                    }
                    if (info.size > 0) {
                        Text("Size: ${info.size / 1024 / 1024} MB", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (info.isNewer) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                downloading = true
                                progress = 0
                                show = false
                                val success = UpdateChecker.downloadAndInstall(
                                    context,
                                    info.downloadUrl
                                ) { p -> progress = p }
                                downloading = false
                                if (!success) {
                                    UpdateChecker.openReleasePage(context, info.htmlUrl)
                                }
                            }
                        }
                    ) { Text("Download & Install") }
                } else {
                    TextButton(onClick = { show = false }) { Text("OK") }
                }
            },
            dismissButton = {
                Row {
                    if (info.isNewer) {
                        TextButton(onClick = {
                            UpdateChecker.skipVersion(context, info.version)
                            show = false
                        }) { Text("Skip") }
                    }
                    TextButton(onClick = {
                        UpdateChecker.openReleasePage(context, info.htmlUrl)
                    }) { Text("Open") }
                    TextButton(onClick = { show = false }) { Text("Close") }
                }
            }
        )
    }
}

@Composable
internal fun ProviderCredits() {
    var expanded by remember { mutableStateOf(false) }
    val uri = LocalUriHandler.current
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Hide provider credits and legal notices" else "Provider credits and legal notices")
    }
    if (expanded) {
        Text("Independent app; no provider endorsement. Names, artwork and subtitles remain subject to their owners' rights. These notices do not grant rights to redistribute movies or provider content.",
            style = MaterialTheme.typography.bodySmall)
        ProviderNotices.entries.forEach { provider ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(provider.name, style = MaterialTheme.typography.titleSmall)
                    Text(provider.description, style = MaterialTheme.typography.bodyMedium)
                    Text(provider.notice, style = MaterialTheme.typography.bodySmall)
                    provider.links.forEach { (label, url) ->
                        TextButton(onClick = { runCatching { uri.openUri(url) } }) { Text(label) }
                    }
                }
            }
        }
        Text("Notices are stored offline. Opening provider/licence links uses the internet. Optional provider queries are sent directly to their services. Library and playback data are stored locally; Android backup and user exports may copy app data.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun WhatsNewDialog() {
    val context = LocalContext.current
    var show by remember { mutableStateOf(UpdateChecker.shouldShowWhatsNew(context)) }
    val version = UpdateChecker.getWhatsNewVersion(context) ?: return

    if (show) {
        val changelog = when {
            version.contains("2.6.60") -> """
                • Signed release 56M with mpv — plays all videos
                • Background auto check for updates on app startup — now allowed and enabled by default
                • Auto update dialog shows when new version available on startup
                • Changed wording: official signed full mpv → signed release
                • Fixed YAML syntax error in release workflow (block style)
                • Secrets correctly implemented — restore signing key success
            """.trimIndent()
            version.contains("2.6.59") -> """
                • Fixed PiP: expanding PiP now auto-resumes playback on 32-bit devices
                • Removed WhatsApp contact
                • In-app updates: check, download and install from GitHub
                • Automatic update check at startup (once per day)
                • What's New dialog shows changelog after update
                • Added GitHub and F-Droid distribution support
            """.trimIndent()
            version.contains("2.6.58") -> """
                • Removed all home-screen widgets for performance
                • Faster startup: no blocking on main thread
                • Lower memory: 6/16 MiB image cache, 64/192 MiB disk
                • Smoother scrolling: lock-free poster cache, limited prefetch
                • Baseline profiles for faster ART compilation
                • R8 fullMode optimizations
            """.trimIndent()
            else -> "Updated to $version with performance improvements and bug fixes."
        }

        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            onDismissRequest = {
                UpdateChecker.dismissWhatsNew(context)
                show = false
            },
            title = { Text("What's New in $version") },
            text = {
                Column {
                    Text(changelog, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    UpdateChecker.dismissWhatsNew(context)
                    show = false
                }) { Text("Got it") }
            },
            dismissButton = {
                TextButton(onClick = {
                    UpdateChecker.openReleasesPage(context)
                }) { Text("Open Releases") }
            }
        )
    }
}

@Composable
internal fun AutoUpdateDialog() {
    val context = LocalContext.current
    var show by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Background auto check for updates on app startup - allowed
    LaunchedEffect(Unit) {
        // Delay 3s to let app start, then check if update available from background auto check
        kotlinx.coroutines.delay(3000)
        if (!UpdateChecker.isAutoCheckEnabled(context)) return@LaunchedEffect
        try {
            val info = UpdateChecker.getAvailableUpdateInfo(context)
            if (info != null && info.isNewer && !UpdateChecker.isSkipped(context, info.version)) {
                updateInfo = info
                show = true
            } else {
                // If no stored info, try fresh check in background (respects 6h interval)
                val fresh = UpdateChecker.checkForUpdate(context, force = false)
                if (fresh != null && fresh.isNewer && !UpdateChecker.isSkipped(context, fresh.version)) {
                    updateInfo = fresh
                    show = true
                }
            }
        } catch (_: Exception) { }
    }

    if (show && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            onDismissRequest = { show = false },
            title = { Text("Update available: ${info.version}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A new signed release is available!", style = MaterialTheme.typography.titleSmall)
                    Text("Installed: ${UpdateChecker.getInstalledVersion(context).first}", style = MaterialTheme.typography.bodySmall)
                    Text("Latest: ${info.version}", style = MaterialTheme.typography.bodyMedium)
                    if (info.changelog.isNotBlank()) {
                        Text("What's new:", style = MaterialTheme.typography.titleSmall)
                        Text(info.changelog.take(600), style = MaterialTheme.typography.bodySmall, maxLines = 8)
                    }
                    if (info.size > 0) {
                        Text("Size: ${info.size / 1024 / 1024} MB", style = MaterialTheme.typography.bodySmall)
                    }
                    if (downloading) {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("Downloading: $progress%", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Background auto check enabled — checks on every startup (every 6h)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            downloading = true
                            progress = 0
                            val success = UpdateChecker.downloadAndInstall(context, info.downloadUrl) { p -> progress = p }
                            downloading = false
                            if (success) {
                                show = false
                            } else {
                                UpdateChecker.openReleasePage(context, info.htmlUrl)
                            }
                        }
                    },
                    enabled = !downloading
                ) { Text("Download & Install") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        UpdateChecker.skipVersion(context, info.version)
                        show = false
                    }) { Text("Skip") }
                    TextButton(onClick = {
                        UpdateChecker.clearAvailableUpdate(context)
                        show = false
                    }) { Text("Later") }
                }
            }
        )
    }
}
