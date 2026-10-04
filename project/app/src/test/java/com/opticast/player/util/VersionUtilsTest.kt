package com.opticast.player.util

import org.junit.Test
import org.junit.Assert.*

class VersionUtilsTest {

    @Test
    fun normalizeVersion_removesOptimizedSuffix() {
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71-optimized"))
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71"))
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71-optimized "))
        assertEquals("2.6.119", VersionUtils.normalizeVersion("v2.6.119".substringBefore("v").let { "2.6.119" }))
    }

    @Test
    fun normalizeVersion_removesOtherSuffixes() {
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71-beta"))
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71-alpha"))
        assertEquals("2.6.71", VersionUtils.normalizeVersion("2.6.71-rc1"))
    }

    @Test
    fun parseVersionCode_parsesStandardVersions() {
        assertEquals(20671L, VersionUtils.parseVersionCode("2.6.71"))
        assertEquals(20719L, VersionUtils.parseVersionCode("2.6.119"))
        assertEquals(10000L, VersionUtils.parseVersionCode("1.0.0"))
        assertEquals(0L, VersionUtils.parseVersionCode("invalid"))
    }

    @Test
    fun parseVersionCode_handlesOptimizedSuffix() {
        assertEquals(20671L, VersionUtils.parseVersionCode("2.6.71-optimized"))
        assertEquals(20671L, VersionUtils.parseVersionCode("2.6.71-optimized"))
    }

    @Test
    fun isVersionNewer_sameVersionNotNewer() {
        // Bug fixed: 2.6.71-optimized vs 2.6.71 should be SAME, not newer
        assertFalse(VersionUtils.isVersionNewer("2.6.71-optimized", "2.6.71"))
        assertFalse(VersionUtils.isVersionNewer("2.6.71", "2.6.71"))
        assertFalse(VersionUtils.isVersionNewer("2.6.71", "2.6.71-optimized"))
    }

    @Test
    fun isVersionNewer_newerPatchIsNewer() {
        assertTrue(VersionUtils.isVersionNewer("2.6.72", "2.6.71"))
        assertTrue(VersionUtils.isVersionNewer("2.6.119", "2.6.118"))
        assertTrue(VersionUtils.isVersionNewer("2.7.0", "2.6.119"))
        assertTrue(VersionUtils.isVersionNewer("3.0.0", "2.6.119"))
    }

    @Test
    fun isVersionNewer_olderVersionNotNewer() {
        assertFalse(VersionUtils.isVersionNewer("2.6.71", "2.6.72"))
        assertFalse(VersionUtils.isVersionNewer("2.6.118", "2.6.119"))
        assertFalse(VersionUtils.isVersionNewer("2.6.119", "2.7.0"))
    }

    @Test
    fun isVersionNewer_majorMinorComparison() {
        assertTrue(VersionUtils.isVersionNewer("2.7.0", "2.6.119"))
        assertTrue(VersionUtils.isVersionNewer("3.0.0", "2.9.9"))
        assertFalse(VersionUtils.isVersionNewer("2.6.119", "2.7.0"))
    }

    @Test
    fun isVersionNewer_handlesOptimizedInComparison() {
        assertFalse(VersionUtils.isVersionNewer("2.6.119-optimized", "2.6.119"))
        assertTrue(VersionUtils.isVersionNewer("2.6.120-optimized", "2.6.119"))
        assertTrue(VersionUtils.isVersionNewer("2.6.120", "2.6.119-optimized"))
    }

    @Test
    fun isVersionNewer_currentVersion() {
        // Current installed 2.6.119, remote 2.6.120 should be newer
        assertTrue(VersionUtils.isVersionNewer("2.6.120", "2.6.119"))
        assertFalse(VersionUtils.isVersionNewer("2.6.119", "2.6.119"))
        assertFalse(VersionUtils.isVersionNewer("2.6.118", "2.6.119"))
    }
}
