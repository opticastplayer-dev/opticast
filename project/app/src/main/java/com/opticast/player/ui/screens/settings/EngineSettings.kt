package com.opticast.player.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opticast.player.data.AppContainer
import com.opticast.player.data.AppSettings
import com.opticast.player.ui.screens.PrefToggle
import com.opticast.player.ui.screens.SettingsCard
import com.opticast.player.ui.screens.settingsHeaderTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Playback settings - extracted for maintainability.
 * No logic change.
 */
@Composable
internal fun EngineSettingsCard(settings: AppSettings, scope: CoroutineScope) {
    val playbackContext = LocalContext.current
    SettingsCard(icon = Icons.Filled.PlayArrow, title = "Playback") {
        Text("To set as default, open a video from your file manager, select OptiCast and choose Always. You can also share videos to OptiCast.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            runCatching { playbackContext.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${playbackContext.packageName}"))) }
        }) { Text("System default settings") }
        PrefToggle(
            label = "Use external player",
            description = "Open videos in another installed player.",
            checked = settings.useExternalPlayer,
        ) { enabled -> scope.launch { AppContainer.settings.setUseExternalPlayer(enabled) } }

        Spacer(Modifier.height(10.dp))
        Text(settingsHeaderTitle("Engine selection"), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("mpv" to "MPV (Default)", "media3" to "Media3").forEach { (id, name) ->
                FilterChip(selected = if (id == "media3") settings.playbackEngine != "mpv" else settings.playbackEngine == id,
                    onClick = { scope.launch { AppContainer.settings.setPlaybackEngine(id) } },
                    label = { Text(name) }, enabled = !settings.useExternalPlayer)
            }
        }
        Text("MPV handles local files with automatic fallback. Network sources and audio enhancements use Media3. Changes apply to the next video.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(10.dp))
        var enginesCleared by remember { mutableStateOf(false) }
        TextButton(onClick = {
            com.opticast.player.player.VideoPlaybackPreferences(playbackContext).clearEngines()
            enginesCleared = true
        }) { Text(if (enginesCleared) "Remembered engines cleared" else "Clear remembered engines") }
    }
}
