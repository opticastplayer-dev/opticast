package com.opticast.player.domain.usecase

import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gold Standard UseCase — Continue Watching, offline-first
 * Combines library entries + playback progress
 */
class GetContinueWatchingUseCase(
    private val repository: LibraryRepository? = null
) {
    operator fun invoke(): Flow<List<LibraryEntry>> {
        val entriesFlow = repository?.entries ?: AppContainer.metadataStore.version.let {
            // Fallback: empty flow if no repo
            kotlinx.coroutines.flow.MutableStateFlow(emptyList<LibraryEntry>())
        }
        return entriesFlow.map { entries ->
            entries.filter { entry ->
                val state = AppContainer.playbackState.state(entry.video.id)
                state?.isResumable == true && state.isWatched != true
            }.sortedByDescending { AppContainer.playbackState.state(it.video.id)?.updatedAt ?: 0L }
        }
    }

    suspend fun get(): List<LibraryEntry> {
        val entries = repository?.entries?.value ?: emptyList()
        return entries.filter { entry ->
            val state = AppContainer.playbackState.state(entry.video.id)
            state?.isResumable == true && state.isWatched != true
        }.sortedByDescending { AppContainer.playbackState.state(it.video.id)?.updatedAt ?: 0L }
    }
}
