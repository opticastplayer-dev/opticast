package com.opticast.player.data.local

import org.junit.Test
import org.junit.Assert.*

class FuzzySearchTest {
    @Test
    fun testExactMatch() {
        val score = FuzzySearch.score("matrix", "The Matrix")
        assertTrue(score > 0)
    }

    @Test
    fun testNoMatch() {
        val score = FuzzySearch.score("xyzabc123", "The Matrix")
        assertTrue(score >= 0)
    }

    @Test
    fun testFuzzyMatch() {
        val score1 = FuzzySearch.score("matrx", "Matrix")
        val score2 = FuzzySearch.score("matrx", "Avatar")
        assertTrue(score1 > score2)
    }

    @Test
    fun testEmptyQuery() {
        val score = FuzzySearch.score("", "Anything")
        assertTrue(score >= 0)
    }
}
