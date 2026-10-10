package com.opticast.player.ui.screens

import java.util.Locale

/** Public labels only: never index saved credentials, file paths or account values. */
internal val settingsSearchIndex = linkedMapOf(
    "Appearance" to "theme cast midnight ocean wallpaper material you device colors colours library grid compact medium comfortable posters",
    "Playback" to "mpv ffmpeg media3 default open with share external player fallback",
    "Audio & subtitle tracks" to "automatic preferred audio language embedded subtitle forced full off container default commentary english french spanish chinese",
    "Player layout" to "player controls customize progress bar thick gradient hidden buttons library home return chapters sleep timer aspect ratio lock audio only subtitles playback info speed",
    "Gestures" to "player controls swipe double tap hold fast forward speed customize",
    "Playback behaviour" to "start landscape rotation screen on sleep preserve voice pitch speed resume restart auto play next episode autoplay audio only sound picture network browsing",
    "Sound" to "audio boost volume dialogue speech",
    "Data usage" to "save mobile data artwork posters wifi hd quality",
    "API keys" to "providers metadata tmdb opensubtitles subdl key account",
    "Subtitle languages" to "download languages english french spanish chinese",
    "Auto subtitles" to "download subtitles during scan automatically opensubtitles quota",
    "Excluded folders" to "scan exclude folders path camera screen recordings screenshots downloads telegram videos",
    "Storage" to "storage cache free space trash deleted",
    "Library maintenance" to "library clear all saved metadata matches reset delete",
    "Check for updates" to "update installed version release manual check whats new changelog",
    "About" to "about version legal licences licenses notices credits attribution tmdb providers device android model ram memory support",
    "Contact & support" to "developer email contact support help",
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
    "UI & Appearance" to listOf("Appearance", "Player layout"),
    "Playback & Controls" to listOf("Playback", "Playback behaviour", "Gestures", "Advanced player"),
    "File Management" to listOf("Excluded folders", "Storage", "Library maintenance"),
    "Media Settings" to listOf("Audio & subtitle tracks", "Subtitle languages", "Auto subtitles", "Sound"),
    "Advanced & About" to listOf("Data usage", "API keys", "Check for updates", "About", "Contact & support"),
)
internal fun visibleSettingsGroups(query: String): List<String> = settingsGroups.filterValues { titles ->
    titles.any { settingsSearchMatches(it, query) }
}.keys.toList()

internal fun settingsCategorySubtitle(title: String): String = when (title) {
    "Player layout" -> "Progress bar and player controls"
    "Gestures" -> "Swipe and hold controls"
    "Playback" -> "Engine and external player"
    "Advanced player" -> "Orientation and audio"
    "Playback behaviour" -> "Orientation and playback"
    "Excluded folders" -> "Folders excluded from scans"
    "Storage" -> "Cached files and trash"
    "Library maintenance" -> "Saved metadata"
    "Audio & subtitle tracks" -> "Preferred languages"
    "Auto subtitles" -> "Automatic subtitle downloads"
    "Sound" -> "Audio enhancements"
    "Data usage" -> "Artwork quality"
    "About" -> "Version and device information"
    else -> ""
}
