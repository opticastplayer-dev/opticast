package com.opticast.player.data.local

import com.opticast.player.data.model.LibraryEntry
import kotlinx.serialization.Serializable

@Serializable
data class SmartRule(val id: String, val name: String, val type: String = "all", val unwatched: Boolean = false,
    val recentDays: Int? = null, val maxMinutes: Int? = null, val genre: String = "", val yearFrom: Int? = null, val yearTo: Int? = null)

fun smartRuleMatches(rule: SmartRule, entry: LibraryEntry, watched: Boolean, nowSec: Long): Boolean {
    val show = entry.video.isEpisode || entry.metadata?.type == "tv"
    if (rule.type == "movies" && show || rule.type == "tv" && !show || rule.unwatched && watched) return false
    if (rule.recentDays != null && (entry.video.dateAddedSec <= 0 || entry.video.dateAddedSec < nowSec-rule.recentDays.toLong()*86400)) return false
    if (rule.maxMinutes != null && (entry.video.durationMs <= 0 || entry.video.durationMs >= rule.maxMinutes.toLong()*60000)) return false
    if (rule.genre.isNotBlank() && entry.metadata?.genres?.none { it.equals(rule.genre.trim(),true) } != false) return false
    if (rule.yearFrom != null && (entry.metadata?.year ?: Int.MIN_VALUE) < rule.yearFrom) return false
    if (rule.yearTo != null && (entry.metadata?.year ?: Int.MAX_VALUE) > rule.yearTo) return false
    return true
}

@Serializable data class SkipSpan(val startMs: Long, val endMs: Long)
@Serializable data class SkipProfile(val enabled: Boolean = true, val intro: SkipSpan? = null, val credits: SkipSpan? = null)
fun validSkipSpan(span: SkipSpan, durationMs: Long): Boolean = span.startMs >= 0 && span.endMs > span.startMs && (durationMs <= 0 || span.endMs <= durationMs)
fun skipPrompt(profile: SkipProfile, positionMs: Long, durationMs: Long): Pair<String,Long>? {
    if (!profile.enabled || durationMs <= 0) return null
    return listOf("Skip intro" to profile.intro, "Skip credits" to profile.credits).firstNotNullOfOrNull { (label,span) ->
        span?.takeIf { validSkipSpan(it,durationMs) && positionMs >= it.startMs && positionMs < it.endMs }?.let { label to it.endMs }
    }
}
fun parseSkipTime(text: String): Long? {
    val p=text.trim().split(':'); if(p.size !in 1..3) return null
    val values=p.map { it.toLongOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    if(values.drop(1).any { it >= 60 }) return null
    var seconds=0L
    for(v in values) { if(v > 604800 || seconds > 604800) return null; seconds=seconds*60+v }
    return seconds.takeIf { it <= 604800 }?.times(1000)
}
fun formatSkipTime(ms: Long?) = ms?.let { "%d:%02d".format(it/60000,(it/1000)%60) } ?: ""

fun diagnosticAdvice(code: Int?): String = when(code) {
    in 2000..2999 -> "Check storage permission/file availability or network access. Reconnect the source and retry. Do not delete your Library entry."
    in 3000..3999 -> "The container may be unsupported or damaged. Try the existing external-player option or another known-good copy."
    in 4000..4999 -> "The device decoder may not support this codec/profile. Try the existing external-player option; a different file encoding may be needed."
    in 5000..5999 -> "Check the selected audio track and output device. Try another audio track or the existing external-player option."
    in 6000..6999 -> "This content requires DRM support/authorization. Use the authorized provider app; OptiCast cannot bypass DRM."
    null -> "No captured decoder error. If playback buffers, check the source/network; if audio fails, try another audio track or output device."
    else -> "Retry playback after checking the source. If it still fails, use the existing external-player option and share this report."
}
