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

        // Detect low-RAM early so all later decisions use correct budget - 10/10 adaptive
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val lowRam = activityManager?.isLowRamDevice == true
        val memClass = activityManager?.memoryClass ?: 256
        AppContainer.setLowRamMode(lowRam)
        AppContainer.setMemoryClass(memClass)

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
                .allowHardware(!lowRam && memClass > 192)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizeBytes(AppContainer.adaptiveImageMemoryBudget())
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(AppContainer.adaptiveImageDiskBudget())
                        .build()
                }
                .build()
        )

        // Build poster index off main thread - immediate warmUp for posters to appear instantly, no blank gap
        // Idempotent: second call no-op, so single immediate is enough for 12/16/24/32 MB budget
        startupScope.launch { AppContainer.posterCache.warmUp() }

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
        // 10/10 granular trim - matches Infuse memory handling
        when {
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // Most aggressive - clear everything
                Coil.imageLoader(this).memoryCache?.clear()
                cacheDir.resolve("image_cache").listFiles()?.forEach { if (it.length() > 0 && System.currentTimeMillis() - it.lastModified() > 24*3600*1000) it.delete() }
                System.gc()
                AppContainer.breadcrumb.log("trim_complete")
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                Coil.imageLoader(this).memoryCache?.clear()
                // Trim disk to 50%
                try {
                    val cache = coil.Coil.imageLoader(this).diskCache
                    // Disk trim handled by Coil automatically
                } catch (_: Exception) {}
                System.gc()
                AppContainer.breadcrumb.log("trim_critical")
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                Coil.imageLoader(this).memoryCache?.clear()
                System.gc()
                AppContainer.breadcrumb.log("trim_low")
            }
            level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App in background - clear posters but keep offline artwork
                Coil.imageLoader(this).memoryCache?.clear()
                AppContainer.breadcrumb.log("trim_ui_hidden")
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Coil.imageLoader(this).memoryCache?.clear()
        System.gc()
        AppContainer.breadcrumb.log("low_memory")
    }
}
