package com.opticast.player.data.remote

import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Drop-in replacement for old TmdbApi - same method names, but uses secure proxy
 * No API key needed. Handles Render waking.
 *
 * To migrate: in AppContainer.kt change:
 *   val tmdb: TmdbApi by lazy { TmdbApi(settings) }
 * to:
 *   val tmdb: TmdbApiProxyAdapter by lazy { TmdbApiProxyAdapter() }
 *
 * Or rename this class to TmdbApi and delete old file.
 */
class TmdbApiProxyAdapter {

    private val proxy = TmdbProxyService()

    // Cache like old client
    private val castCache = mutableMapOf<String, List<CastMember>>()
    private val imdbCache = mutableMapOf<String, String>()

    suspend fun searchMovies(query: String, year: Int? = null): List<TmdbSearchResult> {
        return when (val res = proxy.searchMovies(query, year)) {
            is ApiResult.Success -> res.data.map { it.toSearchResult() }
            else -> emptyList()
        }
    }

    suspend fun searchTv(query: String, year: Int? = null): List<TmdbSearchResult> {
        return when (val res = proxy.searchTv(query, year)) {
            is ApiResult.Success -> res.data.map { it.toSearchResult() }
            else -> emptyList()
        }
    }

    suspend fun searchAll(query: String): List<MatchCandidate> {
        if (query.isBlank()) return emptyList()
        val movies = searchMovies(query).map { MatchCandidate(it, isTv = false) }
        val tv = searchTv(query).map { MatchCandidate(it, isTv = true) }
        return (movies + tv).sortedByDescending { it.result.voteAverage }
    }

    suspend fun movieMetadata(tmdbId: Int): Metadata? = withContext(Dispatchers.IO) {
        when (val res = proxy.getMovieDetails(tmdbId)) {
            is ApiResult.Success -> {
                val dto = res.data
                Metadata(
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
            else -> null
        }
    }

    suspend fun showMetadata(tmdbId: Int): Metadata? = withContext(Dispatchers.IO) {
        when (val res = proxy.getTvDetails(tmdbId)) {
            is ApiResult.Success -> {
                val dto = res.data
                Metadata(
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
            else -> null
        }
    }

    suspend fun episodeMetadata(tmdbId: Int, season: Int, episode: Int): Metadata? {
        val show = showMetadata(tmdbId) ?: return null
        val seasonRes = proxy.getSeasonDetails(tmdbId, season)
        val episodeDto = when (seasonRes) {
            is ApiResult.Success -> seasonRes.data.episodes.firstOrNull { it.episodeNumber == episode }
            else -> null
        }
        return show.copy(
            seasonNumber = season,
            episodeNumber = episode,
            episodeName = episodeDto?.name,
            overview = episodeDto?.overview?.takeIf { it.isNotBlank() } ?: show.overview,
            episodeStillPath = episodeDto?.stillPath,
        )
    }

    suspend fun castFor(tmdbId: Int, isTv: Boolean): List<CastMember> {
        val cacheKey = "${if (isTv) "tv" else "movie"}-$tmdbId"
        castCache[cacheKey]?.let { return it }

        val result = if (isTv) proxy.getTvCredits(tmdbId) else proxy.getMovieCredits(tmdbId)
        val cast = when (result) {
            is ApiResult.Success -> result.data.cast
                .filter { it.name.isNotBlank() }
                .take(15)
                .map {
                    CastMember(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profilePath
                    )
                }
            else -> emptyList()
        }
        castCache[cacheKey] = cast
        return cast
    }

    suspend fun imdbIdFor(tmdbId: Int, isTv: Boolean): String? {
        val cacheKey = "${if (isTv) "tv" else "movie"}-$tmdbId"
        imdbCache[cacheKey]?.let { return it.ifBlank { null } }

        val imdbId = proxy.getImdbId(tmdbId, isTv).orEmpty()
        imdbCache[cacheKey] = imdbId
        return imdbId.takeIf { it.isNotBlank() }
    }

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

    private fun TmdbSearchResultDto.toSearchResult(): TmdbSearchResult {
        return TmdbSearchResult(
            id = id,
            title = title,
            name = name,
            overview = overview,
            posterPath = posterPath,
            backdropPath = backdropPath,
            voteAverage = voteAverage,
            releaseDate = releaseDate,
            firstAirDate = firstAirDate,
            originalTitle = originalTitle,
            originalName = originalName,
            originalLanguage = originalLanguage,
        )
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
