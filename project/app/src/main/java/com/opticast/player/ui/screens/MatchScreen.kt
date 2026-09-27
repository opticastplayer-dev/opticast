package com.opticast.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LocalVideo
import com.opticast.player.data.model.Metadata
import com.opticast.player.data.remote.AniResult
import com.opticast.player.data.remote.MatchCandidate
import com.opticast.player.ui.components.FallbackPoster
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MatchViewModel(private val videoId: Long) : ViewModel() {

    data class UiState(
        val video: LocalVideo? = null,
        val query: String = "",
        val searching: Boolean = false,
        val searched: Boolean = false,
        val saving: Boolean = false,
        val useAniList: Boolean = false,
        val candidates: List<MatchCandidate> = emptyList(),
        val aniCandidates: List<AniResult> = emptyList(),
        val freeCandidates: List<Metadata> = emptyList(),
        val message: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val video = AppContainer.mediaScanner.byId(videoId)
            val initialQuery = video?.parsed?.title ?: ""
            _state.update { it.copy(video = video, query = initialQuery) }
            if (initialQuery.isNotBlank()) search(initialQuery)
        }
    }

    fun setSource(useAniList: Boolean) {
        if (_state.value.useAniList == useAniList) return
        _state.update {
            it.copy(
                useAniList = useAniList,
                searched = false,
                candidates = emptyList(),
                aniCandidates = emptyList(),
                freeCandidates = emptyList(),
                message = null,
            )
        }
        val query = _state.value.query
        if (query.isNotBlank()) search(query)
    }

    fun search(query: String) {
        if (query.isBlank()) return
        val aniList = _state.value.useAniList
        _state.update { it.copy(query = query, searching = true, message = null) }
        viewModelScope.launch {
            if (aniList) {
                val results = runCatching { AppContainer.anilist.search(query) }
                    .getOrElse { emptyList() }
                _state.update {
                    it.copy(
                        aniCandidates = results,
                        searching = false,
                        searched = true,
                        message = if (results.isEmpty()) "No anime found for “$query”." else null,
                    )
                }
            } else {
                val ready = AppContainer.settings.current().tmdbApiKey.isNotBlank()
                val results = if (ready) runCatching { AppContainer.tmdb.searchAll(query) }
                    .getOrDefault(emptyList()) else emptyList()
                val video = _state.value.video
                val free = if (results.isEmpty() && video != null) runCatching {
                    AppContainer.keyless.autoMatch(video.copy(parsed = video.parsed.copy(title = query)))
                }.getOrNull() else null
                _state.update {
                    it.copy(candidates = results, freeCandidates = listOfNotNull(free),
                        searching = false, searched = true,
                        message = if (results.isEmpty() && free == null)
                            "No confident match. Check the title/year and internet connection. Wikipedia and TVmaze need no key." else null)
                }
            }
        }
    }

    fun select(candidate: MatchCandidate, onDone: () -> Unit) {
        val video = _state.value.video ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val metadata = runCatching {
                when {
                    !candidate.isTv -> AppContainer.tmdb.movieMetadata(candidate.result.id)
                    video.isEpisode -> AppContainer.tmdb.episodeMetadata(
                        candidate.result.id,
                        video.parsed.season!!,
                        video.parsed.episode!!,
                    )
                    else -> AppContainer.tmdb.showMetadata(candidate.result.id)
                }
            }.getOrNull()
            if (metadata != null) {
                AppContainer.detailCache.clear(videoId)
                AppContainer.metadataStore.save(videoId, metadata.copy(manuallyMatched = true))
                AppContainer.offlineLibrary.prepare(videoId, metadata)
                onDone()
            } else {
                _state.update {
                    it.copy(saving = false, message = "Could not load details for that title.")
                }
            }
        }
    }

    fun selectAni(result: AniResult, onDone: () -> Unit) {
        val video = _state.value.video ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val metadata = runCatching {
                AppContainer.anilist.mediaMetadata(
                    id = result.id,
                    season = video.parsed.season ?: 1,
                    episode = video.parsed.episode,
                )
            }.getOrNull()
            if (metadata != null) {
                AppContainer.detailCache.clear(videoId)
                AppContainer.metadataStore.save(videoId, metadata.copy(manuallyMatched = true))
                AppContainer.offlineLibrary.prepare(videoId, metadata)
                onDone()
            } else {
                _state.update {
                    it.copy(saving = false, message = "Could not load details for that title.")
                }
            }
        }
    }

    fun selectFree(metadata: Metadata, onDone: () -> Unit) {
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            AppContainer.detailCache.clear(videoId)
            AppContainer.metadataStore.save(videoId, metadata.copy(manuallyMatched = true))
            AppContainer.offlineLibrary.prepare(videoId, metadata)
            onDone()
        }
    }

    fun clearMatch(onDone: () -> Unit) {
        AppContainer.detailCache.clear(videoId)
        AppContainer.metadataStore.clear(videoId)
        AppContainer.posterCache.deleteForVideo(videoId)
        onDone()
    }
}

