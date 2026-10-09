package com.opticast.player.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty State — premium, simple, professional
 * Used for library empty, search no results, offline, no internet
 * Spring animation, large icon, clear actions
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "emptyScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "emptyAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
            .scale(scale)
            .alpha(alpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(96.dp)
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSecondaryAction, modifier = Modifier.fillMaxWidth()) {
                Text(secondaryActionLabel)
            }
        }
    }
}

@Composable
fun EmptyLibraryPremium(
    fromSearch: Boolean,
    onOpenSettings: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    if (fromSearch) {
        EmptyState(
            icon = Icons.Filled.SearchOff,
            title = "Nothing matches",
            subtitle = "Try a different title or choose All genres. Search is offline-first.",
            actionLabel = "Clear search",
            onAction = onOpenSettings
        )
    } else {
        EmptyState(
            icon = Icons.Filled.Movie,
            title = "Your library is empty",
            subtitle = "OptiCast looks for videos in Movies, DCIM, and Download. Add a movie, then pull to refresh. Posters are cached for offline.",
            actionLabel = "Open Settings",
            onAction = onOpenSettings,
            secondaryActionLabel = "Refresh",
            onSecondaryAction = onRefresh
        )
    }
}

@Composable
fun OfflineEmptyState(
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        icon = Icons.Filled.WifiOff,
        title = "Offline",
        subtitle = "You're offline. Your cached posters and library still work. Metadata will sync when internet returns.",
        actionLabel = "Retry",
        onAction = onRetry
    )
}

@Composable
fun NoInternetEmptyState(
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        icon = Icons.Filled.CloudOff,
        title = "No internet",
        subtitle = "Check your connection. Offline library works, but new posters need internet once.",
        actionLabel = "Retry",
        onAction = onRetry
    )
}
