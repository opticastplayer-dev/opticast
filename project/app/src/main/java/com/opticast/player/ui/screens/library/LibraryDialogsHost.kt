package com.opticast.player.ui.screens.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.opticast.player.data.model.LibraryEntry

/**
 * Dialogs host extracted from LibraryScreen.kt bottom 200 lines
 * Single responsibility: customize, collections, smart collections, missing files, rename, genre, delete confirm
 */
@Composable
internal fun LibraryDialogsHost(
    showCustomize: Boolean,
    tab: String,
    design: com.opticast.player.ui.screens.LibraryDesign,
    designStore: com.opticast.player.ui.screens.LibraryDesignStore,
    designRevision: Int,
    onDesignRevisionChange: (Int) -> Unit,
    onShowCustomizeChange: (Boolean) -> Unit,
    showCollections: Boolean,
    personalCollections: List<com.opticast.player.ui.screens.PersonalCollection>,
    entries: List<LibraryEntry>,
    onShowCollectionsChange: (Boolean) -> Unit,
    openCollectionId: String?,
    onOpenCollectionIdChange: (String?) -> Unit,
    showSmartCollections: Boolean,
    smartRules: List<com.opticast.player.data.local.SmartRule>,
    extrasStore: com.opticast.player.data.local.LibraryExtrasStore,
    extrasRevision: Int,
    onExtrasRevisionChange: (Int) -> Unit,
    onShowSmartCollectionsChange: (Boolean) -> Unit,
    allCollections: List<com.opticast.player.ui.screens.PersonalCollection>,
    showRenameSuggestions: Boolean,
    onShowRenameSuggestionsChange: (Boolean) -> Unit,
    onOpenMatch: (Long) -> Unit,
    viewModel: com.opticast.player.ui.screens.LibraryViewModel,
    showMissingFiles: Boolean,
    missingFiles: List<com.opticast.player.data.model.LocalVideo>,
    fileScanError: String?,
    checkingFiles: Boolean,
    onShowMissingFilesChange: (Boolean) -> Unit,
    showGenrePicker: Boolean,
    genres: List<String>,
    selectedGenre: String,
    onSelectedGenreChange: (String) -> Unit,
    onShowGenrePickerChange: (Boolean) -> Unit,
    confirmDeleteIds: List<Long>?,
    onConfirmDeleteIdsChange: (List<Long>?) -> Unit,
    onPerformDelete: (List<Long>) -> Unit,
    onExitSelection: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    if (showCustomize) com.opticast.player.ui.screens.LibraryCustomizeDialog(
        tab, design,
        onChange = { designStore.save(tab, it); onDesignRevisionChange(designRevision + 1) },
        onCollections = { onShowCustomizeChange(false); onShowCollectionsChange(true) },
        onDismiss = { onShowCustomizeChange(false) }
    )
    if (showCollections) com.opticast.player.ui.screens.CollectionsDialog(
        personalCollections, entries,
        onSave = { designStore.saveCollections(it); onDesignRevisionChange(designRevision + 1) },
        onOpen = { onShowCollectionsChange(false); onOpenCollectionIdChange(it.id) },
        onSmart = { onShowCollectionsChange(false); onShowSmartCollectionsChange(true) },
        onDismiss = { onShowCollectionsChange(false) }
    )
    if (showSmartCollections) com.opticast.player.ui.screens.SmartCollectionsDialog(
        smartRules,
        onSave = { extrasStore.saveRules(it); onExtrasRevisionChange(extrasRevision + 1) },
        onDismiss = { onShowSmartCollectionsChange(false) }
    )
    val openCollection = allCollections.firstOrNull { it.id == openCollectionId }
    if (openCollection != null) {
        com.opticast.player.ui.screens.CollectionContentsDialog(
            openCollection, entries,
            onOpen = { id -> onOpenCollectionIdChange(null); onOpenDetail(id) },
            onDismiss = { onOpenCollectionIdChange(null) }
        )
    }

    com.opticast.player.ui.screens.RenameSuggestionsPanel(
        visible = showRenameSuggestions,
        entries = entries,
        onClose = { onShowRenameSuggestionsChange(false) },
        onIdentify = { id -> onShowRenameSuggestionsChange(false); onOpenMatch(id) },
        onChanged = { viewModel.recheckFiles() }
    )

    if (showMissingFiles) {
        com.opticast.player.ui.screens.MissingFilesDialog(
            files = missingFiles,
            checking = checkingFiles,
            error = fileScanError,
            onRecheck = { viewModel.recheckFiles() },
            onDismissEntry = { viewModel.dismissMissing(it) },
            onClose = { onShowMissingFilesChange(false) }
        )
    }

    if (showGenrePicker) {
        LibraryGenrePickerSheet(
            genres = genres,
            selectedGenre = selectedGenre,
            onGenreSelected = { onSelectedGenreChange(it); onShowGenrePickerChange(false) },
            onDismiss = { onShowGenrePickerChange(false) }
        )
    }

    confirmDeleteIds?.let { ids ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            onDismissRequest = { onConfirmDeleteIdsChange(null) },
            title = { Text(if (ids.size == 1) "Delete this video?" else "Delete ${ids.size} videos?") },
            text = {
                Text(
                    if (ids.size == 1) "The file will be permanently deleted from your device storage."
                    else "These ${ids.size} files will be permanently deleted from your device storage."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmDeleteIdsChange(null)
                    onPerformDelete(ids)
                    onExitSelection()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { onConfirmDeleteIdsChange(null) }) { Text("Cancel") }
            }
        )
    }
}
