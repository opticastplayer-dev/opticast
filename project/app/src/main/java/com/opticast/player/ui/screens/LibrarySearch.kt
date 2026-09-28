package com.opticast.player.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

/**
 * Search components extracted for performance
 * - Only recomposes when query changes
 * - Keeps search logic separate from grid
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit = {}
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSubmit(); keyboard?.hide() }),
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        placeholder = { Text("Search your library") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, "Clear")
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
fun SearchFilters(
    searchScope: String,
    onScopeChange: (String) -> Unit,
    recentQueries: List<String>,
    onQuerySelected: (String) -> Unit,
    onClearHistory: () -> Unit,
    query: String,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "movies" to "Movies", "tv" to "TV").forEach { (id, label) ->
                FilterChip(selected = searchScope == id, onClick = { onScopeChange(id) }, label = { Text(label) })
            }
        }
        if (query.isBlank() && recentQueries.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recent searches", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onClearHistory) { Text("Clear") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recentQueries, key = { it }) { recent ->
                    SuggestionChip(
                        onClick = { onQuerySelected(recent) },
                        label = { Text(recent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp)) }
                    )
                }
            }
        }
    }
}
