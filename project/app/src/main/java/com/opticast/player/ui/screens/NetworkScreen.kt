package com.opticast.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.NetworkSource
import com.opticast.player.data.remote.RemoteAccessException
import com.opticast.player.data.remote.RemoteEntry
import com.opticast.player.data.remote.RemoteFileSystem
import com.opticast.player.player.RemoteDownloader
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Network libraries: browse and stream from an SMB share, WebDAV server or plain
 * HTTP folder, and download a file for offline watching.
 *
 * Deliberately a separate screen rather than a new tab: the library's pill
 * navigation and header are frozen, and this way nothing about the validated
 * library layout changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    onBack: () -> Unit,
    onPlayRemote: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sources by AppContainer.networkSources.sources.collectAsStateWithLifecycle()

    var openSource by remember { mutableStateOf<NetworkSource?>(null) }
    var editing by remember { mutableStateOf<NetworkSource?>(null) }
    var adding by remember { mutableStateOf(false) }
    var downloads by remember { mutableStateOf(RemoteDownloader.downloaded(context)) }
    var busyDownload by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var message by remember { mutableStateOf<String?>(null) }

    val current = openSource
    if (current != null) {
        BrowserView(
            source = current,
            onBack = { openSource = null },
            onPlayRemote = onPlayRemote,
            onDownload = { entry ->
                scope.launch {
                    busyDownload = entry.name
                    progress = 0f
                    message = null
                    runCatching {
                        withContext(Dispatchers.IO) {
                            RemoteDownloader.download(
                                context = context,
                                source = current,
                                remotePath = entry.path,
                                fileName = entry.name,
                                onProgress = { progress = it },
                            )
                        }
                    }.onSuccess {
                        downloads = RemoteDownloader.downloaded(context)
                        message = "Saved “${entry.name}” for offline playback."
                    }.onFailure {
                        message = it.message ?: "Download failed."
                    }
                    busyDownload = null
                }
            },
            busyDownload = busyDownload,
            progress = progress,
            message = message,
            onDismissMessage = { message = null },
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Network libraries",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add library")
                }
            }
        }

        item {
            Text(
                "Stream straight from a PC, NAS or router USB drive over Wi-Fi - " +
                    "no mobile data used - or download a file to keep it offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        if (sources.isEmpty()) {
            item {
                Column(Modifier.padding(20.dp)) {
                    Text("No libraries yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add an SMB (Windows share / NAS) location, a WebDAV server " +
                            "(Nextcloud, Synology) or an HTTP folder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { adding = true }, shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add a library")
                    }
                }
            }
        }

        items(sources, key = { it.id }) { source ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(20.dp))
                    .clickable { openSource = source }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(source.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOfNotNull(
                            source.typeLabel,
                            source.host,
                            source.share.ifBlank { source.path.ifBlank { null } },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { editing = source }) {
                    Icon(
                        Icons.Filled.Movie,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { AppContainer.networkSources.remove(source.id) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (downloads.isNotEmpty()) {
            item {
                Text(
                    "Downloaded for offline",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
                )
            }
            items(downloads, key = { it.absolutePath }) { file ->
                DownloadedRow(
                    file = file,
                    onPlay = { onPlayRemote(file.toURI().toString(), file.name) },
                    onDelete = {
                        RemoteDownloader.delete(file)
                        downloads = RemoteDownloader.downloaded(context)
                    },
                )
            }
        }
    }

    if (adding) {
        SourceEditorDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { source ->
                AppContainer.networkSources.save(source)
                adding = false
            },
        )
    }

    editing?.let { source ->
        SourceEditorDialog(
            initial = source,
            onDismiss = { editing = null },
            onSave = { updated ->
                AppContainer.networkSources.save(updated)
                editing = null
            },
        )
    }
}

@Composable
private fun DownloadedRow(file: File, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(18.dp))
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "%.0f MB · offline".format(file.length() / 1_048_576.0),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete download", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun BrowserView(
    source: NetworkSource,
    onBack: () -> Unit,
    onPlayRemote: (String, String) -> Unit,
    onDownload: (RemoteEntry) -> Unit,
    busyDownload: String?,
    progress: Float,
    message: String?,
    onDismissMessage: () -> Unit,
) {
    var path by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<RemoteEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(source.id, path) {
        loading = true
        errorState = null
        runCatching { RemoteFileSystem.list(source, path) }
            .onSuccess { entries = it }
            .onFailure {
                entries = emptyList()
                errorState = (it as? RemoteAccessException)?.message ?: it.message ?: "Could not open the folder."
            }
        loading = false
    }

    val error = errorState
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { if (path.isEmpty()) onBack() else path = path.substringBeforeLast('/', "") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up")
            }
            Column(Modifier.weight(1f)) {
                Text(source.label, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(
                    if (path.isEmpty()) source.typeLabel else path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        busyDownload?.let {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text("Downloading $it…", style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        message?.let {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismissMessage) { Text("OK") }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            error != null -> Column(Modifier.padding(20.dp)) {
                Text("Could not connect", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Check the address, the share name and the username/password in the " +
                        "library settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            entries.isEmpty() -> Text(
                "This folder has no videos or sub-folders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )

            else -> LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                items(entries, key = { it.path }) { entry ->
                    RemoteRow(
                        entry = entry,
                        onOpen = {
                            if (entry.isDirectory) path = entry.path
                            else onPlayRemote(source.uriFor(entry.path), entry.name)
                        },
                        onDownload = { onDownload(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteRow(entry: RemoteEntry, onOpen: () -> Unit, onDownload: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .clickable { onOpen() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.isDirectory) Icons.Filled.Folder else Icons.Filled.Movie,
            contentDescription = null,
            tint = if (entry.isDirectory) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!entry.isDirectory && entry.sizeBytes > 0) {
                Text(
                    "%.0f MB".format(entry.sizeBytes / 1_048_576.0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (entry.isVideo) {
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, contentDescription = "Download for offline")
            }
        }
    }
}

@Composable
internal fun SourceEditorDialog(
    initial: NetworkSource?,
    onDismiss: () -> Unit,
    onSave: (NetworkSource) -> Unit,
) {
    val isNew = initial == null || AppContainer.networkSources.find(initial.id) == null
    var label by remember { mutableStateOf(initial?.label.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: "smb") }
    var host by remember { mutableStateOf(initial?.host.orEmpty()) }
    var share by remember { mutableStateOf(initial?.share.orEmpty()) }
    var path by remember { mutableStateOf(initial?.path.orEmpty()) }
    var port by remember { mutableStateOf(initial?.port?.takeIf { it > 0 }?.toString().orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var domain by remember { mutableStateOf(initial?.domain.orEmpty()) }
    var useTls by remember { mutableStateOf(initial?.useTls == true || initial?.port == 443) }

    AlertDialog(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), 
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add network library" else "Edit library") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    listOf("smb" to "SMB", "webdav" to "WebDAV", "http" to "HTTP").forEach { (id, name) ->
                        FilterChip(
                            selected = type == id,
                            onClick = { type = id },
                            label = { Text(name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name (e.g. Home server)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Address (e.g. 192.168.1.10)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == "smb") {
                    OutlinedTextField(
                        value = share,
                        onValueChange = { share = it },
                        label = { Text("Share name (e.g. Movies)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = domain,
                        onValueChange = { domain = it },
                        label = { Text("Domain (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text(if (type == "smb") "Starting folder (optional)" else "Path (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text(if (type == "smb") "Port (445 default)" else "Port (80 default)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type != "smb") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(checked = useTls || port == "443", enabled = port != "443", onCheckedChange = { useTls = it })
                        Text("Use HTTPS / TLS", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (optional)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Stored only on this device, in the app's private storage.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank() && host.isNotBlank(),
                onClick = {
                    onSave(
                        (initial ?: NetworkSource(
                            id = UUID.randomUUID().toString(),
                            label = label,
                            type = type,
                            host = host,
                        )).copy(
                            label = label.trim(),
                            type = type,
                            host = host.trim(),
                            share = share.trim().trim('/'),
                            path = path.trim().trim('/'),
                            port = port.toIntOrNull() ?: 0,
                            username = username.trim(),
                            password = password,
                            domain = domain.trim(),
                            useTls = type != "smb" && useTls,
                        ),
                    )
                },
            ) { Text(if (isNew) "Add" else "Save") }
        },
        dismissButton = {
            FilledTonalButton(onClick = onDismiss, shape = RoundedCornerShape(18.dp)) { Text("Cancel") }
        },
    )
}
