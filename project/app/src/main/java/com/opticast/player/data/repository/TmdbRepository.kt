package com.opticast.player.data.repository

import com.opticast.player.data.model.Metadata
import com.opticast.player.data.remote.ApiResult
import com.opticast.player.data.remote.TmdbMovieDetailsDto
import com.opticast.player.data.remote.TmdbSearchResultDto
import com.opticast.player.data.remote.TmdbTvDetailsDto

/**
 * Repository interface for TMDB - separates data from UI, easy to test
 * interface + impl + fake, no God object
 */
interface TmdbRepository {
    suspend fun searchMovies(query: String, year: Int? = null): ApiResult<List<TmdbSearchResultDto>>
    suspend fun searchTv(query: String, year: Int? = null): ApiResult<List<TmdbSearchResultDto>>
    suspend fun getMovieDetails(tmdbId: Int): ApiResult<TmdbMovieDetailsDto>
    suspend fun getTvDetails(tmdbId: Int): ApiResult<TmdbTvDetailsDto>
    suspend fun getImdbId(tmdbId: Int, isTv: Boolean): String?
}

class TmdbRepositoryImpl(
    private val service: com.opticast.player.data.remote.TmdbProxyService = com.opticast.player.data.remote.TmdbProxyService()
) : TmdbRepository {
    override suspend fun searchMovies(query: String, year: Int?): ApiResult<List<TmdbSearchResultDto>> {
        return service.searchMovies(query, year)
    }
    override suspend fun searchTv(query: String, year: Int?): ApiResult<List<TmdbSearchResultDto>> {
        return service.searchTv(query, year)
    }
    override suspend fun getMovieDetails(tmdbId: Int): ApiResult<TmdbMovieDetailsDto> {
        return service.getMovieDetails(tmdbId)
    }
    override suspend fun getTvDetails(tmdbId: Int): ApiResult<TmdbTvDetailsDto> {
        return service.getTvDetails(tmdbId)
    }
    override suspend fun getImdbId(tmdbId: Int, isTv: Boolean): String? {
        return service.getImdbId(tmdbId, isTv)
    }
}

class FakeTmdbRepository : TmdbRepository {
    var movies: List<TmdbSearchResultDto> = emptyList()
    var tv: List<TmdbSearchResultDto> = emptyList()
    override suspend fun searchMovies(query: String, year: Int?): ApiResult<List<TmdbSearchResultDto>> {
        return ApiResult.Success(movies.filter { it.displayTitle.contains(query, ignoreCase = true) })
    }
    override suspend fun searchTv(query: String, year: Int?): ApiResult<List<TmdbSearchResultDto>> {
        return ApiResult.Success(tv.filter { it.displayTitle.contains(query, ignoreCase = true) })
    }
    override suspend fun getMovieDetails(tmdbId: Int): ApiResult<TmdbMovieDetailsDto> {
        return ApiResult.Error("Fake")
    }
    override suspend fun getTvDetails(tmdbId: Int): ApiResult<TmdbTvDetailsDto> {
        return ApiResult.Error("Fake")
    }
    override suspend fun getImdbId(tmdbId: Int, isTv: Boolean): String? = null
}
