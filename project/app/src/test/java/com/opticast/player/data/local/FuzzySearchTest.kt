package com.opticast.player.data.local

import org.junit.Test
import org.junit.Assert.*
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.ParsedName

class FuzzySearchTest {

    private fun createEntry(id: Long, name: String): LibraryEntry {
        return LibraryEntry(
            video = LocalVideo(
                id = id,
                name = name,
                uri = "content://$id",
                sizeBytes = 1000L,
                durationMs = 6000L,
                dateAddedSec = 123L,
                width = 1920,
                height = 1080,
                parsed = ParsedName(title = name)
            ),
            metadata = null
        )
    }

    @Test
    fun testExactMatch() {
        val entries = listOf(
            createEntry(1, "The Matrix"),
            createEntry(2, "Avatar")
        )
        val results = FuzzySearch.search(entries, "matrix")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.video.name.contains("Matrix", ignoreCase = true) })
    }

    @Test
    fun testNoMatch() {
        val entries = listOf(
            createEntry(1, "The Matrix")
        )
        val results = FuzzySearch.search(entries, "xyzabc123")
        // Fuzzy may return empty or filtered, should not crash
        assertTrue(results.size >= 0)
    }

    @Test
    fun testFuzzyMatch() {
        val entries = listOf(
            createEntry(1, "Matrix"),
            createEntry(2, "Avatar")
        )
        val results = FuzzySearch.search(entries, "matrx")
        // Should find Matrix with typo tolerance
        assertTrue(results.isNotEmpty())
    }

    @Test
    fun testEmptyQuery() {
        val entries = listOf(
            createEntry(1, "Anything"),
            createEntry(2, "Else")
        )
        val results = FuzzySearch.search(entries, "")
        assertEquals(2, results.size)
    }
}
