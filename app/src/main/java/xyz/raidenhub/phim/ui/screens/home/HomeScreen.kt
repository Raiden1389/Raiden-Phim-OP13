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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.platform.LocalHapticFeedback
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
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
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
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long) -> Unit = { _, _, _, _ -> },
    onCategoryClick: (String, String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val favorites by FavoriteManager.favorites.collectAsState()
    val continueList by WatchHistoryManager.continueList.collectAsState()
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
            val settingsCountries by SettingsManager.selectedCountries.collectAsState()
            val settingsGenres by SettingsManager.selectedGenres.collectAsState()
            val filterCount = settingsCountries.size + settingsGenres.size
            // H-6: section order state (collected in composable scope, usable in LazyListScope)
            val sectionOrder by SectionOrderManager.order.collectAsState()

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

                    // Hero Carousel — H-1: filter out hidden slugs
                    item {
                        val hiddenSlugs by HeroFilterManager.hiddenSlugs.collectAsState()
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
                                                    onContinue(item.slug, item.server, item.episode, item.positionMs)
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    val triple = sectionMap[sectionId] ?: return@forEach
                    val (label, movies, catPair) = triple
                    if (movies.isNotEmpty()) {
                        item(key = label) {
                            MovieRowSection(label, movies, onMovieClick, onContinue, haptic) {
                                onCategoryClick(catPair.first, catPair.second)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarousel(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onHideMovie: ((String) -> Unit)? = null  // H-1: callback ẩn phim khỏi carousel
) {
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

            // H-1: Dropdown state per slide
            var showMenu by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .combinedClickable(
                        onClick = { onMovieClick(movie.slug) },
                        onLongClick = {
                            if (onHideMovie != null) showMenu = true
                        }
                    )
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

                // H-1: Context menu khi long press
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("🚫 Bỏ qua phim này") },
                        onClick = {
                            showMenu = false
                            onHideMovie?.invoke(movie.slug)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🔍 Xem chi tiết") },
                        onClick = {
                            showMenu = false
                            onMovieClick(movie.slug)
                        }
                    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MovieRowSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
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
                // H-7: Long press → Quick Play từ đầu (positionMs = 0)
                Box(modifier = Modifier.width(130.dp)) {
                    MovieCard(
                        movie = movie,
                        onClick = { onMovieClick(movie.slug) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onContinue(movie.slug, 0, 0, 0L)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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

