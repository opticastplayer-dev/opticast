package com.opticast.player.player.mpv

import android.content.Context

/** Records interruption, not a claimed crash cause. JNI crashes cannot be caught in Kotlin. */
object NativeSessionJournal {
    private val processToken = java.util.UUID.randomUUID().toString()
    private fun prefs(context: Context) = context.getSharedPreferences("native_session_journal", Context.MODE_PRIVATE)
    // Called on the native worker. Commit before JNI so a process exit does not lose the marker.
    fun opened(context: Context) { prefs(context).edit().putString("active_process", processToken).commit() }
    fun closed(context: Context) { prefs(context).edit().remove("active_process").commit() }
    fun consumeInterrupted(context: Context): Boolean {
        val previous = prefs(context).getString("active_process", null) ?: return false
        if (previous == processToken) return false
        prefs(context).edit().remove("active_process").apply()
        return true
    }
}
