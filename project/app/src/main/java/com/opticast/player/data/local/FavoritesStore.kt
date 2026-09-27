package com.opticast.player.data.local

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Simple file-backed list of favourite videos. */
class FavoritesStore(context: Context) {

    private val file = File(context.filesDir, "favorites.json")
    private val json = Json {}
    private val ids = mutableSetOf<Long>()

    private val _version = MutableStateFlow(0)

    /** Bumps on every change so screens can refresh. */
    val version: StateFlow<Int> = _version

    init {
        runCatching {
            if (file.exists()) {
                json.decodeFromString<List<Long>>(file.readText()).forEach { ids.add(it) }
            }
        }
    }

    fun isFavorite(videoId: Long): Boolean = synchronized(ids) { videoId in ids }

    /** Every favourite id, for the backup file. */
    fun all(): Set<Long> = synchronized(ids) { ids.toSet() }

    fun toggle(videoId: Long) {
        synchronized(ids) {
            if (!ids.add(videoId)) ids.remove(videoId)
        }
        persist()
    }

    /** Bulk "Add to favorites" from the library's multi-select bar. */
    fun add(videoIds: Collection<Long>) {
        if (videoIds.isEmpty()) return
        synchronized(ids) { ids.addAll(videoIds) }
        persist()
    }

    fun remove(videoIds: Collection<Long>) {
        if (videoIds.isEmpty()) return
        synchronized(ids) { ids.removeAll(videoIds.toSet()) }
        persist()
    }

    private fun persist() {
        runCatching {
            file.writeText(json.encodeToString(synchronized(ids) { ids.toList() }))
        }
        _version.value++
    }
}
