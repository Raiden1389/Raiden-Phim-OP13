package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyz.raidenhub.phim.ui.theme.C

/**
 * Play + Favorite action buttons for Fshare detail screen (Mobile).
 * Adapted from TV: key navigation → touch clickable.
 */
@Composable
fun FshareActionButtons(
    isFavorite: Boolean,
    showPlay: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPlay) {
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Primary)
                    .clickable { onPlay() }
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "▶  XEM NGAY",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
        }

        // Favorite toggle
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Surface)
                .clickable { onToggleFavorite() }
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isFavorite) "❤️ Yêu thích" else "🤍 Yêu thích",
                style = MaterialTheme.typography.titleSmall,
                color = if (isFavorite) C.Primary else C.TextSecondary
            )
        }
    }
}
