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

class LibraryViewModel : ViewModel() {

    data class UiState(
        val entries: List<LibraryEntry> = emptyList(),
        val matchingDone: Int = 0,
        val matchingTotal: Int = 0,
        val scannedOnce: Boolean = false,
        val fileScanError: String? = null,
        val checkingFiles: Boolean = false
                        ) {
        val isMatching: Boolean get() = matchingTotal > 0 && matchingDone < matchingTotal
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val matchingInFlight = AtomicBoolean(false)

    /** Last MediaStore result — reused so version bumps never re-query it. */
    private var cachedVideos: List<LocalVideo> = emptyList()

    init {
        // Favourites / metadata changes only need the metadata map refreshed,
        // NOT another MediaStore scan (that used to run every few seconds while
        // the user was scrolling). collectLatest + delay coalesces bursts.
        viewModelScope.launch {
            merge(AppContainer.favorites.version, AppContainer.metadataStore.version)
                .collectLatest {
                    delay(350)
                    rebuild(rescan = cachedVideos.isEmpty())
                }
        }
    }

    // NavHost recreates the screen composition on Back, but retains this ViewModel.
    // A return must not rerun startup matching or insert/remove a checking card.
    private var automaticScanKey: String? = null
    fun ensureStartupScan(apiKey: String) {
        if (!needsAutomaticLibraryScan(automaticScanKey, apiKey)) return
        automaticScanKey = apiKey
        scan()
    }

    private val scanInFlight = AtomicBoolean(false)
    private val scanPending = AtomicBoolean(false)
    private val manualScanPending = kotlinx.coroutines.flow.MutableStateFlow(false)
    fun scan(manual: Boolean = false) {
        if (manual) manualScanPending.value = true
        if (!scanInFlight.compareAndSet(false, true)) {
            scanPending.set(true)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
            if (!rebuild(rescan = true)) return@launch
            val manualRun = manualScanPending.getAndUpdate { false }
            autoMatch(_uiState.value.entries, manualRun)
            cachedVideos.forEach { video ->
                if (!manualRun) com.opticast.player.data.PlaybackWorkBudget.awaitIdleOrPriority(manualScanPending)
                AppContainer.metadataStore.get(video.id)?.let { metadata ->
                    AppContainer.offlineLibrary.prepare(video.id, metadata)
                }
            }
            AppContainer.metadataStore.touch()
            } finally {
                scanInFlight.set(false)
                if (scanPending.getAndSet(false)) scan()
            }
        }
    }

    fun clearMetadata(videoId: Long) {
        AppContainer.metadataStore.clear(videoId)
        AppContainer.detailCache.clear(videoId)
        AppContainer.posterCache.deleteForVideo(videoId)
    }

