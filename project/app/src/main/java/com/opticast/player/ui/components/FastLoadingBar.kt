package com.opticast.player.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

/** Faster visual sweep, not a fabricated percentage or faster-scan claim. */
@Composable
fun FastLoadingBar(modifier: Modifier = Modifier, progress: Float? = null) {
    val motion = rememberInfiniteTransition(label = "startupSweep")
    val phase = motion.animateFloat(0f, 1f,
        infiniteRepeatable(tween(750, easing = LinearEasing)), label = "startupPhase")
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(6.dp).semantics {
        progressBarRangeInfo = progress?.let { ProgressBarRangeInfo(it.coerceIn(0f,1f),0f..1f) }
            ?: ProgressBarRangeInfo.Indeterminate
    }) {
        val y=size.height/2
        drawLine(color.copy(alpha=0.18f),Offset(0f,y),Offset(size.width,y),size.height,StrokeCap.Round)
        progress?.let { drawLine(color.copy(alpha=0.65f),Offset(0f,y),Offset(size.width*it.coerceIn(0f,1f),y),size.height,StrokeCap.Round) }
        val end=if(progress==null) size.width else size.width*progress.coerceIn(0f,1f)
        val head=(-0.25f+1.5f*phase.value)*end
        val left=head.coerceIn(0f,end);val right=(head+end*0.25f).coerceIn(0f,end)
        if(right>left) drawLine(color,Offset(left,y),Offset(right,y),size.height,StrokeCap.Round)
    }
}
