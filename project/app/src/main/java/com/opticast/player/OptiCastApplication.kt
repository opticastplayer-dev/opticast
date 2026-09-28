package com.opticast.player

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.opticast.player.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OptiCastApplication : Application() {

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Detect low-RAM early so all later decisions use correct budget
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val lowRam = activityManager?.isLowRamDevice == true
        AppContainer.setLowRamMode(lowRam)

        // Init settings off main thread where possible; AppContainer now avoids runBlocking
        AppContainer.init(this)

        // App-wide image loader tuned for performance:
        // - crossfade off: grids bind many images per second
        // - RGB_565: half memory, critical on 2GB devices
        // - no hardware bitmaps on low-RAM: hardware bitmaps can't be cached efficiently and cause extra copies
        // - larger memory budget (32/96 MiB) for smooth scrolling - 6/16 was too small causing eviction during fling
        // - disk cache (128/256 MiB) for offline posters to reduce pressure
        // - offline artwork mapper to avoid HD downloads on data saver
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(
                        object : coil.map.Mapper<String, String> {
                            override fun map(
                                data: String,
                                options: coil.request.Options,
                            ): String? {
                                val reduced = com.opticast.player.data.remote.downsampleTmdbUrl(data) ?: data
                                return AppContainer.offlineArtwork.localUrl(reduced) ?: reduced
                            }
                        },
                    )
                }
                .crossfade(false)
                .respectCacheHeaders(false)
                .allowRgb565(true)
                .allowHardware(!lowRam)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizeBytes(com.opticast.player.data.imageMemoryBudgetBytes(lowRam))
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(if (lowRam) 64L * 1024 * 1024 else 192L * 1024 * 1024)
                        .build()
                }
                .build()
        )

        // Build poster index off main thread - first fling never waits on FS stats
        startupScope.launch { AppContainer.posterCache.warmUp() }

        // Build poster index off main thread with delay for smooth startup - critical for library scrolling responsiveness matching settings
        startupScope.launch {
            try {
                // Delay warmUp to let library first frame render - settings smooth because no warmUp needed
                kotlinx.coroutines.delay(1500)
                AppContainer.posterCache.warmUp()
            } catch (_: Exception) { }
        }

        // OFFLINE-FIRST: Check for updates once when internet detected, not every 6 hours, minimal data usage
        // User priority: offline use, little data, check once when internet detected
        // Only one check at startup, delayed to not affect library scrolling, checks only if online
        // No second check at 12s - saves data, respects offline rule
        startupScope.launch {
            try {
                // Delay 6s to let library be buttery smooth first, then check once if online
                kotlinx.coroutines.delay(6000)
                if (AppContainer.isOnline()) {
                    com.opticast.player.data.remote.UpdateChecker.checkWhenInternetDetected(this@OptiCastApplication)
                }
                // Also run regular startup check which will handle what's new and version tracking
                com.opticast.player.data.remote.UpdateChecker.checkAtStartup(this@OptiCastApplication)
            } catch (_: Exception) { }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Aggressive trim on low memory to keep playback smooth
            Coil.imageLoader(this).memoryCache?.clear()
            System.gc()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Coil.imageLoader(this).memoryCache?.clear()
    }
}
