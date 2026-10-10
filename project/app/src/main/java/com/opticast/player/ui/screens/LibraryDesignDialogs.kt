@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.opticast.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.posterUrlFor

@Composable
internal fun LibraryCustomizeDialog(tab: String, design: LibraryDesign, onChange: (LibraryDesign) -> Unit,
    onCollections: () -> Unit, onDismiss: () -> Unit) {
    val latestChange = rememberUpdatedState(onChange)
    val latestDesign = rememberUpdatedState(design)
    val state = rememberLazyListState()
    val direction = LocalLayoutDirection.current
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    fun reorderAtPointer() {
        val id = dragging ?: return
        val target = state.layoutInfo.visibleItemsInfo.firstOrNull {
            dragY >= it.offset && dragY < it.offset + it.size && it.key in latestDesign.value.order
        }?.key as? String ?: return
        val current = latestDesign.value
        val next = moveLibrarySectionTo(current.order, id, target)
        if (next != current.order) latestChange.value(current.copy(order = next))
    }
    LaunchedEffect(dragging) {
        while (dragging != null) {
            val info = state.layoutInfo
            val margin = 64f
            val step = when {
                dragY < info.viewportStartOffset + margin -> -8f
                dragY > info.viewportEndOffset - margin -> 8f
                else -> 0f
            }
            if (step != 0f) { state.scrollBy(step); reorderAtPointer() }
            delay(32)
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth(0.94f).widthIn(max = 560.dp)
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.9f).dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f)) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text("Customize Library", Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
                // The gesture belongs to the stable list, not a row that moves/recomposes during dragging.
                LazyColumn(state = state, modifier = Modifier.weight(1f, fill = false)
                    .pointerInput(tab, direction) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { point ->
                                val onHandle = if (direction == LayoutDirection.Ltr) point.x < 48.dp.toPx()
                                    else point.x > size.width - 48.dp.toPx()
                                dragging = if (onHandle) state.layoutInfo.visibleItemsInfo.firstOrNull {
                                    point.y >= it.offset && point.y < it.offset + it.size && it.key in latestDesign.value.order
                                }?.key as? String else null
                                dragY = point.y
                            },
                            onDrag = { change, amount ->
                                if (dragging != null) { change.consume(); dragY += amount.y; reorderAtPointer() }
                            },
                            onDragEnd = { dragging = null }, onDragCancel = { dragging = null })
                    }) {
                    item(key = LibraryCustomizeControl.STYLE.lazyKey) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("classic" to "Classic", "minimal" to "Minimal").forEach { (id, label) ->
                                FilterChip(selected = design.style == id, onClick = { onChange(design.copy(style = id)) }, label = { Text(label) })
                            }
                        }
                    }
                    if (design.style == "classic") {
                        item(key = LibraryCustomizeControl.STATISTICS.lazyKey) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {                                Text("Library statistics", style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("Hold a handle to drag. Use ⋮ for move buttons.", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        items(design.order, key = { it }) { id ->
                            var menu by remember { mutableStateOf(false) }
                            Surface(color = if (dragging == id) MaterialTheme.colorScheme.secondaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent, shape = MaterialTheme.shapes.small) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.DragHandle, "Hold to reorder ${sectionNames[id]}")
                                    }
                                    Text(sectionNames[id] ?: id, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    Box {
                                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Move ${sectionNames[id]}") }
                                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                            listOf(-1 to "Move up", 1 to "Move down").forEach { (delta, label) ->
                                                DropdownMenuItem(text = { Text(label) }, enabled = if (delta < 0) design.order.first() != id else design.order.last() != id,
                                                    onClick = { menu = false; val now = latestDesign.value
                                                        latestChange.value(now.copy(order = moveLibrarySection(now.order, id, delta))) })
                                            }
                                        }
                                    }
                                    if (id != "titles") Checkbox(checked = id !in design.hidden,
                                        onCheckedChange = { onChange(design.copy(hidden = if (it) design.hidden - id else design.hidden + id)) },
                                        modifier = Modifier.semantics { contentDescription = "Show ${sectionNames[id]}" })
                                    else Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, "Titles are always visible") }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                        item(key = LibraryCustomizeControl.RESET.lazyKey) { TextButton(onClick = { onChange(design.copy(order = classicSectionOrder, hidden = emptySet(), )) }) { Text("Reset this tab") } }
                    } else item(key = LibraryCustomizeControl.MINIMAL_DESCRIPTION.lazyKey) { Text("Poster grids only. Switch to Classic to reorder sections.", style = MaterialTheme.typography.bodyMedium) }
                    item(key = LibraryCustomizeControl.MANAGE_COLLECTIONS.lazyKey) { TextButton(onClick = onCollections) { Text("Manage collections") } }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
