@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.opticast.player.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import com.opticast.player.ui.components.EmptyLibrary
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opticast.player.R
import com.opticast.player.data.AppContainer
import com.opticast.player.data.model.LibraryEntry
import com.opticast.player.data.remote.UpdateChecker
import com.opticast.player.data.remote.tmdbBackdropUrl
import com.opticast.player.ui.components.AutoFitLabel
import com.opticast.player.ui.components.PosterImage
import com.opticast.player.ui.components.discoveryMotionEnabled
import com.opticast.player.data.model.showTitleOf
import com.opticast.player.ui.components.posterUrlFor
import kotlinx.coroutines.delay


// Extracted from LibraryScreen.kt to reduce god file size - all components made internal for reuse

// Extracted from LibraryScreen.kt to reduce god file size - all components made internal for reuse

// Extracted from LibraryScreen.kt to reduce god file size - all components made internal for reuse

// Extracted from LibraryScreen.kt to reduce god file size - all components made internal for reuse

@Composable
internal fun LibraryHeader(onOpenSettings: () -> Unit, onCustomize: () -> Unit, scanning: Boolean, onScan: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = LibraryPosterInsetDp.dp, vertical = 6.dp)) {
        val viewportWidth = maxWidth + (LibraryPosterInsetDp * 2).dp
        val rowWidth = maxOf(maxWidth, 248.dp)
        val logoWidth = minOf(viewportWidth * LibraryWordmarkWidthFraction, rowWidth - 148.dp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).width(rowWidth),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Image(painter = painterResource(R.drawable.opticast_wordmark), contentDescription = "OptiCast",
                    contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                    modifier = Modifier.width(logoWidth).height(logoWidth * (67f / 260f)))
                Text("Your local cinema", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onCustomize, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Tune, "Customize Library", modifier=Modifier.size(22.dp)) }
            IconButton(onClick = onScan, enabled = !scanning, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = if (scanning) "Scanning library" else "Scan library" }) {
                if (scanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
            }
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Surface(shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (pressed) 0.95f else 0.55f),
                modifier = Modifier.size(48.dp).clickable(interactionSource = interaction, indication = null, onClick = onOpenSettings)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {}
) {
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    OutlinedTextField(
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSubmit(); keyboard?.hide() }),
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        placeholder = { Text("Search your library") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
internal fun HeroPager(items: List<LibraryEntry>, onOpenDetail: (Long) -> Unit, onPlay: (LibraryEntry) -> Unit,
    selectionMode: Boolean, selectedIds: Set<Long>, onToggle: (LibraryEntry) -> Unit, onHold: (LibraryEntry) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    val motion = discoveryMotionEnabled() && !selectionMode
    var autoAdvance by rememberSaveable { mutableStateOf(true) }
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()
    val context = LocalContext.current
    val accessibility = context.getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
        as? android.view.accessibility.AccessibilityManager
    val config = LocalConfiguration.current
    val tight = com.opticast.player.ui.layout.useCompactLayout(config.screenWidthDp, config.screenHeightDp)
    val cardHeight = (if (tight) 210.dp else 228.dp) + (96f * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)).dp

    LaunchedEffect(items.map { it.video.id }, motion, autoAdvance, dragged) {
        if (!motion || !autoAdvance || dragged) return@LaunchedEffect
        while (items.size > 1) {
            delay(8000)
            if (!pagerState.isScrollInProgress && accessibility?.isTouchExplorationEnabled != true) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size, animationSpec = tween(durationMillis = 1100, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            }
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            key = { items[it].video.id },
            contentPadding = PaddingValues(horizontal = DiscoveryGutterDp.dp),
            pageSpacing = DiscoveryGapDp.dp,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().height(cardHeight)
        ) { page ->
            val entry = items[page]
            val metadata = entry.metadata
            val title = metadata?.displayTitle ?: entry.video.parsed.title.ifBlank { entry.video.name }
            val image = tmdbBackdropUrl(metadata?.backdropPath) ?: posterUrlFor(entry)
            fun activate(watchButton: Boolean) {
                when(featuredTap(selectionMode, watchButton)) {
                    FeaturedTap.SELECT -> onToggle(entry)
                    FeaturedTap.PLAY -> onPlay(entry)
                    FeaturedTap.DETAILS -> onOpenDetail(entry.video.id)
                }
            }
            SelectableCard(selectionMode, entry.video.id in selectedIds, Modifier.fillMaxSize(), onToggle = { onToggle(entry) }) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer {
                        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        scaleY = 1f
                        alpha = if (motion) featuredPageOpacity(offset) else 1f
                    }
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF091526))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(30.dp))
                    .combinedClickable(onClick = { activate(false) },
                        onLongClick = { if (selectionMode) onToggle(entry) else onHold(entry) })
            ) {
                PosterImage(url = image, fallbackTitle = title, videoId = entry.video.id,
                    modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(Color(0xC906101F), Color(0x1006101F)))))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x4206101F), Color(0xF906101F)))))
                Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f, fill = false).clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFB0F8FF), Color(0xFF64B9FF))))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFF061827), modifier = Modifier.size(14.dp))
                        Text("FEATURED PICK", color = Color(0xFF061827), fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                    }
                    if (!selectionMode) Text("${page + 1} / ${items.size}", color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 9.dp, vertical = 6.dp))
                }
                Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                        letterSpacing = (-0.7).sp, color = Color.White, maxLines = if (tight) 1 else 2, overflow = TextOverflow.Ellipsis)
                    val facts = listOfNotNull(
                        if (entry.video.isEpisode || metadata?.type == "tv") "TV SERIES" else "MOVIE",
                        metadata?.year?.toString(),
                        metadata?.genres?.firstOrNull(),
                        metadata?.voteAverage?.takeIf { it > 0.0 }?.let { "★ %.1f".format(it) }
                    ).joinToString("  ·  ")
                    Text(facts, color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium, maxLines = if (tight) 1 else 2, overflow = TextOverflow.Ellipsis)
                    Button(onClick = { activate(true) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor=Color.White, contentColor=Color(0xFF061827))) {
                        Icon(if(selectionMode) Icons.Filled.Check else Icons.Filled.PlayArrow, contentDescription=null, modifier=Modifier.size(20.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(if(selectionMode) "Select title" else "Watch Now", fontWeight=FontWeight.Bold)
                    }
                }
            }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = DiscoveryGutterDp.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (items.size > 1) "${pagerState.currentPage + 1} / ${items.size} · Swipe to explore" else "From your collection",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (items.size > 1 && motion) IconButton(onClick = { autoAdvance = !autoAdvance }) {
                Icon(if(autoAdvance) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if(autoAdvance) "Pause featured rotation" else "Resume featured rotation")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(items.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(Modifier.width(if (selected) 24.dp else 6.dp).height(6.dp).clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                }
            }
        }
    }
}

