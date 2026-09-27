package com.opticast.player.data.local

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.opticast.player.data.model.LocalVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.security.MessageDigest

fun duplicateCandidates(videos: List<LocalVideo>): List<List<LocalVideo>> = videos.filter { it.sizeBytes>0 }.groupBy { it.sizeBytes }.values.filter { it.size>1 }
fun deletableDuplicateSelection(group: List<LocalVideo>, selected: Set<Long>): List<LocalVideo> {
    val picks=group.filter { it.id in selected }.distinctBy { it.id }
    return if(picks.size in 1 until group.distinctBy { it.id }.size) picks else emptyList()
}

/** Explicit local I/O only. No automatic hashing, uploads or deletion. */
suspend fun verifyLocalFileHash(context: Context, video: LocalVideo): String = withContext(Dispatchers.IO) {
    val uri=Uri.parse(video.uri)
    require(uri.scheme=="content" || uri.scheme=="file") { "Only local sources can be verified" }
    fun identity(): Pair<Long,Long>? = if(uri.scheme=="file") {
        val file=java.io.File(uri.path ?: "");if(file.isFile) file.length() to file.lastModified() else null
    } else runCatching {
        context.contentResolver.query(uri,arrayOf(MediaStore.MediaColumns.SIZE,MediaStore.MediaColumns.DATE_MODIFIED),null,null,null)?.use { c ->
            if(c.moveToFirst()) c.getLong(0) to c.getLong(1) else null
        }
    }.getOrNull()
    val before=identity() ?: error("Cannot verify file identity; recheck storage permissions")
    require(before.first==video.sizeBytes) { "File size changed; rescan before verifying" }
    val hash=MessageDigest.getInstance("SHA-256");var total=0L
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val buffer=ByteArray(128*1024)
        while(true) { currentCoroutineContext().ensureActive();val n=stream.read(buffer);if(n<0) break;hash.update(buffer,0,n);total+=n }
    } ?: error("Cannot open file")
    require(total==before.first && identity()==before) { "File changed while reading; result discarded" }
    hash.digest().joinToString("") { "%02x".format(it) }
}
