package com.opticast.player.player

/** Centre transport controls always seek within the current video; never select another file. */
internal fun transportSeekPosition(positionMs: Long, durationMs: Long, forward: Boolean): Long {
    val position = positionMs.coerceAtLeast(0L)
    val target = if (forward) {
        if (position > Long.MAX_VALUE - 10_000L) Long.MAX_VALUE else position + 10_000L
    } else (position - 10_000L).coerceAtLeast(0L)
    return if (durationMs > 0L) target.coerceAtMost(durationMs) else target
}