@Composable
internal fun LibraryBottomBar(tab: String, onTabChange: (String) -> Unit,
    movieCount: Int, showCount: Int, favoriteCount: Int, searchOpen: Boolean, onSearch: () -> Unit,
    modifier: Modifier = Modifier, favoriteVersion: Int = 0) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val favoriteBounce = remember { Animatable(1f) }
    var lastFavoriteVersion by rememberSaveable { mutableStateOf(favoriteVersion) }
    LaunchedEffect(favoriteVersion) {
        if (favoriteVersion != lastFavoriteVersion) {
            lastFavoriteVersion = favoriteVersion
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            favoriteBounce.snapTo(0.8f)
            favoriteBounce.animateTo(1.22f, tween(130))
            favoriteBounce.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 420f))
        }
    }
    val destinations = listOf(Triple("movies", countedLibraryTab("Movies", movieCount), Icons.Filled.Movie),
        Triple("tv", countedLibraryTab("TV shows", showCount), Icons.Filled.LiveTv),
        Triple("favs", countedLibraryTab("Favorites", favoriteCount), Icons.Filled.Favorite))
    val textMeasurer = rememberTextMeasurer()
    val countStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val widestLabelPx = destinations.maxOf { textMeasurer.measure(it.second, countStyle, softWrap = false).size.width }
    val barHeight = 52.dp + 14.dp * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
    Surface(modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = LibraryBottomBarAlpha), tonalElevation = 0.dp) {
        Column {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val sideInset = 0.dp
        val cellWidth = maxOf(com.opticast.player.ui.layout.adaptiveControlWidthDp(density.fontScale).dp,
            with(density) { widestLabelPx.toDp() } + 12.dp)
        val rowWidth = maxOf(maxWidth, cellWidth * 4 + sideInset)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).width(rowWidth).navigationBarsPadding().height(barHeight),
            verticalAlignment = Alignment.CenterVertically) {
            destinations.forEach { (id, label, icon) ->
                val selected = tab == id && !searchOpen
                val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Box(Modifier.weight(1f).height(barHeight)
                    .selectable(selected = selected, role = Role.Tab,
                        onClick = { onTabChange(id) }), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp).graphicsLayer { val factor = if (id == "favs") favoriteBounce.value else 1f; scaleX = factor; scaleY = factor }, tint = tint)
                        com.opticast.player.ui.components.AutoFitLabel(label, color = tint, maxSp = 12,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium))
                    }
                }
            }
            Box(Modifier.weight(1f).height(barHeight)
                .selectable(selected = searchOpen, role = Role.Button, onClick = onSearch),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                    val tint = if (searchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)
                    com.opticast.player.ui.components.AutoFitLabel("Search", maxSp = 12, color = tint,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (searchOpen) FontWeight.Bold else FontWeight.Medium))
                }
            }
        }
    }
}
}
}

