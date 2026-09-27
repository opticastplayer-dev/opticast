package com.opticast.player.player

/** A single timed subtitle line. */
data class SubtitleCue(val startMs: Long, val endMs: Long, val text: String)

/**
 * Minimal subtitle parser used by the dual-subtitle feature.
 *
 * The primary subtitle track is still rendered by Media3's own SubtitleView
 * (untouched, hardware path). This parser exists only so a *second* subtitle can
 * be drawn as a Compose overlay on top of it - the player pipeline never changes.
 *
 * Handles SubRip (.srt), WebVTT (.vtt) and Advanced SubStation (.ass/.ssa) well
 * enough to show text: ASS override blocks are stripped and `\N` is treated as a
 * line break. Anything it cannot understand yields an empty cue list, so the
 * overlay simply stays hidden rather than showing garbage.
 */
object SubtitleCues {

    /** Parses [content], returning cues sorted by start time. */
    fun parse(content: String): List<SubtitleCue> {
        val text = content.removePrefix("\uFEFF")
        val cues = when {
            text.contains("-->") && text.lineSequence().any { it.trim().startsWith("WEBVTT") } ->
                parseBlockFormat(text, vtt = true)
            text.contains("-->") -> parseBlockFormat(text, vtt = false)
            text.contains("[Events]", ignoreCase = true) -> parseAss(text)
            else -> parseBlockFormat(text, vtt = false)
        }
        return cues.filter { it.text.isNotBlank() && it.endMs >= it.startMs }
            .sortedBy { it.startMs }
    }

    /** Text visible at [positionMs], or null. Binary search over the cue list. */
    fun cueAt(cues: List<SubtitleCue>, positionMs: Long): String? {
        var low = 0
        var high = cues.size - 1
        while (low <= high) {
            val mid = (low + high) / 2
            val cue = cues[mid]
            when {
                positionMs < cue.startMs -> high = mid - 1
                positionMs > cue.endMs -> low = mid + 1
                else -> return cue.text
            }
        }
        // Subtitle gaps are normal; also catch a cue that started just after the
        // searched position but is still the one on screen (overlapping cues).
        val nearest = cues.getOrNull(low) ?: return null
        return if (positionMs in nearest.startMs..nearest.endMs) nearest.text else null
    }

    // ------------------------------------------------------------------ srt / vtt

    private fun parseBlockFormat(text: String, vtt: Boolean): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val arrow = line.indexOf("-->")
            if (arrow < 0) {
                i++
                continue
            }
            val start = parseTimecode(line.substring(0, arrow).trim())
            // Strip any VTT cue settings that follow the end timestamp.
            val endPart = line.substring(arrow + 3).trim().split(' ').firstOrNull().orEmpty()
            val end = parseTimecode(endPart)
            i++
            val body = StringBuilder()
            while (i < lines.size && lines[i].isNotBlank()) {
                if (body.isNotEmpty()) body.append('\n')
                body.append(clean(lines[i]))
                i++
            }
            if (start != null && end != null) {
                cues += SubtitleCue(start, end, body.toString().trim())
            }
            i++
        }
        return cues
    }

    private fun parseTimecode(raw: String): Long? {
        // 00:01:02,345  |  00:01:02.345  |  01:02.345
        val cleaned = raw.replace(',', '.').trim()
        val parts = cleaned.split(':')
        if (parts.isEmpty()) return null
        return runCatching {
            var hours = 0L
            var minutes = 0L
            var seconds = 0f
            when (parts.size) {
                3 -> {
                    hours = parts[0].toLong()
                    minutes = parts[1].toLong()
                    seconds = parts[2].toFloat()
                }
                2 -> {
                    minutes = parts[0].toLong()
                    seconds = parts[1].toFloat()
                }
                else -> seconds = parts[0].toFloat()
            }
            (hours * 3_600_000L) + (minutes * 60_000L) + (seconds * 1000f).toLong()
        }.getOrNull()
    }

    // ---------------------------------------------------------------------- ass

    private fun parseAss(text: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var inEvents = false
        for (raw in text.replace("\r\n", "\n").split('\n')) {
            val line = raw.trim()
            if (line.startsWith("[")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            if (!line.startsWith("Dialogue:", ignoreCase = true)) continue
            // Format: Dialogue: layer,start,end,style,name,marginL,marginR,marginV,effect,text
            val parts = line.substringAfter(':').split(',', limit = 10)
            if (parts.size < 10) continue
            val start = parseTimecode(parts[1].trim()) ?: continue
            val end = parseTimecode(parts[2].trim()) ?: continue
            val body = parts[9]
                .replace("\\N", "\n")
                .replace("\\n", "\n")
                .replace("\\h", " ")
            cues += SubtitleCue(start, end, clean(body))
        }
        return cues
    }

    // ------------------------------------------------------------------- shared

    /** Removes markup and decode-able entities so the overlay shows plain text. */
    private fun clean(line: String): String = line
        .replace(Regex("\\{[^}]*}"), "")        // ASS override blocks {\an8}, {\pos(..)}
        .replace(Regex("<[^>]+>"), "")          // SRT/VTT inline tags <i>, <font>, <c>
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+$"), "")
        .trim()
}
