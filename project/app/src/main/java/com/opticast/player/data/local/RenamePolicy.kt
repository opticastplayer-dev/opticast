package com.opticast.player.data.local

import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.ParsedName
import com.opticast.player.data.parser.NameParser
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
internal data class RenameClue(val title: String = "", val year: Int? = null,
    val season: Int? = null, val episode: Int? = null, val source: String = "") {
    fun parsed() = ParsedName(title, year, season, episode)
}

internal data class RenameSuggestion(val entry: LibraryEntry, val proposedName: String?, val reason: String)

private val siteTag = Regex("(?i)(?:www[._-])?(?:tfpdl|yts(?:[._-](?:mx|am|ag))?|yify|rarbg)(?:[._-](?:com|net|org))?")
private val genericTitles = setOf("download", "downloads", "video", "videos", "movie", "movies", "tv", "tv shows", "series", "episodes", "media", "telegram", "unknown", "untitled")
internal fun credibleRenameTitle(title: String): Boolean {
    val value = title.trim()
    if (value.length < 2 || value.lowercase(Locale.ROOT) in genericTitles) return false
    if (Regex("(?i)^(?:season|s|episode|e)\\s*\\d+$").matches(value)) return false
    if (Regex("(?i)^[a-z]{1,8}[-_]?\\d{2,}$").matches(value)) return false
    if (Regex("(?i)^[a-f0-9]{12,}$").matches(value)) return false
    if (value.contains("www.", true) || value.contains("://") || value.contains("encoded by", true)) return false
    return value.any { it.isLetter() }
}

internal fun requiresRenameClue(name: String): Boolean {
    val title = NameParser.parse(name.replace(siteTag, " ").trim(' ', '-', '_', '.')).title.trim()
    return filenameClue(name).title.isBlank() && (siteTag.containsMatchIn(name) ||
        Regex("(?i)^[a-z]{1,8}[-_]?\\d{2,}$").matches(title) ||
        Regex("(?i)^[a-f0-9]{12,}$").matches(title) || title.lowercase(Locale.ROOT) in genericTitles)
}

internal fun filenameClue(name: String): RenameClue {
    val cleaned = name.replace(siteTag, " ").replace(Regex("^[ ._\\-\\[\\]()]+"), "")
    val raw = NameParser.parse(cleaned)
    val parsed = raw.copy(title = raw.title.trim().trim(' ' , '(', ')', '[', ']', '-'))
    return if (credibleRenameTitle(parsed.title)) RenameClue(parsed.title.trim(), parsed.year, parsed.season, parsed.episode, "Filename cleanup") else RenameClue()
}

internal fun chooseRenameClue(name: String, relativePath: String, embedded: String?): RenameClue {
    filenameClue(name).takeIf { it.title.isNotBlank() }?.let { return it }
    embedded?.takeIf { it.isNotBlank() }?.let {
        val parsed = filenameClue(if (it.substringAfterLast('.', "").lowercase(Locale.ROOT) in setOf("mkv","mp4","avi","webm","mov","m4v")) it else "$it.mkv")
        if (parsed.title.isNotBlank()) return parsed.copy(source = "Embedded title — verify before renaming")
    }
    relativePath.trim('/').split('/').asReversed().take(3).forEach { folder ->
        val parsed = filenameClue("$folder.mkv")
        if (parsed.title.isNotBlank()) {
            val episode = NameParser.parse(name)
            return parsed.copy(season = episode.season, episode = episode.episode, source = "Folder title — verify before renaming")
        }
    }
    return RenameClue()
}

internal fun originalExtension(name: String): String = name.substringAfterLast('.', "").takeIf {
    name.contains('.') && it.matches(Regex("[A-Za-z0-9]{1,8}"))
}?.let { ".$it" }.orEmpty()

internal fun safeRenameStem(value: String): String {
    var result = value.replace(Regex("[\\x00-\\x1f\\x7f/\\\\:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").trim().trim('.')
    while (result.toByteArray(Charsets.UTF_8).size > 200) result = result.dropLast(if (result.last().isLowSurrogate()) 2 else 1)
    if (Regex("(?i)^(con|prn|aux|nul|com[1-9]|lpt[1-9])$").matches(result)) result = "_$result"
    return result
}

internal fun renameValidationError(original: String, proposed: String): String? = when {
    proposed.isBlank() || proposed == "." || proposed == ".." -> "Enter a filename."
    proposed != proposed.trim() || proposed.endsWith('.') -> "Remove leading/trailing spaces or trailing dots."
    Regex("[\\x00-\\x1f\\x7f/\\\\:*?\"<>|]").containsMatchIn(proposed) -> "Do not use path separators or reserved filename characters."
    proposed.toByteArray(Charsets.UTF_8).size > 240 -> "Filename is too long."
    originalExtension(original) != originalExtension(proposed) -> "Keep the original file extension: ${originalExtension(original)}"
    proposed.removeSuffix(originalExtension(original)).isBlank() -> "Enter a title before the extension."
    else -> null
}

internal fun renameSuggestion(entry: LibraryEntry, clue: RenameClue): RenameSuggestion? {
    val metadata = entry.metadata
    // Matching already succeeded: do not suggest cosmetic canonicalization.
    val identified = metadata != null && metadata.title.isNotBlank() &&
        (metadata.type != "tv" || ((metadata.seasonNumber ?: entry.video.parsed.season) != null &&
            (metadata.episodeNumber ?: entry.video.parsed.episode) != null))
    val parsed = entry.video.parsed
    val readable = (credibleRenameTitle(parsed.title) && filenameClue(entry.video.name).title.equals(parsed.title, ignoreCase = true)) || parsed.title.matches(Regex("[0-9]{1,4}"))
    if (identified || readable) return null
    val title = metadata?.showTitle?.takeIf { metadata.type == "tv" && it.isNotBlank() }
        ?: metadata?.title?.takeIf { it.isNotBlank() } ?: clue.title
    if (title.isBlank()) return RenameSuggestion(entry, null, "Needs identification: this code does not contain a reliable title.")
    val season = metadata?.seasonNumber ?: entry.video.parsed.season ?: clue.season
    val episode = metadata?.episodeNumber ?: entry.video.parsed.episode ?: clue.episode
    val television = entry.video.isEpisode || metadata?.type == "tv" || (season != null && episode != null)
    if (television && (season == null || episode == null)) return RenameSuggestion(entry, null, "TV title found; season and episode still need confirmation. Enter a complete filename or identify the episode.")
    val year = metadata?.year ?: clue.year
    val stem = if (television) "$title - S%02dE%02d".format(Locale.ROOT, season, episode) +
        (metadata?.episodeName?.takeIf { it.isNotBlank() }?.let { " - $it" } ?: "")
        else title + (year?.let { " ($it)" } ?: "")
    val safe = safeRenameStem(stem)
    if (safe.isBlank()) return RenameSuggestion(entry, null, "Needs identification: no safe title is available.")
    val proposed = safe + originalExtension(entry.video.name)
    if (proposed == entry.video.name) return null
    return RenameSuggestion(entry, proposed, when {
        metadata?.manuallyMatched == true -> "Your confirmed metadata match"
        metadata != null -> "Cached metadata match — check the title before renaming"
        else -> clue.source.ifBlank { "Filename cleanup — verify before renaming" }
    })
}
