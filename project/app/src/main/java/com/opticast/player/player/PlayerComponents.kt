@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, UnstableApi::class, ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.opticast.player.player

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Close
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ZoomOutMap
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.model.showTitleOf
import com.opticast.player.data.model.isShowEntry
import com.opticast.player.data.model.LocalVideo
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import java.util.concurrent.atomic.AtomicLong
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.opticast.player.data.AppContainer
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.data.AppSettings
import com.opticast.player.data.model.SavedSubtitle
import com.opticast.player.data.remote.SubtitleResult
import com.opticast.player.ui.components.PlayerProgressBar
import com.opticast.player.ui.components.formatDuration
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.produceState
import androidx.compose.ui.layout.ContentScale
import com.opticast.player.data.local.Chapter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlin.math.absoluteValue
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween

/** A selectable media track (subtitle or audio) shown in the picker sheet. */
private data class TrackOption(
    val type: Int, // C.TRACK_TYPE_TEXT or C.TRACK_TYPE_AUDIO
    val group: Tracks.Group,
    val index: Int,
    val label: String,
)

/** Mutable holder for double-tap detection across gesture events. */
private class TapState {
    var lastTapMs: Long = 0L
    var singleTapJob: Job? = null
}

@Composable
@Composable
internal fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.50f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White.copy(alpha = if (enabled) 1f else 0.3f))
    }
}

@Composable
internal fun BigPlayPauseButton(
    player: Player,
    isPlaying: Boolean,
    onPoke: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "playScale",
    )
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                if (player.isPlaying) player.pause() else player.play()
                onPoke()
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
internal fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------- helpers

/**
 * Builds the sideloaded subtitle configurations, applying a timing offset by
 * rewriting cue timestamps into temp files when needed.
 */
private suspend fun buildSubtitleConfigs(
    context: Context,
    videoId: Long,
    subtitles: List<SavedSubtitle>,
    offsetMs: Long,
): List<MediaItem.SubtitleConfiguration> = withContext(Dispatchers.IO) {
    val shiftedDir = File(context.cacheDir, "shifted").apply { mkdirs() }
    subtitles.mapIndexedNotNull { index, subtitle ->
        val source = File(subtitle.filePath)
        if (!usableSubtitleFile(source)) return@mapIndexedNotNull null
        val shiftable = subtitle.filePath.endsWith(".srt", true) ||
            subtitle.filePath.endsWith(".vtt", true)
        val fileToUse = if (offsetMs != 0L && shiftable && source.exists()) {
            runCatching {
                val shifted = SrtShifter.shift(source.readText(), offsetMs)
                val extension = source.extension.ifBlank { "srt" }
                val out = File(shiftedDir, "$videoId-$index-$offsetMs.$extension")
                out.writeText(shifted)
                out
            }.getOrDefault(source)
        } else {
            source
        }
        MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(fileToUse))
            .setMimeType(mimeForPath(fileToUse.name))
            .setLabel("${subtitle.language.uppercase()} · ${subtitle.releaseName}")
            .setLanguage(subtitle.language)
            .setSelectionFlags(if (index == 0) C.SELECTION_FLAG_DEFAULT else 0)
            .build()
    }
}

internal fun mimeForPath(path: String): String = when {
    path.endsWith(".ass", ignoreCase = true) || path.endsWith(".ssa", ignoreCase = true) ->
        MimeTypes.TEXT_SSA
    path.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
    else -> MimeTypes.APPLICATION_SUBRIP
}

internal fun mimeForName(name: String): String? {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp4", "m4v", "mov" -> MimeTypes.VIDEO_MP4
        "mkv", "mka" -> MimeTypes.VIDEO_MATROSKA
        "webm" -> MimeTypes.VIDEO_WEBM
        "avi" -> MimeTypes.VIDEO_MP4 // fallback — extractor will sniff, but hint helps for content:// without extension
        "3gp", "3gpp" -> "video/3gpp"
        "ts", "m2ts", "mts" -> "video/mp2ts"
        "flv" -> "video/x-flv"
        "wmv", "asf" -> "video/x-ms-wmv"
        "mp3" -> MimeTypes.AUDIO_MPEG
        "aac", "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "opus", "ogg" -> "audio/ogg"
        else -> null
    }
}

