package com.opticast.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.opticast.player.data.local.SmartRule

@Composable
internal fun SmartCollectionsDialog(rules: List<SmartRule>, onSave: (List<SmartRule>) -> Unit, onDismiss: () -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("all") }
    var unwatched by rememberSaveable { mutableStateOf(false) }
    var days by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SmartRule?>(null) }
    fun edit(rule: SmartRule) { editingId=rule.id;name=rule.name;type=rule.type;unwatched=rule.unwatched;days=rule.recentDays?.toString().orEmpty();minutes=rule.maxMinutes?.toString().orEmpty();genre=rule.genre;from=rule.yearFrom?.toString().orEmpty();to=rule.yearTo?.toString().orEmpty();error=null }
    AlertDialog(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=.92f),onDismissRequest=onDismiss,
        title={Text(if(editingId==null) "Smart collections" else "Edit smart collection")},
        confirmButton={TextButton(onClick={
            if(editingId==null) onDismiss() else {
                val numbers=listOf(days,minutes,from,to)
                if(name.isBlank() || numbers.any { it.isNotBlank() && (it.toIntOrNull() ?: 0) <= 0 } || (from.toIntOrNull()!=null && to.toIntOrNull()!=null && from.toInt()>to.toInt())) error="Enter a name and valid positive numbers; the start year must not exceed the end year."
                else { val rule=SmartRule(editingId!!,name.trim().take(60),type,unwatched,days.toIntOrNull(),minutes.toIntOrNull(),genre.trim(),from.toIntOrNull(),to.toIntOrNull());onSave(rules.filterNot{it.id==rule.id}+rule);editingId=null }
            }
        }){Text(if(editingId==null) "Done" else "Save")}},
        dismissButton={if(editingId!=null) TextButton(onClick={editingId=null}){Text("Cancel")}},
        text={Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*.65f).dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(editingId==null) {
                Text("Live groups from cached local metadata. All enabled rules must match. Missing duration/year/genre does not qualify for that filter. No new scans or provider requests.")
                TextButton(onClick={edit(SmartRule(java.util.UUID.randomUUID().toString(),""))}){Text("New smart collection")}
                if(rules.isEmpty()) {
                    TextButton(onClick={edit(SmartRule(java.util.UUID.randomUUID().toString(),"Unwatched movies",type="movies",unwatched=true))}){Text("Start with Unwatched movies")}
                    TextButton(onClick={edit(SmartRule(java.util.UUID.randomUUID().toString(),"Under 90 minutes",type="movies",maxMinutes=90))}){Text("Start with Under 90 minutes")}
                }
                rules.forEach { rule -> Column { Text(rule.name,style=MaterialTheme.typography.titleSmall);Row {TextButton(onClick={edit(rule)}){Text("Edit")};TextButton(onClick={deleting=rule}){Text("Remove")}};HorizontalDivider() } }
            } else {
                OutlinedTextField(name,{name=it.take(60)},label={Text("Name")},singleLine=true)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("all" to "All","movies" to "Movies","tv" to "TV").forEach { (id,label) -> FilterChip(type==id,{type=id},label={Text(label)}) } }
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(unwatched,{unwatched=it});Text("Unwatched only")}
                OutlinedTextField(days,{days=it.take(5)},label={Text("Added within days (optional)")},singleLine=true)
                OutlinedTextField(minutes,{minutes=it.take(5)},label={Text("Under minutes (optional)")},singleLine=true)
                OutlinedTextField(genre,{genre=it.take(60)},label={Text("Exact genre (optional)")},singleLine=true)
                OutlinedTextField(from,{from=it.take(4)},label={Text("From year (optional)")},singleLine=true)
                OutlinedTextField(to,{to=it.take(4)},label={Text("Through year (optional)")},singleLine=true)
                error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            }
        }})
    deleting?.let { rule -> AlertDialog(onDismissRequest={deleting=null},title={Text("Remove smart collection?")},text={Text("Only the rule “${rule.name}” is removed. No media files are deleted.")},confirmButton={TextButton(onClick={onSave(rules.filterNot{it.id==rule.id});deleting=null}){Text("Remove")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}}) }
}
