package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily

/** ▶️ Phát lại — Auto-play toggle + Quality selector */
@Composable
fun PlaybackSection() {
    val autoPlayNext by SettingsManager.autoPlayNext.collectAsState()
    val defaultQuality by SettingsManager.defaultQuality.collectAsState()
    var showQualitySheet by remember { mutableStateOf(false) }

    Text("▶️ Phát lại", color = C.TextPrimary, fontFamily = xyz.raidenhub.phim.ui.theme.JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    // Auto-play next episode
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable { SettingsManager.setAutoPlayNext(!autoPlayNext) }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("⏭ Tự động chuyển tập", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Tự chuyển sang tập tiếp theo khi hết", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
        }
        Switch(
            checked = autoPlayNext,
            onCheckedChange = { SettingsManager.setAutoPlayNext(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Primary, uncheckedThumbColor = C.TextSecondary, uncheckedTrackColor = C.Surface)
        )
    }
    Spacer(Modifier.height(8.dp))

    // Default quality
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable { showQualitySheet = true }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("📺 Chất lượng mặc định", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(SettingsManager.ALL_QUALITIES.find { it.first == defaultQuality }?.second ?: "🔄 Tự động", color = C.TextSecondary, fontSize = 12.sp)
        }
        Text("›", color = C.TextSecondary, fontSize = 20.sp)
    }
    Spacer(Modifier.height(24.dp))

    // Quality selector dialog
    if (showQualitySheet) {
        AlertDialog(
            onDismissRequest = { showQualitySheet = false },
            title = { Text("📺 Chọn chất lượng mặc định", color = C.TextPrimary) },
            text = {
                Column {
                    SettingsManager.ALL_QUALITIES.forEach { (slug, label) ->
                        val isSelected = slug == defaultQuality
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) C.Primary.copy(0.15f) else C.Surface)
                                .clickable { SettingsManager.setDefaultQuality(slug); showQualitySheet = false }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(label, color = if (isSelected) C.Primary else C.TextPrimary, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (isSelected) Icon(Icons.Default.Check, null, tint = C.Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQualitySheet = false }) { Text("Đóng", color = C.Primary) } },
            containerColor = C.Surface
        )
    }
}
