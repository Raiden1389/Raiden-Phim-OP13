package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.Playlist
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.ui.theme.C

/** C-5: Playlist selection dialog */
@Composable
fun DetailPlaylistDialog(
    slug: String,
    movieName: String,
    thumbUrl: String,
    playlists: List<Playlist>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📋 Thêm vào Playlist", color = C.TextPrimary) },
        text = {
            if (playlists.isEmpty()) {
                Text("Chưa có playlist nào. Tạo playlist trong Mục Playlist.", color = C.TextSecondary)
            } else {
                Column {
                    playlists.forEach { pl ->
                        val inList = remember(playlists) {
                            pl.items.any { it.slug == slug }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (inList) PlaylistManager.removeFromPlaylist(pl.id, slug)
                                    else PlaylistManager.addToPlaylist(pl.id, slug, movieName, thumbUrl)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pl.name, color = C.TextPrimary, fontSize = 15.sp)
                            Text(if (inList) "✓" else "+", color = if (inList) C.Primary else C.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = C.SurfaceVariant, thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Xong", color = C.Primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = C.Surface
    )
}
