package com.opticast.player.ui.screens.library

import androidx.compose.runtime.Composable
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.showCollection
import com.opticast.player.data.model.showTitleOf
import com.opticast.player.ui.screens.EntryMenuSheet

/**
 * Gold Standard — Menu host extracted from LibraryScreen.kt
 * Single responsibility: entry menu sheet handling
 */
@Composable
fun LibraryMenuHost(
    menuEntry: LibraryEntry?,
    menuIsWholeShow: Boolean,
    entries: List<LibraryEntry>,
    selectionMode: Boolean,
    selectedIds: MutableList<Long>,
    onMenuEntryChange: (LibraryEntry?) -> Unit,
    onPlay: (LibraryEntry) -> Unit,
    onOpenShow: (String) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenMatch: (Long) -> Unit,
    onRefreshArtwork: (LibraryEntry) -> Unit,
    onSetWatched: (Long, Long, Boolean) -> Unit,
    onClearMetadata: (Long) -> Unit,
    onShare: (List<Long>) -> Unit,
    onConfirmDelete: (List<Long>) -> Unit,
    onSelectionModeChange: (Boolean) -> Unit
) {
    menuEntry?.let { entry ->
        val targets = if (menuIsWholeShow) showCollection(entries, entry) else listOf(entry)
        val targetIds = targets.map { it.video.id }.distinct()
        EntryMenuSheet(
            groupEntries = if (menuIsWholeShow) targets else null,
            entry = entry,
            onDismiss = { onMenuEntryChange(null) },
            onPlay = {
                onMenuEntryChange(null)
                onPlay(targets.firstOrNull { AppContainer.playbackState.state(it.video.id)?.isWatched != true } ?: entry)
            },
            onDetails = {
                onMenuEntryChange(null)
                if (menuIsWholeShow) onOpenShow(showTitleOf(entry)) else onOpenDetail(entry.video.id)
            },
            onMatch = {
                onMenuEntryChange(null)
                onOpenMatch(entry.video.id)
            },
            onRefreshArtwork = {
                onRefreshArtwork(entry)
                onMenuEntryChange(null)
            },
            onToggleWatched = { watched ->
                targets.forEach { onSetWatched(it.video.id, it.video.durationMs, watched) }
                onMenuEntryChange(null)
            },
            onToggleFavorite = {
                if (targets.all { AppContainer.favorites.isFavorite(it.video.id) }) AppContainer.favorites.remove(targetIds)
                else AppContainer.favorites.add(targetIds)
                onMenuEntryChange(null)
            },
            onForget = {
                onClearMetadata(entry.video.id)
                onMenuEntryChange(null)
            },
            onShare = {
                onMenuEntryChange(null)
                onShare(targetIds)
            },
            onDelete = {
                onMenuEntryChange(null)
                onConfirmDelete(targetIds)
            },
            onSelect = {
                onMenuEntryChange(null)
                onSelectionModeChange(true)
                targetIds.forEach { if (it !in selectedIds) selectedIds.add(it) }
            }
        )
    }
}
