package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.util.ImageUtils

/** D-5: Related movies horizontal row */
@Composable
fun DetailRelatedRow(
    relatedMovies: List<Movie>,
    onMovieClick: (String) -> Unit
) {
    if (relatedMovies.isEmpty()) return

    Text(
        "🎞️ Có thể bạn thích",
        color = C.TextPrimary,
        fontFamily = JakartaFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 80.dp)
    ) {
        items(relatedMovies, key = { it.slug }) { related ->
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMovieClick(related.slug) }
            ) {
                AsyncImage(
                    model = ImageUtils.cardImage(related.thumbUrl, related.source),
                    contentDescription = related.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Surface)
                )
                Text(
                    related.name,
                    color = C.TextPrimary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
                )
            }
        }
    }
}
