package xyz.raidenhub.phim.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C

@Composable
fun SettingsScreen() {
    val selectedCountries by SettingsManager.selectedCountries.collectAsState()
    val selectedGenres by SettingsManager.selectedGenres.collectAsState()
    val autoPlayNext by SettingsManager.autoPlayNext.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp)
    ) {
        // Header
        item {
            Text(
                "⚙️ Cài đặt",
                color = C.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // ═══ Playback Settings ═══
        item {
            Text("▶️ Phát lại", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        // Auto-play next episode toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable { SettingsManager.setAutoPlayNext(!autoPlayNext) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("⏭ Tự động chuyển tập", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Tự chuyển sang tập tiếp theo khi hết", color = C.TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = autoPlayNext,
                    onCheckedChange = { SettingsManager.setAutoPlayNext(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = C.Primary,
                        uncheckedThumbColor = C.TextSecondary,
                        uncheckedTrackColor = C.Surface
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ Country Filter ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🌍 Quốc gia", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (selectedCountries.isEmpty()) "Hiện tất cả quốc gia" else "${selectedCountries.size} quốc gia đã chọn",
                        color = C.TextSecondary, fontSize = 13.sp
                    )
                }
                if (selectedCountries.isNotEmpty()) {
                    Text(
                        "Xoá bộ lọc",
                        color = C.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { SettingsManager.clearCountries() }
                            .padding(8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            FlowChips(
                items = SettingsManager.ALL_COUNTRIES,
                selected = selectedCountries,
                onToggle = { SettingsManager.toggleCountry(it) }
            )
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ Genre Filter ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🎭 Thể loại", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (selectedGenres.isEmpty()) "Hiện tất cả thể loại" else "${selectedGenres.size} thể loại đã chọn",
                        color = C.TextSecondary, fontSize = 13.sp
                    )
                }
                if (selectedGenres.isNotEmpty()) {
                    Text(
                        "Xoá bộ lọc",
                        color = C.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { SettingsManager.clearGenres() }
                            .padding(8.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            FlowChips(
                items = SettingsManager.ALL_GENRES,
                selected = selectedGenres,
                onToggle = { SettingsManager.toggleGenre(it) }
            )
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ Data Management ═══
        item {
            Text("🗂️ Dữ liệu", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        // Clear watch history
        item {
            var showConfirmHistory by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable { showConfirmHistory = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🗑️ Xoá lịch sử xem", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Xoá toàn bộ tiến trình xem và đang xem", color = C.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.Delete, "Delete", tint = C.TextSecondary, modifier = Modifier.size(20.dp))
            }
            if (showConfirmHistory) {
                AlertDialog(
                    onDismissRequest = { showConfirmHistory = false },
                    title = { Text("Xác nhận xoá", color = C.TextPrimary) },
                    text = { Text("Bạn có chắc muốn xoá toàn bộ lịch sử xem?", color = C.TextSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            WatchHistoryManager.clearAll()
                            showConfirmHistory = false
                            Toast.makeText(context, "✅ Đã xoá lịch sử xem", Toast.LENGTH_SHORT).show()
                        }) { Text("Xoá", color = C.Primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmHistory = false }) { Text("Huỷ", color = C.TextSecondary) }
                    },
                    containerColor = C.Surface
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Clear favorites
        item {
            var showConfirmFav by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable { showConfirmFav = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("💔 Xoá danh sách yêu thích", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Xoá toàn bộ phim đã lưu", color = C.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.Delete, "Delete", tint = C.TextSecondary, modifier = Modifier.size(20.dp))
            }
            if (showConfirmFav) {
                AlertDialog(
                    onDismissRequest = { showConfirmFav = false },
                    title = { Text("Xác nhận xoá", color = C.TextPrimary) },
                    text = { Text("Bạn có chắc muốn xoá toàn bộ phim yêu thích?", color = C.TextSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            FavoriteManager.clearAll()
                            showConfirmFav = false
                            Toast.makeText(context, "✅ Đã xoá yêu thích", Toast.LENGTH_SHORT).show()
                        }) { Text("Xoá", color = C.Primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmFav = false }) { Text("Huỷ", color = C.TextSecondary) }
                    },
                    containerColor = C.Surface
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // Clear search history
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable {
                        xyz.raidenhub.phim.ui.screens.search.SearchHistoryManager.clearAll(context)
                        Toast.makeText(context, "✅ Đã xoá lịch sử tìm kiếm", Toast.LENGTH_SHORT).show()
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🔍 Xoá lịch sử tìm kiếm", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Xoá toàn bộ từ khoá đã tìm", color = C.TextSecondary, fontSize = 12.sp)
                }
                Icon(Icons.Default.Delete, "Delete", tint = C.TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(24.dp))
        }

        // Info + Version
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                "💡 Bỏ trống = hiện tất cả. Chọn quốc gia/thể loại → chỉ hiện phim phù hợp trên Trang chủ.",
                color = C.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "📱 RaidenPhim v1.6.1",
                color = C.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(
    items: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (slug, label) ->
            val isActive = slug in selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) C.Primary else C.Surface)
                    .clickable { onToggle(slug) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (isActive) {
                    Icon(
                        Icons.Default.Check, "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    color = if (isActive) Color.White else C.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
