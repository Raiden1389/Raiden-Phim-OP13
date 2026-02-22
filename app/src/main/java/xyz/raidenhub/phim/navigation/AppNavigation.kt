package xyz.raidenhub.phim.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import xyz.raidenhub.phim.PlayerActivity
import xyz.raidenhub.phim.ui.screens.category.CategoryScreen
import xyz.raidenhub.phim.ui.screens.detail.DetailScreen
import xyz.raidenhub.phim.ui.screens.genre.GenreHubScreen
import xyz.raidenhub.phim.ui.screens.history.WatchHistoryScreen
import xyz.raidenhub.phim.ui.screens.home.HomeScreen
import xyz.raidenhub.phim.ui.screens.search.SearchScreen
import xyz.raidenhub.phim.ui.screens.settings.SettingsScreen
import xyz.raidenhub.phim.ui.screens.splash.SplashScreen
import xyz.raidenhub.phim.ui.screens.superstream.SuperStreamScreen
import xyz.raidenhub.phim.ui.screens.superstream.SuperStreamDetailScreen
import xyz.raidenhub.phim.ui.screens.watchlist.PlaylistDetailScreen
import xyz.raidenhub.phim.ui.screens.watchlist.PlaylistListScreen
import xyz.raidenhub.phim.ui.screens.watchlist.WatchlistScreen
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily


// #39 — Refined transition specs (premium feel)
private val enterAnim = fadeIn(tween(350)) +
    slideInHorizontally(tween(350, easing = FastOutSlowInEasing)) { it / 5 } +
    scaleIn(tween(350), initialScale = 0.92f)
private val exitAnim = fadeOut(tween(200)) +
    scaleOut(tween(200), targetScale = 0.95f)
private val popEnterAnim = fadeIn(tween(300)) +
    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 5 } +
    scaleIn(tween(300), initialScale = 0.95f)
private val popExitAnim = fadeOut(tween(250)) +
    slideOutHorizontally(tween(250)) { it / 5 }

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // Helper: launch PlayerActivity
    fun startPlayerActivity(slug: String, server: Int, episode: Int, positionMs: Long = 0L, source: String = "kkphim") {
        context.startActivity(Intent(context, PlayerActivity::class.java).apply {
            putExtra("slug", slug)
            putExtra("server", server)
            putExtra("episode", episode)
            putExtra("positionMs", positionMs)
            putExtra("source", source)
        })
    }


    // Hide bottom bar on non-tab screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route, Screen.Search.route, Screen.Favorites.route,
        Screen.Settings.route, Screen.WatchHistory.route,
        Screen.SuperStream.route
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
                // ═══ Glassmorphism Bottom Nav — frosted glass look ═══
                GlassBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
            // #39 — Animated transitions
            enterTransition = { enterAnim },
            exitTransition = { exitAnim },
            popEnterTransition = { popEnterAnim },
            popExitTransition = { popExitAnim }
        ) {
            // CN-2: Splash — no bottom bar, auto-navigate to Home
            composable(
                Screen.Splash.route,
                enterTransition = { fadeIn(tween(300)) },
                exitTransition = { fadeOut(tween(400)) }
            ) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onMovieClick = { slug ->
                        navController.navigate(Screen.Detail.createRoute(slug))
                    },
                    onContinue = { slug, server, ep, positionMs, source ->
                        startPlayerActivity(slug, server, ep, positionMs, source)
                    },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) }
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
                    onContinue = { slug, server, ep, source ->
                        startPlayerActivity(slug, server, ep, source = source)
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

            // ═══ SuperStream (English content) ═══
            composable(Screen.SuperStream.route) {
                SuperStreamScreen(
                    onItemClick = { tmdbId, type ->
                        navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.SuperStreamDetail.route,
                arguments = listOf(
                    navArgument("tmdbId") { type = NavType.IntType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { entry ->
                SuperStreamDetailScreen(
                    tmdbId = entry.arguments?.getInt("tmdbId") ?: 0,
                    type = entry.arguments?.getString("type") ?: "movie",
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// ═══ Glassmorphism Bottom Nav Bar ═══

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val emoji: String? = null
)

private val navItems = listOf(
    NavItem(Screen.Home.route, "Phim", Icons.Default.Home),
    NavItem(Screen.SuperStream.route, "English", emoji = "🌍"),
    NavItem(Screen.Search.route, "Tìm", Icons.Default.Search),
    NavItem(Screen.WatchHistory.route, "Lịch sử", Icons.Default.History),
    NavItem(Screen.Settings.route, "Cài đặt", Icons.Default.Settings),
)

@Composable
private fun GlassBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    // Glass container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC0D0D1A))  // 80% opaque — frosted effect
            .navigationBarsPadding()
    ) {
        // Subtle top border glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(C.Primary.copy(alpha = 0.3f))
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                GlassNavItem(
                    item = item,
                    isSelected = currentRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun GlassNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "nav_scale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) C.Primary else C.TextSecondary,
        animationSpec = tween(250),
        label = "nav_color"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.15f else 0f,
        animationSpec = tween(250),
        label = "nav_bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(C.Primary.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (item.icon != null) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (item.emoji != null) {
                Text(item.emoji, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(2.dp))

        // Animated label
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Text(
                item.label,
                color = C.Primary,
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        if (!isSelected) {
            Text(
                item.label,
                color = C.TextMuted,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
