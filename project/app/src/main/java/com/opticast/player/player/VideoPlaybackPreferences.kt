package com.opticast.player.player

import android.content.Context
import com.opticast.player.data.model.LocalVideo
import java.security.MessageDigest

/** Separate from resume; fingerprints prevent reused MediaStore IDs inheriting choices. No raw paths stored. */
class VideoPlaybackPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("video_playback_preferences", Context.MODE_PRIVATE)
    fun engine(key: String): String? = prefs.getString("$key:engine", null)?.takeIf { it in listOf("mpv", "media3") }
    fun saveEngine(key: String, engine: String) { if (engine in listOf("mpv", "media3")) prefs.edit().putString("$key:engine", engine).apply() }
    fun forgetEngine(key: String) { prefs.edit().remove("$key:engine").apply() }
    fun clearEngines() { val e = prefs.edit(); prefs.all.keys.filter { it.endsWith(":engine") }.forEach { e.remove(it) }; e.apply() }
    fun track(key: String, type: Int): String? = prefs.getString("$key:track:$type", null)
    fun saveTrack(key: String, type: Int, value: String) { prefs.edit().putString("$key:track:$type", value).apply() }
    fun clearTracks(key: String) { val e = prefs.edit(); prefs.all.keys.filter { it.startsWith("$key:track:") }.forEach { e.remove(it) }; e.apply() }
}
fun playbackPreferenceKey(video: LocalVideo): String = MessageDigest.getInstance("SHA-256")
    .digest("${video.id}\n${video.uri}\n${video.sizeBytes}\n${video.modifiedSec}\n${video.dateAddedSec}".toByteArray())
    .joinToString("") { "%02x".format(it) }
