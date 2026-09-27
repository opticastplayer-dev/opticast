package com.opticast.player.ui.components

import androidx.compose.foundation.layout.heightIn

import android.animation.ValueAnimator
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.opticast.player.data.AppContainer

/** Decorative motion only while visible/resumed; honour the device animation setting. */
@Composable
internal fun discoveryMotionEnabled(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return resumed && !AppContainer.lowRamMode && ValueAnimator.areAnimatorsEnabled()
}

/** Bounded, subtle page depth without layout remeasurement. */
internal fun discoveryPageScale(distance: Float): Float =
    1f - 0.05f * (if (distance.isFinite()) kotlin.math.abs(distance).coerceIn(0f, 1f) else 1f)

@Composable
fun DiscoveryHeader(title: String, subtitle: String, accent: Color, count: Int? = null, expanded: Boolean = true, onToggle: (() -> Unit)? = null) {
    if (LocalMinimalStyle.current) {
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp)
            .then(if(onToggle!=null) Modifier.semantics { stateDescription=if(expanded) "Expanded" else "Collapsed" }
                .clickable(role=Role.Button,onClick=onToggle) else Modifier)
            .padding(horizontal=20.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(com.opticast.player.ui.screens.discoveryHeading(title),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold)
            count?.let { Text(it.toString(),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            if(onToggle!=null) Icon(if(expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,contentDescription=null)
        }
        return
    }
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val compact = com.opticast.player.ui.layout.useCompactLayout(config.screenWidthDp, config.screenHeightDp)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .then(if (onToggle != null) Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "Collapse $title" else "Expand $title", onClick = onToggle) else Modifier)
        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(4.dp).height(if (compact) 32.dp else 40.dp).clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.3f)))))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(com.opticast.player.ui.screens.discoveryHeading(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp)
            if (!compact) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        count?.let {
            Box(Modifier.clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.10f))))
                .padding(horizontal = 11.dp, vertical = 7.dp)) {
                Text(it.toString(), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (onToggle != null) Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
    }
}
