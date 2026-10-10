package com.opticast.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.data.GestureConfig
import com.opticast.player.data.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GestureCustomizationScreen(
    onBack: () -> Unit
) {
    val settings by AppContainer.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(GestureConfig.fromString(settings.gestureCustomization)) }

    LaunchedEffect(settings.gestureCustomization) {
        config = GestureConfig.fromString(settings.gestureCustomization)
    }

    fun save(newConfig: GestureConfig) {
        config = newConfig
        scope.launch {
            AppContainer.settings.setGestureCustomization(newConfig.toJson())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestures") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        "Player gestures",
                        Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(28.dp))
                            .padding(vertical = 4.dp)
                    ) {
                        // Swipe section
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.TouchApp, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Swipe", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Vertical swipe on sides of player",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Left side", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("brightness" to "Brightness", "volume" to "Volume", "none" to "None").forEach { (id, label) ->
                                    FilterChip(selected = config.leftSwipe == id, onClick = { save(config.copy(leftSwipe = id)) }, label = { Text(label) })
                                }
                            }
                            Text("Right side", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("brightness" to "Brightness", "volume" to "Volume", "none" to "None").forEach { (id, label) ->
                                    FilterChip(selected = config.rightSwipe == id, onClick = { save(config.copy(rightSwipe = id)) }, label = { Text(label) })
                                }
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(1.dp)
                                .padding(horizontal = 16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        // Double tap section
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.TouchApp, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Double tap", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Action for different areas and seek distance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Left", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("rewind" to "Rewind", "none" to "None").forEach { (id, label) ->
                                    FilterChip(selected = config.doubleTapLeft == id, onClick = { save(config.copy(doubleTapLeft = id)) }, label = { Text(label) })
                                }
                            }
                            Text("Center", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("play_pause" to "Play / Pause", "none" to "None").forEach { (id, label) ->
                                    FilterChip(selected = config.doubleTapCenter == id, onClick = { save(config.copy(doubleTapCenter = id)) }, label = { Text(label) })
                                }
                            }
                            Text("Right", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("forward" to "Forward", "none" to "None").forEach { (id, label) ->
                                    FilterChip(selected = config.doubleTapRight == id, onClick = { save(config.copy(doubleTapRight = id)) }, label = { Text(label) })
                                }
                            }
                            Text("Seek distance", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 15, 30).forEach { sec ->
                                    FilterChip(selected = config.doubleTapSeekSec == sec, onClick = { save(config.copy(doubleTapSeekSec = sec)) }, label = { Text("${sec}s") })
                                }
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().height(1.dp)
                                .padding(horizontal = 16.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        // General section
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.TouchApp, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(14.dp))
                                Text("General", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Swipe to seek", style = MaterialTheme.typography.labelLarge)
                                    Text("Horizontal swipe to seek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = config.swipeSeekEnabled, onCheckedChange = { save(config.copy(swipeSeekEnabled = it)) })
                            }
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Pinch to zoom", style = MaterialTheme.typography.labelLarge)
                                    Text("Pinch to adjust zoom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = config.pinchZoomEnabled, onCheckedChange = { save(config.copy(pinchZoomEnabled = it)) })
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { save(GestureConfig.default()) }, shape = RoundedCornerShape(16.dp)) {
                                Text("Reset to default")
                            }
                        }
                    }
                }
            }
        }
    }
}
