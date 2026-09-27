@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.opticast.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun LegalNoticesButton() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("Licences, warranty & source") }
    if (!open) return
    val context = LocalContext.current
    var document by remember { mutableStateOf("OPTICAST.txt") }
    val paragraphs by produceState<List<String>>(emptyList(), document) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("legal/$document").bufferedReader().use { it.readText() }.split("\n\n") }
                .getOrElse { listOf("See the source archive supplied alongside the APK for the complete notices.") }
        }
    }
    AlertDialog(onDismissRequest = { open = false }, title = { Text("Licences & source") },
        text = {
            Column {
                FlowRow {
                    TextButton(onClick = { document = "OPTICAST.txt" }) { Text("Source") }
                    TextButton(onClick = { document = "GPL-3.0.txt" }) { Text("GPL") }
                    TextButton(onClick = { document = "NATIVE-NOTICES.txt" }) { Text("Native") }
                }
                TextButton(onClick = { document = "MAVEN-NOTICES.txt" }) { Text("Android / JVM dependencies") }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    itemsIndexed(paragraphs) { _, paragraph ->
                        Text(paragraph, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } })
}
