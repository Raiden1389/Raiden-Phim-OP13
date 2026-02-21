package xyz.raidenhub.phim.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.components.EmptyStateView
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int) -> Unit
) {
    val continueList by WatchHistoryManager.continueList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("📜 Lịch sử xem", color = C.TextPrimary, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
        )

        if (continueList.isEmpty()) {
            EmptyStateView(
                emoji = "🍿",
                title = "Chưa xem phim nào",
                subtitle = "Bắt đầu xem phim để thấy lịch sử ở đây"
            )
        } else {
            Text(
                "${continueList.size} phim đang xem",
                color = C.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(continueList, key = { "${it.slug}_${it.source}" }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.Surface)
                            .clickable {
                                onContinue(item.slug, item.server, item.episode)
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail with progress
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageUtils.cardImage(item.thumbUrl, item.source),
                                contentDescription = item.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Progress overlay at bottom
                            LinearProgressIndicator(
                                progress = { item.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter),
                                color = C.Primary,
                                trackColor = Color.Black.copy(0.5f)
                            )
                            // Duration remaining
                            val remainingMins = ((item.durationMs - item.positionMs) / 60_000).toInt()
                            if (remainingMins > 0) {
                                Text(
                                    "${remainingMins}p còn lại",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(0.7f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                item.name,
                                color = C.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Đang xem: ${item.epName}",
                                color = C.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${(item.progress * 100).toInt()}% hoàn thành",
                                color = C.TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Remove button
                        IconButton(
                            onClick = { WatchHistoryManager.removeContinue(item.slug) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, "Remove", tint = C.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
