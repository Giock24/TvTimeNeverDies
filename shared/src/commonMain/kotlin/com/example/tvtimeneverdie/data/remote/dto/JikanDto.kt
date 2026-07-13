package com.example.tvtimeneverdie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanAnimeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    @SerialName("title_english") val titleEnglish: String? = null,
    val images: JikanImagesDto? = null,
    val synopsis: String? = null,
    val status: String? = null,
    val score: Double? = null,
    val aired: JikanAiredDto? = null,
    val genres: List<JikanGenreDto> = emptyList(),
    val episodes: Int? = null,
)

@Serializable
data class JikanImagesDto(
    val jpg: JikanImageVariantDto? = null,
)

@Serializable
data class JikanImageVariantDto(
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)

@Serializable
data class JikanAiredDto(
    val from: String? = null,
)

@Serializable
data class JikanGenreDto(
    val name: String,
)

@Serializable
data class JikanSearchResponseDto(
    val data: List<JikanAnimeDto> = emptyList(),
)

@Serializable
data class JikanAnimeSingleResponseDto(
    val data: JikanAnimeDto,
)

@Serializable
data class JikanEpisodeDto(
    @SerialName("mal_id") val malId: Int,
    val title: String,
    val aired: String? = null,
)

@Serializable
data class JikanPaginationDto(
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)

@Serializable
data class JikanEpisodesResponseDto(
    val data: List<JikanEpisodeDto> = emptyList(),
    val pagination: JikanPaginationDto? = null,
)
