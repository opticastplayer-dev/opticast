package com.opticast.player.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import com.opticast.player.data.model.LibraryEntry
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import com.opticast.player.data.local.LibraryBackup
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.local.PlaybackStateStore
import com.opticast.player.player.RemoteDownloader
import com.opticast.player.ui.components.formatDuration
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.opticast.player.data.local.FolderExclusions
import com.opticast.player.data.AppSettings

/**
 * Storage dashboard, file organiser and backup.
 *
 * The library is the biggest thing on the phone, so this screen answers the only
 * questions that matter: what is taking the space, what can go, and what is
 * duplicated. Nothing here deletes anything without the system's own confirmation
 * sheet. The organiser uses the shared review and Android write-consent flow.
 */
@Composable
fun StorageScreen(onBack: () -> Unit, onOpenMatch: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var videos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var cacheSizes by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var showOrganiser by rememberSaveable { mutableStateOf(false) }
    val metadataVersion by AppContainer.metadataStore.version.collectAsStateWithLifecycle()
    val renameEntries = remember(videos, metadataVersion) { videos.map { LibraryEntry(it, AppContainer.metadataStore.get(it.id)) } }
    var showRestoreConfirm by remember { mutableStateOf<String?>(null) }
    var restorePreview by remember { mutableStateOf<com.opticast.player.data.local.OptiCastBackup?>(null) }
    var backupTick by remember { mutableIntStateOf(0) }
    var includeSearchHistory by rememberSaveable { mutableStateOf(false) }
    var duplicateReview by remember { mutableStateOf<List<LocalVideo>?>(null) }
    val appSettings by AppContainer.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var customFolder by rememberSaveable { mutableStateOf("") }

    // ---- data ----
    LaunchedEffect(refresh) {
        loading = true
        val scanned = withContext(Dispatchers.IO) {
            runCatching { AppContainer.mediaScanner.scan() }.getOrDefault(emptyList())
        }
        withContext(Dispatchers.IO) { AppContainer.renameSuggestions.inspect(scanned) }
        videos = scanned
        cacheSizes = withContext(Dispatchers.IO) { measureCaches(context) }
        loading = false
    }

    // ---- system delete sheet (API 30+): the only way to remove other apps' files ----
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        message = if (result.resultCode == Activity.RESULT_OK) {
            "Files deleted"
        } else {
            "Nothing was deleted"
        }
        refresh++
    }

    // ---- backup / restore through the system file picker ----
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val backup = LibraryBackup.build(
                        context,
                        "${appVersionName(context)}",
                        includeSearchHistory = includeSearchHistory,
                    )
                    (context.contentResolver.openOutputStream(uri) ?: error("Could not open backup destination")).use { out ->
                        out.write(LibraryBackup.toJson(backup).toByteArray())
                    }
                    backup
                }.getOrNull()
            }
            message = if (result != null) {
                "Backup saved \u00b7 ${result.playback.size} watch states, " +
                    "${result.metadata.size} matches, ${result.favorites.size} favourites"
            } else {
                "The backup could not be written"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                }.getOrNull()
            }
            if (text == null) {
                message = "That file could not be read"
                return@launch
            }
            restorePreview = withContext(Dispatchers.IO) { runCatching { LibraryBackup.parse(text) }.getOrNull() }
            showRestoreConfirm = text
        }
    }

    val watchedKept = remember(videos) {
        videos.filter { AppContainer.playbackState.progressOf(it.id)?.isWatched == true }
    }
    val duplicates = remember(videos) { com.opticast.player.data.local.duplicateCandidates(videos) }
    val biggest = remember(videos) { videos.sortedByDescending { it.sizeBytes }.take(8) }

    // ================= layout =================
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Storage", style = MaterialTheme.typography.headlineSmall)
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ---------------- excluded folders (moved from Files & Storage) ----------------
            item {
                StorageCard("Excluded folders", Icons.Filled.FolderOff) {
                    val excludedCount = appSettings.excludedFolders.size
                    Text(
                        if (excludedCount == 0) "Videos inside these folders are skipped when scanning. No folders excluded yet."
                        else "Videos inside these $excludedCount folder${if (excludedCount == 1) "" else "s"} are skipped when scanning:",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (excludedCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        // List folders to compliment the description
                        appSettings.excludedFolders.forEach { folder ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.FolderOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text(folder, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val next = appSettings.excludedFolders - folder
                                    scope.launch {
                                        AppContainer.settings.setExcludedFolders(next)
                                        FolderExclusions.hydrate(next)
                                        AppContainer.metadataStore.touch()
                                    }
                                }) { Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customFolder,
                            onValueChange = { customFolder = it },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            placeholder = { Text("Custom path, e.g. Movies/Clips") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer)
                        )
                        IconButton(onClick = {
                            val path = customFolder.trim().trim('/')
                            if (path.isNotBlank() && !appSettings.excludedFolders.contains(path)) {
                                val next = appSettings.excludedFolders + path
                                scope.launch {
                                    AppContainer.settings.setExcludedFolders(next)
                                    FolderExclusions.hydrate(next)
                                    AppContainer.metadataStore.touch()
                                }
                                customFolder = ""
                            }
                        }) { Icon(Icons.Filled.Add, "Add folder", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }

            // ---------------- overview ----------------
            item {
                StorageCard("Library", Icons.Filled.Storage) {
                    StatRow("Videos", "${videos.size}")
                    StatRow("Total size", formatSize(videos.sumOf { it.sizeBytes }))
                    StatRow(
                        "Watched, still on the phone",
                        "${formatSize(watchedKept.sumOf { it.sizeBytes })} \u00b7 ${watchedKept.size} files",
                    )
                    StatRow("Duplicates", "${duplicates.size} groups")
                }
            }

            // ---------------- caches - simplified brief ----------------
            item {
                StorageCard("Caches", Icons.Filled.CleaningServices) {
                    val totalCache = cacheSizes.firstOrNull { it.first == "App total" }?.second ?: 0L
                    Text(
                        if (totalCache > 0) "Cached files: ${formatSize(totalCache)}"
                        else "No cached files",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    AppContainer.frameArtwork.deleteAll()
                                    AppContainer.thumbnails.deleteAll()
                                    runCatching { File(context.filesDir, "chapters").deleteRecursively() }
                                    runCatching { File(context.filesDir, "posters").deleteRecursively() }
                                }
                                message = "Caches cleared"
                                refresh++
                            }
                        }) { Text("Clear caches") }
                        OutlinedButton(onClick = { backupTick++; refresh++ }) { Text("Refresh") }
                    }
                }
            }

            // ---------------- reclaim: watched ----------------
            if (watchedKept.isNotEmpty()) {
                item {
                    StorageCard("Free up \u00b7 watched and still here", Icons.Filled.Delete) {
                        Text(
                            "${formatSize(watchedKept.sumOf { it.sizeBytes })} of videos you " +
                                "have already finished. Deleting asks the system first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { pendingDelete = watchedKept }) {
                            Text("Review ${watchedKept.size} watched videos")
                        }
                    }
                }
            }

            // ---------------- biggest ----------------
            item {
                StorageCard("Biggest files", Icons.Filled.Storage) {
                    biggest.forEach { video ->
                        FileRow(video) { pendingDelete = listOf(video) }
                    }
                }
            }

            // ---------------- duplicates ----------------
            if (duplicates.isNotEmpty()) {
                item {
                    StorageCard("Duplicates", Icons.Filled.ContentCopy) {
                        Text(
                            "Same-sized candidates, even with different names. Size alone is not proof; compare copies and optionally verify their SHA-256 hashes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        duplicates.forEach { group ->
                            Spacer(Modifier.height(6.dp))
                            Text("${group.size} same-sized files · ${formatSize(group.first().sizeBytes)} each",style=MaterialTheme.typography.titleSmall)
                            Text(group.take(2).joinToString("\n") { it.name },style=MaterialTheme.typography.bodySmall)
                            TextButton(onClick={duplicateReview=group}) { Text("Compare / choose copies") }
                        }
                    }
                }
            }

            // ---------------- organiser ----------------
            item {
                StorageCard("File organiser", Icons.Filled.DriveFileRenameOutline) {
                    Text(
                        "Review suggested names before changing files. Android will ask you to allow changes when required. " +
                            "Filenames change only after you confirm; saved metadata and playback progress stay linked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { showOrganiser = true }) {
                        Text("Review rename suggestions")
                    }
                }
            }

            // ---------------- backup ----------------
            item {
                StorageCard("Backup & restore", Icons.Filled.Save) {
                    Text(
                        "Export settings, watch state, favourites, title matches, collections, smart rules, skip ranges and Library layouts. API keys are included: keep the backup private. Video files/artwork are not copied.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(includeSearchHistory,{includeSearchHistory=it})
                        Text("Include recent search history (optional)",style=MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            exportLauncher.launch("opticast-backup.json")
                        }) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                            Icon(Icons.Filled.Restore, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    message?.let { text ->
        AlertDialog(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), 
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            text = { Text(text) },
        )
    }

    duplicateReview?.let { group -> DuplicateReviewDialog(group,onDelete={picked -> duplicateReview=null;pendingDelete=picked},onDismiss={duplicateReview=null}) }

    if (pendingDelete.isNotEmpty()) {
        AlertDialog(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), 
            onDismissRequest = { pendingDelete = emptyList() },
            title = { Text("Delete ${pendingDelete.size} file(s)?") },
            text = {
                Text(
                    "This removes the video files from your phone - " +
                        formatSize(pendingDelete.sumOf { it.sizeBytes }) + " freed. " +
                        "Android will ask you to confirm.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val targets = pendingDelete
                    pendingDelete = emptyList()
                    requestDelete(context, targets) { sender ->
                        deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    } ?: run { message = directDeleteFallback(context, targets) }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = emptyList() }) { Text("Cancel") }
            },
        )
    }

    // Keep the shared panel composed while Android's write-consent activity is open.
    RenameSuggestionsPanel(
        visible = showOrganiser,
        entries = renameEntries,
        onClose = { showOrganiser = false },
        onIdentify = { id -> showOrganiser = false; onOpenMatch(id) },
        onChanged = { refresh++ },
    )

    showRestoreConfirm?.let { text ->
        val preview=restorePreview
        AlertDialog(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f), 
            onDismissRequest = { showRestoreConfirm = null },
            title = { Text("Restore this backup?") },
            text = {
                Text(
                    if(preview==null) "This is not a readable OptiCast backup." else
                        "${preview.playback.size} watch states, ${preview.metadata.size} title matches, ${preview.favorites.size} favourites, ${preview.libraryPreferences.size} Library preference groups.\n\nNo media files are deleted. Matching settings, rules, skip ranges and same-ID collections are replaced by the imported values; unrelated records are kept. Restart afterward to reload Library preferences.",
                )
            },
            confirmButton = {
                TextButton(enabled=preview!=null,onClick = {
                    val payload = text
                    showRestoreConfirm = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                LibraryBackup.restore(context, preview ?: error("Invalid backup"))
                            }.getOrNull()
                        }
                        message = when {
                            result == null -> "That file is not an OptiCast backup"
                            result.needsRestart ->
                                "Restored ${result.watchStates} watch states and " +
                                    "${result.matchedTitles} matches \u00b7 restart the app " +
                                    "to load the matches"
                            else -> "Restored ${result.watchStates} watch states and " +
                                "${result.matchedTitles} matches"
                        }
                        refresh++
                    }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) { Text("Cancel") }
            },
        )
    }
}

