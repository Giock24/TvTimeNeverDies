package com.example.tvtimeneverdie.domain.model

data class Episode(
    val id: Int,
    val showId: Int,
    val season: Int,
    val number: Int?,
    val name: String,
    val airdate: String?,
    val airtime: String? = null,
    val imageUrl: String?,
    val summary: String,
)
