package com.opticast.player.ui.screens

/** Hysteresis prevents jitter; only actual consumed vertical scrolling counts. */
internal class ScrollChromePolicy(private val thresholdPx: Float) {
    private var distance = 0f
    fun consume(deltaY: Float): Boolean? {
        if (!deltaY.isFinite() || deltaY == 0f) return null
        if (distance * deltaY < 0f) distance = 0f
        distance += deltaY
        if (kotlin.math.abs(distance) < thresholdPx.coerceAtLeast(1f)) return null
        val show = distance > 0f
        distance = 0f
        return show
    }
}
