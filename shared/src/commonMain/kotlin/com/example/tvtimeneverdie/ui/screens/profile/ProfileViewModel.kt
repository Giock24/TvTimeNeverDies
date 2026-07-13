package com.example.tvtimeneverdie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvtimeneverdie.data.repository.AuthRepository
import com.example.tvtimeneverdie.data.repository.MovieRepository
import com.example.tvtimeneverdie.data.repository.TvShowRepository
import com.example.tvtimeneverdie.data.repository.UserMoviesRepository
import com.example.tvtimeneverdie.data.repository.UserShowsRepository
import com.example.tvtimeneverdie.data.repository.WatchedEpisodeEntry
import com.example.tvtimeneverdie.data.repository.WatchedMovieEntry
import com.example.tvtimeneverdie.di.AppContainer
import com.example.tvtimeneverdie.domain.model.Movie
import com.example.tvtimeneverdie.domain.model.Show
import com.example.tvtimeneverdie.domain.model.ShowProgress
import com.example.tvtimeneverdie.domain.model.ShowWatchStatus
import com.example.tvtimeneverdie.util.mapConcurrently
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SERIES_CHUNK_SIZE = 24
private const val MOVIES_CHUNK_SIZE = 24
private const val FETCH_CONCURRENCY = 6

data class ProfileUiState(
    val isLoadingSeries: Boolean = true,
    val isRefreshingSeries: Boolean = false,
    val isLoadingMoreSeries: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val toWatch: List<Show> = emptyList(),
    val watching: List<ShowProgress> = emptyList(),
    val completed: List<ShowProgress> = emptyList(),
    val seriesErrorMessage: String? = null,
    val isLoadingMovies: Boolean = true,
    val isRefreshingMovies: Boolean = false,
    val isLoadingMoreMovies: Boolean = false,
    val toWatchMovies: List<Movie> = emptyList(),
    val watchedMovies: List<Movie> = emptyList(),
    val moviesErrorMessage: String? = null,
)

