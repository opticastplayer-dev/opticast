package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.remote.tmdbImageUrl
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * High-performance poster cache - optimized for smooth scrolling and low memory.
 * Downloads SD-quality posters once, renders offline, minimal allocations on UI thread.
 */
class PosterCache(context: Context) {

    private val dir = File(context.filesDir, "posters").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Concurrent sets for lock-free lookups on UI thread
    private val attempted = ConcurrentHashMap.newKeySet<String>()
    private val existingKeys = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var primed = false

    /** Reads poster directory once - fast path for smooth scrolling */
    fun warmUp() {
        if (primed) return
        val listing = runCatching { dir.listFiles() }.getOrNull() ?: run { primed = true; return }
        runCatching {
            // Fast filter without intermediate allocations
            for (file in listing) {
                if (file.isFile && file.length() > 0 && file.name.endsWith(".jpg")) {
                    existingKeys.add(file.name.removeSuffix(".jpg"))
                }
            }
        }
        primed = true
    }

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version

    private fun posterSize(): String =
        if (AppContainer.lowRamMode || AppContainer.dataSaver) "w185" else "w342"

    private fun downloadConcurrency(): Int = if (AppContainer.lowRamMode) 1 else 2

    private fun keyFor(videoId: Long, metadata: Metadata): String? {
        val poster = metadata.posterPath ?: return null
        return "$videoId-${metadata.source}-${metadata.tmdbId}-${poster.hashCode()}"
    }

    fun keyFor(entry: LibraryEntry): String? =
        entry.metadata?.let { keyFor(entry.video.id, it) }

    private fun fileFor(key: String): File = File(dir, "$key.jpg")

    /** Lock-free local URL lookup - critical for 60fps scrolling */
    fun localUrl(videoId: Long, metadata: Metadata?): String? {
        metadata ?: return null
        val key = keyFor(videoId, metadata) ?: return null
        if (primed) {
            return if (key in existingKeys) "file://${fileFor(key).absolutePath}" else null
        }
        // Cold start fallback - single stat
        val file = fileFor(key)
        return if (file.exists() && file.length() > 0) {
            existingKeys.add(key)
            "file://${file.absolutePath}"
        } else null
    }

    suspend fun prefetch(entries: List<LibraryEntry>) {
        if (entries.isEmpty()) return
        // Only prefetch when idle and not scrolling - critical for library scrolling responsiveness matching settings during startup
        com.opticast.player.data.PlaybackWorkBudget.awaitIdle()
        kotlinx.coroutines.delay(500) // Small delay to let scroll settle for buttery smooth
        if (!AppContainer.isOnline()) return

        // Prefetch only visible + 1 screen ahead (not 60) for low-RAM 32-bit smooth scrolling - previous fast builds did this
        // Visible is first screen, +10 more for next screen, not 60 which caused I/O during scroll
        val prefetchCount = if (AppContainer.lowRamMode) 20 else 30 // 20 for low-RAM 32-bit, 30 for normal - was 60 causing choppiness
        val limitedEntries = if (entries.size > prefetchCount) entries.take(prefetchCount) else entries
        val targets = mutableListOf<Pair<String, String>>()
        for (entry in limitedEntries) {
            val metadata = entry.metadata ?: continue
            val key = keyFor(entry.video.id, metadata) ?: continue
            val url = tmdbImageUrl(metadata.posterPath, posterSize()) ?: continue
            val file = fileFor(key)
            if (file.exists()) {
                if (file.length() > 0) {
                    existingKeys.add(key)
                    continue
                }
                file.delete()
            }
            existingKeys.remove(key)
            if (attempted.add(key)) targets += key to url
        }

        var downloaded = 0
        targets.chunked(downloadConcurrency()).forEach { chunk ->
            com.opticast.player.data.PlaybackWorkBudget.awaitIdle()
            downloaded += coroutineScope {
                chunk.map { (key, url) ->
                    async(Dispatchers.IO) { if (download(key, url)) 1 else 0 }
                }.awaitAll().sum()
            }
        }

        cleanupOrphans(entries)
        if (downloaded > 0) _version.value++
    }

    suspend fun forceRefresh(entry: LibraryEntry) {
        val metadata = entry.metadata ?: return
        val key = keyFor(entry.video.id, metadata) ?: return
        val url = tmdbImageUrl(metadata.posterPath, posterSize()) ?: return
        fileFor(key).delete()
        attempted.remove(key)
        existingKeys.remove(key)
        val ok = download(key, url)
        if (!ok) attempted.remove(key)
        _version.value++
    }

    fun deleteForVideo(videoId: Long) {
        dir.listFiles()
            ?.filter { it.name.startsWith("$videoId-") }
            ?.forEach {
                existingKeys.remove(it.name.removeSuffix(".jpg"))
                it.delete()
            }
        attempted.removeAll { it.startsWith("$videoId-") }
        _version.value++
    }

    private suspend fun download(key: String, url: String): Boolean {
        val tmp = File(dir, "$key.tmp")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) false
                    else {
                        val body = response.body ?: return@runCatching false
                        tmp.outputStream().use { out -> body.byteStream().copyTo(out) }
                        true
                    }
                }
            }.getOrDefault(false)
        }
        return if (ok && tmp.length() > 0) {
            if (tmp.renameTo(fileFor(key))) existingKeys.add(key)
            true
        } else {
            tmp.delete()
            false
        }
    }

    private fun cleanupOrphans(entries: List<LibraryEntry>) {
        if (entries.isEmpty()) return
        val liveVideoIds = entries.mapTo(HashSet()) { it.video.id }
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            when {
                file.name.endsWith(".tmp") -> if (now - file.lastModified() > 10 * 60_000L) file.delete()
                file.name.endsWith(".jpg") -> {
                    val key = file.name.removeSuffix(".jpg")
                    val videoId = key.substringBefore('-').toLongOrNull()
                    if (videoId == null || videoId !in liveVideoIds) {
                        existingKeys.remove(key)
                        file.delete()
                    }
                }
            }
        }
    }
}
