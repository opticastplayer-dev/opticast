package com.opticast.player.data.local

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

@Serializable data class PreferenceSnapshot(val strings: Map<String,String> = emptyMap(), val flags: Map<String,Boolean> = emptyMap(), val sets: Map<String,Set<String>> = emptyMap())
internal val libraryPreferenceFiles = setOf("library_design_v1","discovery_sections","library_extras_v1","video_playback_preferences")

internal fun captureLibraryPreferences(context: Context, includeSearchHistory: Boolean): Map<String,PreferenceSnapshot> = libraryPreferenceFiles.associateWith { name ->
    val values=context.getSharedPreferences(name,Context.MODE_PRIVATE).all.filterKeys { includeSearchHistory || it!="recent_searches" }
    PreferenceSnapshot(values.filterValues { it is String }.mapValues { it.value as String },values.filterValues { it is Boolean }.mapValues { it.value as Boolean },
        values.filterValues { it is Set<*> }.mapValues { (_,v) -> (v as Set<*>).filterIsInstance<String>().toSet() })
}

/** Keep unrelated groups/profiles. For a matching ID the explicitly imported record wins. */
internal fun mergePreferenceJson(key: String, existing: String?, incoming: String): String {
    if(key !in setOf("collections","smart_rules","skip_profiles")) return incoming
    val fresh=Json.parseToJsonElement(incoming)
    val old=existing?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() }
    return if(key=="skip_profiles") JsonObject((old as? JsonObject).orEmpty()+fresh.jsonObject).toString()
    else {
        val records=linkedMapOf<String,JsonElement>()
        (old as? JsonArray).orEmpty().forEach { records[it.jsonObject.getValue("id").jsonPrimitive.content]=it }
        fresh.jsonArray.forEach { records[it.jsonObject.getValue("id").jsonPrimitive.content]=it }
        JsonArray(records.values.toList()).toString()
    }
}
internal fun restoreLibraryPreferences(context: Context, data: Map<String,PreferenceSnapshot>) {
    // Prepare/validate all merged payloads before applying any preference writes.
    val prepared=data.filterKeys { it in libraryPreferenceFiles }.mapValues { (name,snapshot) ->
        val prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE)
        snapshot.copy(strings=snapshot.strings.mapValues { (key,value) -> mergePreferenceJson(key,prefs.getString(key,null),value) })
    }
    prepared.forEach { (name,snapshot) ->
        val edit=context.getSharedPreferences(name,Context.MODE_PRIVATE).edit()
        snapshot.strings.forEach { (k,v) -> edit.putString(k,v) }
        snapshot.flags.forEach { (k,v) -> edit.putBoolean(k,v) }
        snapshot.sets.forEach { (k,v) -> edit.putStringSet(k,v) }
        check(edit.commit()) { "Could not save Library preferences" }
    }
}

internal fun validateLibraryPreferences(data: Map<String,PreferenceSnapshot>) {
    val json=Json { ignoreUnknownKeys=true }
    data.filterKeys { it in libraryPreferenceFiles }.values.forEach { snapshot ->
        require(snapshot.strings.keys.intersect(snapshot.flags.keys).isEmpty() && snapshot.strings.keys.intersect(snapshot.sets.keys).isEmpty() && snapshot.flags.keys.intersect(snapshot.sets.keys).isEmpty()) { "Conflicting preference types" }
        snapshot.strings["collections"]?.let { text ->
            Json.parseToJsonElement(text).jsonArray.forEach { item ->
                val o=item.jsonObject
                require(o.getValue("id").jsonPrimitive.content.isNotBlank() && o.getValue("name").jsonPrimitive.content.isNotBlank())
                require(o.getValue("ids").jsonArray.all { it.jsonPrimitive.longOrNull!=null })
            }
        }
        snapshot.strings["smart_rules"]?.let { text ->
            json.decodeFromString<List<SmartRule>>(text).forEach { rule ->
                require(rule.id.isNotBlank() && rule.name.isNotBlank() && rule.type in setOf("all","movies","tv"))
                require(rule.recentDays==null || rule.recentDays>0); require(rule.maxMinutes==null || rule.maxMinutes>0)
                require(rule.yearFrom==null || rule.yearTo==null || rule.yearFrom<=rule.yearTo)
            }
        }
        snapshot.strings["skip_profiles"]?.let { text ->
            json.decodeFromString<Map<String,SkipProfile>>(text).forEach { (id,p) ->
                require(id.toLongOrNull()!=null)
                require(listOfNotNull(p.intro,p.credits).all { validSkipSpan(it,0) })
                require(p.intro==null || p.credits==null || p.intro.endMs<=p.credits.startMs)
            }
        }
    }
}
