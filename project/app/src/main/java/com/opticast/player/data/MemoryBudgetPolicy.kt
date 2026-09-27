package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Balanced for low RAM 32-bit 3GB - previous fast builds used 16/32, 6/16 too small, 32/96 too large for low RAM causing GC jank. */
internal fun imageMemoryBudgetBytes(lowRam: Boolean): Int = (if (lowRam) 16 else 48) * 1024 * 1024

/** Disk cache budget for Coil - balanced for offline posters, not too large for low RAM */
internal fun imageDiskBudgetBytes(lowRam: Boolean): Long = (if (lowRam) 96 else 192) * 1024 * 1024L

/** Poster download concurrency - single on low-RAM to avoid competing with UI, 2 on normal */
internal fun posterDownloadConcurrency(lowRam: Boolean): Int = if (lowRam) 1 else 2

/** MediaStore scan should filter tiny clips - keep 60s minimum */
internal const val MIN_VIDEO_DURATION_MS = 60_000L
