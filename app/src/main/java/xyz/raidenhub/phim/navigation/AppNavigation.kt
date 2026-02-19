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
import xyz.raidenhub.phim.EnglishPlayerActivity
import xyz.raidenhub.phim.ui.screens.anime.AnimeScreen
import xyz.raidenhub.phim.ui.screens.category.CategoryScreen
import xyz.raidenhub.phim.ui.screens.detail.DetailScreen
import xyz.raidenhub.phim.ui.screens.history.WatchHistoryScreen
import xyz.raidenhub.phim.ui.screens.home.HomeScreen
import xyz.raidenhub.phim.ui.screens.search.SearchScreen
import xyz.raidenhub.phim.ui.screens.english.EnglishScreen
import xyz.raidenhub.phim.ui.screens.english.EnglishDetailScreen
import xyz.raidenhub.phim.ui.screens.settings.SettingsScreen
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
    fun startPlayerActivity(slug: String, server: Int, episode: Int) {
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra("slug", slug)
            putExtra("server", server)
            putExtra("episode", episode)
        })
    }

    // Helper: launch EnglishPlayerActivity
    fun startEnglishPlayerActivity(episodeId: String, mediaId: String, filmName: String) {
        context.startActivity(Intent(context, EnglishPlayerActivity::class.java).apply {
            putExtra("episodeId", episodeId)
            putExtra("mediaId", mediaId)
            putExtra("filmName", filmName)
        })
    }

    // Hide bottom bar on player/detail
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route, Screen.Search.route, Screen.Favorites.route,
        Screen.Settings.route, Screen.WatchHistory.route, Screen.Anime.route,
        Screen.English.route
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
                    // 🍿 English tab
                    NavigationBarItem(
                        selected = currentRoute == Screen.English.route,
                        onClick = {
                            navController.navigate(Screen.English.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text("🍿", fontSize = 20.sp) },
                        label = { Text("English", fontSize = 11.sp) },
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
                        if (slug.startsWith("eng:")) {
                            val mediaId = slug.removePrefix("eng:")
                            navController.navigate(Screen.EnglishDetail.createRoute(mediaId))
                        } else {
                            navController.navigate(Screen.Detail.createRoute(slug))
                        }
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

            // 🍿 English tab
            composable(Screen.English.route) {
                EnglishScreen(
                    onMovieClick = { mediaId ->
                        navController.navigate(Screen.EnglishDetail.createRoute(mediaId))
                    },
                    onSearch = { navController.navigate(Screen.EnglishSearch.route) }
                )
            }

            // English Detail → show detail for Consumet media
            composable(
                Screen.EnglishDetail.route,
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { entry ->
                val mediaId = java.net.URLDecoder.decode(
                    entry.arguments?.getString("mediaId") ?: "", "UTF-8"
                )
                EnglishDetailScreen(
                    mediaId = mediaId,
                    onBack = { navController.popBackStack() },
                    onPlay = { episodeId, mId, filmName ->
                        startEnglishPlayerActivity(episodeId, mId, filmName)
                    }
                )
            }

            // 🍿 English Player — giờ chạy ở EnglishPlayerActivity riêng

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
                    },
                    onContinueEnglish = { episodeId, mediaId, filmName ->
                        startEnglishPlayerActivity(episodeId, mediaId, filmName)
                    },
                    onEnglishDetailClick = { mediaId ->
                        navController.navigate(Screen.EnglishDetail.createRoute(mediaId))
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
                    onSeasonClick = { seasonSlug -> navController.navigate(Screen.Detail.createRoute(seasonSlug)) }
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
                    onPlay = { s, sv, ep -> startPlayerActivity(s, sv, ep) }
                )
            }

            // 🎬 Player — giờ chạy ở PlayerActivity riêng

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

            // #44 — English Search
            composable(Screen.EnglishSearch.route) {
                xyz.raidenhub.phim.ui.screens.english.EnglishSearchScreen(
                    onMovieClick = { mediaId ->
                        navController.navigate(Screen.EnglishDetail.createRoute(mediaId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
