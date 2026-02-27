package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.ui.theme.C

/**
 * Episode panel: shows folder expand or episode grid for Fshare content (Mobile).
 * Adapted from TV: removed focus/key handling, uses touch clickable.
 */
@Composable
fun FshareEpisodePanel(
    episodes: List<EpisodeServer>,
    isFolderPlaceholder: Boolean,
    isFolderExpanding: Boolean,
    folderError: String?,
    slug: String,
    onFolderClick: () -> Unit,
    onEpisodeClick: (slug: String, episodeSlug: String, serverIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val currentServer = episodes.firstOrNull()
        val serverData = currentServer?.serverData ?: emptyList()

        // Header
        val headerText = when {
            isFolderExpanding -> "Đang mở folder..."
            isFolderPlaceholder -> "Chọn tập (Folder)"
            serverData.isNotEmpty() -> "Chọn tập (${serverData.size} tập)"
            else -> "Chưa có tập nào"
        }
        Text(
            text = headerText,
            style = MaterialTheme.typography.titleMedium,
            color = C.TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Folder error
        if (folderError != null) {
            Text(
                text = "⚠️ $folderError",
                style = MaterialTheme.typography.bodySmall,
                color = C.Error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Loading indicator
        if (isFolderExpanding) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Đang kết nối Fshare...", style = MaterialTheme.typography.bodySmall, color = C.TextSecondary)
                }
            }
            return
        }

        if (serverData.isNotEmpty()) {
            // Use fixed height for scrollable grid inside vertical scroll
            // HeightIn with max limits prevents nested scroll issues
            LazyVerticalGrid(
                columns = if (isFolderPlaceholder) GridCells.Fixed(1)
                          else GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (serverData.size <= 4) (serverData.size * 50).dp else 300.dp)
            ) {
                itemsIndexed(
                    items = serverData,
                    key = { _, ep -> ep.slug },
                    contentType = { _, _ -> "fshare_chip" }
                ) { _, episode ->
                    val isFolder = episode.slug == FshareDetailViewModel.FOLDER_SLUG
                    FshareEpisodeChip(
                        name = episode.name,
                        isFolder = isFolder,
                        onClick = {
                            if (isFolder) {
                                onFolderClick()
                            } else {
                                onEpisodeClick(slug, episode.slug, 0)
                            }
                        }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Không tìm thấy link Fshare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = C.TextMuted
                )
            }
        }
    }
}

/**
 * Episode chip for Fshare content — folder or video file (Mobile).
 */
@Composable
fun FshareEpisodeChip(
    name: String,
    isFolder: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = when {
        isFolder -> C.Surface.copy(alpha = 0.8f)
        else -> C.Surface
    }

    Box(
        modifier = Modifier
            .height(if (isFolder) 48.dp else 42.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (isFolder)
                    Modifier.border(1.dp, C.Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name,
            style = if (isFolder) MaterialTheme.typography.bodyMedium
                    else MaterialTheme.typography.bodySmall,
            color = if (isFolder) C.Primary else C.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
