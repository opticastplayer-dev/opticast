package com.opticast.player.ui.screens

import java.util.Locale

/** Public labels only: never index saved credentials, file paths or account values. 
 *  Enhanced index with precise keywords, synonyms and common typos for robust search.
 */
internal val settingsSearchIndex = linkedMapOf(
    "Appearance" to "theme cast midnight ocean dark light system default wallpaper material you device colors colours library grid compact medium comfortable posters list view layout ui appearance style look",
    "Playback" to "default app set as default system settings open with share external player use external vlc mx player keep screen on prevent sleep display awake auto play next episode autoplay continue queue audio boost quiet loud volume 150 percent speech dialogue pitch speed orientation landscape portrait rotation",
    "Gestures" to "player controls swipe left right up down double tap single tap hold fast forward speed customize gesture sensitivity seek volume brightness",
    "Files & Storage" to "scan exclude excluded folders skipped scanning custom path camera dcim screen recordings screenshots downloads telegram whatsapp movies videos storage dashboard cache free space trash deleted library clear metadata matches reset delete backup restore organize",
    "Subtitles" to "automatic preferred audio language embedded subtitle forced full off container default commentary english french spanish chinese japanese korean german download languages auto subtitles during scan opensubtitles subdl quota",
    "Advanced" to "save mobile data saver artwork posters wifi hd high quality low bandwidth providers metadata tmdb opensubtitles subdl key account api token",
    "About" to "about version legal licences licenses notices credits attribution tmdb providers device android model ram memory support update installed version release manual check whats new changelog developer email contact support help github website privacy policy terms",
)

/** Synonyms and common alternative terms for more robust matching */
private val synonymMap = mapOf(
    "theme" to listOf("appearance", "look", "style", "dark", "light"),
    "default" to listOf("system", "open with", "always", "set as"),
    "storage" to listOf("space", "memory", "cache", "files", "dashboard"),
    "exclude" to listOf("skip", "ignore", "hide", "filter"),
    "subtitles" to listOf("subs", "captions", "cc", "text"),
    "audio" to listOf("sound", "volume", "language", "track"),
    "gestures" to listOf("swipe", "tap", "controls", "touch"),
    "update" to listOf("upgrade", "new version", "check"),
)

internal fun settingsSearchMatches(title: String, query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase(Locale.ROOT).trim()
    val words = q.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val group = settingsGroups.entries.firstOrNull { title in it.value }?.key.orEmpty()
    val subtitle = settingsCategorySubtitle(title)
    val searchable = (title + " " + settingsSearchIndex[title].orEmpty() + " " + subtitle + " " + group).lowercase(Locale.ROOT)
    // Expand query with synonyms for more robust matching
    val expandedWords = words.flatMap { word ->
        listOf(word) + (synonymMap[word] ?: emptyList())
    }.distinct()
    return words.all { word ->
        searchable.contains(word) || expandedWords.any { searchable.contains(it) } || 
        searchable.split(" ").any { it.startsWith(word) || word.startsWith(it) }
    }
}

internal fun matchingSettingsSections(query: String): List<String> {
    if (query.isBlank()) return settingsSearchIndex.keys.toList()
    val q = query.lowercase(Locale.ROOT).trim()
    val queryWords = q.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    return settingsSearchIndex.keys
        .map { title ->
            val group = settingsGroups.entries.firstOrNull { title in it.value }?.key.orEmpty()
            val subtitle = settingsCategorySubtitle(title)
            val searchable = (title + " " + settingsSearchIndex[title].orEmpty() + " " + subtitle + " " + group).lowercase(Locale.ROOT)
            val titleLower = title.lowercase(Locale.ROOT)
            val subtitleLower = subtitle.lowercase(Locale.ROOT)
            // Precise scoring: exact match > starts with > contains title > contains subtitle > contains index > word matches > fuzzy
            val score = when {
                titleLower == q -> 0
                titleLower.startsWith(q) -> 1
                subtitleLower.startsWith(q) -> 2
                titleLower.contains(q) -> 3
                subtitleLower.contains(q) -> 4
                searchable.contains(q) -> 5
                else -> {
                    val matchedWords = queryWords.count { word ->
                        searchable.contains(word) || searchable.split(" ").any { it.startsWith(word) }
                    }
                    when {
                        matchedWords == queryWords.size && queryWords.size > 1 -> 6
                        matchedWords >= 1 -> 7 + (queryWords.size - matchedWords)
                        else -> {
                            // Fuzzy: check if query is close to title (typo tolerance)
                            val distance = levenshteinDistance(titleLower, q)
                            if (distance <= 2) 8 else 10
                        }
                    }
                }
            }
            Triple(title, score, queryWords.count { searchable.contains(it) })
        }
        .filter { it.second < 10 }
        .sortedWith(compareBy({ it.second }, { -it.third }, { it.first }))
        .map { it.first }
}

/** Simple Levenshtein distance for typo tolerance */
private fun levenshteinDistance(s1: String, s2: String): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i-1] == s2[j-1]) 0 else 1
            dp[i][j] = minOf(dp[i-1][j] + 1, dp[i][j-1] + 1, dp[i-1][j-1] + cost)
        }
    }
    return dp[s1.length][s2.length]
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
    "Playback" -> "Default app, external player, screen and audio"
    "Files & Storage" -> "Storage dashboard and excluded folders"
    "Subtitles" -> "Audio, tracks and languages"
    "Advanced" -> "Data usage and API keys"
    "About" -> "Version, updates and support"
    else -> ""
}

/** Get precise suggestion text for search results */
internal fun settingsSearchSuggestion(title: String, query: String): String {
    if (query.isBlank()) return settingsCategorySubtitle(title)
    val q = query.lowercase(Locale.ROOT)
    val index = settingsSearchIndex[title].orEmpty().lowercase(Locale.ROOT)
    return when {
        title.lowercase(Locale.ROOT).contains(q) -> "Matches \"$title\""
        settingsCategorySubtitle(title).lowercase(Locale.ROOT).contains(q) -> settingsCategorySubtitle(title)
        index.contains(q) -> {
            // Find matching keyword in index
            val words = index.split(" ").filter { it.contains(q) || q.contains(it) }
            if (words.isNotEmpty()) "Related to ${words.take(2).joinToString(", ")}" else settingsCategorySubtitle(title)
        }
        else -> settingsCategorySubtitle(title)
    }
}
