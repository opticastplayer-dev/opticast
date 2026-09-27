package com.opticast.player.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.opticast.player.data.local.*
import com.opticast.player.data.model.LibraryEntry

@Composable
internal fun SkipRangesDialog(current: LibraryEntry, episodes: List<LibraryEntry>, profile: SkipProfile, positionMs: Long, durationMs: Long,
    onSave: (Map<Long,SkipProfile>) -> Unit, onDismiss: () -> Unit) {
    var enabled by rememberSaveable(current.video.id) { mutableStateOf(profile.enabled) }
    var introStart by rememberSaveable(current.video.id) { mutableStateOf(formatSkipTime(profile.intro?.startMs)) }
    var introEnd by rememberSaveable(current.video.id) { mutableStateOf(formatSkipTime(profile.intro?.endMs)) }
    var creditStart by rememberSaveable(current.video.id) { mutableStateOf(formatSkipTime(profile.credits?.startMs)) }
    var creditEnd by rememberSaveable(current.video.id) { mutableStateOf(formatSkipTime(profile.credits?.endMs)) }
    var copies by rememberSaveable(current.video.id) { mutableStateOf(listOf<Long>()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest=onDismiss,containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=.92f),title={Text("Intro / credits ranges")},
        confirmButton={TextButton(onClick={
            fun span(a:String,b:String): SkipSpan? { if(a.isBlank() && b.isBlank()) return null;return SkipSpan(parseSkipTime(a) ?: -1,parseSkipTime(b) ?: -1) }
            val p=SkipProfile(enabled,span(introStart,introEnd),span(creditStart,creditEnd))
            val targets=listOf(current)+episodes.filter { it.video.id in copies && it.video.id!=current.video.id }
            if(targets.any { entry -> listOfNotNull(p.intro,p.credits).any { !validSkipSpan(it,if(entry.video.id==current.video.id) durationMs else entry.video.durationMs) } }) error="Use seconds or mm:ss (hh:mm:ss also works). End must follow start and fit every selected video's known duration. Leave both fields blank to remove a range."
            else if(p.intro!=null && p.credits!=null && p.intro.endMs>p.credits.startMs) error="Intro must finish before credits begin."
            else { onSave(targets.associate { it.video.id to p });onDismiss() }
        }){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}},text={
            Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.65f).dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text("${current.metadata?.displayTitle ?: current.video.name}\nRanges only offer a button when controls are visible. Nothing skips automatically.")
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(enabled,{enabled=it});Text("Enable skip prompts")}
                @Composable fun field(label:String,value:String,change:(String)->Unit) {
                    OutlinedTextField(value,change,label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())
                    TextButton(onClick={change(formatSkipTime(positionMs))}){Text("Use current position (${formatSkipTime(positionMs)})")}
                }
                field("Intro start",introStart){introStart=it.take(16)};field("Intro end",introEnd){introEnd=it.take(16)}
                field("Credits start",creditStart){creditStart=it.take(16)};field("Credits end",creditEnd){creditEnd=it.take(16)}
                TextButton(onClick={introStart="";introEnd="";creditStart="";creditEnd=""}){Text("Clear ranges (then Save)")}
                val others=episodes.filter { it.video.id!=current.video.id }
                if(others.isNotEmpty()) {
                    Text("Optionally copy to selected episodes of this show. This replaces their ranges, not playback progress. Episode intros may differ.",style=MaterialTheme.typography.bodySmall)
                    others.forEach { entry -> Row(verticalAlignment=Alignment.CenterVertically){Checkbox(entry.video.id in copies,{copies=if(it) copies+entry.video.id else copies-entry.video.id});Text(entry.video.name,Modifier.weight(1f))} }
                }
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            }
        })
}
