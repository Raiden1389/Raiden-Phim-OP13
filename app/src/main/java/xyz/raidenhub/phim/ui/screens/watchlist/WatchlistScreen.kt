package xyz.raidenhub.phim.ui.screens.watchlist

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.local.Playlist
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.data.local.WatchlistItem
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.ui.components.EmptyStateView
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils


// ═══════════════════════════════════════════════
// C-4: Xem Sau (Watchlist) Screen
// CN-3: Gallery Mode — poster-only view (no labels)
// ═══════════════════════════════════════════════
@Composable
fun WatchlistScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    val items by WatchlistManager.items.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var galleryMode by remember { mutableStateOf(false) }  // CN-3: Gallery toggle

    Column(Modifier.fillMaxSize().background(C.Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("🔖 Xem Sau", color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${items.size} phim đã bookmark", color = C.TextSecondary, fontSize = 12.sp)
            }
            // CN-3: Gallery toggle button
            IconButton(onClick = { galleryMode = !galleryMode }) {
                Icon(
                    imageVector = if (galleryMode) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = if (galleryMode) "Chế độ danh sách" else "Chế độ gallery",
                    tint = if (galleryMode) C.Primary else C.TextSecondary
                )
            }
        }

        if (items.isEmpty()) {
            EmptyStateView(
                emoji = "🔖",
                title = "Chưa có phim yêu thích",
                subtitle = "Bấm 🔖 trên phim bất kỳ để lưu vào đây"
            )
        } else {
            // CN-3: Animated switch between Gallery (2-col, no text) and Normal (3-col with text)
            AnimatedContent(
                targetState = galleryMode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "gallery_anim"
            ) { isGallery ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isGallery) 2 else 3),
                    contentPadding = PaddingValues(if (isGallery) 4.dp else 8.dp, 0.dp, if (isGallery) 4.dp else 8.dp, 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.slug }) { item ->
                        WatchlistMovieCard(
                            item = item,
                            onClick = { onMovieClick(item.slug) },
                            galleryMode = isGallery,
                            onRemove = {
                                WatchlistManager.remove(item.slug)
                                Toast.makeText(context, "🗑 Đã xoá khỏi Xem Sau", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun WatchlistMovieCard(
    item: WatchlistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    galleryMode: Boolean = false  // CN-3
) {
    val padding = if (galleryMode) 3.dp else 4.dp
    Box(Modifier.padding(padding)) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(if (galleryMode) 10.dp else 8.dp))
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(if (galleryMode) 10.dp else 8.dp))
                    .background(C.Surface)
            ) {
                AsyncImage(
                    model = ImageUtils.cardImage(item.thumbUrl, item.source),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Remove button — smaller in gallery mode
                if (!galleryMode) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(28.dp)
                            .padding(4.dp)
                            .background(Color.Black.copy(0.7f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Delete, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            // CN-3: Ẩn text hoàn toàn ở Gallery mode
            if (!galleryMode) {
                Text(
                    item.name,
                    color = C.TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp, bottom = 4.dp)
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════
// C-5: Playlists Screen (danh sách tất cả playlist)
// ═══════════════════════════════════════════════
@Composable
fun PlaylistListScreen(
    onBack: () -> Unit,
    onPlaylistClick: (String, String) -> Unit // id, name
) {
    val playlists by PlaylistManager.playlists.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(C.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
                }
                Text("📋 Playlist", color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, "New Playlist", tint = C.Primary)
            }
        }

        if (playlists.isEmpty()) {
            EmptyStateView(
                emoji = "📋",
                title = "Chưa có playlist nào",
                subtitle = "Tạo playlist để sắp xếp phim theo sở thích",
                action = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = C.TextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tạo Playlist mới", color = C.TextPrimary)
                    }
                }
            )
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp)
            ) {
                items(playlists.size) { idx ->
                    val pl = playlists[idx]
                    PlaylistCard(
                        playlist = pl,
                        onClick = { onPlaylistClick(pl.id, pl.name) },
                        onDelete = { showDeleteConfirm = pl.id }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Create dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
            title = { Text("📋 Tạo Playlist mới", color = C.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Tên playlist...", color = C.TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = C.TextPrimary,
                        unfocusedTextColor = C.TextPrimary,
                        focusedBorderColor = C.Primary,
                        unfocusedBorderColor = C.SurfaceVariant
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            PlaylistManager.createPlaylist(newPlaylistName.trim())
                            showCreateDialog = false
                            newPlaylistName = ""
                        }
                    }
                ) { Text("Tạo", color = C.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                    Text("Huỷ", color = C.TextSecondary)
                }
            },
            containerColor = C.Surface
        )
    }

    // Delete confirm
    showDeleteConfirm?.let { id ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Xoá playlist?", color = C.TextPrimary) },
            text = { Text("Playlist và tất cả phim trong đó sẽ bị xoá.", color = C.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { PlaylistManager.deletePlaylist(id); showDeleteConfirm = null }) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Huỷ", color = C.TextSecondary) }
            },
            containerColor = C.Surface
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text("📋", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(playlist.name, color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("${playlist.items.size} phim", color = C.TextSecondary, fontSize = 13.sp)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete", tint = C.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════
// C-5: Playlist Detail Screen
// ═══════════════════════════════════════════════
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    val playlists by PlaylistManager.playlists.collectAsState(initial = emptyList())
    val playlist = playlists.find { it.id == playlistId }

    if (playlist == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Column(Modifier.fillMaxSize().background(C.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
            }
            Column {
                Text(playlist.name, color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${playlist.items.size} phim", color = C.TextSecondary, fontSize = 12.sp)
            }
        }

        if (playlist.items.isEmpty()) {
            EmptyStateView(
                emoji = "🎬",
                title = "Playlist trống",
                subtitle = "Thêm phim từ trang Detail"
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp, 0.dp, 8.dp, 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlist.items, key = { it.slug }) { item ->
                    WatchlistMovieCard(
                        item = item,
                        onClick = { onMovieClick(item.slug) },
                        onRemove = { PlaylistManager.removeFromPlaylist(playlistId, item.slug) }
                    )
                }
            }
        }
    }
}

// needed for LazyColumn in PlaylistListScreen
private fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    block: @Composable (Int) -> Unit
) {
    repeat(count) { idx -> item { block(idx) } }
}
