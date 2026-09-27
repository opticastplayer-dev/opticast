@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.opticast.player.player

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.opticast.player.data.local.diagnosticAdvice
import com.opticast.player.data.model.LocalVideo

/** Whitelist fields, never include exception messages, title/path/URL, headers or API keys. */
internal fun playbackDiagnosticReport(context: Context, controller: MediaController, video: LocalVideo, errorCode: Int?): String = buildString {
    appendLine("OptiCast playback diagnostics")
    appendLine("App: "+runCatching{context.packageManager.getPackageInfo(context.packageName,0).versionName}.getOrNull())
    appendLine("Android API: ${Build.VERSION.SDK_INT}; device: ${Build.MANUFACTURER} ${Build.MODEL}")
    val uri=android.net.Uri.parse(video.uri)
    val source=when { knownLocalPlayback(uri.scheme,uri.authority)->"local"; uri.scheme?.lowercase() in setOf("http","https","smb","rtsp")->"network"; else->"document / unknown" }
    appendLine("Engine: Media3; source: $source")
    val state=when(controller.playbackState) { 1 -> "Idle"; 2 -> "Buffering"; 3 -> "Ready"; 4 -> "Ended"; else -> "Unknown" }
    appendLine("State: $state; playing: ${controller.isPlaying}; buffered: ${controller.bufferedPercentage}%")
    val trace=PlayerStats.startup?.takeIf { it.mediaId==video.id.toString() }
    appendLine("Initialized video decoder: ${safeDecoderName(if(trace!=null) PlayerStats.fullVideoDecoder else null)}")
    appendLine("Initialized audio decoder: ${safeDecoderName(if(trace!=null) PlayerStats.fullAudioDecoder else null)}")
    appendLine("Profile-8 HEVC configuration correction: ${if(Build.VERSION.SDK_INT>=29) "enabled for eligible clear-content fallback; not proof it was used" else "not enabled on this Android version"}")
    if(trace!=null) {
        appendLine("Observed buffering episodes: ${PlayerStats.bufferingHealth.episodes}; time: ${PlayerStats.bufferingHealth.totalMs(android.os.SystemClock.elapsedRealtime())} ms (includes startup, seeks and paused buffering)")
        appendLine("Reported dropped video frames: ${PlayerStats.droppedFrames} (observed since this request; not an A/V sync measurement)")
    }
    appendLine("No-progress warning: 30 seconds without reported position/buffer advance while attempting to load; manual recovery only.")
    appendLine("Startup timings: elapsed ms from player-screen request, not a network speed benchmark.")
    StartupStage.entries.forEach { stage -> appendLine("${stage.label}: ${trace?.elapsedMs?.get(stage)?.let { "$it ms" } ?: "not recorded"}") }
    ResumeProbes.forId(video.id.toString())?.let { appendLine(it.report()) }
    appendLine("Startup/seek gate: "+if(knownLocalPlayback(uri.scheme,uri.authority)) "$LOCAL_START_BUFFER_MS ms buffered media (local)" else "Media3 default (network/unknown provider)")
    appendLine("Rebuffering and ongoing buffer allocation remain at Media3 defaults. First frame and running audio/video are separate events.")
    appendLine("Captured error code: ${errorCode ?: "none"}")
    controller.currentTracks.groups.forEach { group ->
        for(i in 0 until group.length) if(group.isTrackSelected(i)) {
            val f=group.getTrackFormat(i)
            fun safe(s:String?)=s.orEmpty().replace(Regex("[^a-zA-Z0-9.,_ /-]"),"_").take(100)
            appendLine("Selected track: ${safe(f.sampleMimeType)}; codecs: ${safe(f.codecs)}; supported: ${group.isTrackSupported(i)}")
            appendLine("Video: ${f.width}x${f.height}; audio: ${f.channelCount} channels / ${f.sampleRate} Hz; DRM: ${f.drmInitData!=null}; colour transfer: ${f.colorInfo?.colorTransfer ?: "unknown"}")
        }
    }
    appendLine("Unknown format values may appear as -1. Support flags are engine-reported, not a hardware-performance guarantee.")
    appendLine("Recovery: ${diagnosticAdvice(errorCode)}")
    appendLine("No media filenames, paths, URLs, credentials or raw exception text included. Review before sharing; device model and codec details are included.")
}

@Composable
internal fun PlaybackDiagnosticsDialog(controller: MediaController, video: LocalVideo, errorCode: Int?, onDismiss: () -> Unit) {
    val context=LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val report=remember(refresh,errorCode) { playbackDiagnosticReport(context,controller,video,errorCode) }
    var notice by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=.92f),title={Text("Playback diagnostics")},
        confirmButton={TextButton(onClick=onDismiss){Text("Close")}},text={
            Column(Modifier.heightIn(max=(LocalConfiguration.current.screenHeightDp*.7f).dp).verticalScroll(rememberScrollState())) {
                Text(report,style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={refresh++}){Text("Refresh report")}
                TextButton(onClick={
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("OptiCast diagnostics",report));notice="Copied to clipboard"
                }){Text("Copy report")}
                TextButton(onClick={runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {type="text/plain";putExtra(Intent.EXTRA_TEXT,report)},"Share diagnostics")) }.onFailure {notice="No sharing application available"}}){Text("Share reviewed report")}
                notice?.let {Text(it)}
            }
        })
}
