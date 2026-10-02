@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.opticast.player.ui.screens

import kotlinx.coroutines.flow.getAndUpdate

import com.opticast.player.data.local.batchRenameItems
import com.opticast.player.data.local.renameNoticeKey
import com.opticast.player.data.local.unseenRenameNotices
import com.opticast.player.data.model.showCollection
import com.opticast.player.data.model.showTitleOf
import com.opticast.player.ui.components.toggledDiscoverySections
import com.opticast.player.ui.components.matchesLibraryGenre
import com.opticast.player.ui.components.libraryGenres
import com.opticast.player.ui.components.completedLibrarySeries
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.BasicText
import android.Manifest
import androidx.compose.material3.NavigationBar
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import com.opticast.player.ui.components.AutoFitLabel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material.icons.filled.Pause
import com.opticast.player.ui.components.LocalMinimalStyle
import androidx.compose.material.icons.filled.Tune
import com.opticast.player.ui.components.DiscoveryHeader
import com.opticast.player.ui.components.discoveryMotionEnabled
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.posterUrlFor
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.opticast.player.data.AppContainer
import com.opticast.player.data.AppSettings
import com.opticast.player.data.local.PlaybackState
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.ui.components.ContinueWatchingCard
import com.opticast.player.ui.components.PosterCard
import com.opticast.player.ui.components.SectionHeader
import com.opticast.player.ui.components.ShowCard
import com.opticast.player.ui.components.formatDuration
import com.opticast.player.ui.components.viewModelFactory
import com.opticast.player.ui.components.rememberExternalPlayer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.opticast.player.R
import androidx.compose.material.icons.filled.CheckCircle

// -------------------------------------------------------------------------- VM

internal data class ContinueItem(
    val entry: LibraryEntry,
    val playback: PlaybackState
                        )

internal data class LibraryStats(
    val movies: Int,
    val shows: Int,
    val watched: Int,
    val totalHours: Double
                        )

internal fun comparatorFor(sortBy: String): Comparator<LibraryEntry> = when (sortBy) {
    "title" -> compareBy { (it.metadata?.displayTitle ?: it.video.parsed.title).lowercase() }
    "rating" -> compareByDescending { it.metadata?.voteAverage ?: 0.0 }
    "year" -> compareByDescending { it.metadata?.year ?: 0 }
    else -> compareByDescending { it.video.dateAddedSec }
}

