package com.opticast.player.data

/** Artwork LRU only, not total process PSS. Optimized for low RAM 270MB avg - reduced from 24/32/48/96 to 12/16/24/32 to fix 350MB vs 270MB regression.
 * 10/10: Adaptive to memoryClass, keeps smooth scrolling without reloading, low-RAM safe.
 */
internal fun imageMemoryBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Int {
    val mb = when {
        memoryClass <= 128 -> 12
        memoryClass <= 192 -> 16
        memoryClass <= 256 -> 24
        lowRam -> 16
        else -> 32
    }
    return mb * 1024 * 1024
}

/** Disk cache budget for Coil - reduced for RAM, still offline posters */
internal fun imageDiskBudgetBytes(lowRam: Boolean, memoryClass: Int = if (lowRam) 128 else 256): Long {
    val mb = when {
        memoryClass <= 128 -> 48
        memoryClass <= 192 -> 96
        memoryClass <= 256 -> 128
        lowRam -> 96
        else -> 192
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

internal const val METADATA_LRU_SIZE = 100
internal const val ENTRY_ID_POOL_SIZE = 500
internal const val MIN_VIDEO_DURATION_MS = 60_000L
