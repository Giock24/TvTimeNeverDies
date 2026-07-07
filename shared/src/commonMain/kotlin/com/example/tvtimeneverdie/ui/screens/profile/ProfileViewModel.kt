package com.example.tvtimeneverdie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvtimeneverdie.data.repository.AuthRepository
import com.example.tvtimeneverdie.data.repository.MovieRepository
import com.example.tvtimeneverdie.data.repository.TvShowRepository
import com.example.tvtimeneverdie.data.repository.UserMoviesRepository
import com.example.tvtimeneverdie.data.repository.UserShowsRepository
import com.example.tvtimeneverdie.data.repository.WatchedEpisodeEntry
import com.example.tvtimeneverdie.di.AppContainer
import com.example.tvtimeneverdie.domain.model.Movie
import com.example.tvtimeneverdie.domain.model.Show
import com.example.tvtimeneverdie.domain.model.ShowProgress
import com.example.tvtimeneverdie.domain.model.ShowWatchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoadingSeries: Boolean = true,
    val email: String? = null,
    val displayName: String? = null,
    val toWatch: List<Show> = emptyList(),
    val watching: List<ShowProgress> = emptyList(),
    val completed: List<ShowProgress> = emptyList(),
    val seriesErrorMessage: String? = null,
    val isLoadingMovies: Boolean = true,
    val toWatchMovies: List<Movie> = emptyList(),
    val watchedMovies: List<Movie> = emptyList(),
    val moviesErrorMessage: String? = null,
)

class ProfileViewModel(
    private val uid: String,
    email: String?,
    displayName: String?,
    private val tvShowRepository: TvShowRepository = AppContainer.tvShowRepository,
    private val userShowsRepository: UserShowsRepository = AppContainer.userShowsRepository,
    private val movieRepository: MovieRepository = AppContainer.movieRepository,
    private val userMoviesRepository: UserMoviesRepository = AppContainer.userMoviesRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(email = email, displayName = displayName))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userShowsRepository.watchlistIds(uid),
                userShowsRepository.allWatchedEpisodes(uid),
            ) { watchlistIds, watchedEntries -> watchlistIds to watchedEntries }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingSeries = false, seriesErrorMessage = e.message ?: "Errore Firestore")
                    }
                }
                .collect { (watchlistIds, watchedEntries) -> refreshSeries(watchlistIds, watchedEntries) }
        }
        viewModelScope.launch {
            combine(
                userMoviesRepository.watchlistIds(uid),
                userMoviesRepository.watchedIds(uid),
            ) { watchlistIds, watchedIds -> watchlistIds to watchedIds }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingMovies = false, moviesErrorMessage = e.message ?: "Errore Firestore")
                    }
                }
                .collect { (watchlistIds, watchedIds) -> refreshMovies(watchlistIds, watchedIds) }
        }
    }

    private suspend fun refreshSeries(watchlistIds: Set<Int>, watchedEntries: List<WatchedEpisodeEntry>) {
        _uiState.update { it.copy(isLoadingSeries = true, seriesErrorMessage = null) }
        try {
            val watchedByShow = watchedEntries.groupBy { it.showId }
            val allShowIds = (watchlistIds + watchedByShow.keys).distinct()

            val toWatch = mutableListOf<Show>()
            val watching = mutableListOf<ShowProgress>()
            val completed = mutableListOf<ShowProgress>()

            for (showId in allShowIds) {
                val show = runCatching { tvShowRepository.getShow(showId) }.getOrNull() ?: continue
                val watchedCount = watchedByShow[showId]?.size ?: 0
                if (watchedCount == 0) {
                    if (showId in watchlistIds) toWatch.add(show)
                    continue
                }
                val totalCount = runCatching { tvShowRepository.getEpisodes(showId) }.getOrNull()?.size ?: watchedCount
                val status = if (totalCount > 0 && watchedCount >= totalCount) {
                    ShowWatchStatus.COMPLETED
                } else {
                    ShowWatchStatus.WATCHING
                }
                val progress = ShowProgress(
                    show = show,
                    watchedEpisodeCount = watchedCount,
                    totalEpisodeCount = totalCount,
                    status = status,
                )
                if (status == ShowWatchStatus.COMPLETED) completed.add(progress) else watching.add(progress)
            }

            _uiState.update {
                it.copy(
                    isLoadingSeries = false,
                    toWatch = toWatch.sortedByDescending { show -> show.premiered ?: "" },
                    watching = watching.sortedByDescending { progress -> progress.show.premiered ?: "" },
                    completed = completed.sortedByDescending { progress -> progress.show.premiered ?: "" },
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoadingSeries = false, seriesErrorMessage = e.message ?: "Errore nel caricamento del profilo")
            }
        }
    }

    private suspend fun refreshMovies(watchlistIds: Set<Int>, watchedIds: Set<Int>) {
        _uiState.update { it.copy(isLoadingMovies = true, moviesErrorMessage = null) }
        try {
            val toWatch = watchlistIds.mapNotNull { id -> runCatching { movieRepository.getMovie(id) }.getOrNull() }
            val watched = watchedIds.mapNotNull { id -> runCatching { movieRepository.getMovie(id) }.getOrNull() }
            _uiState.update {
                it.copy(
                    isLoadingMovies = false,
                    toWatchMovies = toWatch.sortedByDescending { movie -> movie.releaseDate ?: "" },
                    watchedMovies = watched.sortedByDescending { movie -> movie.releaseDate ?: "" },
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoadingMovies = false, moviesErrorMessage = e.message ?: "Errore nel caricamento dei film")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
