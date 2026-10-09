package com.opticast.player.domain.usecase

import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * UseCase - Get library entries, single responsibility
 */
class GetLibraryEntriesUseCase(
    private val repository: LibraryRepository
) {
    operator fun invoke(): StateFlow<List<LibraryEntry>> {
        return repository.entries
    }

    fun getById(id: Long): LibraryEntry? {
        return repository.getEntryById(id)
    }
}

class ClearMetadataUseCase {
    operator fun invoke(videoId: Long) {
        com.opticast.player.data.AppContainer.metadataStore.clear(videoId)
        com.opticast.player.data.AppContainer.detailCache.clear(videoId)
        com.opticast.player.data.AppContainer.posterCache.deleteForVideo(videoId)
    }
}

class SetWatchedUseCase {
    operator fun invoke(videoId: Long, durationMs: Long, watched: Boolean) {
        com.opticast.player.data.AppContainer.playbackState.setWatched(videoId, watched, durationMs)
    }
}
