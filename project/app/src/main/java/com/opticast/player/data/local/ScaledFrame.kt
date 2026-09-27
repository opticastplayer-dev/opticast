package com.opticast.player.data.local

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class FrameTarget(val width: Int, val height: Int)

/** Display-oriented bounds, matching the existing width-based thumbnail size without upscaling. */
internal fun scaledFrameTarget(width: Int?, height: Int?, rotation: Int?, targetWidth: Int): FrameTarget? {
    if (width == null || height == null || width !in 1..32768 || height !in 1..32768 || targetWidth !in 1..4096) return null
    if (rotation !in listOf(0, 90, 180, 270)) return null
    val w = if (rotation == 90 || rotation == 270) height else width
    val h = if (rotation == 90 || rotation == 270) width else height
    val resultWidth = minOf(w, targetWidth)
    return FrameTarget(resultWidth, (h.toDouble() * resultWidth / w).roundToInt().coerceAtLeast(1))
}

internal fun suitableScaledFrame(width: Int, height: Int, target: FrameTarget): Boolean =
    width > 0 && height > 0 && width >= target.width * 0.95 && height >= target.height * 0.95 &&
        abs(width.toDouble() / height - target.width.toDouble() / target.height) <= maxOf(0.03, 2.0 / target.height)

/** Reuse metadata for all seek-preview frames. Missing metadata uses the compatibility path. */
internal fun MediaMetadataRetriever.thumbnailTarget(targetWidth: Int): FrameTarget? = runCatching {
    scaledFrameTarget(extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
        extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
        extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull(), targetWidth)
}.getOrNull()

/** Caller owns the returned bitmap. Timestamp and sync-frame selection are unchanged. */
internal fun MediaMetadataRetriever.thumbnailFrame(timeUs: Long, target: FrameTarget?): Bitmap? {
    if (Build.VERSION.SDK_INT >= 27 && target != null) {
        val frame = runCatching { getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, target.width, target.height) }.getOrNull()
        if (frame != null) {
            if (suitableScaledFrame(frame.width, frame.height, target)) return frame
            frame.recycle()
        }
    }
    return runCatching { getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) }.getOrNull()
}
