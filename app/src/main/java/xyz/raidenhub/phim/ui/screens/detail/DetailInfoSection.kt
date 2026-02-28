package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily

/** Info section: ratings, metadata, genres, director, cast, description */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailInfoSection(
    movie: MovieDetail,
    imdbRating: String?,
    tmdbRating: String?,
    actorPhotos: Map<String, String>
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        val infos = buildList {
            if (movie.year > 0) add("year:${movie.year}")
            if (movie.country.isNotEmpty()) add("🌍 ${movie.country.joinToString { it.name }}")
            if (movie.time.isNotBlank() && !movie.time.contains("?")) add("⏱ ${movie.time}")
            if (movie.episodeTotal.isNotBlank() && !movie.episodeTotal.contains("?")) {
                val epText = movie.episodeTotal
                val label = if (epText.contains("tập", ignoreCase = true)) epText else "$epText tập"
                add("📺 $label")
            }
        }
        // VP-2: Animated ratings row
        if (imdbRating != null || tmdbRating != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                imdbRating?.toFloatOrNull()?.let { rating ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ IMDb ", color = C.TextSecondary, fontSize = 13.sp)
                        AnimatedFloatCounter(rating, suffix = "/10")
                    }
                }
                tmdbRating?.toFloatOrNull()?.let { rating ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🍅 TMDB ", color = C.TextSecondary, fontSize = 13.sp)
                        AnimatedFloatCounter(rating, suffix = "/10")
                    }
                }
            }
        }
        // Static info row
        if (infos.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                infos.forEach { info ->
                    if (info.startsWith("year:")) {
                        val year = info.removePrefix("year:").toIntOrNull() ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅 ", color = C.TextSecondary, fontSize = 13.sp)
                            AnimatedIntCounter(year)
                        }
                    } else {
                        Text(info, color = C.TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Genres
        if (movie.category.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                movie.category.forEach {
                    Text(it.name, color = C.Accent, fontSize = 12.sp,
                        modifier = Modifier.background(C.SurfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }

        // #19 — Director
        if (movie.director.isNotEmpty() && movie.director.any { it.isNotBlank() }) {
            Text(
                "🎬 Đạo diễn: ${movie.director.filter { it.isNotBlank() }.joinToString(", ")}",
                color = C.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // D-6: Cast grid
        if (movie.actor.isNotEmpty() && movie.actor.any { it.isNotBlank() }) {
            val actors = movie.actor.filter { it.isNotBlank() }
            Text("🎭 Diễn viên:", color = C.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
            val tmdbKeys = actorPhotos.keys.toList()
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(actors.take(12).size) { idx ->
                    val actor = actors[idx]
                    val tmdbName = tmdbKeys.getOrNull(idx)
                    val displayName = tmdbName ?: actor
                    val photoUrl = actorPhotos[actor] ?: tmdbName?.let { actorPhotos[it] }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(C.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = actor,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50))
                                )
                            } else {
                                Text("👤", fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            displayName.split(" ").takeLast(2).joinToString(" "),
                            color = C.TextSecondary,
                            fontFamily = InterFamily,
                            fontSize = 10.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // #14 / D-7 — Description (expandable with gradient fade)
        if (movie.content.isNotBlank()) {
            val cleaned = movie.content.replace(Regex("<[^>]*>"), "").trim()
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    cleaned,
                    color = C.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!expanded) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        C.Background.copy(alpha = 0.85f),
                                        C.Background
                                    ),
                                    startY = 30f
                                )
                            )
                    )
                }
            }
            Text(
                if (expanded) "Thu gọn ▲" else "Xem thêm ▼",
                color = C.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(bottom = 12.dp)
            )
        }
    }
}
