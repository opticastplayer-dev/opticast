package com.opticast.player.data.remote

import com.opticast.player.data.SettingsRepository
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import com.opticast.player.data.AppContainer

/**
 * Builds a full image URL. Relative TMDB paths (e.g. "/xYz.jpg") get the CDN
 * prefix; absolute URLs (AniList covers, Fanart.tv art) pass through as-is.
 */
/**
 * Backdrop / episode still at a data-friendly size while the data saver is on
 * (w300 is roughly a quarter of the bytes of w780).
 */
fun tmdbBackdropUrl(path: String?): String? =
    tmdbImageUrl(path, if (AppContainer.dataSaver) "w300" else "w780")

/** Poster at a data-friendly size while the data saver is on. */
fun tmdbPosterUrl(path: String?): String? =
    tmdbImageUrl(
        path,
        if (AppContainer.dataSaver || AppContainer.lowRamMode) "w185" else "w342",
    )

private const val TMDB_IMG_PREFIX = "https://image.tmdb.org/t/p/"

/**
 * Last line of defence against HD image downloads.
 *
 * Applied to EVERY image Coil loads (see OptiCastApplication), so even a code path
 * that asks for a large size cannot pull HD bytes while the data saver is on.
 * Returns the URL unchanged when the saver is off, or when it is not a TMDB
 * image (Fanart.tv and AniList URLs are simply never used in that mode).
 */
fun downsampleTmdbUrl(url: String): String? {
    if (!AppContainer.dataSaver) return url
    if (!url.startsWith(TMDB_IMG_PREFIX)) return url
    val rest = url.removePrefix(TMDB_IMG_PREFIX)
    val slash = rest.indexOf('/')
    if (slash <= 0) return url
    val size = rest.substring(0, slash)
    val path = rest.substring(slash + 1)
    val reduced = when (size) {
        // Backdrops, stills and originals -> small backdrop.
        "w780", "w1280", "original", "h632" -> "w300"
        // Posters and profile shots -> small poster.
        "w342", "w500" -> "w185"
        else -> size
    }
    return "$TMDB_IMG_PREFIX$reduced/$path"
}

fun tmdbImageUrl(path: String?, size: String = "w500"): String? = path?.let {
    if (it.startsWith("http://") || it.startsWith("https://")) it
    else "https://image.tmdb.org/t/p/$size$it"
}

@Serializable
data class TmdbSearchResult(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
) {
    val displayTitle: String get() = title ?: name ?: ""
    val year: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
}

/** A unified search hit (movie or TV show) for the manual match screen. */
data class MatchCandidate(
    val result: TmdbSearchResult,
    val isTv: Boolean,
) {
    val title: String get() = result.displayTitle
    val year: Int? get() = result.year
    val posterUrl: String? get() = tmdbImageUrl(result.posterPath, "w185")
}

