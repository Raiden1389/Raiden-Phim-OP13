package xyz.raidenhub.phim.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.raidenhub.phim.ui.theme.C

// ═══════════════════════════════════════════════════════════
// Shared shimmer brush — gradient sweep animation
// ═══════════════════════════════════════════════════════════

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        C.Surface,
        C.SurfaceVariant.copy(alpha = 0.7f),
        C.Surface
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

// ═══════════════════════════════════════════════════════════
// ShimmerBox — a basic shimmer placeholder
// ═══════════════════════════════════════════════════════════

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val brush = rememberShimmerBrush()
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius)).background(brush))
}

// ═══════════════════════════════════════════════════════════
// Detail Screen Shimmer Skeleton
// ═══════════════════════════════════════════════════════════

@Composable
fun ShimmerDetailScreen() {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // Backdrop shimmer
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(brush)
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))

            // Title shimmer
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(8.dp))

            // Subtitle shimmer
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(16.dp))

            // Rating badges shimmer
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .width(60.dp)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Play button shimmer
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(16.dp))

            // Cast shimmer row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(5) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .width(40.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Description shimmer
            repeat(4) {
                Box(
                    Modifier
                        .fillMaxWidth(if (it == 3) 0.6f else 1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(Modifier.height(6.dp))
            }
            Spacer(Modifier.height(16.dp))

            // Episode grid shimmer
            Box(
                Modifier
                    .width(100.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) {
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Category / Search Grid Shimmer
// ═══════════════════════════════════════════════════════════

@Composable
fun ShimmerGrid(columns: Int = 3, rows: Int = 4) {
    val brush = rememberShimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        repeat(rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(columns) {
                    Column(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
