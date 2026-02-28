package xyz.raidenhub.phim.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.local.ContinueItem
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils

// ═══ Continue Watching Section ═══

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueWatchingSection(
    continueList: List<ContinueItem>,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String, fshareEpSlug: String) -> Unit,
) {
    if (continueList.isEmpty()) return

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.padding(top = 16.dp)) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 20.dp)
                        .background(C.Primary, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Xem tiếp",
                    color = C.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${continueList.size} phim",
                color = C.TextMuted,
                fontSize = 12.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(continueList, key = { "${it.slug}_${it.source}" }) { item ->
                ContinueWatchingCard(
                    item = item,
                    onClick = {
                        onContinue(item.slug, item.server, item.episode, item.positionMs, item.source, item.episodeSlug)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        WatchHistoryManager.removeContinue(item.slug)
                        Toast.makeText(context, "🗑 Đã xoá", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ═══ Cinematic Landscape Card (190×110dp) ═══

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    item: ContinueItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val pct = (item.progress * 100).toInt().coerceIn(0, 100)
    val timeAgo = remember(item.lastWatched) {
        val diffMs = System.currentTimeMillis() - item.lastWatched
        val mins = diffMs / 60_000
        val hours = mins / 60
        val days = hours / 24
        when {
            mins < 1 -> "Vừa xong"
            mins < 60 -> "${mins}ph trước"
            hours < 24 -> "${hours}h trước"
            days < 7 -> "${days} ngày trước"
            else -> "${days / 7} tuần trước"
        }
    }

    Box(
        modifier = Modifier
            .width(190.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Full-bleed thumbnail (or gradient fallback for old entries)
        if (item.thumbUrl.isNotBlank()) {
            AsyncImage(
                model = ImageUtils.cardImage(item.thumbUrl, item.source),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Gradient placeholder for entries saved without poster
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(C.Primary.copy(0.4f), C.Surface))
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.name.take(20),
                    color = Color.White.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // Bottom gradient scrim
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(0.3f),
                        Color.Black.copy(0.85f)
                    ),
                    startY = 20f
                )
            )
        )

        // Episode badge — top left
        val badgeText = if (item.source == "fshare") {
            "💎 Fshare"
        } else {
            item.epName.ifBlank { "Tập ${item.episode + 1}" }
        }
        Text(
            badgeText,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(C.Primary.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )

        // Time ago — top right
        Text(
            timeAgo,
            color = Color.White.copy(0.85f),
            fontSize = 9.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        )

        // Play button — center
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
                .background(Color.White.copy(0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow, "Play",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Bottom info: title + progress
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$pct%",
                    color = C.Primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(C.Primary, C.Primary.copy(0.7f)))
                        )
                )
            }
        }
    }
}
