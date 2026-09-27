package com.opticast.player.data.model

import java.util.Locale

internal fun isShowEntry(entry: LibraryEntry): Boolean = entry.video.isEpisode || entry.metadata?.type == "tv"
internal fun showTitleOf(entry: LibraryEntry): String = entry.metadata?.showTitle?.takeIf { it.isNotBlank() }
    ?: entry.video.parsed.title.ifBlank { entry.video.name }
private fun showNameKey(entry: LibraryEntry): String = showTitleOf(entry).trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
internal fun sameShow(a: LibraryEntry, b: LibraryEntry): Boolean {
    if (!isShowEntry(a) || !isShowEntry(b)) return false
    val ma = a.metadata; val mb = b.metadata
    if (ma?.type == "tv" && mb?.type == "tv" && ma.source == mb.source && ma.tmdbId > 0 && mb.tmdbId > 0)
        return ma.tmdbId == mb.tmdbId
    return showNameKey(a).isNotBlank() && showNameKey(a) == showNameKey(b)
}

/** Complete local show, independent of search, genre, watched and favourite filters. */
internal fun showCollection(entries: List<LibraryEntry>, selected: LibraryEntry): List<LibraryEntry> = entries
    .filter { sameShow(it, selected) }.distinctBy { it.video.id }
    .sortedWith(compareBy<LibraryEntry> { it.video.parsed.season ?: it.metadata?.seasonNumber ?: Int.MAX_VALUE }
        .thenBy { it.video.parsed.episode ?: it.metadata?.episodeNumber ?: Int.MAX_VALUE }
        .thenBy { it.video.name.lowercase(Locale.ROOT) }.thenBy { it.video.id })
