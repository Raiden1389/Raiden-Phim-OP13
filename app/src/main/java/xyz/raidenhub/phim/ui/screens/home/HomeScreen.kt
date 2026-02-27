package xyz.raidenhub.phim.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.api.models.CineMovie
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.util.ImageUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String) -> Unit = { _, _, _, _, _ -> },
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
        allContinue.filter { it.source in listOf("ophim", "kkphim") }
    }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }

    when (val s = state) {
        is HomeState.Loading -> {
            // #2 — Shimmer skeleton
            ShimmerHomeScreen()
        }
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
            // H-6: section order state — only visible sections (collected in composable scope, usable in LazyListScope)
            val sectionOrder by SectionOrderManager.visibleOrder.collectAsState(initial = emptyList())
            // P6: Hoist hiddenSlugs ra ngoài LazyColumn — tránh re-subscribe mỗi recompose
            val hiddenSlugs by HeroFilterManager.hiddenSlugs.collectAsState(initial = emptySet())

            // Genre filter only — country filter cướng bức ở tầng API (Constants.ALLOWED_COUNTRIES)
            fun List<Movie>.applySettingsFilter(): List<Movie> {
                if (settingsGenres.isEmpty()) return this
                return filter { m -> m.category.any { it.slug in settingsGenres } }
            }

            // Greeting based on time — xưng hô Sếp/Tông Chủ xen kẽ theo phút lẻ/chẵn
            val greeting = remember {
                val cal = java.util.Calendar.getInstance()
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val min  = cal.get(java.util.Calendar.MINUTE)
                val title = if (min % 2 == 0) "Sếp" else "Tông Chủ"
                when {
                    hour < 6  -> "🌙✨ Khuya rồi, $title ơi! Xem phim gì nhỉ?"
                    hour < 9  -> "🌅🔥 Chào buổi sáng, $title! Ngày mới tươi sáng!"
                    hour < 12 -> "☀️💪 Buổi sáng năng động, $title! Làm gì đây?"
                    hour < 14 -> "🌤️🍜 Giờ nghỉ trưa rồi, $title! Xem phim thôi~"
                    hour < 18 -> "🌤️⚡ Chào buổi chiều, $title! Hôm nay thế nào?"
                    hour < 21 -> "🌙🎬 Tối rồi, $title! Chill phim nào?"
                    else      -> "🌃🍿 Đêm khuya, $title! Đêm nay xem gì đây?"
                }
            }

            // #5 — Pull-to-Refresh
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
                    // Greeting + filter badge
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Hero Carousel — H-1: filter out hidden slugs
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

                // ▶️ Continue Watching row — Cinematic redesign
                if (continueList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp, 20.dp)
                                            .background(C.Primary, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Xem tiếp",
                                        color = C.TextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "${continueList.size} phim",
                                    color = C.TextMuted,
                                    fontSize = 12.sp
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(continueList, key = { "${it.slug}_${it.source}" }) { item ->
                                    val pct = (item.progress * 100).toInt().coerceIn(0, 100)
                                    val timeAgo = remember(item.lastWatched) {
                                        val diffMs = System.currentTimeMillis() - item.lastWatched
                                        val mins = diffMs / 60_000
                                        val hours = mins / 60
                                        val days = hours / 24
                                        when {
                                            mins < 1 -> "Vừa xong"
                                            mins < 60 -> "${mins}ph trước"
                                            hours < 24 -> "${hours}h trước"
                                            days < 7 -> "${days} ngày trước"
                                            else -> "${days / 7} tuần trước"
                                        }
                                    }

                                    // Cinematic landscape card: 190×110dp
                                    Box(
                                        modifier = Modifier
                                            .width(190.dp)
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    onContinue(item.slug, item.server, item.episode, item.positionMs, item.source)
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    WatchHistoryManager.removeContinue(item.slug)
                                                    Toast.makeText(context, "🗑 Đã xoá", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                    ) {
                                        // Full-bleed thumbnail
                                        AsyncImage(
                                            model = ImageUtils.cardImage(item.thumbUrl, item.source),
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Bottom gradient scrim
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(0.3f),
                                                        Color.Black.copy(0.85f)
                                                    ),
                                                    startY = 20f
                                                )
                                            )
                                        )

                                        // Episode badge — top left
                                        Text(
                                            item.epName.ifBlank { "Tập ${item.episode + 1}" },
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp)
                                                .background(
                                                    C.Primary.copy(alpha = 0.9f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )

                                        // Time ago — top right
                                        Text(
                                            timeAgo,
                                            color = Color.White.copy(0.85f),
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .background(
                                                    Color.Black.copy(0.55f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        )

                                        // Play button — center
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(44.dp)
                                                .background(
                                                    Color.White.copy(0.18f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow, "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        // Bottom info: title + progress
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    item.name,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "$pct%",
                                                    color = C.Primary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            // Progress bar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color.White.copy(0.25f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                                                        .fillMaxHeight()
                                                        .background(
                                                            Brush.horizontalGradient(
                                                                listOf(C.Primary, C.Primary.copy(0.7f))
                                                            )
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ❤️ Favorites row (with delete UX)
                if (favorites.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("❤️ Yêu thích", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("${favorites.size} phim", color = C.TextSecondary, fontSize = 13.sp)
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(favorites, key = { "${it.slug}_${it.source}" }) { fav ->
                                    Box(modifier = Modifier.width(130.dp)) {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .combinedClickable(
                                                    onClick = { onMovieClick(fav.slug) },
                                                    onLongClick = {
                                                        FavoriteManager.toggle(fav.slug, fav.name)
                                                        Toast.makeText(context, "💔 Đã xoá ${fav.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(2f / 3f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(C.Surface)
                                            ) {
                                                AsyncImage(
                                                    model = ImageUtils.cardImage(fav.thumbUrl, fav.source),
                                                    contentDescription = fav.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                // Remove button
                                                IconButton(
                                                    onClick = {
                                                        FavoriteManager.toggle(fav.slug, fav.name)
                                                        Toast.makeText(context, "💔 Đã xoá ${fav.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(28.dp)
                                                        .padding(4.dp)
                                                        .background(Color.Black.copy(0.6f), RoundedCornerShape(50))
                                                ) {
                                                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                                // Source badge
                                                if (fav.source == "fshare") {
                                                    Text(
                                                        "F",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp)
                                                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                } else if (fav.source == "superstream") {
                                                    Text(
                                                        "SS",
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier
                                                            .align(Alignment.TopStart)
                                                            .padding(4.dp)
                                                            .background(Color(0xFF2196F3), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                fav.name,
                                                color = C.TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Category rows — H-6: render theo SectionOrderManager order
                val newFiltered    = d.newMovies.applySettingsFilter()
                val koreanFiltered = d.korean.applySettingsFilter()
                val seriesFiltered = d.series.applySettingsFilter()
                val singleFiltered = d.singleMovies.applySettingsFilter()
                val animeFiltered  = d.anime.applySettingsFilter()
                val tvFiltered     = d.tvShows.applySettingsFilter()

                // Map sectionId → (label, list, categorySlug, categoryName)
                val sectionMap = mapOf(
                    "new"     to Triple("🔥 Phim Mới",   newFiltered,    "phim-moi-cap-nhat" to "Phim Mới"),
                    "korean"  to Triple("🇰🇷 K-Drama",   koreanFiltered, "han-quoc" to "K-Drama"),
                    "series"  to Triple("📺 Phim Bộ",    seriesFiltered, "phim-bo" to "Phim Bộ"),
                    "single"  to Triple("🎬 Phim Lẻ",    singleFiltered, "phim-le" to "Phim Lẻ"),
                    "anime"   to Triple("🎌 Hoạt Hình",  animeFiltered,  "hoat-hinh" to "Hoạt Hình"),
                    "tvshows" to Triple("📺 TV Shows",   tvFiltered,     "tv-shows" to "TV Shows"),
                )

                sectionOrder.forEach { sectionId ->
                    // OPhim rows
                    val triple = sectionMap[sectionId]
                    if (triple != null) {
                        val (label, movies, catPair) = triple
                        if (movies.isNotEmpty()) {
                            item(key = label) {
                                MovieRowSection(label, movies, onMovieClick, onContinue, haptic) {
                                    onCategoryClick(catPair.first, catPair.second)
                                }
                            }
                        }
                        return@forEach
                    }

                    // Fshare rows
                    when (sectionId) {
                        "fshare_movies" -> if (fshareMovies.isNotEmpty()) {
                            item(key = "fshare_movies") {
                                FshareRow(
                                    title = "💎 Fshare Phim Lẻ",
                                    items = fshareMovies,
                                    onItemClick = { movie ->
                                        val enriched = "fshare-folder:${movie.detailUrl}|||${movie.title}|||${movie.thumbnailUrl}"
                                        onFshareClick(enriched)
                                    },
                                    onSeeMore = { onFshareSeeMore("https://thuviencine.com/movies/", "Fshare Phim Lẻ") }
                                )
                            }
                        }
                        "fshare_series" -> if (fshareSeries.isNotEmpty()) {
                            item(key = "fshare_series") {
                                FshareRow(
                                    title = "💎 Fshare Phim Bộ",
                                    items = fshareSeries,
                                    onItemClick = { movie ->
                                        val enriched = "fshare-folder:${movie.detailUrl}|||${movie.title}|||${movie.thumbnailUrl}"
                                        onFshareClick(enriched)
                                    },
                                    onSeeMore = { onFshareSeeMore("https://thuviencine.com/tv-series/", "Fshare Phim Bộ") }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
