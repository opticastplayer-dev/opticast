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
 * Fast poster cache — works offline, saves data, smooth scrolling
 * - Downloads posters once, saves for offline viewing
 * - Uses small size on metered connection to save data (w185 vs w342)
 * - Only downloads when not playing video (saves battery, smooth playback)
 * - Small delay to let scroll settle — buttery smooth like settings
 * - Lock-free lookup for 60fps scrolling
 */
class PosterCache(context: Context) {

    private val dir = File(context.filesDir, "posters").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Concurrent sets for lock-free lookups — no waiting on UI thread
    private val attempted = ConcurrentHashMap.newKeySet<String>()
    private val existingKeys = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var primed = false

    /** Reads poster folder once — fast for smooth scrolling */
    fun warmUp() {
        if (primed) return
        val listing = runCatching { dir.listFiles() }.getOrNull() ?: run { primed = true; return }
        runCatching {
            // Fast filter without extra work
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

    // Offline-first, data sipping: small size on metered or low memory, saves data
    private fun posterSize(): String {
        // Check if on metered network or data saver enabled — use small size to save data
        val metered = AppContainer.isMeteredNetwork()
        val dataSaver = AppContainer.dataSaver
        val lowRam = AppContainer.lowRamMode
        return if (lowRam || dataSaver || metered) "w185" else "w342"
    }

    private fun downloadConcurrency(): Int = if (AppContainer.lowRamMode) 1 else 2

    private fun keyFor(videoId: Long, metadata: Metadata): String? {
        val poster = metadata.posterPath ?: return null
        return "$videoId-${metadata.source}-${metadata.tmdbId}-${poster.hashCode()}"
    }

    fun keyFor(entry: LibraryEntry): String? =
        entry.metadata?.let { keyFor(entry.video.id, it) }

    private fun fileFor(key: String): File = File(dir, "$key.jpg")

    /** Fast local lookup — no waiting, critical for smooth scrolling */
    fun localUrl(videoId: Long, metadata: Metadata?): String? {
        metadata ?: return null
        val key = keyFor(videoId, metadata) ?: return null
        if (primed) {
            return if (key in existingKeys) "file://${fileFor(key).absolutePath}" else null
        }
        // Cold start fallback — single check
        val file = fileFor(key)
        return if (file.exists() && file.length() > 0) {
            existingKeys.add(key)
            "file://${file.absolutePath}"
        } else null
    }

    suspend fun prefetch(entries: List<LibraryEntry>) {
        if (entries.isEmpty()) return
        // Stability: Only download when not playing video — saves battery, keeps playback smooth
        // Critical for library scrolling responsiveness matching settings during startup
        com.opticast.player.data.PlaybackWorkBudget.awaitIdle()
        kotlinx.coroutines.delay(500) // Small delay to let scroll settle — buttery smooth
        if (!AppContainer.isOnline()) return
        // Offline-first data sipping: check if on metered and data saver — still prefetch but small size
        // Uses w185 on metered to save data, already handled in posterSize()

        val targets = mutableListOf<Pair<String, String>>()
        for (entry in entries) {
            try {
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
            } catch (e: Exception) {
                // Stability: Skip corrupted entry, don't crash whole prefetch
                android.util.Log.w("PosterCache", "Skipping poster for ${entry.video.id}: ${e.message}")
                continue
            }
        }

        var downloaded = 0
        targets.chunked(downloadConcurrency()).forEach { chunk ->
            // Stability: Check idle again before each chunk — if user starts playing, pause downloads
            com.opticast.player.data.PlaybackWorkBudget.awaitIdle()
            downloaded += coroutineScope {
                chunk.map { (key, url) ->
                    async(Dispatchers.IO) {
                        try {
                            if (download(key, url)) 1 else 0
                        } catch (e: Exception) {
                            // Stability: Skip failed download, don't crash
                            android.util.Log.w("PosterCache", "Failed download $key: ${e.message}")
                            0
                        }
                    }
                }.awaitAll().sum()
            }
        }

        cleanupOrphans(entries)
        if (downloaded > 0) _version.value++
    }

    suspend fun forceRefresh(entry: LibraryEntry) {
        try {
            val metadata = entry.metadata ?: return
            val key = keyFor(entry.video.id, metadata) ?: return
            val url = tmdbImageUrl(metadata.posterPath, posterSize()) ?: return
            fileFor(key).delete()
            attempted.remove(key)
            existingKeys.remove(key)
            val ok = download(key, url)
            if (!ok) attempted.remove(key)
            _version.value++
        } catch (e: Exception) {
            android.util.Log.w("PosterCache", "Force refresh failed for ${entry.video.id}: ${e.message}")
        }
    }

    fun deleteForVideo(videoId: Long) {
        try {
            dir.listFiles()
                ?.filter { it.name.startsWith("$videoId-") }
                ?.forEach {
                    existingKeys.remove(it.name.removeSuffix(".jpg"))
                    it.delete()
                }
            attempted.removeAll { it.startsWith("$videoId-") }
            _version.value++
        } catch (e: Exception) {
            android.util.Log.w("PosterCache", "Delete failed for $videoId: ${e.message}")
        }
    }

    private suspend fun download(key: String, url: String): Boolean {
        // Exponential backoff retry - 1s, 2s, 4s max 3 times, prevents network loop on bad URL
        var attempt = 0
        var delayMs = 1000L
        while (attempt < 3) {
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
            if (ok && tmp.length() > 0) {
                if (tmp.renameTo(fileFor(key))) existingKeys.add(key)
                return true
            } else {
                tmp.delete()
                attempt++
                if (attempt < 3) {
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                }
            }
        }
        return false
    }

    private fun cleanupOrphans(entries: List<LibraryEntry>) {
        if (entries.isEmpty()) return
        try {
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
        } catch (e: Exception) {
            android.util.Log.w("PosterCache", "Cleanup failed: ${e.message}")
        }
    }
}
