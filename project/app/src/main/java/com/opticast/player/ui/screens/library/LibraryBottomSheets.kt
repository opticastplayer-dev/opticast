package com.opticast.player.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom sheets extracted from LibraryScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryGenrePickerSheet(
    genres: List<String>,
    selectedGenre: String,
    onGenreSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f),
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text("Genre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            (listOf("") + genres).forEach { genre ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = selectedGenre == genre, onClick = { onGenreSelected(genre) })
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        genre.ifBlank { "All genres" },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (selectedGenre == genre) Icon(
                        Icons.Filled.Check,
                        "Selected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
