package com.opticast.player.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crash reporting — saves crash info for debugging, works offline
 * - Saves crashes to file, no internet needed
 * - Keeps last 5 crashes to save space
 * - Helps fix stability issues quickly
 * - Private: stays on your device, not sent anywhere
 */
object CrashReporting {
    private const val TAG = "OptiCastCrash"
    private const val CRASH_DIR = "crashes"
    
    fun init(context: Context) {
        // Save crashes to file when app crashes — helps fix bugs
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(context, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash info", e)
            }
            // Let system handle crash normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "Crash reporting ready")
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
                Data Saver: ${isDataSaver(context)}
                
                What happened:
                $stackTrace
            """.trimIndent()
            
            crashFile.writeText(deviceInfo)
            Log.e(TAG, "Crash saved to ${crashFile.absolutePath}")
            
            // Keep only last 5 crashes to save space — stability
            val crashes = crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
            if (crashes.size > 5) {
                crashes.drop(5).forEach {
                    try { it.delete() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash", e)
        }
    }
    
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
            // Could also save non-fatal errors to file for debugging
        } catch (_: Exception) {
            // Stability: Don't crash while logging error
        }
    }
    
    fun getCrashReports(context: Context): List<File> {
        return try {
            val crashDir = File(context.filesDir, CRASH_DIR)
            crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    fun clearCrashes(context: Context) {
        try {
            val crashDir = File(context.filesDir, CRASH_DIR)
            crashDir.listFiles()?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            // Stability: Don't crash while clearing
        }
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
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.isLowRamDevice
        } catch (_: Exception) {
            false
        }
    }

    private fun isDataSaver(context: Context): Boolean {
        return try {
            AppContainer.dataSaver
        } catch (_: Exception) {
            false
        }
    }
}
