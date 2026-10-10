package com.opticast.player.data

import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.Metadata

data class SmartCollection(
    val id: String,
    val name: String,
    val description: String,
    val filter: (LibraryEntry) -> Boolean
)

object SmartCollections {

    fun getAll(): List<SmartCollection> {
        return listOf(
            SmartCollection(
                id = "unwatched",
                name = "Unwatched",
                description = "Movies and shows you haven't watched yet",
                filter = { entry ->
                    val state = AppContainer.playbackState.state(entry.video.id)
                    state?.isWatched != true && (state?.positionMs ?: 0L) < 1000
                }
            ),
            SmartCollection(
                id = "recent",
                name = "Recently Added",
                description = "Added in last 30 days",
                filter = { entry ->
                    entry.video.dateAddedSec * 1000L > System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                }
            ),
            SmartCollection(
                id = "favorites",
                name = "Favorites",
                description = "Your favorite videos",
                filter = { entry -> AppContainer.favorites.isFavorite(entry.video.id) }
            ),
            SmartCollection(
                id = "shorts",
                name = "Shorts",
                description = "Videos under 20 minutes",
                filter = { entry -> entry.video.durationMs in 1..20 * 60 * 1000 }
            ),
            SmartCollection(
                id = "long",
                name = "Long Movies",
                description = "Movies over 2 hours",
                filter = { entry -> entry.video.durationMs > 2 * 60 * 60 * 1000 }
            ),
            SmartCollection(
                id = "4k",
                name = "4K",
                description = "4K resolution videos",
                filter = { entry ->
                    entry.video.name.contains("2160p", ignoreCase = true) ||
                    entry.video.name.contains("4K", ignoreCase = true) ||
                    entry.video.name.contains("UHD", ignoreCase = true)
                }
            ),
            SmartCollection(
                id = "by_year_2024",
                name = "2024 Releases",
                description = "Movies from 2024",
                filter = { it.metadata?.year == 2024 }
            ),
            SmartCollection(
                id = "by_year_2023",
                name = "2023 Releases",
                description = "Movies from 2023",
                filter = { it.metadata?.year == 2023 }
            ),
            SmartCollection(
                id = "continue_watching",
                name = "Continue Watching",
                description = "Resume where you left off",
                filter = { entry ->
                    val state = AppContainer.playbackState.state(entry.video.id)
                    val pos = state?.positionMs ?: 0L
                    pos > 1000 && pos < entry.video.durationMs - 5000
                }
            )
        )
    }

    fun getByGenre(entries: List<LibraryEntry>): List<SmartCollection> {
        val genres = entries.flatMap { it.metadata?.genres ?: emptyList() }.distinct().take(10)
        return genres.map { genre ->
            SmartCollection(
                id = "genre_${genre.lowercase()}",
                name = genre,
                description = "All $genre movies and shows",
                filter = { it.metadata?.genres?.contains(genre) == true }
            )
        }
    }

    fun getByYear(entries: List<LibraryEntry>): List<SmartCollection> {
        val years = entries.mapNotNull { it.metadata?.year }.distinct().sortedDescending().take(10)
        return years.map { year ->
            SmartCollection(
                id = "year_$year",
                name = "$year",
                description = "Movies from $year",
                filter = { it.metadata?.year == year }
            )
        }
    }
}
