package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Reduced budgets for better performance on all devices. */
internal fun imageMemoryBudgetBytes(lowRam: Boolean): Int = (if (lowRam) 6 else 16) * 1024 * 1024

/** Disk cache budget for Coil - smaller to reduce I/O pressure */
internal fun imageDiskBudgetBytes(lowRam: Boolean): Long = (if (lowRam) 64 else 192) * 1024 * 1024L

/** Poster download concurrency - single on low-RAM to avoid competing with UI */
internal fun posterDownloadConcurrency(lowRam: Boolean): Int = if (lowRam) 1 else 2

/** MediaStore scan should filter tiny clips - keep 60s minimum */
internal const val MIN_VIDEO_DURATION_MS = 60_000L
