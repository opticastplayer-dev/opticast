package com.opticast.player.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/**
 * Seek-bar preview frames ("scrub thumbnails").
 *
 * Frames are pulled with [MediaMetadataRetriever] at even intervals across the
 * file, shrunk to a thumbnail and stored as JPEGs, so scrubbing shows a real
 * frame instead of a bare time label. Generation is lazy (it starts the first
 * time the user scrubs), runs once per title, is throttled to a fixed frame
 * budget, and is stored on disk for every later session.
 *
 * Disk cost per title is roughly 100 KB for a two-hour film, and the frames are
 * tiny (160x90) so the memory cost during scrubbing is negligible.
 */
class ThumbnailCache(private val context: Context) {

    @Serializable
    data class Meta(val count: Int, val intervalMs: Long, val durationMs: Long)

    private val root = File(context.filesDir, "thumbs").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    private fun dirFor(videoId: Long) = File(root, videoId.toString())
    private fun metaFor(videoId: Long) = File(dirFor(videoId), "meta.json")

    /** Frames are capped so a very long file cannot explode generation time. */
    private val maxFrames = 100
    private val minFrames = 12
    private val frameWidth = 160

    /**
     * True when previews for this title are ready to be shown. Cheap enough to
     * call while scrubbing (one small file read, no decoding).
     */
    fun meta(videoId: Long): Meta? = runCatching {
        val f = metaFor(videoId)
        if (!f.exists()) null else json.decodeFromString<Meta>(f.readText())
    }.getOrNull()

    /** Frame file for a position, or null when previews are not available. */
    fun frameFor(videoId: Long, positionMs: Long): File? {
        val meta = meta(videoId) ?: return null
        if (meta.intervalMs <= 0L) return null
        val index = (positionMs / meta.intervalMs).toInt().coerceIn(0, meta.count - 1)
        val file = File(dirFor(videoId), "$index.jpg")
        return if (file.exists()) file else null
    }

    /**
     * Builds the frame set if it is missing. Safe to call repeatedly: it returns
     * immediately once the work is done, and a cancelled call leaves the partial
     * set on disk for the next attempt. Returns true when previews are available.
     */
    suspend fun prepare(videoId: Long, uri: String, durationMs: Long): Boolean =
        withContext(Dispatchers.IO) {
            if (durationMs < 30_000L) return@withContext false
            meta(videoId)?.let { return@withContext it.count > 0 }

            val dir = dirFor(videoId).apply { mkdirs() }
            val count = ((durationMs / 60_000L).toInt()).coerceIn(minFrames, maxFrames)
            val interval = durationMs / count

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uri))
            } catch (t: Throwable) {
                runCatching { retriever.release() }
                return@withContext false
            }

            val frameTarget = retriever.thumbnailTarget(frameWidth)
            var written = 0
            try {
                for (i in 0 until count) {
                    coroutineContext.ensureActive()
                    val target = File(dir, "$i.jpg")
                    if (target.exists()) {
                        written++
                        continue
                    }
                    val grabbed = retriever.thumbnailFrame((i * interval) * 1000L, frameTarget)
                    if (grabbed == null) continue
                    val frame = grabbed

                    var scaled: Bitmap? = null
                    try {
                        val rendered = scale(frame)
                        scaled = rendered
                        if (runCatching { write(rendered, target) }.getOrDefault(false)) written++
                    } finally {
                        if (scaled !== frame) scaled?.recycle()
                        frame.recycle()
                    }
                }
            } catch (_: Throwable) {
                // Cancelled or the file went away: keep whatever was written.
            } finally {
                runCatching { retriever.release() }
            }

            if (written > 0) {
                runCatching {
                    metaFor(videoId)
                        .writeText(json.encodeToString(Meta(written, interval, durationMs)))
                }
            }
            written > 0
        }

    private fun scale(source: Bitmap): Bitmap {
        if (source.width <= frameWidth) return source
        val height = (source.height.toFloat() * frameWidth / source.width).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, frameWidth, height, true)
    }

    private fun write(bitmap: Bitmap, target: File): Boolean {
        // Written to a temp file first so a torn write can never leave a
        // half-image that Coil would fail to decode.
        val temp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(temp).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)) return false
        }
        return temp.renameTo(target)
    }

    /** Housekeeping hook for the future storage dashboard. */
    fun sizeBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun deleteAll() {
        runCatching { root.deleteRecursively() }
        runCatching { root.mkdirs() }
    }
}
