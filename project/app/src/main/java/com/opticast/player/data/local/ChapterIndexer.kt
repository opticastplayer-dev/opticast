package com.opticast.player.data.local

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One chapter marker inside a video file. */
@Serializable
data class Chapter(val startMs: Long, val title: String)

/**
 * Chapter index for local video files.
 *
 * Media3 1.7.1 does not expose container chapters (there is no
 * `Player.getCurrentChapters()` and the Matroska extractor drops them), so the
 * chapters are read straight out of the file's EBML structure. Only the small
 * header region of an MKV has to be scanned, and the result is cached on disk,
 * so this costs nothing after the first open. Everything is defensive: any
 * malformed or unexpected file simply yields an empty list and the chapter UI
 * stays hidden.
 */
class ChapterIndexer(private val context: Context) {

    private val dir = File(context.filesDir, "chapters").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    /** In-memory hit so re-opening a file never touches the disk again. */
    private val memory = mutableMapOf<Long, List<Chapter>>()

    suspend fun forVideo(videoId: Long, uri: String): List<Chapter> = withContext(Dispatchers.IO) {
        memory[videoId]?.let { return@withContext it }

        val cacheFile = File(dir, "$videoId.json")
        if (cacheFile.exists()) {
            val cached = runCatching {
                json.decodeFromString<List<Chapter>>(cacheFile.readText())
            }.getOrNull()
            if (cached != null) {
                memory[videoId] = cached
                return@withContext cached
            }
        }

        val parsed = runCatching { parseMkvChapters(uri) }.getOrDefault(emptyList())
        memory[videoId] = parsed
        // Only cache a real result: an empty list may just mean the file was
        // unreadable at this moment (permission not granted yet, card removed).
        if (parsed.isNotEmpty()) {
            runCatching { cacheFile.writeText(json.encodeToString(parsed)) }
        }
        parsed
    }

    fun forget(videoId: Long) {
        memory.remove(videoId)
        runCatching { File(dir, "$videoId.json").delete() }
    }

    // ------------------------------------------------------------------ EBML

    private fun parseMkvChapters(uri: String): List<Chapter> {
        val stream = context.contentResolver.openInputStream(Uri.parse(uri))
            ?: return emptyList()
        return stream.use { raw ->
            val input = BufferedInputStream(raw, 64 * 1024)
            // Guard against pathological files: chapters normally sit in the
            // first few hundred kilobytes, right after the track list.
            var budget = 8L * 1024 * 1024
            while (budget > 0) {
                val id = readElementId(input) ?: return emptyList()
                val size = readElementSize(input) ?: return emptyList()
                if (size < 0) return emptyList()
                when (id) {
                    ID_SEGMENT -> {
                        // Descend into the segment without consuming its size.
                        budget -= 12
                    }
                    ID_CHAPTERS -> {
                        val limit = size.coerceAtMost(1L * 1024 * 1024)
                        val bytes = readBytes(input, limit) ?: return emptyList()
                        return parseChapters(bytes)
                    }
                    else -> {
                        if (!skipFully(input, size)) return emptyList()
                        budget -= size + 12
                    }
                }
            }
            emptyList()
        }
    }

    private fun parseChapters(bytes: ByteArray): List<Chapter> {
        val out = mutableListOf<Chapter>()
        var p = 0
        while (p < bytes.size) {
            val id = readId(bytes, p) ?: break
            p += id.second
            val size = readSize(bytes, p) ?: break
            p += size.second
            val body = size.first
            if (body < 0 || body > bytes.size.toLong() || p + body > bytes.size) break
            val bodyInt = body.toInt()
            if (id.first == ID_EDITION_ENTRY) {
                parseEdition(bytes, p, bodyInt, out)
            }
            p += bodyInt
        }
        return out.sortedBy { it.startMs }
    }

    private fun parseEdition(bytes: ByteArray, start: Int, length: Int, out: MutableList<Chapter>) {
        var p = start
        val end = start + length
        while (p < end) {
            val id = readId(bytes, p) ?: break
            p += id.second
            val size = readSize(bytes, p) ?: break
            p += size.second
            val body = size.first
            if (body < 0 || p + body > end) break
            val bodyInt = body.toInt()
            if (id.first == ID_CHAPTER_ATOM) {
                parseChapterAtom(bytes, p, bodyInt, out)
            }
            p += bodyInt
        }
    }

