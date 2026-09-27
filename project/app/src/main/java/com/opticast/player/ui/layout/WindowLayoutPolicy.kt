package com.opticast.player.ui.layout

import kotlin.math.roundToInt

/** Physical pixels only: subtract just the keyboard overlap not already resized away. */
internal fun remainingImeOverlapPx(windowHeight: Int, hostTop: Int, hostHeight: Int, imeBottom: Int): Int {
    if (windowHeight <= 0 || hostHeight <= 0 || imeBottom <= 0) return 0
    val ime = imeBottom.coerceIn(0, windowHeight)
    val keyboardTop = windowHeight.toLong() - ime
    return (hostTop.toLong() + hostHeight - keyboardTop).coerceIn(0L, minOf(hostHeight, ime).toLong()).toInt()
}
internal fun windowPixelsToDp(pixels: Int, density: Float): Int =
    (pixels / density.takeIf { it.isFinite() && it > 0f }.let { it ?: 1f }).roundToInt().coerceAtLeast(1)
