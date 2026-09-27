package com.opticast.player.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.*
import com.opticast.player.ui.components.EpisodeProgressStatus
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.posterUrlFor
import com.opticast.player.ui.components.posterRemoteUrlFor
import com.opticast.player.data.AppContainer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.ui.components.formatDuration
import java.util.Locale

internal fun playerMovies(entries: List<LibraryEntry>): List<LibraryEntry> = entries
    .filter { !it.video.isEpisode && it.metadata?.type != "tv" }
    .distinctBy { it.video.id }
    .sortedWith(compareBy<LibraryEntry> { it.video.name.lowercase(Locale.ROOT) }.thenBy { it.video.id })

internal fun movieCountLabel(count: Int): String = "$count movie${if (count == 1) "" else "s"}"

internal fun moviePositionLabel(entries: List<LibraryEntry>, currentId: Long): String {
    val index = entries.indexOfFirst { it.video.id == currentId }
    return if (index >= 0) "${index + 1}/${entries.size}" else movieCountLabel(entries.size)
}

internal fun playerCollection(entries: List<LibraryEntry>, current: LibraryEntry): List<LibraryEntry> =
    if (isShowEntry(current)) showCollection(entries, current) else playerMovies(entries)
internal fun collectionCountLabel(count: Int, episodes: Boolean): String =
    if (episodes) "$count episode${if (count == 1) "" else "s"}" else movieCountLabel(count)
internal fun collectionPositionLabel(entries: List<LibraryEntry>, currentId: Long, episodes: Boolean): String {
    val index = entries.indexOfFirst { it.video.id == currentId }
    return if (index >= 0) "${index + 1}/${entries.size}" else collectionCountLabel(entries.size, episodes)
}
private fun browserEntryTitle(entry: LibraryEntry): String {
    if (!isShowEntry(entry)) return entry.video.name
    val season = entry.video.parsed.season ?: entry.metadata?.seasonNumber
    val episode = entry.video.parsed.episode ?: entry.metadata?.episodeNumber
    val tag = if (season != null && episode != null) "S%02dE%02d".format(season, episode) else null
    return listOfNotNull(tag, entry.metadata?.episodeName?.takeIf { it.isNotBlank() } ?: entry.video.name).joinToString(" · ")
}

@Composable
internal fun MovieBrowser(movies: List<LibraryEntry>, currentId: Long, loading: Boolean,
    error: String?, onRetry: () -> Unit, onSelect: (LibraryEntry) -> Unit, onDismiss: () -> Unit, showTitle: String? = null) {
    val prefs = LocalContext.current.getSharedPreferences("player_browser", android.content.Context.MODE_PRIVATE)
    val layoutKey = if (showTitle != null) "episodes_horizontal" else "horizontal"
    var horizontal by remember(layoutKey) { mutableStateOf(prefs.getBoolean(layoutKey, true)) }
    val posterVersion by AppContainer.posterCache.version.collectAsStateWithLifecycle()
    val currentIndex = movies.indexOfFirst { it.video.id == currentId }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
    LaunchedEffect(horizontal, currentId, movies) {
        if (currentIndex >= 0) {
            if (horizontal) rowState.scrollToItem(currentIndex) else listState.scrollToItem(currentIndex)
        }
    }
    val listHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    PlayerMenu(onDismissRequest = onDismiss, maxWidth = if (horizontal) 960.dp else 600.dp,
        headerTitle = if (loading) "Now Playing · Loading…" else if (error != null) "Now Playing" else "${showTitle ?: "Now Playing"} · ${collectionCountLabel(movies.size, showTitle != null)}") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (horizontal) "Horizontal cards" else "Vertical list", Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = {
                horizontal = !horizontal
                prefs.edit().putBoolean(layoutKey, horizontal).apply()
            }) { Icon(if (horizontal) Icons.Filled.ViewList else Icons.Filled.ViewCarousel,
                contentDescription = if (horizontal) "Switch to vertical list" else "Switch to horizontal cards") }
        }
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Column(Modifier.padding(20.dp)) {
                Text(error)
                TextButton(onClick = onRetry) { Text("Retry local library") }
            }
            movies.isEmpty() -> Text(if (showTitle != null) "No available episodes found for this show." else "No movies found in the local library. TV episodes and excluded folders are not listed here.", Modifier.padding(20.dp))
            horizontal -> LazyRow(state = rowState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(movies, key = { _, entry -> entry.video.id }) { index, entry ->
                    MovieBrowserCard(entry, index + 1, entry.video.id == currentId, true, posterVersion) { onSelect(entry) }
                }
            }
            else -> LazyColumn(state = listState, modifier = Modifier.heightIn(max = listHeight),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(movies, key = { _, entry -> entry.video.id }) { index, entry ->
                    MovieBrowserCard(entry, index + 1, entry.video.id == currentId, false, posterVersion) { onSelect(entry) }
                }
            }
        }
    }
}

@Composable
private fun MovieBrowserCard(entry: LibraryEntry, number: Int, selected: Boolean, horizontal: Boolean, posterVersion: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    Surface(modifier = (if (horizontal) Modifier.width(156.dp) else Modifier.fillMaxWidth()).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp), color = if (selected) colors.primaryContainer else colors.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) accent else colors.outlineVariant)) {
        if (horizontal) Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrowserThumbnail(entry, number, Modifier.fillMaxWidth().aspectRatio(2f / 3f), posterVersion, durationBadge = true)
            Text(browserEntryTitle(entry), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            BrowserFacts(entry, selected, showDuration = false)
        } else Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BrowserThumbnail(entry, number, Modifier.width(72.dp).aspectRatio(2f / 3f), posterVersion)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(browserEntryTitle(entry), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                BrowserFacts(entry, selected)
            }
        }
    }
}

@Composable
private fun BrowserFacts(entry: LibraryEntry, selected: Boolean, showDuration: Boolean = true) {
    val video = entry.video
    val playback = AppContainer.playbackState.progressOf(video.id)
    val resolution = if (video.width > 0 && video.height > 0) "${video.width}×${video.height}" else "Resolution unavailable"
    Text(listOfNotNull(video.durationMs.takeIf { showDuration && it > 0 }?.formatDuration(), resolution,
        if (selected) "Current" else null).joinToString(" · "),
        style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
    if (isShowEntry(entry)) EpisodeProgressStatus(playback)
}

@Composable
private fun BrowserThumbnail(entry: LibraryEntry, number: Int, modifier: Modifier, posterVersion: Int, durationBadge: Boolean = false) {
    // Same resolver, loader, aspect ratio and cache invalidation as the library posters.
    // A frame is only a fallback when library poster artwork is unavailable.
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainer)) {
        PosterImage(url = posterUrlFor(entry), remoteUrl = posterRemoteUrlFor(entry),
            fallbackTitle = entry.video.parsed.title.ifBlank { entry.video.name },
            videoId = entry.video.id, cacheBust = posterVersion, modifier = Modifier.fillMaxSize())
        if (durationBadge && entry.video.durationMs > 0) Text(entry.video.durationMs.formatDuration(),
            color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp).clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(horizontal = 6.dp, vertical = 3.dp))
        Text(number.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.TopStart).padding(5.dp).clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).padding(horizontal = 6.dp, vertical = 3.dp))
    }
}
