package com.opticast.player.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.C
import com.opticast.player.data.AppSettings
import kotlin.math.log10

/**
 * Simplified tone and loudness processing - lightweight, no presets.
 * Only dialogue boost + volume boost, no EQ presets for solo maintainability.
 * Uses platform effects (Equalizer, BassBoost, LoudnessEnhancer) fail-soft.
 */
class AudioEffects {

    private var sessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var lastApplied: AppSettings? = null

    fun attach(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        if (sessionId == this.sessionId) return
        release()
        this.sessionId = sessionId
        runCatching { equalizer = Equalizer(0, sessionId).apply { enabled = false } }
        runCatching { bassBoost = BassBoost(0, sessionId).apply { enabled = false } }
        runCatching { loudness = LoudnessEnhancer(sessionId).apply { enabled = false } }
        lastApplied?.let { apply(it) }
    }

    /** Applies user's audio settings - simplified, no presets. */
    fun apply(prefs: AppSettings) {
        lastApplied = prefs
        if (sessionId <= 0) return

        // Dialogue boost: slight mid lift via EQ if enabled
        runCatching {
            val eq = equalizer ?: return@runCatching
            val range = eq.bandLevelRange
            val low = range[0].toInt()
            val high = range[1].toInt()
            for (band in 0 until eq.numberOfBands) {
                val centerHz = eq.getCenterFreq(band.toShort()) / 1000f
                val gainDb = if (prefs.dialogueBoost) {
                    when {
                        centerHz < 200f -> -1f
                        centerHz < 1000f -> 2f
                        centerHz < 4000f -> 3f
                        else -> 0f
                    }
                } else 0f
                val millibels = (gainDb * 100f).toInt().coerceIn(low, high)
                eq.setBandLevel(band.toShort(), millibels.toShort())
            }
            eq.enabled = prefs.dialogueBoost
        }

        // Loudness boost
        runCatching {
            val enhancer = loudness ?: return@runCatching
            val gainPct = prefs.audioBoostPct.coerceIn(100, 400)
            if (gainPct > 100) {
                enhancer.setTargetGain((2000 * log10(gainPct / 100.0)).toInt())
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
