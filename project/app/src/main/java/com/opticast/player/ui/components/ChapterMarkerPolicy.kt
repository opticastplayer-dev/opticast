package com.opticast.player.ui.components

/** Exclude invalid/end ticks, deduplicate and sort without consulting a player or disk. */
internal fun chapterFractions(chapters: List<Long>, start: Float, end: Float): List<Float> {
    val span = end - start
    if (!span.isFinite() || span <= 0f) return emptyList()
    return chapters.map { (it.toDouble() - start) / span }.filter { it > 0.0 && it < 1.0 }
        .map { it.toFloat() }.distinct().sorted()
}
