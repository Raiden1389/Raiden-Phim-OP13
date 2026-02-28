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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/** #40 — Season grouping row */
@Composable
fun DetailSeasonRow(
    currentSeason: Int?,
    relatedSeasons: List<Movie>,
    seasonRegex: Regex,
    onSeasonClick: (String) -> Unit
) {
    if (relatedSeasons.isEmpty() || currentSeason == null) return

    Text(
        "📺 Các phần khác (${relatedSeasons.size + 1} phần)",
        color = C.TextPrimary,
        fontFamily = JakartaFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Current season highlighted
        item {
            Text(
                "Phần $currentSeason ★",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(C.Primary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
        // Other seasons
        items(relatedSeasons) { season ->
            val sNum = seasonRegex.find(season.name)?.groupValues?.get(1) ?: "?"
            Text(
                "Phần $sNum",
                color = C.TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(C.Surface)
                    .clickable { onSeasonClick(season.slug) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}
