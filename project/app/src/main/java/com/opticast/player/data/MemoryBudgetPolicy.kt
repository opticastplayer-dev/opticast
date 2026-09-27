package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Increased for smooth scrolling - 6/16 was too small causing eviction during scroll. */
internal fun imageMemoryBudgetBytes(lowRam: Boolean): Int = (if (lowRam) 32 else 96) * 1024 * 1024

/** Disk cache budget for Coil - increased for offline posters */
internal fun imageDiskBudgetBytes(lowRam: Boolean): Long = (if (lowRam) 128 else 256) * 1024 * 1024L

/** Poster download concurrency - single on low-RAM to avoid competing with UI */
internal fun posterDownloadConcurrency(lowRam: Boolean): Int = if (lowRam) 1 else 2

/** MediaStore scan should filter tiny clips - keep 60s minimum */
internal const val MIN_VIDEO_DURATION_MS = 60_000L
