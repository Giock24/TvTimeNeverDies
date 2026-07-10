package com.example.tvtimeneverdie.ui.screens.serie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.tvtimeneverdie.domain.model.Episode
import com.example.tvtimeneverdie.domain.model.Show
import com.example.tvtimeneverdie.ui.components.ShowGridItem
import com.example.tvtimeneverdie.ui.rememberViewModel
import com.example.tvtimeneverdie.util.dateBucketLabel
import com.example.tvtimeneverdie.util.todayEpochDay

private val PremiereColor = Color(0xFFF5F5F5)
private val NuovoColor = Color(0xFFFFC107)

private enum class SerieTab { NOVITA, IN_ARRIVO }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerieScreen(uid: String, onShowClick: (Int) -> Unit, onEpisodeClick: (Episode) -> Unit) {
    val viewModel = rememberViewModel { SerieViewModel(uid) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(SerieTab.NOVITA) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Serie TV") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == SerieTab.NOVITA,
                    onClick = { selectedTab = SerieTab.NOVITA },
                    text = { Text("Novità") },
                )
                Tab(
                    selected = selectedTab == SerieTab.IN_ARRIVO,
                    onClick = { selectedTab = SerieTab.IN_ARRIVO },
                    text = { Text("In arrivo") },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    SerieTab.NOVITA -> NoviteGrid(
                        isLoading = state.isLoadingRecent,
                        shows = state.recentShows,
                        errorMessage = state.recentErrorMessage,
                        onShowClick = onShowClick,
                    )
                    SerieTab.IN_ARRIVO -> UpcomingList(
                        isLoading = state.isLoadingUpcoming,
                        isLoadingMore = state.isLoadingMoreUpcoming,
                        upcomingByDay = state.upcomingByDay,
                        errorMessage = state.upcomingErrorMessage,
                        onShowClick = onShowClick,
                        onEpisodeClick = onEpisodeClick,
                        onToggleWatched = viewModel::toggleEpisodeWatched,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoviteGrid(
    isLoading: Boolean,
    shows: List<Show>,
    errorMessage: String?,
    onShowClick: (Int) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            errorMessage != null -> Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shows, key = { it.id }) { show ->
                    ShowGridItem(
                        show = show,
                        onClick = { onShowClick(show.id) },
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingList(
    isLoading: Boolean,
    isLoadingMore: Boolean,
    upcomingByDay: List<Pair<Long, List<UpcomingEpisodeItem>>>,
    errorMessage: String?,
    onShowClick: (Int) -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    onToggleWatched: (UpcomingEpisodeItem) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            errorMessage != null -> Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            upcomingByDay.isEmpty() -> Text(
                text = "Nessun episodio in arrivo per le serie che segui",
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                val today = todayEpochDay()
                upcomingByDay.forEach { (epochDay, dayItems) ->
                    item(key = "header_$epochDay") {
                        DateBucketHeader(label = dateBucketLabel(epochDay, today))
                    }
                    items(dayItems, key = { it.episode.id }) { upcomingItem ->
                        UpcomingEpisodeRow(
                            item = upcomingItem,
                            isAired = epochDay <= today,
                            onClick = { onEpisodeClick(upcomingItem.episode) },
                            onShowClick = { onShowClick(upcomingItem.show.id) },
                            onToggleWatched = { onToggleWatched(upcomingItem) },
                        )
                    }
                }
                if (isLoadingMore) {
                    item(key = "loading_more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateBucketHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UpcomingEpisodeRow(
    item: UpcomingEpisodeItem,
    isAired: Boolean,
    onClick: () -> Unit,
    onShowClick: () -> Unit,
    onToggleWatched: () -> Unit,
) {
    val isPremiere = item.episode.number == 1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.episode.imageUrl ?: item.show.imageUrl,
            contentDescription = item.show.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 72.dp, height = 96.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onShowClick)
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = item.show.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "S${item.episode.season.toString().padStart(2, '0')} | " +
                    "E${(item.episode.number ?: 0).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.episode.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isPremiere || isAired) {
                Spacer(Modifier.height(6.dp))
                Row {
                    if (isPremiere) {
                        Badge(text = "PREMIERE", backgroundColor = PremiereColor, textColor = Color.Black)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (isAired) {
                        Badge(text = "NUOVO", backgroundColor = NuovoColor, textColor = Color.Black)
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        if (item.episode.airtime != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text(item.episode.airtime, style = MaterialTheme.typography.bodySmall)
                item.show.network?.let { network ->
                    Text(network, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        if (isAired) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (item.isWatched) Color.White else MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onToggleWatched),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = if (item.isWatched) "Segna da vedere" else "Segna visto",
                    tint = if (item.isWatched) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String, backgroundColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
