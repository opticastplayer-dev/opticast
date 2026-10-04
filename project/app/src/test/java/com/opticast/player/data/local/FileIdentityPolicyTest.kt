package com.opticast.player.data.local

import org.junit.Test
import org.junit.Assert.*
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.ParsedName

class FileIdentityPolicyTest {

    private fun createVideo(id: Long, uri: String, size: Long = 1000L): LocalVideo {
        return LocalVideo(
            id = id,
            name = "test.mp4",
            uri = uri,
            sizeBytes = size,
            durationMs = 6000L,
            dateAddedSec = 123456L,
            width = 1920,
            height = 1080,
            parsed = ParsedName(title = "test")
        )
    }

    @Test
    fun testSameFile() {
        val v1 = createVideo(1, "/sdcard/Movies/test.mp4", 1000L)
        val v2 = createVideo(1, "/sdcard/Movies/test.mp4", 1000L)
        val tracked1 = TrackedFile(v1, signature = "sig1")
        val tracked2 = TrackedFile(v2, signature = "sig1")
        val observed = ObservedFile(v1, signature = "sig1")
        assertTrue(sameFileContent(tracked1, observed))
        assertTrue(sameFileContent(tracked2, observed))
    }

    @Test
    fun testDifferentSize() {
        val v1 = createVideo(1, "/sdcard/Movies/test.mp4", 1000L)
        val v2 = createVideo(1, "/sdcard/Movies/test.mp4", 999999L)
        val tracked1 = TrackedFile(v1, signature = "sig1")
        val observed2 = ObservedFile(v2, signature = "sig1")
        // Different size should not be same content
        assertFalse(sameFileContent(tracked1, observed2))
    }

    @Test
    fun testDifferentPath() {
        val v1 = createVideo(1, "/sdcard/Movies/a.mp4", 1000L)
        val v2 = createVideo(2, "/sdcard/Movies/b.mp4", 1000L)
        assertNotEquals(v1.uri, v2.uri)
        assertNotEquals(v1.id, v2.id)
    }

    @Test
    fun testReconcileFiles() {
        val v1 = createVideo(1, "content://1", 1000L)
        val tracked = listOf(TrackedFile(v1, signature = "sig1", available = true))
        val observed = listOf(ObservedFile(v1, signature = "sig1"))
        val result = reconcileFiles(tracked, observed)
        assertEquals(1, result.size)
        assertTrue(result[0].available)
    }
}
