package com.opticast.player.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import com.opticast.player.data.local.PlaybackState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.State
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.data.remote.tmdbImageUrl
import com.opticast.player.data.remote.tmdbPosterUrl
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import com.opticast.player.data.model.LocalVideo
import androidx.compose.runtime.LaunchedEffect
import java.io.File

/** Builds a one-shot [ViewModelProvider.Factory] - keeps ViewModels simple without Hilt. */
fun viewModelFactory(creator: () -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
    }

fun Long.formatDuration(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/**
 * Placeholder poster shown for files that have no artwork yet. Deliberately
 * *looks like a video frame* - a film strip with perforations and a play glyph -
 * the way a dedicated video player renders an unscraped file, instead of the old
 * flat colour block. Drawn with Canvas, so it costs nothing to ship and scales
 * to any grid size.
 */
@Composable
fun FallbackPoster(title: String, modifier: Modifier = Modifier) {
    // A little hue variety so a screen full of unmatched files is still
    // readable, but kept dark and low-saturation so it reads as "placeholder".
    val hues = listOf(210f, 260f, 330f, 25f, 170f, 45f, 290f)
    val hue = hues[Math.floorMod(title.hashCode(), hues.size)]
    val line = Color.White.copy(alpha = 0.16f)
    val glyph = Color.White.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.hsl(hue, 0.20f, 0.17f),
                        Color.hsl((hue + 25f) % 360f, 0.24f, 0.07f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Film-strip perforations along the top and bottom edges.
            val perfW = w * 0.062f
            val perfH = perfW * 0.62f
            val gap = perfW * 0.72f
            val edge = h * 0.035f
            var x = gap
            while (x + perfW < w) {
                drawRoundRect(
                    color = line,
                    topLeft = Offset(x, edge),
                    size = Size(perfW, perfH),
                    cornerRadius = CornerRadius(perfH * 0.28f),
                )
                drawRoundRect(
                    color = line,
                    topLeft = Offset(x, h - edge - perfH),
                    size = Size(perfW, perfH),
                    cornerRadius = CornerRadius(perfH * 0.28f),
                )
                x += perfW + gap
            }
            // Centre frame with a play glyph: the "this is a video" cue.
            val fw = w * 0.44f
            val fh = fw * 0.74f
            val cx = w / 2f
            val cy = h * 0.45f
            drawRoundRect(
                color = glyph,
                topLeft = Offset(cx - fw / 2f, cy - fh / 2f),
                size = Size(fw, fh),
                cornerRadius = CornerRadius(fw * 0.12f),
                style = Stroke(width = w * 0.014f),
            )
            val t = fw * 0.30f
            val path = Path().apply {
                moveTo(cx - t * 0.42f, cy - t * 0.62f)
                lineTo(cx + t * 0.62f, cy)
                lineTo(cx - t * 0.42f, cy + t * 0.62f)
                close()
            }
            drawPath(path, glyph)
        }
    }
}

/** Brand accents for the poster badges (same cyan as the app icon). */
private val BadgeCyan = Color(0xFF35C8FF)
private val BadgeInk = Color(0xFF04121F)

/** How long a freshly added file keeps its NEW badge. */
private const val NEW_BADGE_WINDOW_DAYS = 14L

/**
 * "Newly added" for the NEW badge, from MediaStore's DATE_ADDED: a file copied
 * onto the device in the last two weeks is flagged. Intentionally simple - no
 * extra persisted state that could go stale or need migrating.
 */
fun LocalVideo.isNewlyAdded(nowSec: Long = System.currentTimeMillis() / 1000L): Boolean =
    dateAddedSec > 0L && (nowSec - dateAddedSec) in 0L..(NEW_BADGE_WINDOW_DAYS * 86_400L)

/** Small pill used for the SxxEyy / NEW / unmatched markers on a poster. */
@Composable
private fun PosterPill(text: String, container: Color, content: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
        Box(Modifier.background(Brush.linearGradient(listOf(container, container.copy(alpha = 0.68f))))) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
        }
    }
}

