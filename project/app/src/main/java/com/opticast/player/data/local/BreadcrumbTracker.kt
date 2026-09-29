package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.CrashReporting
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 10/10 Breadcrumb tracker - logs last 50 actions for crash debugging
 * - Stays on device, private, no internet
 * - Helps fix stability issues quickly
 * - Lightweight, no performance impact
 */
class BreadcrumbTracker(private val context: Context) {
    private val maxBreadcrumbs = 50

    fun log(action: String) {
        try {
            CrashReporting.addBreadcrumb(context, action)
            // Also log to local file for quick access
            val crashDir = File(context.filesDir, "crashes")
            crashDir.mkdirs()
            val bcFile = File(crashDir, "breadcrumb.txt")
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val line = "[$timestamp] $action\n"
            val existing = if (bcFile.exists()) bcFile.readLines().takeLast(maxBreadcrumbs - 1) else emptyList()
            bcFile.writeText((existing + line).joinToString(""))
        } catch (_: Exception) {}
    }

    fun logScan(count: Int) = log("scan:$count videos")
    fun logPrefetch(count: Int) = log("prefetch:$count posters")
    fun logPlayback(id: Long, action: String) = log("playback:$id $action")
    fun logSearch(query: String) = log("search:${query.take(20)}")
    fun logGridChange(grid: String) = log("grid:$grid")
    fun logCrash(message: String) = log("crash:$message")
}
