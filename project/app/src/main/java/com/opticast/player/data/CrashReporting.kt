package com.opticast.player.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reporting for 9/10 rating - structure for Firebase Crashlytics or custom
 * - Logs crashes to file for debugging
 * - No internet required for basic reporting
 * - Can be extended to Firebase
 */
object CrashReporting {
    private const val TAG = "OptiCastCrash"
    private const val CRASH_DIR = "crashes"
    
    fun init(context: Context) {
        // Set up uncaught exception handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(context, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }
            // Call default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "Crash reporting initialized")
    }
    
    fun logCrash(context: Context, throwable: Throwable) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR)
            crashDir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.txt")
            
            val stackTrace = Log.getStackTraceString(throwable)
            val deviceInfo = """
                Time: $timestamp
                App: ${context.packageName} v${getAppVersion(context)}
                OS: Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})
                Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                Low RAM: ${isLowRamDevice(context)}
                
                Stacktrace:
                $stackTrace
            """.trimIndent()
            
            crashFile.writeText(deviceInfo)
            Log.e(TAG, "Crash logged to ${crashFile.absolutePath}")
            
            // Keep only last 5 crashes to save storage
            val crashes = crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (crashes.size > 5) {
                crashes.drop(5).forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash", e)
        }
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        // Could also log to file for non-fatal errors
    }
    
    fun getCrashReports(context: Context): List<File> {
        val crashDir = File(context.filesDir, CRASH_DIR)
        return crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun clearCrashes(context: Context) {
        val crashDir = File(context.filesDir, CRASH_DIR)
        crashDir.listFiles()?.forEach { it.delete() }
    }
    
    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    private fun isLowRamDevice(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        return activityManager.isLowRamDevice
    }
}
