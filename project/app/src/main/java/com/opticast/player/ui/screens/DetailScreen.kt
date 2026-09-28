@file:OptIn(ExperimentalMaterial3Api::class)

package com.opticast.player.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.opticast.player.data.local.CachedDetails
import com.opticast.player.data.remote.CastMember
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.data.remote.tmdbPosterUrl
import com.opticast.player.data.AppSettings
import com.opticast.player.data.local.PlaybackState
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.model.SavedSubtitle
import com.opticast.player.data.remote.FanartArtwork
import com.opticast.player.data.remote.OmdbRatings
import com.opticast.player.data.remote.SubtitleResult
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.ui.components.CastRow
import com.opticast.player.ui.components.rememberExternalPlayer
import com.opticast.player.ui.components.FallbackPoster
import com.opticast.player.ui.components.rememberFrameArtwork
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.formatDuration
import com.opticast.player.ui.components.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// -------------------------------------------------------------------------- VM

class DetailViewModel(private val videoId: Long) : ViewModel() {

    data class UiState(
        val video: LocalVideo? = null,
        val metadata: Metadata? = null,
        val subtitles: List<SavedSubtitle> = emptyList(),
        val resume: PlaybackState? = null,
        val watched: Boolean = false,
        val matching: Boolean = false,
        val searching: Boolean = false,
        val downloadingKey: String? = null,
        val results: List<SubtitleResult> = emptyList(),
        val message: String? = null,
        val ratings: OmdbRatings? = null,
        val artwork: FanartArtwork? = null,
        val cast: List<CastMember> = emptyList(),
        val favorite: Boolean = false,
        /** True while a background refresh is running with cached data shown. */
        val refreshing: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            AppContainer.metadataStore.version.collect { load() }
        }
        viewModelScope.launch {
            AppContainer.playbackState.version.collect { load() }
        }
        viewModelScope.launch {
            AppContainer.favorites.version.collect { load() }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val video = AppContainer.mediaScanner.byId(videoId)
            val metadata = AppContainer.metadataStore.get(videoId)
            val subtitles = AppContainer.metadataStore.subtitlesFor(videoId)
            val resume = AppContainer.playbackState.state(videoId)
            val favorite = AppContainer.favorites.isFavorite(videoId)
            _state.update {
                it.copy(
                    video = video,
                    metadata = metadata,
                    subtitles = subtitles,
                    resume = resume,
                    watched = resume?.isWatched == true,
                    favorite = favorite,
                )
            }
            enrich(metadata)
        }
    }

    /**
     * Ratings (OMDb), extra artwork (Fanart.tv), the IMDb id and the cast list.
     *
     * Offline-first: the persisted [CachedDetails] bundle is used directly and
     * NO network request is made while it is fresh. A refresh only happens when
     * nothing is cached yet or the bundle has aged out, and a failed refresh
     * always keeps the cached copy.
     */
    private fun enrich(metadata: Metadata?) {
        if (metadata == null || metadata.source != "tmdb") {
            _state.update { it.copy(ratings = null, artwork = null, cast = emptyList()) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val cached = AppContainer.detailCache.get(videoId)
            // 1. Whatever is on disk is shown immediately.
            if (cached != null) {
                _state.update {
                    it.copy(
                        ratings = cached.toRatings(),
                        artwork = cached.toArtwork(),
                        cast = cached.toCast(),
                    )
                }
                prefetchDetailImages(metadata, cached, force = false)
            }
            if (cached != null && cached.isFresh) return@launch

            _state.update { it.copy(refreshing = true) }
            AppContainer.offlineLibrary.prepare(videoId, metadata)
            val merged = AppContainer.detailCache.get(videoId)
            if (merged == null) {
                _state.update { it.copy(refreshing = false) }
                return@launch
            }

            _state.update {
                it.copy(
                    ratings = merged.toRatings() ?: it.ratings,
                    artwork = merged.toArtwork() ?: it.artwork,
                    cast = merged.toCast().ifEmpty { it.cast },
                    refreshing = false,
                )
            }
            prefetchDetailImages(metadata, merged, force = true)
        }
    }

    /** Puts the backdrop, poster and cast headshots on disk for offline use. */
    private fun prefetchDetailImages(
        metadata: Metadata,
        details: CachedDetails,
        force: Boolean,
    ) {
        val urls = buildList {
            add(tmdbBackdropUrl(metadata.backdropPath))
            add(tmdbBackdropUrl(metadata.episodeStillPath))
            add(tmdbPosterUrl(metadata.posterPath))
            details.imageUrls().forEach { path ->
                // Cast paths are TMDB relative paths; Fanart URLs are absolute
                // HD images, which the data saver avoids.
                if (path.startsWith("http")) {
                    if (!AppContainer.dataSaver) add(path)
                } else {
                    add(tmdbImageUrl(path, "w185"))
                }
            }
        }
        if (force) {
            AppContainer.prefetchArtwork(urls)
        } else {
            AppContainer.prefetchArtworkOnce(videoId, urls)
        }
    }


    fun ensureMatched() {
        if (_state.value.metadata != null || _state.value.matching) return
        val video = _state.value.video ?: return
        viewModelScope.launch {
            _state.update { it.copy(matching = true) }
            runCatching {
                val matched = AppContainer.offlineLibrary.match(video)
                matched?.let {
                    AppContainer.metadataStore.save(videoId, it)
                    AppContainer.offlineLibrary.prepare(videoId, it)
                }
            }
            _state.update { it.copy(matching = false) }
        }
    }

    fun searchSubtitles() {
        val video = _state.value.video ?: return
        if (_state.value.searching) return
        _state.update { it.copy(searching = true, message = null) }
        viewModelScope.launch {
            val current = _state.value
            runCatching {
                val settings = AppContainer.settings.current()
                AppContainer.subtitles.search(
                    metadata = current.metadata,
                    fallbackQuery = current.metadata?.displayTitle
                        ?: video.parsed.title.ifBlank { video.name },
                    languages = settings.subtitleLanguages,
                    season = video.parsed.season,
                    episode = video.parsed.episode,
                )
            }.onSuccess { results ->
                _state.update {
                    it.copy(
                        searching = false,
                        results = results,
                        message = if (results.isEmpty()) "No subtitles found." else null,
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(searching = false, message = error.message ?: "Subtitle search failed.")
                }
            }
        }
    }

    fun downloadSubtitle(result: SubtitleResult) {
        if (_state.value.downloadingKey != null) return
        _state.update { it.copy(downloadingKey = result.key) }
        viewModelScope.launch {
            runCatching { AppContainer.subtitles.download(result, videoId) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            downloadingKey = null,
                            message = "Subtitle saved — it loads automatically in the player.",
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            downloadingKey = null,
                            message = error.message ?: "Download failed.",
                        )
                    }
                }
        }
    }

    fun deleteSubtitle(subtitle: SavedSubtitle) {
        viewModelScope.launch(Dispatchers.IO) {
            AppContainer.metadataStore.deleteSubtitle(subtitle)
        }
    }

    fun toggleWatched() {
        val video = _state.value.video ?: return
        val currentlyWatched = _state.value.watched
        AppContainer.playbackState.setWatched(videoId, !currentlyWatched, video.durationMs)
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }
}

