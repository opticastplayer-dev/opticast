package com.opticast.player.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.showCollection

/**
 * Selection state extracted from LibraryScreen.kt
 * Single responsibility: multi-select mode
 */
class LibrarySelectionState(
    val selectionMode: MutableState<Boolean>,
    val selectedIds: SnapshotStateList<Long>,
    val confirmDeleteIds: MutableState<List<Long>?>,
    val pendingWriteDelete: MutableState<List<Long>>,
    val legacyDeleteTick: MutableState<Int>
) {
    fun exit() {
        selectionMode.value = false
        selectedIds.clear()
    }

    fun toggle(videoId: Long) {
        if (!selectedIds.remove(videoId)) selectedIds.add(videoId)
    }

    fun toggleShow(entries: List<LibraryEntry>, entry: LibraryEntry) {
        val ids = showCollection(entries, entry).map { it.video.id }
        if (ids.all { it in selectedIds }) selectedIds.removeAll(ids)
        else selectedIds.addAll(ids.filterNot { it in selectedIds })
    }

    val isSelectionMode: Boolean get() = selectionMode.value
}

@Composable
fun rememberLibrarySelectionState(): LibrarySelectionState {
    val selectionMode = rememberSaveable { mutableStateOf(false) }
    val selectedIds = rememberSaveable(
        saver = listSaver<SnapshotStateList<Long>, Long>(
            save = { it.toList() },
            restore = { values -> mutableStateListOf<Long>().apply { addAll(values) } }
        )
    ) { mutableStateListOf<Long>() }
    val confirmDeleteIds = androidx.compose.runtime.remember { mutableStateOf<List<Long>?>(null) }
    val pendingWriteDelete = androidx.compose.runtime.remember { mutableStateOf<List<Long>>(emptyList()) }
    val legacyDeleteTick = androidx.compose.runtime.remember { mutableStateOf(0) }

    return LibrarySelectionState(
        selectionMode = selectionMode,
        selectedIds = selectedIds,
        confirmDeleteIds = confirmDeleteIds,
        pendingWriteDelete = pendingWriteDelete,
        legacyDeleteTick = legacyDeleteTick
    )
}
