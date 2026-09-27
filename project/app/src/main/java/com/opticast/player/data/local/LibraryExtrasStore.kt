package com.opticast.player.data.local

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class LibraryExtrasStore(context: Context) {
    private val prefs=context.getSharedPreferences("library_extras_v1",Context.MODE_PRIVATE)
    private val json=Json { ignoreUnknownKeys=true }
    fun rules(): List<SmartRule> = runCatching { json.decodeFromString<List<SmartRule>>(prefs.getString("smart_rules","[]")!!) }.getOrDefault(emptyList())
    fun saveRules(rules: List<SmartRule>) { prefs.edit().putString("smart_rules",json.encodeToString(rules.distinctBy { it.id })).apply() }
    fun profiles(): Map<String,SkipProfile> = runCatching { json.decodeFromString<Map<String,SkipProfile>>(prefs.getString("skip_profiles","{}")!!) }.getOrDefault(emptyMap())
    fun profile(id: Long) = profiles()[id.toString()] ?: SkipProfile()
    fun saveProfiles(values: Map<Long,SkipProfile>) { prefs.edit().putString("skip_profiles",json.encodeToString(profiles()+values.mapKeys { it.key.toString() })).apply() }
}
