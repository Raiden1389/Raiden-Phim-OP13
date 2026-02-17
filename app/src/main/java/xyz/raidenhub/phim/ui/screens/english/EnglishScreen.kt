package xyz.raidenhub.phim.ui.screens.english

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.ConsumetItem
import xyz.raidenhub.phim.data.repository.ConsumetRepository
import xyz.raidenhub.phim.ui.theme.C

@Composable
fun EnglishScreen(
    onMovieClick: (String) -> Unit,
    onSearch: () -> Unit = {}
) {
    var homeData by remember { mutableStateOf<ConsumetRepository.EnglishHomeData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            ConsumetRepository.getHomeData()
                .onSuccess { homeData = it; isLoading = false }
                .onFailure { error = it.message; isLoading = false }
        }
    }

    when {
        isLoading -> ShimmerEnglishScreen()
        error != null -> ErrorState(error!!) {
            isLoading = true; error = null
            scope.launch {
                ConsumetRepository.getHomeData()
                    .onSuccess { homeData = it; isLoading = false }
                    .onFailure { e -> error = e.message; isLoading = false }
            }
        }
        homeData != null -> EnglishContent(homeData!!, onMovieClick, onSearch)
    }
}

@Composable
private fun EnglishContent(
    data: ConsumetRepository.EnglishHomeData,
    onMovieClick: (String) -> Unit,
    onSearch: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ═══ Hero Banner — first trending ═══
        if (data.trending.isNotEmpty()) {
            item {
                HeroBanner(data.trending.first(), onMovieClick)
            }
        }

        // ═══ Header ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🍿 English Movies & TV",
                    color = C.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, "Search", tint = C.TextPrimary)
                }
            }
        }

        // ═══ Trending Row ═══
        if (data.trending.size > 1) {
            item {
                SectionHeader("🔥 Trending")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.trending.drop(1), key = { it.id }) { movie ->
                        MovieCard(movie, onMovieClick)
                    }
                }
            }
        }

        // ═══ Recent Movies ═══
        if (data.recentMovies.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("🎬 Recent Movies")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.recentMovies, key = { it.id }) { movie ->
                        MovieCard(movie, onMovieClick)
                    }
                }
            }
        }

        // ═══ Recent TV Shows ═══
        if (data.recentShows.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("📺 Recent TV Shows")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.recentShows, key = { it.id }) { movie ->
                        MovieCard(movie, onMovieClick)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// COMPONENTS
// ═══════════════════════════════════════════════

@Composable
private fun HeroBanner(movie: ConsumetItem, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable { onClick(movie.id) }
    ) {
        AsyncImage(
            model = movie.image,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, C.Background),
                    startY = 100f
                )
            )
        )
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (movie.type.isNotBlank()) BadgeChip(movie.type, C.Primary)
                if (movie.releaseDate.isNotBlank()) BadgeChip(movie.releaseDate, Color(0xFF607D8B))
                if (movie.duration.isNotBlank()) BadgeChip(movie.duration, C.Badge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                movie.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onClick(movie.id) },
                colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Watch Now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MovieCard(movie: ConsumetItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick(movie.id) }
    ) {
        Box {
            AsyncImage(
                model = movie.image,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
            )
            // Type badge
            if (movie.type.isNotBlank()) {
                BadgeChip(
                    movie.type,
                    if (movie.type == "Movie") C.Primary else Color(0xFF9C27B0),
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
            // Duration badge
            if (movie.duration.isNotBlank()) {
                BadgeChip(
                    movie.duration,
                    C.Badge,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                )
            }
        }
        Text(
            movie.title,
            color = C.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (movie.releaseDate.isNotBlank()) {
            Text(
                movie.releaseDate,
                color = C.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        color = C.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun BadgeChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(0.9f),
        modifier = modifier
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(C.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😢", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Unable to load content", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(message, color = C.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

// ═══ Shimmer Loading ═══
@Composable
private fun ShimmerEnglishScreen() {
    val shimmerColor = C.Surface
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero shimmer
        item {
            Box(Modifier.fillMaxWidth().height(300.dp).background(shimmerColor.copy(alpha)))
        }
        // Title shimmer
        item {
            Box(
                Modifier.padding(16.dp).width(200.dp).height(28.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp))
            )
        }
        // Rows shimmer
        items(3) {
            Box(
                Modifier.padding(horizontal = 16.dp).width(140.dp).height(20.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(4) {
                    Column(Modifier.width(130.dp)) {
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                .background(shimmerColor.copy(alpha), RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(14.dp)
                                .background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
