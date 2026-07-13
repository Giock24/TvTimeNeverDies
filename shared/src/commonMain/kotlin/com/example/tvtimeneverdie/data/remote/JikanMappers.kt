package com.example.tvtimeneverdie.data.remote

import com.example.tvtimeneverdie.data.remote.dto.JikanAnimeDto
import com.example.tvtimeneverdie.data.remote.dto.JikanEpisodeDto
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.domain.model.Show

/**
 * Gli ID MAL (Jikan) e TVmaze sono entrambi interi piccoli assegnati in modo indipendente e si
 * sovrappongono quasi certamente. Per evitare collisioni nelle chiavi Firestore/cache/navigazione
 * (tutte basate su un semplice Int), gli ID Jikan vengono codificati con un offset fisso, ben oltre
 * la portata realistica degli ID TVmaze. Gli episodi Jikan sono numerati da 1 dentro ogni serie,
 * quindi la codifica combina anche l'ID della serie per restare unica globalmente.
 */
const val JIKAN_SHOW_ID_OFFSET = 10_000_000
private const val JIKAN_EPISODE_ID_OFFSET = 100_000_000
private const val JIKAN_EPISODE_SHOW_MULTIPLIER = 10_000

fun isJikanShowId(showId: Int): Boolean = showId >= JIKAN_SHOW_ID_OFFSET

fun JikanAnimeDto.toDomain(): Show = Show(
    id = JIKAN_SHOW_ID_OFFSET + malId,
    name = titleEnglish ?: title,
    imageUrl = images?.jpg?.largeImageUrl,
    summary = synopsis.orEmpty(),
    genres = genres.map { it.name },
    status = status,
    premiered = aired?.from?.take(10),
    rating = score,
    network = null,
    episodeCount = episodes,
)

fun JikanEpisodeDto.toDomain(showMalId: Int): Episode = Episode(
    id = JIKAN_EPISODE_ID_OFFSET + showMalId * JIKAN_EPISODE_SHOW_MULTIPLIER + malId,
    showId = JIKAN_SHOW_ID_OFFSET + showMalId,
    season = 1,
    number = malId,
    name = title,
    airdate = aired?.take(10),
    imageUrl = null,
    summary = "",
)

/**
 * Episodio segnaposto usato quando Jikan conosce il numero totale di episodi (campo "episodes"
 * della risposta anime) ma non ha i dati episodio-per-episodio: capita spesso per anime meno
 * mainstream, dove il database community di MAL è incompleto pur avendo il conteggio totale.
 */
fun placeholderJikanEpisode(showMalId: Int, number: Int): Episode = Episode(
    id = JIKAN_EPISODE_ID_OFFSET + showMalId * JIKAN_EPISODE_SHOW_MULTIPLIER + number,
    showId = JIKAN_SHOW_ID_OFFSET + showMalId,
    season = 1,
    number = number,
    name = "Episodio $number",
    airdate = null,
    imageUrl = null,
    summary = "",
)
