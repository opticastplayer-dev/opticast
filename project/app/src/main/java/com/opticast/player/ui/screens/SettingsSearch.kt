package com.opticast.player.ui.screens

import java.util.Locale

/** Public labels only: never index saved credentials, file paths or account values. */
internal val settingsSearchIndex = linkedMapOf(
    "Appearance" to "theme cast midnight ocean wallpaper material you device colors colours library grid compact medium comfortable posters progress bar thick gradient hidden buttons library home return chapters sleep timer aspect ratio lock audio only subtitles playback info speed",
    "Playback" to "mpv ffmpeg media3 default open with share external player fallback engine orientation auto crop volume normalization audio boost dialogue speech landscape rotation screen on sleep preserve voice pitch speed resume restart auto play next episode autoplay audio only sound picture network browsing",
    "Gestures" to "player controls swipe double tap hold fast forward speed customize",
    "Files & Storage" to "scan exclude folders path camera screen recordings screenshots downloads telegram videos storage cache free space trash deleted library clear all saved metadata matches reset delete",
    "Subtitles" to "automatic preferred audio language embedded subtitle forced full off container default commentary english french spanish chinese download languages auto subtitles during scan opensubtitles quota",
    "Advanced" to "save mobile data artwork posters wifi hd quality providers metadata tmdb opensubtitles subdl key account",
    "About" to "about version legal licences licenses notices credits attribution tmdb providers device android model ram memory support update installed version release manual check whats new changelog developer email contact support help",
)

internal fun settingsSearchMatches(title: String, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase(Locale.ROOT).trim()
    val words = q.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val group = settingsGroups.entries.firstOrNull { title in it.value }?.key.orEmpty()
    val subtitle = settingsCategorySubtitle(title)
    val searchable = (title + " " + settingsSearchIndex[title].orEmpty() + " " + subtitle + " " + group).lowercase(Locale.ROOT)
    return words.all { searchable.contains(it) }
}

internal fun matchingSettingsSections(query: String): List<String> {
    if (query.isBlank()) return settingsSearchIndex.keys.toList()
    val q = query.lowercase(Locale.ROOT).trim()
    return settingsSearchIndex.keys
        .map { title ->
            val group = settingsGroups.entries.firstOrNull { title in it.value }?.key.orEmpty()
            val subtitle = settingsCategorySubtitle(title)
            val searchable = (title + " " + settingsSearchIndex[title].orEmpty() + " " + subtitle + " " + group).lowercase(Locale.ROOT)
            val titleLower = title.lowercase(Locale.ROOT)
            val score = when {
                titleLower == q -> 0
                titleLower.startsWith(q) -> 1
                titleLower.contains(q) -> 2
                searchable.contains(q) -> 3
                else -> {
                    val words = q.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
                    if (words.all { searchable.contains(it) }) 4 else 10
                }
            }
            title to score
        }
        .filter { it.second < 10 }
        .sortedBy { it.second }
        .map { it.first }
}

/** Display-only title casing keeps search identifiers and provider acronyms intact. */
internal fun settingsHeaderTitle(value: String): String = buildString {
    var initial = true
    for (c in value) {
        append(if (initial) c.titlecase(Locale.ROOT) else c.toString())
        initial = c.isWhitespace()
    }
}

internal val settingsGroups = linkedMapOf(
    "UI & Appearance" to listOf("Appearance"),
    "Playback & Controls" to listOf("Playback", "Gestures"),
    "File Management" to listOf("Files & Storage"),
    "Media Settings" to listOf("Subtitles"),
    "Advanced & About" to listOf("Advanced", "About"),
)
internal fun visibleSettingsGroups(query: String): List<String> = settingsGroups.filterValues { titles ->
    titles.any { settingsSearchMatches(it, query) }
}.keys.toList()

internal fun settingsCategorySubtitle(title: String): String = when (title) {
    "Appearance" -> "Theme, grid and player layout"
    "Gestures" -> "Swipe and hold controls"
    "Playback" -> "Engine, behaviour, orientation and audio"
    "Files & Storage" -> "Excluded folders, storage and maintenance"
    "Subtitles" -> "Audio, tracks and languages"
    "Advanced" -> "Data usage and API keys"
    "About" -> "Version, updates and support"
    else -> ""
}
