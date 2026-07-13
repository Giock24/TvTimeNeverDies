package com.example.tvtimeneverdie.ui.screens.serie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvtimeneverdie.data.repository.TvShowRepository
import com.example.tvtimeneverdie.data.repository.UserShowsRepository
import com.example.tvtimeneverdie.data.repository.WatchedEpisodeEntry
import com.example.tvtimeneverdie.di.AppContainer
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.domain.model.Show
import com.example.tvtimeneverdie.util.isoDateToEpochDay
import com.example.tvtimeneverdie.util.mapConcurrently
import com.example.tvtimeneverdie.util.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SHOWS_CHUNK_SIZE = 24
private const val FETCH_CONCURRENCY = 6
private const val PAST_WINDOW_DAYS = 7L
private const val FUTURE_WINDOW_DAYS = 30L

data class UpcomingEpisodeItem(
    val episode: Episode,
    val show: Show,
    val epochDay: Long,
    val isWatched: Boolean,
)

data class SerieUiState(
    val isLoadingRecent: Boolean = true,
    val isRefreshingRecent: Boolean = false,
    val recentShows: List<Show> = emptyList(),
    val recentErrorMessage: String? = null,
    val isLoadingUpcoming: Boolean = true,
    val isRefreshingUpcoming: Boolean = false,
    val isLoadingMoreUpcoming: Boolean = false,
    val upcomingByDay: List<Pair<Long, List<UpcomingEpisodeItem>>> = emptyList(),
    val upcomingErrorMessage: String? = null,
)

class SerieViewModel(
    private val uid: String,
    private val tvShowRepository: TvShowRepository = AppContainer.tvShowRepository,
    private val userShowsRepository: UserShowsRepository = AppContainer.userShowsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SerieUiState())
    val uiState: StateFlow<SerieUiState> = _uiState.asStateFlow()

    private var latestWatchlistIds: Set<Int> = emptySet()
    private var latestWatchedEntries: List<WatchedEpisodeEntry> = emptyList()

    init {
        loadRecentShows()
        viewModelScope.launch {
            combine(
                userShowsRepository.watchlistIds(uid),
                userShowsRepository.allWatchedEpisodes(uid),
            ) { watchlistIds, watchedEntries -> watchlistIds to watchedEntries }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoadingUpcoming = false, upcomingErrorMessage = e.message ?: "Errore Firestore")
                    }
                }
                .collect { (watchlistIds, watchedEntries) ->
                    latestWatchlistIds = watchlistIds
                    latestWatchedEntries = watchedEntries
                    refreshUpcoming(watchlistIds, watchedEntries)
                }
        }
    }

    private fun loadRecentShows() {
        viewModelScope.launch { fetchRecentShows(isRefresh = false) }
    }

    /** Richiamata dal gesto di pull-to-refresh sulla tab "Novità". */
    fun refreshRecent() {
        viewModelScope.launch {
            tvShowRepository.clearCache()
            fetchRecentShows(isRefresh = true)
        }
    }

    private suspend fun fetchRecentShows(isRefresh: Boolean) {
        _uiState.update {
            if (isRefresh) it.copy(isRefreshingRecent = true, recentErrorMessage = null)
            else it.copy(isLoadingRecent = true, recentErrorMessage = null)
        }
        try {
            val shows = tvShowRepository.getRecentShows()
            _uiState.update { it.copy(isLoadingRecent = false, isRefreshingRecent = false, recentShows = shows) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingRecent = false,
                    isRefreshingRecent = false,
                    recentErrorMessage = e.message ?: "Errore nel caricamento",
                )
            }
        }
    }

    /** Richiamata dal gesto di pull-to-refresh sulla tab "In arrivo": forza un refetch di show/episodi. */
    fun refreshUpcomingManually() {
        viewModelScope.launch {
            tvShowRepository.clearCache()
            refreshUpcoming(latestWatchlistIds, latestWatchedEntries, isManualRefresh = true)
        }
    }

    private suspend fun refreshUpcoming(
        watchlistIds: Set<Int>,
        watchedEntries: List<WatchedEpisodeEntry>,
        isManualRefresh: Boolean = false,
    ) {
        _uiState.update {
            if (isManualRefresh) it.copy(isRefreshingUpcoming = true, upcomingErrorMessage = null)
            else it.copy(isLoadingUpcoming = true, isLoadingMoreUpcoming = false, upcomingErrorMessage = null)
        }
        try {
            val watchedEpisodeIdsByShow = watchedEntries.groupBy { it.showId }
                .mapValues { (_, entries) -> entries.map { it.episodeId }.toSet() }
            val allShowIds = (watchlistIds + watchedEpisodeIdsByShow.keys).distinct()
            val chunks = allShowIds.chunked(SHOWS_CHUNK_SIZE)

            if (chunks.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingUpcoming = false,
                        isRefreshingUpcoming = false,
                        isLoadingMoreUpcoming = false,
                        upcomingByDay = emptyList(),
                    )
                }
                return
            }

            val today = todayEpochDay()
            val minDay = today - PAST_WINDOW_DAYS
            val maxDay = today + FUTURE_WINDOW_DAYS
            val items = mutableListOf<UpcomingEpisodeItem>()

            chunks.forEachIndexed { chunkIndex, chunk ->
                val results = chunk.mapConcurrently(FETCH_CONCURRENCY) { showId ->
                    runCatching {
                        val show = tvShowRepository.getShow(showId)
                        val episodes = tvShowRepository.getEpisodes(showId)
                        val watchedIds = watchedEpisodeIdsByShow[showId].orEmpty()
                        episodes.mapNotNull { episode ->
                            val airdate = episode.airdate ?: return@mapNotNull null
                            val epochDay = isoDateToEpochDay(airdate) ?: return@mapNotNull null
                            if (epochDay < minDay || epochDay > maxDay) return@mapNotNull null
                            UpcomingEpisodeItem(
                                episode = episode,
                                show = show,
                                epochDay = epochDay,
                                isWatched = episode.id in watchedIds,
                            )
                        }
                    }.getOrNull().orEmpty()
                }
                items += results.flatten()

                val grouped = items
                    .sortedWith(compareBy({ it.epochDay }, { it.episode.airtime ?: "99:99" }, { it.show.name }))
                    .groupBy { it.epochDay }
                    .toList()
                    .sortedBy { it.first }

                val hasMore = chunkIndex < chunks.lastIndex
                _uiState.update {
                    it.copy(
                        isLoadingUpcoming = false,
                        isRefreshingUpcoming = false,
                        isLoadingMoreUpcoming = hasMore,
                        upcomingByDay = grouped,
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoadingUpcoming = false,
                    isRefreshingUpcoming = false,
                    isLoadingMoreUpcoming = false,
                    upcomingErrorMessage = e.message ?: "Errore nel caricamento",
                )
            }
        }
    }

    fun toggleEpisodeWatched(item: UpcomingEpisodeItem) {
        viewModelScope.launch {
            userShowsRepository.setEpisodeWatched(
                uid = uid,
                showId = item.show.id,
                episodeId = item.episode.id,
                season = item.episode.season,
                number = item.episode.number,
                watched = !item.isWatched,
            )
        }
    }
}
