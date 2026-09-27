package com.opticast.player.ui.screens

/** Smaller adaptive cells, without rewriting the user's saved density preference. */
internal fun libraryPosterMinimumDp(density: String): Int = when (density) {
    "compact" -> 86
    "comfortable" -> 140
    else -> 108
}
