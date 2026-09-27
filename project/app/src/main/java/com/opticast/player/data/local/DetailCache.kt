package com.opticast.player.data.local

import android.content.Context
import com.opticast.player.data.remote.CastMember
import com.opticast.player.data.remote.FanartArtwork
import com.opticast.player.data.remote.OmdbRatings
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
 * Everything the detail page needs beyond the core metadata: ratings, extra
 * artwork, the IMDb id and the cast list.
 *
 * OptiCast is meant to work offline, so this is written ONCE per title and then
 * reused — opening a detail page no longer fires TMDB/OMDb/Fanart requests
 * every time. A cached bundle never expires; clearing/rematching a title explicitly resets it.
 */
@Serializable
data class CachedDetails(
    val imdbId: String? = null,
    val imdb: String? = null,
    val imdbVotes: String? = null,
    val rottenTomatoes: String? = null,
    val metacritic: String? = null,
    val rated: String? = null,
    val awards: String? = null,
    val boxOffice: String? = null,
    val director: String? = null,
    val writer: String? = null,
    val logo: String? = null,
    val fanartBackground: String? = null,
    val banner: String? = null,
    val discart: String? = null,
    val clearart: String? = null,
    val cast: List<CachedCast> = emptyList(),
    val savedAt: Long = 0L,
) {
    val hasRatings: Boolean
        get() = imdb != null || rottenTomatoes != null || metacritic != null || awards != null

    val hasArtwork: Boolean
        get() = logo != null || fanartBackground != null || banner != null ||
            discart != null || clearart != null

    /** Still usable as-is (no refresh needed). */
    val isFresh: Boolean
        get() = savedAt > 0L

    fun toRatings(): OmdbRatings? =
        if (!hasRatings) null
        else OmdbRatings(
            imdb = imdb,
            imdbVotes = imdbVotes,
            rottenTomatoes = rottenTomatoes,
            metacritic = metacritic,
            rated = rated,
            awards = awards,
            boxOffice = boxOffice,
            director = director,
            writer = writer,
        )

    fun toArtwork(): FanartArtwork? =
        if (!hasArtwork) null
        else FanartArtwork(
            logo = logo,
            background = fanartBackground,
            banner = banner,
            discart = discart,
            clearart = clearart,
        )

    fun toCast(): List<CastMember> = cast.map {
        CastMember(id = it.id, name = it.name, character = it.character, profilePath = it.profilePath)
    }

    /** URLs worth keeping on disk so the page renders offline. */
    fun imageUrls(): List<String> = buildList {
        logo?.let(::add)
        fanartBackground?.let(::add)
        banner?.let(::add)
        clearart?.let(::add)
        discart?.let(::add)
        cast.mapNotNullTo(this) { it.profilePath }
    }

    companion object {

        fun from(
            imdbId: String?,
            ratings: OmdbRatings?,
            artwork: FanartArtwork?,
            cast: List<CastMember>,
            previous: CachedDetails?,
        ): CachedDetails = CachedDetails(
            imdbId = imdbId ?: previous?.imdbId,
            imdb = ratings?.imdb ?: previous?.imdb,
            imdbVotes = ratings?.imdbVotes ?: previous?.imdbVotes,
            rottenTomatoes = ratings?.rottenTomatoes ?: previous?.rottenTomatoes,
            metacritic = ratings?.metacritic ?: previous?.metacritic,
            rated = ratings?.rated ?: previous?.rated,
            awards = ratings?.awards ?: previous?.awards,
            boxOffice = ratings?.boxOffice ?: previous?.boxOffice,
            director = ratings?.director ?: previous?.director,
            writer = ratings?.writer ?: previous?.writer,
            logo = artwork?.logo ?: previous?.logo,
            fanartBackground = artwork?.background ?: previous?.fanartBackground,
            banner = artwork?.banner ?: previous?.banner,
            discart = artwork?.discart ?: previous?.discart,
            clearart = artwork?.clearart ?: previous?.clearart,
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

    /** Number of titles whose ratings/artwork/cast are stored on disk. */
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
