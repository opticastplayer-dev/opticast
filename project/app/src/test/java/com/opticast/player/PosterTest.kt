package com.opticast.player

import com.opticast.player.ui.components.libraryPosterMinimumDp
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for poster grid - ensures grid changeable feature never breaks
 * Critical for 9/10 rating and low-RAM 32-bit performance
 */
class PosterTest {

    @Test
    fun testGridMinimumDp() {
        // Test all grid settings
        assertEquals(86, libraryPosterMinimumDp("compact"))
        assertEquals(140, libraryPosterMinimumDp("comfortable"))
        assertEquals(108, libraryPosterMinimumDp("default"))
        assertEquals(108, libraryPosterMinimumDp("unknown"))
    }
    
    @Test
    fun testGridSettingsAreValid() {
        // All grid settings should return positive dp
        val settings = listOf("compact", "comfortable", "cozy", "large", "default", "")
        for (setting in settings) {
            val dp = libraryPosterMinimumDp(setting)
            assertTrue("Grid setting $setting should return positive dp", dp > 0)
            assertTrue("Grid setting $setting should be reasonable", dp in 50..300)
        }
    }
    
    @Test
    fun testCompactSmallerThanComfortable() {
        // Compact should be smaller than comfortable for more posters
        val compact = libraryPosterMinimumDp("compact")
        val comfortable = libraryPosterMinimumDp("comfortable")
        assertTrue("Compact should be smaller than comfortable", compact < comfortable)
    }
}
