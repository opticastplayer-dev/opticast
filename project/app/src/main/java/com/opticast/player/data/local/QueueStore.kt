package com.opticast.player.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class QueueItem(
    val videoId: Long,
    val addedAt: Long,
    val order: Int
)

@Serializable
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<QueueItem>
)

class QueueStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val queueFile: File by lazy {
        File(context.filesDir, "queue.json")
    }
    private val playlistsFile: File by lazy {
        File(context.filesDir, "playlists.json")
    }

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    init {
        _queue.value = loadQueue()
        _playlists.value = loadPlaylists()
    }

    private fun loadQueue(): List<QueueItem> {
        return try {
            if (!queueFile.exists()) return emptyList()
            json.decodeFromString<List<QueueItem>>(queueFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun loadPlaylists(): List<Playlist> {
        return try {
            if (!playlistsFile.exists()) return emptyList()
            json.decodeFromString<List<Playlist>>(playlistsFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addToQueue(videoId: Long) {
        val current = _queue.value.toMutableList()
        if (current.none { it.videoId == videoId }) {
            current.add(QueueItem(videoId, System.currentTimeMillis(), current.size))
            _queue.value = current
            saveQueue(current)
        }
    }

    fun addNext(videoId: Long) {
        val current = _queue.value.toMutableList()
        current.removeAll { it.videoId == videoId }
        current.add(0, QueueItem(videoId, System.currentTimeMillis(), 0))
        // Reorder
        val reordered = current.mapIndexed { index, item -> item.copy(order = index) }
        _queue.value = reordered
        saveQueue(reordered)
    }

    fun removeFromQueue(videoId: Long) {
        val current = _queue.value.toMutableList()
        current.removeAll { it.videoId == videoId }
        val reordered = current.mapIndexed { index, item -> item.copy(order = index) }
        _queue.value = reordered
        saveQueue(reordered)
    }

    fun clearQueue() {
        _queue.value = emptyList()
        saveQueue(emptyList())
    }

    fun reorderQueue(from: Int, to: Int) {
        val current = _queue.value.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            val reordered = current.mapIndexed { index, it -> it.copy(order = index) }
            _queue.value = reordered
            saveQueue(reordered)
        }
    }

    fun shuffleQueue() {
        val shuffled = _queue.value.shuffled().mapIndexed { index, item -> item.copy(order = index) }
        _queue.value = shuffled
        saveQueue(shuffled)
    }

    private fun saveQueue(queue: List<QueueItem>) {
        try {
            queueFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(QueueItem.serializer()), queue))
        } catch (_: Exception) {
        }
    }

    fun createPlaylist(name: String, videoIds: List<Long>): Playlist {
        val playlist = Playlist(
            id = System.currentTimeMillis(),
            name = name,
            createdAt = System.currentTimeMillis(),
            items = videoIds.mapIndexed { index, id -> QueueItem(id, System.currentTimeMillis(), index) }
        )
        val current = _playlists.value.toMutableList()
        current.add(playlist)
        _playlists.value = current
        savePlaylists(current)
        return playlist
    }

    fun deletePlaylist(id: Long) {
        val current = _playlists.value.toMutableList()
        current.removeAll { it.id == id }
        _playlists.value = current
        savePlaylists(current)
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        try {
            playlistsFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(Playlist.serializer()), playlists))
        } catch (_: Exception) {
        }
    }
}
