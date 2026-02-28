package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/** 🎭 Thể loại — Genre chip filter */
@Composable
fun GenreFilterSection() {
    val selectedGenres by SettingsManager.selectedGenres.collectAsState()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("🎭 Thể loại", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(if (selectedGenres.isEmpty()) "Hiện tất cả thể loại" else "${selectedGenres.size} thể loại đã chọn", color = C.TextSecondary, fontSize = 13.sp)
        }
        if (selectedGenres.isNotEmpty()) {
            Text("Xoá bộ lọc", color = C.Primary, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { SettingsManager.clearGenres() }.padding(8.dp))
        }
    }
    Spacer(Modifier.height(12.dp))

    FlowChips(items = SettingsManager.ALL_GENRES, selected = selectedGenres, onToggle = { SettingsManager.toggleGenre(it) })
    Spacer(Modifier.height(24.dp))
}

// ═══ Country source info (fixed scope) ═══
@Composable
fun CountrySourceInfo() {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("🌍 Nguồn phim", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                xyz.raidenhub.phim.util.Constants.ALLOWED_COUNTRIES.joinToString(" · ") {
                    when (it) { "han-quoc" -> "🇰🇷 Hàn Quốc"; "trung-quoc" -> "🇨🇳 Trung Quốc"; "au-my" -> "🇺🇸 Âu Mỹ"; else -> it }
                },
                color = C.TextSecondary, fontSize = 12.sp
            )
        }
        Text("Cố định", color = C.TextMuted, fontSize = 11.sp)
    }
    Spacer(Modifier.height(24.dp))
}

/** FlowChips — reusable chip grid */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowChips(
    items: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (slug, label) ->
            val isActive = slug in selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) C.Primary else C.Surface)
                    .clickable { onToggle(slug) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isActive) {
                    Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, color = if (isActive) Color.White else C.TextSecondary, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
