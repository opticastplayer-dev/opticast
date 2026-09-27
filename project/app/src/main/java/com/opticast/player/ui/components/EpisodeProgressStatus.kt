package com.opticast.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opticast.player.data.local.PlaybackState

internal fun episodeStatusLabel(state: PlaybackState?, preferredResume: Boolean = false): String? = when {
    state?.isWatched == true -> "Completed"
    state?.isResumable == true -> if (preferredResume) "Continue watching" else "In progress"
    else -> null
}

internal fun latestUnfinishedEpisode(states: Map<Long, PlaybackState?>): Long? = states.entries
    .filter { it.value?.isResumable == true }
    .maxWithOrNull(compareBy<Map.Entry<Long, PlaybackState?>> { it.value!!.updatedAt }.thenBy { it.key })?.key

@Composable
internal fun EpisodeProgressStatus(state: PlaybackState?, preferredResume: Boolean = false) {
    val label = episodeStatusLabel(state, preferredResume) ?: return
    val saved = state ?: return
    val complete = saved.isWatched
    val accent = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val fraction = visibleResumeFraction(saved)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(if (complete) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow, null, tint = accent, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
        }
        if (fraction != null) {
            Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.20f))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f); contentDescription = "Episode progress" }) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.65f), accent))))
            }
        }
        Text(if (complete) "100% watched" else if (fraction != null)
            "${(fraction * 100).toInt()}% · ${saved.positionMs.formatDuration()}" else "Resume at ${saved.positionMs.formatDuration()}",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