// ============================ files ============================

/** System delete request (API 30+), or null when the platform will not ask. */
private fun requestDelete(
    context: Context,
    videos: List<LocalVideo>,
    launch: (android.content.IntentSender) -> Unit,
): Unit? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val uris = videos.map { Uri.parse(it.uri) }
    return runCatching {
        val pending = MediaStore.createDeleteRequest(context.contentResolver, uris)
        launch(pending.intentSender)
    }.getOrNull()
}

/** Older Androids: try a direct delete; the app may not own the file. */
private fun directDeleteFallback(context: Context, videos: List<LocalVideo>): String {
    var deleted = 0
    videos.forEach { video ->
        runCatching {
            if (context.contentResolver.delete(Uri.parse(video.uri), null, null) > 0) deleted++
        }
    }
    return if (deleted == 0) {
        "Android would not let OptiCast delete these files. Delete them from your " +
            "file manager instead."
    } else {
        "$deleted file(s) deleted"
    }
}

@Composable
private fun FileRow(video: LocalVideo, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onDelete)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                video.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatSize(video.sizeBytes) + " \u00b7 " + video.durationMs.formatDuration(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun StorageCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, body: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        body()
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    com.opticast.player.ui.components.AlignedLabelValue(label, value, valueAtEnd = true)
}

// ============================ helpers ============================

private fun measureCaches(context: Context): List<Pair<String, Long>> {
    fun size(name: String) = runCatching {
        File(context.filesDir, name).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)

    return listOf(
        "Poster artwork" to size("posters"),
        "Video frame thumbnails" to size("frames"),
        "Scrub previews" to size("thumbs"),
        "Chapter indexes" to size("chapters"),
        "Downloaded from network" to size("network"),
        "Subtitles & metadata" to (size("subtitles") + size("metadata")),
        "App total" to runCatching {
            context.filesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }.getOrDefault(0L),
    )
}

/** Same byte size and a near-identical name: the same file, twice. */
private fun findDuplicates(videos: List<LocalVideo>): List<List<LocalVideo>> {
    fun normalize(name: String) = name.substringBeforeLast('.')
        .lowercase()
        .replace(Regex("""[^a-z0-9]"""), "")

    return videos.groupBy { it.sizeBytes to normalize(it.name) }
        .filter { (_, group) -> group.size > 1 }
        .values
        .toList()
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun appVersionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}.getOrDefault("?")
