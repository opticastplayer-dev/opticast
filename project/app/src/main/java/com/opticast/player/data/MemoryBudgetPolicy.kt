package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Optimized for low RAM 270MB avg - reduced from 24/32/48/96 to 12/16/24/32 to fix 350MB vs 270MB regression.
 * 10/10: Adaptive to memoryClass, keeps smooth scrolling without reloading, low-RAM safe.
 */
internal fun imageMemoryBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Int {
    val mb = when {
        memoryClass <= 128 -> 8
        memoryClass <= 192 -> 12
        memoryClass <= 256 -> 16
        lowRam -> 12
        else -> 20
    }
    return mb * 1024 * 1024
}

/** Disk cache budget for Coil - reduced for RAM, still offline posters */
internal fun imageDiskBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Long {
    val mb = when {
        memoryClass <= 128 -> 32
        memoryClass <= 192 -> 48
        memoryClass <= 256 -> 64
        lowRam -> 48
        else -> 96
    }
    return mb * 1024 * 1024L
}

internal fun posterDownloadConcurrency(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Int {
    return when {
        memoryClass <= 128 -> 1
        memoryClass <= 192 -> 1
        lowRam -> 1
        else -> 2
    }
}

internal const val METADATA_LRU_SIZE = 30
internal const val ENTRY_ID_POOL_SIZE = 200
internal const val MIN_VIDEO_DURATION_MS = 60_000L
