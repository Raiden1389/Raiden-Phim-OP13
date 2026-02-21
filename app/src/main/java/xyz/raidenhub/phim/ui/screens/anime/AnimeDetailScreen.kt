package xyz.raidenhub.phim.ui.screens.anime

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import xyz.raidenhub.phim.data.api.models.Anime47Detail
import xyz.raidenhub.phim.data.api.models.Anime47Episode
import xyz.raidenhub.phim.data.repository.AnimeRepository
import xyz.raidenhub.phim.ui.components.ShimmerDetailScreen
import xyz.raidenhub.phim.ui.theme.C

// #45 — Anime Detail Screen
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: Int,
    slug: String,
    onBack: () -> Unit,
    // Hướng B: truyền toàn bộ episodeIds + title để PlayerActivity fetch stream
    onPlayAnime47: (episodeIds: IntArray, epIdx: Int, title: String) -> Unit
) {
    var detail by remember { mutableStateOf<Anime47Detail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(animeId) {
        AnimeRepository.getAnimeDetail(animeId)
            .onSuccess { detail = it; isLoading = false }
            .onFailure { error = it.message; isLoading = false }
    }

    when {
        isLoading -> ShimmerDetailScreen()
        error != null -> Box(Modifier.fillMaxSize().background(C.Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😕 $error", color = C.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("← Quay lại", color = C.Primary, modifier = Modifier.clickable(onClick = onBack))
            }
        }
        detail != null -> AnimeDetailContent(detail!!, onBack, onPlayAnime47)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimeDetailContent(
    anime: Anime47Detail,
    onBack: () -> Unit,
    onPlayAnime47: (episodeIds: IntArray, epIdx: Int, title: String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val episodes = anime.latestEpisodes
    // Build IntArray of episode IDs theo thứ tự latestEpisodes
    val episodeIds = remember(episodes) { episodes.map { it.id }.toIntArray() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(C.Background)
    ) {
        // ═══ Backdrop ═══
        item {
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = anime.backdropImage.ifBlank { anime.poster },
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, C.Background), startY = 100f
                )))
                // Back button
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                // Title overlay
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(anime.title, color = C.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (anime.quality.isNotBlank()) AnimeBadge(anime.quality, C.Primary)
                        if (anime.type.isNotBlank()) AnimeBadge(anime.type, Color(0xFF2196F3))
                        if (anime.rating.isNotBlank()) AnimeBadge("⭐ ${anime.rating}", Color(0xFFFFA000))
                        if (anime.status.isNotBlank()) AnimeBadge(anime.status, Color(0xFF4CAF50))
                    }
                }
            }
        }

        // ═══ Play Button ═══
        if (episodes.isNotEmpty()) {
            item {
                Button(
                    onClick = { onPlayAnime47(episodeIds, 0, anime.title) },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(48.dp)
                ) {
                    Text("▶ Xem Phim", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ═══ Info ═══
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                val infos = buildList {
                    if (anime.releaseDate.isNotBlank()) add("📅 ${anime.releaseDate}")
                    if (anime.duration.isNotBlank() && anime.duration != "Unknown") add("⏱ ${anime.duration}")
                    add("👀 ${anime.views} lượt xem")
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    infos.forEach { Text(it, color = C.TextSecondary, fontSize = 13.sp) }
                }

                // Genres
                if (anime.genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        anime.genres.forEach { genre ->
                            Text(
                                genre,
                                color = C.Accent,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(C.SurfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Description
                if (anime.description.isNotBlank()) {
                    val cleaned = anime.description.replace(Regex("<[^>]*>"), "").replace("*", "").trim()
                    Text(
                        cleaned,
                        color = C.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        if (expanded) "Thu gọn ▲" else "Xem thêm ▼",
                        color = C.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { expanded = !expanded }.padding(bottom = 12.dp)
                    )
                }
            }
        }

        // ═══ Anime Groups (Season Grouping) ═══
        val groups = anime.animeGroups
        if (groups is List<*> && groups.isNotEmpty()) {
            item {
                Text("📺 Các phần liên quan", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            // animeGroups is already parsed from JSON — need to handle raw Any?
        }

        // ═══ Characters ═══
        val characters = anime.characters
        if (characters is List<*> && characters.isNotEmpty()) {
            item {
                Text("🎭 Nhân vật", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // Characters are part of Anime47Detail but typed as Any
                // Placeholder — characters display
                Text("${(characters as List<*>).size} nhân vật", color = C.TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        // ═══ Episodes ═══
        if (episodes.isNotEmpty()) {
            item {
                Text(
                    "📋 Danh sách tập (${episodes.size})",
                    color = C.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(episodes) { ep ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onPlayAnime47(episodeIds, (ep.number - 1).coerceAtLeast(0), anime.title) },
                    shape = RoundedCornerShape(12.dp),
                    color = C.Surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(C.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(ep.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Tập ${ep.title}", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (ep.name.isNotBlank() && ep.name != ep.title) {
                                Text(ep.name, color = C.TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun AnimeBadge(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
