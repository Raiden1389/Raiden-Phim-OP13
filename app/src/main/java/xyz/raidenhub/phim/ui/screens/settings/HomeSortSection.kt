package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/** 🗂️ Sắp xếp trang chủ + 🚫 Hero Carousel Filter */
@Composable
fun HomeSortSection() {
    // ═══ Section ordering ═══
    Text("🗂️ Sắp xếp trang chủ", color = C.TextPrimary, fontSize = 18.sp, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Text("Kéo các nút ↑↓ để thay đổi thứ tự hiển thị", color = C.TextSecondary, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))

    val sectionOrder by SectionOrderManager.order.collectAsState(initial = emptyList())
    val sectionVisibility by SectionOrderManager.visibility.collectAsState(initial = emptyMap())

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sectionOrder.forEachIndexed { idx, id ->
            val info = SectionOrderManager.getSectionInfo(id)
            val isVisible = sectionVisibility[id] ?: true
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (isVisible) C.Surface else C.Surface.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${info?.emoji ?: "▪"} ${info?.label ?: id}",
                    color = if (isVisible) C.TextPrimary else C.TextMuted,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { SectionOrderManager.toggleVisibility(id) }, modifier = Modifier.size(32.dp)) {
                        Text(if (isVisible) "👁" else "🚫", fontSize = 14.sp)
                    }
                    IconButton(onClick = { SectionOrderManager.moveUp(id) }, enabled = idx > 0, modifier = Modifier.size(32.dp)) {
                        Text("↑", color = if (idx > 0) C.Primary else C.TextMuted, fontSize = 18.sp)
                    }
                    IconButton(onClick = { SectionOrderManager.moveDown(id) }, enabled = idx < sectionOrder.size - 1, modifier = Modifier.size(32.dp)) {
                        Text("↓", color = if (idx < sectionOrder.size - 1) C.Primary else C.TextMuted, fontSize = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { SectionOrderManager.reset() }) {
            Text("↺ Khôi phục mặc định", color = C.TextSecondary, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(24.dp))

    // ═══ Hero Carousel Filter ═══
    val hiddenCount by HeroFilterManager.hiddenCount.collectAsState(initial = 0)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("🚫 Phim bị ẩn khỏi Carousel", color = C.TextPrimary, fontSize = 18.sp, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold)
            Text(if (hiddenCount == 0) "Chưa ẩn phim nào" else "Đang ẩn $hiddenCount phim", color = C.TextSecondary, fontSize = 13.sp)
        }
        if (hiddenCount > 0) {
            TextButton(onClick = { HeroFilterManager.clearAll() }) { Text("Hiện lại tất cả", color = C.Primary, fontSize = 13.sp) }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("Long press vào slide trên trang chủ → \"Bỏ qua phim này\" để ẩn phim khỏi Hero Carousel", color = C.TextMuted, fontSize = 12.sp)
    Spacer(Modifier.height(24.dp))
}