internal fun CollectionsDialog(collections: List<PersonalCollection>, entries: List<LibraryEntry>,
    onSave: (List<PersonalCollection>) -> Unit, onOpen: (PersonalCollection) -> Unit, onSmart: () -> Unit, onDismiss: () -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = editingId?.let { id -> collections.firstOrNull { it.id==id } ?: PersonalCollection(id,"",emptyList()) }
    var name by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<PersonalCollection?>(null) }
    val selected = rememberSaveable(saver=androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Long>,Long>(
        save={it.toList()},restore={values -> mutableStateListOf<Long>().apply { addAll(values) }})) { mutableStateListOf<Long>() }
    fun edit(value: PersonalCollection) { editingId=value.id; name=value.name; query=""; error=null; selected.clear(); selected.addAll(value.videoIds) }
    AlertDialog(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=0.92f), onDismissRequest = onDismiss, title = { Text(if(editing==null) "Collections" else "Edit collection") },
        confirmButton = { TextButton(onClick = {
            val current=editing
            if(current==null) onDismiss() else if(name.trim().isEmpty()) error="Enter a collection name."
            else { onSave(collections.filterNot { it.id==current.id } + current.copy(name=name.trim().take(60),videoIds=selected.toList())); editingId=null }
        }) { Text(if(editing==null) "Done" else "Save") } },
        dismissButton = { if(editing!=null) TextButton(onClick={editingId=null}) { Text("Cancel") } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max=(LocalConfiguration.current.screenHeightDp*0.65f).dp)) {
                if(editing==null) {
                    Text("Create your own franchise or personal collections. These are groups of existing files, not copies.")
                    TextButton(onClick=onSmart) { Text("Smart collections") }
                    TextButton(onClick={edit(PersonalCollection(java.util.UUID.randomUUID().toString(),"",emptyList()))}) { Text("New collection") }
                    if(collections.isEmpty()) Text("No collections yet.")
                    LazyColumn(Modifier.weight(1f,fill=false)) { items(collections,key={it.id}) { value ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { onOpen(value) }.padding(vertical=12.dp)) { Text(value.name,maxLines=2); Text("${value.videoIds.size} files",style=MaterialTheme.typography.labelSmall) }
                            IconButton(onClick={edit(value)}) { Icon(Icons.Filled.Edit,"Edit ${value.name}") }
                            IconButton(onClick={deleting=value}) { Icon(Icons.Filled.Delete,"Remove collection") }
                        }
                    } }
                } else {
                    OutlinedTextField(name,{name=it.take(60);error=null},label={Text("Collection name")},singleLine=true)
                    error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                    OutlinedTextField(query,{query=it},label={Text("Find movies or episodes")},singleLine=true)
                    Text("${selected.size} selected. Unavailable members are retained.",style=MaterialTheme.typography.labelSmall)
                    val shown=entries.filter { (it.metadata?.displayTitle ?: it.video.name).contains(query,true) || it.video.name.contains(query,true) }
                    LazyColumn(Modifier.weight(1f,fill=false)) { items(shown,key={it.video.id}) { entry ->
                        Row(Modifier.fillMaxWidth().clickable { if(!selected.remove(entry.video.id)) selected.add(entry.video.id) }.padding(vertical=4.dp), verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(entry.video.id in selected,{ if(it) { if(entry.video.id !in selected) selected.add(entry.video.id) } else selected.remove(entry.video.id) })
                            Column(Modifier.weight(1f)) {
                                Text(entry.metadata?.displayTitle ?: entry.video.name,maxLines=2,overflow=TextOverflow.Ellipsis)
                                if(entry.metadata!=null) Text(entry.video.name,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                            }
                        }
                    } }
                }
            }
        })
    deleting?.let { value -> AlertDialog(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=0.92f), onDismissRequest={deleting=null},title={Text("Remove collection?")},text={Text("Only “${value.name}” is removed. Your video files, metadata and progress are untouched.")},
        confirmButton={TextButton(onClick={onSave(collections.filterNot{it.id==value.id});deleting=null}){Text("Remove")}},dismissButton={TextButton(onClick={deleting=null}){Text("Cancel")}}) }
}