@Composable
fun MatchScreen(videoId: Long, onDone: () -> Unit) {
    val viewModel: MatchViewModel =
        viewModel(key = "match$videoId", factory = viewModelFactory { MatchViewModel(videoId) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    var queryDraft by remember { mutableStateOf<String?>(null) }
    val query = queryDraft ?: state.query

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text("Match title", style = MaterialTheme.typography.headlineSmall)
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { queryDraft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(22.dp),
                    placeholder = { Text("Movie or show name") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { queryDraft = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !state.useAniList,
                        onClick = { viewModel.setSource(useAniList = false) },
                        label = { Text("Movies & TV") },
                    )
                    FilterChip(
                        selected = state.useAniList,
                        onClick = { viewModel.setSource(useAniList = true) },
                        label = { Text("Anime (AniList)") },
                    )
                }
            }

            if (state.saving) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.searching) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                state.message?.let { message ->
                    item {
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
                if (state.useAniList) {
                    items(state.aniCandidates, key = { "ani-${it.id}" }) { result ->
                        AniCandidateRow(result = result, onClick = {
                            viewModel.selectAni(result, onDone)
                        })
                    }
                } else {
                    items(state.freeCandidates, key = { "${it.source}-${it.tmdbId}" }) { candidate ->
                        Column(Modifier.fillMaxWidth().clickable { viewModel.selectFree(candidate, onDone) }
                            .padding(horizontal = 24.dp, vertical = 16.dp)) {
                            Text(candidate.title, style = MaterialTheme.typography.titleMedium)
                            Text("${candidate.year ?: "?"} · ${candidate.source} · No API key",
                                style = MaterialTheme.typography.labelSmall)
                            Text(candidate.overview, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    items(state.candidates, key = { "${it.isTv}-${it.result.id}" }) { candidate ->
                        CandidateRow(candidate = candidate, onClick = {
                            viewModel.select(candidate, onDone)
                        })
                    }
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Wrong match? Pick the correct title above, or reset this file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = { viewModel.clearMatch(onDone) }) {
                        Text("Clear metadata for this file")
                    }
                }
            }
        }
    }
}

@Composable
private fun AniCandidateRow(result: AniResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            url = result.coverUrl,
            fallbackTitle = result.title,
            modifier = Modifier
                .width(60.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    result.year?.toString() ?: "?",
                    "Anime",
                    result.episodes?.let { "$it eps" },
                    result.averageScore?.takeIf { it > 0 }?.let { "★ %.1f".format(it / 10.0) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (result.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    result.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: MatchCandidate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            url = candidate.posterUrl,
            fallbackTitle = candidate.title,
            modifier = Modifier
                .width(60.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                candidate.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    candidate.year?.toString() ?: "?",
                    if (candidate.isTv) "TV" else "Movie",
                    if (candidate.result.voteAverage > 0)
                        "★ %.1f".format(candidate.result.voteAverage) else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (candidate.result.overview.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    candidate.result.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
