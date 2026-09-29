package com.opticast.player.data.local

import org.junit.Test
import org.junit.Assert.*

class FileIdentityPolicyTest {
    @Test
    fun testSameFile() {
        val id1 = FileIdentityPolicy.identityFor("/sdcard/Movies/test.mp4", 1000L, 123456L)
        val id2 = FileIdentityPolicy.identityFor("/sdcard/Movies/test.mp4", 1000L, 123456L)
        assertEquals(id1, id2)
    }

    @Test
    fun testDifferentSize() {
        val id1 = FileIdentityPolicy.identityFor("/sdcard/Movies/test.mp4", 1000L, 123456L)
        val id2 = FileIdentityPolicy.identityFor("/sdcard/Movies/test.mp4", 1000L, 999999L)
        assertNotEquals(id1, id2)
    }

    @Test
    fun testDifferentPath() {
        val id1 = FileIdentityPolicy.identityFor("/sdcard/Movies/a.mp4", 1000L, 123L)
        val id2 = FileIdentityPolicy.identityFor("/sdcard/Movies/b.mp4", 1000L, 123L)
        assertNotEquals(id1, id2)
    }
}
