package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.ContinueItem
import xyz.raidenhub.phim.ui.theme.C

/** #20 — Play/Continue + Favorite + Watchlist + Playlist buttons */
@Composable
fun DetailActionRow(
    slug: String,
    accentColor: Color,
    isFav: Boolean,
    isWatchlisted: Boolean,
    hasContinue: Boolean,
    continueItem: ContinueItem?,
    continueEp: Int,
    selectedServer: Int,
    onPlay: (slug: String, server: Int, episode: Int) -> Unit,
    onToggleFav: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onShowPlaylist: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                if (hasContinue) onPlay(slug, selectedServer, continueEp)
                else onPlay(slug, selectedServer, 0)
            },
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(Icons.Default.PlayArrow, "Play", tint = C.TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text(
                if (hasContinue) "Tiếp tục Tập ${continueItem?.epName ?: (continueEp + 1)}"
                else "Xem Phim",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Favorite toggle
        IconButton(
            onClick = onToggleFav,
            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
        ) {
            Icon(
                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                "Favorite",
                tint = if (isFav) C.Primary else C.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        // C-4: Watchlist toggle
        IconButton(
            onClick = onToggleWatchlist,
            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
        ) {
            Icon(
                if (isWatchlisted) Icons.Default.Bookmark else Icons.Default.Add,
                contentDescription = if (isWatchlisted) "Đã xem sau" else "Xem sau",
                tint = if (isWatchlisted) C.Primary else C.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        // C-5: Add to playlist
        IconButton(
            onClick = onShowPlaylist,
            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
        ) {
            Text("📋", fontSize = 20.sp)
        }
    }
}
