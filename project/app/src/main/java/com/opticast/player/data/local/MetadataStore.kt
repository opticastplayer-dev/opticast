package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.model.SavedSubtitle
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Simple file-based persistence: matched metadata is stored as one JSON file
 * per video, subtitles live in a sibling directory with a JSON sidecar.
 */
class MetadataStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val metadataDir = File(context.filesDir, "metadata").apply { mkdirs() }
    private val subtitlesDir = File(context.filesDir, "subtitles").apply { mkdirs() }
    private val offsetsFile = File(context.filesDir, "subtitle_offsets.json")
    private val secondaryFile = File(context.filesDir, "secondary_subtitles.json")
    private val offsets: MutableMap<String, Long> = mutableMapOf()

    init {
        runCatching {
            if (offsetsFile.exists()) {
                json.decodeFromString<Map<String, Long>>(offsetsFile.readText())
                    .forEach { (key, value) -> offsets[key] = value }
            }
        }
    }

    private val _version = MutableStateFlow(0)

    /** Bumps whenever stored data changes so the UI can refresh. */
    val version: StateFlow<Int> = _version

    // ---------------------------------------------------------------- metadata

    /** Parsed metadata, mirrored in memory so rebuilds never re-read the disk. */
    private val memory = java.util.concurrent.ConcurrentHashMap<Long, Metadata>()
    private val knownAbsent = java.util.Collections.newSetFromMap(
        java.util.concurrent.ConcurrentHashMap<Long, Boolean>(),
    )

    fun get(videoId: Long): Metadata? {
        memory[videoId]?.let { return it }
        if (knownAbsent.contains(videoId)) return null
        val file = File(metadataDir, "$videoId.json")
        if (!file.exists()) {
            // Genuinely unmatched: remember it so repeat rebuilds skip the stat.
            knownAbsent.add(videoId)
            return null
        }
        val parsed = runCatching { json.decodeFromString<Metadata>(file.readText()) }.getOrNull()
        // A parse failure (half-written file, older schema) must NOT be treated
        // as "no metadata" — that used to hide posters for the whole session.
        if (parsed != null) memory[videoId] = parsed
        return parsed
    }

    fun save(videoId: Long, metadata: Metadata) {
        memory[videoId] = metadata
        knownAbsent.remove(videoId)
        runCatching {
            // Atomic replace: a concurrent reader never sees partial JSON.
            val target = File(metadataDir, "$videoId.json")
            val tmp = File(metadataDir, "$videoId.json.part")
            tmp.writeText(json.encodeToString(metadata))
            if (!tmp.renameTo(target)) {
                target.writeText(json.encodeToString(metadata))
                tmp.delete()
            }
        }
        _version.value++
    }

    /** In-memory snapshot of everything already parsed — used by statistics. */
    fun cached(videoId: Long): Metadata? = memory[videoId]

    fun clear(videoId: Long) {
        memory.remove(videoId)
        knownAbsent.add(videoId)
        File(metadataDir, "$videoId.json").delete()
        _version.value++
    }

    fun clearAll() {
        memory.clear()
        knownAbsent.clear()
        metadataDir.listFiles()?.forEach { it.delete() }
        _version.value++
    }

    /** Forces UI refresh (e.g. after settings changed what the scan returns). */
    fun touch() {
        _version.value++
    }

    // --------------------------------------------------------------- subtitles

    fun saveSubtitle(
        videoId: Long,
        language: String,
        releaseName: String,
        content: ByteArray,
        source: String = "opensubtitles",
        extension: String = "srt",
    ): SavedSubtitle {
        val stamp = System.currentTimeMillis()
        val safeExt = extension.lowercase().takeIf { it in listOf("srt", "ass", "vtt", "ssa") } ?: "srt"
        val file = File(subtitlesDir, "$videoId-$stamp.$safeExt")
        file.writeBytes(content)
        val subtitle = SavedSubtitle(
            id = "$videoId-$stamp",
            videoId = videoId,
            language = language,
            releaseName = releaseName,
            filePath = file.absolutePath,
            source = source,
        )
        File(subtitlesDir, "$videoId-$stamp.meta.json").writeText(json.encodeToString(subtitle))
        _version.value++
        return subtitle
    }

    fun subtitlesFor(videoId: Long): List<SavedSubtitle> {
        val metas = subtitlesDir.listFiles { f ->
            f.name.startsWith("$videoId-") && f.name.endsWith(".meta.json")
        } ?: return emptyList()
        return metas.mapNotNull { file ->
            runCatching { json.decodeFromString<SavedSubtitle>(file.readText()) }.getOrNull()
        }.filter { File(it.filePath).exists() }
            .sortedWith(compareBy({ it.language }, { it.releaseName }))
    }

    fun deleteSubtitle(subtitle: SavedSubtitle) {
        File(subtitle.filePath).delete()
        File(subtitlesDir, "${subtitle.id}.meta.json").delete()
        _version.value++
    }

    // --------------------------------------------- subtitle sync offsets

    /**
     * The second subtitle shown alongside the primary one (dual subtitles).
     * Stored per video, beside the subtitle offsets.
     */
    private val secondarySubs: MutableMap<String, String> = runCatching {
        if (secondaryFile.exists()) {
            json.decodeFromString<Map<String, String>>(secondaryFile.readText()).toMutableMap()
        } else {
            mutableMapOf()
        }
    }.getOrDefault(mutableMapOf())

    fun secondarySubtitle(videoId: Long): String? =
        synchronized(secondarySubs) { secondarySubs[videoId.toString()] }

    fun setSecondarySubtitle(videoId: Long, subtitleId: String?) {
        synchronized(secondarySubs) {
            if (subtitleId.isNullOrBlank()) secondarySubs.remove(videoId.toString())
            else secondarySubs[videoId.toString()] = subtitleId
        }
        runCatching {
            secondaryFile.writeText(json.encodeToString(synchronized(secondarySubs) { secondarySubs.toMap() }))
        }
    }

    fun subtitleOffset(videoId: Long): Long =
        synchronized(offsets) { offsets[videoId.toString()] ?: 0L }

    fun setSubtitleOffset(videoId: Long, offsetMs: Long) {
        synchronized(offsets) {
            if (offsetMs == 0L) offsets.remove(videoId.toString())
            else offsets[videoId.toString()] = offsetMs
            runCatching { offsetsFile.writeText(json.encodeToString(offsets.toMap())) }
        }
        _version.value++
    }
}
