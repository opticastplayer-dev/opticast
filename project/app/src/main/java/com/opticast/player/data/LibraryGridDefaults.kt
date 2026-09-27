package com.opticast.player.data

internal const val DEFAULT_LIBRARY_GRID = "medium"

/** Respect saved choices; installations with no choice use the medium grid. */
internal fun resolvedLibraryGrid(saved: String?): String = when (saved) {
    "compact", "medium", "comfortable" -> saved
    else -> DEFAULT_LIBRARY_GRID
}
