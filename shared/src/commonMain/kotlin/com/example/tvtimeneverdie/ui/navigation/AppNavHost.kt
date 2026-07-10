package com.example.tvtimeneverdie.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tvtimeneverdie.domain.model.AuthUser
import com.example.tvtimeneverdie.ui.screens.episodedetail.EpisodeDetailScreen
import com.example.tvtimeneverdie.ui.screens.film.FilmScreen
import com.example.tvtimeneverdie.ui.screens.moviedetail.MovieDetailScreen
import com.example.tvtimeneverdie.ui.screens.profile.ProfileScreen
import com.example.tvtimeneverdie.ui.screens.search.SearchScreen
import com.example.tvtimeneverdie.ui.screens.serie.SerieScreen
import com.example.tvtimeneverdie.ui.screens.showdetail.ShowDetailScreen

@Composable
fun MainScreen(authUser: AuthUser) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val isTopLevelDestination = currentDestination?.hasRoute(SerieDestination::class) == true ||
        currentDestination?.hasRoute(FilmDestination::class) == true ||
        currentDestination?.hasRoute(SearchDestination::class) == true ||
        currentDestination?.hasRoute(ProfileDestination::class) == true

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination.hasRoute(SerieDestination::class) == true,
                        onClick = { navController.navigate(SerieDestination) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Tv, contentDescription = null) },
                        label = { Text("Serie") },
                    )
                    NavigationBarItem(
                        selected = currentDestination.hasRoute(FilmDestination::class) == true,
                        onClick = { navController.navigate(FilmDestination) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Movie, contentDescription = null) },
                        label = { Text("Film") },
                    )
                    NavigationBarItem(
                        selected = currentDestination.hasRoute(SearchDestination::class) == true,
                        onClick = { navController.navigate(SearchDestination) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("Cerca") },
                    )
                    NavigationBarItem(
                        selected = currentDestination.hasRoute(ProfileDestination::class) == true,
                        onClick = { navController.navigate(ProfileDestination) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profilo") },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ProfileDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable<SerieDestination> {
                SerieScreen(
                    uid = authUser.uid,
                    onShowClick = { showId -> navController.navigate(ShowDetailDestination(showId)) },
                    onEpisodeClick = { episode ->
                        navController.navigate(
                            EpisodeDetailDestination(
                                episodeId = episode.id,
                                showId = episode.showId,
                                season = episode.season,
                                number = episode.number,
                            ),
                        )
                    },
                )
            }
            composable<FilmDestination> {
                FilmScreen(
                    onMovieClick = { movieId -> navController.navigate(MovieDetailDestination(movieId)) },
                )
            }
            composable<SearchDestination> {
                SearchScreen(
                    onShowClick = { showId -> navController.navigate(ShowDetailDestination(showId)) },
                    onMovieClick = { movieId -> navController.navigate(MovieDetailDestination(movieId)) },
                )
            }
            composable<ProfileDestination> {
                ProfileScreen(
                    uid = authUser.uid,
                    email = authUser.email,
                    displayName = authUser.displayName,
                    onShowClick = { showId -> navController.navigate(ShowDetailDestination(showId)) },
                    onMovieClick = { movieId -> navController.navigate(MovieDetailDestination(movieId)) },
                )
            }
            composable<MovieDetailDestination> { entry ->
                val route: MovieDetailDestination = entry.toRoute()
                MovieDetailScreen(
                    movieId = route.movieId,
                    uid = authUser.uid,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<ShowDetailDestination> { entry ->
                val route: ShowDetailDestination = entry.toRoute()
                ShowDetailScreen(
                    showId = route.showId,
                    uid = authUser.uid,
                    onBack = { navController.popBackStack() },
                    onEpisodeClick = { episode ->
                        navController.navigate(
                            EpisodeDetailDestination(
                                episodeId = episode.id,
                                showId = episode.showId,
                                season = episode.season,
                                number = episode.number,
                            ),
                        )
                    },
                )
            }
            composable<EpisodeDetailDestination> { entry ->
                val route: EpisodeDetailDestination = entry.toRoute()
                EpisodeDetailScreen(
                    episodeId = route.episodeId,
                    showId = route.showId,
                    uid = authUser.uid,
                    displayName = authUser.displayName ?: authUser.email ?: "Utente",
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
