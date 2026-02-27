package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily

/**
 * PlayerSeekSection — Seek tooltip, red seekbar, time display.
 */
@Composable
fun PlayerSeekSection(
    player: ExoPlayer,
    currentPos: Long,
    duration: Long,
    showRemaining: Boolean,
    isSeekbarDragging: Boolean,
    seekbarDragFraction: Float,
    onSeek: (Long) -> Unit,
    onSeekbarDrag: (Float) -> Unit,
    onSeekbarDragEnd: () -> Unit,
    onToggleRemaining: () -> Unit,
) {
    // Seek tooltip
    AnimatedVisibility(
        visible = isSeekbarDragging && duration > 0,
        enter = fadeIn(tween(80)), exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                formatTime((seekbarDragFraction * duration).toLong()),
                color = Color.White, fontFamily = JakartaFamily,
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(C.Primary.copy(0.88f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
    }

    // Red Seekbar
    Slider(
        value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
        onValueChange = { fraction ->
            onSeekbarDrag(fraction)
            val seekTo = (fraction * duration).toLong()
            player.seekTo(seekTo)
            onSeek(seekTo)
        },
        onValueChangeFinished = onSeekbarDragEnd,
        colors = SliderDefaults.colors(
            thumbColor = C.Primary, activeTrackColor = C.Primary,
            inactiveTrackColor = Color.White.copy(0.25f)
        ),
        modifier = Modifier.fillMaxWidth().height(24.dp)
    )

    // Time display — tap to toggle elapsed ↔ remaining
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            if (showRemaining && duration > 0) "-${formatTime(duration - currentPos)}"
            else "${formatTime(currentPos)} / ${formatTime(duration)}",
            color = Color.White.copy(0.8f), fontFamily = InterFamily, fontSize = 12.sp,
            modifier = Modifier.background(Color.White.copy(0.08f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clickable { onToggleRemaining() }
        )
    }
}
