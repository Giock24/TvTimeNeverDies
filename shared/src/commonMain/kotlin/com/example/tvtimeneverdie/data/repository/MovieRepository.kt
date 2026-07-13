package com.example.tvtimeneverdie.data.repository

import com.example.tvtimeneverdie.data.remote.TmdbApi
import com.example.tvtimeneverdie.data.remote.toDomain
import com.example.tvtimeneverdie.domain.model.Movie
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MovieRepository(
    private val api: TmdbApi = TmdbApi(),
) {
    private val movieCache = mutableMapOf<Int, Movie>()
    private val mutex = Mutex()

    suspend fun getNowPlaying(): List<Movie> =
        api.getNowPlaying().map { it.toDomain() }.also { movies ->
            mutex.withLock { movies.forEach { movieCache[it.id] = it } }
        }

    suspend fun getMovie(movieId: Int): Movie {
        mutex.withLock { movieCache[movieId] }?.let { return it }
        val movie = api.getMovie(movieId).toDomain()
        mutex.withLock { movieCache[movieId] = movie }
        return movie
    }

    suspend fun searchMovies(query: String): List<Movie> {
        if (query.isBlank()) return emptyList()
        return api.searchMovies(query).map { it.toDomain() }.also { movies ->
            mutex.withLock { movies.forEach { movieCache[it.id] = it } }
        }
    }

    /** Svuota la cache in memoria, forzando un refetch da rete alla prossima chiamata (usato dal pull-to-refresh). */
    suspend fun clearCache() {
        mutex.withLock { movieCache.clear() }
    }
}