@Composable
internal fun CollectionContentsDialog(collection: PersonalCollection, entries: List<LibraryEntry>, onOpen: (Long) -> Unit, onDismiss: () -> Unit) {
    val available = entries.associateBy { it.video.id }
    val members = collection.videoIds.mapNotNull { available[it] }
    AlertDialog(containerColor=MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha=0.92f), onDismissRequest=onDismiss,title={Text(collection.name)}, confirmButton={TextButton(onClick=onDismiss){Text("Close")}}, text={
        Column(Modifier.heightIn(max=(LocalConfiguration.current.screenHeightDp*0.65f).dp)) {
            if(members.isEmpty()) Text(if(collection.id.startsWith("smart:")) "No available titles match this rule yet. Edit it in Smart collections." else "No available titles in this collection. Add files from Manage collections.")
            if(members.size<collection.videoIds.size) Text("${collection.videoIds.size-members.size} unavailable files are retained in this collection.",style=MaterialTheme.typography.bodySmall)
            LazyColumn { items(members,key={it.video.id}) { entry ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(entry.video.id) }.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    PosterImage(posterUrlFor(entry),entry.metadata?.displayTitle ?: entry.video.name,Modifier.size(44.dp,66.dp),videoId=entry.video.id)
                    Column(Modifier.weight(1f)) {
                                Text(entry.metadata?.displayTitle ?: entry.video.name,maxLines=2,overflow=TextOverflow.Ellipsis)
                                if(entry.metadata!=null) Text(entry.video.name,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                            }
                }
            } }
        }
    })
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun CollectionCover(collection: PersonalCollection, members: List<LibraryEntry>, onClick: () -> Unit, onHold: () -> Unit) {
    Column(Modifier.fillMaxWidth().combinedClickable(onClick=onClick,onLongClick=onHold)) {
        Box(Modifier.fillMaxWidth().height((DiscoveryPosterDp * 1.5f + 12).dp)) {
            val covers=members.asSequence().distinctBy { posterUrlFor(it) ?: it.video.id.toString() }.take(3).toList()
            if(covers.isEmpty()) Surface(Modifier.size(DiscoveryPosterDp.dp,(DiscoveryPosterDp * 1.5f).dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(contentAlignment=Alignment.Center) { Icon(Icons.Filled.VideoLibrary,null) }
            }
            covers.indices.reversed().forEach { index ->
                val entry=covers[index]
                Surface(Modifier.offset(x=(index*12).dp,y=(index*6).dp).size(DiscoveryPosterDp.dp,(DiscoveryPosterDp * 1.5f).dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
                    PosterImage(posterUrlFor(entry),entry.metadata?.displayTitle ?: entry.video.name,Modifier.fillMaxSize(),videoId=entry.video.id)
                }
            }
        }
        Text(collection.name,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
        Text("${collection.videoIds.size} files" + if(collection.id.startsWith("smart:")) " · Smart" else "",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
