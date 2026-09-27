package com.opticast.player.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.C
import com.opticast.player.data.AppSettings
import kotlin.math.abs
import kotlin.math.log10

/**
 * Tone and loudness processing attached to the player's audio session.
 *
 * Everything here uses the long-stable platform effects ([Equalizer],
 * [BassBoost], [LoudnessEnhancer]) rather than `DynamicsProcessing`. That is a
 * deliberate trade: `DynamicsProcessing` would give true multiband compression,
 * but its stage-enabling config cannot be constructed from this SDK's public
 * API, and a wrong guess would either throw at runtime or silently do nothing.
 * The effects used here are device-universal and fail soft - if a device has no
 * bass boost, the rest still works.
 *
 * Each effect is created lazily on first use, wrapped in runCatching, and
 * released when the audio session goes away, so a device that refuses any one of
 * them simply gets less processing, never a crash and never silence.
 */
class AudioEffects {

    private var sessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var lastApplied: AppSettings? = null

    /** (Re)builds the effect chain for a new audio session. */
    fun attach(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        if (sessionId == this.sessionId) return
        release()
        this.sessionId = sessionId
        // Created disabled; apply() decides what gets switched on.
        runCatching { equalizer = Equalizer(0, sessionId).apply { enabled = false } }
        runCatching { bassBoost = BassBoost(0, sessionId).apply { enabled = false } }
        runCatching { loudness = LoudnessEnhancer(sessionId).apply { enabled = false } }
        lastApplied?.let { apply(it) }
    }

    /** Applies the user's audio settings. Safe to call on every settings change. */
    fun apply(prefs: AppSettings) {
        lastApplied = prefs
        if (sessionId <= 0) return
        val preset = AudioPreset.forId(prefs.audioPreset)

        // ---- tone curve -------------------------------------------------
        runCatching {
            val eq = equalizer ?: return@runCatching
            val range = eq.bandLevelRange
            val low = range[0].toInt()
            val high = range[1].toInt()
            for (band in 0 until eq.numberOfBands) {
                // Centre frequency comes back in milliHertz.
                val centerHz = eq.getCenterFreq(band.toShort()) / 1000f
                val gainDb = preset.gainAt(centerHz)
                val millibels = (gainDb * 100f).toInt().coerceIn(low, high)
                eq.setBandLevel(band.toShort(), millibels.toShort())
            }
            eq.enabled = abs(preset.bassStrength) > 0 || preset.gains.any { abs(it.second) > 0.5f }
        }

        // ---- bass reinforcement ----------------------------------------
        runCatching {
            val bass = bassBoost ?: return@runCatching
            if (preset.bassStrength > 0 && bass.strengthSupported) {
                bass.setStrength(preset.bassStrength.toShort())
                bass.enabled = true
            } else {
                bass.enabled = false
            }
        }

        // ---- loudness (boost + preset makeup) --------------------------
        runCatching {
            val enhancer = loudness ?: return@runCatching
            val gainPct = prefs.audioBoostPct.coerceIn(100, 400)
            // Presets that push the mids (dialogue) and pull them (night) also
            // change perceived loudness; make up for it here rather than with EQ
            // so quiet recordings stay intelligible.
            val totalPct = (gainPct * preset.makeupFactor).toInt().coerceIn(100, 500)
            if (totalPct > 100) {
                enhancer.setTargetGain((2000 * log10(totalPct / 100.0)).toInt())
                enhancer.enabled = true
            } else {
                enhancer.enabled = false
            }
        }
    }

    fun release() {
        runCatching { equalizer?.enabled = false; equalizer?.release() }
        runCatching { bassBoost?.enabled = false; bassBoost?.release() }
        runCatching { loudness?.enabled = false; loudness?.release() }
        equalizer = null
        bassBoost = null
        loudness = null
        sessionId = C.AUDIO_SESSION_ID_UNSET
    }
}

/**
 * Named tone curves.
 *
 * Gains are given as (frequency Hz, decibels) anchor points and interpolated
 * against the device's real band centres, so a 5-band phone and a 10-band phone
 * both end up with the intended shape instead of blind band-index writes.
 */
enum class AudioPreset(
    val id: String,
    val label: String,
    val blurb: String,
    val gains: List<Pair<Float, Float>>,
    val bassStrength: Int = 0,
    /** Multiplier applied to the volume-boost percentage. */
    val makeupFactor: Float = 1f,
) {
    FLAT(
        id = "flat",
        label = "Flat",
        blurb = "No tone shaping",
        gains = listOf(60f to 0f, 230f to 0f, 910f to 0f, 3600f to 0f, 14000f to 0f),
    ),
    DIALOGUE(
        id = "dialogue",
        label = "Dialogue",
        blurb = "Lifts speech, trims rumble",
        gains = listOf(60f to -4f, 230f to -1f, 910f to 2.5f, 3600f to 4.5f, 14000f to 1f),
        makeupFactor = 1.15f,
    ),
    NIGHT(
        id = "night",
        label = "Night",
        blurb = "Quiet, speech-forward for late viewing",
        gains = listOf(60f to -6f, 230f to 0f, 910f to 3.5f, 3600f to 2f, 14000f to -3f),
        // Deliberately no boost: the point of night mode is to stay quiet while
        // dialogue remains audible.
        makeupFactor = 1f,
    ),
    BASS(
        id = "bass",
        label = "Bass",
        blurb = "Extra low end",
        gains = listOf(60f to 5f, 230f to 3f, 910f to 0f, 3600f to 0f, 14000f to -1f),
        bassStrength = 450,
    ),
    TREBLE(
        id = "treble",
        label = "Treble",
        blurb = "Brighter, more detail",
        gains = listOf(60f to -2f, 230f to 0f, 910f to 1f, 3600f to 4f, 14000f to 5.5f),
    ),
    LOUD(
        id = "loud",
        label = "Loud",
        blurb = "Maximises perceived volume",
        gains = listOf(60f to 4f, 230f to 2f, 910f to 1f, 3600f to 2f, 14000f to 2f),
        bassStrength = 200,
        makeupFactor = 1.35f,
    );

    /** Piecewise-linear gain at [hz], clamped outside the anchor range. */
    fun gainAt(hz: Float): Float {
        val points = gains.sortedBy { it.first }
        if (hz <= points.first().first) return points.first().second
        if (hz >= points.last().first) return points.last().second
        for (i in 0 until points.size - 1) {
            val (f0, g0) = points[i]
            val (f1, g1) = points[i + 1]
            if (hz in f0..f1) {
                val t = if (f1 - f0 <= 0f) 0f else (hz - f0) / (f1 - f0)
                return g0 + (g1 - g0) * t
            }
        }
        return 0f
    }

    companion object {
        fun forId(id: String): AudioPreset = entries.firstOrNull { it.id == id } ?: FLAT
    }
}
