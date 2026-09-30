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
fun PlayerScreen(
    videoId: Long,
    remoteUri: String? = null,
    remoteTitle: String? = null,
    onEngineFallback: ((Long) -> Unit)? = null,
    onBack: () -> Unit,
    onSystemBack: (() -> Unit)? = null,
    onClosePlayer: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    fun backToLibrary() {
        OptiCastPlaybackService.stopPlayback(context)
        context.startActivity(android.content.Intent(context, com.opticast.player.MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(com.opticast.player.MainActivity.EXTRA_SHOW_LIBRARY, true))
        (onClosePlayer ?: onBack)()
    }
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    val storeVersion by AppContainer.metadataStore.version.collectAsState()
    val appSettings by AppContainer.settings.settings.collectAsState(initial = AppContainer.initialSettings)

    // Instant landscape: rotate immediately on entry (no waiting screen).
    LaunchedEffect(Unit) {
        if (AppContainer.initialSettings.autoLandscape) {
            activity?.requestedOrientation =
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // Internal id so "Up next" can switch episodes without re-navigating.
    var activeVideoId by remember(videoId, remoteUri) { mutableLongStateOf(videoId) }

    val remoteVideo = remember(activeVideoId,remoteUri,remoteTitle) {
        if(remoteUri!=null && activeVideoId<=0L) syntheticRemoteVideo(remoteUri,remoteTitle) else null
    }
    val requestedId=remoteVideo?.id ?: activeVideoId
    val requestToken=remember(requestedId,remoteUri) { Any() }
    val latestRequest=rememberUpdatedState(requestToken)
    val requestedAt=remember(activeVideoId,remoteUri) { android.os.SystemClock.elapsedRealtime() }
    var resolvedVideo by remember(activeVideoId,remoteUri) { mutableStateOf<LocalVideo?>(remoteVideo) }
    var resolvingVideo by remember(activeVideoId,remoteUri) { mutableStateOf(remoteVideo==null) }
    LaunchedEffect(activeVideoId,remoteUri) {
        PlayerStats.beginStartup(requestedId.toString(),requestedAt)
        // Identity checks remain mandatory, but no longer block UI/session connection on provider I/O.
        if(remoteVideo==null) resolvedVideo=withContext(Dispatchers.IO) { runCatching { AppContainer.mediaScanner.byId(activeVideoId) }.getOrNull() }
        resolvingVideo=false
        if(resolvedVideo!=null) {
            resolvedVideo?.let { ResumeProbes.forId(requestedId.toString())?.bind(it.uri) }
            PlayerStats.markStartup(requestedId.toString(),StartupStage.RESOLVED)
        }
    }
    var resolutionSlow by remember(requestToken) { mutableStateOf(false) }
    LaunchedEffect(requestToken,resolvingVideo) {
        if(resolvingVideo) { delay(PLAYER_CONNECTION_TIMEOUT_MS); resolutionSlow=true }
    }
    val video=resolvedVideo
    val libraryVideoId = video?.id ?: activeVideoId
    val metadata = remember(libraryVideoId, storeVersion) {
        AppContainer.metadataStore.get(libraryVideoId)
    }
    val savedSubtitles = remember(libraryVideoId, storeVersion) {
        AppContainer.metadataStore.subtitlesFor(libraryVideoId)
    }

    // --------------------- connect to the session-owned player ---------------------
    var connectionAttempt by remember { mutableIntStateOf(0) }
    var engineOverride by remember(requestedId) { mutableStateOf<String?>(null) }
    var fallbackTried by remember(requestedId) { mutableStateOf(false) }
    val videoPreferences = remember(context) { VideoPlaybackPreferences(context) }
    val preferenceKey = remember(video) { video?.let(::playbackPreferenceKey) }
    // Freeze the remembered engine for this request; recording success must not recreate the session.
    val rememberedEngine = remember(requestedId, preferenceKey) { preferenceKey?.let(videoPreferences::engine) }
    val selectedEngine = engineOverride ?: initialPlaybackEngine(appSettings.playbackEngine,
        Uri.parse(video?.uri ?: remoteUri.orEmpty()).scheme, rememberedEngine, appSettings.engineMemoryEnabled)
    // An old controller's intentional release must not mark the replacement disconnected.
    var connectionLost by remember(connectionAttempt, selectedEngine) { mutableStateOf(false) }
    val controllerFuture = remember(connectionAttempt, selectedEngine) {
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, OptiCastPlaybackService::class.java)),
        ).setConnectionHints(android.os.Bundle().apply {
            putString(OptiCastPlaybackService.ENGINE_HINT, selectedEngine)
        }).setListener(object : MediaController.Listener {
            override fun onDisconnected(controller: MediaController) { connectionLost=true }
        }).buildAsync()
    }
    val releaseConnection = remember(controllerFuture) {
        val released=java.util.concurrent.atomic.AtomicBoolean(false)
        val release: () -> Unit = { if(released.compareAndSet(false,true)) MediaController.releaseFuture(controllerFuture) }
        release
    }
    DisposableEffect(controllerFuture) { onDispose { releaseConnection() } }
    var controllerState by remember(controllerFuture) { mutableStateOf<MediaController?>(null) }
    var connectFailed by remember(controllerFuture) { mutableStateOf(false) }
    LaunchedEffect(controllerFuture) {
        val connected = kotlinx.coroutines.withTimeoutOrNull(PLAYER_CONNECTION_TIMEOUT_MS) {
            kotlinx.coroutines.suspendCancellableCoroutine<MediaController?> { continuation ->
                controllerFuture.addListener({
                    if(continuation.isActive) continuation.resumeWith(Result.success(runCatching { controllerFuture.get() }.getOrNull()))
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
                continuation.invokeOnCancellation { releaseConnection() }
            }
        }
        if(connected==null) connectFailed=true else controllerState=connected
    }


    // RAM: Clear Coil memory cache when entering playback to save RAM during video playing
    LaunchedEffect(controllerState) {
        if (controllerState != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { coil.Coil.imageLoader(context).memoryCache?.clear() }
            }
        }
    }

    LaunchedEffect(requestedId,controllerState) {
        if(controllerState!=null) {
            PlayerStats.markStartup(requestedId.toString(),StartupStage.CONNECTED)
            // Pause the outgoing item while verifying a different request; service saves its own ID.
            if(!samePlaybackItem(requestedId,controllerState?.currentMediaItem?.mediaId)) controllerState?.pause()
        }
    }
    val controllerRef = rememberUpdatedState(controllerState)

    // Identity of the requested resume operation, checked against the controller
    // before re-seeking. Never used as a persistence key.
    val mediaIdRef = remember { AtomicLong(0L) }

    // One-shot re-seek guard in case the stored start position was dropped
    // while the item was still initialising.
    val pendingResumeRef = remember { AtomicLong(0L) }
    val pendingResumeDeadlineRef = remember { AtomicLong(0L) }

    // The service synchronously persists its own item before stopping. UI readouts
    // may lag during a switch, so screen cleanup never writes playback snapshots.
    DisposableEffect(Unit) {
        onDispose {
            if (activity is PlayerActivity && !activity.ownsPlaybackSession()) return@onDispose
            val c = controllerRef.value
            // The service owns persistence; never combine a route ID with a controller snapshot.
            runCatching { c?.pause() }
            // Guaranteed stop at the UI layer: the moment the player leaves the
            // composition (PiP window closed, player dismissed, activity gone)
            // playback is torn down. This does not depend on any activity
            // lifecycle callback firing, which is what left audio running
            // before.
            if (activity?.isChangingConfigurations != true) runCatching { OptiCastPlaybackService.stopPlayback(context) }
            PiPController.isPlayerActive = false
            PiPController.isPlaying = { false }
            PiPController.wasPlayingBeforePip = false
            PiPController.onEnterPip = null
            PiPController.onBackground = null
            PiPController.onReturnFromPip = null
            PiPController.onPlayerClosing = null
            PiPController.onTogglePlay = null
            PiPController.onSeekBy = null
            PiPController.onPlayingChanged = null
            PiPController.onPipAspectChanged = null
            PiPController.videoWidth = 0
            PiPController.videoHeight = 0
        }
    }

    if(resolvingVideo && !connectFailed && !connectionLost) {
        Box(Modifier.fillMaxSize().background(Color.Black),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                androidx.compose.material3.LoadingIndicator()
                if(resolutionSlow) Text("Storage is taking longer to respond. You can return to the Library.",color=Color.White,modifier=Modifier.padding(24.dp))
                TextButton(onClick=::backToLibrary) { Text("Back to Library") }
            }
        }
        return
    }
    if (video == null || connectFailed || connectionLost) {
        val fileUnavailable=video==null && !resolvingVideo
        LaunchedEffect(Unit) {
            PiPController.isPlayerActive=false;PiPController.isPlaying={false}
            PiPController.onBackground=null;PiPController.onTogglePlay=null;PiPController.onSeekBy=null
            PiPController.onEnterPip=null;PiPController.onPlayerClosing=null
        }
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),contentAlignment=Alignment.Center) {
            Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(if(fileUnavailable) "Video unavailable" else "Could not connect to the player",style=MaterialTheme.typography.titleLarge)
                Text(if(fileUnavailable) "The file may have moved or its storage may be disconnected. Return to the Library to recheck it."
                    else "The player service did not become available or disconnected. You can retry without clearing your Library or watch history.")
                if(!fileUnavailable) Button(onClick={connectionAttempt++}) { Text("Retry connection") }
                TextButton(onClick=::backToLibrary) { Text("Back to Library") }
            }
        }
        return
    }

    if (controllerState == null) {
        // Splash with the title's backdrop so playback feels instant.
        val splashUrl = tmdbBackdropUrl(metadata?.backdropPath)
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (splashUrl != null) {
                coil.compose.AsyncImage(
                    model = splashUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                )
            }
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.material3.LoadingIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    metadata?.displayTitle ?: video.parsed.title.ifBlank { video.name },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }
        return
    }
    val controller: MediaController = controllerState ?: return
    fun requestIsCurrent(): Boolean = (activity !is PlayerActivity || activity.ownsPlaybackSession()) && currentPlaybackRequest(requestToken,latestRequest.value) && controller.isConnected && controller === controllerRef.value
    fun eventIsCurrent(): Boolean = requestIsCurrent() && samePlaybackItem(video.id,controller.currentMediaItem?.mediaId)

    // ------------------------- state mirrored from the player -------------------------
    // STABILITY: Fix loading animation persisting when quickly jumping videos
    // Key all player state to video.id so rapid switches don't retain old buffering
    var isPlaying by remember(video.id) { mutableStateOf(false) }
    var buffering by remember(video.id) { mutableStateOf(false) }
    // Reset all loading state immediately when video changes - prevents persistent spinner
    LaunchedEffect(video.id) {
        buffering = false
        isPlaying = false
    }
    var playbackEnded by remember(video.id) { mutableStateOf(false) }
    // Errors and progress warnings are scoped to the currently requested movie.
    var engineError by remember(video.id) { mutableStateOf<String?>(null) }
    var diagnosticErrorCode by remember(video.id) { mutableStateOf<Int?>(null) }
    var progressWarning by remember(video.id) { mutableStateOf(false) }
    var retryAttempt by remember(video.id) { mutableIntStateOf(0) }
    // STABILITY: When video changes quickly, reset all error/ended state immediately
    LaunchedEffect(video.id) {
        playbackEnded = false
        engineError = null
        diagnosticErrorCode = null
        progressWarning = false
        retryAttempt = 0
    }
    var showDiagnostics by remember { mutableStateOf(false) }
    var showSkipRanges by remember { mutableStateOf(false) }
    val extrasStore = remember(context) { com.opticast.player.data.local.LibraryExtrasStore(context) }
    var skipRevision by remember { mutableStateOf(0) }
    val skipProfile = remember(video.id,skipRevision) { extrasStore.profile(video.id) }
    var positionMs by remember(video.id) { mutableLongStateOf(
        AppContainer.playbackState.state(video.id)?.takeIf { it.isResumable }?.positionMs ?: 0L) }
    val durationMs = if (samePlaybackItem(video.id, controller.currentMediaItem?.mediaId))
        controller.duration.takeIf { it > 0L } ?: video.durationMs.coerceAtLeast(0L)
        else video.durationMs.coerceAtLeast(0L)

    var controlsVisible by remember { mutableStateOf(false) }
    var screenLocked by remember { mutableStateOf(false) }
    var unlockVisible by remember { mutableStateOf(false) }
    var aspectHud by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(aspectHud) { if (aspectHud != null) { delay(1500); aspectHud = null } }
    var unlockDragging by remember { mutableStateOf(false) }
    LaunchedEffect(screenLocked, unlockVisible, unlockDragging) {
        if (unlockAutoHideAllowed(screenLocked, unlockVisible, unlockDragging)) {
            delay(4000)
            unlockVisible = false
        }
    }

    // Key events only reach Compose when something inside it holds the focus -
    // on a TV that is what lets the remote drive the playing screen at all.
    val keyFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) {
        // Fails harmlessly when the window is not ready yet; a second attempt
        // after the first frame is enough for a remote to work.
        runCatching { keyFocus.requestFocus() }
    }
    val actionRailScroll = rememberScrollState()
    var lastInteractionMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showMovieBrowser by remember { mutableStateOf(false) }
    var showMoreControls by remember { mutableStateOf(false) }
    var deviceVideos by remember { mutableStateOf<List<LocalVideo>>(emptyList()) }
    var movieLoading by remember { mutableStateOf(true) }
    var movieLoadError by remember { mutableStateOf<String?>(null) }
    var movieRefresh by remember { mutableIntStateOf(0) }
    val currentCollectionEntry = LibraryEntry(video, metadata)
    val episodeCollection = isShowEntry(currentCollectionEntry)
    val movies = remember(deviceVideos, storeVersion, video, metadata) {
        playerCollection(deviceVideos.map { LibraryEntry(it, AppContainer.metadataStore.get(it.id)) }, currentCollectionEntry)
    }
    var scrubPosition by remember(video.id) { mutableStateOf<Float?>(null) }
    val lastScrubSeekAt = remember { longArrayOf(0L) }
    fun liveSeek(target: Long) {
        OptiCastPlaybackService.setFastSeeking(true)
        val now = android.os.SystemClock.elapsedRealtime()
        if (liveSeekDue(now, lastScrubSeekAt[0])) {
            lastScrubSeekAt[0] = now
            controller.seekTo(target)
        }
    }
    fun selectMovie(entry: LibraryEntry) {
        if(!requestIsCurrent()) return
        showMovieBrowser=false
        if(entry.video.id==video.id) {
            // A queued tap can reselect this movie before an earlier route change recomposes.
            if(activity is PlayerActivity) activity.selectLocalMovie(entry.video.id) else activeVideoId=entry.video.id
            return
        }
        // Route immediately. The keyed resolver validates identity once before any preparation.
        controller.pause()
        controlsVisible=false
        if(activity is PlayerActivity) activity.selectLocalMovie(entry.video.id)
        else activeVideoId=entry.video.id
    }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    // Sleep timer: either a wall-clock moment, or "when this video ends".
    var sleepUntilMs by remember { mutableStateOf<Long?>(null) }
    var sleepAtEnd by remember { mutableStateOf(false) }
    var sleepLabel by remember { mutableStateOf<String?>(null) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var secondarySubId by remember(activeVideoId) {
        mutableStateOf(AppContainer.metadataStore.secondarySubtitle(video.id))
    }
    var secondaryCues by remember(activeVideoId, secondarySubId) {
        mutableStateOf<List<SubtitleCue>>(emptyList())
    }
    var chapters by remember(activeVideoId) { mutableStateOf<List<Chapter>>(emptyList()) }
    var thumbsReady by remember(activeVideoId) { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var rotationMode by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(if (AppContainer.initialSettings.autoLandscape) 1 else 2) }
    // --- Pinch-to-zoom & pan state (GPU-composited, pleasant and easy) ---
    LaunchedEffect(controller, resizeMode, appSettings.captionStyle, appSettings.captionScale) {
        OptiCastPlaybackService.setNativePresentation(NativePresentation(
            aggressiveVideoScale(resizeMode), appSettings.captionStyle, appSettings.captionScale))
    }
    var zoomScale by remember { mutableStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomHudScale by remember { mutableStateOf<Float?>(null) }
    var seekTranslateX by remember { mutableStateOf(0f) }
    LaunchedEffect(video.id) { zoomScale = 1f; zoomOffset = Offset.Zero; zoomHudScale = null }

    // Smooth spring-back when finger lifts after horizontal scrub
    LaunchedEffect(zoomHudScale) {
        if (zoomHudScale != null) {
            delay(900)
            zoomHudScale = null
        }
    }

    // Applies the user's "preserve pitch" preference to any speed change.
    fun speedParams(speed: Float): PlaybackParameters =
        if (appSettings.preservePitch) PlaybackParameters(speed)
        else PlaybackParameters(speed, speed)

    // Sleep timer (0 = off) and hold-to-fast-forward.
    var sleepEndMs by remember { mutableLongStateOf(0L) }
    var sleepSetMinutes by remember { mutableIntStateOf(0) }
    var sleepEndedFlash by remember { mutableStateOf(false) }
    var holdSpeedActive by remember { mutableStateOf(false) }

    // In-player subtitle search.
    var subSearching by remember { mutableStateOf(false) }
    var subResults by remember { mutableStateOf<List<SubtitleResult>?>(null) }
    var subMessage by remember { mutableStateOf<String?>(null) }

    var subtitleOwner by androidx.compose.runtime.saveable.rememberSaveable { mutableLongStateOf(0L) }
    val subtitlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        (activity as? PlayerActivity)?.subtitlePickerActive = false
        val owner = subtitleOwner
        subtitleOwner = 0L
        if (uri != null && owner != 0L) scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    } ?: uri.lastPathSegment.orEmpty()
                    val extension = subtitleFileExtension(name)
                        ?: error("Choose an SRT, ASS, SSA or VTT subtitle file.")
                    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                        val buffer = java.io.ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        while (true) {
                            val n = stream.read(chunk)
                            if (n < 0) break
                            check(buffer.size() + n <= 4 * 1024 * 1024) { "Subtitle is too large (maximum 4 MB)." }
                            buffer.write(chunk, 0, n)
                        }
                        buffer.toByteArray()
                    } ?: error("Cannot read the selected subtitle.")
                    check(bytes.isNotEmpty()) { "The selected subtitle is empty." }
                    val normalized = when {
                        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                            String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE).toByteArray(Charsets.UTF_8)
                        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                            String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE).toByteArray(Charsets.UTF_8)
                        else -> bytes
                    }
                    check(normalized.none { it == 0.toByte() }) { "Choose a text subtitle file, not a binary file." }
                    AppContainer.metadataStore.saveSubtitle(owner, "und", name, normalized, source = "local", extension = extension)
                }
            }
            subMessage = result.fold({ "Added ${it.releaseName}. Select its track below; playback remains paused." },
                { it.message ?: "Could not import subtitle." })
        }
    }

    val trackOptions = remember(controller, video.id) { mutableStateListOf<TrackOption>() }
    var trackPreferenceRevision by remember(video.id) { mutableIntStateOf(0) }
    fun trackKey(option: TrackOption): String {
        val format = option.group.getTrackFormat(option.index)
        // Include track identity to disambiguate same-language/different mixes. On engine changes a
        // missing descriptor intentionally falls back to global rules rather than guessing a track.
        return "${selectedEngine}|${option.group.mediaTrackGroup.id}|${option.index}|${format.language}|${format.label}|${format.selectionFlags}"
    }
    fun rememberTrack(option: TrackOption) {
        preferenceKey?.let { videoPreferences.saveTrack(it, option.type, trackKey(option)) }
    }
    var selectedTextOption by remember { mutableStateOf<Int?>(null) }
    var selectedAudioOption by remember { mutableStateOf<Int?>(null) }
    var textDisabled by remember { mutableStateOf(false) }

    // Gesture HUD state
    var seekHudMs by remember { mutableStateOf<Long?>(null) }
    var volumeHud by remember { mutableStateOf<Float?>(null) }
    var brightnessHud by remember { mutableStateOf<Float?>(null) }
    var tapFlash by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val tapState = remember { TapState() }

    // "Up next" state
    var nextEpisode by remember { mutableStateOf<NextEpisode?>(null) }
    var countdown by remember { mutableIntStateOf(5) }
    var nextCancelled by remember { mutableStateOf(false) }

    fun poke() {
        lastInteractionMs = System.currentTimeMillis()
        controlsVisible = true
    }

    fun switchToNext() {
        val next = nextEpisode ?: return
        if(!requestIsCurrent()) return
        controller.pause()
        nextCancelled = false
        if (activity is PlayerActivity) activity.selectLocalMovie(next.video.id)
        else activeVideoId = next.video.id
        lastInteractionMs = System.currentTimeMillis()
        controlsVisible = false
    }

    // Subtitle sync offset (persisted per video, shifts cue timestamps).
    // Chapters come straight out of the file (Media3 does not surface them).
    // Parsed off the main thread, cached on disk, empty for files without them.
    LaunchedEffect(activeVideoId, video?.uri) {
        val v = video ?: return@LaunchedEffect
        androidx.compose.runtime.snapshotFlow { (isPlaying && samePlaybackItem(video.id,controller.currentMediaItem?.mediaId)) || showMoreControls || showChaptersSheet }.first { it }
        chapters = AppContainer.chapters.forVideo(v.id, v.uri)
    }

    var offsetMs by remember(video.id,video.uri) {
        mutableLongStateOf(AppContainer.metadataStore.subtitleOffset(video.id))
    }

    fun applyOffset(newOffsetMs: Long) {
        if(!eventIsCurrent()) return
        val clamped=newOffsetMs.coerceIn(-3_600_000L,3_600_000L)
        AppContainer.metadataStore.setSubtitleOffset(video.id,clamped)
        offsetMs=clamped
        // The one keyed preparation effect below owns subtitle reloads, too.
        // No independent coroutine may later restore an outgoing movie.
        poke()
    }

    // Auto-align: a conservative estimate of how far the subtitle file is out of
    // step, measured against the video's own length (see SubtitleSync). Runs off
    // the main thread because it reads and parses the subtitle file.
    var syncNote by remember(activeVideoId) { mutableStateOf<String?>(null) }
    var syncBusy by remember(activeVideoId) { mutableStateOf(false) }
    val autoAlign: () -> Unit = {
        val candidate = savedSubtitles.firstOrNull()
        when {
            candidate == null -> syncNote =
                "Load a subtitle for this video first - auto-align measures an " +
                    "existing subtitle file against the video."
            syncBusy -> Unit
            else -> {
                syncBusy = true
                scope.launch {
                    val cues = withContext(Dispatchers.IO) {
                        runCatching {
                            SubtitleCues.parse(File(candidate.filePath).readText())
                        }.getOrDefault(emptyList())
                    }
                    val duration = controller.duration
                    val estimate = if (duration > 0L) {
                        SubtitleSync.estimate(cues, duration)
                    } else {
                        null
                    }
                    when {
                        estimate == null -> syncNote =
                            "The subtitle file could not be read, or the video length is " +
                                "not known yet. Play a moment, then try again."
                        estimate.quality == SubtitleSync.Quality.FITS -> {
                            if (offsetMs != 0L) applyOffset(0L)
                            syncNote = estimate.note
                        }
                        estimate.offsetMs == 0L -> syncNote = estimate.note
                        else -> {
                            applyOffset(estimate.offsetMs)
                            syncNote = estimate.note
                        }
                    }
                    syncBusy = false
                }
            }
        }
    }

    // Dual subtitles: the second track is parsed here and drawn as an overlay, so
    // Media3's own subtitle rendering stays untouched. The per-video offset is
    // applied to it exactly like the primary track.
    LaunchedEffect(activeVideoId, secondarySubId, savedSubtitles, offsetMs) {
        val chosen = savedSubtitles.firstOrNull { it.id == secondarySubId }
        secondaryCues = if (chosen == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val raw = File(chosen.filePath).readText()
                    SubtitleCues.parse(SrtShifter.shift(raw, offsetMs))
                }.getOrDefault(emptyList())
            }
        }
    }

    // Sleep timer: pauses playback when the timer elapses.
    LaunchedEffect(sleepEndMs) {
        val end = sleepEndMs
        if (end > 0L) {
            val remaining = end - System.currentTimeMillis()
            if (remaining > 0L) delay(remaining)
            controller.pause()
            sleepEndMs = 0L
            sleepSetMinutes = 0
            sleepEndedFlash = true
        }
    }
    LaunchedEffect(sleepEndedFlash) {
        if (sleepEndedFlash) {
            delay(3500)
            sleepEndedFlash = false
        }
    }

    fun searchPlayerSubtitles() {
        if (subSearching) return
        subSearching = true
        subMessage = null
        subResults = null
        scope.launch {
            runCatching {
                val settings = AppContainer.settings.current()
                AppContainer.subtitles.search(
                    metadata = metadata,
                    fallbackQuery = metadata?.displayTitle
                        ?: video.parsed.title.ifBlank { video.name },
                    languages = settings.subtitleLanguages,
                    season = video.parsed.season ?: metadata?.seasonNumber,
                    episode = video.parsed.episode ?: metadata?.episodeNumber,
                )
            }.onSuccess { results ->
                subResults = results
                subSearching = false
                if (results.isEmpty()) subMessage = "No subtitles found."
            }.onFailure { e ->
                subSearching = false
                subMessage = e.message ?: "Search failed."
            }
        }
    }

    fun downloadPlayerSubtitle(result: SubtitleResult) {
        scope.launch {
            runCatching { AppContainer.subtitles.download(result, video.id) }
                .onSuccess {
                    subResults = null
                    subMessage = "Saved. Subtitles are reloading."
                }
                .onFailure { e -> subMessage = e.message ?: "Download failed." }
        }
    }

    // ------------------------------ player lifecycle ------------------------------
    DisposableEffect(controller, video.id) {
        OptiCastPlaybackService.setFastSeeking(false)
        PiPController.isPlayerActive = true
        PiPController.isPlaying = { controller.isPlaying }
        // Seed PiP aspect from current video size so first enter has correct ratio.
        runCatching {
            val vs = controller.videoSize
            if (vs.width > 0 && vs.height > 0) {
                PiPController.videoWidth = vs.width
                PiPController.videoHeight = vs.height
            } else if (video.width > 0 && video.height > 0) {
                PiPController.videoWidth = video.width
                PiPController.videoHeight = video.height
            }
        }
        PiPController.onEnterPip = { controlsVisible = false; unlockVisible = false; aspectHud = null; zoomHudScale = null; showTracksSheet = false; showAudioSheet = false; showSpeedSheet = false; showChaptersSheet = false; showSleepSheet = false; showInfoSheet = false; showMovieBrowser = false; showMoreControls = false; showSkipRanges = false; showDiagnostics = false }
        PiPController.onBackground = {
            controller.pause()
            OptiCastPlaybackService.setFastSeeking(false)
            scrubPosition?.let { controller.seekTo(it.toLong()) }
            scrubPosition = null
            tapState.singleTapJob?.cancel()
            controlsVisible = false; unlockVisible = false
            showTracksSheet = false; showAudioSheet = false; showSpeedSheet = false
            showChaptersSheet = false; showSleepSheet = false; showInfoSheet = false
            showMovieBrowser = false; showMoreControls = false; showSkipRanges = false; showDiagnostics = false
            seekHudMs = null; volumeHud = null; brightnessHud = null
        }
        // Fix for 32-bit: when expanding PiP, automatically resume if it was playing before
        PiPController.onReturnFromPip = {
            if (PiPController.wasPlayingBeforePip) {
                controller.play()
                controlsVisible = false
            }
            PiPController.wasPlayingBeforePip = false
        }
        // The floating window's transport buttons drive the same controller.
        PiPController.onTogglePlay = {
            if (controller.isPlaying) controller.pause() else controller.play()
        }
        PiPController.onSeekBy = { delta ->
            val ceiling = controller.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
            controller.seekTo((controller.currentPosition + delta).coerceIn(0L, ceiling))
        }
        // Called when the player really closes: persist the exact position
        // synchronously so it is never lost (user requested: always remember).
        PiPController.onPlayerClosing = {
            OptiCastPlaybackService.stopPlayback(context)
        }

        val listener = object : Player.Listener {
            // Errors belong to this media identity; recovery is explicit, never a retry loop.
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if(!eventIsCurrent()) return
                android.util.Log.e("OptiCast", "${if(selectedEngine == "mpv") "mpv" else "Media3"} error: ${error.errorCodeName} (${error.errorCode})")
                progressWarning=false
                diagnosticErrorCode = error.errorCode
                preferenceKey?.let(videoPreferences::forgetEngine)
                if (shouldFallbackToMedia3(selectedEngine, fallbackTried)) {
                    fallbackTried = true
                    controller.pause()
                    engineOverride = "media3"
                    engineError = null
                    return
                }
                engineError = if(selectedEngine == "mpv") error.message ?: "mpv playback failed" else "Playback failed: ${error.errorCodeName}"
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                if(!eventIsCurrent()) return
                isPlaying = playing
                if (playing) controlsVisible = false
                // Keeps the floating window's pause/play icon honest.
                PiPController.onPlayingChanged?.invoke(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if(!eventIsCurrent()) return
                if (selectedEngine != "mpv" && playbackState == Player.STATE_ENDED &&
                    endedUnexpectedly(controller.currentPosition, controller.duration)) {
                    onPlayerError(androidx.media3.common.PlaybackException("Playback ended before the expected end", null,
                        androidx.media3.common.PlaybackException.ERROR_CODE_UNSPECIFIED))
                    return
                }
                if (!eventIsCurrent()) return
                buffering = playbackState == Player.STATE_BUFFERING && samePlaybackItem(video.id, controller.currentMediaItem?.mediaId)
                playbackEnded = playbackState == Player.STATE_ENDED && samePlaybackItem(video.id, controller.currentMediaItem?.mediaId)
                if (playbackState == Player.STATE_ENDED && sleepAtEnd) {
                    sleepAtEnd = false
                    sleepLabel = "Sleep timer: finished this video"
                }

            }

            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if(!eventIsCurrent()) return
                if (videoSize.width > 0 && videoSize.height > 0) {
                    PiPController.videoWidth = videoSize.width
                    PiPController.videoHeight = videoSize.height
                    PiPController.onPipAspectChanged?.invoke()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                if(!eventIsCurrent()) return
                trackOptions.clear()
                tracks.groups.forEach { group ->
                    if (group.type != C.TRACK_TYPE_TEXT && group.type != C.TRACK_TYPE_AUDIO) return@forEach
                    for (i in 0 until group.length) {
                        if (!group.isTrackSupported(i)) continue
                        val format = group.getTrackFormat(i)
                        val label = when (group.type) {
                            C.TRACK_TYPE_TEXT -> format.label
                                ?: format.language?.uppercase()
                                ?: "Subtitle"
                            else -> format.label
                                ?: format.language?.uppercase()
                                ?: "Audio"
                        }
                        trackOptions += TrackOption(group.type, group, i, label)
                    }
                }
            }
        }
        controller.addListener(listener)
        if (eventIsCurrent()) listener.onTracksChanged(controller.currentTracks)
        // A newly connected controller can already contain an error without replaying it.
        controller.playerError?.let { listener.onPlayerError(it) }
        isPlaying = eventIsCurrent() && controller.isPlaying
        buffering = eventIsCurrent() && controller.playbackState == Player.STATE_BUFFERING
        playbackEnded = eventIsCurrent() && controller.playbackState == Player.STATE_ENDED
        onDispose {
            controller.removeListener(listener)
            if (activity is PlayerActivity && !activity.ownsPlaybackSession()) return@onDispose
            PiPController.isPlayerActive=false
            PiPController.isPlaying={false}
            PiPController.wasPlayingBeforePip=false
            PiPController.onTogglePlay=null
            PiPController.onSeekBy=null
            PiPController.onEnterPip=null
            PiPController.onBackground=null
            PiPController.onReturnFromPip=null
            PiPController.onPlayerClosing=null
        }
    }

    // Immersive mode + keep the screen awake while playing.
    DisposableEffect(activity) {
        val window = activity?.window
        if (AppContainer.initialSettings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val insetsController = window?.let { WindowInsetsControllerCompat(it, window.decorView) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            // Do not flash system bars or rotate the outgoing player during exit.
            if (activity?.isFinishing != true && activity?.isChangingConfigurations != true) {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Sleep timer: pause when the clock runs out.
    LaunchedEffect(sleepUntilMs, sleepAtEnd) {
        val until = sleepUntilMs
        if (until == null) return@LaunchedEffect
        while (true) {
            val left = until - System.currentTimeMillis()
            if (left <= 0L) {
                runCatching { controller.pause() }
                sleepUntilMs = null
                sleepAtEnd = false
                sleepLabel = "Sleep timer: paused"
                controlsVisible = false
                return@LaunchedEffect
            }
            kotlinx.coroutines.delay(1_000L)
        }
    }

    // One cancellable preparation owner for route changes, subtitles and subtitle offsets.
    var appliedSubtitleKey by remember(controller) { mutableStateOf<String?>(null) }
    LaunchedEffect(controller, requestToken, video.uri, savedSubtitles, offsetMs) {
        controlsVisible=false
        if(mediaIdRef.get()!=video.id) {
            pendingResumeRef.set(0L)
            pendingResumeDeadlineRef.set(0L)
        }
        val playableSubtitles=if(savedSubtitles.isEmpty()) emptyList() else withContext(Dispatchers.IO) {
            savedSubtitles.filter { usableSubtitleFile(File(it.filePath)) }
        }
        val configs=if(playableSubtitles.isEmpty()) emptyList() else buildSubtitleConfigs(context,video.id,playableSubtitles,offsetMs)
        currentCoroutineContext().ensureActive()
        if(!requestIsCurrent()) return@LaunchedEffect
        // Read live identity/position only AFTER all suspending preparation has completed.
        val targetUri=Uri.parse(video.uri)
        val currentConfig=controller.currentMediaItem?.localConfiguration
        val sameItem=samePlaybackItem(video.id,controller.currentMediaItem?.mediaId) && (currentConfig==null || currentConfig.uri==targetUri)
        val subtitleKey="${video.id}:$offsetMs:$playableSubtitles"
        val changedSubtitles=subtitleReloadNeeded(currentConfig?.subtitleConfigurations?.size,playableSubtitles.size,appliedSubtitleKey,subtitleKey)
        if(!sameItem || changedSubtitles) {
            val item=MediaItem.Builder().setMediaId(video.id.toString()).setUri(targetUri)
                .apply { mimeForName(video.name)?.let { setMimeType(it) } }.setSubtitleConfigurations(configs).build()
            val position=playbackStartPosition(sameItem,controller.currentPosition,
                AppContainer.playbackState.state(video.id)?.takeIf { it.isResumable }?.positionMs)
            val play=if(sameItem) controller.playWhenReady else true
            // No suspension between this final fence and submitting the complete request.
            if(!requestIsCurrent()) return@LaunchedEffect
            ResumeProbes.forId(video.id.toString())?.event(ProbeStage.SUBMITTED,android.os.SystemClock.elapsedRealtime(),position ?: -1L)
            PlayerStats.markStartup(video.id.toString(),StartupStage.PREPARED)
            if (!sameItem) {
                trackOptions.clear()
                controller.trackSelectionParameters = controller.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO).clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
            }
            if(position!=null) controller.setMediaItem(item,position) else controller.setMediaItem(item)
            mediaIdRef.set(video.id)
            pendingResumeRef.set(if(!sameItem) position ?: 0L else 0L)
            pendingResumeDeadlineRef.set(android.os.SystemClock.elapsedRealtime()+12_000L)
            controller.playWhenReady=play
            controller.prepare()
            if(play) controller.play()
        } else {
            mediaIdRef.set(video.id)
            // Metadata changes must not implicitly retry a failed item or unpause playback.
            if(controller.playbackState==Player.STATE_IDLE && controller.playerError==null && controller.playWhenReady) controller.prepare()
        }
        appliedSubtitleKey=subtitleKey
    }

    // A progress warning, never an automatic restart. Pauses/ended/ready are not stalls.
    LaunchedEffect(controller,requestToken,retryAttempt) {
        val watch=PlaybackProgressWatch()
        while(true) {
            val current=eventIsCurrent()
            val waiting=current && controller.playWhenReady && controller.playerError==null &&
                controller.playbackState in listOf(Player.STATE_IDLE,Player.STATE_BUFFERING)
            val now=android.os.SystemClock.elapsedRealtime()
            if(watch.observe(now,waiting,controller.currentPosition,controller.bufferedPosition) && engineError==null) {
                progressWarning=true
                engineError="Playback has made no reported position or buffer progress for 30 seconds. You can wait, retry, or return to the Library."
            }
            if(progressWarning && current && controller.playbackState==Player.STATE_READY) {
                progressWarning=false
                engineError=null
            }
            delay(1000)
        }
    }

    // Resolve the next episode of the current show.
    LaunchedEffect(activeVideoId, metadata, movieRefresh) {
        movieLoading = true
        val cached=withContext(Dispatchers.IO) { runCatching { AppContainer.mediaScanner.inventory.available() } }
        val result=if(needsPlayerCollectionScan(cached.getOrDefault(emptyList()).size,movieRefresh>0)) {
            if(movieRefresh==0) androidx.compose.runtime.snapshotFlow { (isPlaying && samePlaybackItem(video.id,controller.currentMediaItem?.mediaId)) || showMovieBrowser || playbackEnded }.first { it }
            withContext(Dispatchers.IO) { runCatching { AppContainer.mediaScanner.scan() } }
        } else cached
        val all = result.getOrDefault(emptyList())
        deviceVideos = all
        movieLoadError = if (result.isFailure) "Cannot read the local library. Check video access in Android settings." else null
        movieLoading = false
        nextEpisode = withContext(Dispatchers.IO) { NextEpisodeResolver.find(video, metadata, all) }
    }

    // Reset per-request UI state, including remote-to-remote switches.
    LaunchedEffect(requestToken) {
        playbackEnded = false
        nextCancelled = false
        selectedTextOption = null
        selectedAudioOption = null
        textDisabled = false
    }

    // Hide-only throttling: subtitle/skip timing and startup resume correction keep the original cadence.
    val fastPositionUpdates = controlsVisible || showInfoSheet || showChaptersSheet || showSkipRanges ||
        scrubPosition != null || seekHudMs != null
    val timedPositionOverlay = secondaryCues.isNotEmpty() ||
        (skipProfile.enabled && (skipProfile.intro != null || skipProfile.credits != null))
    // Persistence and native decoding remain service-owned and unchanged.
    LaunchedEffect(controller, video.id, fastPositionUpdates, timedPositionOverlay) {
        while (true) {
            if (samePlaybackItem(video.id, controller.currentMediaItem?.mediaId)) {
                positionMs = controller.currentPosition.coerceAtLeast(0L)
                val pending = pendingResumeRef.get()
                if (mediaIdRef.get() == video.id && pending > 0L && controller.duration > 0L) {
                    if (android.os.SystemClock.elapsedRealtime() < pendingResumeDeadlineRef.get() &&
                        controller.currentPosition < pending - 3_000L) {
                        ResumeProbes.forId(video.id.toString())?.event(ProbeStage.RESUME_CORRECTION,android.os.SystemClock.elapsedRealtime(),pending)
                        controller.seekTo(pending)
                    }
                    pendingResumeRef.set(0L)
                }
            }
            delay(positionRefreshDelayMs(fastPositionUpdates, timedPositionOverlay,
                pendingResumeRef.get() > 0L || !eventIsCurrent() || controller.playbackState != Player.STATE_READY))
        }
    }

    // Keep-screen-on follows the setting live.
    LaunchedEffect(appSettings.keepScreenOn) {
        val window = activity?.window ?: return@LaunchedEffect
        if (appSettings.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-hide controls while playing.
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) return@LaunchedEffect
        while (true) {
            delay(500)
            if (actionRailScroll.isScrollInProgress) lastInteractionMs = System.currentTimeMillis()
            if (controller.isPlaying && !showTracksSheet && !showAudioSheet && !showSpeedSheet &&
                !showChaptersSheet && !showSleepSheet && !showInfoSheet && !showMovieBrowser && !showMoreControls && !showSkipRanges && !showDiagnostics &&
                System.currentTimeMillis() - lastInteractionMs > 3500
            ) {
                controlsVisible = false
                break
            }
            if (!controller.isPlaying) break
        }
    }

    // Fade out gesture HUDs shortly after the last change.
    LaunchedEffect(seekHudMs, volumeHud, brightnessHud, tapFlash) {
        delay(900)
        seekHudMs = null
        volumeHud = null
        brightnessHud = null
        tapFlash = null
    }

    // "Up next" countdown.
    LaunchedEffect(playbackEnded, nextEpisode, nextCancelled) {
        if (!playbackEnded || nextEpisode == null || nextCancelled ||
            !appSettings.autoNextEpisode
        ) {
            return@LaunchedEffect
        }
        countdown = 5
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        switchToNext()
    }

    // Fix: Swiping back from now playing screen doesn't work
    if (android.os.Build.VERSION.SDK_INT >= 34) {
        androidx.activity.compose.PredictiveBackHandler(enabled = true) { progress ->
            try {
                progress.collect { backEvent ->
                    val _progressValue = backEvent.progress
                }
            } catch (_: Exception) {
                return@PredictiveBackHandler
            }
            if (screenLocked) unlockVisible = true else (onSystemBack ?: onBack)()
        }
    } else {
        BackHandler { if (screenLocked) unlockVisible = true else (onSystemBack ?: onBack)() }
    }

    // ----------------------------------- titles -----------------------------------
    val titleLine = when {
        metadata == null -> video.parsed.title.ifBlank { video.name }
        metadata.type == "tv" -> buildString {
            append(metadata.showTitle ?: metadata.title)
            val s = metadata.seasonNumber
            val e = metadata.episodeNumber
            if (s != null && e != null) append(" · S%02dE%02d".format(s, e))
        }
        else -> metadata.title
    }
    val subtitleLine = when {
        metadata == null -> ""
        metadata.type == "tv" -> metadata.episodeName.orEmpty()
        else -> metadata.year?.toString() ?: ""
    }

    val textOptions = trackOptions.filter { it.type == C.TRACK_TYPE_TEXT }
    val audioOptions = trackOptions.filter { it.type == C.TRACK_TYPE_AUDIO }

    val trackInventoryKey = trackOptions.map(::trackKey)
    LaunchedEffect(controller, video.id, trackInventoryKey, trackPreferenceRevision,
        appSettings.preferredAudioLanguage, appSettings.embeddedSubtitleLanguage,
        appSettings.embeddedSubtitleMode, appSettings.avoidCommentary) {
        if (!eventIsCurrent() || trackOptions.isEmpty()) return@LaunchedEffect
        fun descriptor(option: TrackOption): PreferenceTrack {
            val f = option.group.getTrackFormat(option.index)
            return PreferenceTrack(trackKey(option), f.language, f.label,
                f.selectionFlags and C.SELECTION_FLAG_FORCED != 0,
                f.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0,
                f.roleFlags and C.ROLE_FLAG_COMMENTARY != 0)
        }
        val audio = preferredAudioTrack(audioOptions.map(::descriptor), appSettings.preferredAudioLanguage,
            appSettings.avoidCommentary, preferenceKey?.let { videoPreferences.track(it, C.TRACK_TYPE_AUDIO) })
        val text = preferredSubtitleTrack(textOptions.map(::descriptor), appSettings.embeddedSubtitleLanguage,
            appSettings.embeddedSubtitleMode, preferenceKey?.let { videoPreferences.track(it, C.TRACK_TYPE_TEXT) })
        val params = controller.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO).clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, text == TRACK_OFF)
        audioOptions.firstOrNull { trackKey(it) == audio?.key }?.let {
            params.setOverrideForType(TrackSelectionOverride(it.group.mediaTrackGroup, it.index))
        }
        textOptions.firstOrNull { trackKey(it) == text }?.let {
            params.setOverrideForType(TrackSelectionOverride(it.group.mediaTrackGroup, it.index))
        }
        controller.trackSelectionParameters = params.build()
        selectedAudioOption = audioOptions.indexOfFirst { trackKey(it) == audio?.key }.takeIf { it >= 0 }
        selectedTextOption = textOptions.indexOfFirst { trackKey(it) == text }.takeIf { it >= 0 }
        textDisabled = text == TRACK_OFF
    }
    LaunchedEffect(controller, video.id, appSettings.engineMemoryEnabled) {
        if (!appSettings.engineMemoryEnabled || preferenceKey == null || Uri.parse(video.uri).scheme !in listOf(null, "file", "content")) return@LaunchedEffect
        val success = EngineSuccessWindow()
        var recorded = false
        while (true) {
            delay(1000)
            if (!eventIsCurrent()) continue
            if (controller.playerError != null || engineError != null) {
                videoPreferences.forgetEngine(preferenceKey)
                recorded = false
                success.observe(controller.currentPosition, false)
            } else if (success.observe(controller.currentPosition, controller.isPlaying) && !recorded) {
                videoPreferences.saveEngine(preferenceKey, selectedEngine)
                recorded = true
            }
        }
    }

    // ----------------------------------- layout -----------------------------------
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(keyFocus)
            .focusable()
            // ------------------------- remote / D-pad keys -------------------------
            // TVs and boxes have no touchscreen: without this the playing screen
            // was unresponsive to a Fire TV / Android TV remote. Preview phase, so
            // the keys are seen before any child. With the controls hidden the
            // arrows only wake them; once they are up, left/right seek (the
            // expected TV behaviour) and everything else falls through to normal
            // focus navigation of the on-screen buttons.
            .onPreviewKeyEvent { event ->
                if (screenLocked) { unlockVisible = true; return@onPreviewKeyEvent true }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar, Key.MediaPlayPause -> {
                        if (controlsVisible) return@onPreviewKeyEvent false
                        controller.playWhenReady = !controller.playWhenReady
                        poke()
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionLeft -> {
                        if (controlsVisible) {
                            controller.seekTo(
                                (controller.currentPosition - 10_000L).coerceAtLeast(0L),
                            )
                        }
                        poke()
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionRight -> {
                        if (controlsVisible) {
                            val ceiling = controller.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            controller.seekTo(
                                (controller.currentPosition + 10_000L).coerceAtMost(ceiling),
                            )
                        }
                        poke()
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionUp, Key.DirectionDown, Key.MediaPlay, Key.MediaNext,
                    Key.MediaPrevious, Key.Menu,
                    -> {
                        poke()
                        return@onPreviewKeyEvent true
                    }

                    else -> return@onPreviewKeyEvent false
                }
            },
    ) {
        AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = zoomOffset.x + seekTranslateX,
                        translationY = zoomOffset.y,
                    ),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(Color.Black.toArgb())
                    }
                },
                onReset = null,
                onRelease = { view -> view.player = null },
                update = { view ->
                    view.player = controller
                    view.useController = false
                    view.resizeMode = surfaceResizeMode(resizeMode)
                    val surfaceScale = if(selectedEngine == "mpv") 1f else aggressiveVideoScale(resizeMode)
                    view.videoSurfaceView?.apply { scaleX = surfaceScale; scaleY = surfaceScale }
                    view.subtitleView?.apply {
                        setStyle(captionStyleFor(appSettings.captionStyle))
                        setFractionalTextSize(
                            SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * appSettings.captionScale
                        )
                        setApplyEmbeddedStyles(false)
                    }
                },
            )

        // --------------------- engine-failure banner (Media3 only) ----------------------
        // A separate dialog window owns error input, above video, gestures and screen lock.
        engineError?.let { message ->
            if (!showDiagnostics) androidx.compose.ui.window.Dialog(
                onDismissRequest = { if(progressWarning) engineError = null else backToLibrary() },
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                ) {
                    Column(
                        Modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.8f).dp)
                            .verticalScroll(rememberScrollState()).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            video.name,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if(progressWarning) "This warning does not prove the file or decoder is unsupported. Playback has not been stopped automatically." else com.opticast.player.data.local.diagnosticAdvice(diagnosticErrorCode),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        if (selectedEngine == "mpv") TextButton(onClick = {
                            controller.pause(); engineError = null; engineOverride = "media3"
                        }) { Text("Try Media3 for this video") }
                        else {
                            TextButton(onClick={showDiagnostics=true}) { Text("Playback diagnostics") }
                            if (android.net.Uri.parse(video.uri).scheme in setOf(null, "content", "file")) {
                                TextButton(onClick = { controller.pause(); engineError = null; engineOverride = "mpv" }) {
                                    Text("Try mpv for this video")
                                }
                            }
                        }
                        TextButton(onClick={
                            runCatching {
                                if(!controller.isConnected || !samePlaybackItem(video.id,controller.currentMediaItem?.mediaId)) error("Player not connected")
                                if(!requestIsCurrent()) error("Request superseded")
                                // stop() retains the item and position; restart only at the user's request.
                                controller.stop()
                                controller.prepare();controller.play()
                                progressWarning=false;engineError=null;retryAttempt++
                            }.onFailure { engineError="Could not retry playback. Return to the Library and reopen this video." }
                        }) { Text("Retry playback") }
                        Spacer(Modifier.height(14.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { if(progressWarning) { engineError = null; progressWarning=false } else backToLibrary() }) { Text(if(progressWarning) "Keep waiting" else "Back to Library") }
                            FilledTonalButton(onClick = {
                                runCatching {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(android.net.Uri.parse(video.uri), "video/*")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                    engineError = null
                                }.onFailure { engineError = "Could not open an external player. Install or enable a video player, then try again." }
                            }) { Text("Open in External Player") }
                        }
                    }
                }
            }
        }
        }

        // ------------------------------ gesture layer — pleasant & easy ------------------------------
        // Pinch-to-zoom (0.5x–2x, two-finger pan when zoomed), horizontal swipe to scrub with video following finger,
        // vertical swipe left/right for brightness/volume, double-tap left/right to seek, long-press for 2x.
        val haptic = LocalHapticFeedback.current
        // Pinch-to-zoom is handled as a separate lightweight gesture so it does not fight the scrub gesture.
        // When zoomed, two-finger pan moves the zoomed video. Double-tap center resets zoom.
        if (playerGestureInputEnabled(screenLocked,engineError!=null,showDiagnostics)) Box(
            Modifier
                .fillMaxSize()
                .pointerInput(video.id) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var pinching = false
                        do {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            if (event.changes.count { it.pressed } >= 2) {
                                pinching = true
                                val newScale = clampedVideoScale(zoomScale * event.calculateZoom())
                                val pan = event.calculatePan()
                                zoomScale = newScale
                                zoomHudScale = newScale
                                if (newScale <= 1f) zoomOffset = Offset.Zero
                                else {
                                    val maxX = size.width * (newScale - 1f) / 2f
                                    val maxY = size.height * (newScale - 1f) / 2f
                                    zoomOffset = Offset((zoomOffset.x + pan.x).coerceIn(-maxX, maxX),
                                        (zoomOffset.y + pan.y).coerceIn(-maxY, maxY))
                                }
                                event.changes.forEach { it.consume() }
                            } else if (pinching) event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(controller) {
                    val audioManager =
                        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume =
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var totalDrag = Offset.Zero
                        var mode = 0 // 0 none, 1 seek (horizontal), 2 volume (right vertical), 3 brightness (left vertical)
                        var holdTriggered = false
                        var preHoldSpeed = 1f
                        val holdJob = scope.launch {
                            delay(550)
                            if (mode == 0 && !holdTriggered &&
                                appSettings.holdToSpeed &&
                                totalDrag.getDistance() < viewConfiguration.touchSlop * 2.5f &&
                                System.currentTimeMillis() - tapState.lastTapMs > 380L
                            ) {
                                holdTriggered = true
                                preHoldSpeed = controller.playbackParameters.speed
                                controller.playbackParameters =
                                    speedParams(appSettings.holdSpeedFactor)
                                holdSpeedActive = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                        var basePositionMs = 0L
                        var baseVolumeFraction = 0f
                        var baseBrightness = 0.5f
                        var pointerUp = false
                        var lastHapticStep = 0L

                        while (!pointerUp) {
                            val event = awaitPointerEvent()
                            // If a second finger appears mid-gesture, hand off to pinch
                            if (event.changes.size >= 2) {
                                holdJob.cancel()
                                // Reset seek translation so video does not stay offset
                                seekTranslateX = 0f
                                mode = 4
                                break
                            }
                            val change: PointerInputChange = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                pointerUp = true
                                break
                            }
                            totalDrag += change.position - change.previousPosition
                            if (mode != 0) holdJob.cancel()
                            if (mode == 0 && totalDrag.getDistance() > viewConfiguration.touchSlop * 2.5f) {
                                mode = if (abs(totalDrag.x) > abs(totalDrag.y) * 1.15f) {
                                    if (appSettings.gestureSeek) 1 else 0
                                } else if (!appSettings.gestureVolumeBrightness) {
                                    0
                                } else if (down.position.x > size.width / 2f) 2
                                else 3
                                when (mode) {
                                    1 -> {
                                        basePositionMs = controller.currentPosition
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    2 -> baseVolumeFraction = audioManager
                                        .getStreamVolume(AudioManager.STREAM_MUSIC)
                                        .toFloat() / maxVolume
                                    3 -> {
                                        val current =
                                            activity?.window?.attributes?.screenBrightness ?: -1f
                                        baseBrightness = if (current >= 0f) current else 0.5f
                                    }
                                }
                            }
                            if (mode != 0) {
                                change.consume()
                                when (mode) {
                                    1 -> {
                                        // Horizontal scrub: video follows finger slightly, HUD shows delta.
                                        // Slightly lower sensitivity (90s across width) feels more precise and pleasant.
                                        val deltaMs = (totalDrag.x / size.width) * 90_000f
                                        val target = (basePositionMs + deltaMs.toLong())
                                            .coerceIn(0L, durationMs)
                                        // Video moves with finger — tactile, "the video moves when seeking"
                                        seekTranslateX = 0f
                                        // Subtle scale down while scrubbing feels like grabbing the timeline
                                        // (handled via seekTranslateX + gentle haptics, not extra scale, to keep it light)
                                        seekHudMs = target
                                        // Live scrub — ExoPlayer seeks are cheap, and this makes the video
                                        // appear to move with the finger. Throttle haptics to avoid buzzing.
                                        liveSeek(target)
                                        val now = System.currentTimeMillis()
                                        if (now - lastHapticStep > 120L && kotlin.math.abs(deltaMs) > 2500) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastHapticStep = now
                                        }
                                    }
                                    2 -> {
                                        val fraction = (baseVolumeFraction +
                                            (-totalDrag.y / size.height) * 1.3f)
                                            .coerceIn(0f, 1f)
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            (fraction * maxVolume).roundToInt(),
                                            0,
                                        )
                                        volumeHud = fraction
                                    }
                                    3 -> {
                                        val fraction = (baseBrightness +
                                            (-totalDrag.y / size.height) * 1.3f)
                                            .coerceIn(0.01f, 1f)
                                        activity?.window?.let { w ->
                                            val attrs = w.attributes
                                            attrs.screenBrightness = fraction
                                            w.attributes = attrs
                                        }
                                        brightnessHud = fraction
                                    }
                                }
                            }
                        }

                        // Pinching can cancel a horizontal scrub; always leave preview seek mode.
                        OptiCastPlaybackService.setFastSeeking(false)
                        if (mode == 1) {
                            seekHudMs?.let { controller.seekTo(it) }
                            seekTranslateX = 0f
                        } else if (mode == 0) {
                            // Tap / double-tap handling — with pinch-reset
                            val fractionX = down.position.x / size.width.toFloat()
                            val now = System.currentTimeMillis()
                            if (now - tapState.lastTapMs < 300L) {
                                tapState.singleTapJob?.cancel()
                                tapState.lastTapMs = 0L
                                // If zoomed, double-tap center resets zoom (expected pinch companion)
                                if (abs(zoomScale - 1f) > 0.02f && fractionX in 0.35f..0.65f) {
                                    zoomScale = 1f
                                    zoomOffset = Offset.Zero
                                    zoomHudScale = 1f
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    poke()
                                } else when {
                                    fractionX < 0.35f -> {
                                        controller.seekTo(
                                            (controller.currentPosition -
                                                appSettings.doubleTapSeekSec * 1000L)
                                                .coerceAtLeast(0L)
                                        )
                                        tapFlash = "${appSettings.doubleTapSeekSec}s" to true
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        poke()
                                    }
                                    fractionX > 0.65f -> {
                                        controller.seekTo(
                                            controller.currentPosition +
                                                appSettings.doubleTapSeekSec * 1000L
                                        )
                                        tapFlash = "${appSettings.doubleTapSeekSec}s" to false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        poke()
                                    }
                                    else -> if (controller.isPlaying) {
                                        controller.pause()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        controller.play()
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            } else {
                                tapState.lastTapMs = now
                                tapState.singleTapJob?.cancel()
                                tapState.singleTapJob = scope.launch {
                                    delay(300)
                                    lastInteractionMs = System.currentTimeMillis()
                                    controlsVisible = !controlsVisible
                                }
                            }
                        }
                        holdJob.cancel()
                        if (holdTriggered) {
                            controller.playbackParameters = speedParams(preHoldSpeed)
                            holdTriggered = false
                            holdSpeedActive = false
                            poke()
                        }
                        // Clean up seek HUD snap if not already
                        if (mode != 1) {
                            seekTranslateX = 0f
                        }
                    }
                }
        )

        // ------------------------------- gesture HUDs -------------------------------
        // Enhanced seek HUD — shows delta, progress, and video-follows-finger hint
        seekHudMs?.let { target ->
            val deltaSec = (target - positionMs) / 1000
            val deltaLabel = when {
                deltaSec > 0 -> "+${deltaSec}s"
                deltaSec < 0 -> "${deltaSec}s"
                else -> "00:00"
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 64.dp),
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (deltaSec >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                deltaLabel,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                "${target.formatDuration()} / ${durationMs.formatDuration()}",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (target.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
        // Zoom HUD — pleasant pill that appears while pinching
        zoomHudScale?.let { scale ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.statusBarsPadding().padding(top = 64.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            "Zoom ${if (zoomPercent(scale) > 0) "+" else ""}${zoomPercent(scale)}%",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        if (holdSpeedActive) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 64.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.FastForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "${appSettings.holdSpeedFactor.toInt()}\u00d7 speed while holding",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        if (sleepEndedFlash) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.7f)) {
                    Text(
                        "Sleep timer ended \u2014 playback paused",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }
        volumeHud?.let { fraction -> VerticalGestureHud(Icons.Filled.VolumeUp, effectiveVolumeLevel(fraction, appSettings.audioBoostPct), false, "Volume") }
        brightnessHud?.let { fraction -> VerticalGestureHud(Icons.Filled.BrightnessHigh, fraction, true, "Brightness") }
        tapFlash?.let { (label, isLeft) ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp),
                contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.6f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            if (isLeft) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Loading feedback is independent of initially hidden transport controls.
        // STABILITY FIX: Don't show loading persistently when quickly jumping videos
        // - Keyed to video.id so rapid switches reset immediately
        // - Only show if buffering for CURRENT video and same request
        // - Auto-hide after 3s max to prevent stuck spinner
        var showLoading by remember(video.id) { mutableStateOf(false) }
        LaunchedEffect(video.id) {
            showLoading = false
        }
        LaunchedEffect(buffering, isPlaying, video.id, requestToken) {
            if (video.id <= 0) {
                showLoading = false
                return@LaunchedEffect
            }
            // Only show loading if buffering for current video and not playing
            // Check samePlaybackItem to avoid showing for previous video's buffering
            val isCurrentVideo = samePlaybackItem(video.id, controller.currentMediaItem?.mediaId) && requestIsCurrent()
            if (buffering && !isPlaying && isCurrentVideo && engineError == null) {
                kotlinx.coroutines.delay(300) // Small delay to avoid flicker when quickly jumping
                // Re-check after delay - video may have changed
                if (buffering && !isPlaying && samePlaybackItem(video.id, controller.currentMediaItem?.mediaId) && requestIsCurrent()) {
                    showLoading = true
                }
            } else {
                showLoading = false
            }
        }
        // Auto-hide loading after 3s if still showing to prevent persistent animation until screen closed
        LaunchedEffect(showLoading, video.id) {
            if (showLoading) {
                kotlinx.coroutines.delay(3000)
                if (showLoading) {
                    showLoading = false
                    // Don't reset buffering here - let player state drive it, but ensure spinner gone
                }
            }
        }
        // Extra safety: if video changes, force hide loading immediately
        LaunchedEffect(activeVideoId) {
            showLoading = false
        }
        if (showLoading && engineError == null && !showDiagnostics && samePlaybackItem(video.id, controller.currentMediaItem?.mediaId)) {
            androidx.compose.material3.LoadingIndicator(
                modifier = Modifier.align(Alignment.Center).offset(y = if (controlsVisible) (-80).dp else 0.dp).size(64.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // ------------------------------ controls overlay ----------------------------
        AnimatedVisibility(
            visible = controlsVisible && playerGestureInputEnabled(screenLocked,engineError!=null,showDiagnostics),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(100)),
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.64f), Color.Transparent, Color.Black.copy(alpha = 0.70f))))) {
                // Shared centred action rail; narrow/large-text layouts reflow instead of squeezing the title.
                PlayerTopBar(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp), title = {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (appSettings.playerControls.contains("back")) PlayerActionButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                        Surface(modifier = Modifier.weight(1f, fill = false).clickable { showMovieBrowser = true; poke() },
                            shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.52f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))) {
                            Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(titleLine, Modifier.weight(1f, fill = false), color = Color.White,
                                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (movieLoading) "…" else if (movieLoadError != null) "· —" else "· ${collectionPositionLabel(movies, video.id, episodeCollection)}",
                                    color = Color(0xFFBED5E5), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            }
                        }
                    }
                }, actions = {
                    if (appSettings.playerControls.contains("library")) PlayerActionButton(onClick = ::backToLibrary) {
                        Icon(Icons.Filled.VideoLibrary, "Library", tint = Color.White)
                    }
                    if (appSettings.playerControls.contains("subtitles")) PlayerActionButton(onClick = { showTracksSheet = true; poke() }) {
                        Icon(Icons.Filled.Subtitles, "Subtitles and Audio", tint = Color.White)
                    }
                    PlayerActionButton(onClick = { showMoreControls = true; poke() }) {
                        Icon(Icons.Filled.MoreVert, "More playback controls", tint = Color.White)
                    }
                })

                // Bottom actions flank the video, leaving the seek bar unobstructed.
                if (scrubPosition == null) Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 64.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.weight(1f).horizontalScroll(actionRailScroll),
                        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerActionButton(onClick = {
                            screenLocked = true; unlockVisible = true; controlsVisible = false; tapState.singleTapJob?.cancel()
                        }) { Icon(Icons.Filled.Lock, "Lock screen", tint = Color.White) }
                        PlayerActionButton(onClick = {
                            val landscape = activity?.resources?.configuration?.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                            rotationMode = if (landscape) 2 else 1
                            activity?.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            poke()
                        }) { Icon(Icons.Filled.ScreenRotation, "Rotate portrait or landscape", tint = Color.White) }
                        if (appSettings.playerControls.contains("speed")) PlayerActionButton(onClick = { showSpeedSheet = true; poke() }) {
                            Icon(Icons.Filled.Speed, "Playback speed", tint = Color.White)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PlayerActionButton(onClick = { zoomScale = 1f; zoomOffset = Offset.Zero; zoomHudScale = 1f; poke() }) {
                            Icon(Icons.Filled.ZoomOutMap, "Reset zoom (${zoomPercent(zoomScale)}%)", tint = Color.White)
                        }
                        PlayerActionButton(onClick = {
                            if ((activity as? PlayerActivity)?.enterPipFromControls() != true)
                                Toast.makeText(context, "Picture-in-picture is unavailable or disabled on this device.", Toast.LENGTH_LONG).show()
                        }) { Icon(Icons.Filled.PictureInPictureAlt, "Picture in picture", tint = Color.White) }
                        PlayerActionButton(onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AGGRESSIVE_STRETCH
                                AGGRESSIVE_STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                                AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            zoomScale = 1f; zoomOffset = Offset.Zero; aspectHud = aspectLabel(resizeMode); poke()
                        }) {
                            AnimatedContent(resizeMode, transitionSpec = {
                                (fadeIn(tween(160)) + scaleIn(tween(180), initialScale = 0.7f)) togetherWith fadeOut(tween(100))
                            }, label = "aspectControl") { mode ->
                                Icon(Icons.Filled.AspectRatio, "Aspect ratio: ${aspectLabel(mode)}", tint = Color.White)
                            }
                        }
                    }
                }

                // Center transport controls
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                ) {
                    TransportButton(Icons.Filled.Replay10, "Back 10 seconds") {
                        controller.seekTo(transportSeekPosition(controller.currentPosition, controller.duration, false)); poke()
                    }
                    BigPlayPauseButton(player = controller, isPlaying = isPlaying, onPoke = ::poke)
                    TransportButton(Icons.Filled.Forward10, "Forward 10 seconds") {
                        controller.seekTo(transportSeekPosition(controller.currentPosition, controller.duration, true)); poke()
                    }
                }

                // Second subtitle line, drawn above the control bar so it never
                // collides with Media3's own subtitle rendering underneath.
                SubtitleCues.cueAt(secondaryCues, positionMs)?.let { line ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = if (controlsVisible) 128.dp else 88.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.55f),
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Bottom seek bar
                // Frames are built the first time the user actually scrubs, so
                // opening a video costs nothing extra.
                LaunchedEffect(scrubPosition != null, activeVideoId) {
                    if (scrubPosition == null || thumbsReady) return@LaunchedEffect
                    val v = video ?: return@LaunchedEffect
                    thumbsReady = AppContainer.thumbnails.prepare(v.id, v.uri, v.durationMs)
                }
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Scrub preview: a real frame from the file at the position
                    // being dragged, with the target time underneath.
                    scrubPosition?.let { value ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            ScrubPreview(
                                videoId = activeVideoId,
                                positionMs = value.toLong(),
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            positionMs.formatDuration(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        PlayerProgressBar(
                            value = scrubPosition ?: positionMs.toFloat(),
                            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                            style = appSettings.progressBarStyle,
                            chapterPositionsMs = if (appSettings.showChapterStamps) chapters.map { it.startMs } else emptyList(),
                            onValueChange = { value ->
                                scrubPosition = value
                                liveSeek(value.toLong())
                                poke()
                            },
                            onValueChangeFinished = {
                                OptiCastPlaybackService.setFastSeeking(false)
                                scrubPosition?.let { controller.seekTo(it.toLong()) }
                                scrubPosition = null
                                poke()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp),
                        )
                        Text(
                            durationMs.formatDuration(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        if (aspectHud != null && !screenLocked) {
            Box(Modifier.fillMaxSize().padding(bottom = 84.dp), contentAlignment = Alignment.BottomCenter) {
                AnimatedContent(aspectHud, label = "aspectStatus") { label ->
                    if (label != null) Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                    }
                }
            }
        }
        if (screenLocked && engineError==null && !showDiagnostics) {
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { unlockVisible = !unlockVisible }) })
            if (unlockVisible) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.padding(top = 120.dp)) {
                    SlideToUnlock(onDraggingChanged = { unlockDragging = it }, onUnlock = {
                        screenLocked = false; controlsVisible = true; unlockVisible = false; poke()
                    })
                }
            }
        }

        if (controlsVisible && !screenLocked && !playbackEnded && engineError==null && samePlaybackItem(video.id,controller.currentMediaItem?.mediaId)) {
            com.opticast.player.data.local.skipPrompt(skipProfile,positionMs,durationMs)?.let { (label,target) ->
                Button(onClick={controller.seekTo(target);poke()},modifier=Modifier.align(Alignment.TopEnd).padding(top=76.dp,end=20.dp)) { Text(label) }
            }
        }

        // ------------------------------ "Up next" overlay ------------------------------
        val next = nextEpisode
        if (!screenLocked && playbackEnded && next != null && !nextCancelled && appSettings.autoNextEpisode) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 32.dp),
                ) {
                    Text(
                        "Up next",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Text(
                        next.metadata?.showTitle ?: next.video.parsed.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    val episodeLine = buildString {
                        append(next.tag)
                        next.metadata?.episodeName?.takeIf { it.isNotBlank() }
                            ?.let { append(" · ").append(it) }
                    }
                    if (episodeLine.isNotBlank()) {
                        Text(
                            episodeLine,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        "Playing in ${countdown}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { switchToNext() },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Play now")
                        }
                        FilledTonalButton(
                            onClick = { nextCancelled = true },
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showMovieBrowser && !screenLocked) MovieBrowser(movies, video.id, movieLoading, movieLoadError,
        onRetry = { movieRefresh++ }, onSelect = ::selectMovie, onDismiss = { showMovieBrowser = false; poke() },
        showTitle = if (episodeCollection) showTitleOf(currentCollectionEntry) else null)

    if (showMoreControls && !screenLocked) PlayerMenu(onDismissRequest = { showMoreControls = false; poke() }, headerTitle = "Playback options") {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(video.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if(video.id>0L) TrackRow("Intro / credits ranges", false) { showMoreControls=false;showSkipRanges=true }
            if (selectedEngine != "mpv") TrackRow("Playback diagnostics", false) { showMoreControls=false;showDiagnostics=true }
            if (selectedEngine != "mpv") TrackRow("Sound / Equalizer", false) { showMoreControls = false; showAudioSheet = true }
            TrackRow("Now Playing", false) { showMoreControls = false; showMovieBrowser = true }
            if (appSettings.playerControls.contains("chapters") && chapters.isNotEmpty())
                TrackRow("Chapters", false) { showMoreControls = false; showChaptersSheet = true }
            if (appSettings.playerControls.contains("sleep"))
                TrackRow("Sleep timer", false) { showMoreControls = false; showSleepSheet = true }
            if (appSettings.playerControls.contains("info"))
                TrackRow("Playback information", false) { showMoreControls = false; showInfoSheet = true }
            Text("Double-tap either side to seek. Pinch to zoom. Swipe the bottom-left controls on narrow screens.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // ------------------------------- audio bottom sheet -------------------------------
    if (showAudioSheet) {
        PlayerMenu(onDismissRequest = { showAudioSheet = false }) {
            if (selectedEngine == "mpv") {
                Text("mpv uses its own audio output; EQ and volume boost are unavailable in this build.", Modifier.padding(20.dp))
                TextButton(onClick = { showAudioSheet = false; showTracksSheet = true }) { Text("Choose audio / subtitle track") }
            } else AudioControls(
                settings = appSettings,
                onPreset = { id -> scope.launch { AppContainer.settings.setAudioPreset(id) } },
                onDialogueBoost = { enabled ->
                    scope.launch { AppContainer.settings.setDialogueBoost(enabled) }
                },
                onVolumeBoost = { pct ->
                    scope.launch { AppContainer.settings.setAudioBoostPct(pct) }
                },
            )
        }
    }

    // ------------------------------ chapters bottom sheet ----------------------------
    if (showSleepSheet) {
        PlayerMenu(onDismissRequest = { showSleepSheet = false }) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
            ) {
                Text("Sleep timer", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                listOf(15, 30, 45, 60, 90).forEach { minutes ->
                    val active = sleepUntilMs != null && sleepLabel == "${minutes} minutes"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                        else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sleepUntilMs = System.currentTimeMillis() + minutes * 60_000L
                                sleepAtEnd = false
                                sleepLabel = "$minutes minutes"
                                showSleepSheet = false
                                poke()
                            },
                    ) {
                        Text(
                            "Stop after $minutes minutes",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (sleepAtEnd) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                    else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sleepUntilMs = null
                            sleepAtEnd = true
                            sleepLabel = "end of this video"
                            showSleepSheet = false
                            poke()
                        },
                ) {
                    Text(
                        "Stop at the end of this video",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sleepUntilMs = null
                            sleepAtEnd = false
                            sleepLabel = null
                            showSleepSheet = false
                            poke()
                        },
                ) {
                    Text(
                        "Turn the timer off",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }

    if (showChaptersSheet) {
        // Chapter thumbnails come from the same frame store the scrub previews
        // use; opening the sheet is enough to build them, once per title.
        LaunchedEffect(activeVideoId) {
            val v = video ?: return@LaunchedEffect
            if (AppContainer.thumbnails.meta(v.id) == null) {
                AppContainer.thumbnails.prepare(v.id, v.uri, v.durationMs)
            }
        }
        PlayerMenu(onDismissRequest = { showChaptersSheet = false }) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Chapters",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val previous = chapters.lastOrNull { it.startMs < positionMs - 1_000L }
                        controller.seekTo(previous?.startMs ?: 0L)
                    }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous chapter")
                    }
                    IconButton(onClick = {
                        val next = chapters.firstOrNull { it.startMs > positionMs + 1_000L }
                        if (next != null) controller.seekTo(next.startMs)
                    }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next chapter")
                    }
                }
                Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Show stamps on progress bar", Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = appSettings.showChapterStamps,
                    onCheckedChange = { show -> scope.launch { AppContainer.settings.setShowChapterStamps(show) } })
            }
                val active = chapters.indexOfLast { it.startMs <= positionMs }
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(chapters.size) { index ->
                        val chapter = chapters[index]
                        val selected = index == active
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    controller.seekTo(chapter.startMs)
                                    poke()
                                },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    (index + 1).toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(30.dp),
                                )
                                // A real frame from the chapter's own position.
                                // Only shown once the preview store has something
                                // for this title; until then the row is just text.
                                val thumb = AppContainer.thumbnails
                                    .frameFor(activeVideoId, chapter.startMs)
                                Box(
                                    Modifier
                                        .width(64.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer),
                                ) {
                                    if (thumb != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(thumb)
                                                .crossfade(false)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    chapter.title.ifBlank { "Chapter ${index + 1}" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    chapter.startMs.formatDuration(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if(showSkipRanges && !screenLocked) SkipRangesDialog(currentCollectionEntry,if(episodeCollection) movies else emptyList(),skipProfile,positionMs,durationMs,
        onSave={extrasStore.saveProfiles(it);skipRevision++},onDismiss={showSkipRanges=false;poke()})
    if(showDiagnostics && (!screenLocked || engineError!=null)) PlaybackDiagnosticsDialog(controller,video,diagnosticErrorCode,onDismiss={showDiagnostics=false;poke()})

    // ------------------------------ playback info panel ------------------------------
    if (showInfoSheet) {
        PlayerMenu(onDismissRequest = { showInfoSheet = false }) {
            PlaybackInfoPanel(
                controller = controller,
                video = video,
                positionMs = positionMs,
                durationMs = durationMs,
                engine = selectedEngine,
            )
        }
    }

    // ------------------------------ tracks bottom sheet ------------------------------
    if (showTracksSheet) {
        PlayerMenu(onDismissRequest = { showTracksSheet = false }) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Subtitles and Audio", style = MaterialTheme.typography.headlineSmall)
                Text("Manual choices are remembered for this video only. Missing tracks use your Settings preferences.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    preferenceKey?.let(videoPreferences::clearTracks)
                    trackPreferenceRevision++
                }) { Text("Use automatic track preferences") }
                Spacer(Modifier.height(6.dp))

                FilledTonalButton(onClick = {
                    subtitleOwner = video.id
                    controller.pause()
                    (activity as? PlayerActivity)?.subtitlePickerActive = true
                    runCatching { subtitlePicker.launch(arrayOf("*/*")) }.onFailure {
                        (activity as? PlayerActivity)?.subtitlePickerActive = false
                        subtitleOwner = 0L
                        subMessage = "No file picker is available."
                    }
                }) {
                    Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add external subtitles")
                }
                Text("SRT, ASS, SSA or VTT · copied for offline use", style = MaterialTheme.typography.bodySmall)
                subMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                Text("Subtitles", style = MaterialTheme.typography.titleMedium)
                TrackRow(
                    label = "Off",
                    selected = textDisabled,
                    onClick = {
                        val params = controller.trackSelectionParameters.buildUpon()
                        params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        controller.trackSelectionParameters = params.build()
                        textDisabled = true
                        preferenceKey?.let { videoPreferences.saveTrack(it, C.TRACK_TYPE_TEXT, TRACK_OFF) }
                        selectedTextOption = null
                    },
                )
                textOptions.forEachIndexed { optionIndex, option ->
                    TrackRow(
                        label = option.label,
                        selected = !textDisabled && (selectedTextOption == optionIndex || (selectedTextOption == null && option.group.isTrackSelected(option.index))),
                        onClick = {
                            val params = controller.trackSelectionParameters.buildUpon()
                            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            params.clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            params.setOverrideForType(
                                TrackSelectionOverride(option.group.mediaTrackGroup, option.index)
                            )
                            controller.trackSelectionParameters = params.build()
                            textDisabled = false
                            selectedTextOption = optionIndex
                            rememberTrack(option)
                        },
                    )
                }
                if (textOptions.isEmpty()) {
                    Text(
                        "No subtitle tracks. Add a local file above or search below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ---- dual subtitles: pick a second track to show together ----
                if (savedSubtitles.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Second subtitle", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Shows both subtitles at once - two languages, or one to learn " +
                            "from. Remembered for this video.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TrackRow(
                        label = "Off",
                        selected = secondarySubId == null,
                        onClick = {
                            secondarySubId = null
                            AppContainer.metadataStore.setSecondarySubtitle(video.id, null)
                        },
                    )
                    savedSubtitles.forEach { sub ->
                        TrackRow(
                            label = "${sub.language.uppercase()} · ${sub.releaseName.take(40)}",
                            selected = secondarySubId == sub.id,
                            onClick = {
                                secondarySubId = sub.id
                                AppContainer.metadataStore
                                    .setSecondarySubtitle(video.id, sub.id)
                            },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Audio", style = MaterialTheme.typography.titleMedium)
                audioOptions.forEachIndexed { optionIndex, option ->
                    TrackRow(
                        label = option.label,
                        selected = selectedAudioOption == optionIndex ||
                            (selectedAudioOption == null && option.group.isTrackSelected(option.index)),
                        onClick = {
                            val params = controller.trackSelectionParameters.buildUpon()
                            params.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            params.clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            params.setOverrideForType(
                                TrackSelectionOverride(option.group.mediaTrackGroup, option.index)
                            )
                            controller.trackSelectionParameters = params.build()
                            selectedAudioOption = optionIndex
                            rememberTrack(option)
                        },
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Caption size", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Small" to 0.8f, "Medium" to 1f, "Large" to 1.3f).forEach { (name, scale) ->
                        FilterChip(
                            selected = abs(appSettings.captionScale - scale) < 0.01f,
                            onClick = {
                                scope.launch { AppContainer.settings.setCaptionScale(scale) }
                            },
                            label = { Text(name) },
                        )
                    }
                }
                Text("Caption style", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("Boxed · default" to 1, "White · no background" to 3, "Yellow" to 2).forEach { (name, style) ->
                        FilterChip(
                            selected = appSettings.captionStyle == style,
                            onClick = {
                                scope.launch { AppContainer.settings.setCaptionStyle(style) }
                            },
                            label = { Text(name) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Subtitle sync", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalIconButton(onClick = { applyOffset(offsetMs - 250) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Earlier")
                    }
                    Text(
                        "%+.2fs".format(offsetMs / 1000f),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.Center,
                    )
                    FilledTonalIconButton(onClick = { applyOffset(offsetMs + 250) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Later")
                    }
                    if (offsetMs != 0L) {
                        TextButton(onClick = { applyOffset(0) }) { Text("Reset") }
                    }
                    TextButton(onClick = { autoAlign() }, enabled = !syncBusy) {
                        Text("Auto-align")
                    }
                }
                Text(
                    "Shifts subtitle timing for this video (earlier / later). Saved per video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Auto-align reads the subtitle file's own timing against the video's " +
                        "length: it fixes subtitles that start or finish at the wrong moment. " +
                        "It cannot hear the audio, so anything still out of step needs the " +
                        "nudge above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                syncNote?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text("Find new subtitles", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                FilledTonalButton(onClick = { searchPlayerSubtitles() }) {
                    if (subSearching) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Searching…")
                    } else {
                        Text("Search online (OpenSubtitles / SubDL)")
                    }
                }
                subMessage?.let { msg ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                subResults?.let { results ->
                    Spacer(Modifier.height(8.dp))
                    if (results.isEmpty()) {
                        Text(
                            "Nothing found for this title. Try matching the video again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    results.take(8).forEach { result ->
                        Surface(
                            shape = RoundedCornerShape(16),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        result.releaseName.ifBlank { result.subtitleId },
                                        style = MaterialTheme.typography.bodyMedium,
                                            )
                                    Text(
                                        "${result.language} · ${result.source}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { downloadPlayerSubtitle(result) },
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = "Download")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------- speed bottom sheet ------------------------------
    if (showSpeedSheet) {
        var currentSpeed by remember { mutableStateOf(controller.playbackParameters.speed) }
        PlayerMenu(onDismissRequest = { showSpeedSheet = false }) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
            ) {
                Text("Playback speed", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f).forEach { speed ->
                        FilterChip(
                            selected = currentSpeed == speed,
                            onClick = {
                                controller.playbackParameters = speedParams(speed)
                                currentSpeed = speed
                            },
                            label = { Text("${speed}×") },
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Sleep timer", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(0, 15, 30, 45, 60).forEach { minutes ->
                        FilterChip(
                            selected = sleepSetMinutes == minutes,
                            onClick = {
                                if (minutes == 0) {
                                    sleepEndMs = 0L
                                    sleepSetMinutes = 0
                                } else {
                                    sleepEndMs = System.currentTimeMillis() +
                                        minutes * 60_000L
                                    sleepSetMinutes = minutes
                                }
                            },
                            label = { Text(if (minutes == 0) "Off" else "${minutes} min") },
                        )
                    }
                }
                if (sleepEndMs > 0L) {
                    val remainingMin =
                        ((sleepEndMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
                    Text(
                        "Playback will pause in about $remainingMin min.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Pauses playback automatically — great for watching in bed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Tip: press & hold the video to temporarily speed up to 2×.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ components

@Composable
private fun TransportButton(
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
private fun BigPlayPauseButton(
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
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
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

private fun mimeForPath(path: String): String = when {
    path.endsWith(".ass", ignoreCase = true) || path.endsWith(".ssa", ignoreCase = true) ->
        MimeTypes.TEXT_SSA
    path.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
    else -> MimeTypes.APPLICATION_SUBRIP
}

private fun mimeForName(name: String): String? {
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

private fun captionStyleFor(index: Int): CaptionStyleCompat = when (index) {
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
private fun ScrubPreview(videoId: Long, positionMs: Long) {
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
private fun PlaybackInfoPanel(
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
private fun selectedFormat(
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

private fun formatBitrate(bits: Int): String = formatBitrate(bits.toLong())

private fun formatBitrate(bits: Long): String = when {
    bits >= 1_000_000 -> "%.1f Mbps".format(bits / 1_000_000.0)
    bits >= 1_000 -> "%.0f kbps".format(bits / 1_000.0)
    else -> "$bits bps"
}

@Composable
private fun InfoRow(label: String, value: String) {
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
private fun syntheticRemoteVideo(
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