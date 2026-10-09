package com.opticast.player.domain.usecase

import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase — pure business logic, no Android, testable
 * Searches library offline-first, uses repository cache
 */
class SearchLibraryUseCase(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(query: String): List<LibraryEntry> = withContext(Dispatchers.Default) {
        if (query.isBlank()) {
            repository.entries.value
        } else {
            repository.search(query)
        }
    }
}
