package com.opticast.player.data

/** Read-time migration only; never touches playback state or supported explicit choices. */
internal fun resolvedAppTheme(stored: String?): String = when (stored) {
    "midnight", "ocean" -> stored
    else -> "cast"
}
internal fun resolvedProgressStyle(stored: String?): String = when (stored) {
    "thin", "wavy" -> "thick"
    "gradient", "hidden" -> stored
    else -> "thick"
}
