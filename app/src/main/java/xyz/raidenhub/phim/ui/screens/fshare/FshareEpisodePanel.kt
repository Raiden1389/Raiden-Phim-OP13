package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.ui.theme.C

/**
 * Episode panel with grid/list toggle for Fshare content (Mobile).
 * - Grid: compact 2-column chips (good for series with many episodes)
 * - List: full-width rows showing complete filenames (good for quality variants)
 */
@Composable
fun FshareEpisodePanel(
    episodes: List<EpisodeServer>,
    isFolderPlaceholder: Boolean,
    isFolderExpanding: Boolean,
    folderError: String?,
    slug: String,
    onFolderClick: (folderUrl: String) -> Unit,
    onEpisodeClick: (slug: String, episodeSlug: String, serverIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val currentServer = episodes.firstOrNull()
        val serverData = currentServer?.serverData ?: emptyList()
        var isListView by remember { mutableStateOf(true) }  // default: list

        // Header with toggle
        val headerText = when {
            isFolderExpanding -> "Đang mở folder..."
            isFolderPlaceholder -> "Chọn tập (Folder)"
            serverData.isNotEmpty() -> "Danh sách file (${serverData.size})"
            else -> "Chưa có tập nào"
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
                color = C.TextPrimary
            )
            // Grid/List toggle — only show when we have items
            if (serverData.isNotEmpty() && !isFolderPlaceholder) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(C.Surface)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Grid button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isListView) C.Primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { isListView = false }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("⊞", fontSize = 16.sp, color = if (!isListView) C.Primary else C.TextMuted)
                    }
                    // List button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isListView) C.Primary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { isListView = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("☰", fontSize = 16.sp, color = if (isListView) C.Primary else C.TextMuted)
                    }
                }
            }
        }

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
                modifier = Modifier.fillMaxWidth().height(80.dp),
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
            if (isListView) {
                // ═══ LIST VIEW — full filename ═══
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (serverData.size <= 5) (serverData.size * 56).dp else 340.dp)
                ) {
                    itemsIndexed(
                        items = serverData,
                        key = { index, ep -> "${ep.slug}_$index" },
                        contentType = { _, _ -> "fshare_list" }
                    ) { index, episode ->
                        val isFolder = episode.slug == FshareDetailViewModel.FOLDER_SLUG
                        FshareFileItem(
                            index = index + 1,
                            name = episode.name,
                            isFolder = isFolder,
                            onClick = {
                                if (isFolder) onFolderClick(episode.linkM3u8)
                                else onEpisodeClick(slug, episode.slug, 0)
                            }
                        )
                    }
                }
            } else {
                // ═══ GRID VIEW — compact 2-column ═══
                LazyVerticalGrid(
                    columns = if (isFolderPlaceholder) GridCells.Fixed(1) else GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (serverData.size <= 4) (serverData.size * 50).dp else 300.dp)
                ) {
                    gridItemsIndexed(
                        items = serverData,
                        key = { index, ep -> "${ep.slug}_g$index" },
                        contentType = { _, _ -> "fshare_grid" }
                    ) { index, episode ->
                        val isFolder = episode.slug == FshareDetailViewModel.FOLDER_SLUG
                        FshareGridChip(
                            name = episode.name,
                            isFolder = isFolder,
                            onClick = {
                                if (isFolder) onFolderClick(episode.linkM3u8)
                                else onEpisodeClick(slug, episode.slug, 0)
                            }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy link Fshare", style = MaterialTheme.typography.bodyMedium, color = C.TextMuted)
            }
        }
    }
}

// ═══ LIST VIEW ITEM ═══

@Composable
fun FshareFileItem(
    index: Int,
    name: String,
    isFolder: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFolder) C.Surface.copy(alpha = 0.8f) else C.Surface)
            .then(
                if (isFolder) Modifier.border(1.dp, C.Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFolder) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(C.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = C.Primary)
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = if (isFolder) "📁 $name" else name,
            style = if (isFolder) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (isFolder) C.Primary else C.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ═══ GRID VIEW CHIP ═══

@Composable
fun FshareGridChip(
    name: String,
    isFolder: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(if (isFolder) 48.dp else 42.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFolder) C.Surface.copy(alpha = 0.8f) else C.Surface)
            .then(
                if (isFolder) Modifier.border(1.dp, C.Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = name,
            style = if (isFolder) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            color = if (isFolder) C.Primary else C.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


