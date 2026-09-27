package com.opticast.player.ui.components

/** Local metadata only: selecting a genre never starts a network lookup. */
internal fun libraryGenres(genres: List<String>): List<String> = genres.map { it.trim() }
    .filter { it.isNotBlank() }.distinctBy { it.lowercase(java.util.Locale.ROOT) }
    .sortedWith(String.CASE_INSENSITIVE_ORDER)

internal fun matchesLibraryGenre(genres: List<String>, selected: String): Boolean =
    selected.isBlank() || genres.any { it.trim().equals(selected.trim(), ignoreCase = true) }

/** Do not archive a series until every available episode is complete. */
internal fun completedLibrarySeries(watchedEpisodes: List<Boolean>): Boolean =
    watchedEpisodes.isNotEmpty() && watchedEpisodes.all { it }
