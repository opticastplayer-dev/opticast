package com.opticast.player.ui.screens

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Separate additive preferences: never edits metadata, playback state or media files. */
internal class LibraryDesignStore(context: Context) {
    private val prefs = context.getSharedPreferences("library_design_v1", Context.MODE_PRIVATE)
    fun design(tab: String) = LibraryDesign(
        supportedLibraryStyle(prefs.getString("style", "classic")),
        normalizeSectionOrder(prefs.getString("order_$tab", classicSectionOrder.joinToString(","))!!.split(',')),
        prefs.getStringSet("hidden_$tab", emptySet())!!.toSet(), prefs.getBoolean("stats_$tab", true))
    fun save(tab: String, value: LibraryDesign) {
        prefs.edit().putString("style", value.style).putString("order_$tab", normalizeSectionOrder(value.order).joinToString(","))
            .putStringSet("hidden_$tab", value.hidden - "titles").putBoolean("stats_$tab", value.stats).apply()
    }
    fun history(): List<String> = runCatching { val a=JSONArray(prefs.getString("recent_searches", "[]")); (0 until a.length()).map { a.getString(it) } }.getOrDefault(emptyList())
    fun recordQuery(query: String) { prefs.edit().putString("recent_searches", JSONArray(addRecentQuery(history(), query)).toString()).apply() }
    fun clearHistory() { prefs.edit().remove("recent_searches").apply() }
    fun collections(): List<PersonalCollection> = runCatching {
        val a = JSONArray(prefs.getString("collections", "[]"))
        (0 until a.length()).map { index -> val o=a.getJSONObject(index); val ids=o.getJSONArray("ids")
            PersonalCollection(o.getString("id"), o.getString("name"), (0 until ids.length()).map { ids.getLong(it) }.distinct()) }
    }.getOrDefault(emptyList())
    fun saveCollections(values: List<PersonalCollection>) {
        val a = JSONArray()
        values.forEach { value -> a.put(JSONObject().put("id",value.id).put("name",value.name.trim().take(60)).put("ids",JSONArray(value.videoIds.distinct()))) }
        prefs.edit().putString("collections",a.toString()).apply()
    }
}
