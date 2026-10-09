package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.remote.CastMember
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Cast entry as persisted on disk. */
@Serializable
data class CachedCast(
    val id: Int = 0,
    val name: String = "",
    val character: String = "",
    val profilePath: String? = null,
)

/**
 * Everything the detail page needs beyond core metadata: IMDb id and cast list.
 * Offline-first: written once per title and reused — opening detail page no longer
 * fires extra requests. Cached bundle never expires; clearing/rematching resets it.
 * Simplified: removed OMDb ratings and Fanart.tv artwork — TMDB only for lightness.
 */
@Serializable
data class CachedDetails(
    val imdbId: String? = null,
    val cast: List<CachedCast> = emptyList(),
    val savedAt: Long = 0L,
) {
    val isFresh: Boolean get() = savedAt > 0L

    fun toCast(): List<CastMember> = cast.map {
        CastMember(id = it.id, name = it.name, character = it.character, profilePath = it.profilePath)
    }

    fun imageUrls(): List<String> = cast.mapNotNull { it.profilePath }

    companion object {
        fun from(
            imdbId: String?,
            cast: List<CastMember>,
            previous: CachedDetails?,
        ): CachedDetails = CachedDetails(
            imdbId = imdbId ?: previous?.imdbId,
            cast = if (cast.isNotEmpty()) {
                cast.map {
                    CachedCast(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profilePath,
                    )
                }
            } else {
                previous?.cast ?: emptyList()
            },
            savedAt = System.currentTimeMillis(),
        )
    }
}

/** JSON file persistence for [CachedDetails], one entry per video. */
class DetailCache(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "details_cache.json")
    private val cache = mutableMapOf<String, CachedDetails>()

    init {
        runCatching {
            if (file.exists()) {
                json.decodeFromString<Map<String, CachedDetails>>(file.readText())
                    .forEach { (key, value) -> cache[key] = value }
            }
        }
    }

    fun get(videoId: Long): CachedDetails? = synchronized(cache) { cache[videoId.toString()] }

    fun count(): Int = synchronized(cache) { cache.size }

    fun save(videoId: Long, details: CachedDetails) {
        synchronized(cache) {
            cache[videoId.toString()] = details
            runCatching {
                val tmp = File(file.parentFile, "details_cache.json.part")
                tmp.writeText(json.encodeToString(cache.toMap()))
                if (!tmp.renameTo(file)) {
                    file.writeText(json.encodeToString(cache.toMap()))
                    tmp.delete()
                }
            }
        }
    }

    fun clear(videoId: Long) {
        synchronized(cache) {
            cache.remove(videoId.toString())
            runCatching { file.writeText(json.encodeToString(cache.toMap())) }
        }
    }
}
