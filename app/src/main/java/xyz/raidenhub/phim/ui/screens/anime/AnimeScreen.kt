package xyz.raidenhub.phim.ui.screens.anime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import xyz.raidenhub.phim.data.api.models.Anime47Genre
import xyz.raidenhub.phim.data.api.models.Anime47Item
import xyz.raidenhub.phim.data.repository.AnimeRepository
import xyz.raidenhub.phim.ui.theme.C

@Composable
fun AnimeScreen(
    onAnimeClick: (Int, String) -> Unit
) {
    var homeData by remember { mutableStateOf<AnimeRepository.AnimeHomeData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            AnimeRepository.getHomeData()
                .onSuccess { homeData = it; isLoading = false }
                .onFailure { error = it.message; isLoading = false }
        }
    }

    when {
        isLoading -> ShimmerAnimeScreen()
        error != null -> ErrorState(error!!) {
            isLoading = true; error = null
            scope.launch {
                AnimeRepository.getHomeData()
                    .onSuccess { homeData = it; isLoading = false }
                    .onFailure { e -> error = e.message; isLoading = false }
            }
        }
        homeData != null -> AnimeContent(homeData!!, onAnimeClick)
    }
}

@Composable
private fun AnimeContent(
    data: AnimeRepository.AnimeHomeData,
    onAnimeClick: (Int, String) -> Unit
) {
    var selectedGenre by remember { mutableStateOf<Anime47Genre?>(null) }
    var genreResults by remember { mutableStateOf<List<Anime47Item>>(emptyList()) }
    var genreLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Fetch anime by genre slug — accurate filter, fallback to keyword search
    LaunchedEffect(selectedGenre) {
        val genre = selectedGenre ?: run { genreResults = emptyList(); return@LaunchedEffect }
        genreLoading = true
        AnimeRepository.getAnimeByGenre(genre.slug, genre.name).onSuccess { results ->
            genreResults = results
        }.onFailure {
            genreResults = emptyList()
        }
        genreLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ═══ Hero Banner ═══
        if (data.hero.isNotEmpty()) {
            item {
                HeroBanner(data.hero.first(), onAnimeClick)
            }
        }

        // ═══ Header ═══
        item {
            Text(
                "🎌 Anime",
                color = C.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // ═══ Genre Chips (moved to top for discoverability) ═══
        if (data.genres.isNotEmpty()) {
            item {
                SectionHeader("🏷️ Thể Loại")
                GenreChips(
                    genres = data.genres,
                    selectedGenreSlug = selectedGenre?.slug,
                    onGenreClick = { genre ->
                        selectedGenre = if (selectedGenre?.slug == genre.slug) null else genre
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        // ═══ Genre Results ═══
        if (selectedGenre != null) {
            if (genreLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(32.dp))
                    }
                }
            } else if (genreResults.isNotEmpty()) {
                item {
                    SectionHeader("📂 ${selectedGenre!!.name} (${genreResults.size} kết quả)")
                }
                items(genreResults.chunked(3)) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { anime ->
                            Box(modifier = Modifier.weight(1f)) {
                                AnimeCard(anime, onAnimeClick)
                            }
                        }
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Không tìm thấy anime thể loại \"${selectedGenre?.name}\"",
                        color = C.TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // ═══ Trending Row ═══
        if (data.trending.isNotEmpty() && selectedGenre == null) {
            item {
                SectionHeader("🔥 Trending")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.trending, key = { it.id }) { anime ->
                        AnimeCard(anime, onAnimeClick)
                    }
                }
            }
        }

        // ═══ 🐉 Donghua (Hoạt Hình Trung Quốc) ═══
        if (selectedGenre == null) {
            item {
                DonghuaSection(onAnimeClick)
            }
        }

        // ═══ Latest Episodes Row ═══
        if (data.latest.isNotEmpty() && selectedGenre == null) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("📺 Mới Cập Nhật")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.latest, key = { it.id }) { anime ->
                        AnimeCard(anime, onAnimeClick)
                    }
                }
            }
        }

        // ═══ Upcoming Row ═══
        if (data.upcoming.isNotEmpty() && selectedGenre == null) {
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("🗓️ Sắp Ra Mắt")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(data.upcoming, key = { it.id }) { anime ->
                        AnimeCard(anime, onAnimeClick)
                    }
                }
            }
        }

        // ═══ Hero Carousel (remaining heroes) ═══
        if (data.hero.size > 1) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("⭐ Nổi Bật")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(data.hero.drop(1), key = { it.id }) { anime ->
                        FeaturedCard(anime, onAnimeClick)
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
private fun HeroBanner(anime: Anime47Item, onClick: (Int, String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable { onClick(anime.id, anime.slug) }
    ) {
        AsyncImage(
            model = anime.backdropImage.ifBlank { anime.displayImage },
            contentDescription = anime.title,
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
            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (anime.quality.isNotBlank()) BadgeChip(anime.quality, C.Primary)
                if (anime.rating.isNotBlank()) {
                    BadgeChip("⭐ ${anime.rating}", Color(0xFFFF9800))
                }
                if (anime.type.isNotBlank()) BadgeChip(anime.type.uppercase(), C.Badge)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                anime.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (anime.genres.isNotEmpty()) {
                Text(
                    anime.genres.take(3).joinToString(" · "),
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            // Play button
            Button(
                onClick = { onClick(anime.id, anime.slug) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = C.Primary
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Xem ngay", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnimeCard(anime: Anime47Item, onClick: (Int, String) -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick(anime.id, anime.slug) }
    ) {
        Box {
            AsyncImage(
                model = anime.displayImage,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
            )
            // Quality badge
            if (anime.quality.isNotBlank()) {
                BadgeChip(
                    anime.quality,
                    C.Primary,
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
            // Episode badge
            val epLabel = anime.episodeLabel
            if (epLabel.isNotBlank()) {
                BadgeChip(
                    epLabel,
                    C.Badge,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                )
            }
        }
        Text(
            anime.title,
            color = C.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun FeaturedCard(anime: Anime47Item, onClick: (Int, String) -> Unit) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(anime.id, anime.slug) }
    ) {
        AsyncImage(
            model = anime.backdropImage.ifBlank { anime.displayImage },
            contentDescription = anime.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(0.8f)),
                    startY = 60f
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (anime.quality.isNotBlank()) BadgeChip(anime.quality, C.Primary)
                if (anime.rating.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, "Rating", tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Text(anime.rating, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }
            Text(
                anime.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (anime.genres.isNotEmpty()) {
                Text(
                    anime.genres.take(2).joinToString(" · "),
                    color = Color.White.copy(0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GenreChips(
    genres: List<Anime47Genre>,
    selectedGenreSlug: String? = null,
    onGenreClick: (Anime47Genre) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        genres.take(30).forEach { genre ->
            val isSelected = selectedGenreSlug == genre.slug
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) C.Accent else C.Surface,
                modifier = Modifier.clickable { onGenreClick(genre) }
            ) {
                Text(
                    genre.name,
                    color = if (isSelected) Color.White else C.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun DonghuaSection(onAnimeClick: (Int, String) -> Unit) {
    var donghuaList by remember { mutableStateOf<List<Anime47Item>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        AnimeRepository.getDonghua().onSuccess { list ->
            donghuaList = list
        }
        isLoading = false
    }

    if (isLoading) {
        // Shimmer placeholder
        Box(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(24.dp))
        }
    } else if (donghuaList.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        SectionHeader("🐉 Hoạt Hình Trung Quốc")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(donghuaList, key = { it.id }) { anime ->
                AnimeCard(anime, onAnimeClick)
            }
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
            Text("Không thể tải dữ liệu", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(message, color = C.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Thử lại")
            }
        }
    }
}

// ═══ Shimmer Loading ═══
@Composable
private fun ShimmerAnimeScreen() {
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
                Modifier.padding(16.dp).width(160.dp).height(28.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp))
            )
        }
        // Section header shimmer
        item {
            Box(
                Modifier.padding(horizontal = 16.dp).width(140.dp).height(20.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(12.dp))
        }
        // Row shimmer
        items(2) {
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
