package xyz.raidenhub.phim.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch

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
private val enterAnim = fadeIn(tween(300)) +
    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
    scaleIn(tween(300), initialScale = 0.94f)
private val exitAnim = fadeOut(tween(200)) +
    scaleOut(tween(200), targetScale = 0.96f)
private val popEnterAnim = fadeIn(tween(280)) +
    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 5 } +
    scaleIn(tween(280), initialScale = 0.96f)
private val popExitAnim = fadeOut(tween(220)) +
    slideOutHorizontally(tween(220)) { it / 5 }

// Detail screen — cinematic slide-up from card
private val detailEnterAnim = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
    slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 3 } +
    scaleIn(tween(380, easing = FastOutSlowInEasing), initialScale = 0.88f)
private val detailExitAnim = fadeOut(tween(250)) +
    scaleOut(tween(250), targetScale = 0.95f)
private val detailPopExitAnim = fadeOut(tween(300)) +
    slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
    scaleOut(tween(300), targetScale = 0.90f)

@OptIn(ExperimentalFoundationApi::class)
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

    // MU-1: Tab routes in swipe order
    val tabRoutes = remember { navItems.map { it.route } }
    val currentTabIndex = remember(currentRoute) {
        tabRoutes.indexOf(currentRoute).takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabRoutes.size })

    // MU-1 Fix: Dùng settledPage thay vì currentPage + isScrollInProgress
    // settledPage chỉ update khi animation HOÀN TOÀN xong → không có race condition
    LaunchedEffect(pagerState.settledPage) {
        val targetRoute = tabRoutes[pagerState.settledPage]
        // Đọc currentNavRoute trực tiếp từ navController (tránh stale closure)
        val currentNavRoute = navController.currentBackStackEntry?.destination?.route
        if (currentNavRoute != targetRoute && currentNavRoute in tabRoutes) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // MU-1: Sync NavController → pager khi tap bottom bar
    // Guard !isScrollInProgress: tránh fight khi user đang swipe ngược chiều
    LaunchedEffect(currentRoute) {
        val idx = tabRoutes.indexOf(currentRoute)
        if (idx >= 0 && idx != pagerState.settledPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(idx)
        }
    }

    // Hide bottom bar on non-tab screens
    val showBottomBar = currentRoute in tabRoutes
    val coroutineScope = rememberCoroutineScope()

    // MU-1: Helper navigate tab by index (dùng cho swipe bottom bar)
    fun navigateToTabIndex(idx: Int) {
        val route = tabRoutes.getOrNull(idx) ?: return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        coroutineScope.launch { pagerState.animateScrollToPage(idx) }
    }

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
                    },
                    onSwipeLeft = {
                        val next = (pagerState.settledPage + 1).coerceAtMost(tabRoutes.lastIndex)
                        navigateToTabIndex(next)
                    },
                    onSwipeRight = {
                        val prev = (pagerState.settledPage - 1).coerceAtLeast(0)
                        navigateToTabIndex(prev)
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

            // MU-1 (P2): Tất cả tab routes share 1 HorizontalPager instance — không destroy-recreate
            composable(Screen.Home.route) {
                MainTabsContent(
                    pagerState = pagerState,
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep, positionMs, source -> startPlayerActivity(slug, server, ep, positionMs, source) },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) },
                    onSuperStreamItemClick = { tmdbId, type -> navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type)) },
                    onBack = { navController.popBackStack() },
                    onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistList.route) }
                )
            }
            composable(Screen.SuperStream.route) {
                MainTabsContent(
                    pagerState = pagerState,
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep, positionMs, source -> startPlayerActivity(slug, server, ep, positionMs, source) },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) },
                    onSuperStreamItemClick = { tmdbId, type -> navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type)) },
                    onBack = { navController.popBackStack() },
                    onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistList.route) }
                )
            }
            composable(Screen.Search.route) {
                MainTabsContent(
                    pagerState = pagerState,
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep, positionMs, source -> startPlayerActivity(slug, server, ep, positionMs, source) },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) },
                    onSuperStreamItemClick = { tmdbId, type -> navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type)) },
                    onBack = { navController.popBackStack() },
                    onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistList.route) }
                )
            }

            // WatchHistory + Settings routes (tab 3 & 4 also served via pager when accessed directly)
            composable(Screen.WatchHistory.route) {
                MainTabsContent(
                    pagerState = pagerState,
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep, positionMs, source -> startPlayerActivity(slug, server, ep, positionMs, source) },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) },
                    onSuperStreamItemClick = { tmdbId, type -> navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type)) },
                    onBack = { navController.popBackStack() },
                    onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistList.route) }
                )
            }
            composable(Screen.Settings.route) {
                MainTabsContent(
                    pagerState = pagerState,
                    onMovieClick = { slug -> navController.navigate(Screen.Detail.createRoute(slug)) },
                    onContinue = { slug, server, ep, positionMs, source -> startPlayerActivity(slug, server, ep, positionMs, source) },
                    onCategoryClick = { s, title -> navController.navigate(Screen.Category.createRoute(s, title)) },
                    onSuperStreamItemClick = { tmdbId, type -> navController.navigate(Screen.SuperStreamDetail.createRoute(tmdbId, type)) },
                    onBack = { navController.popBackStack() },
                    onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
                    onPlaylistClick = { navController.navigate(Screen.PlaylistList.route) }
                )
            }

            composable(
                Screen.Detail.route,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                enterTransition = { detailEnterAnim },
                exitTransition = { detailExitAnim },
                popEnterTransition = { popEnterAnim },
                popExitTransition = { detailPopExitAnim }
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

            // SuperStreamDetail — non-tab fullscreen route
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

// P2: Single HorizontalPager instance shared across all tab routes
// Tr\u01b0\u1edbc \u0111\u00e2y m\u1ed7i route t\u1ea1o 1 Pager ri\u00eang \u2192 destroy-recreate khi swipe tab
// Gi\u1edd t\u1ea5t c\u1ea3 tab routes render c\u00f9ng 1 composable n\u00e0y
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainTabsContent(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onSuperStreamItemClick: (tmdbId: Int, type: String) -> Unit,
    onBack: () -> Unit,
    onWatchlistClick: () -> Unit = {},
    onPlaylistClick: () -> Unit = {}
) {
    // MU-1: userScrollEnabled=false — swipe chuyển tab chỉ ở bottom nav bar (không phải full-screen)
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
        beyondViewportPageCount = 1
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                onMovieClick = onMovieClick,
                onContinue = onContinue,
                onCategoryClick = onCategoryClick
            )
            1 -> SuperStreamScreen(
                onItemClick = onSuperStreamItemClick,
                onBack = onBack
            )
            2 -> SearchScreen(onMovieClick = onMovieClick)
            3 -> WatchHistoryScreen(
                onBack = onBack,
                onMovieClick = onMovieClick,
                onContinue = { slug, server, ep, source -> onContinue(slug, server, ep, 0L, source) },
                onWatchlistClick = onWatchlistClick,
                onPlaylistClick = onPlaylistClick
            )
            4 -> SettingsScreen()
            else -> HomeScreen(
                onMovieClick = onMovieClick,
                onContinue = onContinue,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun GlassBottomNav(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onSwipeLeft: () -> Unit = {},   // swipe trái → tab tiếp theo
    onSwipeRight: () -> Unit = {}   // swipe phải → tab trước
) {
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 48.dp.toPx() }

    // Glass container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC0D0D1A))  // 80% opaque — frosted effect
            .navigationBarsPadding()
            // MU-1: Swipe gesture chỉ trên bottom nav bar
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        when {
                            totalDragX < -swipeThresholdPx -> onSwipeLeft()   // ← next tab
                            totalDragX > swipeThresholdPx  -> onSwipeRight()  // → prev tab
                        }
                        totalDragX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount }
                )
            }
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
    val iconScale = 1f  // No scale animation — chỉ color đổi khi chuyển tab
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

        // Label luôn hiện, chỉ đổi màu — zero layout shift
        val labelColor by animateColorAsState(
            targetValue = if (isSelected) C.Primary else C.TextSecondary,
            animationSpec = tween(250),
            label = "nav_label_color"
        )
        Text(
            item.label,
            color = labelColor,
            fontFamily = InterFamily,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
