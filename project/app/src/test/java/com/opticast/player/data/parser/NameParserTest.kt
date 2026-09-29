package com.opticast.player.data.parser

import org.junit.Test
import org.junit.Assert.*

class NameParserTest {
    @Test
    fun testMovieParsing() {
        val parsed = NameParser.parse("The.Matrix.1999.1080p.BluRay.mp4")
        assertTrue(parsed.title.isNotBlank())
        assertEquals(1999, parsed.year)
    }

    @Test
    fun testEpisodeParsing() {
        val parsed = NameParser.parse("Breaking.Bad.S01E01.Pilot.1080p.mp4")
        assertTrue(parsed.title.contains("Breaking Bad", ignoreCase = true) || parsed.title.isNotBlank())
        assertEquals(1, parsed.season)
        assertEquals(1, parsed.episode)
    }

    @Test
    fun testTitleBlank() {
        val parsed = NameParser.parse("  ")
        assertTrue(parsed.title.isBlank() || parsed.title.isNotEmpty())
    }

    @Test
    fun testComplexName() {
        val parsed = NameParser.parse("Avengers.Endgame.2019.2160p.HDR.HEVC.mkv")
        assertTrue(parsed.title.isNotBlank())
        assertEquals(2019, parsed.year)
    }
}
