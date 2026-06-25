package com.aviateclone.launcher.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.aviateclone.launcher.data.AppInfo
import kotlinx.coroutines.launch

private sealed class ListEntry {
    data class Header(val letter: String) : ListEntry()
    data class App(val info: AppInfo) : ListEntry()
}

/**
 * Cassetto app: ricerca + lista raggruppata per lettera iniziale + side
 * alphabet per saltare rapidamente a una sezione (tap o drag continuo).
 */
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    badgeCounts: Map<String, Int> = emptyMap(),
    statusBarPadding: Dp = 0.dp,
    navBarPadding: Dp = 0.dp,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    val entries = remember(filteredApps) {
        val list = mutableListOf<ListEntry>()
        var lastKey = ""
        filteredApps.sortedBy { it.appName.lowercase() }.forEach { app ->
            val key = if (app.firstLetter.firstOrNull()?.isLetter() == true) app.firstLetter else "#"
            if (key != lastKey) { list.add(ListEntry.Header(key)); lastKey = key }
            list.add(ListEntry.App(app))
        }
        list
    }

    // Lettere realmente presenti, in ordine: "#" prima se c'è, poi A-Z
    val availableLetters = remember(entries) {
        entries.filterIsInstance<ListEntry.Header>().map { it.letter }
    }

    fun scrollToLetter(letter: String) {
        val idx = entries.indexOfFirst { it is ListEntry.Header && it.letter == letter }
        if (idx >= 0) scope.launch { listState.scrollToItem(idx) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AppListSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .padding(top = statusBarPadding + 12.dp, bottom = 8.dp)
                    .padding(end = 36.dp) // spazio per la colonna side-alphabet (28dp + margini)
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp, end = 40.dp,
                    bottom = navBarPadding + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries, key = {
                    when (it) {
                        is ListEntry.Header -> "h_${it.letter}"
                        is ListEntry.App -> it.info.packageName
                    }
                }) { entry ->
                    when (entry) {
                        is ListEntry.Header -> LetterHeader(entry.letter)
                        is ListEntry.App -> AppListRow(
                            app = entry.info,
                            badgeCount = badgeCounts[entry.info.packageName] ?: 0,
                            onClick = { onAppClick(entry.info) },
                            onLongClick = { onAppLongClick(entry.info) }
                        )
                    }
                }
            }
        }

        // Side alphabet: visibile solo senza ricerca attiva, come nel vecchio comportamento
        if (query.isBlank() && availableLetters.isNotEmpty()) {
            SideAlphabet(
                letters = availableLetters,
                onLetterSelected = { scrollToLetter(it) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = statusBarPadding, bottom = navBarPadding, end = 4.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun AppListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cerca app") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Pulisci")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                focusedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        letter,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun AppListRow(
    app: AppInfo,
    badgeCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                app.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (badgeCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
