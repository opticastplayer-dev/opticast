package com.opticast.player.data.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.local.FolderExclusions
import com.opticast.player.data.parser.NameParser

/** Scans the device for video files via MediaStore. */
class MediaScanner(private val context: Context) {
    val inventory = FileInventory(context)

    // Note: WIDTH / HEIGHT columns only exist on API 29+, so they are read
    // defensively below instead of being part of the projection.
    // RELATIVE_PATH (API 29+) is only requested when available, for folder
    // exclusions.
    private fun projection(): Array<String> {
        val base = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        return if (Build.VERSION.SDK_INT >= 29) base + arrayOf("relative_path", "width", "height") else base
    }

    /** Returns all videos longer than [minDurationMs] (filters out tiny clips). */
    @Synchronized
    fun scan(minDurationMs: Long = 60_000L): List<LocalVideo> {
        val result = mutableListOf<LocalVideo>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        context.contentResolver.query(
            collection,
            projection(),
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndex("width") // -1 below API 29
            val heightCol = cursor.getColumnIndex("height")
            val pathCol = cursor.getColumnIndex("relative_path") // -1 below API 29

            while (cursor.moveToNext()) {
                try {
                    val name = try { cursor.getString(nameCol) } catch (_: Exception) { null } ?: continue
                    val duration = try { cursor.getLong(durCol) } catch (_: Exception) { 0L }
                    if (duration < minDurationMs) continue
                    val id = try { cursor.getLong(idCol) } catch (_: Exception) { continue }
                    result += LocalVideo(
                        id = id,
                        name = name,
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        sizeBytes = try { cursor.getLong(sizeCol) } catch (_: Exception) { 0L },
                        durationMs = duration,
                        dateAddedSec = try { cursor.getLong(dateCol) } catch (_: Exception) { 0L },
                        width = if (widthCol >= 0) try { cursor.getInt(widthCol) } catch (_: Exception) { 0 } else 0,
                        height = if (heightCol >= 0) try { cursor.getInt(heightCol) } catch (_: Exception) { 0 } else 0,
                        parsed = try { NameParser.parse(name) } catch (_: Exception) { NameParser.parse("video") },
                        relativePath = if (pathCol >= 0) try { cursor.getString(pathCol).orEmpty() } catch (_: Exception) { "" } else "",
                        modifiedSec = try { cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)) } catch (_: Exception) { 0L },
                    )
                } catch (e: SecurityException) {
                    // 10/10: Android 13+ MediaStore can throw SecurityException for restricted files
                    android.util.Log.w("MediaScanner", "Skipping restricted file at cursor ${cursor.position}: ${e.message}")
                    continue
                } catch (e: IllegalStateException) {
                    android.util.Log.w("MediaScanner", "Skipping illegal state at cursor ${cursor.position}: ${e.message}")
                    continue
                } catch (e: Exception) {
                    // STABILITY: Skip corrupted file, don't crash whole scan
                    android.util.Log.w("MediaScanner", "Skipping corrupted file at cursor ${cursor.position}: ${e.message}")
                    continue
                }
            }
        } ?: error("Video storage could not be queried. Saved library records were kept.")
        // A null cursor is a provider failure, not an empty device.
        return inventory.reconcile(result).filterNot { FolderExclusions.isExcluded(it.relativePath) }
    }

    /** Resolves a stable library id to its currently verified MediaStore URI. */
    fun byId(id: Long): LocalVideo? {
        val uri = inventory.uriFor(id) ?: return null
        return readUri(uri)?.let { inventory.resolve(id, it) }
    }

    private fun readUri(uri: android.net.Uri): LocalVideo? {
        val id = ContentUris.parseId(uri)
        context.contentResolver.query(uri, projection(), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                ) ?: return null
                return LocalVideo(
                    id = id,
                    name = name,
                    uri = uri.toString(),
                    sizeBytes = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    ),
                    durationMs = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    ),
                    dateAddedSec = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    ),
                    width = cursor.getColumnIndex("width")
                        .takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0,
                    height = cursor.getColumnIndex("height")
                        .takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0,
                    parsed = NameParser.parse(name),
                    relativePath = cursor.getColumnIndex("relative_path").takeIf { it >= 0 }?.let { cursor.getString(it) }.orEmpty(),
                    modifiedSec = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)),
                )
            }
        }
        return null
    }
}
