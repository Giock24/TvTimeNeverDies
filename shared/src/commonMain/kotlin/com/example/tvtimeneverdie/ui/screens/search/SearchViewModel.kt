package com.example.tvtimeneverdie.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvtimeneverdie.data.repository.MovieRepository
import com.example.tvtimeneverdie.data.repository.TvShowRepository
import com.example.tvtimeneverdie.di.AppContainer
import com.example.tvtimeneverdie.domain.model.Movie
import com.example.tvtimeneverdie.domain.model.Show
import com.example.tvtimeneverdie.ui.components.MediaType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val mediaType: MediaType = MediaType.SERIES,
    val query: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasSearched: Boolean = false,
    val showResults: List<Show> = emptyList(),
    val movieResults: List<Movie> = emptyList(),
    val errorMessage: String? = null,
)

class SearchViewModel(
    private val tvShowRepository: TvShowRepository = AppContainer.tvShowRepository,
    private val movieRepository: MovieRepository = AppContainer.movieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onMediaTypeSelected(mediaType: MediaType) {
        _uiState.update {
            it.copy(
                mediaType = mediaType,
                hasSearched = false,
                showResults = emptyList(),
                movieResults = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun search() = runSearch(isRefresh = false)

    /** Richiamata dal gesto di pull-to-refresh: ripete l'ultima ricerca eseguita. */
    fun refresh() = runSearch(isRefresh = true)

    private fun runSearch(isRefresh: Boolean) {
        val query = _uiState.value.query.trim()
        val mediaType = _uiState.value.mediaType
        if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    showResults = emptyList(),
                    movieResults = emptyList(),
                    hasSearched = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }
            try {
                when (mediaType) {
                    MediaType.SERIES -> {
                        val results = tvShowRepository.searchShows(query)
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, hasSearched = true, showResults = results)
                        }
                    }
                    MediaType.MOVIES -> {
                        val results = movieRepository.searchMovies(query)
                        _uiState.update {
                            it.copy(isLoading = false, isRefreshing = false, hasSearched = true, movieResults = results)
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, errorMessage = e.message ?: "Errore nella ricerca")
                }
            }
        }
    }
}
