package xyz.raidenhub.phim.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import xyz.raidenhub.phim.PlayerActivity
import xyz.raidenhub.phim.ui.screens.anime.AnimeScreen
import xyz.raidenhub.phim.ui.screens.category.CategoryScreen
import xyz.raidenhub.phim.ui.screens.detail.DetailScreen
import xyz.raidenhub.phim.ui.screens.genre.GenreHubScreen
import xyz.raidenhub.phim.ui.screens.history.WatchHistoryScreen
import xyz.raidenhub.phim.ui.screens.home.HomeScreen
import xyz.raidenhub.phim.ui.screens.search.SearchScreen
import xyz.raidenhub.phim.ui.screens.settings.SettingsScreen
import xyz.raidenhub.phim.ui.screens.watchlist.PlaylistDetailScreen
import xyz.raidenhub.phim.ui.screens.watchlist.PlaylistListScreen
import xyz.raidenhub.phim.ui.screens.watchlist.WatchlistScreen
import xyz.raidenhub.phim.ui.theme.C

// #39 — Animated transition specs
private val enterAnim = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 }
private val exitAnim = fadeOut(tween(200))
private val popEnterAnim = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 }
private val popExitAnim = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { it / 4 }

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Helper: launch PlayerActivity
    fun startPlayerActivity(slug: String, server: Int, episode: Int, positionMs: Long = 0L) {
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra("slug", slug)
            putExtra("server", server)
            putExtra("episode", episode)
            putExtra("positionMs", positionMs)
            putExtra("source", "kkphim")
        })
    }

    // Hướng B — Anime47 player
    fun startAnime47PlayerActivity(episodeIds: IntArray, epIdx: Int, title: String) {
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra("source", "anime47")
            putExtra("episodeIds", episodeIds)
            putExtra("episode", epIdx)
            putExtra("animeTitle", title)
        })
    }

    // Hide bottom bar on non-tab screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route, Screen.Search.route, Screen.Favorites.route,
        Screen.Settings.route, Screen.WatchHistory.route, Screen.Anime.route
    )

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = C.Primary,
        selectedTextColor = C.Primary,
        indicatorColor = C.Primary.copy(0.15f),
        unselectedIconColor = C.TextSecondary,
        unselectedTextColor = C.TextSecondary
    )

    Scaffold(
        containerColor = C.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = C.Surface) {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Phim", fontSize = 11.sp) },
                        colors = navColors
                    )
                    // 🎌 Anime tab
                    NavigationBarItem(
                        selected = currentRoute == Screen.Anime.route,
                        onClick = {
                            navController.navigate(Screen.Anime.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text("🎌", fontSize = 20.sp) },
                        label = { Text("Anime", fontSize = 11.sp) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Search.route,
                        onClick = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Search, "Search") },
                        label = { Text("Tìm kiếm", fontSize = 11.sp) },
                        colors = navColors
                    )
                    // #36 — Watch History tab
                    NavigationBarItem(
                        selected = currentRoute == Screen.WatchHistory.route,
                        onClick = {
                            navController.navigate(Screen.WatchHistory.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, "History") },
                        label = { Text("Lịch sử", fontSize = 11.sp) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, "Settings") },
                        label = { Text("Cài đặt", fontSize = 11.sp) },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            // #39 — Animated transitions
            enterTransition = { enterAnim },
            exitTransition = { exitAnim },
            popEnterTransition = { popEnterAnim },
            popExitTransition = { popExitAnim }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onMovieClick = { slug ->
                        navController.navigate(Screen.Detail.createRoute(slug))
                    },
                    onContinue = { slug, server, ep, positionMs ->
                        startPlayerActivity(slug, server, ep, positionMs)
                    },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) }
                )
            }

            // 🎌 Anime tab
            composable(Screen.Anime.route) {
                AnimeScreen(
                    onAnimeClick = { id, slug ->
                        navController.navigate(Screen.AnimeDetail.createRoute(id, slug))
                    }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) }
                )
            }

            // #36 — Watch History screen
            composable(Screen.WatchHistory.route) {
                WatchHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep ->
                        startPlayerActivity(slug, server, ep)
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                Screen.Detail.route,
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { entry ->
                val slug = entry.arguments?.getString("slug") ?: ""
                DetailScreen(
                    slug = slug,
                    onBack = { navController.popBackStack() },
                    onPlay = { s, sv, ep -> startPlayerActivity(s, sv, ep) },
                    onSeasonClick = { seasonSlug -> navController.navigate(Screen.Detail.createRoute(seasonSlug)) },
                    onMovieClick = { movieSlug -> navController.navigate(Screen.Detail.createRoute(movieSlug)) }
                )
            }

            // #45 — AnimeDetail → Anime47 API detail
            composable(
                Screen.AnimeDetail.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType },
                    navArgument("slug") { type = NavType.StringType }
                )
            ) { entry ->
                val id = entry.arguments?.getInt("id") ?: 0
                val slug = entry.arguments?.getString("slug") ?: ""
            xyz.raidenhub.phim.ui.screens.anime.AnimeDetailScreen(
                    animeId = id,
                    slug = slug,
                    onBack = { navController.popBackStack() },
                    onPlayAnime47 = { ids, epIdx, title ->
                        startAnime47PlayerActivity(ids, epIdx, title)
                    }
                )
            }

            composable(
                Screen.Category.route,
                arguments = listOf(
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { entry ->
                CategoryScreen(
                    slug = entry.arguments?.getString("slug") ?: "",
                    title = entry.arguments?.getString("title") ?: "",
                    onBack = { navController.popBackStack() },
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) }
                )
            }

            // C-4: Watchlist (Xem Sau)
            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onBack = { navController.popBackStack() },
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) }
                )
            }

            // C-5: Playlist list
            composable(Screen.PlaylistList.route) {
                PlaylistListScreen(
                    onBack = { navController.popBackStack() },
                    onPlaylistClick = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    }
                )
            }

            // C-5: Playlist detail
            composable(
                Screen.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { entry ->
                PlaylistDetailScreen(
                    playlistId = entry.arguments?.getString("id") ?: "",
                    onBack = { navController.popBackStack() },
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) }
                )
            }

            // C-2: Genre Hub
            composable(Screen.GenreHub.route) {
                GenreHubScreen(
                    onBack = { navController.popBackStack() },
                    onGenreClick = { slug, name ->
                        navController.navigate(Screen.Category.createRoute(slug, name))
                    }
                )
            }
        }
    }
}
