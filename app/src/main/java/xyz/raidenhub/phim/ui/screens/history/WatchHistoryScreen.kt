package xyz.raidenhub.phim.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.local.ContinueItem
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.components.EmptyStateView
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, source: String) -> Unit
) {
    val continueList by WatchHistoryManager.continueList.collectAsState(initial = emptyList())

    // MU-3: Stats tab
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("📜 Lịch sử", "📊 Thống kê")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Hoạt động", color = C.TextPrimary, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
        )

        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = C.Background,
            contentColor = C.Primary
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            label,
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = if (selectedTab == i) C.Primary else C.TextSecondary
                        )
                    },
                    selectedContentColor = C.Primary,
                    unselectedContentColor = C.TextSecondary
                )
            }
        }

        when (selectedTab) {
            0 -> HistoryTab(continueList, onMovieClick, onContinue)
            1 -> StatsTab(continueList)
        }
    }
}

// ═══ History Tab ═══
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    continueList: List<ContinueItem>,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, source: String) -> Unit
) {
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
            fontFamily = InterFamily,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(continueList, key = { "${it.slug}_${it.source}" }) { item ->
                // IA-2: Swipe to dismiss (left=xóa, right=pin)
                SwipeHistoryItem(
                    item = item,
                    onContinue = onContinue,
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeHistoryItem(
    item: ContinueItem,
    onContinue: (slug: String, server: Int, episode: Int, source: String) -> Unit,
    onMovieClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left — xóa
                    WatchHistoryManager.removeContinue(item.slug)
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right — pin lên đầu
                    WatchHistoryManager.pinToTop(item.slug)
                    false // reset về Settled (không xóa)
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isLeft = direction == SwipeToDismissBoxValue.EndToStart
            val isRight = direction == SwipeToDismissBoxValue.StartToEnd

            val bgColor by animateColorAsState(
                when {
                    isLeft -> Color(0xFFE53935)     // Delete = red
                    isRight -> Color(0xFF7C4DFF)    // Pin = purple
                    else -> Color.Transparent
                },
                label = "swipe_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isLeft) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (isLeft) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text("Xóa", color = Color.White, fontSize = 11.sp, fontFamily = InterFamily)
                    }
                } else if (isRight) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PushPin, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text("Ghim đầu", color = Color.White, fontSize = 11.sp, fontFamily = InterFamily)
                    }
                }
            }
        },
        enableDismissFromStartToEnd = true,  // swipe right = pin
        enableDismissFromEndToStart = true    // swipe left = delete
    ) {
        HistoryCard(item = item, onContinue = onContinue, onMovieClick = onMovieClick)
    }
}

@Composable
fun HistoryCard(
    item: ContinueItem,
    onContinue: (slug: String, server: Int, episode: Int, source: String) -> Unit,
    onMovieClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .clickable { onContinue(item.slug, item.server, item.episodeIdx, item.source) }
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
                overflow = TextOverflow.Ellipsis,
                fontFamily = InterFamily
            )
            Text(
                "Đang xem: ${item.epName}",
                color = C.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFamily
            )
            Text(
                "${(item.progress * 100).toInt()}% hoàn thành",
                color = C.TextSecondary,
                fontSize = 11.sp,
                fontFamily = InterFamily
            )
        }

        // Swipe hint icon
        Icon(
            Icons.Default.ChevronLeft,
            null,
            tint = C.TextMuted,
            modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
        )
    }
}

// ═══ MU-3: Stats Tab ═══
@Composable
fun StatsTab(continueList: List<ContinueItem>) {
    val totalMovies = continueList.size
    val finishedMovies = continueList.count { it.progress > 0.85f }
    val inProgressMovies = continueList.count { it.progress in 0.05f..0.85f }
    val totalWatchedMs = continueList.sumOf { it.positionMs }
    val totalHours = totalWatchedMs / 3_600_000L
    val totalMins = (totalWatchedMs % 3_600_000L) / 60_000L

    // Source distribution
    val sourceGroups = continueList.groupBy { it.source }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero stat
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(C.Primary.copy(0.3f), C.Primary.copy(0.1f))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m",
                        color = C.Primary,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = JakartaFamily
                    )
                    Text(
                        "⏱️ Tổng thời gian đã xem",
                        color = C.TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = InterFamily
                    )
                }
            }
        }

        // Stats grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    emoji = "🎬",
                    value = "$totalMovies",
                    label = "Phim đã xem",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    emoji = "✅",
                    value = "$finishedMovies",
                    label = "Hoàn thành",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    emoji = "▶️",
                    value = "$inProgressMovies",
                    label = "Đang xem",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Progress bars by source
        if (sourceGroups.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📡 Theo nguồn phim",
                        color = C.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = InterFamily
                    )
                    sourceGroups.forEach { (source, items) ->
                        val fraction = items.size.toFloat() / totalMovies.coerceAtLeast(1)
                        val sourceName = when (source) {
                            "ophim" -> "🎬 OPhim"
                            "kkphim" -> "📺 KKPhim"
                            else -> "🌍 ${source.replaceFirstChar { it.uppercase() }}"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sourceName, color = C.TextPrimary, fontSize = 13.sp, fontFamily = InterFamily)
                                Text("${items.size} phim", color = C.TextSecondary, fontSize = 12.sp, fontFamily = InterFamily)
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = C.Primary,
                                trackColor = C.SurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Top watched (most complete)
        val topMovies = continueList.sortedByDescending { it.progress }.take(5)
        if (topMovies.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(C.Surface)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "🏆 Xem nhiều nhất",
                        color = C.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = InterFamily
                    )
                    topMovies.forEachIndexed { i, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val medals = listOf("🥇", "🥈", "🥉", "4️⃣", "5️⃣")
                            Text(medals.getOrElse(i) { "${i + 1}" }, fontSize = 18.sp)
                            Text(
                                item.name,
                                color = C.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = InterFamily,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${(item.progress * 100).toInt()}%",
                                color = C.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = InterFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(C.Surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 22.sp)
        Text(value, color = C.Primary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JakartaFamily)
        Text(label, color = C.TextSecondary, fontSize = 10.sp, fontFamily = InterFamily, maxLines = 1)
    }
}
