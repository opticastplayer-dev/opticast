package com.opticast.player.player

import kotlin.math.abs

/**
 * A deliberately conservative estimate of how far a subtitle file is out of step
 * with its video.
 *
 * What this can honestly do: read the subtitle's own timing and compare it with
 * the length of the video. Subtitles that start or finish at the wrong moment
 * (wrong release, wrong cut, ads stitched on the front or missing from the file)
 * show up as a subtitle span that does not fit the runtime, and the difference is
 * exactly the shift needed. That case is common and this fixes it.
 *
 * What this cannot do: hear the audio. Subtitles that sit *inside* the video's
 * runtime but are uniformly late cannot be detected without decoding the
 * soundtrack, so nothing is applied for them - the manual nudge stays the tool
 * for that, and the note says so instead of pretending.
 */
object SubtitleSync {

    enum class Quality {
        /** Timing already matches: no shift worth applying. */
        FITS,

        /** A real mismatch, small enough to trust. */
        LIKELY,

        /** A real mismatch, large enough that it is worth a look. */
        ROUGH,

        /** Not a delay at all - the file runs at a different rate. */
        RATE,

        /** Too far off to be the same cut of the film. */
        UNKNOWN,
    }

    data class Estimate(
        val offsetMs: Long,
        val quality: Quality,
        val note: String,
    )

    /** 25 / 23.976 = 1.0427, and the reverse, both with a small tolerance. */
    private val RATE_RATIOS = listOf(1.0427, 0.9590)
    private const val RATE_TOLERANCE = 0.012

    /** Anything under this is noise from the last line's display time. */
    private const val SLACK_MS = 1_200L

    /** Above this, it is probably a different cut of the film. */
    private const val ROUGH_LIMIT_MS = 150_000L

    fun estimate(cues: List<SubtitleCue>, durationMs: Long): Estimate? {
        if (cues.isEmpty() || durationMs <= 0L) return null
        val lastEnd = cues.maxOf { it.endMs }
        if (lastEnd <= 0L) return null

        val drift = lastEnd - durationMs
        if (abs(drift) <= SLACK_MS) {
            return Estimate(
                0L,
                Quality.FITS,
                "Subtitle timing already fits the video length (last line ends " +
                    signed(lastEnd - durationMs) + ") - nothing to shift.",
            )
        }

        // A length difference of about 4% is a frame-rate mismatch: 23.976 fps
        // material played against 25 fps subtitles (or the reverse). Shifting it
        // would line up the first line and throw out the last, so it is refused.
        val ratio = durationMs.toDouble() / lastEnd.toDouble()
        if (RATE_RATIOS.any { abs(ratio - it) <= RATE_TOLERANCE }) {
            val percent = abs((ratio - 1.0) * 100.0)
            val direction = if (ratio > 1.0) "shorter" else "longer"
            return Estimate(
                0L,
                Quality.RATE,
                "The subtitle timing runs about " + "%.1f".format(percent) + "% " + direction +
                    " than the video over its whole length - that is a frame-rate difference " +
                    "(23.976 against 25 fps), not a delay, so a single shift would drift again " +
                    "by the end. A release for this copy of the film will fit; the manual " +
                    "nudge can still tidy the opening.",
            )
        }

        val offsetMs = -drift
        return when {
            abs(offsetMs) <= 20_000L -> Estimate(
                offsetMs,
                Quality.LIKELY,
                "Last line lands " + signed(drift) + " past the end of the video - shifting " +
                    "the subtitles " + signed(offsetMs) + " should line them up.",
            )
            abs(offsetMs) <= ROUGH_LIMIT_MS -> Estimate(
                offsetMs,
                Quality.ROUGH,
                "Subtitles finish " + signed(drift) + " from the end of the video. The " +
                    "estimate is " + signed(offsetMs) + "; check the opening minutes after " +
                    "applying it.",
            )
            else -> Estimate(
                0L,
                Quality.UNKNOWN,
                "Subtitles do not match this video's length (" + signed(drift) + " out, about " +
                    (abs(offsetMs) / 60_000) + " minutes). That is probably a different cut of " +
                    "the film, so nothing was applied - try another subtitle.",
            )
        }
    }

    private fun signed(ms: Long): String {
        val sign = if (ms < 0) "-" else "+"
        val total = abs(ms)
        val minutes = total / 60_000
        val seconds = (total % 60_000) / 1000.0
        return if (minutes > 0) {
            "%s%d:%04.1fs".format(sign, minutes, seconds)
        } else {
            "%s%.1fs".format(sign, seconds)
        }
    }
}
