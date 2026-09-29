package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Increased for smooth scrolling - 6/16 was too small causing eviction during scroll. 
 * 10/10: Adaptive to memoryClass for ultra low-RAM 1.5GB devices - 24MB for 128MB class, 32 for 192, 48 for 256, 96 for high.
 */
internal fun imageMemoryBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Int {
    val mb = when {
        memoryClass <= 128 -> 24
        memoryClass <= 192 -> 32
        memoryClass <= 256 -> 48
        lowRam -> 48
        else -> 96
    }
    return mb * 1024 * 1024
}

/** Disk cache budget for Coil - increased for offline posters, adaptive */
internal fun imageDiskBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Long {
    val mb = when {
        memoryClass <= 128 -> 64
        memoryClass <= 192 -> 128
        memoryClass <= 256 -> 192
        lowRam -> 128
        else -> 256
    }
    return mb * 1024 * 1024L
}

/** Poster download concurrency - single on low-RAM to avoid competing with UI, adaptive */
internal fun posterDownloadConcurrency(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Int {
    return when {
        memoryClass <= 128 -> 1
        memoryClass <= 192 -> 1
        lowRam -> 1
        else -> 2
    }
}

/** WeakReference pool for LibraryEntry list - holds only IDs in memory, metadata LRU 100 items for 1.5GB devices */
internal const val METADATA_LRU_SIZE = 100
internal const val ENTRY_ID_POOL_SIZE = 500

/** MediaStore scan should filter tiny clips - keep 60s minimum */
internal const val MIN_VIDEO_DURATION_MS = 60_000L
