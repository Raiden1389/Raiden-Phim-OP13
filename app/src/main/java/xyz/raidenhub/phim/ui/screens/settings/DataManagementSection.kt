package xyz.raidenhub.phim.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/** 🗂️ Dữ liệu — Clear history/favorites/search */
@Composable
fun DataManagementSection() {
    val context = LocalContext.current

    Text("🗂️ Dữ liệu", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    // Clear watch history
    var showConfirmHistory by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable { showConfirmHistory = true }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
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
                TextButton(onClick = { WatchHistoryManager.clearAll(); showConfirmHistory = false; Toast.makeText(context, "✅ Đã xoá lịch sử xem", Toast.LENGTH_SHORT).show() }) { Text("Xoá", color = C.Primary) }
            },
            dismissButton = { TextButton(onClick = { showConfirmHistory = false }) { Text("Huỷ", color = C.TextSecondary) } },
            containerColor = C.Surface
        )
    }
    Spacer(Modifier.height(8.dp))

    // Clear favorites
    var showConfirmFav by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable { showConfirmFav = true }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
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
                TextButton(onClick = { FavoriteManager.clearAll(); showConfirmFav = false; Toast.makeText(context, "✅ Đã xoá yêu thích", Toast.LENGTH_SHORT).show() }) { Text("Xoá", color = C.Primary) }
            },
            dismissButton = { TextButton(onClick = { showConfirmFav = false }) { Text("Huỷ", color = C.TextSecondary) } },
            containerColor = C.Surface
        )
    }
    Spacer(Modifier.height(24.dp))

    // Clear search history
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable {
                xyz.raidenhub.phim.data.local.SearchHistoryManager.clearAll(context)
                Toast.makeText(context, "✅ Đã xoá lịch sử tìm kiếm", Toast.LENGTH_SHORT).show()
            }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("🔍 Xoá lịch sử tìm kiếm", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Xoá toàn bộ từ khoá đã tìm", color = C.TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.Delete, "Delete", tint = C.TextSecondary, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.height(24.dp))
}
