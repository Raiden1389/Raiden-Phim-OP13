package xyz.raidenhub.phim.ui.screens.home

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.HomeLayout
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.components.MovieCard
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.util.ImageUtils

// ═══ Hero Carousel ═══

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
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
                    Text("🔥 Phim Nổi Bật", color = C.Primary, fontFamily = JakartaFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(movie.name, color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
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

// ═══ Movie Row Section ═══

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieRowSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String, fshareEpSlug: String) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSeeMore: () -> Unit
) {
    if (movies.isEmpty()) return

    // CN-1: Consume homeLayout from SettingsManager
    val homeLayout by SettingsManager.homeLayout.collectAsState()

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Xem thêm →", color = C.Primary, fontFamily = InterFamily, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onSeeMore))
        }

        when (homeLayout) {
            HomeLayout.LIST -> {
                // CN-1 LIST: Vertical list rows
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movies.take(8).forEach { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Surface.copy(alpha = 0.6f))
                                .clickable { onMovieClick(movie.slug) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            AsyncImage(
                                model = ImageUtils.cardImage(movie.thumbUrl, "ophim"),
                                contentDescription = movie.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(54.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            // Text info
                            Column(Modifier.weight(1f)) {
                                Text(
                                    movie.name,
                                    color = C.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    movie.country.firstOrNull()?.name?.let { "🌍 $it" }
                                        ?: if (movie.year > 0) "${movie.year}" else "",
                                    color = C.TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                if (!movie.episodeCurrent.isNullOrBlank()) {
                                    Text(
                                        movie.episodeCurrent,
                                        color = C.Primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // CN-1 COMFORTABLE (150dp) or COMPACT (110dp)
                val cardWidth = if (homeLayout == HomeLayout.COMFORTABLE) 150.dp else 110.dp
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(movies.take(12), key = { it.slug }) { movie ->
                        Box(modifier = Modifier.width(cardWidth)) {
                            MovieCard(
                                movie = movie,
                                onClick = { onMovieClick(movie.slug) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onContinue(movie.slug, 0, 0, 0L, "ophim", "")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══ Shimmer Skeleton Loading ═══

@Composable
fun ShimmerHomeScreen() {
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

// ═══ Fshare HD Row ═══

@Composable
fun FshareRow(
    title: String,
    items: List<xyz.raidenhub.phim.data.api.models.CineMovie>,
    onItemClick: (xyz.raidenhub.phim.data.api.models.CineMovie) -> Unit,
    onSeeMore: () -> Unit = {}
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("HD", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Xem thêm →", color = C.Primary, fontFamily = InterFamily, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onSeeMore))
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items, key = { it.slug }) { movie ->
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onItemClick(movie) }
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
                            model = movie.thumbnailUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Quality badge
                        if (movie.quality.isNotBlank()) {
                            Text(
                                movie.quality,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .background(C.Primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        movie.title,
                        color = C.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (movie.year.isNotBlank()) {
                        Text(
                            movie.year,
                            color = C.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
