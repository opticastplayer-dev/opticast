package com.opticast.player.data.local

import android.content.Context
import android.graphics.BitmapFactory
import com.opticast.player.data.remote.downsampleTmdbUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** App files, not Android's disposable cache. No TTL, no LRU eviction. */
class OfflineArtwork(context: Context) {
    private val directory = File(context.filesDir, "offline_artwork").apply { mkdirs() }
    private val existing = ConcurrentHashMap.newKeySet<String>().apply {
        directory.listFiles()?.filter { it.name.endsWith(".img") && it.length() > 0 }?.forEach { add(it.name) }
    }
    private val gate = Mutex()
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    private fun key(url: String) = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
        .joinToString("") { "%02x".format(it) } + ".img"
    fun localUrl(url: String): String? {
        val name = key(url)
        return if (name in existing) "file://${File(directory, name).absolutePath}" else null
    }
    suspend fun prefetch(urls: List<String?>, deferDuringPlayback: Boolean = true) = withContext(Dispatchers.IO) {
        for (raw in urls.filterNotNull().distinct()) {
            // Never wait for playback while holding the mutex: explicit foreground work can proceed.
            if (deferDuringPlayback) com.opticast.player.data.PlaybackWorkBudget.awaitIdle()
            gate.withLock {
                val url = downsampleTmdbUrl(raw) ?: raw
                if (!url.startsWith("https://") || localUrl(url) != null) return@withLock
                val name = key(url)
                val temp = File(directory, "$name.part")
                try {
                    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        if (!response.isSuccessful) return@use
                        val body = response.body ?: return@use
                        // Bound disk/network cost even if a provider returns a bad URL.
                        body.byteStream().use { input ->
                            temp.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    if (total > 8 * 1024 * 1024) throw java.io.IOException("Artwork too large")
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(temp.absolutePath, options)
                        if (options.outWidth > 0 && options.outHeight > 0 && temp.renameTo(File(directory, name))) {
                            existing.add(name)
                        }
                    }
                } catch (_: java.io.IOException) {
                    // Leave missing images retryable on the next scan; never replace a valid image.
                } finally { temp.delete() }
            }
        }
    }
}
