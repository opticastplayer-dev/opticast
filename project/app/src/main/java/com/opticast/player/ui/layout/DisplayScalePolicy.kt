package com.opticast.player.ui.layout

import kotlin.math.roundToInt

internal const val MIN_TOUCH_DP = 48
internal fun interactiveRowMinimumDp(count: Int, gap: Int = 0): Int =
    count.coerceAtLeast(0) * MIN_TOUCH_DP + (count - 1).coerceAtLeast(0) * gap.coerceAtLeast(0)
internal fun useCompactLayout(widthDp: Int, heightDp: Int): Boolean = widthDp < 360 || heightDp < 480

private fun readableScale(fontScale: Float): Float =
    if (fontScale.isFinite() && fontScale > 0f) fontScale.coerceAtLeast(1f) else 1f
internal fun adaptiveStatsColumns(widthDp: Float, fontScale: Float): Int = when {
    widthDp / readableScale(fontScale) >= 240f -> 4
    widthDp / readableScale(fontScale) >= 120f -> 2
    else -> 1
}
internal fun adaptiveControlWidthDp(fontScale: Float): Int = (68 * readableScale(fontScale)).roundToInt()
internal fun adaptiveChipHeightDp(fontScale: Float): Int = maxOf(34, (20 * readableScale(fontScale) + 8).roundToInt())
