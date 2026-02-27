package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sleek track selection bottom sheet for Audio & Subtitle.
 * Mobile-optimized: touch clickable, glassmorphism dark overlay.
 */
@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<TrackInfo>,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(indication = null, interactionSource = null) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A2E))
                .clickable(enabled = false) {} // block click-through
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Track items
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tracks.forEach { track ->
                    TrackItem(
                        track = track,
                        onClick = {
                            onSelect(track.index)
                            onDismiss()
                        }
                    )
                }

                // "Tắt" option for subtitles
                if (title.contains("Phụ đề", ignoreCase = true) || title.contains("Sub", ignoreCase = true)) {
                    TrackItem(
                        track = TrackInfo(
                            index = -1,
                            label = "Tắt phụ đề",
                            isSelected = tracks.none { it.isSelected }
                        ),
                        onClick = {
                            onSelect(-1)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: TrackInfo,
    onClick: () -> Unit
) {
    val accentColor = Color(0xFFE50914)
    val bgColor by animateColorAsState(
        targetValue = when {
            track.isSelected -> Color.White.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        label = "track_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection dot
            Text(
                text = if (track.isSelected) "●" else "○",
                fontSize = 14.sp,
                color = if (track.isSelected) accentColor else Color.White.copy(alpha = 0.4f)
            )

            Text(
                text = track.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (track.isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                fontWeight = if (track.isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        // Language tag
        if (track.language.isNotEmpty()) {
            Text(
                text = track.language.uppercase(),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
