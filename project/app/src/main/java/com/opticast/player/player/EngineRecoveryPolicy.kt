package com.opticast.player.player

/** Local mpv by default; network transports/authentication remain on Media3. */
fun initialPlaybackEngine(preference: String, scheme: String?, remembered: String? = null, memoryEnabled: Boolean = true): String =
    if (preference == "media3" || scheme !in setOf(null, "file", "content")) "media3"
    else if (memoryEnabled && remembered in setOf("mpv", "media3")) remembered ?: "mpv" else "mpv"

/** One-way recovery for this video request. Media3 errors never bounce back to mpv. */
fun shouldFallbackToMedia3(engine: String, alreadyTried: Boolean): Boolean =
    engine == "mpv" && !alreadyTried

/** Allow modest container-duration inaccuracies at the end of a file. */
fun endedUnexpectedly(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0 && positionMs >= 0 && durationMs - positionMs > maxOf(5_000L, durationMs / 50)
