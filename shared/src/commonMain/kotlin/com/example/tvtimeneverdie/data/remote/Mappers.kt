package com.example.tvtimeneverdie.data.remote

import com.example.tvtimeneverdie.data.remote.dto.EpisodeDto
import com.example.tvtimeneverdie.data.remote.dto.ShowDto
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.domain.model.Show

private val HTML_TAG_REGEX = Regex("<[^>]*>")

fun String?.stripHtml(): String = this?.replace(HTML_TAG_REGEX, "")?.trim().orEmpty()

fun ShowDto.toDomain(): Show = Show(
    id = id,
    name = name,
    imageUrl = image?.original ?: image?.medium,
    summary = summary.stripHtml(),
    genres = genres,
    status = status,
    premiered = premiered,
    rating = rating?.average,
    network = network?.name ?: webChannel?.name,
)

fun EpisodeDto.toDomain(showId: Int): Episode = Episode(
    id = id,
    showId = showId,
    season = season,
    number = number,
    name = name,
    airdate = airdate,
    airtime = airtime,
    imageUrl = image?.original ?: image?.medium,
    summary = summary.stripHtml(),
)
