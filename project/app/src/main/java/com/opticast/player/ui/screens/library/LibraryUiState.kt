package com.opticast.player.ui.screens.library

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.screens.LibraryDesignStore

/**
 * Gold Standard — UI state extracted from LibraryScreen.kt top 200 lines
 * Single responsibility: holds designStore, collections, search, filters, tabs
 * Was 200+ lines of rememberSaveable in God composable, now reusable
 */
class LibraryUiState(
    val designStore: LibraryDesignStore,
    val designRevision: MutableState<Int>,
    val showCustomize: MutableState<Boolean>,
    val showCollections: MutableState<Boolean>,
    val openCollectionId: MutableState<String?>,
    val showSmartCollections: MutableState<Boolean>,
    val extrasRevision: MutableState<Int>,
    val query: MutableState<String>,
    val searchOpen: MutableState<Boolean>,
    val searchScope: MutableState<String>,
    val sortBy: MutableState<String>,
    val selectedGenre: MutableState<String>,
    val showGenrePicker: MutableState<Boolean>,
    val showMissingFiles: MutableState<Boolean>,
    val showRenameSuggestions: MutableState<Boolean>,
    val tab: MutableState<String>,
    val collapsedSections: MutableState<Set<String>>,
    val menuEntry: MutableState<LibraryEntry?>,
    val menuIsWholeShow: MutableState<Boolean>,
    val ruleNowSec: MutableState<Long>
)

@Composable
fun rememberLibraryUiState(
    context: Context,
    initialTab: String = "movies"
): LibraryUiState {
    val designStore = remember(context) { LibraryDesignStore(context) }
    return LibraryUiState(
        designStore = designStore,
        designRevision = remember { mutableStateOf(0) },
        showCustomize = rememberSaveable { mutableStateOf(false) },
        showCollections = rememberSaveable { mutableStateOf(false) },
        openCollectionId = rememberSaveable { mutableStateOf<String?>(null) },
        showSmartCollections = rememberSaveable { mutableStateOf(false) },
        extrasRevision = remember { mutableStateOf(0) },
        query = rememberSaveable { mutableStateOf("") },
        searchOpen = rememberSaveable { mutableStateOf(false) },
        searchScope = rememberSaveable { mutableStateOf("all") },
        sortBy = rememberSaveable { mutableStateOf("recent") },
        selectedGenre = rememberSaveable { mutableStateOf("") },
        showGenrePicker = rememberSaveable { mutableStateOf(false) },
        showMissingFiles = rememberSaveable { mutableStateOf(false) },
        showRenameSuggestions = rememberSaveable { mutableStateOf(false) },
        tab = rememberSaveable { mutableStateOf(initialTab) },
        collapsedSections = remember(initialTab) { mutableStateOf(setOf<String>()) },
        menuEntry = remember { mutableStateOf<LibraryEntry?>(null) },
        menuIsWholeShow = remember { mutableStateOf(false) },
        ruleNowSec = remember { mutableStateOf(System.currentTimeMillis() / 1000) }
    )
}
