package com.opticast.player.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.PersonalCollection
import com.opticast.player.ui.screens.LibraryDesignStore

/**
 * Gold Standard — Screen state holder extracted from LibraryScreen.kt
 * Single responsibility: holds UI state that was previously 400+ lines of rememberSaveable in composable
 * Makes LibraryScreen thin, testable, and reduces 1335 → ~800 lines in first step
 */
class LibraryScreenStateHolder(
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
    val ruleNowSec: MutableState<Long>,
    val allCollections: List<PersonalCollection>,
    val smartCollections: List<PersonalCollection>,
    val recentQueries: List<String>
) {
    fun toggleSection(id: String, tab: String, prefs: android.content.SharedPreferences) {
        val newSet = com.opticast.player.ui.components.toggledDiscoverySections(collapsedSections.value, id)
        collapsedSections.value = newSet
        prefs.edit().putStringSet("collapsed_$tab", newSet).apply()
    }
}

@Composable
fun rememberLibraryScreenStateHolder(
    context: android.content.Context,
    tab: String,
    entries: List<LibraryEntry>,
    progressTick: Int
): LibraryScreenStateHolder {
    val designStore = remember(context) { LibraryDesignStore(context) }
    val designRevision = remember { mutableStateOf(0) }
    val showCustomize = rememberSaveable { mutableStateOf(false) }
    val showCollections = rememberSaveable { mutableStateOf(false) }
    val openCollectionId = rememberSaveable { mutableStateOf<String?>(null) }
    val extrasStore = remember(context) { com.opticast.player.data.local.LibraryExtrasStore(context) }
    val extrasRevision = remember { mutableStateOf(0) }
    val showSmartCollections = rememberSaveable { mutableStateOf(false) }
    val query = rememberSaveable { mutableStateOf("") }
    val searchOpen = rememberSaveable { mutableStateOf(false) }
    val searchScope = rememberSaveable { mutableStateOf("all") }
    val sortBy = rememberSaveable { mutableStateOf("recent") }
    val selectedGenre = rememberSaveable { mutableStateOf("") }
    val showGenrePicker = rememberSaveable { mutableStateOf(false) }
    val showMissingFiles = rememberSaveable { mutableStateOf(false) }
    val showRenameSuggestions = rememberSaveable { mutableStateOf(false) }
    val tabState = rememberSaveable { mutableStateOf(tab) }
    val menuEntry = remember { mutableStateOf<LibraryEntry?>(null) }
    val menuIsWholeShow = remember { mutableStateOf(false) }
    val ruleNowSec = remember { mutableStateOf(System.currentTimeMillis() / 1000) }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        ruleNowSec.value = System.currentTimeMillis() / 1000
    }

    val smartRules = remember(extrasRevision.value) { extrasStore.rules() }
    val smartCollections = remember(smartRules, entries, progressTick, ruleNowSec.value) {
        smartRules.map { rule ->
            com.opticast.player.data.model.PersonalCollection(
                "smart:" + rule.id,
                rule.name,
                entries.filter { entry ->
                    com.opticast.player.data.local.smartRuleMatches(
                        rule,
                        entry,
                        AppContainer.playbackState.state(entry.video.id)?.isWatched == true,
                        ruleNowSec.value
                    )
                }.map { it.video.id }
            )
        }
    }
    val personalCollections = remember(designRevision.value) { designStore.collections() }
    val allCollections = personalCollections + smartCollections
    val recentQueries = remember(designRevision.value) { designStore.history() }

    val collapsePreferences = context.getSharedPreferences("discovery_sections", android.content.Context.MODE_PRIVATE)
    val collapsedSections = remember(tabState.value) {
        mutableStateOf(
            collapsePreferences.getStringSet("collapsed_${tabState.value}", collapsePreferences.getStringSet("collapsed", emptySet()))?.toSet()
                ?: emptySet()
        )
    }

    return LibraryScreenStateHolder(
        designStore = designStore,
        designRevision = designRevision,
        showCustomize = showCustomize,
        showCollections = showCollections,
        openCollectionId = openCollectionId,
        showSmartCollections = showSmartCollections,
        extrasRevision = extrasRevision,
        query = query,
        searchOpen = searchOpen,
        searchScope = searchScope,
        sortBy = sortBy,
        selectedGenre = selectedGenre,
        showGenrePicker = showGenrePicker,
        showMissingFiles = showMissingFiles,
        showRenameSuggestions = showRenameSuggestions,
        tab = tabState,
        collapsedSections = collapsedSections,
        menuEntry = menuEntry,
        menuIsWholeShow = menuIsWholeShow,
        ruleNowSec = ruleNowSec,
        allCollections = allCollections,
        smartCollections = smartCollections,
        recentQueries = recentQueries
    )
}
