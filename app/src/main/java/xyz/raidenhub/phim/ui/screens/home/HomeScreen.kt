package xyz.raidenhub.phim.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.ui.components.MovieCard
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            MovieRepository.getHomeData()
                .onSuccess { _state.value = HomeState.Success(it) }
                .onFailure { _state.value = HomeState.Error(it.message ?: "Unknown error") }
        }
    }
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val data: MovieRepository.HomeData) : HomeState()
    data class Error(val message: String) : HomeState()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val favorites by FavoriteManager.favorites.collectAsState()
    val continueList by WatchHistoryManager.continueList.collectAsState()
    val context = LocalContext.current
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
            val settingsCountries by SettingsManager.selectedCountries.collectAsState()
            val settingsGenres by SettingsManager.selectedGenres.collectAsState()
            val filterCount = settingsCountries.size + settingsGenres.size

            // Filter helpers using Settings
            fun List<Movie>.applySettingsFilter(): List<Movie> {
                var result = this
                if (settingsCountries.isNotEmpty()) {
                    result = result.filter { m -> m.country.any { it.slug in settingsCountries } }
                }
                if (settingsGenres.isNotEmpty()) {
                    result = result.filter { m -> m.category.any { it.slug in settingsGenres } }
                }
                return result
            }

            // Greeting based on time
            val greeting = remember {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when {
                    hour < 6 -> "🌙 Khuya rồi, xem phim gì nhỉ?"
                    hour < 12 -> "☀️ Chào buổi sáng!"
                    hour < 18 -> "🌤️ Chào buổi chiều!"
                    else -> "🌙 Chào buổi tối!"
                }
            }



            Box {
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
                            Text(greeting, color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (filterCount > 0) {
                                Text(
                                    "🔵 $filterCount bộ lọc",
                                    color = C.Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(C.Primary.copy(0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Hero Carousel
                    item {
                        HeroCarousel(
                            movies = d.newMovies.take(5),
                            onMovieClick = onMovieClick
                        )
                    }

                // ▶️ Continue Watching row
                if (continueList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "▶️ Xem tiếp",
                                color = C.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(continueList, key = { it.slug }) { item ->
                                    Box(modifier = Modifier.width(130.dp)) {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .combinedClickable(
                                                    onClick = { onMovieClick(item.slug) },
                                                    onLongClick = {
                                                        WatchHistoryManager.removeContinue(item.slug)
                                                        Toast.makeText(context, "🗑 Đã xoá", Toast.LENGTH_SHORT).show()
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
                                                    model = ImageUtils.cardImage(item.thumbUrl, item.source),
                                                    contentDescription = item.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                // Episode label
                                                Text(
                                                    item.epName,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                                // Progress bar at bottom
                                                LinearProgressIndicator(
                                                    progress = { item.progress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .align(Alignment.BottomCenter),
                                                    color = C.Primary,
                                                    trackColor = Color.Black.copy(0.5f)
                                                )
                                            }
                                            Text(
                                                item.name,
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
                                items(favorites, key = { it.slug }) { fav ->
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

                // Category rows (filtered by Settings)
                val newFiltered = d.newMovies.applySettingsFilter()
                val koreanFiltered = d.korean.applySettingsFilter()
                val seriesFiltered = d.series.applySettingsFilter()
                val singleFiltered = d.singleMovies.applySettingsFilter()
                val animeFiltered = d.anime.applySettingsFilter()
                val tvFiltered = d.tvShows.applySettingsFilter()

                if (newFiltered.isNotEmpty()) {
                    item { MovieRowSection("🔥 Phim Mới", newFiltered, onMovieClick) { onCategoryClick("phim-moi-cap-nhat", "Phim Mới") } }
                }
                if (koreanFiltered.isNotEmpty()) {
                    item { MovieRowSection("🇰🇷 K-Drama", koreanFiltered, onMovieClick) { onCategoryClick("han-quoc", "K-Drama") } }
                }
                if (seriesFiltered.isNotEmpty()) {
                    item { MovieRowSection("📺 Phim Bộ", seriesFiltered, onMovieClick) { onCategoryClick("phim-bo", "Phim Bộ") } }
                }
                if (singleFiltered.isNotEmpty()) {
                    item { MovieRowSection("🎬 Phim Lẻ", singleFiltered, onMovieClick) { onCategoryClick("phim-le", "Phim Lẻ") } }
                }
                if (animeFiltered.isNotEmpty()) {
                    item { MovieRowSection("🎌 Hoạt Hình", animeFiltered, onMovieClick) { onCategoryClick("hoat-hinh", "Hoạt Hình") } }
                }
                if (tvFiltered.isNotEmpty()) {
                    item { MovieRowSection("📺 TV Shows", tvFiltered, onMovieClick) { onCategoryClick("tv-shows", "TV Shows") } }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(movies: List<Movie>, onMovieClick: (String) -> Unit) {
    if (movies.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { movies.size })

    // Auto-scroll every 5s
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            val next = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(next)
        }
    }

    Box {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) { page ->
            val movie = movies[page]

            // Ken Burns: slow zoom 1.0→1.15 over 10s
            val kenBurnsScale = remember { Animatable(1f) }
            val kenBurnsX = remember { Animatable(0f) }
            val kenBurnsY = remember { Animatable(0f) }

            LaunchedEffect(page, pagerState.currentPage) {
                if (page == pagerState.currentPage) {
                    // Reset
                    kenBurnsScale.snapTo(1f)
                    kenBurnsX.snapTo(0f)
                    kenBurnsY.snapTo(0f)
                    // Animate zoom + subtle pan
                    launch {
                        kenBurnsScale.animateTo(
                            1.15f,
                            animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                        )
                    }
                    launch {
                        kenBurnsX.animateTo(
                            if (page % 2 == 0) 15f else -15f,
                            animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                        )
                    }
                    launch {
                        kenBurnsY.animateTo(
                            if (page % 3 == 0) -8f else 8f,
                            animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .clickable { onMovieClick(movie.slug) }
            ) {
                AsyncImage(
                    model = ImageUtils.heroImage(movie.posterUrl.ifBlank { movie.thumbUrl }, movie.source),
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale.value
                            scaleY = kenBurnsScale.value
                            translationX = kenBurnsX.value
                            translationY = kenBurnsY.value
                        }
                )
                // Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(
                            colors = listOf(C.HeroGradientTop, C.HeroGradientBottom),
                            startY = 80f
                        ))
                )
                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text("🔥 Phim Nổi Bật", color = C.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(movie.name, color = C.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (movie.quality.isNotBlank()) Text(movie.quality, color = C.Accent, fontSize = 12.sp)
                        if (movie.year > 0) Text("${movie.year}", color = C.TextSecondary, fontSize = 12.sp)
                        if (movie.country.isNotEmpty()) Text(movie.country.first().name, color = C.TextSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    // "Xem Ngay" button
                    Button(
                        onClick = { onMovieClick(movie.slug) },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Xem Ngay", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Page indicator dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(movies.size) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (idx == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (idx == pagerState.currentPage) C.Primary else Color.White.copy(0.5f)
                        )
                )
            }
        }
    }
}

@Composable
private fun MovieRowSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onSeeMore: () -> Unit
) {
    if (movies.isEmpty()) return

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Xem thêm →", color = C.Primary, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onSeeMore))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(movies.take(12), key = { it.slug }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie.slug) },
                    modifier = Modifier.width(130.dp)
                )
            }
        }
    }
}

// #2 — Shimmer skeleton loading
@Composable
private fun ShimmerHomeScreen() {
    val shimmerColor = C.Surface
    val shimmerHighlight = C.SurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero shimmer
        item {
            Box(
                Modifier.fillMaxWidth().height(280.dp)
                    .background(shimmerColor.copy(alpha))
            )
        }
        // Greeting shimmer
        item {
            Box(
                Modifier.padding(16.dp).width(200.dp).height(24.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp))
            )
        }
        // Movie row shimmers (3 rows)
        items(3) {
            Column(Modifier.padding(top = 16.dp)) {
                Box(
                    Modifier.padding(horizontal = 12.dp).width(150.dp).height(20.dp)
                        .background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) {
                        Column(Modifier.width(130.dp)) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                    .background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier.fillMaxWidth().height(14.dp)
                                    .background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

