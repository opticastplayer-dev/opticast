package com.opticast.player.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opticast.player.R
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.formatDuration
import com.opticast.player.ui.components.posterRemoteUrlFor
import com.opticast.player.ui.components.posterUrlFor
import com.opticast.player.ui.components.viewModelFactory
import com.opticast.player.ui.screens.LibraryViewModel

/**
 * Android TV / Fire TV home.
 *
 * Same library, same metadata, same player - laid out for a remote instead of a
 * thumb: big rails, one obvious focus ring, and an information strip that names
 * whatever the remote is currently sitting on. The phone layout is untouched:
 * this screen is only ever shown when [isTelevision] is true.
 */
@Composable
fun TvHomeScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenPlayer: (Long) -> Unit,
    onOpenShow: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNetwork: () -> Unit,
) {
    val viewModel: LibraryViewModel = viewModel(factory = viewModelFactory { LibraryViewModel() })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!state.scannedOnce) viewModel.scan()
    }

    val entries = state.entries
    // Every rail is derived once per library change, not per recomposition.
    val continueWatching = remember(entries) {
        entries
            .mapNotNull { entry ->
                AppContainer.playbackState.progressOf(entry.video.id)
                    ?.takeIf { it.isResumable }
                    ?.let { entry to it }
            }
            .sortedByDescending { it.second.updatedAt }
            .take(20)
    }
    val movies = remember(entries) { entries.filter { !it.video.isEpisode } }
    val shows = remember(entries) {
        entries.filter { it.video.isEpisode }
            .groupBy { it.metadata?.showTitle ?: it.video.parsed.title.ifBlank { it.video.name } }
            .toList()
            .sortedBy { it.first.lowercase() }
    }
    val recentlyAdded = remember(entries) {
        entries.sortedByDescending { it.video.dateAddedSec }.take(20)
    }

    // Whatever the remote is sitting on, described in the strip at the bottom.
    var focusedEntry by remember { mutableStateOf<LibraryEntry?>(entries.firstOrNull()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    com.opticast.player.R.drawable.ic_opticast_mark,
                ),
                contentDescription = null,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "OptiCast",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            TvChip("Network", Icons.Filled.Wifi, onOpenNetwork)
            Spacer(Modifier.width(12.dp))
            TvChip("Settings", Icons.Filled.Settings, onOpenSettings)
        }

        Box(Modifier.weight(1f)) {
            when {
                !state.scannedOnce && entries.isEmpty() -> TvMessage(
                    "Looking through your videos…",
                    busy = true,
                )
                entries.isEmpty() -> TvMessage(
                    "No videos found yet. Plug in a drive, or add a network folder.",
                    busy = false,
                    action = "Scan again" to { viewModel.scan() },
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    if (continueWatching.isNotEmpty()) {
                        item(key = "cw") {
                            TvRail("Continue watching") {
                                items(continueWatching, key = { it.first.video.id }) { pair ->
                                    ContinueCard(
                                        entry = pair.first,
                                        fraction = pair.second.progress,
                                        remainingMs = pair.second.remainingMs,
                                        onFocus = { focusedEntry = pair.first },
                                        onClick = { onOpenPlayer(pair.first.video.id) },
                                    )
                                }
                            }
                        }
                    }
                    if (movies.isNotEmpty()) {
                        item(key = "movies") {
                            TvRail("Movies") {
                                items(movies, key = { it.video.id }) { entry ->
                                    PosterCard(
                                        entry = entry,
                                        onFocus = { focusedEntry = entry },
                                        onClick = { onOpenDetail(entry.video.id) },
                                    )
                                }
                            }
                        }
                    }
                    if (shows.isNotEmpty()) {
                        item(key = "shows") {
                            TvRail("TV shows") {
                                items(shows, key = { it.first }) { group ->
                                    val lead = group.second.first()
                                    PosterCard(
                                        entry = lead,
                                        badge = "${group.second.size} episode" +
                                            if (group.second.size == 1) "" else "s",
                                        onFocus = { focusedEntry = lead },
                                        onClick = { onOpenShow(group.first) },
                                    )
                                }
                            }
                        }
                    }
                    if (recentlyAdded.isNotEmpty()) {
                        item(key = "recent") {
                            TvRail("Recently added") {
                                items(recentlyAdded, key = { it.video.id }) { entry ->
                                    PosterCard(
                                        entry = entry,
                                        onFocus = { focusedEntry = entry },
                                        onClick = { onOpenDetail(entry.video.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.isMatching) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 34.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Matching artwork and details - ${state.matchingDone}/${state.matchingTotal}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        focusedEntry?.let { entry -> InfoStrip(entry, onOpenDetail, onOpenPlayer) }
    }
}

/** One focusable chip. */
@Composable
private fun TvChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    TvFocusableCard(
        modifier = Modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(23.dp),
        focusedScale = 1.06f,
    ) {
        Row(
            Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** A titled row of cards. */
@Composable
private fun TvRail(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 34.dp, bottom = 10.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun PosterCard(
    entry: LibraryEntry,
    badge: String? = null,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    Column(Modifier.width(150.dp)) {
        TvFocusableCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clickable(onClick = onClick),
            onFocusChanged = { if (it) onFocus() },
        ) {
            PosterImage(
                url = posterUrlFor(entry),
                fallbackTitle = entry.metadata?.displayTitle ?: entry.video.parsed.title,
                remoteUrl = posterRemoteUrlFor(entry),
                videoId = entry.video.id,
                modifier = Modifier.fillMaxSize(),
            )
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            entry.metadata?.displayTitle ?: entry.video.parsed.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 16:9 resume card: play straight from the rail, progress along the bottom. */
@Composable
private fun ContinueCard(
    entry: LibraryEntry,
    fraction: Float,
    remainingMs: Long,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    Column(Modifier.width(260.dp)) {
        TvFocusableCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(onClick = onClick),
            onFocusChanged = { if (it) onFocus() },
        ) {
            PosterImage(
                url = posterUrlFor(entry),
                fallbackTitle = entry.metadata?.displayTitle ?: entry.video.parsed.title,
                remoteUrl = posterRemoteUrlFor(entry),
                videoId = entry.video.id,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                        ),
                    ),
            )
            Row(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${remainingMs.formatDuration()} left",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(5.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            entry.metadata?.displayTitle ?: entry.video.parsed.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Nothing to show yet, or nothing at all. */
@Composable
private fun TvMessage(
    text: String,
    busy: Boolean,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (busy) {
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(Modifier.height(18.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            TvChip(action.first, Icons.Filled.Refresh, action.second)
        }
    }
}

/** The strip that describes whatever the remote is pointing at. */
@Composable
private fun InfoStrip(
    entry: LibraryEntry,
    onOpenDetail: (Long) -> Unit,
    onOpenPlayer: (Long) -> Unit,
) {
    val metadata = entry.metadata
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 34.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                metadata?.displayTitle ?: entry.video.parsed.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val facts = buildList {
                metadata?.year?.let { add(it.toString()) }
                metadata?.genres?.take(2)?.forEach { add(it) }
                entry.video.resolutionLabel?.let { add(it) }
                if (entry.video.durationMs > 0) add(entry.video.durationMs.formatDuration())
            }
            if (facts.isNotEmpty()) {
                Text(
                    facts.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val overview = metadata?.overview.orEmpty()
            if (overview.isNotBlank()) {
                Text(
                    overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(20.dp))
        TvChip("Play", Icons.Filled.PlayArrow) { onOpenPlayer(entry.video.id) }
        Spacer(Modifier.width(12.dp))
        TvChip("Details", Icons.Filled.Info) { onOpenDetail(entry.video.id) }
    }
}
