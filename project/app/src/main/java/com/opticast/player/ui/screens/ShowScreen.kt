package com.opticast.player.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.opticast.player.ui.components.EpisodeProgressStatus
import com.opticast.player.ui.components.latestUnfinishedEpisode
import com.opticast.player.ui.components.DiscoveryHeader
import com.opticast.player.ui.components.toggledDiscoverySections
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.opticast.player.data.AppContainer
import com.opticast.player.data.remote.CastMember
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.data.AppSettings
import com.opticast.player.ui.components.rememberExternalPlayer
import com.opticast.player.ui.components.rememberFrameArtwork
import com.opticast.player.ui.components.rememberShowFrameArtwork
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.remote.FanartArtwork
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.ui.components.CastRow
import com.opticast.player.ui.components.SectionHeader
import com.opticast.player.ui.components.formatDuration
import com.opticast.player.ui.components.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShowViewModel(private val showName: String) : ViewModel() {

    data class UiState(
        val episodes: List<LibraryEntry> = emptyList(),
        val showMeta: Metadata? = null,
        val artwork: FanartArtwork? = null,
        val cast: List<CastMember> = emptyList(),
        val loading: Boolean = true,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init { reload() }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = runCatching { AppContainer.mediaScanner.scan() }.getOrDefault(emptyList())
            val episodes = all
                .filter { (it.isEpisode || AppContainer.metadataStore.get(it.id)?.type == "tv") && showKeyOf(it) == showName }
                .map { LibraryEntry(it, AppContainer.metadataStore.get(it.id)) }
                .sortedWith(
                    compareBy(
                        { it.video.parsed.season ?: it.metadata?.seasonNumber ?: 0 },
                        { it.video.parsed.episode ?: it.metadata?.episodeNumber ?: 0 },
                    )
                )
            _state.update { it.copy(episodes = episodes, loading = false) }

            // Startup already saved the show overview and extras; opening this page
            // must not trigger fresh API requests or misinterpret another provider's ID.
            val metadata = episodes.firstNotNullOfOrNull { it.metadata }
            val cached = episodes.firstNotNullOfOrNull { AppContainer.detailCache.get(it.video.id) }
            _state.update { it.copy(showMeta = metadata, artwork = cached?.toArtwork(),
                cast = cached?.toCast().orEmpty()) }

        }
    }

    private fun showKeyOf(video: LocalVideo): String {
        val meta = AppContainer.metadataStore.get(video.id)
        return meta?.showTitle ?: video.parsed.title.ifBlank { video.name }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowScreen(
    showName: String,
    onBack: () -> Unit,
    onPlayEpisode: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
) {
    val viewModel: ShowViewModel =
        viewModel(key = "show-$showName", factory = viewModelFactory { ShowViewModel(showName) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appSettings by AppContainer.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val launchExternal = rememberExternalPlayer { v, position, duration ->
        AppContainer.playbackState.save(v.id, position, duration)
    }

    var menuEpisode by remember { mutableStateOf<LibraryEntry?>(null) }
    val episodes = state.episodes
    val anyMeta = episodes.firstNotNullOfOrNull { it.metadata }
    val showMeta = state.showMeta
    val backdropUrl = state.artwork?.background
        ?: tmdbBackdropUrl(showMeta?.backdropPath ?: anyMeta?.backdropPath)
    val logoUrl = state.artwork?.logo
    val title = showMeta?.title ?: anyMeta?.showTitle ?: showName
    val year = showMeta?.year ?: anyMeta?.year
    val rating = showMeta?.voteAverage ?: anyMeta?.voteAverage ?: 0.0
    val genres = showMeta?.genres ?: anyMeta?.genres ?: emptyList()
    val overview = showMeta?.overview.orEmpty()

    val progressTick = AppContainer.playbackState.progressTick
    val continueEpisodeId = remember(episodes, progressTick) {
        latestUnfinishedEpisode(episodes.associate { it.video.id to AppContainer.playbackState.state(it.video.id) })
    }
    val seasonPreferences = LocalContext.current.getSharedPreferences("show_seasons", android.content.Context.MODE_PRIVATE)
    var collapsedSeasons by remember(showName) {
        mutableStateOf(seasonPreferences.getStringSet(showName, emptySet())?.toSet() ?: emptySet())
    }
    fun toggleSeason(season: Int) {
        collapsedSeasons = toggledDiscoverySections(collapsedSeasons, season.toString())
        seasonPreferences.edit().putStringSet(showName, collapsedSeasons).apply()
    }
    val firstUnwatched = remember(episodes, progressTick) {
        episodes.firstOrNull {
            AppContainer.playbackState.state(it.video.id)?.isWatched != true
        } ?: episodes.firstOrNull()
    }

    fun playEntry(entry: LibraryEntry) {
        if (appSettings.useExternalPlayer) {
            launchExternal(
                entry.video,
                AppContainer.playbackState.state(entry.video.id)?.positionMs ?: 0L,
                entry.metadata?.displayTitle ?: entry.video.name,
            )
        } else {
            onPlayEpisode(entry.video.id)
        }
    }
    EpisodeActions(entry = menuEpisode, onDismiss = { menuEpisode = null },
        onPlay = { playEntry(it) }, onDetails = { onOpenDetail(it.video.id) }, onChanged = { viewModel.reload() })

    val seasons = remember(episodes) {
        episodes
            .groupBy { it.video.parsed.season ?: it.metadata?.seasonNumber ?: 0 }
            .toSortedMap()
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    var backdropFailed by remember(backdropUrl) { mutableStateOf(false) }
                    if (backdropUrl != null && !backdropFailed) {
                        AsyncImage(
                            onError = { backdropFailed = true },
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(backdropUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // No TMDB backdrop: use a real frame from any episode in
                        // the show — not just the first, so the hero never stays
                        // blank when the first episode fails to decode (fix: shows
                        // were blank while episodes had thumbnails).
                        val heroFrame = if (episodes.isEmpty()) null else rememberShowFrameArtwork(episodes)
                        if (heroFrame != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(heroFrame)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            )
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    )
                                )
                            )
                    )
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(8.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = title,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                modifier = Modifier.height(60.dp),
                            )
                        } else {
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (rating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFC94D),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "%.1f".format(rating),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                            year?.let {
                                Text(
                                    it.toString(),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Text(
                                "${episodes.size} episodes",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            item(key = "actions") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { firstUnwatched?.let { playEntry(it) } },
                        enabled = firstUnwatched != null,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            firstUnwatched?.let { AppContainer.playbackState.progressOf(it.video.id) }
                                ?.takeIf { it.isResumable }?.let { "Resume · ${it.positionMs.formatDuration()}" } ?: "Watch Now",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            if (overview.isNotBlank()) {
                item(key = "overview") {
                    var expanded by remember { mutableStateOf(false) }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        Text(
                            overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        androidx.compose.material3.TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Less" else "More")
                        }
                    }
                }
            }

            if (state.cast.isNotEmpty()) {
                item(key = "cast") {
                    CastRow(members = state.cast)
                }
            }

            seasons.forEach { (seasonNumber, seasonEpisodes) ->
                item(key = "season-$seasonNumber") {
                    val completed = seasonEpisodes.count { AppContainer.playbackState.progressOf(it.video.id)?.isWatched == true }
                    DiscoveryHeader(
                        title = if (seasonNumber > 0) "Season $seasonNumber" else "Episodes",
                        subtitle = "$completed of ${seasonEpisodes.size} completed",
                        accent = MaterialTheme.colorScheme.primary,
                        count = seasonEpisodes.size,
                        expanded = seasonNumber.toString() !in collapsedSeasons,
                        onToggle = { toggleSeason(seasonNumber) },
                    )
                }
                if (seasonNumber.toString() !in collapsedSeasons) items(seasonEpisodes, key = { "ep-${it.video.id}" }) { entry ->
                    EpisodeRow(
                        entry = entry,
                        preferredResume = entry.video.id == continueEpisodeId,
                        onClick = { onOpenDetail(entry.video.id) },
                        onLongClick = { menuEpisode = entry },
                        onPlay = { playEntry(entry) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeRow(
    entry: LibraryEntry,
    preferredResume: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
) {
    val metadata = entry.metadata
    val playback = AppContainer.playbackState.progressOf(entry.video.id)
    val completed = playback?.isWatched == true
    val started = playback?.isResumable == true
    val accent = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val highlighted = started || completed
    val stillUrl = tmdbImageUrl(metadata?.episodeStillPath, "w300")
        ?: tmdbImageUrl(metadata?.backdropPath, "w300")
    val episodeNumber = entry.video.parsed.episode ?: metadata?.episodeNumber
    val episodeName = metadata?.episodeName
        ?: entry.video.parsed.title.substringAfterLast(' ').takeIf { it.isNotBlank() }
        ?: entry.video.name
    val description = metadata?.overview?.takeIf { it.isNotBlank() }
        ?: entry.video.durationMs.formatDuration()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) accent.copy(alpha = if (preferredResume) 0.16f else 0.08f) else MaterialTheme.colorScheme.surfaceContainer)
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) accent else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(if (LocalConfiguration.current.screenWidthDp < 400) 128.dp else 160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(onClick = onPlay, onLongClick = onLongClick),
        ) {
            // The frame is also the safety net: a TMDB still that fails to load
            // must not leave an empty box.
            var stillFailed by remember(stillUrl) { mutableStateOf(false) }
            val stillFrame = if (stillUrl == null || stillFailed) {
                rememberFrameArtwork(entry.video.id)
            } else {
                null
            }
            if (stillUrl != null && !stillFailed) {
                AsyncImage(
                    onError = { stillFailed = true },
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(stillUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (stillFrame != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(stillFrame)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val tag = episodeNumber?.let { "E%02d".format(it) } ?: entry.video.seasonEpisodeTag
            Text(
                text = listOfNotNull(tag, episodeName).joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (highlighted) {
                Spacer(Modifier.height(8.dp))
                EpisodeProgressStatus(playback, preferredResume)
            }
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play episode", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
