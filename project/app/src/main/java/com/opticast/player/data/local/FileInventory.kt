package com.opticast.player.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LocalVideo
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Atomic offline inventory. No video is deleted and no playback record is rewritten. */
class FileInventory(private val context: Context) {
    private val file = AtomicFile(File(context.filesDir, "file_inventory.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private var loadFailed = false
    private var records: List<TrackedFile> = try {
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists())
            json.decodeFromString(file.openRead().bufferedReader().use { it.readText() }) else emptyList()
    } catch (_: Exception) { loadFailed = true; emptyList() }
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    /** Last reconciled available records, without probing storage or writing the inventory. */
    @Synchronized
    fun available(): List<LocalVideo> {
        check(!loadFailed) { "Saved file inventory could not be read." }
        return records.filter { it.available && !FolderExclusions.isExcluded(it.video.relativePath) }.map { it.video }
    }

    @Synchronized
    fun missing(): List<LocalVideo> = records.filter { !it.available && !it.dismissed &&
        !FolderExclusions.isExcluded(it.video.relativePath) }.map { it.video }

    @Synchronized
    fun dismiss(id: Long) {
        check(!loadFailed) { "Saved file inventory could not be read; no records were changed." }
        commit(records.map { if (it.video.id == id && !it.available) it.copy(dismissed = true) else it })
    }

    @Synchronized
    fun reconcile(videos: List<LocalVideo>): List<LocalVideo> {
        check(!loadFailed) { "Saved file inventory could not be read; no records were changed." }
        val observed = videos.mapNotNull { video ->
            val atUri = records.filter { it.video.uri == video.uri }
            val old = atUri.singleOrNull { it.available } ?: atUri.singleOrNull()
            val signature = if (old != null && old.available && unchangedFileRow(old.video, video) && old.signature != null)
                old.signature else sampledSignature(video)
            if (old?.signature != null && signature == null) null else ObservedFile(video, signature)
        }
        val next = reconcileFiles(records, observed)
        // Keep failed automatic matching suppressed across rename/path/id changes.
        next.filter { it.available }.forEach { fresh ->
            records.firstOrNull { it.video.id == fresh.video.id }?.let { old ->
                if (old.video != fresh.video) AppContainer.offlineLibrary.preserveAttempt(old.video, fresh.video)
            }
        }
        commit(next)
        return next.filter { it.available }.map { it.video }
    }

    @Synchronized
    fun uriFor(id: Long): Uri? {
        check(!loadFailed) { "Saved file inventory could not be read." }
        records.firstOrNull { it.video.id == id }?.let { return Uri.parse(it.video.uri) }
        // Never interpret another title's new MediaStore id as an old library id.
        if (records.any { runCatching { ContentUris.parseId(Uri.parse(it.video.uri)) }.getOrNull() == id }) return null
        return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
    }

    @Synchronized
    fun resolve(id: Long, raw: LocalVideo): LocalVideo? {
        val old = records.firstOrNull { it.video.id == id } ?: return raw.copy(id = id)
        if (old.available && unchangedFileRow(old.video, raw)) return raw.copy(id = id)
        return if (sameFileContent(old, ObservedFile(raw, sampledSignature(raw)))) raw.copy(id = id) else null
    }

    /** Establish a content signature before a user-approved filename mutation. */
    @Synchronized
    fun prepareRename(id: Long) {
        check(!loadFailed) { "Cannot read the saved inventory. Nothing was renamed." }
        val old = records.firstOrNull { it.video.id == id && it.available }
            ?: error("Scan this file first so its history can be preserved.")
        val signature = sampledSignature(old.video) ?: error("Cannot verify this file's identity. Nothing was renamed.")
        check(old.signature == null || old.signature == signature) { "File content changed. Recheck storage before renaming." }
        if (old.signature == null) commit(records.map { if (it.video.id == id) it.copy(signature = signature) else it })
    }

    @Synchronized
    fun exportRecords(): String {
        check(!loadFailed) { "Saved file inventory could not be read." }
        return json.encodeToString(records)
    }

    /** Restored rows must be verified locally again; conflicting identities reject the import. */
    @Synchronized
    fun restoreRecords(text: String) {
        check(!loadFailed) { "Saved file inventory could not be read." }
        val incoming = json.decodeFromString<List<TrackedFile>>(text)
        require(incoming.map { it.video.id }.distinct().size == incoming.size) { "Duplicate backup identities." }
        incoming.forEach { restored ->
            require(restored.video.id > 0 && restored.video.uri.startsWith("content://media/external/video/media/"))
            records.firstOrNull { it.video.id == restored.video.id }?.let { existing ->
                require(existing == restored || (existing.signature != null && existing.signature == restored.signature &&
                    existing.video.sizeBytes == restored.video.sizeBytes && existing.video.durationMs == restored.video.durationMs)) {
                    "Backup file identities conflict with this library. Existing records were kept."
                }
            }
        }
        val ids = incoming.map { it.video.id }.toSet()
        commit(records.filter { it.video.id !in ids } + incoming.map { it.copy(available = false) })
    }

    private fun commit(next: List<TrackedFile>) {
        if (next == records) return
        var output: java.io.FileOutputStream? = null
        try {
            output = file.startWrite()
            output.write(json.encodeToString(next).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
        records = next
        _version.value++
    }

    /** Three 64 KiB samples + exact size/duration: bounded local I/O, no uploads. */
    private fun sampledSignature(video: LocalVideo): String? = runCatching {
        if (video.sizeBytes <= 0) return null
        context.contentResolver.openFileDescriptor(Uri.parse(video.uri), "r")?.let { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                if (input.channel.size() != video.sizeBytes) return null
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update("OptiCast-sample-v1:${video.sizeBytes}:".toByteArray())
                val count = minOf(video.sizeBytes, 65_536L).toInt()
                val offsets = listOf(0L, (video.sizeBytes - count) / 2, video.sizeBytes - count).distinct()
                val buffer = ByteArray(count)
                offsets.forEach { offset ->
                    Os.lseek(descriptor.fileDescriptor, offset, OsConstants.SEEK_SET)
                    var read = 0
                    while (read < count) {
                        val n = input.read(buffer, read, count - read)
                        check(n > 0)
                        read += n
                    }
                    digest.update(offset.toString().toByteArray())
                    digest.update(buffer)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        }
    }.getOrNull()
}
