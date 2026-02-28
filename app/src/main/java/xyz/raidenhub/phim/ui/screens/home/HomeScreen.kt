package xyz.raidenhub.phim.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C

@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String, fshareEpSlug: String) -> Unit = { _, _, _, _, _, _ -> },
    onCategoryClick: (String, String) -> Unit,
    onFshareClick: (String) -> Unit = {},     // enriched slug: "fshare-folder:URL|||NAME|||THUMB"
    onFshareSeeMore: (url: String, title: String) -> Unit = { _, _ -> },
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val fshareMovies by vm.fshareMovies.collectAsState()
    val fshareSeries by vm.fshareSeries.collectAsState()
    val favorites by FavoriteManager.favorites.collectAsState(initial = emptyList())
    val allContinue by WatchHistoryManager.continueList.collectAsState(initial = emptyList())
    val continueList = remember(allContinue) {
        allContinue.filter { it.source in listOf("ophim", "kkphim", "fshare") }
    }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }

    // Proactive Fshare auto-login (mirrors PhimTV) — ensures Fshare ready for continue watching & browse
    LaunchedEffect(Unit) {
        try {
            val fshareRepo = xyz.raidenhub.phim.data.repository.FshareRepository.getInstance(context)
            fshareRepo.autoLogin()
        } catch (_: Exception) { /* silent — user can login manually from Settings */ }
    }

    when (val s = state) {
        is HomeState.Loading -> ShimmerHomeScreen()

        is HomeState.Error -> Box(Modifier.fillMaxSize().background(C.Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😕 Lỗi tải dữ liệu", color = C.TextPrimary, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(s.message, color = C.TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text("🔄 Thử lại", color = C.Primary, fontSize = 16.sp,
                    modifier = Modifier.clickable { vm.load() })
            }
        }

        is HomeState.Success -> {
            val d = s.data
            val settingsGenres by SettingsManager.selectedGenres.collectAsState()
            val sectionOrder by SectionOrderManager.visibleOrder.collectAsState(initial = emptyList())
            val hiddenSlugs by HeroFilterManager.hiddenSlugs.collectAsState(initial = emptySet())

            // Greeting — re-evaluates when the hour changes (fixes stale greeting bug)
            val currentHour = remember {
                java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            }
            val greeting = remember(currentHour) {
                val min = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
                val title = if (min % 2 == 0) "Sếp" else "Tông Chủ"
                when {
                    currentHour < 6  -> "🌙✨ Khuya rồi, $title ơi! Xem phim gì nhỉ?"
                    currentHour < 9  -> "🌅🔥 Chào buổi sáng, $title! Ngày mới tươi sáng!"
                    currentHour < 12 -> "☀️💪 Buổi sáng năng động, $title! Làm gì đây?"
                    currentHour < 14 -> "🌤️🍜 Giờ nghỉ trưa rồi, $title! Xem phim thôi~"
                    currentHour < 18 -> "🌤️⚡ Chào buổi chiều, $title! Hôm nay thế nào?"
                    currentHour < 21 -> "🌙🎬 Tối rồi, $title! Chill phim nào?"
                    else             -> "🌃🍿 Đêm khuya, $title! Đêm nay xem gì đây?"
                }
            }

            // Build section map (filter + mapping moved to DynamicSectionsRenderer)
            val sectionMap = remember(d, settingsGenres) {
                buildSectionMap(d, settingsGenres)
            }

            val scope = rememberCoroutineScope()

            @OptIn(ExperimentalMaterial3Api::class)
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        vm.load()
                        delay(800)
                        isRefreshing = false
                    }
                }
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(C.Background),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // ═══ Greeting ═══
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                greeting,
                                color = C.TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // ═══ Hero Carousel ═══
                    item {
                        val heroMovies = remember(d.newMovies, hiddenSlugs) {
                            d.newMovies.filter { it.slug !in hiddenSlugs }.take(5)
                        }
                        HeroCarousel(
                            movies = heroMovies,
                            onMovieClick = onMovieClick,
                            onHideMovie = { slug ->
                                HeroFilterManager.hide(slug)
                                Toast.makeText(context, "🚫 Đã ẩn phim này", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // ═══ Continue Watching ═══
                    if (continueList.isNotEmpty()) {
                        item {
                            ContinueWatchingSection(
                                continueList = continueList,
                                onContinue = onContinue,
                            )
                        }
                    }

                    // ═══ Favorites ═══
                    if (favorites.isNotEmpty()) {
                        item {
                            FavoritesSection(
                                favorites = favorites,
                                onMovieClick = onMovieClick,
                            )
                        }
                    }

                    // ═══ Dynamic Category Rows (OPhim + Fshare) ═══
                    renderDynamicSections(
                        sectionOrder = sectionOrder,
                        sectionMap = sectionMap,
                        fshareMovies = fshareMovies,
                        fshareSeries = fshareSeries,
                        onMovieClick = onMovieClick,
                        onContinue = onContinue,
                        onCategoryClick = onCategoryClick,
                        onFshareClick = onFshareClick,
                        onFshareSeeMore = onFshareSeeMore,
                        haptic = haptic,
                    )
                }
            }
        }
    }
}
