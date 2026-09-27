package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.AppContainer
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * One file that carries everything OptiCast knows: settings, watch state,
 * favourites and the subtitle offsets. Moving to a new phone is then a single
 * import instead of a week of re-matching.
 *
 * Deliberately *not* included: API keys are in here too, because the point is a
 * complete restore - the file is written wherever the user chooses through the
 * system file picker, never uploaded anywhere.
 */
@Serializable
data class OptiCastBackup(
    val format: Int = 1,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    /** Every DataStore preference, as stored. */
    val settings: Map<String, String> = emptyMap(),
    /** videoId -> position/duration/updatedAt. */
    val playback: Map<String, List<Long>> = emptyMap(),
    val favorites: List<Long> = emptyList(),
    /** videoId -> subtitle sync offset in ms. */
    val subtitleOffsets: Map<String, Long> = emptyMap(),
    /** file name -> raw JSON, for the per-title metadata records. */
    val metadata: Map<String, String> = emptyMap(),
    val fileInventory: String? = null,
    val libraryPreferences: Map<String,PreferenceSnapshot> = emptyMap(),
)

/** Writes and reads [OptiCastBackup] files. */
object LibraryBackup {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    /** Everything currently on this device. */
    fun build(context: Context, versionName: String, includeSearchHistory: Boolean = false): OptiCastBackup {
        val metadataDir = File(context.filesDir, "metadata")
        val metadata = runCatching {
            metadataDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.associate { it.name to it.readText() }
                ?: emptyMap()
        }.getOrDefault(emptyMap())

        val offsets = runCatching {
            val file = File(context.filesDir, "subtitle_offsets.json")
            if (!file.exists()) emptyMap()
            else json.decodeFromString<Map<String, Long>>(file.readText())
        }.getOrDefault(emptyMap())

        return OptiCastBackup(
            appVersion = versionName,
            exportedAt = System.currentTimeMillis(),
            settings = AppContainer.settings.exportSettings(),
            playback = AppContainer.playbackState.snapshot(),
            favorites = AppContainer.favorites.all().toList(),
            subtitleOffsets = offsets,
            metadata = metadata,
            fileInventory = AppContainer.mediaScanner.inventory.exportRecords(),
            libraryPreferences = captureLibraryPreferences(context, includeSearchHistory),
        )
    }

    fun toJson(backup: OptiCastBackup): String = json.encodeToString(backup)

    fun parse(text: String): OptiCastBackup = json.decodeFromString(text)

    /**
     * Applies a backup. Everything here is content the user can see and re-create
     * by hand, so an import is additive and never destructive: it writes what the
     * file contains and leaves anything else alone.
     */
    fun restore(context: Context, backup: OptiCastBackup): RestoreResult {
        validateLibraryPreferences(backup.libraryPreferences)
        var restored = 0
        // Validate stable identity conflicts before touching settings/history.
        backup.fileInventory?.let { AppContainer.mediaScanner.inventory.restoreRecords(it); restored++ }

        if (backup.libraryPreferences.isNotEmpty()) { restoreLibraryPreferences(context,backup.libraryPreferences); restored++ }

        if (backup.settings.isNotEmpty()) {
            AppContainer.settings.importSettings(backup.settings)
            restored++
        }

        if (backup.playback.isNotEmpty()) {
            AppContainer.playbackState.restore(
                backup.playback.mapValues { (_, values) ->
                    PlaybackState(
                        positionMs = values.getOrNull(0) ?: 0L,
                        durationMs = values.getOrNull(1) ?: 0L,
                        updatedAt = values.getOrNull(2) ?: 0L,
                    )
                },
            )
            restored++
        }

        if (backup.favorites.isNotEmpty()) {
            AppContainer.favorites.add(backup.favorites)
            restored++
        }

        if (backup.subtitleOffsets.isNotEmpty()) {
            runCatching {
                File(context.filesDir, "subtitle_offsets.json")
                    .writeText(json.encodeToString(backup.subtitleOffsets))
            }
            restored++
        }

        val metadataDir = File(context.filesDir, "metadata").apply { mkdirs() }
        backup.metadata.forEach { (name, body) ->
            runCatching { require(name == File(name).name && name.endsWith(".json")); File(metadataDir, name).writeText(body) }
        }
        if (backup.metadata.isNotEmpty()) restored++

        return RestoreResult(
            sections = restored,
            matchedTitles = backup.metadata.size,
            watchStates = backup.playback.size,
            // The per-title metadata is cached in memory, so it appears after the
            // next start rather than instantly.
            needsRestart = backup.metadata.isNotEmpty() || backup.fileInventory != null || backup.libraryPreferences.isNotEmpty(),
        )
    }

    data class RestoreResult(
        val sections: Int,
        val matchedTitles: Int,
        val watchStates: Int,
        val needsRestart: Boolean,
    )
}
