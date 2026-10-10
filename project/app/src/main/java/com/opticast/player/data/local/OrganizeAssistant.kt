package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.model.LibraryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FileHealthReport(
    val messyNames: List<FileSuggestion>,
    val duplicates: List<DuplicateGroup>,
    val emptyFolders: List<String>,
    val misplacedMovies: List<FileSuggestion>,
    val misplacedShows: List<FileSuggestion>,
    val totalIssues: Int
)

@Serializable
data class FileSuggestion(
    val id: String,
    val beforePath: String,
    val afterPath: String,
    val reason: String,
    val type: String, // rename, move_movie, move_show, duplicate
    val size: Long,
    val checked: Boolean = false
)

@Serializable
data class DuplicateGroup(
    val files: List<String>,
    val size: Long,
    val hash: String
)

@Serializable
data class OrganizeLog(
    val timestamp: Long,
    val operations: List<OrganizeOperation>
)

@Serializable
data class OrganizeOperation(
    val from: String,
    val to: String,
    val type: String,
    val success: Boolean
)

class OrganizeAssistant(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val logFile: File by lazy {
        File(context.filesDir, "organize_log.json")
    }
    private val ignoreFile: File by lazy {
        File(context.filesDir, "organize_ignore.json")
    }

    fun scanLibrary(entries: List<LibraryEntry>): FileHealthReport {
        val messyNames = mutableListOf<FileSuggestion>()
        val misplacedMovies = mutableListOf<FileSuggestion>()
        val misplacedShows = mutableListOf<FileSuggestion>()
        val emptyFolders = mutableListOf<String>()

        val ignoreList = getIgnoreList()

        entries.forEach { entry ->
            if (entry.path in ignoreList) return@forEach

            // Check messy names
            if (isMessyName(entry.name)) {
                val cleanName = cleanFileName(entry.name)
                if (cleanName != entry.name) {
                    messyNames.add(
                        FileSuggestion(
                            id = "${entry.id}_rename",
                            beforePath = entry.path,
                            afterPath = File(File(entry.path).parent, cleanName).absolutePath,
                            reason = "Clean filename",
                            type = "rename",
                            size = File(entry.path).length()
                        )
                    )
                }
            }

            // Check misplaced movies (in Download but looks like movie)
            if (entry.path.contains("/Download/", ignoreCase = true) && !entry.isShow) {
                if (isMovieFile(entry.name)) {
                    val afterPath = "/Movies/${getMovieFolderName(entry.name)}/${cleanFileName(entry.name)}"
                    misplacedMovies.add(
                        FileSuggestion(
                            id = "${entry.id}_move_movie",
                            beforePath = entry.path,
                            afterPath = afterPath,
                            reason = "Movie in Download, move to Movies",
                            type = "move_movie",
                            size = File(entry.path).length()
                        )
                    )
                }
            }

            // Check misplaced shows (S01E01 pattern but not in TV Shows folder)
            if (entry.isShow && !entry.path.contains("/TV Shows/", ignoreCase = true)) {
                val seasonEpisode = extractSeasonEpisode(entry.name)
                if (seasonEpisode != null) {
                    val showName = getShowName(entry.name)
                    val afterPath = "/TV Shows/$showName/Season ${seasonEpisode.first.toString().padStart(2, '0')}/$showName S${seasonEpisode.first.toString().padStart(2, '0')}E${seasonEpisode.second.toString().padStart(2, '0')}${File(entry.path).extension.let { if (it.isNotBlank()) ".$it" else "" }}"
                    misplacedShows.add(
                        FileSuggestion(
                            id = "${entry.id}_move_show",
                            beforePath = entry.path,
                            afterPath = afterPath,
                            reason = "TV show detected ${seasonEpisode.first}x${seasonEpisode.second}",
                            type = "move_show",
                            size = File(entry.path).length()
                        )
                    )
                }
            }
        }

        // Check empty folders (simplified)
        // This would need actual folder scanning

        return FileHealthReport(
            messyNames = messyNames,
            duplicates = emptyList(), // Would need hash comparison
            emptyFolders = emptyFolders,
            misplacedMovies = misplacedMovies,
            misplacedShows = misplacedShows,
            totalIssues = messyNames.size + misplacedMovies.size + misplacedShows.size
        )
    }

    private fun isMessyName(name: String): Boolean {
        val messyPatterns = listOf(
            "1080p", "720p", "2160p", "4K", "x264", "x265", "HEVC", "YIFY", "YTS",
            "BluRay", "WEBRip", "WEB-DL", "HDR", "AAC", "AC3"
        )
        return messyPatterns.any { name.contains(it, ignoreCase = true) } && name.contains("_") || name.contains(".") && name.length > 50
    }

    private fun cleanFileName(name: String): String {
        // Simple cleaning: remove common tags, replace dots/underscores with spaces, keep year
        var clean = name
        // Remove extension temporarily
        val ext = File(name).extension
        val nameWithoutExt = if (ext.isNotBlank()) name.removeSuffix(".$ext") else name
        
        // Extract year if present
        val yearRegex = Regex("""\((\d{4})\)|(\d{4})""")
        val yearMatch = yearRegex.find(nameWithoutExt)
        val year = yearMatch?.value?.filter { it.isDigit() }?.takeIf { it.length == 4 }

        // Remove common tags
        val tagsToRemove = listOf("1080p", "720p", "2160p", "4K", "UHD", "BluRay", "WEBRip", "WEB-DL", "HDR", "x264", "x265", "HEVC", "AAC", "AC3", "YIFY", "YTS", "YIFY", "ETRG", "RARBG")
        clean = nameWithoutExt
        tagsToRemove.forEach { tag ->
            clean = clean.replace(tag, "", ignoreCase = true)
        }
        // Replace dots and underscores with spaces, clean multiple spaces
        clean = clean.replace(".", " ").replace("_", " ").replace(Regex("\\s+"), " ").trim()
        
        // Add year back if found
        if (year != null && !clean.contains(year)) {
            clean = "$clean ($year)"
        }
        
        // Add extension back
        return if (ext.isNotBlank()) "$clean.$ext" else clean
    }

    private fun isMovieFile(name: String): Boolean {
        // Simple heuristic: contains year pattern
        return Regex("""\(?\d{4}\)?""").containsMatchIn(name)
    }

    private fun getMovieFolderName(name: String): String {
        val clean = cleanFileName(name)
        return File(clean).nameWithoutExtension
    }

    private fun extractSeasonEpisode(name: String): Pair<Int, Int>? {
        // S01E01 pattern
        val sEregex = Regex("""[Ss](\d+)[Ee](\d+)""")
        val match = sEregex.find(name)
        if (match != null) {
            val season = match.groupValues[1].toIntOrNull() ?: return null
            val episode = match.groupValues[2].toIntOrNull() ?: return null
            return Pair(season, episode)
        }
        // 1x01 pattern
        val xRegex = Regex("""(\d+)x(\d+)""")
        val xMatch = xRegex.find(name)
        if (xMatch != null) {
            val season = xMatch.groupValues[1].toIntOrNull() ?: return null
            val episode = xMatch.groupValues[2].toIntOrNull() ?: return null
            return Pair(season, episode)
        }
        return null
    }

    private fun getShowName(name: String): String {
        // Extract show name before S01E01
        val sEregex = Regex("""(.+?)[Ss]\d+[Ee]\d+""")
        val match = sEregex.find(name)
        return match?.groupValues?.get(1)?.trim()?.replace(".", " ")?.replace("_", " ") ?: "Unknown Show"
    }

    fun getIgnoreList(): Set<String> {
        return try {
            if (!ignoreFile.exists()) return emptySet()
            json.decodeFromString<Set<String>>(ignoreFile.readText())
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun addToIgnore(path: String) {
        try {
            val current = getIgnoreList().toMutableSet()
            current.add(path)
            ignoreFile.writeText(json.encodeToString(kotlinx.serialization.builtins.SetSerializer(kotlinx.serialization.builtins.serializer<String>()), current))
        } catch (_: Exception) {
        }
    }

    fun saveLog(operations: List<OrganizeOperation>) {
        try {
            val log = OrganizeLog(System.currentTimeMillis(), operations)
            val currentLogs = try {
                if (logFile.exists()) json.decodeFromString<List<OrganizeLog>>(logFile.readText()) else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            val newLogs = currentLogs + log
            logFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(OrganizeLog.serializer()), newLogs.takeLast(50)))
        } catch (_: Exception) {
        }
    }

    fun getLogs(): List<OrganizeLog> {
        return try {
            if (!logFile.exists()) return emptyList()
            json.decodeFromString<List<OrganizeLog>>(logFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }
}