/** One cast member from TMDB credits. */
@Serializable
data class CastMember(
    val id: Int = 0,
    val name: String = "",
    val character: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
private data class SearchResponse(val results: List<TmdbSearchResult> = emptyList())

@Serializable
private data class GenreDto(val name: String = "")

@Serializable
private data class MovieDetailsDto(
    val id: Int,
    val title: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    val runtime: Int? = null,
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
private data class TvDetailsDto(
    val id: Int,
    val name: String? = null,
    val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
private data class EpisodeDto(
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val name: String? = null,
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null,
)

@Serializable
private data class SeasonDto(val episodes: List<EpisodeDto> = emptyList())

@Serializable
private data class CreditsResponse(val cast: List<CastMember> = emptyList())

@Serializable
private data class ImdbIdDto(@SerialName("imdb_id") val imdbId: String? = null)

/**
 * Minimal TMDB (v3, api_key) client. Returns empty/null results on any
 * network or parsing failure so callers can degrade gracefully.
 */
class TmdbApi(private val settings: SettingsRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val pausedKeys = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val castCache = mutableMapOf<String, List<CastMember>>()
    private val imdbCache = mutableMapOf<String, String>()

    private suspend fun apiKey(): String? =
        settings.current().tmdbApiKey.trim().takeIf { it.isNotBlank() }

    private suspend fun getBody(url: String): String? = withContext(Dispatchers.IO) {
        val key = url.toHttpUrl().queryParameter("api_key").orEmpty()
        if ((pausedKeys[key] ?: 0L) > System.currentTimeMillis()) return@withContext null
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else {
                    val seconds = when (response.code) {
                        401, 403 -> 3600L
                        429 -> response.header("Retry-After")?.toLongOrNull()?.coerceIn(1, 3600) ?: 60L
                        in 500..599 -> 30L
                        else -> 0L
                    }
                    if (seconds > 0) pausedKeys[key] = System.currentTimeMillis() + seconds * 1000
                    null
                }
            }
        }.onFailure { pausedKeys[key] = System.currentTimeMillis() + 30_000L }.getOrNull()
    }

    // ------------------------------------------------------------------ search

    suspend fun searchMovies(query: String, year: Int? = null): List<TmdbSearchResult> {
        val key = apiKey() ?: return emptyList()
        val url = "https://api.themoviedb.org/3/search/movie".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("query", query)
            .addQueryParameter("include_adult", "false")
            .apply { year?.let { addQueryParameter("year", it.toString()) } }
            .build()
        val body = getBody(url.toString()) ?: return emptyList()
        return runCatching { json.decodeFromString<SearchResponse>(body).results }
            .getOrElse { emptyList() }
    }

    suspend fun searchTv(query: String, year: Int? = null): List<TmdbSearchResult> {
        val key = apiKey() ?: return emptyList()
        val url = "https://api.themoviedb.org/3/search/tv".toHttpUrl().newBuilder()
            .addQueryParameter("api_key", key)
            .addQueryParameter("query", query)
            .addQueryParameter("include_adult", "false")
            .apply { year?.let { addQueryParameter("first_air_date_year", it.toString()) } }
            .build()
        val body = getBody(url.toString()) ?: return emptyList()
        return runCatching { json.decodeFromString<SearchResponse>(body).results }
            .getOrElse { emptyList() }
    }

    /** Searches both movies and TV shows; used by the manual match screen. */
    suspend fun searchAll(query: String): List<MatchCandidate> {
        if (query.isBlank()) return emptyList()
        val movies = searchMovies(query).map { MatchCandidate(it, isTv = false) }
        val tv = searchTv(query).map { MatchCandidate(it, isTv = true) }
        return (movies + tv).sortedByDescending { it.result.voteAverage }
    }

    // ---------------------------------------------------------------- details

    suspend fun movieMetadata(tmdbId: Int): Metadata? {
        val key = apiKey() ?: return null
        val body = getBody("https://api.themoviedb.org/3/movie/$tmdbId?api_key=$key")
            ?: return null
        val dto = runCatching { json.decodeFromString<MovieDetailsDto>(body) }.getOrNull()
            ?: return null
        return Metadata(
            tmdbId = dto.id,
            type = "movie",
            title = dto.title ?: "",
            originalTitle = dto.originalTitle ?: "",
            overview = dto.overview,
            posterPath = dto.posterPath,
            backdropPath = dto.backdropPath,
            voteAverage = dto.voteAverage,
            year = dto.releaseDate?.take(4)?.toIntOrNull(),
            genres = dto.genres.map { it.name },
            runtimeMinutes = dto.runtime,
        )
    }

    suspend fun showMetadata(tmdbId: Int): Metadata? {
        val key = apiKey() ?: return null
        val body = getBody("https://api.themoviedb.org/3/tv/$tmdbId?api_key=$key")
            ?: return null
        val dto = runCatching { json.decodeFromString<TvDetailsDto>(body) }.getOrNull()
            ?: return null
        return Metadata(
            tmdbId = dto.id,
            type = "tv",
            title = dto.name ?: "",
            originalTitle = dto.originalName ?: "",
            overview = dto.overview,
            posterPath = dto.posterPath,
            backdropPath = dto.backdropPath,
            voteAverage = dto.voteAverage,
            year = dto.firstAirDate?.take(4)?.toIntOrNull(),
            genres = dto.genres.map { it.name },
            runtimeMinutes = dto.episodeRunTime.firstOrNull(),
            showTitle = dto.name,
        )
    }

    suspend fun episodeMetadata(tmdbId: Int, season: Int, episode: Int): Metadata? {
        val show = showMetadata(tmdbId) ?: return null
        val key = apiKey() ?: return show
        val body = getBody("https://api.themoviedb.org/3/tv/$tmdbId/season/$season?api_key=$key")
        val episodeDto = body
            ?.let { runCatching { json.decodeFromString<SeasonDto>(it) }.getOrNull() }
            ?.episodes
            ?.firstOrNull { it.episodeNumber == episode }
        return show.copy(
            seasonNumber = season,
            episodeNumber = episode,
            episodeName = episodeDto?.name,
            overview = episodeDto?.overview?.takeIf { it.isNotBlank() } ?: show.overview,
            episodeStillPath = episodeDto?.stillPath,
        )
    }

    // --------------------------------------------------------- cast & videos

    /** Top cast members for a movie or show (cached in memory). */
    suspend fun castFor(tmdbId: Int, isTv: Boolean): List<CastMember> {
        val key = apiKey() ?: return emptyList()
        val cacheKey = "${if (isTv) "tv" else "movie"}-$tmdbId"
        castCache[cacheKey]?.let { return it }
        val kind = if (isTv) "tv" else "movie"
        val body = getBody("https://api.themoviedb.org/3/$kind/$tmdbId/credits?api_key=$key")
            ?: return emptyList()
        val cast = runCatching { json.decodeFromString<CreditsResponse>(body).cast }
            .getOrElse { emptyList() }
            .filter { it.name.isNotBlank() }
            .take(15)
        castCache[cacheKey] = cast
        return cast
    }

    /** IMDb id for a TMDB title (cached) — needed to enrich via OMDb. */
    suspend fun imdbIdFor(tmdbId: Int, isTv: Boolean): String? {
        val key = apiKey() ?: return null
        val cacheKey = "${if (isTv) "tv" else "movie"}-$tmdbId"
        imdbCache[cacheKey]?.let { return it.ifBlank { null } }
        val url = if (isTv) {
            "https://api.themoviedb.org/3/tv/$tmdbId/external_ids?api_key=$key"
        } else {
            "https://api.themoviedb.org/3/movie/$tmdbId?api_key=$key"
        }
        val body = getBody(url) ?: return null
        val imdbId = runCatching { json.decodeFromString<ImdbIdDto>(body).imdbId.orEmpty() }
            .getOrDefault("")
        imdbCache[cacheKey] = imdbId
        return imdbId.takeIf { it.isNotBlank() }
    }

    // -------------------------------------------------------------- auto match

    /**
     * Best-effort automatic match for a scanned file. Movies are matched by
     * title (+ year), episodes by show title + season/episode number.
     */
    suspend fun autoMatch(video: LocalVideo): Metadata? {
        val parsed = video.parsed
        if (parsed.title.isBlank()) return null
        return if (video.isEpisode) {
            val shows = searchTv(parsed.title)
            val best = pickBest(shows, parsed.title, null) ?: return null
            val season = parsed.season ?: return null
            val episode = parsed.episode ?: return null
            episodeMetadata(best.id, season, episode)
        } else {
            val movies = searchMovies(parsed.title, parsed.year)
            val best = pickBest(movies, parsed.title, parsed.year) ?: return null
            movieMetadata(best.id)
        }
    }

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun pickBest(
        results: List<TmdbSearchResult>,
        title: String,
        year: Int?,
    ): TmdbSearchResult? {
        if (results.isEmpty()) return null
        val target = normalize(title)

        fun score(result: TmdbSearchResult): Double {
            val candidate = normalize(result.displayTitle)
            var score = when {
                candidate == target -> 100.0
                candidate.startsWith(target) || target.startsWith(candidate) -> 80.0
                candidate.contains(target) || target.contains(candidate) -> 60.0
                else -> {
                    val targetWords = target.split(" ").toSet()
                    val candidateWords = candidate.split(" ").toSet()
                    if (targetWords.isEmpty()) 0.0
                    else 40.0 * targetWords.intersect(candidateWords).size / targetWords.size
                }
            }
            if (year != null && result.year == year) score += 10.0
            return score
        }

        return results.maxByOrNull { score(it) }?.takeIf { score(it) >= 55.0 }
    }
}