// ---------------------------------------------------------------------- screen

@Composable
fun DetailScreen(
    videoId: Long,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onOpenMatch: () -> Unit,
) {
    val viewModel: DetailViewModel =
        viewModel(key = "detail$videoId", factory = viewModelFactory { DetailViewModel(videoId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSubtitlesSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.ensureMatched() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    val video = state.video
    if (video == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val metadata = state.metadata
    val backdropUrl = state.artwork?.background
        ?: tmdbBackdropUrl(metadata?.backdropPath ?: metadata?.episodeStillPath)
    val logoUrl = state.artwork?.logo
    val posterVersion by AppContainer.posterCache.version.collectAsStateWithLifecycle()
    val posterUrl = AppContainer.posterCache.localUrl(videoId, metadata)
        ?: tmdbPosterUrl(metadata?.posterPath)

    // Immersive hero: true 16:9 on wide phones, never below 280dp so the
    // title block is never clipped.
    val configuration = LocalConfiguration.current
    val heroHeight = ((configuration.screenWidthDp * 9f) / 16f).coerceIn(280f, 460f).dp
    val appSettings by AppContainer.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())
    val launchExternal = rememberExternalPlayer { v, position, duration ->
        AppContainer.playbackState.save(v.id, position, duration)
    }
    val shareContext = LocalContext.current
    fun toggleFavorite() {
        AppContainer.favorites.toggle(videoId)
    }

    /** Shares the actual video file via Quick Share / the system share sheet. */
    fun shareMovie() {
        val uri = Uri.parse(video.uri)
        // A concrete MIME type makes the share sheet treat this as a video
        // (thumbnail preview) instead of a generic file/text item.
        val mime = shareContext.contentResolver.getType(uri)
            ?: when (video.name.substringAfterLast('.', "").lowercase()) {
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
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            // Lets the receiving app (Quick Share, WhatsApp...) read this file.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            shareContext.startActivity(Intent.createChooser(send, "Share video"))
        }
    }

    fun play() {
        if (appSettings.useExternalPlayer) {
            launchExternal(
                video,
                AppContainer.playbackState.state(videoId)?.takeIf { it.isResumable }?.positionMs ?: 0L,
                metadata?.displayTitle ?: video.parsed.title.ifBlank { video.name },
            )
        } else {
            onPlay()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth().height(heroHeight)) {
                    if (backdropUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(backdropUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // No backdrop: try frame, then blurred poster fallback (fix black background)
                        // User reported black background for Afterburn (2025) - offline-first needs fallback
                        val frame = rememberFrameArtwork(video.id)
                        if (frame != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(frame)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (posterUrl != null) {
                            // Blurred poster as background when no backdrop and no frame - never black
                            // Offline-first: poster is cached, backdrop may not be (new movie, slow internet)
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(posterUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(24.dp), // Blur poster for background effect
                            )
                            // Dark overlay to ensure text readability
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                            )
                        } else {
                            FallbackPoster(metadata?.displayTitle ?: video.parsed.title)
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.25f),
                                    0.35f to Color.Transparent,
                                    0.72f to MaterialTheme.colorScheme.background
                                        .copy(alpha = 0.72f),
                                    1f to MaterialTheme.colorScheme.background,
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { toggleFavorite() }) {
                            Icon(
                                if (state.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (state.favorite) Color(0xFFFF5470) else Color.White,
                            )
                        }
                        IconButton(onClick = { shareMovie() }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                            )
                        }
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
                    ) {
                        if (logoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = metadata?.displayTitle ?: "Title logo",
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart,
                                modifier = Modifier.height(60.dp),
                            )
                        } else {
                            Text(
                                text = metadata?.displayTitle
                                    ?: video.parsed.title.ifBlank { video.name },
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
                            if (metadata != null && metadata.voteAverage > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFC94D),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "%.1f".format(metadata.voteAverage),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                            metadata?.year?.let {
                                Text(
                                    it.toString(),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            metadata?.runtimeMinutes?.let {
                                Text(
                                    "$it min",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            video.resolutionLabel?.let {
                                Text(
                                    it,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            state.ratings?.let { ratings ->
                item(key = "ratings") {
                    RatingsRow(ratings)
                }
            }

            item(key = "actions") {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 4.dp)
                ) {
                    PosterImage(
                        url = posterUrl,
                        fallbackTitle = video.parsed.title,
                        cacheBust = posterVersion,
                        // Same artwork the library grid shows: for an unscraped
                        // title this becomes the frame extracted from the file.
                        videoId = videoId,
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(16.dp))
                            .shadow(16.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f).align(Alignment.Bottom)) {
                        val resume = state.resume
                        val playLabel = when {
                            appSettings.useExternalPlayer && resume?.isResumable == true ->
                                "Resume · ${resume.positionMs.formatDuration()}"
                            appSettings.useExternalPlayer -> "Watch Now"
                            resume?.isResumable == true ->
                                "Resume · ${resume.positionMs.formatDuration()}"
                            else -> "Watch Now"
                        }
                        Button(
                            onClick = { play() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(playLabel, style = MaterialTheme.typography.labelLarge)
                        }
                        if (resume?.isResumable == true) {
                            TextButton(onClick = {
                                AppContainer.playbackState.restart(videoId, video.durationMs)
                                play()
                            }, modifier = Modifier.fillMaxWidth()) { Text("Restart from beginning") }
                        }
                        Spacer(Modifier.height(10.dp))
                        // Fixed horizontal pair even at maximum display/font zoom.
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { showSubtitlesSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(48.dp)) {
                                com.opticast.player.ui.components.AutoFitLabel("Subtitles", modifier = Modifier.weight(1f))
                            }
                            FilledTonalButton(onClick = onOpenMatch,
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(48.dp)) {
                                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                com.opticast.player.ui.components.AutoFitLabel("Match", modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { viewModel.toggleWatched() },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (state.watched) "Watched — tap to mark unwatched"
                                else "Mark as watched",
                            )
                        }
                    }
                }
            }

            if (metadata?.isEpisodeMetadata == true) {
                item(key = "episode") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            val tag = "S%02d E%02d".format(
                                metadata.seasonNumber ?: 0,
                                metadata.episodeNumber ?: 0,
                            )
                            Text(
                                text = listOf(tag, metadata.episodeName.orEmpty())
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (!metadata.overview.isBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    metadata.overview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            val overview = if (metadata?.isEpisodeMetadata != true) metadata?.overview.orEmpty() else ""
            if (overview.isNotBlank()) {
                item(key = "overview") {
                    var expanded by remember { mutableStateOf(false) }
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text("Story", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            overview,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Less" else "More")
                        }
                    }
                }
            }

            if (metadata?.attribution != null) {
                item(key = "provider_credit") {
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(metadata.attribution, style = MaterialTheme.typography.bodySmall)
                        metadata.sourceUrl?.let { url ->
                            TextButton(onClick = { uriHandler.openUri(url) }) { Text("Source and contributors") }
                        }
                        TextButton(onClick = { uriHandler.openUri(if (metadata.source == "wikidata")
                            "https://creativecommons.org/publicdomain/zero/1.0/" else "https://creativecommons.org/licenses/by-sa/4.0/") }) {
                            Text(if (metadata.source == "wikidata") "CC0 dedication" else "CC BY-SA 4.0 license")
                        }
                    }
                }
            }

            val genres = metadata?.genres.orEmpty()
            if (genres.isNotEmpty()) {
                item(key = "genres") {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        genres.forEach { genre ->
                            AssistChip(onClick = {}, label = { Text(genre) })
                        }
                    }
                }
            }

            val tmdbId = metadata?.tmdbId
            if (tmdbId != null) {
                val isTv = metadata.type == "tv"
                item(key = "cast") {
                    CastRow(members = state.cast)
                }
            }

            item(key = "file") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("File", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        FileInfoRow("Name", video.name)
                        FileInfoRow("Length", video.durationMs.formatDuration())
                        FileInfoRow(
                            "Size",
                            "%.1f MB".format(video.sizeBytes / 1_048_576f),
                        )
                        if (video.width > 0 && video.height > 0) {
                            FileInfoRow("Resolution", "${video.width}×${video.height}")
                        }
                        FileInfoRow(
                            "Saved subtitles",
                            state.subtitles.size.toString(),
                        )
                    }
                }
            }
        }
    }

    if (showSubtitlesSheet) {
        SubtitlesSheet(
            state = state,
            onDismiss = { showSubtitlesSheet = false },
            onSearch = viewModel::searchSubtitles,
            onDownload = viewModel::downloadSubtitle,
            onDelete = viewModel::deleteSubtitle,
        )
    }
}

/** Cross-provider rating badges (OMDb): IMDb · Rotten Tomatoes · Metacritic. */
@Composable
private fun RatingsRow(ratings: OmdbRatings) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ratings.imdb?.let {
                RatingChip(text = "IMDb $it", highlight = true)
            }
            ratings.rottenTomatoes?.let {
                RatingChip(text = "🍅 $it%")
            }
            ratings.metacritic?.let {
                RatingChip(text = "MC $it")
            }
            ratings.rated?.let {
                RatingChip(text = it)
            }
        }
        val extra = listOfNotNull(
            ratings.imdbVotes?.let { "$it IMDb votes" },
            ratings.boxOffice,
        ).joinToString(" · ")
        if (extra.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                extra,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ratings.awards?.let {
            Text(
                "🏆 $it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun RatingChip(text: String, highlight: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    com.opticast.player.ui.components.AlignedLabelValue(label, value)
}

@Composable
private fun SubtitlesSheet(
    state: DetailViewModel.UiState,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onDownload: (SubtitleResult) -> Unit,
    onDelete: (SavedSubtitle) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f), tonalElevation = 0.dp) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Subtitles", style = MaterialTheme.typography.headlineSmall)

            if (state.subtitles.isEmpty()) {
                Text(
                    "No saved subtitles yet. Search below — OpenSubtitles and SubDL are queried together, and downloaded tracks load automatically in the player.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.subtitles.forEach { subtitle ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                subtitle.releaseName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${subtitle.language.uppercase()} · ${subtitle.source}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onDelete(subtitle) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete subtitle",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Subtitle sources", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Searches in your preferred subtitle languages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onSearch,
                    enabled = !state.searching,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (state.searching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Search")
                }
            }

            state.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (state.searching) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.results.forEach { result ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                result.releaseName,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    result.language.uppercase(),
                                    if (result.downloads > 0) "${result.downloads} downloads" else null,
                                    if (result.source == "subdl") "SubDL" else null,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalIconButton(
                            onClick = { onDownload(result) },
                            enabled = state.downloadingKey != result.key,
                        ) {
                            if (state.downloadingKey == result.key) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Download, contentDescription = "Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
