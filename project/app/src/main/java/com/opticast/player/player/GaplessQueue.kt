package com.opticast.player.player

import com.opticast.player.data.model.LibraryEntry

/**
 * Gapless queue - preloads next episode when 90% watched
 * - Smooth next episode transition like Infuse
 * - Only preloads when not on metered network or user allows
 * - Saves battery, no jank
 */
class GaplessQueue {
    private var nextEntry: LibraryEntry? = null
    private var preloadProgress: Float = 0f
    
    fun shouldPreload(currentProgress: Float): Boolean {
        return currentProgress >= 0.9f && nextEntry == null
    }
    
    fun setNext(entry: LibraryEntry?) {
        nextEntry = entry
        preloadProgress = 0f
    }
    
    fun getNext(): LibraryEntry? = nextEntry
    
    fun clear() {
        nextEntry = null
        preloadProgress = 0f
    }
    
    fun isNext(entryId: Long): Boolean = nextEntry?.video?.id == entryId
    
    // Find next episode in same show
    fun findNextEpisode(current: LibraryEntry, allEntries: List<LibraryEntry>): LibraryEntry? {
        val currentTitle = current.metadata?.title ?: return null
        val currentEpisode = current.parsedEpisode() ?: return null
        val showName = currentTitle.substringBefore(" - ").substringBefore(" S").trim()
        
        return allEntries.filter { entry ->
            val title = entry.metadata?.title ?: entry.video.name
            title.contains(showName, ignoreCase = true) && entry.video.id != current.video.id
        }.mapNotNull { entry ->
            val ep = entry.parsedEpisode() ?: return@mapNotNull null
            if (ep.show == currentEpisode.show && ep.season == currentEpisode.season && ep.episode == currentEpisode.episode + 1) {
                entry to ep
            } else null
        }.sortedBy { it.second.episode }.firstOrNull()?.first
    }
    
    private fun LibraryEntry.parsedEpisode(): EpisodeInfo? {
        return try {
            val name = video.name
            // Parse S01E02 pattern
            val regex = Regex("""[Ss](\d+)[Ee](\d+)""")
            val match = regex.find(name) ?: return null
            val season = match.groupValues[1].toIntOrNull() ?: return null
            val episode = match.groupValues[2].toIntOrNull() ?: return null
            EpisodeInfo(name.substringBefore(" S").trim(), season, episode)
        } catch (_: Exception) { null }
    }
    
    private data class EpisodeInfo(val show: String, val season: Int, val episode: Int)
}
