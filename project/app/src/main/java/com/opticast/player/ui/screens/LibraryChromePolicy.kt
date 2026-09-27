package com.opticast.player.ui.screens

// The adaptive main movie grid already has this outer card inset; do not shrink its posters.
internal const val LibraryPosterInsetDp = 4
internal const val LibraryWordmarkWidthFraction = 0.225f
internal const val LibraryBottomBarAlpha = 0.78f
/** Full-library title totals, independent of transient search/genre filters. */
internal fun countedLibraryTab(label: String, count: Int): String = "$label (${count.coerceAtLeast(0)})"