    private fun parseChapterAtom(bytes: ByteArray, start: Int, length: Int, out: MutableList<Chapter>) {
        var p = start
        val end = start + length
        var timeNs: Long? = null
        var title: String? = null
        while (p < end) {
            val id = readId(bytes, p) ?: break
            p += id.second
            val size = readSize(bytes, p) ?: break
            p += size.second
            val body = size.first
            if (body < 0 || p + body > end) break
            val bodyInt = body.toInt()
            when (id.first) {
                ID_CHAPTER_TIME_START -> timeNs = readUInt(bytes, p, bodyInt)
                ID_CHAPTER_DISPLAY -> title = readChapterDisplay(bytes, p, bodyInt)
            }
            p += bodyInt
        }
        val start = timeNs ?: return
        out += Chapter(startMs = start / 1_000_000L, title = title?.trim().orEmpty())
    }

    private fun readChapterDisplay(bytes: ByteArray, start: Int, length: Int): String? {
        var p = start
        val end = start + length
        while (p < end) {
            val id = readId(bytes, p) ?: return null
            p += id.second
            val size = readSize(bytes, p) ?: return null
            p += size.second
            val body = size.first
            if (body < 0 || p + body > end) return null
            val bodyInt = body.toInt()
            if (id.first == ID_CHAP_STRING) {
                return runCatching { String(bytes, p, bodyInt, Charsets.UTF_8) }.getOrNull()
            }
            p += bodyInt
        }
        return null
    }

    // ------------------------------------------------------- EBML primitives

    /** Returns the element id and how many bytes it occupied. */
    private fun readId(bytes: ByteArray, offset: Int): Pair<Long, Int>? {
        if (offset >= bytes.size) return null
        val first = bytes[offset].toInt() and 0xFF
        if (first == 0) return null
        val length = Integer.numberOfLeadingZeros(first) - 24 + 1
        if (length < 1 || length > 4 || offset + length > bytes.size) return null
        var value = 0L
        for (i in 0 until length) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return value to length
    }

    private fun readSize(bytes: ByteArray, offset: Int): Pair<Long, Int>? {
        if (offset >= bytes.size) return null
        val first = bytes[offset].toInt() and 0xFF
        if (first == 0) return null
        val length = Integer.numberOfLeadingZeros(first) - 24 + 1
        if (length < 1 || length > 8 || offset + length > bytes.size) return null
        var value = (first and (0xFF ushr length)).toLong()
        var allOnes = value == (0xFF ushr length).toLong()
        for (i in 1 until length) {
            val b = bytes[offset + i].toInt() and 0xFF
            if (b != 0xFF) allOnes = false
            value = (value shl 8) or b.toLong()
        }
        // "Unknown size" elements are legal in Matroska; treat as unusable here.
        if (allOnes) return -1L to length
        return value to length
    }

    private fun readElementId(input: InputStream): Long? {
        val first = input.read()
        if (first <= 0) return null
        val length = Integer.numberOfLeadingZeros(first) - 24 + 1
        if (length < 1 || length > 4) return null
        var value = first.toLong()
        repeat(length - 1) {
            val b = input.read()
            if (b < 0) return null
            value = (value shl 8) or b.toLong()
        }
        return value
    }

    private fun readElementSize(input: InputStream): Long? {
        val first = input.read()
        if (first <= 0) return null
        val length = Integer.numberOfLeadingZeros(first) - 24 + 1
        if (length < 1 || length > 8) return null
        var value = (first and (0xFF ushr length)).toLong()
        var allOnes = value == (0xFF ushr length).toLong()
        repeat(length - 1) {
            val b = input.read()
            if (b < 0) return null
            if (b != 0xFF) allOnes = false
            value = (value shl 8) or b.toLong()
        }
        return if (allOnes) -1L else value
    }

    private fun readBytes(input: InputStream, length: Long): ByteArray? {
        if (length <= 0 || length > 4L * 1024 * 1024) return null
        val out = ByteArray(length.toInt())
        var read = 0
        while (read < out.size) {
            val n = input.read(out, read, out.size - read)
            if (n < 0) return null
            read += n
        }
        return out
    }

    private fun skipFully(input: InputStream, length: Long): Boolean {
        if (length < 0) return false
        var remaining = length
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0) {
            val n = input.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
            if (n < 0) return false
            remaining -= n
        }
        return true
    }

    private fun readUInt(bytes: ByteArray, offset: Int, length: Int): Long {
        var value = 0L
        for (i in 0 until length.coerceAtMost(8)) {
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return value
    }

    private companion object {
        const val ID_SEGMENT = 0x18538067L
        const val ID_CHAPTERS = 0x1043A770L
        const val ID_EDITION_ENTRY = 0x45B9L
        const val ID_CHAPTER_ATOM = 0xB6L
        const val ID_CHAPTER_TIME_START = 0x91L
        const val ID_CHAPTER_DISPLAY = 0x80L
        const val ID_CHAP_STRING = 0x85L
    }
}
