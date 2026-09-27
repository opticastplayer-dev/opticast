package com.opticast.player.player

import java.util.Locale

/** Engine-independent embedded-track policy. Null means leave the engine's default alone. */
data class PreferenceTrack(
    val key: String, val language: String?, val label: String?,
    val forced: Boolean = false, val default: Boolean = false, val commentary: Boolean = false,
)

fun normalizedTrackLanguage(value: String?): String {
    val base = value.orEmpty().trim().lowercase(Locale.ROOT).replace('_', '-').substringBefore('-')
    if (base.isBlank() || base == "und") return ""
    return when (base) {
        "eng" -> "en"; "fra", "fre" -> "fr"; "deu", "ger" -> "de"; "spa" -> "es"
        "por" -> "pt"; "ita" -> "it"; "jpn" -> "ja"; "kor" -> "ko"; "zho", "chi" -> "zh"
        "ara" -> "ar"; "hin" -> "hi"; "rus" -> "ru"; "nya" -> "ny"; else -> base
    }
}
fun isCommentaryTrack(track: PreferenceTrack): Boolean = track.commentary ||
    Regex("(?i)\\b(commentary|director['’]?s? comments?)\\b").containsMatchIn(track.label.orEmpty())

fun preferredAudioTrack(tracks: List<PreferenceTrack>, language: String, avoidCommentary: Boolean,
    override: String? = null): PreferenceTrack? {
    tracks.firstOrNull { it.key == override }?.let { return it }
    val lang = normalizedTrackLanguage(language)
    val preferred = tracks.filter { lang.isNotEmpty() && normalizedTrackLanguage(it.language) == lang }
    val pool = preferred.ifEmpty { tracks }
    val clean = if (avoidCommentary) pool.filterNot(::isCommentaryTrack) else pool
    val candidates = clean.ifEmpty { pool }
    // No requested language and no commentary to avoid: keep container/engine default.
    if (preferred.isEmpty() && (!avoidCommentary || tracks.none(::isCommentaryTrack))) return null
    return candidates.firstOrNull { it.default } ?: candidates.firstOrNull()
}

/** OFF is an explicit disable, null retains engine default; missing saved tracks use global rules. */
const val TRACK_OFF = "off"
fun preferredSubtitleTrack(tracks: List<PreferenceTrack>, language: String, mode: String,
    override: String? = null): String? {
    if (override == TRACK_OFF) return TRACK_OFF
    tracks.firstOrNull { it.key == override }?.let { return it.key }
    if (mode == "default") return null
    if (mode == "off") return TRACK_OFF
    val lang = normalizedTrackLanguage(language)
    val candidates = tracks.filter { (lang.isEmpty() || normalizedTrackLanguage(it.language) == lang) &&
        if (mode == "forced") it.forced else !it.forced }
    return (candidates.firstOrNull { it.default } ?: candidates.firstOrNull())?.key ?: TRACK_OFF
}

/** Do not turn a startup event or a seek into evidence of successful playback. */
class EngineSuccessWindow {
    private var lastPosition: Long? = null
    private var playingMs = 0L
    fun observe(position: Long, playing: Boolean, intervalMs: Long = 1000L): Boolean {
        val delta = lastPosition?.let { position - it } ?: 0L
        lastPosition = position
        if (!playing || delta <= 0 || delta > intervalMs * 4) playingMs = 0L
        else playingMs += intervalMs
        return playingMs >= 10_000L
    }
}
