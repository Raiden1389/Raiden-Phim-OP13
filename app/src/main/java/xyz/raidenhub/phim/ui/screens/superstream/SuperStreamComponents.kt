package xyz.raidenhub.phim.ui.screens.superstream

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import xyz.raidenhub.phim.data.api.models.FebBoxFile
import xyz.raidenhub.phim.data.api.models.TmdbSearchItem
import xyz.raidenhub.phim.ui.theme.C

// ═══════════════════════════════════════════════════
//  SuperStream Reusable Components
// ═══════════════════════════════════════════════════

/**
 * Movie/TV card for SuperStream content (TMDB poster).
 */
@Composable
fun SuperStreamCard(
    item: TmdbSearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Quality badge
            if (item.voteAverage > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(C.Overlay, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        "%.1f".format(item.voteAverage),
                        color = C.TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Type badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(
                        if (item.type == "tv") C.Accent else C.Primary,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (item.type == "tv") "TV" else "MOVIE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            item.displayTitle,
            color = C.TextPrimary,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )

        if (item.year > 0) {
            Text(
                "${item.year}",
                color = C.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Horizontal scrollable row of SuperStream cards.
 */
@Composable
fun SuperStreamRow(
    title: String,
    items: List<TmdbSearchItem>,
    onItemClick: (TmdbSearchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title,
            color = C.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                SuperStreamCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

/**
 * Season selector chips (horizontal).
 */
@Composable
fun SeasonSelector(
    seasons: List<FebBoxFile>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(seasons.size) { index ->
            val season = seasons[index]
            val isSelected = index == selectedIndex

            FilterChip(
                selected = isSelected,
                onClick = { onSelect(index) },
                label = {
                    Text(
                        season.name.replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = C.Primary,
                    selectedLabelColor = Color.White,
                    containerColor = C.SurfaceVariant,
                    labelColor = C.TextSecondary
                )
            )
        }
    }
}

/**
 * Episode list item.
 */
@Composable
fun EpisodeItem(
    file: FebBoxFile,
    episodeName: String? = null,
    episodeThumb: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(C.SurfaceVariant)
        ) {
            if (!episodeThumb.isNullOrBlank()) {
                AsyncImage(
                    model = episodeThumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Play icon
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(C.Overlay, RoundedCornerShape(50))
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Extract episode number for display
            val epNum = extractDisplayEpisode(file.name)
            Text(
                epNum,
                color = C.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (!episodeName.isNullOrBlank()) {
                Text(
                    episodeName,
                    color = C.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                file.size,
                color = C.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Hero banner for detail screen backdrop.
 */
@Composable
fun DetailHeroBanner(
    backdropUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        AsyncImage(
            model = backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, C.Background),
                        startY = 100f
                    )
                )
        )
    }
}

// ═══ Helpers ═══

private fun extractDisplayEpisode(fileName: String): String {
    val seMatch = Regex("""[Ss](\d+)[Ee](\d+)""").find(fileName)
    if (seMatch != null) {
        return "S${seMatch.groupValues[1]}E${seMatch.groupValues[2]}"
    }
    val epMatch = Regex("""(?:Episode|Ep)\s*(\d+)""", RegexOption.IGNORE_CASE).find(fileName)
    if (epMatch != null) {
        return "Episode ${epMatch.groupValues[1]}"
    }
    return fileName.substringBeforeLast('.').take(40)
}
