package com.opticast.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring for low-RAM 32-bit devices
 * Helps track jank and improve smoothness
 */
object PerformanceMonitor {
    private const val TAG = "OptiCastPerf"
    
    fun measureFrame(block: () -> Unit): Long {
        return measureTimeMillis(block)
    }
    
    fun logSlowFrame(tag: String, durationMs: Long, thresholdMs: Long = 16) {
        if (durationMs > thresholdMs) {
            Log.w(TAG, "Slow frame in $tag: ${durationMs}ms > ${thresholdMs}ms")
        }
    }
    
    fun isLowRamDevice(context: android.content.Context): Boolean {
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return activityManager.isLowRamDevice
    }
}

/**
 * Composable that logs slow compositions - for debugging choppiness
 */
@Composable
fun TrackSlowComposition(tag: String, thresholdMs: Long = 16) {
    val startTime = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        val duration = System.currentTimeMillis() - startTime
        if (duration > thresholdMs) {
            Log.w("OptiCastPerf", "Slow composition in $tag: ${duration}ms")
        }
    }
}

/**
 * Memory pressure monitoring for 3GB devices
 */
object MemoryMonitor {
    fun getAvailableMemory(context: android.content.Context): Long {
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }
    
    fun shouldReduceQuality(context: android.content.Context): Boolean {
        val available = getAvailableMemory(context)
        // If less than 500MB available, reduce quality
        return available < 500 * 1024 * 1024
    }
}
