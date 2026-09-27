package com.opticast.player.ui.screens

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Always composed so dismissing the menu does not discard Android's delete result. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EpisodeActions(entry: LibraryEntry?, onDismiss: () -> Unit,
    onPlay: (LibraryEntry) -> Unit, onDetails: (LibraryEntry) -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= 30) { pendingId = null; onChanged() } else retry++
        } else pendingId = null
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) retry++ else { pendingId = null; message("Storage permission was not granted. Nothing was deleted.") }
    }
    LaunchedEffect(pendingId, retry) {
        val id = pendingId ?: return@LaunchedEffect
        try {
            val video = withContext(Dispatchers.IO) { AppContainer.mediaScanner.byId(id) }
            if (video == null) { pendingId = null; message("Episode unavailable. Recheck storage first."); return@LaunchedEffect }
            val uri = Uri.parse(video.uri)
            if (Build.VERSION.SDK_INT >= 30) {
                deleteLauncher.launch(IntentSenderRequest.Builder(MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender).build())
            } else if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                try {
                    val deleted = withContext(Dispatchers.IO) { context.contentResolver.delete(uri, null, null) }
                    pendingId = null
                    if (deleted == 0) message("Episode was not deleted. Refresh the show and try again.")
                    onChanged()
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT == 29 && e is RecoverableSecurityException) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                    } else throw e
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { pendingId = null; message("Could not delete this episode. Your saved history was kept.") }
    }
    entry?.let { selected ->
        val watched = AppContainer.playbackState.progressOf(selected.video.id)?.isWatched == true
        val favorite = AppContainer.favorites.isFavorite(selected.video.id)
        ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f), tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding()) {
                Text(selected.metadata?.displayTitle ?: selected.video.name, style = MaterialTheme.typography.titleMedium)
                @Composable fun Action(icon: ImageVector, label: String, destructive: Boolean = false, block: () -> Unit) {
                    MenuActionRow(icon, label, block, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                Action(Icons.Filled.PlayArrow, "Play episode") { onDismiss(); onPlay(selected) }
                Action(Icons.Filled.Info, "Episode information") { onDismiss(); onDetails(selected) }
                Action(Icons.Filled.CheckCircle, if (watched) "Mark as unwatched" else "Mark as watched") {
                    AppContainer.playbackState.setWatched(selected.video.id, !watched, selected.video.durationMs); onDismiss()
                }
                Action(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, if (favorite) "Remove from favourites" else "Add to favourites") { AppContainer.favorites.toggle(selected.video.id); onDismiss() }
                Action(Icons.Filled.Share, "Share episode") {
                    onDismiss()
                    scope.launch {
                        runCatching {
                            val video = withContext(Dispatchers.IO) { AppContainer.mediaScanner.byId(selected.video.id) } ?: error("Unavailable")
                            val uri = Uri.parse(video.uri)
                            val intent = Intent(Intent.ACTION_SEND).setType(context.contentResolver.getType(uri) ?: "video/*")
                                .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.clipData = ClipData.newUri(context.contentResolver, video.name, uri)
                            context.startActivity(Intent.createChooser(intent, "Share episode"))
                        }.onFailure { message("Could not share this episode. Check that the file is available.") }
                    }
                }
                Action(Icons.Filled.Delete, "Delete episode from device", destructive = true) { confirmId = selected.video.id; onDismiss() }
            }
        }
    }
    confirmId?.let { id ->
        AlertDialog(onDismissRequest = { confirmId = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            title = { Text("Delete this episode?") }, text = { Text("Only this episode's video file will be permanently deleted. Other episodes and saved history are not deleted.") },
            confirmButton = { TextButton(enabled = pendingId == null, onClick = { confirmId = null; pendingId = id }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancel") } })
    }
}