/** Gradients are painted behind the label; only NEW has a quiet periodic shimmer. */
@Composable
private fun StatusBadge(status: PosterBadge, modifier: Modifier = Modifier, singleLine: Boolean = false) {
    val minimal = LocalMinimalStyle.current
    val colours = remember(status) { listOf(Color(status.gradientStart), Color(status.gradientEnd)) }
    val gradient = remember(status) { Brush.linearGradient(colours) }
    // Shimmer for NEW badge only
    val shimmerAlpha = if (status == PosterBadge.NEW) {
        val infiniteTransition = rememberInfiniteTransition(label = "newShimmer")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1000),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "shimmerAlpha"
        )
        alpha
    } else 1f
    val icon = when (status) {
        PosterBadge.NEW -> Icons.Filled.AutoAwesome
        PosterBadge.CONTINUE -> Icons.Filled.PlayArrow
        PosterBadge.WATCHED -> Icons.Filled.Check
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(7.dp),
        color = Color.Transparent, contentColor = BadgeInk,
        border = BorderStroke(1.dp, Color.White.copy(alpha = if(minimal) 0.16f else 0.75f)), shadowElevation = if(minimal) 0.dp else 2.dp) {
        Row(Modifier.background(gradient).padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            // Reserve the narrow portrait badge for the full two-line resume label.
            if (status != PosterBadge.CONTINUE || singleLine) Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp))
            Text(if (singleLine) status.label.replace('\n', ' ') else status.label,
                modifier = Modifier.weight(1f, fill = false),
                fontSize = 10.sp, lineHeight = 12.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (status == PosterBadge.CONTINUE && !singleLine) 2 else 1)
        }
    }
}

/**
 * Artwork for a title with no poster to show.
 *
 * Dynamic frame extraction: a real frame from the video file is pulled once,
 * cached on disk, and used as the artwork. Until it is ready (a moment, the
 * first time) the drawn film-frame placeholder stands in, so a card is never
 * blank and never a flat colour block.
 */
private fun findFirstCachedFrame(ids: List<Long>): File? {
    return ids.firstNotNullOfOrNull { AppContainer.frameArtwork.cached(it) }
}

@Composable
fun rememberFrameArtwork(videoId: Long?): File? {
    var artwork by remember(videoId) {
        mutableStateOf(videoId?.let { AppContainer.frameArtwork.cached(it) })
    }
    LaunchedEffect(videoId) {
        if (videoId == null || videoId <= 0L || artwork != null) return@LaunchedEffect
        artwork = AppContainer.frameArtwork.generate(videoId)
    }
    return artwork
}

/** Loads a poster image with placeholder for smooth scrolling on low-RAM 32-bit 3GB devices. */
@Composable
fun PosterImage(
    url: String?,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
    cacheBust: Int = 0, // kept for compatibility but not used - causes choppiness
    remoteUrl: String? = null,
    videoId: Long? = null,
) {
    var useRemote by remember(url, remoteUrl) { mutableStateOf(false) }
    var failed by remember(url, remoteUrl) { mutableStateOf(false) }
    val effectiveUrl = if (useRemote && remoteUrl != null) remoteUrl else url
    // Placeholder brush - shows immediately while real poster loads, prevents white flash
    val placeholderBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2A2A),
                Color(0xFF3A3A3A),
                Color(0xFF2A2A2A)
            )
        )
    }
    Box(modifier.background(placeholderBrush)) {
        if (effectiveUrl != null && !failed) {
            AsyncImage(
                onError = {
                    if (!useRemote && remoteUrl != null && effectiveUrl != remoteUrl) {
                        useRemote = true
                    } else {
                        failed = true
                    }
                },
                model = ImageRequest.Builder(LocalContext.current)
                    .data(effectiveUrl)
                    .crossfade(false)
                    .crossfade(0)
                    .memoryCacheKey(effectiveUrl)
                    .diskCacheKey(effectiveUrl)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = fallbackTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val frame = rememberFrameArtwork(videoId)
            if (frame != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(frame)
                        .crossfade(true)
                        .crossfade(200)
                        .memoryCacheKey("frame-${videoId}")
                        .diskCacheKey("frame-${videoId}")
                        .build(),
                    contentDescription = fallbackTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FallbackPoster(fallbackTitle)
            }
        }
    }
}

/**
 * Poster URL for a library entry: the locally cached file when available
 * (offline!), otherwise the remote TMDB image.
 */
fun posterUrlFor(entry: LibraryEntry): String? =
    AppContainer.posterCache.localUrl(entry.video.id, entry.metadata)
        ?: tmdbPosterUrl(entry.metadata?.posterPath)

