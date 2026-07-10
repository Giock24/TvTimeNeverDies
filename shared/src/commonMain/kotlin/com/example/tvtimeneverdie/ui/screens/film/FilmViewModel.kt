package com.example.tvtimeneverdie.ui.screens.film

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvtimeneverdie.data.repository.MovieRepository
import com.example.tvtimeneverdie.di.AppContainer
import com.example.tvtimeneverdie.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilmUiState(
    val isLoading: Boolean = true,
    val movies: List<Movie> = emptyList(),
    val errorMessage: String? = null,
)

class FilmViewModel(
    private val movieRepository: MovieRepository = AppContainer.movieRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilmUiState())
    val uiState: StateFlow<FilmUiState> = _uiState.asStateFlow()

    init {
        loadMovies()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val movies = movieRepository.getNowPlaying()
                _uiState.update { it.copy(isLoading = false, movies = movies) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Errore nel caricamento") }
            }
        }
    }
}