@Composable
internal fun UpToDateCard(version: String, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("✅", style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Up To Date - v$version",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "You have the latest version from GitHub! 🎉 Your app is fully updated and ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(
                onClick = {
                    UpdateChecker.clearUpToDate(context)
                    onDismiss?.invoke()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
internal fun WhatsNewCard(version: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var showConfetti by remember { mutableStateOf(true) }
    LaunchedEffect(version) {
        showConfetti = true
        delay(2000)
        showConfetti = false
    }
    val realChangelog = remember(version) {
        val stored = UpdateChecker.getWhatsNewChangelog(context)
        if (!stored.isNullOrBlank() && stored.length > 20 && !stored.contains("clearly shows what's new")) {
            stored.lines().filter { it.isNotBlank() }.take(4).joinToString("\n")
        } else {
            when {
                version.contains("2.6.86") -> "• Library smooth like Settings - no white flash, fast scrolling\n• Scan never freezes - handles damaged files, crash log saved\n• Saves data - small posters on metered, only when not playing"
                version.contains("2.6.85") -> "• Easy to understand for everyone - removed confusing technical words\n• Beautiful, fast, offline-first - works on all phones\n• Plays everything - simple language, no jargon"
                version.contains("2.6.84") -> "• Mobile only - focused on phones for best experience\n• Faster and smoother - grid changeable, better file handling\n• Compact What's New shows real changes"
                version.contains("2.6.81") -> "• Fixed loading spinner when quickly switching videos\n• Picture-in-Picture auto-resumes playback\n• Beautiful poster fallback fixes black background"
                else -> "• Stability and performance improvements\n• Works offline, fast and smooth"
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🎉 What's New in $version",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        "Dismiss",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                realChangelog,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 4
            )
        }
    }
        if (showConfetti) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🎊 ✨ 🎉 ✨ 🎊", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun StatsCard(stats: LibraryStats) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = DiscoveryGutterDp.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val columns = com.opticast.player.ui.layout.adaptiveStatsColumns(maxWidth.value, LocalDensity.current.fontScale)
            val cells = listOf(stats.watched.toString() to "Watched",
                (if (stats.totalHours >= 10) "%.0fh".format(stats.totalHours) else "%.1fh".format(stats.totalHours)) to "Library time")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        group.forEach { (value, label) -> StatCell(value, label, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        com.opticast.player.ui.components.AutoFitLabel(value, maxSp = 22, minSp = 20,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.primary)
        Text(label, Modifier.fillMaxWidth(), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun FilterChipsRow(sortBy: String, onSortChange: (String) -> Unit, selectedGenre: String, onGenreClick: () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    val visualHeight = com.opticast.player.ui.layout.adaptiveChipHeightDp(fontScale).dp
    val targetHeight = maxOf(48.dp, visualHeight + 14.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val rowWidth = maxOf(maxWidth, (5 * com.opticast.player.ui.layout.adaptiveControlWidthDp(fontScale) + 24).dp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).width(rowWidth),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("recent" to "Recent", "title" to "Title", "rating" to "Rating", "year" to "Year", "genre" to "Genre").forEach { (id, label) ->
                    val selected = if (id == "genre") selectedGenre.isNotBlank() else sortBy == id
                    Box(Modifier.weight(1f).height(targetHeight)
                        .selectable(selected = selected, role = Role.Button,
                            onClick = { if (id == "genre") onGenreClick() else onSortChange(id) }), contentAlignment = Alignment.Center) {
                        Surface(Modifier.fillMaxWidth().height(visualHeight), shape = RoundedCornerShape(12.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant)) {
                            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                com.opticast.player.ui.components.AutoFitLabel(label, maxSp = 14, minSp = 13, style = MaterialTheme.typography.labelLarge.copy(lineHeight = 20.sp))
                            }
                        }
                    }
                }
            }
        }
        if (selectedGenre.isNotBlank()) Text("Genre: $selectedGenre", Modifier.padding(top = 4.dp, start = 4.dp),
            style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun EmptyLibrary(fromSearch: Boolean) {
    EmptyLibrary(
        fromSearch = fromSearch,
        onOpenSettings = null,
        onRefresh = null
    )
}

@Composable
internal fun PermissionGate(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Movie,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Your videos, beautifully organized",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "OptiCast finds your movies and shows on this device and organizes them with posters. It works offline, no ads, no tracking. Grant video access to start.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Text("Allow access")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Private • Offline-first • Open source",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun EntryMenuSheet(
    entry: LibraryEntry,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
    onMatch: () -> Unit,
    onRefreshArtwork: () -> Unit,
    onToggleWatched: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onForget: () -> Unit,
    groupEntries: List<LibraryEntry>? = null
) {
    val targets = groupEntries ?: listOf(entry)
    val watched = targets.isNotEmpty() && targets.all { AppContainer.playbackState.state(it.video.id)?.isWatched == true }
    val favorite = targets.isNotEmpty() && targets.all { AppContainer.favorites.isFavorite(it.video.id) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f), tonalElevation = 0.dp) {
        Column(
            Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = if (groupEntries != null) "${showTitleOf(entry)} · ${targets.size} episodes" else entry.metadata?.displayTitle ?: entry.video.parsed.title.ifBlank { entry.video.name },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MenuActionRow(Icons.Filled.PlayArrow, "Play now", onPlay)
            MenuActionRow(Icons.Filled.Queue, "Play next", onClick = {
                targets.forEach { AppContainer.queueStore.addNext(it.video.id) }
                onDismiss()
            })
            MenuActionRow(Icons.Filled.Queue, "Add to queue", onClick = {
                targets.forEach { AppContainer.queueStore.addToQueue(it.video.id) }
                onDismiss()
            })
            MenuActionRow(Icons.Filled.Info, "Details", onDetails)
            MenuActionRow(
                Icons.Filled.Check,
                if (watched) "Mark as unwatched" else "Mark as watched",
                onClick = { onToggleWatched(!watched) }
            )
            MenuActionRow(
                if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (favorite) "Remove from favorites" else "Add to favorites",
                onToggleFavorite
            )
            MenuActionRow(Icons.Filled.Share, if (groupEntries != null) "Share all ${targets.size} episodes" else "Share file", onShare)
            MenuActionRow(Icons.Filled.CheckBox, "Select", onSelect)
            MenuActionRow(
                Icons.Filled.Delete,
                if (groupEntries != null) "Delete all ${targets.size} episodes" else "Delete from device",
                onDelete,
                tint = MaterialTheme.colorScheme.error
            )
            if (groupEntries == null) MenuActionRow(Icons.Filled.AutoFixHigh, "Find metadata (TMDB)", onMatch)
            if (groupEntries == null && entry.metadata?.posterPath != null) {
                MenuActionRow(
                    Icons.Filled.Refresh,
                    "Refresh artwork (poster looks wrong)",
                    onRefreshArtwork
                )
            }
            if (groupEntries == null && entry.metadata != null) {
                MenuActionRow(Icons.Filled.Delete, "Forget metadata", onForget)
            }
        }
    }
}

@Composable
internal fun SelectableCard(selectionMode: Boolean, selected: Boolean, modifier: Modifier = Modifier,
    onToggle: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(modifier) {
        content()
        if (selectionMode) Box(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp).size(48.dp)
            .then(if (onToggle != null) Modifier.selectable(selected = selected,
                role = Role.Checkbox, onClick = onToggle) else Modifier), contentAlignment = Alignment.Center) {
            Box(Modifier.size(24.dp).clip(CircleShape)
                .background(if (selected) Color(0xFFE53935) else Color.Black.copy(alpha = 0.65f))
                .border(1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Filled.Check, "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                else Box(Modifier.size(12.dp).semantics { contentDescription = "Select item" })
            }
        }
    }
}

@Composable
internal fun MenuActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}