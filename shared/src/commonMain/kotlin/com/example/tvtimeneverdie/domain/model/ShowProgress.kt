package com.example.tvtimeneverdie.domain.model

enum class ShowWatchStatus {
    WATCHLIST,
    WATCHING,
    COMPLETED,
}

data class ShowProgress(
    val show: Show,
    val watchedEpisodeCount: Int,
    val totalEpisodeCount: Int,
    val status: ShowWatchStatus,
    val lastWatchedAtMillis: Long? = null,
)
