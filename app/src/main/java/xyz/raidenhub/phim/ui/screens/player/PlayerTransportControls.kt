package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.ui.theme.C

/**
 * PlayerTransportControls — Loading spinner, prev/next, play/pause.
 */
@Composable
fun PlayerTransportControls(
    player: ExoPlayer,
    isFetchingEp: Boolean,
    currentEp: Int,
    hasNext: Boolean,
    onPrevEp: () -> Unit,
    onNextEp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isFetchingEp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
                Spacer(Modifier.height(8.dp))
                Text("⏳ Đang tải tập...", color = Color.White, fontSize = 13.sp)
            }
        } else {
            // Skip previous
            if (currentEp > 0) {
                IconButton(
                    onClick = onPrevEp,
                    modifier = Modifier.align(Alignment.CenterStart).offset(x = (-80).dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            // Play/Pause button with red gradient
            Box(
                modifier = Modifier.size(76.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(C.Primary.copy(0.7f), C.PrimaryDark.copy(0.4f), Color.Transparent),
                            radius = 120f
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable {
                        if (player.isPlaying) player.pause() else player.play()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(62.dp).background(C.Primary.copy(0.35f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause", tint = Color.White, modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Skip next
            if (hasNext) {
                IconButton(
                    onClick = onNextEp,
                    modifier = Modifier.align(Alignment.CenterEnd).offset(x = 80.dp)
                ) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}
