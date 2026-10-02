package com.opticast.player.domain.usecase

import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Gold Standard UseCase — Continue Watching, offline-first
 * Combines library entries + playback progress
 */
class GetContinueWatchingUseCase(
    private val repository: LibraryRepository? = null
) {
    operator fun invoke(): Flow<List<LibraryEntry>> {
        // If repository provided, use it, else use AppContainer (backward compat)
        return if (repository != null) {
            combine(repository.entries, AppContainer.playbackState.progressFlow) { entries, _ ->
                entries.filter { entry ->
                    val state = AppContainer.playbackState.state(entry.video.id)
                    state?.isResumable == true && state.isWatched != true
                }.sortedByDescending { AppContainer.playbackState.state(it.video.id)?.lastPlayedMs ?: 0L }
            }
        } else {
            combine(AppContainer.playbackState.progressFlow, AppContainer.metadataStore.version) { _, _ ->
                emptyList<LibraryEntry>()
            }
        }
    }

    suspend fun get(): List<LibraryEntry> {
        val entries = repository?.entries?.value ?: emptyList()
        return entries.filter { entry ->
            val state = AppContainer.playbackState.state(entry.video.id)
            state?.isResumable == true && state.isWatched != true
        }.sortedByDescending { AppContainer.playbackState.state(it.video.id)?.lastPlayedMs ?: 0L }
    }
}
