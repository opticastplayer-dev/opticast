package com.opticast.player.player

fun choosePlaybackEngine(remembered: String?, memoryEnabled: Boolean): String =
    if (memoryEnabled && remembered in setOf("mpv", "media3")) remembered ?: "mpv" else "mpv"
