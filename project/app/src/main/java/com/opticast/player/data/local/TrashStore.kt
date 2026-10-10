package com.opticast.player.data.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TrashEntry(
    val id: Long,
    val originalPath: String,
    val trashPath: String,
    val name: String,
    val deletedAt: Long,
    val size: Long
)

class TrashStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val trashDir: File by lazy {
        File(context.filesDir, ".trash").apply { mkdirs() }
    }
    private val indexFile: File by lazy {
        File(trashDir, "index.json")
    }

    fun getAll(): List<TrashEntry> {
        return try {
            if (!indexFile.exists()) return emptyList()
            val text = indexFile.readText()
            json.decodeFromString<List<TrashEntry>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun moveToTrash(originalFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!originalFile.exists()) return@withContext false
            val trashFile = File(trashDir, "${System.currentTimeMillis()}_${originalFile.name}")
            originalFile.copyTo(trashFile, overwrite = true)
            if (trashFile.exists() && trashFile.length() == originalFile.length()) {
                originalFile.delete()
                val entry = TrashEntry(
                    id = System.currentTimeMillis(),
                    originalPath = originalFile.absolutePath,
                    trashPath = trashFile.absolutePath,
                    name = originalFile.name,
                    deletedAt = System.currentTimeMillis(),
                    size = trashFile.length()
                )
                val current = getAll().toMutableList()
                current.add(entry)
                indexFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TrashEntry.serializer()), current))
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restore(entry: TrashEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(entry.trashPath)
            val originalFile = File(entry.originalPath)
            if (!trashFile.exists()) return@withContext false
            originalFile.parentFile?.mkdirs()
            trashFile.copyTo(originalFile, overwrite = true)
            if (originalFile.exists()) {
                trashFile.delete()
                val current = getAll().toMutableList()
                current.removeAll { it.id == entry.id }
                indexFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TrashEntry.serializer()), current))
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deletePermanently(entry: TrashEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(entry.trashPath)
            if (trashFile.exists()) trashFile.delete()
            val current = getAll().toMutableList()
            current.removeAll { it.id == entry.id }
            indexFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TrashEntry.serializer()), current))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun cleanExpired(retentionDays: Int) = withContext(Dispatchers.IO) {
        try {
            val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60 * 60 * 1000
            val current = getAll().toMutableList()
            val expired = current.filter { it.deletedAt < cutoff }
            expired.forEach { entry ->
                File(entry.trashPath).delete()
            }
            val remaining = current.filter { it.deletedAt >= cutoff }
            indexFile.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TrashEntry.serializer()), remaining))
        } catch (_: Exception) {
        }
    }

    fun getTotalSize(): Long {
        return getAll().sumOf { it.size }
    }
}