/** Remote (TMDB) poster URL - fallback when the cached file is unavailable. */
fun posterRemoteUrlFor(entry: LibraryEntry): String? =
    tmdbPosterUrl(entry.metadata?.posterPath)

/** Bottom scrim shared by every poster/show card (allocated once, not per card). */
private val PosterScrim = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
)

/** Poster grid card for a single movie / episode file. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)

@Composable
private fun BasePosterCard(
    title: String,
    subtitle: String,
    posterUrl: String?,
    remotePosterUrl: String?,
    fallbackVideoId: Long?,
    showFrame: File?,
    badge: PosterBadge?,
    extraPill: String?,
    resumeState: PlaybackState?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cacheBust: Int = 0,
) {
    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        if (posterUrl != null || remotePosterUrl != null) {
            PosterImage(url = posterUrl, fallbackTitle = title, modifier = Modifier.fillMaxSize(), cacheBust = cacheBust, remoteUrl = remotePosterUrl, videoId = fallbackVideoId)
        } else if (showFrame != null) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(showFrame).crossfade(false).build(), contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            FallbackPoster(title, modifier = Modifier.fillMaxSize())
        }
        Box(Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(PosterScrim).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                if (resumeState != null) ResumeProgress(resumeState)
            }
        }
        Column(Modifier.align(Alignment.TopStart).padding(posterBadgeInsetDp(if(LocalMinimalStyle.current) 8f else 22f).dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            badge?.let { StatusBadge(it) }
            extraPill?.let { PosterPill(it, Color.Black.copy(alpha = 0.8f), Color.White) }
        }
    }
}


@Composable
fun PosterCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cacheBust: Int = 0,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val metadata = entry.metadata
    val posterUrl = remember(entry.video.id, entry.metadata?.posterPath) { posterUrlFor(entry) }
    val remotePosterUrl = remember(entry.metadata?.posterPath) { posterRemoteUrlFor(entry) }
    val fallbackTitle = remember(entry.video.name, entry.video.parsed.title) { (entry.video.parsed.title.ifBlank { entry.video.name }).trim().trimEnd('(', ')', '[', ']', ' ', '-', '_', '.').trim() }
    val playback = remember(entry.video.id) { AppContainer.playbackState.progressOf(entry.video.id) }
    val badge = primaryPosterBadge(entry.video.isNewlyAdded(), playback?.isWatched == true, playback?.isResumable == true)
    val subtitle = metadata?.year?.toString() ?: entry.video.seasonEpisodeTag ?: entry.video.durationMs.formatDuration()
    val resume = if (playback?.isResumable == true) playback else null
    BasePosterCard(title = metadata?.displayTitle ?: fallbackTitle, subtitle = subtitle, posterUrl = posterUrl, remotePosterUrl = remotePosterUrl, fallbackVideoId = entry.video.id, showFrame = null, badge = badge, extraPill = entry.video.seasonEpisodeTag, resumeState = resume, onClick = onClick, onLongClick = onLongClick, modifier = modifier, cacheBust = cacheBust)
}


/**
 * Show-level frame: a real bitmap from any episode when the show has no TMDB
 * poster. The old [ShowCard] passed no [videoId] so the grid was blank while
 * the per-episode row showed a thumbnail - this closes that gap.
 * Tries cached frames first for instant display, then generates on demand.
 */
@Composable
fun rememberShowFrameArtwork(episodes: List<LibraryEntry>): File? {
    val ids = remember(episodes) { episodes.map { it.video.id } }
    var file by remember(ids) { mutableStateOf(findFirstCachedFrame(ids)) }
    LaunchedEffect(ids) {
        if (file != null) return@LaunchedEffect
        for (id in ids) {
            val generated = AppContainer.frameArtwork.generate(id)
            if (generated != null) { file = generated; break }
        }
        if (file == null && ids.isNotEmpty()) file = AppContainer.frameArtwork.cached(ids.first())
    }
    return file
}

