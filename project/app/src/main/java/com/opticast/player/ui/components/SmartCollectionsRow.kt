package com.opticast.player.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opticast.player.data.SmartCollection

@Composable
fun SmartCollectionsRow(
    collections: List<SmartCollection>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (collections.isEmpty()) return
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        collections.forEach { coll ->
            FilterChip(
                selected = selectedId == coll.id,
                onClick = { onSelect(coll.id) },
                label = { Text(coll.name) }
            )
        }
    }
}
