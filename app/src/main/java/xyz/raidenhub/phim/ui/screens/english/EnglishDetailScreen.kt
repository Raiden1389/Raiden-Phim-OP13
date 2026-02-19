package xyz.raidenhub.phim.ui.screens.english

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
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
import xyz.raidenhub.phim.data.api.models.ConsumetDetail
import xyz.raidenhub.phim.data.api.models.ConsumetEpisode
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.repository.ConsumetRepository
import xyz.raidenhub.phim.ui.theme.C

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishDetailScreen(
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (episodeId: String, mediaId: String, filmName: String) -> Unit
) {
    var detail by remember { mutableStateOf<ConsumetDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mediaId) {
        scope.launch {
            ConsumetRepository.getDetail(mediaId)
                .onSuccess { detail = it; isLoading = false }
                .onFailure { error = it.message; isLoading = false }
        }
    }

    val favorites by FavoriteManager.favorites.collectAsState()
    val isFav = favorites.any { it.slug == mediaId }

    Scaffold(
        containerColor = C.Background,
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "Loading...", color = C.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = C.TextPrimary)
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = {
                            FavoriteManager.toggle(mediaId, detail!!.title, detail!!.image, "english")
                        }) {
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favorite",
                                tint = if (isFav) C.Primary else C.TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        when {
            isLoading -> ShimmerDetail(Modifier.padding(padding))
            error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😢", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Unable to load", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(error ?: "", color = C.TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        isLoading = true; error = null
                        scope.launch {
                            ConsumetRepository.getDetail(mediaId)
                                .onSuccess { detail = it; isLoading = false }
                                .onFailure { e -> error = e.message; isLoading = false }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = C.Primary)) {
                        Text("Retry")
                    }
                }
            }
            detail != null -> DetailContent(
                detail = detail!!,
                selectedSeason = selectedSeason,
                onSeasonChange = { selectedSeason = it },
                onPlay = { epId, mId -> onPlay(epId, mId, detail!!.title) },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: ConsumetDetail,
    selectedSeason: Int,
    onSeasonChange: (Int) -> Unit,
    onPlay: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = detail.episodes.map { it.season }.distinct().sorted().ifEmpty { listOf(1) }
    val currentEpisodes = detail.episodes.filter {
        it.season == selectedSeason || seasons.size <= 1
    }.sortedBy { it.number }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ═══ Cover Image ═══
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp)
            ) {
                AsyncImage(
                    model = detail.cover.ifBlank { detail.image },
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, C.Background),
                            startY = 100f
                        )
                    )
                )
            }
        }

        // ═══ Info Section ═══
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    detail.title,
                    color = C.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                // Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (detail.type.isNotBlank()) BadgeChip(detail.type, C.Primary)
                    if (detail.releaseDate.isNotBlank()) BadgeChip(detail.releaseDate, Color(0xFF607D8B))
                    if (detail.duration.isNotBlank()) BadgeChip(detail.duration, C.Badge)
                }

                // Genres
                if (detail.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        detail.genres.joinToString(" · "),
                        color = C.TextSecondary,
                        fontSize = 13.sp
                    )
                }

                // Casts
                if (detail.casts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cast: ${detail.casts.take(5).joinToString(", ")}",
                        color = C.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Description
                if (detail.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        detail.description,
                        color = C.TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ═══ Season Selector ═══
        if (seasons.size > 1) {
            item {
                Text(
                    "Seasons",
                    color = C.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    seasons.forEach { season ->
                        FilterChip(
                            selected = season == selectedSeason,
                            onClick = { onSeasonChange(season) },
                            label = { Text("S$season") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = C.Surface,
                                labelColor = C.TextPrimary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ═══ Episodes Header ═══
        item {
            Text(
                if (currentEpisodes.size == 1 && detail.type == "Movie")
                    "▶ Watch Now"
                else
                    "Episodes (${currentEpisodes.size})",
                color = C.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ═══ Episode Grid ═══
        item {
            Column(Modifier.padding(horizontal = 12.dp)) {
                currentEpisodes.forEach { episode ->
                    EpisodeItem(
                        episode = episode,
                        onClick = { onPlay(episode.id, detail.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeItem(episode: ConsumetEpisode, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = C.Surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(C.Primary.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = C.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (episode.title.isNotBlank()) episode.title
                    else "Episode ${episode.number}",
                    color = C.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.season > 0) {
                    Text(
                        "S${episode.season} · E${episode.number}",
                        color = C.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(0.9f), modifier = modifier) {
        Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun ShimmerDetail(modifier: Modifier = Modifier) {
    val shimmerColor = C.Surface
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer_alpha"
    )

    Column(modifier.fillMaxSize().background(C.Background)) {
        Box(Modifier.fillMaxWidth().height(250.dp).background(shimmerColor.copy(alpha)))
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.width(250.dp).height(28.dp).background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp)))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.width(180.dp).height(16.dp).background(shimmerColor.copy(alpha), RoundedCornerShape(4.dp)))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(60.dp).background(shimmerColor.copy(alpha), RoundedCornerShape(8.dp)))
            Spacer(Modifier.height(20.dp))
            repeat(5) {
                Box(Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp)
                    .background(shimmerColor.copy(alpha), RoundedCornerShape(10.dp)))
            }
        }
    }
}
