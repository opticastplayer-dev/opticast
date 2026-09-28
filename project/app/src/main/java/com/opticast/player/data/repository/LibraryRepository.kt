package com.opticast.player.data.repository

import com.opticast.player.data.local.MediaScanner
import com.opticast.player.data.local.MetadataStore
import com.opticast.player.data.local.PosterCache
import com.opticast.player.data.model.LibraryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * Repository pattern for library - separates data from UI
 * - Offline first: returns cached data immediately, then updates from network
 * - Single source of truth for library entries
 * - Easy to test with fake implementation
 * - Reduces work in composables
 */
interface LibraryRepository {
    val entries: StateFlow<List<LibraryEntry>>
    suspend fun refresh()
    suspend fun search(query: String): List<LibraryEntry>
    fun getEntryById(id: Long): LibraryEntry?
}

class LibraryRepositoryImpl(
    private val mediaScanner: MediaScanner,
    private val metadataStore: MetadataStore,
    private val posterCache: PosterCache
) : LibraryRepository {
    
    private val _entries = MutableStateFlow<List<LibraryEntry>>(emptyList())
    override val entries: StateFlow<List<LibraryEntry>> = _entries.asStateFlow()
    
    // Cache for fast access
    private var entriesCache: List<LibraryEntry> = emptyList()
    private var entriesById: Map<Long, LibraryEntry> = emptyMap()
    
    override suspend fun refresh() = withContext(Dispatchers.IO) {
        try {
            val videos = mediaScanner.scan()
            val entries = videos.map { video ->
                com.opticast.player.data.model.LibraryEntry(
                    video = video,
                    metadata = metadataStore.get(video.id)
                )
            }
            // Update cache
            entriesCache = entries
            entriesById = entries.associateBy { it.video.id }
            _entries.value = entries
        } catch (e: Exception) {
            // Offline: keep cached data, don't show empty
            // Log error but don't crash
            e.printStackTrace()
        }
    }
    
    override suspend fun search(query: String): List<LibraryEntry> = withContext(Dispatchers.Default) {
        if (query.isBlank()) return@withContext entriesCache
        val lowerQuery = query.lowercase()
        entriesCache.filter { entry ->
            entry.video.name.lowercase().contains(lowerQuery) ||
            entry.metadata?.displayTitle?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    override fun getEntryById(id: Long): LibraryEntry? {
        return entriesById[id]
    }
}

/**
 * Fake repository for tests and previews
 */
class FakeLibraryRepository : LibraryRepository {
    private val _entries = MutableStateFlow<List<LibraryEntry>>(emptyList())
    override val entries: StateFlow<List<LibraryEntry>> = _entries.asStateFlow()
    
    fun setEntries(entries: List<LibraryEntry>) {
        _entries.value = entries
    }
    
    override suspend fun refresh() {}
    override suspend fun search(query: String): List<LibraryEntry> {
        return _entries.value.filter { it.video.name.contains(query, ignoreCase = true) }
    }
    override fun getEntryById(id: Long): LibraryEntry? {
        return _entries.value.find { it.video.id == id }
    }
}
