package com.example.tvtimeneverdie.domain.model

data class Show(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val summary: String,
    val genres: List<String>,
    val status: String?,
    val premiered: String?,
    val rating: Double?,
    val network: String?,
    val episodeCount: Int? = null,
)
