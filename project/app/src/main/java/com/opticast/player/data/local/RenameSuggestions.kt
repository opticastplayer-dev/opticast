package com.opticast.player.data.local

import android.content.Context
import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.LocalVideo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Suggestions are local and approvals explicit; only the existing matcher uses the network. */
class RenameSuggestions(private val context: Context) {
    private val prefs = context.getSharedPreferences("rename_suggestions", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version
    private fun clueKey(video: LocalVideo) = "clue:${video.id}:${video.sizeBytes}:${video.durationMs}"
    private fun dismissal(suggestion: RenameSuggestion): String = "dismiss:" + java.security.MessageDigest.getInstance("SHA-256")
        .digest("${suggestion.entry.video.id}|${suggestion.entry.video.name}|${suggestion.proposedName}".toByteArray())
        .joinToString("") { "%02x".format(it) }

    /** Called on IO during library scanning, before automatic metadata matching. */
    fun inspect(videos: List<LocalVideo>) {
        val edit = prefs.edit()
        var changed = false
        videos.forEach { video ->
            if (!prefs.contains(clueKey(video))) {
                val embedded = if (filenameClue(video.name).title.isBlank()) {
                    val retriever = MediaMetadataRetriever()
                    try { retriever.setDataSource(context, Uri.parse(video.uri)); retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) }
                    catch (_: Exception) { null } finally { runCatching { retriever.release() } }
                } else null
                edit.putString(clueKey(video), json.encodeToString(chooseRenameClue(video.name, video.relativePath, embedded)))
                changed = true
            }
        }
        if (changed && edit.commit()) _version.value++
    }

    internal fun clue(video: LocalVideo): RenameClue {
        val current = filenameClue(video.name)
        if (current.title.isNotBlank()) return current
        return prefs.getString(clueKey(video), null)?.let {
            runCatching { json.decodeFromString<RenameClue>(it) }.getOrNull()
        } ?: chooseRenameClue(video.name, video.relativePath, null)
    }

    internal fun suggestions(entries: List<LibraryEntry>, includeDismissed: Boolean = false): List<RenameSuggestion> = entries.mapNotNull { entry ->
        renameSuggestion(entry, clue(entry.video))?.takeIf { includeDismissed || !prefs.getBoolean(dismissal(it), false) }
    }
    internal fun dismiss(suggestion: RenameSuggestion) {
        prefs.edit().putBoolean(dismissal(suggestion), true).apply(); _version.value++
    }

    /** Check identity, extension and destination before requesting Android write consent. */
    fun validate(id: Long, expectedName: String, newName: String): LocalVideo {
        check(android.os.Build.VERSION.SDK_INT >= 29) { "In-app file renaming requires Android 10 or later. Use a file manager on this device." }
        renameValidationError(expectedName, newName)?.let { error(it) }
        val video = AppContainer.mediaScanner.byId(id) ?: error("File unavailable. Recheck storage first.")
        check(video.name == expectedName) { "Filename changed since review. Refresh suggestions first." }
        check(video.name != newName) { "The file already has this name." }
        AppContainer.mediaScanner.inventory.prepareRename(id)
        checkNoCollision(video, newName)
        return video
    }

    /** Never overwrite or move folders; only update DISPLAY_NAME of the verified video URI. */
    fun rename(id: Long, expectedName: String, newName: String, rescan: Boolean = true) {
        val video = validate(id, expectedName, newName)
        val changed = context.contentResolver.update(Uri.parse(video.uri), ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }, null, null)
        check(changed == 1) { "Android did not rename this file. No history was changed." }
        val renamed = AppContainer.mediaScanner.byId(id)
        check(renamed?.name == newName) { "Android returned a different filename. Recheck storage to review the result." }
        // The inventory rechecks content and retains the stable ID; all caches stay keyed to it.
        if (rescan) AppContainer.mediaScanner.scan()
        _version.value++
    }

    private fun checkNoCollision(video: LocalVideo, newName: String) {
        val uri = Uri.parse(video.uri)
        val volume = uri.pathSegments.firstOrNull() ?: "external"
        val collection = MediaStore.Files.getContentUri(volume)
        val pathColumn = if (android.os.Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val currentPath = context.contentResolver.query(uri, arrayOf(pathColumn), null, null, null)?.use {
            check(it.moveToFirst()); it.getString(0)
        } ?: error("Could not verify the destination folder. Nothing was renamed.")
        val parent = if (android.os.Build.VERSION.SDK_INT >= 29) currentPath else currentPath.substringBeforeLast('/') + "/"
        context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, pathColumn), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0).orEmpty(); val raw = cursor.getString(1).orEmpty()
                val folder = if (android.os.Build.VERSION.SDK_INT >= 29) raw else raw.substringBeforeLast('/') + "/"
                check(!(folder == parent && name.equals(newName, true))) { "A file with this name already exists in the folder. Choose another name." }
            }
        } ?: error("Could not check for duplicate filenames. Nothing was renamed.")
    }
}
