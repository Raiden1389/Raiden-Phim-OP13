package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.ui.theme.C

/**
 * Gesture HUD indicators + vertical slider columns for brightness/volume.
 */

/** Floating brightness indicator shown while dragging (controls hidden) */
@Composable
fun BrightnessIndicator(brightness: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (brightness > 0.5f) Icons.Default.LightMode else Icons.Default.BrightnessLow,
                "Brightness", tint = Color.White, modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("${(brightness * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Floating volume indicator shown while dragging (controls hidden) */
@Composable
fun VolumeIndicator(volume: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, "Volume", tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text("${(volume * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Vertical slider column used in controls overlay for brightness/volume */
@Composable
fun VerticalSliderColumn(
    value: Float,
    icon: ImageVector,
    fillColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.height(120.dp).width(28.dp)
                .background(Color.White.copy(0.1f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(value)
                    .background(fillColor, RoundedCornerShape(14.dp))
                    .align(Alignment.BottomCenter)
            )
            Box(
                modifier = Modifier.offset(y = -(value * 112).dp)
                    .size(14.dp).background(thumbColor, RoundedCornerShape(50))
                    .align(Alignment.BottomCenter)
            )
        }
    }
}