private data class SeriesFetchResult(
    val toWatch: Show? = null,
    val progress: ShowProgress? = null,
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

    private var latestSeriesWatchlistIds: Set<Int> = emptySet()
    private var latestSeriesWatchedEntries: List<WatchedEpisodeEntry> = emptyList()
    private var latestMoviesWatchlistIds: Set<Int> = emptySet()
    private var latestMoviesWatchedEntries: List<WatchedMovieEntry> = emptyList()

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
                .collect { (watchlistIds, watchedEntries) ->
                    latestSeriesWatchlistIds = watchlistIds
                    latestSeriesWatchedEntries = watchedEntries
                    refreshSeries(watchlistIds, watchedEntries)
                }
        }
        viewModelScope.launch {
            combine(
                userMoviesRepository.watchlistIds(uid),
                userMoviesRepository.watchedEntries(uid),
            ) { watchlistIds, watchedEntries -> watchlistIds to watchedEntries }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingMovies = false, moviesErrorMessage = e.message ?: "Errore Firestore")
                    }
                }
                .collect { (watchlistIds, watchedEntries) ->
                    latestMoviesWatchlistIds = watchlistIds
                    latestMoviesWatchedEntries = watchedEntries
                    refreshMovies(watchlistIds, watchedEntries)
                }
        }
    }

    /** Richiamata dal gesto di pull-to-refresh sulla tab serie: forza un refetch di show/episodi. */
    fun refreshSeriesManually() {
        viewModelScope.launch {
            tvShowRepository.clearCache()
            refreshSeries(latestSeriesWatchlistIds, latestSeriesWatchedEntries, isManualRefresh = true)
        }
    }

    /** Richiamata dal gesto di pull-to-refresh sulla tab film: forza un refetch dei film. */
    fun refreshMoviesManually() {
        viewModelScope.launch {
            movieRepository.clearCache()
            refreshMovies(latestMoviesWatchlistIds, latestMoviesWatchedEntries, isManualRefresh = true)
        }
    }

    private suspend fun refreshSeries(
        watchlistIds: Set<Int>,
        watchedEntries: List<WatchedEpisodeEntry>,
        isManualRefresh: Boolean = false,
    ) {
        _uiState.update {
            if (isManualRefresh) it.copy(isRefreshingSeries = true, seriesErrorMessage = null)
            else it.copy(isLoadingSeries = true, isLoadingMoreSeries = false, seriesErrorMessage = null)
        }
        try {
            val watchedByShow = watchedEntries.groupBy { it.showId }
            val allShowIds = (watchlistIds + watchedByShow.keys).distinct()
            val chunks = allShowIds.chunked(SERIES_CHUNK_SIZE)

            if (chunks.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingSeries = false,
                        isRefreshingSeries = false,
                        isLoadingMoreSeries = false,
                        toWatch = emptyList(),
                        watching = emptyList(),
                        completed = emptyList(),
                    )
                }
                return
            }

            val toWatch = mutableListOf<Show>()
            val watching = mutableListOf<ShowProgress>()
            val completed = mutableListOf<ShowProgress>()

            chunks.forEachIndexed { chunkIndex, chunk ->
                val results = chunk.mapConcurrently(FETCH_CONCURRENCY) { showId ->
                    val show = runCatching { tvShowRepository.getShow(showId) }.getOrNull()
                    if (show == null) {
                        SeriesFetchResult()
                    } else {
                        val watchedCount = watchedByShow[showId]?.size ?: 0
                        if (watchedCount == 0) {
                            if (showId in watchlistIds) SeriesFetchResult(toWatch = show) else SeriesFetchResult()
                        } else {
                            val totalCount = runCatching { tvShowRepository.getEpisodes(showId) }.getOrNull()?.size ?: watchedCount
                            val status = if (totalCount > 0 && watchedCount >= totalCount) {
                                ShowWatchStatus.COMPLETED
                            } else {
                                ShowWatchStatus.WATCHING
                            }
                            val lastWatchedAtMillis = watchedByShow[showId]?.maxOfOrNull { it.watchedAtMillis }
                            SeriesFetchResult(
                                progress = ShowProgress(
                                    show = show,
                                    watchedEpisodeCount = watchedCount,
                                    totalEpisodeCount = totalCount,
                                    status = status,
                                    lastWatchedAtMillis = lastWatchedAtMillis,
                                ),
                            )
                        }
                    }
                }

                results.forEach { result ->
                    result.toWatch?.let { toWatch.add(it) }
                    result.progress?.let { progress ->
                        if (progress.status == ShowWatchStatus.COMPLETED) completed.add(progress) else watching.add(progress)
                    }
                }

                val hasMore = chunkIndex < chunks.lastIndex
                _uiState.update {
                    it.copy(
                        isLoadingSeries = false,
                        isRefreshingSeries = false,
                        isLoadingMoreSeries = hasMore,
                        toWatch = toWatch.sortedByDescending { show -> show.premiered ?: "" },
                        watching = watching.sortedWith(
                            compareByDescending<ShowProgress> { it.lastWatchedAtMillis ?: Long.MIN_VALUE }
                                .thenByDescending { it.show.premiered ?: "" },
                        ),
                        completed = completed.sortedByDescending { progress -> progress.show.premiered ?: "" },
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingSeries = false,
                    isRefreshingSeries = false,
                    isLoadingMoreSeries = false,
                    seriesErrorMessage = e.message ?: "Errore nel caricamento del profilo",
                )
            }
        }
    }

    private suspend fun refreshMovies(
        watchlistIds: Set<Int>,
        watchedEntries: List<WatchedMovieEntry>,
        isManualRefresh: Boolean = false,
    ) {
        _uiState.update {
            if (isManualRefresh) it.copy(isRefreshingMovies = true, moviesErrorMessage = null)
            else it.copy(isLoadingMovies = true, isLoadingMoreMovies = false, moviesErrorMessage = null)
        }
        try {
            val watchedAtByMovieId = watchedEntries.associate { it.movieId to it.watchedAtMillis }
            val watchedIds = watchedAtByMovieId.keys
            val allMovieIds = (watchlistIds + watchedIds).distinct()
            val chunks = allMovieIds.chunked(MOVIES_CHUNK_SIZE)

            if (chunks.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingMovies = false,
                        isRefreshingMovies = false,
                        isLoadingMoreMovies = false,
                        toWatchMovies = emptyList(),
                        watchedMovies = emptyList(),
                    )
                }
                return
            }

            val toWatch = mutableListOf<Movie>()
            val watched = mutableListOf<Movie>()

            chunks.forEachIndexed { chunkIndex, chunk ->
                val movies = chunk.mapConcurrently(FETCH_CONCURRENCY) { id ->
                    runCatching { movieRepository.getMovie(id) }.getOrNull()
                }
                movies.forEach { movie ->
                    if (movie != null) {
                        if (movie.id in watchlistIds) toWatch.add(movie)
                        if (movie.id in watchedIds) watched.add(movie)
                    }
                }

                val hasMore = chunkIndex < chunks.lastIndex
                _uiState.update {
                    it.copy(
                        isLoadingMovies = false,
                        isRefreshingMovies = false,
                        isLoadingMoreMovies = hasMore,
                        toWatchMovies = toWatch.sortedByDescending { movie -> movie.releaseDate ?: "" },
                        watchedMovies = watched.sortedByDescending { movie -> watchedAtByMovieId[movie.id] ?: 0L },
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingMovies = false,
                    isRefreshingMovies = false,
                    isLoadingMoreMovies = false,
                    moviesErrorMessage = e.message ?: "Errore nel caricamento dei film",
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
