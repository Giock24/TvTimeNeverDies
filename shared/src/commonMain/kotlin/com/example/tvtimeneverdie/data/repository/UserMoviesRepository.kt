package com.example.tvtimeneverdie.data.repository

import com.example.tvtimeneverdie.util.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class MovieWatchlistEntry(
    val movieId: Int = 0,
    val addedAtMillis: Long = 0,
)

@Serializable
data class WatchedMovieEntry(
    val movieId: Int = 0,
    val watchedAtMillis: Long = 0,
)

class UserMoviesRepository {
    private val firestore = Firebase.firestore

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun watchlistCollection(uid: String) = userDoc(uid).collection("movieWatchlist")
    private fun watchedCollection(uid: String) = userDoc(uid).collection("watchedMovies")

    suspend fun addToWatchlist(uid: String, movieId: Int) {
        watchlistCollection(uid).document(movieId.toString())
            .set(MovieWatchlistEntry(movieId = movieId, addedAtMillis = currentTimeMillis()))
    }

    suspend fun removeFromWatchlist(uid: String, movieId: Int) {
        watchlistCollection(uid).document(movieId.toString()).delete()
    }

    suspend fun markWatched(uid: String, movieId: Int) {
        watchedCollection(uid).document(movieId.toString())
            .set(WatchedMovieEntry(movieId = movieId, watchedAtMillis = currentTimeMillis()))
        removeFromWatchlist(uid, movieId)
    }

    suspend fun unmarkWatched(uid: String, movieId: Int) {
        watchedCollection(uid).document(movieId.toString()).delete()
    }

    fun watchlistIds(uid: String): Flow<Set<Int>> =
        watchlistCollection(uid).snapshots.map { snapshot ->
            snapshot.documents.map { it.data<MovieWatchlistEntry>().movieId }.toSet()
        }

    fun watchedIds(uid: String): Flow<Set<Int>> =
        watchedCollection(uid).snapshots.map { snapshot ->
            snapshot.documents.map { it.data<WatchedMovieEntry>().movieId }.toSet()
        }

    /** Tutti i film che l'utente ha segnato come visti, con la data in cui li ha visti. */
    fun watchedEntries(uid: String): Flow<List<WatchedMovieEntry>> =
        watchedCollection(uid).snapshots.map { snapshot ->
            snapshot.documents.map { it.data<WatchedMovieEntry>() }
        }
}