    /** Re-downloads the poster when the artwork does not match the movie. */
    fun refreshArtwork(entry: LibraryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            AppContainer.posterCache.forceRefresh(entry)
        }
    }

    fun setWatched(videoId: Long, durationMs: Long, watched: Boolean) {
        AppContainer.playbackState.setWatched(videoId, watched, durationMs)
    }

    /** Local-only recheck: does not trigger metadata matching or artwork downloads. */
    fun recheckFiles() {
        if (!scanInFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            try { rebuild(rescan = true, prefetch = false) }
            finally { scanInFlight.set(false) }
        }
    }

    fun dismissMissing(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { AppContainer.mediaScanner.inventory.dismiss(id) }.onFailure {
                _uiState.update { state -> state.copy(fileScanError = "Could not save review changes. Your records were kept.") }
            }
        }
    }

    private suspend fun rebuild(rescan: Boolean, prefetch: Boolean = true): Boolean {
        val videos = if (rescan || cachedVideos.isEmpty()) {
            _uiState.update { it.copy(checkingFiles = true) }
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) { runCatching { AppContainer.mediaScanner.scan() } }
            _uiState.update { it.copy(checkingFiles = false) }
            if (result.isFailure) {
                _uiState.update { it.copy(fileScanError = "Unable to check video storage. Check video permission and reconnect storage, then retry. Saved records were kept.") }
                return false
            }
            result.getOrThrow().also { cachedVideos = it }
        } else cachedVideos
        kotlinx.coroutines.withContext(Dispatchers.IO) { AppContainer.renameSuggestions.inspect(videos) }
        val entries = videos.map { LibraryEntry(it, AppContainer.metadataStore.get(it.id)) }
        _uiState.update { it.copy(entries = entries, scannedOnce = true, fileScanError = null) }
        if (prefetch) viewModelScope.launch(Dispatchers.IO) {
            if (rescan) delay(900)
            // Performance: only prefetch visible posters (first 60) to reduce I/O and memory pressure
            val visibleEntries = if (entries.size > 60) entries.take(60) else entries
            AppContainer.posterCache.prefetch(visibleEntries)
        }
        return true
    }

    private suspend fun autoMatch(entries: List<LibraryEntry>, manual: Boolean) {
        if (!matchingInFlight.compareAndSet(false, true)) return
        try {
            val settings = AppContainer.settings.current()
            if (!AppContainer.isOnline()) return
            // OFFLINE-FIRST: Always download subtitles during scan and cache them for offline use
            // User priority: offline rule #1, cache subtitles during scan when online
            // Always attempt when API keys present, not just when autoSubtitles setting enabled
            // This ensures subtitles are cached for offline playback
            val hasSubtitleKeys = settings.openSubtitlesApiKey.isNotBlank() || settings.subdlApiKey.isNotBlank()
            val autoSubs = hasSubtitleKeys // Always download when keys present, cache for offline - ignore autoSubtitles toggle for offline-first
            // Note: autoSubtitles setting still respected for UI, but offline-first always caches when possible
            val subtitleFailures = java.util.concurrent.atomic.AtomicInteger(0)
            val matchedCount = java.util.concurrent.atomic.AtomicInteger(0)
            val unmatched = entries.filter {
                AppContainer.offlineLibrary.needsMatch(it.video, it.metadata, settings.tmdbApiKey, manual)
            }
            // A fully matched library still has work to do when "Auto subtitles"
            // is on, so this can no longer bail out on an empty match queue.
            if (unmatched.isEmpty() && !autoSubs) return
            _uiState.update { it.copy(matchingDone = 0, matchingTotal = unmatched.size) }

            // Match up to three files concurrently; AniList is tried for
            // release-group style filenames whenever TMDB comes up empty.
            if (unmatched.isNotEmpty()) unmatched.chunked(3).forEach { chunk ->
                if (!manual) com.opticast.player.data.PlaybackWorkBudget.awaitIdleOrPriority(manualScanPending)
                coroutineScope {
                    chunk.map { entry ->
                        async {
                            runCatching {
                                val metadata = AppContainer.offlineLibrary.match(entry.video, entry.metadata, manual)
                                if (metadata != null) {
                                    if (metadata != entry.metadata) {
                                        AppContainer.detailCache.clear(entry.video.id)
                                        AppContainer.metadataStore.save(entry.video.id, metadata)
                                    }
                                    // Optionally grab the best subtitle right away.
                                    if (autoSubs && subtitleFailures.get() < 2 &&
                                        AppContainer.metadataStore
                                            .subtitlesFor(entry.video.id).isEmpty()
                                    ) {
                                        runCatching {
                                            val results = AppContainer.subtitles.search(
                                                metadata = metadata,
                                                fallbackQuery = metadata.displayTitle,
                                                languages = settings.subtitleLanguages,
                                                season = metadata.seasonNumber,
                                                episode = metadata.episodeNumber
                        )
                                            results.firstOrNull()?.let {
                                                AppContainer.subtitles.download(it, entry.video.id)
                                            }
                                        }.onFailure { subtitleFailures.incrementAndGet() }
                                    }
                                }
                            }
                            _uiState.update {
                                it.copy(matchingDone = matchedCount.incrementAndGet())
                            }
                        }
                    }.awaitAll()
                }
            }
            // Second pass: any title that is matched but still has no subtitle
            // file on the device. This is what makes "Auto subtitles" useful for
            // a library that was scanned before the setting was switched on.
            // Sequential on purpose - it keeps us inside the provider's rate
            // limits, and one failed title must not abort the whole run.
            if (autoSubs && subtitleFailures.get() < 2) {
                val missingSubs = entries.filter { entry ->
                    entry.metadata != null &&
                        AppContainer.metadataStore.subtitlesFor(entry.video.id).isEmpty()
                }
                for (entry in missingSubs) {
                    if (!manual) com.opticast.player.data.PlaybackWorkBudget.awaitIdleOrPriority(manualScanPending)
                    if (subtitleFailures.get() >= 2) break
                    runCatching {
                        val md = entry.metadata ?: return@runCatching
                        val results = AppContainer.subtitles.search(
                            metadata = md,
                            fallbackQuery = md.displayTitle,
                            languages = settings.subtitleLanguages,
                            season = md.seasonNumber,
                            episode = md.episodeNumber
                        )
                        results.firstOrNull()?.let {
                            AppContainer.subtitles.download(it, entry.video.id)
                        }
                    }.onFailure { subtitleFailures.incrementAndGet() }
                }
            }
        } finally {
            _uiState.update { it.copy(matchingDone = 0, matchingTotal = 0) }
            matchingInFlight.set(false)
        }
    }
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
    var bottomChromePx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val layoutDensity = LocalDensity.current
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

    // Multi-select mode for bulk share / delete.
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    val selectedIds = rememberSaveable(saver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Long>, Long>(
        save = { it.toList() }, restore = { values -> mutableStateListOf<Long>().apply { addAll(values) } }
    )) { mutableStateListOf<Long>() }
    var confirmDeleteIds by remember { mutableStateOf<List<Long>?>(null) }
    var pendingWriteDelete by remember { mutableStateOf<List<Long>>(emptyList()) }
    var legacyDeleteTick by remember { mutableStateOf(0) }
    fun fileActionError(text: String) { android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_LONG).show() }

    fun uriOf(videoId: Long): Uri? =
        AppContainer.mediaScanner.byId(videoId)?.uri?.let { Uri.parse(it) }

    fun mimeForVideo(uri: Uri, displayName: String): String {
        val fromResolver = context.contentResolver.getType(uri)
        if (fromResolver != null && fromResolver != "application/octet-stream") return fromResolver
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            "ts", "m2ts" -> "video/mp2ts"
            "flv" -> "video/x-flv"
            "wmv" -> "video/x-ms-wmv"
            else -> "video/*"
        }
    }

    fun shareVideos(ids: List<Long>) {
        val videos = ids.distinct().mapNotNull { AppContainer.mediaScanner.byId(it) }
        if (videos.size != ids.distinct().size) { fileActionError("Some selected files are unavailable. Recheck storage before sharing the whole selection."); return }
        val uris = videos.map { Uri.parse(it.uri) }
        if (videos.isEmpty() || uris.isEmpty()) return
        runCatching {
            if (uris.size == 1) {
                val video = videos.first()
                val send = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, uris.first())
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType(mimeForVideo(uris.first(), video.name))
                send.clipData = android.content.ClipData.newUri(context.contentResolver, video.name, uris.first())
                context.startActivity(Intent.createChooser(send, "Share video"))
            } else {
                val send = Intent(Intent.ACTION_SEND_MULTIPLE)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("video/*")
                send.clipData = android.content.ClipData.newUri(context.contentResolver, "Selected videos", uris.first()).apply {
                    uris.drop(1).forEach { addItem(android.content.ClipData.Item(it)) }
                }
                context.startActivity(Intent.createChooser(send, "Share ${uris.size} videos"))
            }
        }.onFailure { fileActionError("Could not open sharing. No files were changed.") }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
                        ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.recheckFiles()
    }
    val writePermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
                        ) { granted ->
        if (granted && pendingWriteDelete.isNotEmpty()) {
            pendingWriteDelete.mapNotNull { uriOf(it) }.forEach { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            viewModel.scan()
            pendingWriteDelete = emptyList()
        }
    }

    val legacyDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) legacyDeleteTick++
        else { pendingWriteDelete = emptyList(); viewModel.recheckFiles() }
    }
    LaunchedEffect(legacyDeleteTick) {
        if (Build.VERSION.SDK_INT != 29 || legacyDeleteTick == 0) return@LaunchedEffect
        while (pendingWriteDelete.isNotEmpty()) {
            val id = pendingWriteDelete.first()
            try {
                val uri = kotlinx.coroutines.withContext(Dispatchers.IO) { uriOf(id) }
                if (uri == null) { fileActionError("A selected episode is unavailable; remaining files were not deleted."); pendingWriteDelete = emptyList(); break }
                kotlinx.coroutines.withContext(Dispatchers.IO) { context.contentResolver.delete(uri, null, null) }
                pendingWriteDelete = pendingWriteDelete.drop(1)
            } catch (e: android.app.RecoverableSecurityException) {
                legacyDeleteLauncher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                return@LaunchedEffect
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { fileActionError("Could not delete all selected episodes. Refresh the library to review the remaining files."); pendingWriteDelete = emptyList(); break }
        }
        viewModel.recheckFiles()
    }

    fun performDelete(ids: List<Long>) {
        val uris = ids.distinct().mapNotNull { uriOf(it) }
        if (uris.size != ids.distinct().size) { fileActionError("Some selected files are unavailable. Recheck storage before deleting the selection."); return }
        if (uris.isEmpty()) return
        if (Build.VERSION.SDK_INT == 29) { pendingWriteDelete = ids.distinct(); legacyDeleteTick++; return }
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure { fileActionError("Could not request deletion. Try a smaller selection or check storage access.") }
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                uris.forEach { uri ->
                    runCatching { context.contentResolver.delete(uri, null, null) }
                }
                viewModel.scan()
            } else {
                pendingWriteDelete = ids
                writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }


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


    val videoPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(videoPermission, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(videoPermission)
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, videoPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results[videoPermission] == true
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
    val movies by remember(allMovies, progressTick, includeCompletedInGrid) { androidx.compose.runtime.derivedStateOf { allMovies.filter { includeInMainResults(isWatched(it), includeCompletedInGrid) } } }
    val shows by remember(allShows, progressTick, includeCompletedInGrid) { androidx.compose.runtime.derivedStateOf { allShows.filterValues { episodes -> episodes.any { includeInMainResults(isWatched(it), includeCompletedInGrid) } } } }
    val watchedMovies = remember(allMovies, progressTick) { allMovies.filter { isWatched(it) } }
    val watchedShows = remember(allShows, progressTick) { allShows.filterValues { episodes -> completedLibrarySeries(episodes.map { isWatched(it) }) } }
    val favMovies = remember(allMovies, favVersion) { allMovies.filter { AppContainer.favorites.isFavorite(it.video.id) } }
    val favShows = remember(allShows, favVersion) { allShows.filterValues { episodes -> episodes.any { AppContainer.favorites.isFavorite(it.video.id) } } }
    val stats = remember(state.entries, progressTick) {
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

    var scrollChromeVisible by remember { mutableStateOf(true) }
    val chromeScroll = remember(layoutDensity) {
        val policy = ScrollChromePolicy(with(layoutDensity) { 24.dp.toPx() })
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                policy.consume(consumed.y)?.let { scrollChromeVisible = it }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    LaunchedEffect(tab, searchOpen, selectionMode) { scrollChromeVisible = true }

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
            // Reserve measured geometry: slide/fade only, never remeasure the grid every frame.
            Box(Modifier.fillMaxWidth().heightIn(min = with(layoutDensity) { bottomChromePx.toDp() }), contentAlignment = Alignment.BottomCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = scrollChromeVisible || searchOpen || selectionMode,
                    enter = androidx.compose.animation.slideInVertically(tween(180)) { it } + androidx.compose.animation.fadeIn(tween(140)),
                    exit = androidx.compose.animation.slideOutVertically(tween(180)) { it } + androidx.compose.animation.fadeOut(tween(140))
                        ) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    LibraryBottomBar(favoriteVersion = favVersion, tab = tab,
                        onTabChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            closeSearch(); tab = it
                        },
                        movieCount = stats.movies, showCount = stats.shows, favoriteCount = favoriteTitleCount,
                        searchOpen = searchOpen, onSearch = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            if (searchOpen) closeSearch() else searchOpen = true
                        },
                        modifier = Modifier.onSizeChanged { bottomChromePx = it.height })
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // Edge-to-edge: content scrolls under the status bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { padding ->
            val currentTab = tab
            val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            LaunchedEffect(gridState) {
                androidx.compose.runtime.snapshotFlow { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
                    .collect { atTop -> if (atTop) scrollChromeVisible = true }
            }
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
            val recentlyAdded = remember(filtered, currentTab, searching, searchScope, progressTick) { filtered
                .filter(tabFilter)
                .filterNot { isWatched(it) }
                .sortedByDescending { it.video.dateAddedSec }
                .take(10) }
            val featured = remember(filtered, currentTab, searching, searchScope, progressTick, query) { if (query.isBlank()) {
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
        LaunchedEffect(appSettings.libraryGrid) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.opticast.player.data.AppContainer.breadcrumb.logGridChange(appSettings.libraryGrid)
            }
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LazyVerticalGrid(
            state = gridState,
            columns = currentGridCells,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(chromeScroll)
                .background(MaterialTheme.colorScheme.background)
                .padding(top = padding.calculateTopPadding()).consumeWindowInsets(padding),
            // Fix: posters cutting into bottom tab bar — add bottomChromePx + 80dp
            contentPadding = PaddingValues(bottom = with(layoutDensity) { bottomChromePx.toDp() } + padding.calculateBottomPadding() + 80.dp)
                        ) {
            if (!searching) item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryHeader(onOpenSettings = onOpenSettings, onCustomize = { showCustomize = true }, scanning = state.isMatching || state.checkingFiles, onScan = { viewModel.scan(manual = true) })
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
                            .padding(60.dp),
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
                        flingBehavior = rememberSnapFlingBehavior(carouselState),
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
                        flingBehavior = rememberSnapFlingBehavior(carouselState),
                        contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(DiscoveryGapDp.dp)
                        ) {
                        lazyItems(recentlyAdded, key = { "recent-${it.video.id}" }) { entry ->
                            SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.width(DiscoveryPosterDp.dp), onToggle = { toggleSelect(entry.video.id) }) {
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
                            SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.width(DiscoveryPosterDp.dp), onToggle = { toggleSelect(entry.video.id) }) {
                                PosterCard(entry = entry,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    onClick = { if (selectionMode) toggleSelect(entry.video.id) else onOpenDetail(entry.video.id) },
                                    onLongClick = { if (selectionMode) toggleSelect(entry.video.id) else { menuIsWholeShow = false; menuEntry = entry } },
                                    modifier = Modifier.fillMaxWidth())
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
            FastScrollThumb(
                gridState = gridState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }

    if(showCustomize) LibraryCustomizeDialog(tab,design,onChange={designStore.save(tab,it);designRevision++},
        onCollections={showCustomize=false;showCollections=true},onDismiss={showCustomize=false})
    if(showCollections) CollectionsDialog(personalCollections,state.entries,
        onSave={designStore.saveCollections(it);designRevision++},
        onOpen={showCollections=false;openCollectionId=it.id},onSmart={showCollections=false;showSmartCollections=true},onDismiss={showCollections=false})
    if(showSmartCollections) SmartCollectionsDialog(smartRules,onSave={extrasStore.saveRules(it);extrasRevision++},onDismiss={showSmartCollections=false})
    allCollections.firstOrNull { it.id==openCollectionId }?.let { collection ->
        CollectionContentsDialog(collection,state.entries,onOpen={id -> openCollectionId=null; onOpenDetail(id)},onDismiss={openCollectionId=null})
    }

    RenameSuggestionsPanel(visible = showRenameSuggestions, entries = state.entries,
        onClose = { showRenameSuggestions = false },
        onIdentify = { id -> showRenameSuggestions = false; onOpenMatch(id) },
        onChanged = { viewModel.recheckFiles() })

    if (showMissingFiles) {
        MissingFilesDialog(files = missingFiles, checking = state.checkingFiles, error = state.fileScanError,
            onRecheck = { viewModel.recheckFiles() }, onDismissEntry = { viewModel.dismissMissing(it) },
            onClose = { showMissingFiles = false })
    }

    if (showGenrePicker) {
        ModalBottomSheet(onDismissRequest = { showGenrePicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f), tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = DiscoveryGutterDp.dp).navigationBarsPadding()) {
                Text("Genre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                (listOf("") + genres).forEach { genre ->
                    Row(Modifier.fillMaxWidth().selectable(selected = selectedGenre == genre,
                        onClick = { selectedGenre = genre; showGenrePicker = false }).padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(genre.ifBlank { "All genres" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (selectedGenre == genre) Icon(Icons.Filled.Check, "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }


    // Floating multi-select action bar.
    AnimatedVisibility(
        visible = selectionMode,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = with(layoutDensity) { bottomChromePx.toDp() } + 12.dp),
        enter = fadeIn(tween(200)) + slideIn(tween(260)) { IntOffset(0, 90) },
        exit = fadeOut(tween(160))
                        ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                        ) {
            Row(
                Modifier.padding(start = 18.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
                        ) {
                Text(
                    "${selectedIds.size} selected",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp)
                        )
                IconButton(
                    onClick = {
                        val chosen = state.entries.filter { it.video.id in selectedIds }
                        chosen.forEach {
                            viewModel.setWatched(it.video.id, it.video.durationMs, true)
                        }
                        exitSelection()
                    }
                        ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Mark selected as watched",
                        tint = MaterialTheme.colorScheme.primary
                        )
                }
                IconButton(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) AppContainer.favorites.remove(selectedIds.toList())
                            else AppContainer.favorites.add(selectedIds.toList())
                            exitSelection()
                        }
                    }
                        ) {
                    Icon(
                        if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) Icons.Outlined.FavoriteBorder else Icons.Filled.Favorite,
                        contentDescription = if (tab == "favs" || selectedIds.all { AppContainer.favorites.isFavorite(it) }) "Remove selected from favorites" else "Add selected to favorites",
                        tint = MaterialTheme.colorScheme.primary
                        )
                }
                IconButton(
                    onClick = {
                        if (selectedIds.isNotEmpty()) {
                            shareVideos(selectedIds.toList())
                        }
                    }
                        ) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share selected",
                        tint = MaterialTheme.colorScheme.primary
                        )
                }
                IconButton(
                    onClick = {
                        if (selectedIds.isNotEmpty()) confirmDeleteIds = selectedIds.toList()
                    }
                        ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.error
                        )
                }
                IconButton(onClick = { exitSelection() }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Exit selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
            }
        }
    }
    }

    menuEntry?.let { entry ->
        val targets = if (menuIsWholeShow) showCollection(state.entries, entry) else listOf(entry)
        val targetIds = targets.map { it.video.id }.distinct()
        EntryMenuSheet(
            groupEntries = if (menuIsWholeShow) targets else null,
            entry = entry,
            onDismiss = { menuEntry = null },
            onPlay = {
                menuEntry = null
                playEntry(targets.firstOrNull { AppContainer.playbackState.state(it.video.id)?.isWatched != true } ?: entry)
            },
            onDetails = {
                menuEntry = null
                if (menuIsWholeShow) onOpenShow(showTitleOf(entry)) else onOpenDetail(entry.video.id)
            },
            onMatch = {
                menuEntry = null
                onOpenMatch(entry.video.id)
            },
            onRefreshArtwork = {
                viewModel.refreshArtwork(entry)
                menuEntry = null
            },
            onToggleWatched = { watched ->
                targets.forEach { viewModel.setWatched(it.video.id, it.video.durationMs, watched) }
                menuEntry = null
            },
            onToggleFavorite = {
                if (targets.all { AppContainer.favorites.isFavorite(it.video.id) }) AppContainer.favorites.remove(targetIds)
                else AppContainer.favorites.add(targetIds)
                menuEntry = null
            },
            onForget = {
                viewModel.clearMetadata(entry.video.id)
                menuEntry = null
            },
            onShare = {
                menuEntry = null
                shareVideos(targetIds)
            },
            onDelete = {
                menuEntry = null
                confirmDeleteIds = targetIds
            },
            onSelect = {
                menuEntry = null
                selectionMode = true
                targetIds.forEach { if (it !in selectedIds) selectedIds.add(it) }
            }
                        )
    }

    confirmDeleteIds?.let { ids ->
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            onDismissRequest = { confirmDeleteIds = null },
            title = {
                Text(if (ids.size == 1) "Delete this video?" else "Delete ${ids.size} videos?")
            },
            text = {
                Text(
                    if (ids.size == 1) {
                        "The file will be permanently deleted from your device storage."
                    } else {
                        "These ${ids.size} files will be permanently deleted from your device storage."
                    }
                        )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteIds = null
                    performDelete(ids)
                    exitSelection()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteIds = null }) { Text("Cancel") }
            }
                        )
    }
    } // Library-only visual style; never changes the player theme or density.
}

/** Fast-scroll thumb overlay like Infuse - low-RAM safe with derivedStateOf + graphicsLayer */
@Composable
fun FastScrollThumb(
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val showThumb by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { gridState.isScrollInProgress } }
    val firstVisible by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { gridState.firstVisibleItemIndex } }
    val totalItems by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { gridState.layoutInfo.totalItemsCount } }
    androidx.compose.animation.AnimatedVisibility(
        visible = showThumb && totalItems > 20,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(32.dp)
                .padding(vertical = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val progress = if (totalItems > 0) firstVisible.toFloat() / totalItems else 0f
            Box(
                Modifier
                    .fillMaxHeight(0.1f)
                    .width(4.dp)
                    .graphicsLayer { translationY = progress * 200f }
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            )
        }
    }
}

