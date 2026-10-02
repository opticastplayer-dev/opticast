package com.opticast.player.ui.screens.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Gold Standard — Permission handling extracted from LibraryScreen.kt
 * Single responsibility: video permission + notification permission
 */
class LibraryPermissionState(
    val hasPermission: MutableState<Boolean>,
    val permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
)

@Composable
fun rememberLibraryPermissionState(): LibraryPermissionState {
    val context = LocalContext.current
    val videoPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(videoPermission, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(videoPermission)
    }
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, videoPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission.value = results[videoPermission] == true
    }

    return LibraryPermissionState(hasPermission, permissionLauncher)
}
