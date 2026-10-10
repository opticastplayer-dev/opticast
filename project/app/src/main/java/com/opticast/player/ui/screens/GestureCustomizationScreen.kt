package com.opticast.player.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opticast.player.data.AppContainer
import com.opticast.player.data.GestureConfig
import com.opticast.player.data.AppSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text("Gesture customization") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.TouchApp, null)
                            Text("Swipe actions", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Left side swipe", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("brightness" to "Brightness", "volume" to "Volume", "none" to "None").forEach { (id, label) ->
                                FilterChip(selected = config.leftSwipe == id, onClick = { save(config.copy(leftSwipe = id)) }, label = { Text(label) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Right side swipe", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("brightness" to "Brightness", "volume" to "Volume", "none" to "None").forEach { (id, label) ->
                                FilterChip(selected = config.rightSwipe == id, onClick = { save(config.copy(rightSwipe = id)) }, label = { Text(label) })
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.TouchApp, null)
                            Text("Double tap", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Double-tap left", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("rewind" to "Rewind", "none" to "None").forEach { (id, label) ->
                                FilterChip(selected = config.doubleTapLeft == id, onClick = { save(config.copy(doubleTapLeft = id)) }, label = { Text(label) })
                            }
                        }
                        Text("Double-tap center", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("play_pause" to "Play/Pause", "none" to "None").forEach { (id, label) ->
                                FilterChip(selected = config.doubleTapCenter == id, onClick = { save(config.copy(doubleTapCenter = id)) }, label = { Text(label) })
                            }
                        }
                        Text("Double-tap right", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("forward" to "Forward", "none" to "None").forEach { (id, label) ->
                                FilterChip(selected = config.doubleTapRight == id, onClick = { save(config.copy(doubleTapRight = id)) }, label = { Text(label) })
                            }
                        }
                        Text("Seek duration", style = MaterialTheme.typography.labelLarge)
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { sec ->
                                FilterChip(selected = config.doubleTapSeekSec == sec, onClick = { save(config.copy(doubleTapSeekSec = sec)) }, label = { Text("${sec}s") })
                            }
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.TouchApp, null)
                            Text("Other gestures", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Swipe to seek")
                            Switch(checked = config.swipeSeekEnabled, onCheckedChange = { save(config.copy(swipeSeekEnabled = it)) })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pinch to zoom")
                            Switch(checked = config.pinchZoomEnabled, onCheckedChange = { save(config.copy(pinchZoomEnabled = it)) })
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { save(GestureConfig.default()) }, shape = RoundedCornerShape(16.dp)) {
                            Text("Reset to default")
                        }
                    }
                }
            }
        }
    }
}
