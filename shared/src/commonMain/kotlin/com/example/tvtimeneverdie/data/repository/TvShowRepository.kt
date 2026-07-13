package com.example.tvtimeneverdie.data.repository

import com.example.tvtimeneverdie.data.remote.JIKAN_SHOW_ID_OFFSET
import com.example.tvtimeneverdie.data.remote.JikanApi
import com.example.tvtimeneverdie.data.remote.TvMazeApi
import com.example.tvtimeneverdie.data.remote.isJikanShowId
import com.example.tvtimeneverdie.data.remote.placeholderJikanEpisode
import com.example.tvtimeneverdie.data.remote.toDomain
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.domain.model.Show
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TvShowRepository(
    private val api: TvMazeApi = TvMazeApi(),
    private val jikanApi: JikanApi = JikanApi(),
) {
    private val showCache = mutableMapOf<Int, Show>()
    private val episodesCache = mutableMapOf<Int, List<Episode>>()
    private val mutex = Mutex()

    suspend fun getRecentShows(limit: Int = 30): List<Show> {
        val ids = api.getRecentlyUpdatedShowIds(limit)
        return ids.mapNotNull { id -> runCatching { getShow(id) }.getOrNull() }
    }

    suspend fun getShow(showId: Int): Show {
        mutex.withLock { showCache[showId] }?.let { return it }
        val show = if (isJikanShowId(showId)) {
            jikanApi.getAnime(showId - JIKAN_SHOW_ID_OFFSET).toDomain()
        } else {
            api.getShow(showId).toDomain()
        }
        mutex.withLock { showCache[showId] = show }
        return show
    }

    suspend fun getEpisodes(showId: Int): List<Episode> {
        mutex.withLock { episodesCache[showId] }?.let { return it }
        val episodes = if (isJikanShowId(showId)) {
            val malId = showId - JIKAN_SHOW_ID_OFFSET
            val fetched = jikanApi.getEpisodes(malId).map { it.toDomain(malId) }
            fetched.ifEmpty { placeholderEpisodesFromTotalCount(showId, malId) }
        } else {
            api.getEpisodes(showId).map { it.toDomain(showId) }
        }
        mutex.withLock { episodesCache[showId] = episodes }
        return episodes
    }

    /**
     * Jikan spesso conosce il numero totale di episodi di un anime (campo "episodes" della
     * risposta anime) anche quando il database episodio-per-episodio (community MAL) è vuoto o
     * incompleto: in quel caso generiamo episodi segnaposto numerati invece di mostrare una lista
     * vuota (vedi episodi mancanti per anime poco mainstream come "Black Torch").
     */
    private suspend fun placeholderEpisodesFromTotalCount(showId: Int, malId: Int): List<Episode> {
        val totalEpisodes = mutex.withLock { showCache[showId] }?.episodeCount
            ?: runCatching { jikanApi.getAnime(malId).episodes }.getOrNull()
        return totalEpisodes
            ?.takeIf { it > 0 }
            ?.let { count -> (1..count).map { placeholderJikanEpisode(malId, it) } }
            ?: emptyList()
    }

    /** Svuota la cache in memoria, forzando un refetch da rete alla prossima chiamata (usato dal pull-to-refresh). */
    suspend fun clearCache() {
        mutex.withLock {
            showCache.clear()
            episodesCache.clear()
        }
    }

    /** Cerca su TVmaze e su Jikan (fallback per gli anime, poco coperti da TVmaze) e unisce i risultati. */
    suspend fun searchShows(query: String): List<Show> {
        if (query.isBlank()) return emptyList()
        val tvMazeResults = runCatching { api.searchShows(query).map { it.show.toDomain() } }.getOrDefault(emptyList())
        val jikanResults = runCatching { jikanApi.searchAnime(query).map { it.toDomain() } }.getOrDefault(emptyList())
        val existingNames = tvMazeResults.map { it.name.lowercase() }.toSet()
        val dedupedJikan = jikanResults.filter { it.name.lowercase() !in existingNames }
        return tvMazeResults + dedupedJikan
    }

    /**
     * Trova la corrispondenza migliore per un titolo esatto (usato dall'import GDPR): la ricerca
     * TVmaze restituisce quasi sempre un risultato anche debole/sbagliato (fuzzy matching), quindi
     * un semplice "primo risultato" penalizzerebbe gli anime coperti solo da Jikan. Si preferisce
     * prima un nome identico (case-insensitive) su TVmaze, poi su Jikan, e solo come ultima risorsa
     * si accetta il primo risultato disponibile da una delle due fonti.
     */
    suspend fun findBestMatch(query: String): Show? {
        if (query.isBlank()) return null
        val normalizedQuery = query.trim().lowercase()
        val tvMazeResults = runCatching { api.searchShows(query).map { it.show.toDomain() } }.getOrDefault(emptyList())
        tvMazeResults.firstOrNull { it.name.trim().lowercase() == normalizedQuery }?.let { return it }

        val jikanResults = runCatching { jikanApi.searchAnime(query).map { it.toDomain() } }.getOrDefault(emptyList())
        jikanResults.firstOrNull { it.name.trim().lowercase() == normalizedQuery }?.let { return it }

        return tvMazeResults.firstOrNull() ?: jikanResults.firstOrNull()
    }
}
