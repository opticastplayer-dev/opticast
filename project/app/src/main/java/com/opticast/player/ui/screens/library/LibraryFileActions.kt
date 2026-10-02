package com.opticast.player.ui.screens.library

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.opticast.player.data.AppContainer

/**
 * Gold Standard — File actions extracted from LibraryScreen.kt
 * Single responsibility: share / delete / mime handling
 * Was 200+ lines inside LibraryScreen composable, now reusable, testable
 */
object LibraryFileActions {

    fun fileActionError(context: Context, text: String) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }

    fun uriOf(videoId: Long): Uri? =
        AppContainer.mediaScanner.byId(videoId)?.uri?.let { Uri.parse(it) }

    fun mimeForVideo(context: Context, uri: Uri, displayName: String): String {
        val fromResolver = context.contentResolver.getType(uri)
        if (fromResolver != null && fromResolver != "application/octet-stream") return fromResolver
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            "ts", "m2ts" -> "video/mp2ts"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            else -> "video/*"
        }
    }

    fun shareVideos(context: Context, ids: List<Long>) {
        val videos = ids.distinct().mapNotNull { AppContainer.mediaScanner.byId(it) }
        if (videos.size != ids.distinct().size) {
            fileActionError(context, "Some selected files are unavailable. Recheck storage before sharing the whole selection.")
            return
        }
        val uris = videos.map { Uri.parse(it.uri) }
        if (videos.isEmpty() || uris.isEmpty()) return
        runCatching {
            if (uris.size == 1) {
                val video = videos.first()
                val send = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, uris.first())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType(mimeForVideo(context, uris.first(), video.name))
                send.clipData = ClipData.newUri(context.contentResolver, video.name, uris.first())
                context.startActivity(Intent.createChooser(send, "Share video"))
            } else {
                val send = Intent(Intent.ACTION_SEND_MULTIPLE)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("video/*")
                send.clipData = ClipData.newUri(context.contentResolver, "Selected videos", uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
                context.startActivity(Intent.createChooser(send, "Share ${uris.size} videos"))
            }
        }.onFailure {
            fileActionError(context, "Could not open sharing. No files were changed.")
        }
    }
}
