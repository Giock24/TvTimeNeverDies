package com.example.tvtimeneverdie.ui.screens.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tvtimeneverdie.ui.components.MediaType
import com.example.tvtimeneverdie.ui.components.MediaTypeTabRow
import com.example.tvtimeneverdie.ui.components.MovieListRow
import com.example.tvtimeneverdie.ui.components.ShowGridItem
import com.example.tvtimeneverdie.ui.components.ShowProgressGridItem
import com.example.tvtimeneverdie.ui.rememberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uid: String,
    email: String?,
    displayName: String?,
    onShowClick: (Int) -> Unit,
    onMovieClick: (Int) -> Unit,
) {
    val viewModel = rememberViewModel { ProfileViewModel(uid, email, displayName) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(MediaType.SERIES) }
    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        GdprImportDialog(uid = uid, onDismiss = { showImportDialog = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.displayName ?: state.email ?: "Profilo") },
                actions = {
                    TextButton(onClick = viewModel::signOut) { Text("Esci") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedButton(
                onClick = { showImportDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Carica i tuoi dati di TV Time")
            }
            MediaTypeTabRow(selected = selectedTab, onSelect = { selectedTab = it })
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    MediaType.SERIES -> when {
                        state.isLoadingSeries -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        state.seriesErrorMessage != null -> Text(
                            text = state.seriesErrorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            profileGridSection(
                                title = "In corso",
                                items = state.watching,
                                emptyText = "Nessuna serie in corso",
                            ) { progress ->
                                ShowProgressGridItem(progress = progress, onClick = { onShowClick(progress.show.id) })
                            }

                            profileGridSection(
                                title = "Da vedere",
                                items = state.toWatch,
                                emptyText = "Nessuna serie in Da vedere",
                            ) { show -> ShowGridItem(show = show, onClick = { onShowClick(show.id) }) }

                            profileGridSection(
                                title = "Completate",
                                items = state.completed,
                                emptyText = "Nessuna serie completata",
                            ) { progress ->
                                ShowProgressGridItem(progress = progress, onClick = { onShowClick(progress.show.id) })
                            }
                        }
                    }
                    MediaType.MOVIES -> when {
                        state.isLoadingMovies -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        state.moviesErrorMessage != null -> Text(
                            text = state.moviesErrorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                        else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                            profileSection(
                                title = "Viste",
                                items = state.watchedMovies,
                                emptyText = "Nessun film visto",
                            ) { movie -> MovieListRow(movie = movie, onClick = { onMovieClick(movie.id) }) }

                            profileSection(
                                title = "Da vedere",
                                items = state.toWatchMovies,
                                emptyText = "Nessun film in Da vedere",
                            ) { movie -> MovieListRow(movie = movie, onClick = { onMovieClick(movie.id) }) }
                        }
                    }
                }
            }
        }
    }
}

private fun <T> LazyListScope.profileSection(
    title: String,
    items: List<T>,
    emptyText: String,
    itemContent: @Composable (T) -> Unit,
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    if (items.isEmpty()) {
        item {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    } else {
        items(items) { item -> itemContent(item) }
    }
}

private fun <T> LazyGridScope.profileGridSection(
    title: String,
    items: List<T>,
    emptyText: String,
    itemContent: @Composable (T) -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
    if (items.isEmpty()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    } else {
        items(items) { item ->
            itemContent(item)
        }
    }
}