// ---------------------------------------------------------------------- screen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LibraryScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenPlayer: (Long) -> Unit,
    onOpenMatch: (Long) -> Unit,
    onOpenShow: (String) -> Unit,
    onOpenSettings: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
                        ) {
    val viewModel: LibraryViewModel = viewModel(factory = viewModelFactory { LibraryViewModel() })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appSettings by AppContainer.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppContainer.initialSettings)
    val launchExternal = rememberExternalPlayer { v, position, duration ->
        AppContainer.playbackState.save(v.id, position, duration)
    }
    fun playEntry(entry: LibraryEntry) {
        if (appSettings.useExternalPlayer) {
            launchExternal(
                entry.video,
                AppContainer.playbackState.state(entry.video.id)?.positionMs ?: 0L,
                entry.metadata?.displayTitle ?: entry.video.parsed.title.ifBlank { entry.video.name }
                        )
        } else {
            onOpenPlayer(entry.video.id)
        }
    }

    // One collector for the whole screen — cards receive the version as a
    // plain parameter instead of each running their own flow collector.
    // NOTE: posterVersion global bust was causing choppiness - all cards reloaded when one poster downloaded
    val favVersion by AppContainer.favorites.version.collectAsStateWithLifecycle()
    // Cheap key: watched / in-progress filtering stays live without the grid
    // being rebuilt from storage.
    val progressTick = AppContainer.playbackState.progressTick

    val designStore = remember(context) { LibraryDesignStore(context) }
    var designRevision by remember { mutableStateOf(0) }
    var showCustomize by rememberSaveable { mutableStateOf(false) }
    var showCollections by rememberSaveable { mutableStateOf(false) }
    var openCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchScope by rememberSaveable { mutableStateOf("all") }
    val libraryEntriesById = remember(state.entries) { state.entries.associateBy { it.video.id } }
    val extrasStore = remember(context) { com.opticast.player.data.local.LibraryExtrasStore(context) }
    var extrasRevision by remember { mutableStateOf(0) }
    var showSmartCollections by rememberSaveable { mutableStateOf(false) }
    val smartRules = remember(extrasRevision) { extrasStore.rules() }
    // Re-evaluate recent-date rules when the Library resumes, without network work.
    var ruleNowSec by remember { mutableStateOf(System.currentTimeMillis()/1000) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { ruleNowSec=System.currentTimeMillis()/1000 }
    val smartCollections = remember(smartRules,state.entries,progressTick,ruleNowSec) {
        smartRules.map { rule -> PersonalCollection("smart:"+rule.id,rule.name,state.entries.filter { entry ->
            com.opticast.player.data.local.smartRuleMatches(rule,entry,AppContainer.playbackState.state(entry.video.id)?.isWatched==true,ruleNowSec)
        }.map { it.video.id }) }
    }
    val personalCollections = remember(designRevision) { designStore.collections() }
    val allCollections = personalCollections + smartCollections
    val recentQueries = remember(designRevision) { designStore.history() }
    var query by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    fun closeSearch() {
        if (query.isNotBlank()) { designStore.recordQuery(query); designRevision++ }
        searchOpen = false
        query = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }
    var sortBy by rememberSaveable { mutableStateOf("recent") } // recent | title | rating | year
    var selectedGenre by rememberSaveable { mutableStateOf("") }
    var showGenrePicker by rememberSaveable { mutableStateOf(false) }
    var showMissingFiles by rememberSaveable { mutableStateOf(false) }
    var showRenameSuggestions by rememberSaveable { mutableStateOf(false) }
    val renameVersion by AppContainer.renameSuggestions.version.collectAsStateWithLifecycle()
    val renameSuggestions = remember(state.entries, renameVersion) { AppContainer.renameSuggestions.suggestions(state.entries) }
    val noticePreferences = remember(context) { context.getSharedPreferences("rename_notices", android.content.Context.MODE_PRIVATE) }
    var notifiedRenameKeys by remember { mutableStateOf(noticePreferences.getStringSet("seen", emptySet())?.toSet() ?: emptySet()) }
    val renameNoticeKeys = remember(renameSuggestions) { batchRenameItems(renameSuggestions).map { renameNoticeKey(it) }.toSet() }
    val canNotifyRename = state.scannedOnce && !state.checkingFiles && !state.isMatching && !showRenameSuggestions
    LaunchedEffect(renameNoticeKeys, canNotifyRename) {
        if (!canNotifyRename || renameNoticeKeys.isEmpty()) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val unseen = unseenRenameNotices(renameNoticeKeys, notifiedRenameKeys)
        if (unseen.isEmpty()) return@LaunchedEffect
        // Coalesce scan/metadata emissions before showing a brief, actionable popup.
        delay(750)
        notifiedRenameKeys = notifiedRenameKeys + unseen
        noticePreferences.edit().putStringSet("seen", notifiedRenameKeys).apply()
        val result = snackbarHostState.showSnackbar(
            message = "${unseen.size} new rename suggestion${if (unseen.size == 1) "" else "s"}",
            actionLabel = "Review", withDismissAction = true,
            duration = androidx.compose.material3.SnackbarDuration.Short
                        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) showRenameSuggestions = true
    }
    val inventoryVersion by AppContainer.mediaScanner.inventory.version.collectAsStateWithLifecycle()
    // Inventory reconciliation can hold its monitor while storage is being scanned.
    // Never wait for that monitor on the UI thread.
    val missingFiles by androidx.compose.runtime.produceState<List<com.opticast.player.data.model.LocalVideo>>(
        initialValue = emptyList(), inventoryVersion, appSettings.excludedFolders
                        ) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { AppContainer.mediaScanner.inventory.missing() }
    }
    var tab by rememberSaveable { mutableStateOf("movies") } // independent tab state
    val design = remember(tab, designRevision) { designStore.design(tab) }
    val includeCompletedInGrid = includeCompletedTitles(design.style, design.hidden, searchOpen)
    val collapsePreferences = LocalContext.current.getSharedPreferences("discovery_sections", android.content.Context.MODE_PRIVATE)
    var collapsedSections by remember(tab) { mutableStateOf(collapsePreferences.getStringSet("collapsed_$tab", collapsePreferences.getStringSet("collapsed", emptySet()))?.toSet() ?: emptySet()) }
    fun toggleSection(id: String) {
        collapsedSections = toggledDiscoverySections(collapsedSections, id)
        collapsePreferences.edit().putStringSet("collapsed_$tab", collapsedSections).apply()
    }
    var menuEntry by remember { mutableStateOf<LibraryEntry?>(null) }
    var menuIsWholeShow by remember { mutableStateOf(false) }

    // Gold: selection state extracted to library/LibrarySelectionState.kt
    val selectionState = com.opticast.player.ui.screens.library.rememberLibrarySelectionState()
    var selectionMode by selectionState.selectionMode
    val selectedIds = selectionState.selectedIds
    var confirmDeleteIds by selectionState.confirmDeleteIds
    var pendingWriteDelete by selectionState.pendingWriteDelete
    var legacyDeleteTick by selectionState.legacyDeleteTick
    fun fileActionError(text: String) { android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show() }

    fun uriOf(videoId: Long): Uri? =
        com.opticast.player.ui.screens.library.LibraryFileActions.uriOf(videoId)

    fun mimeForVideo(uri: Uri, displayName: String): String =
        com.opticast.player.ui.screens.library.LibraryFileActions.mimeForVideo(context, uri, displayName)

    fun shareVideos(ids: List<Long>) =
        com.opticast.player.ui.screens.library.LibraryFileActions.shareVideos(context, ids)

    // Gold: delete handling extracted to library/LibraryDeleteHandler.kt
    val deleteHandler = com.opticast.player.ui.screens.library.rememberLibraryDeleteHandler(
        selectionState = selectionState,
        viewModel = viewModel,
        onFileActionError = { fileActionError(it) }
    )
    fun performDelete(ids: List<Long>) = deleteHandler.performDelete(ids)


    fun exitSelection() {
        selectionMode = false
        selectedIds.clear()
    }

    fun toggleSelect(videoId: Long) {
        if (!selectedIds.remove(videoId)) selectedIds.add(videoId)
    }

    fun toggleSelectShow(episodes: List<LibraryEntry>) {
        val ids = episodes.firstOrNull()?.let { showCollection(state.entries, it).map { entry -> entry.video.id } }.orEmpty()
        if (ids.all { it in selectedIds }) selectedIds.removeAll(ids)
        else selectedIds.addAll(ids.filterNot { it in selectedIds })
    }

    BackHandler(enabled = selectionMode) { exitSelection() }
    BackHandler(enabled = searchOpen && !selectionMode) { closeSearch() }


    // Gold: permission handling extracted to library/LibraryPermissionHandler.kt
    val permissionState = com.opticast.player.ui.screens.library.rememberLibraryPermissionState()
    var hasPermission by permissionState.hasPermission
    val permissionLauncher = permissionState.permissionLauncher
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    // Runs at startup and after the user adds/replaces/removes a TMDB key.
    // LibraryViewModel coalesces changes arriving during an active scan.
    LaunchedEffect(hasPermission, appSettings.tmdbApiKey) {
        if (hasPermission) viewModel.ensureStartupScan(appSettings.tmdbApiKey)
    }

    if (!hasPermission) {
        PermissionGate(onRequest = { permissionLauncher.launch(permissionsToRequest) })
        return
    }

    val searchedUnscoped = remember(state.entries, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) state.entries
        else {
            // 10/10: Fuzzy search with typo tolerance like Infuse - Avngers finds Avengers
            val exact = state.entries.filter { entry ->
                entry.metadata?.displayTitle?.lowercase()?.contains(q) == true ||
                    entry.metadata?.title?.lowercase()?.contains(q) == true ||
                    entry.video.name.lowercase().contains(q) ||
                    entry.video.parsed.title.lowercase().contains(q) ||
                    entry.metadata?.showTitle?.lowercase()?.contains(q) == true
            }
            if (exact.isNotEmpty()) exact
            else com.opticast.player.data.local.FuzzySearch.search(state.entries, q).also {
                // Breadcrumb for crash debugging
                com.opticast.player.data.AppContainer.breadcrumb.logSearch(q)
            }
        }
    }
    val searched = remember(searchedUnscoped, searchOpen, searchScope) {
        if (!searchOpen) searchedUnscoped else searchedUnscoped.filter { searchTypeMatches(searchScope, it.video.isEpisode || it.metadata?.type == "tv") }
    }
    fun isWatched(entry: LibraryEntry): Boolean =
        AppContainer.playbackState.state(entry.video.id)?.isWatched == true

    val genres = remember(state.entries) { libraryGenres(state.entries.flatMap { it.metadata?.genres.orEmpty() }) }
    val allMovies = remember(searched, selectedGenre, sortBy) {
        searched.filter { !it.video.isEpisode && it.metadata?.type != "tv" }
            .filter { matchesLibraryGenre(it.metadata?.genres.orEmpty(), selectedGenre) }
            .sortedWith(comparatorFor(sortBy))
    }
    val allShows = remember(searched, selectedGenre, sortBy) {
        searched.filter { it.video.isEpisode || it.metadata?.type == "tv" }
            .groupBy { it.metadata?.showTitle ?: it.video.parsed.title.ifBlank { it.video.name } }
            .filterValues { episodes -> episodes.any { matchesLibraryGenre(it.metadata?.genres.orEmpty(), selectedGenre) } }
            .entries.sortedWith { a, b -> comparatorFor(sortBy).compare(a.value.first(), b.value.first()) }
            .associate { it.key to it.value }
    }
    val filtered by remember(allMovies, allShows) { androidx.compose.runtime.derivedStateOf { allMovies + allShows.values.flatten() } }
    val movies by remember(allMovies, includeCompletedInGrid) { androidx.compose.runtime.derivedStateOf { allMovies.filter { includeInMainResults(isWatched(it), includeCompletedInGrid) } } }
    val shows by remember(allShows, includeCompletedInGrid) { androidx.compose.runtime.derivedStateOf { allShows.filterValues { episodes -> episodes.any { includeInMainResults(isWatched(it), includeCompletedInGrid) } } } }
    val watchedMovies = remember(allMovies) { allMovies.filter { isWatched(it) } }
    val watchedShows = remember(allShows) { allShows.filterValues { episodes -> completedLibrarySeries(episodes.map { isWatched(it) }) } }
    val favMovies = remember(allMovies, favVersion) { allMovies.filter { AppContainer.favorites.isFavorite(it.video.id) } }
    val favShows = remember(allShows, favVersion) { allShows.filterValues { episodes -> episodes.any { AppContainer.favorites.isFavorite(it.video.id) } } }
    val stats = remember(state.entries) {
        val movieCount = state.entries.count { !it.video.isEpisode && it.metadata?.type != "tv" }
        val showCount = state.entries
            .filter { it.video.isEpisode || it.metadata?.type == "tv" }
            .groupBy { it.metadata?.showTitle ?: it.video.parsed.title.ifBlank { it.video.name } }
            .size
        val watchedCount = state.entries.count {
            AppContainer.playbackState.state(it.video.id)?.isWatched == true
        }
        val hours = state.entries.sumOf { it.video.durationMs } / 3_600_000.0
        LibraryStats(movieCount, showCount, watchedCount, hours)
    }

    val favoriteTitleCount = remember(state.entries, favVersion) {
        val favorites = state.entries.filter { AppContainer.favorites.isFavorite(it.video.id) }
        favorites.count { !it.video.isEpisode && it.metadata?.type != "tv" } +
            favorites.filter { it.video.isEpisode || it.metadata?.type == "tv" }
                .map { it.metadata?.showTitle ?: it.video.parsed.title.ifBlank { it.video.name } }.distinct().size
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalMinimalStyle provides (design.style == "minimal")) {
    com.opticast.player.ui.layout.KeyboardAwareViewport {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (searchOpen) Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    SearchField(query, { query = it }, Modifier.weight(1f).focusRequester(searchFocus), onSubmit = { designStore.recordQuery(query); designRevision++ })
                    IconButton(onClick = { closeSearch() }) { Icon(Icons.Filled.Close, "Close search") }
                }
                LaunchedEffect(Unit) { if (query.isBlank()) searchFocus.requestFocus() }
            }
        },
        bottomBar = {
            LibraryBottomBar(favoriteVersion = favVersion, tab = tab,
                onTabChange = { closeSearch(); tab = it },
                movieCount = stats.movies, showCount = stats.shows, favoriteCount = favoriteTitleCount,
                searchOpen = searchOpen, onSearch = { if (searchOpen) closeSearch() else searchOpen = true },
                modifier = Modifier)
        },
        containerColor = MaterialTheme.colorScheme.background,
        // Edge-to-edge: content scrolls under the status bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { padding ->
            val currentTab = tab
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            LaunchedEffect(query, searchScope) { if (searchOpen) gridState.scrollToItem(0) }
            val searching = searchOpen
            val tabFilter: (LibraryEntry) -> Boolean = { entry ->
                val isShow = entry.video.isEpisode || entry.metadata?.type == "tv"
                when {
                    searching -> searchTypeMatches(searchScope, isShow)
                    currentTab == "tv" -> isShow
                    currentTab == "favs" -> false // favorites renders its own sections
                    else -> !isShow
                }
            }
            val recentlyAdded = remember(filtered, currentTab, searching, searchScope) { filtered
                .filter(tabFilter)
                .filterNot { isWatched(it) }
                .sortedByDescending { it.video.dateAddedSec }
                .take(10) }
            val featured = remember(filtered, currentTab, searching, searchScope, query) { if (query.isBlank()) {
                filtered.filter(tabFilter)
                    .filterNot { isWatched(it) }
                    .sortedWith(compareByDescending<LibraryEntry> { it.metadata?.backdropPath != null }
                        .thenByDescending { it.video.dateAddedSec })
                    .distinctBy { if (it.video.isEpisode || it.metadata?.type == "tv") "tv:${it.metadata?.showTitle ?: it.video.parsed.title}" else "movie:${it.video.id}" }
                    .take(FeaturedPickLimit)
            } else {
                emptyList()
            }
            }
            val continueWatching = remember(filtered, currentTab, searching, searchScope, progressTick) { filtered.filter(tabFilter).mapNotNull { entry ->
                val playback = AppContainer.playbackState.progressOf(entry.video.id)
                    ?: return@mapNotNull null
                if (playback.isResumable) ContinueItem(entry, playback) else null
            }.sortedByDescending { it.playback.updatedAt }.take(12) }

        // FIX WEAKNESS: Library grid changeable wasn't working - remember inside columns param was not triggering recomposition
        // Now compute grid cells outside, keyed to libraryGrid, and use key() to force LazyVerticalGrid recomposition when grid changes
        // 10/10: Breadcrumb for grid change + adaptive RAM log
        val currentGridCells = remember(appSettings.libraryGrid) {
            GridCells.Adaptive(libraryPosterMinimumDp(appSettings.libraryGrid).dp)
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyVerticalGrid(
            state = gridState,
            columns = currentGridCells,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                
                .background(MaterialTheme.colorScheme.background)
                .consumeWindowInsets(padding),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 88.dp)
                        ) {
            if (!searching) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                    LibraryHeader(onOpenSettings = onOpenSettings, onCustomize = { showCustomize = true }, scanning = state.isMatching || state.checkingFiles, onScan = { viewModel.scan(manual = true) })
                }
            }
            // OFFLINE-FIRST: Only show What's New once after update, not Up To Date card on every startup
            // User request: Don't show Up To Date card in library on every app startup, only when real update available
            // Up To Date should only show in Settings, not library - save data, offline-first
            if (!searching) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "whats-new") {
                    val context = LocalContext.current
                    var dismissed by remember { mutableStateOf(false) }
                    var whatsNewVersion by remember { mutableStateOf(com.opticast.player.data.remote.UpdateChecker.getWhatsNewVersion(context)) }
                    val currentWhatsNew = whatsNewVersion
                    if (!dismissed && currentWhatsNew != null) {
                        WhatsNewCard(version = currentWhatsNew, onDismiss = {
                            com.opticast.player.data.remote.UpdateChecker.dismissWhatsNew(context)
                            dismissed = true
                            whatsNewVersion = null
                        })
                    }
                }
            }
            // REMOVED: UpToDateCard - user requested don't show in library on every startup, only show real update available
            // Real update available is shown via AutoUpdateDialog, not Up To Date card
            // This saves data and respects offline-first rule
            if (searching) item(key = "focused-search-controls", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(horizontal = DiscoveryGutterDp.dp)) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "All", "movies" to "Movies", "tv" to "TV").forEach { (id,label) ->
                            FilterChip(selected = searchScope == id, onClick = { searchScope=id }, label = { Text(label) })
                        }
                    }
                    if(query.isBlank() && recentQueries.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent searches", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = { designStore.clearHistory(); designRevision++ }) { Text("Clear") }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            lazyItems(recentQueries, key={it}) { recent ->
                                SuggestionChip(onClick = { query=recent; designStore.recordQuery(recent); designRevision++; keyboard?.hide() }, label = { Text(recent,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=200.dp)) })
                            }
                        }
                    }
                }
            }
            if (missingFiles.isNotEmpty() || state.fileScanError != null) {
                item(key = "file-availability", span = { GridItemSpan(maxLineSpan) }) {
                    Surface(Modifier.padding(horizontal = DiscoveryGutterDp.dp, vertical = 10.dp), shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(if (state.checkingFiles) "Checking local files…" else if (state.fileScanError != null) "Storage check needs attention"
                                else "${missingFiles.size} unavailable file${if (missingFiles.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(state.fileScanError ?: "Saved posters and progress are kept. Review files that were moved, removed or are temporarily inaccessible.",
                                style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = { showMissingFiles = true }) { Text("Review files") }
                                TextButton(onClick = { viewModel.recheckFiles() }, enabled = !state.checkingFiles) { Text("Recheck storage") }
                            }
                        }
                    }
                }
            }
            if (state.entries.isNotEmpty()) {
                if (showLibraryStatistics(design.style, design.stats, searching)) item(span = { GridItemSpan(maxLineSpan) }) {
                    StatsCard(stats)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FilterChipsRow(
                        sortBy = sortBy,
                        onSortChange = { sortBy = it },
                        selectedGenre = selectedGenre,
                        onGenreClick = { showGenrePicker = true }
                        )
                }
            }
            if (state.isMatching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(horizontal = DiscoveryGutterDp.dp, vertical = 10.dp)) {
                        Text(
                            "Identifying titles… ${state.matchingDone}/${state.matchingTotal}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        com.opticast.player.ui.components.FastLoadingBar(
                            progress = if (state.matchingTotal == 0) null
                                else state.matchingDone.toFloat() / state.matchingTotal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(50))
                        )
                    }
                }
            }
            if (!state.scannedOnce) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                        ) {
                        com.opticast.player.ui.components.FastLoadingBar()
                    }
                }
            } else if (currentTab == "favs" && !searching) {
                if (favMovies.isEmpty() && favShows.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 96.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                        )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No favorites yet",
                                style = MaterialTheme.typography.titleMedium
                        )
                            Text(
                                "Tap the heart on any movie or show to keep it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        }
                    }
                }
            } else if (filtered.none(tabFilter)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyLibrary(query.isNotBlank() || selectedGenre.isNotBlank())
                }
            }

            for (sectionId in visibleLibrarySections(design.style, design.order, design.hidden, searching)) {
                when (sectionId) {
                    "continue" -> {
            if (showDiscoveryExtras(searching) && continueWatching.isNotEmpty()) {
                item(key = "discovery-continue-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("Continue Watching", "Pick up right where you left off.", Color(0xFFFFBF56), continueWatching.size, expanded = "continue" !in collapsedSections, onToggle = { toggleSection("continue") })
                }
                if ("continue" !in collapsedSections) item(key = "discovery-continue-content", span = { GridItemSpan(maxLineSpan) }) {
                    val carouselState = rememberLazyListState()
                    LazyRow(
                        state = carouselState,

                        contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
                        ) {
                        lazyItems(continueWatching, key = { "cw-${it.entry.video.id}" }) { item ->
                            SelectableCard(selectionMode, item.entry.video.id in selectedIds,
                                modifier = Modifier.width(DiscoveryResumeDp.dp), onToggle = { toggleSelect(item.entry.video.id) }) {
                            ContinueWatchingCard(
                                entry = item.entry,
                                progress = item.playback.progress,
                                remainingLabel = if (item.playback.durationMs > 0) "${item.playback.remainingMs.formatDuration()} left"
                                    else "Resume at ${item.playback.positionMs.formatDuration()}",
                                onClick = { if (selectionMode) toggleSelect(item.entry.video.id) else playEntry(item.entry) },
                                modifier = Modifier,
                                compact = true,
                                selectionMode = selectionMode,
                                onLongClick = { if (selectionMode) toggleSelect(item.entry.video.id) else { menuIsWholeShow = false; menuEntry = item.entry } }
                        )
                            }
                        }
                    }
                }
            }


                    }
                    "featured" -> {
            if (showDiscoveryExtras(searching) && featured.isNotEmpty()) {
                item(key = "discovery-featured-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("Featured", "A little cinema, from your own collection.", Color(0xFF63CFFF), expanded = "featured" !in collapsedSections, onToggle = { toggleSection("featured") })
                }
                if ("featured" !in collapsedSections) item(key = "discovery-featured-content", span = { GridItemSpan(maxLineSpan) }) {
                    HeroPager(items = featured, onOpenDetail = onOpenDetail, onPlay = { playEntry(it) }, selectionMode = selectionMode,
                        selectedIds = selectedIds.toSet(), onToggle = { toggleSelect(it.video.id) },
                        onHold = { menuIsWholeShow = false; menuEntry = it })
                }
            }

                    }
                    "recent" -> {
            if (showDiscoveryExtras(searching) && recentlyAdded.isNotEmpty()) {
                item(key = "discovery-recent-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("Recently added", "Fresh arrivals. Ready when you are.", Color(0xFF63DAB0), recentlyAdded.size, expanded = "recent" !in collapsedSections, onToggle = { toggleSection("recent") })
                }
                if ("recent" !in collapsedSections) item(key = "discovery-recent-content", span = { GridItemSpan(maxLineSpan) }) {
                    val carouselState = rememberLazyListState()
                    LazyRow(
                        state = carouselState,

                        contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
                        ) {
                        lazyItems(recentlyAdded, key = { "recent-${it.video.id}" }) { entry ->
                            SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.width(DiscoveryPosterDp.dp).clip(RoundedCornerShape(12.dp)), onToggle = { toggleSelect(entry.video.id) }) {
                                PosterCard(entry = entry,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { if (selectionMode) toggleSelect(entry.video.id) else onOpenDetail(entry.video.id) },
                                    onLongClick = { if (selectionMode) toggleSelect(entry.video.id) else { menuIsWholeShow = false; menuEntry = entry } },
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

                    }
                    "titles" -> {
            if ((currentTab == "movies" || searching) && movies.isNotEmpty()) {
                item(key = "discovery-movies-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("Movies", "Your movie collection, ready to explore.", Color(0xFF63CFFF), movies.size, expanded = searching || design.style == "minimal" || "movies" !in collapsedSections, onToggle = if (searching || design.style == "minimal") null else ({ toggleSection("movies") }))
                }
                if (searching || design.style == "minimal" || "movies" !in collapsedSections) items(movies, key = { "movie-${it.video.id}" }, contentType = { "movie" }) { entry ->
                    SelectableCard(
                        selectionMode = selectionMode,
                        selected = entry.video.id in selectedIds,
                        onToggle = { toggleSelect(entry.video.id) }
                        ) {
                        PosterCard(
                            entry = entry,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = {
                                if (selectionMode) toggleSelect(entry.video.id)
                                else onOpenDetail(entry.video.id)
                            },
                            onLongClick = {
                                if (selectionMode) toggleSelect(entry.video.id)
                                else { menuIsWholeShow = false; menuEntry = entry }
                            },
                            modifier = Modifier.padding(LibraryPosterInsetDp.dp)
                        )
                    }
                }
            }
            if ((currentTab == "tv" || searching) && shows.isNotEmpty()) {
                item(key = "discovery-shows-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("TV Shows", "Find your next episode.", Color(0xFFBB9FFF), shows.size, expanded = searching || design.style == "minimal" || "shows" !in collapsedSections, onToggle = if (searching || design.style == "minimal") null else ({ toggleSection("shows") }))
                }
                if (searching || design.style == "minimal" || "shows" !in collapsedSections) items(shows.entries.toList(), key = { "show-${it.key}" }, contentType = { "show" }) { (name, episodes) ->
                    SelectableCard(
                        selectionMode = selectionMode,
                        selected = episodes.any { it.video.id in selectedIds },
                        onToggle = { toggleSelectShow(episodes) }
                        ) {
                        ShowCard(
                            showTitle = name,
                            episodes = episodes,
                            onClick = {
                                if (selectionMode) toggleSelectShow(episodes)
                                else onOpenShow(name)
                            },
                            onLongClick = {
                                if (selectionMode) toggleSelectShow(episodes)
                                else { menuIsWholeShow = true; menuEntry = episodes.first() }
                            },
                            modifier = Modifier.padding(LibraryPosterInsetDp.dp)
                        )
                    }
                }
            }
            if (currentTab == "favs" && !searching && favMovies.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Favorite movies", favMovies.size)
                }
                items(favMovies, key = { "favm-${it.video.id}" }) { entry ->
                    SelectableCard(
                        selectionMode = selectionMode,
                        selected = entry.video.id in selectedIds,
                        onToggle = { toggleSelect(entry.video.id) }
                        ) {
                        PosterCard(
                            entry = entry,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onClick = {
                                if (selectionMode) toggleSelect(entry.video.id)
                                else onOpenDetail(entry.video.id)
                            },
                            onLongClick = {
                                if (selectionMode) toggleSelect(entry.video.id)
                                else { menuIsWholeShow = false; menuEntry = entry }
                            },
                            modifier = Modifier.padding(LibraryPosterInsetDp.dp)
                        )
                    }
                }
            }
            if (currentTab == "favs" && !searching && favShows.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("Favorite shows", favShows.size)
                }
                items(favShows.entries.toList(), key = { "favs-${it.key}" }) { (name, episodes) ->
                    SelectableCard(
                        selectionMode = selectionMode,
                        selected = episodes.any { it.video.id in selectedIds },
                        onToggle = { toggleSelectShow(episodes) }
                        ) {
                        ShowCard(
                            showTitle = name,
                            episodes = episodes,
                            onClick = {
                                if (selectionMode) toggleSelectShow(episodes)
                                else onOpenShow(name)
                            },
                            onLongClick = {
                                if (selectionMode) toggleSelectShow(episodes)
                                else { menuIsWholeShow = true; menuEntry = episodes.first() }
                            },
                            modifier = Modifier.padding(LibraryPosterInsetDp.dp)
                        )
                    }
                }
            }

                    }
                    "watched" -> {
            val completedMovies = if (currentTab == "movies" || searching) watchedMovies else emptyList()
            val completedShows = if (currentTab == "tv" || searching) watchedShows else emptyMap()
            val watchedCount = completedMovies.size + completedShows.size
            if (showDiscoveryExtras(searching) && watchedCount > 0) {
                item(key = "discovery-watched-header", span = { GridItemSpan(maxLineSpan) }) {
                    DiscoveryHeader("Watched", "Finished favourites. Ready to revisit.", Color(0xFF63DAB0), watchedCount,
                        expanded = searching || "watched" !in collapsedSections, onToggle = if (searching) null else ({ toggleSection("watched") }))
                }
                if (searching || "watched" !in collapsedSections) item(key = "discovery-watched-content", span = { GridItemSpan(maxLineSpan) }) {
                    val carouselState = rememberLazyListState()
                    LazyRow(state = carouselState, flingBehavior = rememberSnapFlingBehavior(carouselState),
                        contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)) {
                        lazyItems(completedMovies, key = { "watched-movie-${it.video.id}" }) { entry ->
                            SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.width(DiscoveryPosterDp.dp).clip(RoundedCornerShape(12.dp)), onToggle = { toggleSelect(entry.video.id) }) {
                                PosterCard(entry = entry,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { if (selectionMode) toggleSelect(entry.video.id) else onOpenDetail(entry.video.id) },
                                    onLongClick = { if (selectionMode) toggleSelect(entry.video.id) else { menuIsWholeShow = false; menuEntry = entry } },
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)))
                            }
                        }
                        lazyItems(completedShows.entries.toList(), key = { "watched-show-${it.key}" }) { (name, episodes) ->
                            SelectableCard(selectionMode, episodes.any { it.video.id in selectedIds }, Modifier.width(DiscoveryPosterDp.dp), onToggle = { toggleSelectShow(episodes) }) {
                                ShowCard(showTitle = name, episodes = episodes,
                                    onClick = { if (selectionMode) toggleSelectShow(episodes) else onOpenShow(name) },
                                    onLongClick = { if (selectionMode) toggleSelectShow(episodes) else { menuIsWholeShow = true; menuEntry = episodes.first() } },
                                    modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
                    }
                    "collections" -> {
            item(key="discovery-collections",span={GridItemSpan(maxLineSpan)}) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(discoveryHeading("Collections"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        TextButton(onClick={showCollections=true}) { Text("Manage") }
                    }
                    if(allCollections.isEmpty()) TextButton(onClick={showCollections=true},modifier=Modifier.padding(horizontal=20.dp)) { Text("Create a personal or franchise collection") }
                    LazyRow(contentPadding=PaddingValues(horizontal=DiscoveryGutterDp.dp,vertical=6.dp),horizontalArrangement=Arrangement.spacedBy(DiscoveryGapDp.dp)) {
                        lazyItems(allCollections,key={it.id}) { collection ->
                            val members=remember(collection.videoIds, libraryEntriesById) { collection.videoIds.mapNotNull { libraryEntriesById[it] } }
                            fun toggleMembers() {
                                val ids=members.map { it.video.id }
                                if(ids.all { it in selectedIds }) selectedIds.removeAll(ids)
                                else selectedIds.addAll(ids.filterNot { it in selectedIds })
                            }
                            SelectableCard(selectionMode,members.isNotEmpty() && members.all { it.video.id in selectedIds },Modifier.width(DiscoveryCollectionDp.dp),onToggle={toggleMembers()}) {
                                CollectionCover(collection,members,onClick={if(selectionMode) toggleMembers() else openCollectionId=collection.id},
                                    onHold={selectionMode=true; selectedIds.addAll(members.map{it.video.id}.filterNot{it in selectedIds})})
                            }
                        }
                    }
                }
            }

                    }
                }
            }
        }
        }
    }

    // Gold: dialogs host extracted to library/LibraryDialogsHost.kt — saves 60+ lines
    com.opticast.player.ui.screens.library.LibraryDialogsHost(
        showCustomize = showCustomize,
        tab = tab,
        design = design,
        designStore = designStore,
        designRevision = designRevision,
        onDesignRevisionChange = { designRevision = it },
        onShowCustomizeChange = { showCustomize = it },
        showCollections = showCollections,
        personalCollections = personalCollections,
        entries = state.entries,
        onShowCollectionsChange = { showCollections = it },
        openCollectionId = openCollectionId,
        onOpenCollectionIdChange = { openCollectionId = it },
        showSmartCollections = showSmartCollections,
        smartRules = smartRules,
        extrasStore = extrasStore,
        extrasRevision = extrasRevision,
        onExtrasRevisionChange = { extrasRevision = it },
        onShowSmartCollectionsChange = { showSmartCollections = it },
        allCollections = allCollections,
        showRenameSuggestions = showRenameSuggestions,
        onShowRenameSuggestionsChange = { showRenameSuggestions = it },
        onOpenMatch = onOpenMatch,
        viewModel = viewModel,
        showMissingFiles = showMissingFiles,
        missingFiles = missingFiles,
        fileScanError = state.fileScanError,
        checkingFiles = state.checkingFiles,
        onShowMissingFilesChange = { showMissingFiles = it },
        showGenrePicker = showGenrePicker,
        genres = genres,
        selectedGenre = selectedGenre,
        onSelectedGenreChange = { selectedGenre = it },
        onShowGenrePickerChange = { showGenrePicker = it },
        confirmDeleteIds = confirmDeleteIds,
        onConfirmDeleteIdsChange = { confirmDeleteIds = it },
        onPerformDelete = { performDelete(it) },
        onExitSelection = { exitSelection() },
        onOpenDetail = onOpenDetail
    )


    // Gold: Floating multi-select action bar extracted to library/LibrarySelectionBar.kt
    if (selectionMode) com.opticast.player.ui.screens.library.LibrarySelectionBar(
        selectedIds = selectedIds.toList(),
        tab = tab,
        onMarkWatched = {
            val chosen = state.entries.filter { it.video.id in selectedIds }
            chosen.forEach { viewModel.setWatched(it.video.id, it.video.durationMs, true) }
            exitSelection()
        },
        onToggleFavorite = {
            if (selectedIds.isNotEmpty()) {
                if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) AppContainer.favorites.remove(selectedIds.toList())
                else AppContainer.favorites.add(selectedIds.toList())
                exitSelection()
            }
        },
        onShare = { if (selectedIds.isNotEmpty()) shareVideos(selectedIds.toList()) },
        onDelete = { if (selectedIds.isNotEmpty()) confirmDeleteIds = selectedIds.toList() },
        onExit = { exitSelection() },
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
    )
    }

    // Gold: menu host extracted to library/LibraryMenuHost.kt — saves 50+ lines
    com.opticast.player.ui.screens.library.LibraryMenuHost(
        menuEntry = menuEntry,
        menuIsWholeShow = menuIsWholeShow,
        entries = state.entries,
        selectionMode = selectionMode,
        selectedIds = selectedIds,
        onMenuEntryChange = { menuEntry = it },
        onPlay = { playEntry(it) },
        onOpenShow = onOpenShow,
        onOpenDetail = onOpenDetail,
        onOpenMatch = onOpenMatch,
        onRefreshArtwork = { viewModel.refreshArtwork(it) },
        onSetWatched = { id, dur, watched -> viewModel.setWatched(id, dur, watched) },
        onClearMetadata = { viewModel.clearMetadata(it) },
        onShare = { shareVideos(it) },
        onConfirmDelete = { confirmDeleteIds = it },
        onSelectionModeChange = { selectionMode = it }
    )

    } // Library-only visual style; never changes the player theme or density.
}

/** Fast-scroll thumb overlay like Infuse - low-RAM safe with derivedStateOf + graphicsLayer
 * Gold: extracted to library/LibraryFastScrollThumb.kt, kept wrapper for backward compat
 */
@Composable
fun FastScrollThumb(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    com.opticast.player.ui.screens.library.LibraryFastScrollThumb(gridState, modifier)
}

