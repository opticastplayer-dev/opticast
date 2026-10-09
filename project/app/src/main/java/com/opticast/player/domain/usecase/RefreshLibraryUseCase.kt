package com.opticast.player.domain.usecase

import com.opticast.player.data.AppContainer
import com.opticast.player.data.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase — Refresh library, offline-first, single responsibility
 * - Scans MediaStore via repository
 * - Handles errors without crashing
 * - Easy to test with FakeLibraryRepository
 */
class RefreshLibraryUseCase(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            repository.refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            // Offline: keep cached data, don't crash
            Result.failure(e)
        }
    }
}

class RecheckFilesUseCase(
    private val repository: LibraryRepository? = null
) {
    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (repository != null) {
                repository.refresh()
            } else {
                // Fallback to AppContainer for backward compat
                AppContainer.mediaScanner.scan()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
