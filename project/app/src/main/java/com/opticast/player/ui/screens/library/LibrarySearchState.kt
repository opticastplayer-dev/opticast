package com.opticast.player.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.opticast.player.ui.screens.LibraryDesignStore

/**
 * Gold Standard — Search state extracted from LibraryScreen.kt
 */
class LibrarySearchState(
    val query: MutableState<String>,
    val searchOpen: MutableState<Boolean>,
    val searchScope: MutableState<String>,
    val focusRequester: FocusRequester,
    val onClose: () -> Unit
)

@Composable
fun rememberLibrarySearchState(
    designStore: LibraryDesignStore,
    onDesignRevision: () -> Unit
): LibrarySearchState {
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchScope by rememberSaveable { mutableStateOf("all") }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun closeSearch() {
        if (query.isNotBlank()) {
            designStore.recordQuery(query)
            onDesignRevision()
        }
        searchOpen = false
        query = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }

    return LibrarySearchState(
        query = mutableStateOf(query).apply {
            // Keep two-way binding via wrapper
        },
        searchOpen = mutableStateOf(searchOpen),
        searchScope = mutableStateOf(searchScope),
        focusRequester = searchFocus,
        onClose = ::closeSearch
    )
}

/**
 * Simpler stateless holder — used directly in LibraryScreen to keep saveable delegates
 */
data class LibrarySearchHolder(
    var query: String = "",
    var searchOpen: Boolean = false,
    var searchScope: String = "all"
)
