package com.opticast.player.data.local

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.opticast.player.data.AppContainer
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Dynamic frame extraction: a real frame out of the video, used as the artwork
 * for a title OptiCast has no poster for.
 *
 * An unscraped file used to fall back to the drawn film-frame placeholder. A
 * genuine frame is better: it shows which episode or camera copy a file is, and
 * it costs one decode per title, once, cached on disk.
 *
 * The frame is taken a fifth of the way in - past the studio logos, into the
 * actual content - preferring the nearest sync frame, which is both fast and
 * always decodable. Failures are remembered in memory for the session so a file
 * that cannot be decoded is not re-attempted every time the grid scrolls past.
 */
class FrameArtwork(private val context: Context) {

    private val root = File(context.filesDir, "frames").apply { mkdirs() }

    /** Two at a time: enough to fill a grid quickly without stalling the UI. */
    private val gate = Semaphore(2)
    private val failed = mutableSetOf<Long>()

    private val frameWidth = 640

    /** Already extracted, or null. Cheap enough to call during composition. */
    fun cached(videoId: Long): File? {
        if (videoId <= 0L) return null
        val file = File(root, "$videoId.jpg")
        return file.takeIf { it.exists() && it.length() > 0L }
    }

    /**
     * Extracts (or reuses) the artwork frame for a title. Returns null when the
     * file cannot be decoded - the caller keeps the placeholder in that case.
     */
    suspend fun generate(videoId: Long): File? = withContext(Dispatchers.IO) {
        if (videoId <= 0L) return@withContext null
        cached(videoId)?.let { return@withContext it }
        synchronized(failed) { if (videoId in failed) return@withContext null }

        gate.withPermit {
            cached(videoId)?.let { return@withPermit it }
            val video = runCatching { AppContainer.mediaScanner.byId(videoId) }.getOrNull()
                ?: return@withPermit null

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(video.uri))
                val durationMs = video.durationMs.takeIf { it > 0L } ?: runCatching {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                }.getOrDefault(0L)

                val shot = retriever.thumbnailFrame(framePosition(durationMs) * 1000L, retriever.thumbnailTarget(frameWidth))
                if (shot == null) {
                    synchronized(failed) { failed.add(videoId) }
                    return@withPermit null
                }

                var scaled: Bitmap? = null
                try {
                val rendered = if (shot.width > frameWidth) {
                    val height = (shot.height.toFloat() * frameWidth / shot.width)
                        .toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(shot, frameWidth, height, true)
                } else {
                    shot
                }

                scaled = rendered
                val target = File(root, "$videoId.jpg")
                // Written through a temp file so a torn write can never leave a
                // half-image that the image loader would fail to decode.
                val temp = File(root, "$videoId.jpg.tmp")
                val ok = runCatching {
                    FileOutputStream(temp).use { out ->
                        rendered.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                }.getOrDefault(false)

                if (ok && temp.renameTo(target)) {
                    target
                } else {
                    runCatching { temp.delete() }
                    synchronized(failed) { failed.add(videoId) }
                    null
                }
                } finally {
                    if (scaled !== shot) scaled?.recycle()
                    shot.recycle()
                }
            } catch (_: Throwable) {
                synchronized(failed) { failed.add(videoId) }
                null
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    /** A fifth of the way in, but never in the titles and never near the end. */
    private fun framePosition(durationMs: Long): Long = when {
        durationMs <= 0L -> 30_000L
        else -> (durationMs / 5L).coerceIn(10_000L, 90_000L)
    }

    /** Housekeeping hooks for the storage dashboard. */
    fun sizeBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun deleteFor(videoId: Long) {
        runCatching { File(root, "$videoId.jpg").delete() }
        synchronized(failed) { failed.remove(videoId) }
    }

    fun forgetFailures() = synchronized(failed) { failed.clear() }

    fun deleteAll() {
        runCatching { root.deleteRecursively() }
        runCatching { root.mkdirs() }
        synchronized(failed) { failed.clear() }
    }
}
