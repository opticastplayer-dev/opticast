package com.opticast.player.ui.screens.library

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Delete handling extracted from LibraryScreen.kt
 * Single responsibility: delete flow with MediaStore + legacy Android 10 handling
 */
class LibraryDeleteHandler(
    val deleteLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    val writePermLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    val legacyDeleteLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
    val performDelete: (List<Long>) -> Unit
)

@Composable
fun rememberLibraryDeleteHandler(
    selectionState: LibrarySelectionState,
    viewModel: com.opticast.player.ui.screens.LibraryViewModel,
    onFileActionError: (String) -> Unit
): LibraryDeleteHandler {
    val context = LocalContext.current
    val pendingWriteDelete = selectionState.pendingWriteDelete
    val legacyDeleteTick = selectionState.legacyDeleteTick

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) viewModel.recheckFiles()
    }
    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingWriteDelete.value.isNotEmpty()) {
            pendingWriteDelete.value.mapNotNull { LibraryFileActions.uriOf(it) }.forEach { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            viewModel.scan()
            pendingWriteDelete.value = emptyList()
        }
    }

    val legacyDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) legacyDeleteTick.value++
        else {
            pendingWriteDelete.value = emptyList()
            viewModel.recheckFiles()
        }
    }

    LaunchedEffect(legacyDeleteTick.value) {
        if (Build.VERSION.SDK_INT != 29 || legacyDeleteTick.value == 0) return@LaunchedEffect
        while (pendingWriteDelete.value.isNotEmpty()) {
            val id = pendingWriteDelete.value.first()
            try {
                val uri = withContext(Dispatchers.IO) { LibraryFileActions.uriOf(id) }
                if (uri == null) {
                    onFileActionError("A selected episode is unavailable; remaining files were not deleted.")
                    pendingWriteDelete.value = emptyList()
                    break
                }
                withContext(Dispatchers.IO) { context.contentResolver.delete(uri, null, null) }
                pendingWriteDelete.value = pendingWriteDelete.value.drop(1)
            } catch (e: android.app.RecoverableSecurityException) {
                legacyDeleteLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                return@LaunchedEffect
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                onFileActionError("Could not delete all selected episodes. Refresh the library to review the remaining files.")
                pendingWriteDelete.value = emptyList()
                break
            }
        }
        viewModel.recheckFiles()
    }

    fun performDelete(ids: List<Long>) {
        val uris = ids.distinct().mapNotNull { LibraryFileActions.uriOf(it) }
        if (uris.size != ids.distinct().size) {
            onFileActionError("Some selected files are unavailable. Recheck storage before deleting the selection.")
            return
        }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT == 29) {
            pendingWriteDelete.value = ids.distinct()
            legacyDeleteTick.value++
            return
        }
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure {
                onFileActionError("Could not request deletion. Try a smaller selection or check storage access.")
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                uris.forEach { uri ->
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
                viewModel.scan()
            } else {
                pendingWriteDelete.value = ids
                writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    return LibraryDeleteHandler(
        deleteLauncher = deleteLauncher,
        writePermLauncher = writePermLauncher,
        legacyDeleteLauncher = legacyDeleteLauncher,
        performDelete = ::performDelete
    )
}
