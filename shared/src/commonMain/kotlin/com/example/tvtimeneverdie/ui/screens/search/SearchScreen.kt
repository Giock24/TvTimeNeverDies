package com.example.tvtimeneverdie.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tvtimeneverdie.ui.components.MediaType
import com.example.tvtimeneverdie.ui.components.MediaTypeTabRow
import com.example.tvtimeneverdie.ui.components.MovieListRow
import com.example.tvtimeneverdie.ui.components.ShowListRow
import com.example.tvtimeneverdie.ui.rememberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onShowClick: (Int) -> Unit, onMovieClick: (Int) -> Unit) {
    val viewModel = rememberViewModel { SearchViewModel() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val placeholder = if (state.mediaType == MediaType.SERIES) "Titolo della serie" else "Titolo del film"

    Scaffold(
        topBar = { TopAppBar(title = { Text("Cerca") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            MediaTypeTabRow(selected = state.mediaType, onSelect = viewModel::onMediaTypeSelected)
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        label = { Text(placeholder) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.search() }) {
                                Icon(Icons.Filled.Search, contentDescription = "Cerca")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val hasResults = if (state.mediaType == MediaType.SERIES) {
                            state.showResults.isNotEmpty()
                        } else {
                            state.movieResults.isNotEmpty()
                        }
                        when {
                            state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            state.errorMessage != null -> Text(
                                text = state.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            )
                            state.hasSearched && !hasResults -> Text(
                                text = "Nessun risultato",
                                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            )
                            state.mediaType == MediaType.SERIES -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(state.showResults, key = { it.id }) { show ->
                                    ShowListRow(show = show, onClick = { onShowClick(show.id) })
                                }
                            }
                            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                                items(state.movieResults, key = { it.id }) { movie ->
                                    MovieListRow(movie = movie, onClick = { onMovieClick(movie.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
