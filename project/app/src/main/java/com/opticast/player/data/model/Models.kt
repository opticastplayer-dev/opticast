package com.opticast.player.data.model

import kotlinx.serialization.Serializable

/** Result of parsing a video file name. */
@Serializable
data class ParsedName(
    val title: String,
    val year: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
)

/** A single video file discovered on the device through MediaStore. */
@Serializable
data class LocalVideo(
    val id: Long,
    val name: String,
    val uri: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val width: Int,
    val height: Int,
    val parsed: ParsedName,
    val relativePath: String = "",
    val modifiedSec: Long = 0L,
) {
    val isEpisode: Boolean get() = seasonNumber != null && episodeNumber != null

    val seasonNumber: Int? get() = parsed.season
    val episodeNumber: Int? get() = parsed.episode

    val seasonEpisodeTag: String?
        get() = if (isEpisode) "S%02dE%02d".format(seasonNumber, episodeNumber) else null

    val resolutionLabel: String?
        get() = when {
            height >= 2000 || width >= 3500 -> "4K"
            height >= 1080 -> "1080p"
            height >= 720 -> "720p"
            height >= 480 -> "480p"
            height > 0 -> "${height}p"
            else -> null
        }
}

/** Metadata matched from TMDB, persisted per video as JSON. */
@Serializable
data class Metadata(
    val tmdbId: Int,
    val type: String, // "movie" or "tv"
    val title: String,
    val originalTitle: String = "",
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val voteAverage: Double = 0.0,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val runtimeMinutes: Int? = null,
    // Episode-specific fields
    val showTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeName: String? = null,
    val episodeOverview: String? = null,
    val episodeStillPath: String? = null,
    // Which provider produced this metadata: "tmdb" or "anilist"
    val source: String = "tmdb",
    val sourceUrl: String? = null,
    val attribution: String? = null,
    val manuallyMatched: Boolean = false,
) {
    val displayTitle: String
        get() = if (type == "tv") (showTitle ?: title) else title

    val isEpisodeMetadata: Boolean
        get() = seasonNumber != null && episodeNumber != null
}

/** A subtitle file saved on disk for a given video. */
@Serializable
data class SavedSubtitle(
    val id: String,
    val videoId: Long,
    val language: String,
    val releaseName: String,
    val filePath: String,
    val source: String = "opensubtitles",
)

/** A library row: a local video plus its optional matched metadata. */
data class LibraryEntry(
    val video: LocalVideo,
    val metadata: Metadata?,
)
