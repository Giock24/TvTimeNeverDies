package com.example.tvtimeneverdie.ui.screens.showdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.ui.rememberViewModel

private val SeasonProgressGreen = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: Int,
    uid: String,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
) {
    val viewModel = rememberViewModel { ShowDetailViewModel(showId, uid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedSeasons = remember { mutableStateMapOf<Int, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.show?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.errorMessage != null -> Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                state.show != null -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        ShowHeader(
                            imageUrl = state.show?.imageUrl,
                            summary = state.show?.summary.orEmpty(),
                            genres = state.show?.genres.orEmpty(),
                            status = state.show?.status,
                            network = state.show?.network,
                            rating = state.show?.rating,
                            isInWatchlist = state.isInWatchlist,
                            onToggleWatchlist = viewModel::toggleWatchlist,
                        )
                    }
                    if (state.episodesBySeason.isEmpty()) {
                        item {
                            Text(
                                text = "Nessun episodio disponibile per questa serie.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    state.episodesBySeason.forEach { (season, episodes) ->
                        item {
                            SeasonSection(
                                season = season,
                                episodes = episodes,
                                watchedEpisodeIds = state.watchedEpisodeIds,
                                expanded = expandedSeasons[season] ?: false,
                                onToggleExpanded = { expandedSeasons[season] = !(expandedSeasons[season] ?: false) },
                                onToggleSeasonWatched = { viewModel.toggleSeasonWatched(season) },
                                onToggleEpisodeWatched = { viewModel.toggleEpisodeWatched(it) },
                                onEpisodeClick = onEpisodeClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowHeader(
    imageUrl: String?,
    summary: String,
    genres: List<String>,
    status: String?,
    network: String?,
    rating: Double?,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                listOfNotNull(status, network, rating?.let { "Voto $it" })
                    .forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(genres.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onToggleWatchlist) {
                    Text(if (isInWatchlist) "Rimuovi da Da vedere" else "Aggiungi a Da vedere")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(summary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SeasonSection(
    season: Int,
    episodes: List<Episode>,
    watchedEpisodeIds: Set<Int>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleSeasonWatched: () -> Unit,
    onToggleEpisodeWatched: (Episode) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
) {
    val total = episodes.size
    val watchedCount = episodes.count { it.id in watchedEpisodeIds }
    val allWatched = total > 0 && watchedCount == total
    val progress by animateFloatAsState(
        targetValue = if (total > 0) watchedCount / total.toFloat() else 0f,
        label = "seasonProgress",
    )
    val chevronRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevronRotation")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.rotate(chevronRotation),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Stagione $season",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$watchedCount/$total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (allWatched) SeasonProgressGreen else Color.Transparent)
                    .border(
                        width = if (allWatched) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape,
                    )
                    .clickable(onClick = onToggleSeasonWatched),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = if (allWatched) "Segna stagione da vedere" else "Segna stagione come vista",
                    tint = if (allWatched) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(SeasonProgressGreen),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                episodes.forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        isWatched = episode.id in watchedEpisodeIds,
                        onToggleWatched = { onToggleEpisodeWatched(episode) },
                        onClick = { onEpisodeClick(episode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    isWatched: Boolean,
    onToggleWatched: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isWatched, onCheckedChange = { onToggleWatched() })
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        ) {
            val number = episode.number?.let { "Ep. $it" } ?: "Speciale"
            Text("$number - ${episode.name}", style = MaterialTheme.typography.bodyLarge)
            episode.airdate?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
