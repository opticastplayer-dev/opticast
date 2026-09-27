package com.opticast.player.ui.screens

import java.util.Locale

/** Public labels only: never index saved credentials, file paths or account values. */
internal val settingsSearchIndex = linkedMapOf(
    "Appearance" to "theme cast midnight ocean wallpaper material you device colors colours library grid compact medium comfortable posters",
    "Playback engine" to "mpv ffmpeg media3 default open with share external player fallback remember successful engine memory clear reset smaller local playback buffer automatic adaptive fallback trial cache forward backward",
    "Audio & subtitle tracks" to "automatic preferred audio language embedded subtitle forced full off container default commentary english chichewa french spanish portuguese german italian japanese korean chinese arabic hindi russian",
    "Player layout" to "player controls customize progress bar thick gradient hidden buttons library home return chapters sleep timer aspect ratio lock audio only subtitles playback info speed",
    "Gestures" to "player controls double tap seek skip seconds swipe volume brightness hold fast forward speed",
    "Playback behaviour" to "start landscape rotation screen on sleep preserve voice pitch speed resume restart auto play next episode autoplay audio only sound picture",
    "Sound" to "audio boost volume equalizer eq preset flat cinema movie music dialogue speech headphones night bass treble media3",
    "Data & performance" to "tweaks save mobile data artwork posters wifi wi fi performance refresh rate battery 60 hz restart memory ram diagnostic snapshot pss native heap graphics",
    "API keys" to "providers metadata tmdb opensubtitles subdl omdb fanart key account",
    "Subtitle languages" to "download languages english french spanish german italian portuguese dutch russian ukrainian polish czech slovak hungarian romanian bulgarian serbian croatian slovenian greek turkish arabic hebrew persian hindi bengali tamil telugu malayalam urdu chinese japanese korean vietnamese thai indonesian malay swedish norwegian danish finnish icelandic estonian latvian lithuanian afrikaans swahili chichewa zulu",
    "Auto subtitles" to "download subtitles during scan automatically opensubtitles quota",
    "Excluded folders" to "scan exclude folders path camera screen recordings screenshots downloads telegram videos",
    "Network libraries" to "network smb webdav http https server source discover discovery local lan connect saved check",
    "Storage & backups" to "storage cache free space watched videos rename tidy backup export import restore settings watch state",
    "Library maintenance" to "library clear all saved metadata matches reset delete",
    "File naming help" to "how name files naming guide movie tv show episode season anime identify match rename",
    "Check for updates" to "update installed version release manual check whats new changelog",
    "About" to "about version legal licences licenses notices credits attribution tmdb providers device android model ram memory support",
    "Contact & support" to "developer email contact support help",
)

internal fun settingsSearchMatches(title: String, query: String): Boolean {
    val words = query.lowercase(Locale.ROOT).trim().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val group = settingsGroups.entries.firstOrNull { title in it.value }?.key.orEmpty()
    val searchable = (title + " " + settingsSearchIndex[title].orEmpty() + " " + group).lowercase(Locale.ROOT)
    return words.all { searchable.contains(it) }
}
internal fun matchingSettingsSections(query: String): List<String> = settingsSearchIndex.keys.filter { settingsSearchMatches(it, query) }

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
    "Playback & Controls" to listOf("Playback engine", "Playback behaviour", "Gestures"),
    "File Management" to listOf("Excluded folders", "Network libraries", "Storage & backups", "Library maintenance", "File naming help"),
    "Media Settings" to listOf("Audio & subtitle tracks", "Subtitle languages", "Auto subtitles", "Sound"),
    "Advanced & About" to listOf("Data & performance", "API keys", "Check for updates", "About", "Contact & support"),
)
internal fun visibleSettingsGroups(query: String): List<String> = settingsGroups.filterValues { titles ->
    titles.any { settingsSearchMatches(it, query) }
}.keys.toList()

internal fun settingsCategorySubtitle(title: String): String = when (title) {
    "Player layout" -> "Customize player buttons and progress bar"
    "Gestures" -> "Double tap, swipe and press-and-hold controls"
    "Playback engine" -> "Built-in player, fallback and local buffer"
    "Playback behaviour" -> "Orientation, screen and episode playback"
    "Excluded folders" -> "Choose folders to leave out of scans"
    "Network libraries" -> "Saved sources and local-network discovery"
    "Storage & backups" -> "Cached files, space and backups"
    "Library maintenance" -> "Manage saved metadata"
    "Audio & subtitle tracks" -> "Preferred languages and automatic track choices"
    "Auto subtitles" -> "Subtitle downloads during scans"
    "Sound" -> "Audio boost, equalizer and dialogue"
    "About" -> "Version, licences and device information"
    else -> ""
}
