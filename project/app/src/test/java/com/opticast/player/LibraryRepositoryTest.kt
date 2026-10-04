package com.opticast.player

import com.opticast.player.data.repository.FakeLibraryRepository
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.ParsedName
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for library repository - ensures offline-first behavior works
 */
class LibraryRepositoryTest {

    @Test
    fun testSearchFiltersCorrectly() = runTest {
        val repo = FakeLibraryRepository()
        val entries = listOf(
            createEntry(1, "Avengers Endgame"),
            createEntry(2, "Breaking Bad S01E01"),
            createEntry(3, "Avengers Infinity War")
        )
        repo.setEntries(entries)
        
        val results = repo.search("Avengers")
        assertEquals(2, results.size)
        assertTrue(results.all { it.video.name.contains("Avengers") })
    }
    
    @Test
    fun testGetEntryById() = runTest {
        val repo = FakeLibraryRepository()
        val entries = listOf(
            createEntry(1, "Movie 1"),
            createEntry(2, "Movie 2")
        )
        repo.setEntries(entries)
        
        assertNotNull(repo.getEntryById(1))
        assertEquals("Movie 1", repo.getEntryById(1)?.video?.name)
        assertNull(repo.getEntryById(999))
    }
    
    @Test
    fun testEmptySearchReturnsAll() = runTest {
        val repo = FakeLibraryRepository()
        val entries = listOf(
            createEntry(1, "Movie 1"),
            createEntry(2, "Movie 2")
        )
        repo.setEntries(entries)
        
        val results = repo.search("")
        assertEquals(2, results.size)
    }
    
    private fun createEntry(id: Long, name: String): LibraryEntry {
        return LibraryEntry(
            video = LocalVideo(
                id = id,
                name = name,
                uri = "content://media/$id",
                sizeBytes = 1000000L,
                durationMs = 6000000L,
                dateAddedSec = System.currentTimeMillis() / 1000,
                width = 1920,
                height = 1080,
                parsed = ParsedName(title = name, year = null, season = null, episode = null)
            ),
            metadata = null
        )
    }
}
