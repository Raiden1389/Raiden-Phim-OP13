package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.CardShape
import xyz.raidenhub.phim.data.local.HomeLayout
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily

/** 🎨 Giao diện — Home Layout + Card Shape pickers */
@Composable
fun AppearanceSection() {
    val homeLayout by SettingsManager.homeLayout.collectAsState()
    val cardShape by SettingsManager.cardShape.collectAsState()

    Text("🎨 Giao diện", color = C.TextPrimary, fontFamily = xyz.raidenhub.phim.ui.theme.JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    // Home Layout picker
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface).padding(16.dp)
    ) {
        Text("🏠 Bố cục trang chủ", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text("Chọn cách hiển thị phim trên trang chủ", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeLayout.values().forEach { layout ->
                val isSelected = homeLayout == layout
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) C.Primary.copy(alpha = 0.15f) else C.Background)
                        .clickable { SettingsManager.setHomeLayout(layout) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(layout.emoji, fontSize = 20.sp)
                        Text(layout.label, color = if (isSelected) C.Primary else C.TextSecondary, fontFamily = InterFamily, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) {
                            Box(modifier = Modifier.width(20.dp).height(2.dp).background(C.Primary, RoundedCornerShape(1.dp)))
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))

    // Card Shape picker
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface).padding(16.dp)
    ) {
        Text("🃏 Kiểu poster card", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text("Bo góc các poster phim", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardShape.values().forEach { shape ->
                val isSelected = cardShape == shape
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) C.Primary.copy(alpha = 0.15f) else C.Background)
                        .clickable { SettingsManager.setCardShape(shape) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val previewShape = when (shape) {
                        CardShape.ASYMMETRIC -> RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 0.dp)
                        else -> RoundedCornerShape(shape.cornerDp.coerceAtLeast(0).dp)
                    }
                    Box(modifier = Modifier.size(width = 28.dp, height = 38.dp).clip(previewShape).background(if (isSelected) C.Primary else C.SurfaceVariant))
                    Text(shape.emoji, fontSize = 14.sp)
                    Text(shape.label, color = if (isSelected) C.Primary else C.TextSecondary, fontFamily = InterFamily, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}