internal fun captionStyleFor(index: Int): CaptionStyleCompat = when (index) {
    1 -> CaptionStyleCompat(
        Color.White.toArgb(),
        0xB3000000.toInt(), // translucent black box
        Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_NONE,
        Color.Transparent.toArgb(),
        null, // typeface
    )
    3 -> CaptionStyleCompat(
        Color.White.toArgb(), Color.Transparent.toArgb(), Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_NONE, Color.Transparent.toArgb(), null,
    )
    2 -> CaptionStyleCompat(
        Color.Yellow.toArgb(),
        Color.Transparent.toArgb(),
        Color.Transparent.toArgb(),
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        Color.Black.toArgb(),
        null, // typeface
    )
    else -> CaptionStyleCompat.DEFAULT
}

/**
 * A single scrub-preview frame plus its timestamp. Uses whatever thumbnail the
 * cache has for this position; before the frames exist (or for files shorter
 * than 30 seconds, which are not worth indexing) only the time is shown, so the
 * control never breaks.
 */
@Composable
internal fun ScrubPreview(videoId: Long, positionMs: Long) {
    val previewContext = LocalContext.current
    val file = remember(videoId, positionMs / 1000L) {
        AppContainer.thumbnails.frameFor(videoId, positionMs)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        if (file != null) {
            coil.compose.AsyncImage(
                model = remember(previewContext, file) {
                    coil.request.ImageRequest.Builder(previewContext).data(file)
                        .memoryCachePolicy(coil.request.CachePolicy.DISABLED).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(148.dp)
                    .height(83.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(8.dp),
                    ),
            )
        }
        Text(
            positionMs.formatDuration(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Playback-info panel: the nerd view of what is actually being
 * decoded. Counters refresh twice a second while the panel is open.
 */
@Composable
internal fun PlaybackInfoPanel(
    controller: androidx.media3.common.Player,
    video: com.opticast.player.data.model.LocalVideo?,
    positionMs: Long,
    durationMs: Long,
    engine: String,
) {
    // Re-reads the live values on a slow tick; nothing here affects playback.
    val tick by produceState(0) {
        while (true) {
            kotlinx.coroutines.delay(500)
            value++
        }
    }
    val nativeInfo = com.opticast.player.player.mpv.MpvRuntimeInfo.current?.takeIf { engine == "mpv" && it.mediaId == controller.currentMediaItem?.mediaId }
    val videoFormat = remember(tick) { selectedFormat(controller, C.TRACK_TYPE_VIDEO) }
    val audioFormat = remember(tick) { selectedFormat(controller, C.TRACK_TYPE_AUDIO) }
    val textFormat = remember(tick) { selectedFormat(controller, C.TRACK_TYPE_TEXT) }
    val size = remember(tick) { controller.videoSize }
    val buffered = remember(tick) { controller.bufferedPercentage }

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
    ) {
        Text("Playback info", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(14.dp))

        InfoRow("Container", video?.name?.substringAfterLast('.', "")?.uppercase().orEmpty()
            .ifBlank { "unknown" })
        InfoRow("Engine", if(engine == "mpv") "mpv / FFmpeg" else "Media3 ExoPlayer")
        InfoRow(
            "Resolution",
            when {
                size.width > 0 -> "${size.width} x ${size.height}"
                videoFormat?.width != null && videoFormat.width > 0 ->
                    "${videoFormat.width} x ${videoFormat.height}"
                else -> "—"
            },
        )
        InfoRow(
            "Frame rate",
            videoFormat?.frameRate?.takeIf { it > 0f }?.let { "%.3f fps".format(it) } ?: "—",
        )
        InfoRow("Video codec", nativeInfo?.videoCodec ?: videoFormat?.codecs ?: videoFormat?.sampleMimeType ?: "—")
        InfoRow("Video bitrate", videoFormat?.bitrate?.takeIf { it > 0 }?.let { formatBitrate(it) } ?: "—")
        InfoRow(if(engine == "mpv") "Hardware mode" else "Video decoder", if(engine == "mpv") nativeInfo?.hardwareMode ?: "—" else PlayerStats.videoDecoder ?: "—")
        InfoRow("Audio codec", nativeInfo?.audioCodec ?: audioFormat?.sampleMimeType ?: "—")
        InfoRow(
            "Audio",
            audioFormat?.let { f ->
                listOfNotNull(
                    f.channelCount.takeIf { it > 0 }?.let { "$it ch" },
                    f.sampleRate.takeIf { it > 0 }?.let { "$it Hz" },
                    f.bitrate.takeIf { it > 0 }?.let { formatBitrate(it) },
                ).joinToString(" · ").ifBlank { "—" }
            } ?: "—",
        )
        if(engine != "mpv") InfoRow("Audio decoder", PlayerStats.audioDecoder ?: "—")
        InfoRow(
            "Subtitles",
            textFormat?.let { it.label ?: it.language ?: "selected" } ?: "off",
        )
        InfoRow("Dropped frames", if(engine == "mpv") nativeInfo?.droppedFrames ?: "—" else PlayerStats.droppedFrames.toString())
        if (engine == "mpv") InfoRow("Buffer", "Not reported · capture snapshot below")
        else InfoRow("Buffered timeline", "$buffered %")
        InfoRow(
            "Speed",
            "%.2fx".format(controller.playbackParameters.speed),
        )
        if (PlayerStats.bandwidthEstimate > 0L) {
            InfoRow("Network", "${formatBitrate(PlayerStats.bandwidthEstimate)}/s")
        }
        InfoRow("Position", "${positionMs.formatDuration()} / ${durationMs.formatDuration()}")
        Spacer(Modifier.height(10.dp))
        Text(
            "Decoder names come from the renderer; “—” means the platform has not " +
                "reported it yet.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        MemorySnapshotPanel()
    }
}

/** Format of the currently selected track of [type], or null when there is none. */
internal fun selectedFormat(
    player: androidx.media3.common.Player,
    type: Int,
): androidx.media3.common.Format? {
    val groups = player.currentTracks.groups
    for (group in groups) {
        if (group.type != type) continue
        for (i in 0 until group.length) {
            if (group.isTrackSelected(i)) return group.mediaTrackGroup.getFormat(i)
        }
    }
    return null
}

internal fun formatBitrate(bits: Int): String = formatBitrate(bits.toLong())

internal fun formatBitrate(bits: Long): String = when {
    bits >= 1_000_000 -> "%.1f Mbps".format(bits / 1_000_000.0)
    bits >= 1_000 -> "%.0f kbps".format(bits / 1_000.0)
    else -> "$bits bps"
}

@Composable
internal fun InfoRow(label: String, value: String) {
    com.opticast.player.ui.components.AlignedLabelValue(label, value)
}

/**
 * Audio controls, shared by the in-player sheet and the settings screen.
 * Presets are named curves rather than raw band sliders on purpose: they behave
 * predictably on devices whose equalisers have different band counts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioControls(
    settings: AppSettings,
    onPreset: (String) -> Unit,
    onDialogueBoost: (Boolean) -> Unit,
    onVolumeBoost: (Int) -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp),
    ) {
        Text("Audio", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Text("Preset", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            AudioPreset.entries.forEach { preset ->
                FilterChip(
                    selected = settings.audioPreset == preset.id,
                    onClick = { onPreset(preset.id) },
                    label = { Text(preset.label) },
                )
            }
        }
        Text(
            AudioPreset.forId(settings.audioPreset).blurb,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Dialogue boost", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Extra presence on speech without making action scenes louder.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = settings.dialogueBoost, onCheckedChange = onDialogueBoost)
        }

        Spacer(Modifier.height(12.dp))
        Text("Volume boost: ${settings.audioBoostPct}%", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            listOf(100, 125, 150, 200, 300, 400).forEach { pct ->
                FilterChip(
                    selected = settings.audioBoostPct == pct,
                    onClick = { onVolumeBoost(pct) },
                    label = { Text(if (pct == 100) "Off" else "$pct%") },
                )
            }
        }
    }
}

/**
 * A stand-in "video" for a network stream. Remote files have no MediaStore id,
 * so they get a stable negative id derived from the link: everything downstream
 * (media item, title, this screen's state) works unchanged, while progress and
 * subtitle entries stay separate from any local file.
 */
internal fun syntheticRemoteVideo(
    uri: String,
    title: String?,
): com.opticast.player.data.model.LocalVideo {
    val name = title ?: uri.substringAfterLast('/').ifBlank { "Network video" }
    val id = -(uri.hashCode().toLong().absoluteValue % Int.MAX_VALUE.toLong()).coerceAtLeast(1L)
    return com.opticast.player.data.model.LocalVideo(
        id = id,
        name = name,
        uri = uri,
        sizeBytes = 0L,
        durationMs = 0L,
        dateAddedSec = System.currentTimeMillis() / 1000L,
        width = 0,
        height = 0,
        parsed = com.opticast.player.data.parser.NameParser.parse(name),
    )
}