/** Card representing a whole TV show (grouped episodes). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowCard(
    showTitle: String,
    episodes: List<LibraryEntry>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cacheBust: Int = 0,
) {
    val representative = episodes.firstOrNull { it.metadata?.posterPath != null } ?: episodes.first()
    val posterUrl = posterUrlFor(representative)
    val remotePoster = posterRemoteUrlFor(representative)
    val vote = representative.metadata?.voteAverage ?: 0.0
    val watched = episodes.isNotEmpty() && episodes.all { AppContainer.playbackState.progressOf(it.video.id)?.isWatched == true }
    val resumeState = latestResumeProgress(episodes.mapNotNull { AppContainer.playbackState.progressOf(it.video.id) })
    val newlyAdded = episodes.any { it.video.isNewlyAdded() }
    val showFrame = if (posterUrl == null && remotePoster == null) rememberShowFrameArtwork(episodes) else null
    val fallbackVideoId = remember(episodes, showFrame) {
        showFrame?.let { f -> episodes.firstOrNull { AppContainer.frameArtwork.cached(it.video.id) == f }?.video?.id }
            ?: episodes.firstNotNullOfOrNull { e -> AppContainer.frameArtwork.cached(e.video.id)?.let { e.video.id } }
            ?: episodes.firstOrNull()?.video?.id
    }
    val badge = primaryPosterBadge(newlyAdded, watched, resumeState != null)
    val subtitle = "${episodes.size} episode${if (episodes.size == 1) "" else "s"}" + (if (vote > 0.0) " · ★ %.1f".format(vote) else "")
    BasePosterCard(title = showTitle, subtitle = subtitle, posterUrl = posterUrl, remotePosterUrl = remotePoster, fallbackVideoId = fallbackVideoId, showFrame = showFrame, badge = badge, extraPill = null, resumeState = if (!watched) resumeState else null, onClick = onClick, onLongClick = onLongClick, modifier = modifier, cacheBust = cacheBust)
}


/** Highlight matching text in search */

/** Heart burst animation when adding favorite */

/** Streak for stats */

/** Empty states illustration */

/** Bold section header used across the library. */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (count != null) {
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** A bold, local-library resume card with a top ribbon and accurate progress. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingCard(
    entry: LibraryEntry,
    progress: Float,
    remainingLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    selectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    val backdropUrl = tmdbBackdropUrl(entry.metadata?.backdropPath ?: entry.metadata?.episodeStillPath)
    val title = entry.metadata?.displayTitle ?: entry.video.parsed.title
    val tag = entry.video.seasonEpisodeTag ?: entry.metadata?.year?.toString()
    val playback = AppContainer.playbackState.progressOf(entry.video.id)
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val largeText = fontScale > 1.25f
    Box(modifier.width(if (compact) 246.dp else 286.dp).height((if (compact) 192.dp else 216.dp) * fontScale)
        .clip(RoundedCornerShape(if(LocalMinimalStyle.current) 8.dp else 26.dp))
        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(if(LocalMinimalStyle.current) 8.dp else 26.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        PosterImage(url = backdropUrl ?: posterUrlFor(entry), fallbackTitle = title,
            videoId = entry.video.id, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.22f), Color(0xF206101F)))))
        StatusBadge(PosterBadge.CONTINUE, Modifier.align(Alignment.TopStart).padding(posterBadgeInsetDp(if(LocalMinimalStyle.current) 8f else 26f).dp).widthIn(max = if (compact) 184.dp else 224.dp), singleLine = !largeText)
        if (!selectionMode) Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFFFE5AD)))), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = BadgeInk, modifier = Modifier.size(23.dp))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold,
                color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(tag, remainingLabel).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFDEA0),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (playback?.isResumable == true) ResumeProgress(playback)
        }
    }
}

/** High-contrast inset progress, shared by movies, episodes and grouped shows. */
@Composable
private fun ResumeProgress(state: PlaybackState) {
    val fraction = visibleResumeFraction(state)
    Column(Modifier.fillMaxWidth().padding(top = 7.dp)
        .clip(RoundedCornerShape(9.dp)).background(Color(0xE6091420))
        .padding(horizontal = 7.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(if (fraction != null) "${(fraction * 100).toInt()}% played" else "Saved at ${state.positionMs.formatDuration()}",
            color = Color(0xFFFFE4A3), fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .semantics {
                if (fraction != null) progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                else contentDescription = "Playback position saved; total duration unavailable"
            }) {
            if (fraction != null) Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFF0B8), Color(0xFFFFBD4D), Color(0xFFFF963C)))))
        }
    }
}
