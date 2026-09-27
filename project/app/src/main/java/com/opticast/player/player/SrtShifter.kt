package com.opticast.player.player

/**
 * Shifts SRT/VTT cue timestamps by a fixed offset. Used for the
 * "subtitle sync" control in the player.
 */
object SrtShifter {

    private val timestamp = Regex("(\\d{2}):(\\d{2}):(\\d{2})[,.](\\d{3})")

    fun shift(content: String, offsetMs: Long): String {
        if (offsetMs == 0L) return content
        return timestamp.replace(content.removePrefix("\uFEFF")) { match ->
            val original = match.groupValues.let { g ->
                g[1].toLong() * 3_600_000 +
                    g[2].toLong() * 60_000 +
                    g[3].toLong() * 1_000 +
                    g[4].toLong()
            }
            val shifted = (original + offsetMs).coerceAtLeast(0)
            val hours = shifted / 3_600_000
            val minutes = (shifted % 3_600_000) / 60_000
            val seconds = (shifted % 60_000) / 1_000
            val millis = shifted % 1_000
            val separator = if (match.value.contains(",")) "," else "."
            "%02d:%02d:%02d%s%03d".format(hours, minutes, seconds, separator, millis)
        }
    }
}
