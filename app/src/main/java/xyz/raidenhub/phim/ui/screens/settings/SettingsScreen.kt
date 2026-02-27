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
import xyz.raidenhub.phim.BuildConfig
import xyz.raidenhub.phim.data.local.CardShape
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.HomeLayout
import xyz.raidenhub.phim.data.local.HeroFilterManager
import xyz.raidenhub.phim.data.local.SectionOrderManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.util.Constants
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import xyz.raidenhub.phim.notification.EpisodeCheckWorker
import kotlinx.coroutines.launch



@Composable
fun SettingsScreen() {
    // Country filter bị xóa — scope cố định Hàn/Trung/Mỹ qua Constants.ALLOWED_COUNTRIES
    val selectedGenres by SettingsManager.selectedGenres.collectAsState()
    val autoPlayNext by SettingsManager.autoPlayNext.collectAsState()
    val defaultQuality by SettingsManager.defaultQuality.collectAsState()
    val notifyNewEpisode by SettingsManager.notifyNewEpisode.collectAsState()
    val homeLayout by SettingsManager.homeLayout.collectAsState()   // CN-1
    val cardShape by SettingsManager.cardShape.collectAsState()       // VP-5
    val context = LocalContext.current
    val settingsScope = rememberCoroutineScope()
    var showQualitySheet by remember { mutableStateOf(false) }

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
                fontFamily = JakartaFamily,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // ═══ CN-1: Giao diện ═══
        item {
            Text("🎨 Giao diện", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        // Home Layout picker — 3 options inline
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .padding(16.dp)
            ) {
                Text("🏠 Bố cục trang chủ", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Chọn cách hiển thị phim trên trang chủ", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeLayout.values().forEach { layout ->
                        val isSelected = homeLayout == layout
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) C.Primary.copy(alpha = 0.15f)
                                    else C.Background
                                )
                                .then(
                                    if (isSelected) Modifier
                                    else Modifier
                                )
                                .clickable { SettingsManager.setHomeLayout(layout) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(layout.emoji, fontSize = 20.sp)
                                Text(
                                    layout.label,
                                    color = if (isSelected) C.Primary else C.TextSecondary,
                                    fontFamily = InterFamily,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(2.dp)
                                            .background(C.Primary, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // VP-5: Card Shape picker — 4 kiểu bo góc
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .padding(16.dp)
            ) {
                Text("🃏 Kiểu poster card", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Bo góc các poster phim", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardShape.values().forEach { shape ->
                        val isSelected = cardShape == shape
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) C.Primary.copy(alpha = 0.15f) else C.Background)
                                .clickable { SettingsManager.setCardShape(shape) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Mini preview của shape
                            val previewShape = when (shape) {
                                CardShape.ASYMMETRIC -> RoundedCornerShape(
                                    topStart = 0.dp, topEnd = 8.dp,
                                    bottomStart = 8.dp, bottomEnd = 0.dp
                                )
                                else -> RoundedCornerShape(shape.cornerDp.coerceAtLeast(0).dp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 38.dp)
                                    .clip(previewShape)
                                    .background(
                                        if (isSelected) C.Primary else C.SurfaceVariant
                                    )
                            )
                            Text(
                                shape.emoji,
                                fontSize = 14.sp
                            )
                            Text(
                                shape.label,
                                color = if (isSelected) C.Primary else C.TextSecondary,
                                fontFamily = InterFamily,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ Playback Settings ═══
        item {
            Text("▶️ Phát lại", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                    Text("⏭ Tự động chuyển tập", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Tự chuyển sang tập tiếp theo khi hết", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
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
            Spacer(Modifier.height(8.dp))
        }

        // SE-1: Default playback quality
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable { showQualitySheet = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("📺 Chất lượng mặc định", color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        SettingsManager.ALL_QUALITIES.find { it.first == defaultQuality }?.second ?: "🔄 Tự động",
                        color = C.TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Text("›", color = C.TextSecondary, fontSize = 20.sp)
            }
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ N-1: Notifications ═══
        item {
            Text("🔔 Thông báo", color = C.TextPrimary, fontSize = 18.sp, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable {
                        val newVal = !notifyNewEpisode
                        SettingsManager.setNotifyNewEpisode(newVal)
                        if (newVal) EpisodeCheckWorker.schedule(context)
                        else EpisodeCheckWorker.cancel(context)
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🎞️ Tập mới yêu thích", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Thông báo khi phim yêu thích ra tập mới (kiểm tra mỗi 6h)", color = C.TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = notifyNewEpisode,
                    onCheckedChange = { newVal ->
                        SettingsManager.setNotifyNewEpisode(newVal)
                        if (newVal) EpisodeCheckWorker.schedule(context)
                        else EpisodeCheckWorker.cancel(context)
                    },
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

        // ═══ Fshare HD Settings ═══
        item {
            Text("📁 Fshare HD", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }

        item {
            val fshareRepo = remember { xyz.raidenhub.phim.data.repository.FshareRepository.getInstance(context) }
            var fsLoggedIn by remember { mutableStateOf(fshareRepo.isLoggedIn()) }
            var fsEmail by remember { mutableStateOf(fshareRepo.getSavedEmail() ?: "") }
            var fsUserInfo by remember { mutableStateOf(fshareRepo.currentUser) }
            var isLoggingIn by remember { mutableStateOf(false) }
            var loginError by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    fsLoggedIn && fsUserInfo != null -> {
                        // Logged in — show user info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✅ Đã đăng nhập", color = C.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (fsUserInfo!!.isVip) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(C.Primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("VIP 🟢", color = C.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(fsEmail, color = C.TextSecondary, fontSize = 12.sp)
                                if (fsUserInfo!!.expireDate.isNotEmpty()) {
                                    Text("Hết hạn: ${fsUserInfo!!.expireDate}", color = C.TextMuted, fontSize = 11.sp)
                                }
                            }
                            TextButton(onClick = {
                                fshareRepo.logout()
                                fsLoggedIn = false
                                fsUserInfo = null
                                fsEmail = ""
                                Toast.makeText(context, "✅ Đã đăng xuất Fshare", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Đăng xuất", color = C.Error, fontSize = 13.sp)
                            }
                        }
                    }
                    isLoggingIn -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Đang đăng nhập Fshare...", color = C.TextSecondary, fontSize = 14.sp)
                        }
                    }
                    else -> {
                        // Not logged in — show login button
                        Text("Chưa đăng nhập Fshare", color = C.TextSecondary, fontSize = 14.sp)
                        if (loginError != null) {
                            Text("⚠️ $loginError", color = C.Error, fontSize = 12.sp)
                        }
                        val hasBuiltinCreds = BuildConfig.FSHARE_EMAIL.isNotBlank()
                        Text(
                            if (hasBuiltinCreds) "Tài khoản Fshare đã được cấu hình sẵn trong ứng dụng"
                            else "Cần cấu hình FSHARE_EMAIL trong local.properties",
                            color = C.TextMuted, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(C.Primary)
                                .clickable {
                                    if (isLoggingIn) return@clickable
                                    isLoggingIn = true
                                    loginError = null
                                    settingsScope.launch {
                                        val result = fshareRepo.autoLogin()
                                        result.onSuccess { info ->
                                            fsLoggedIn = true
                                            fsUserInfo = info
                                            fsEmail = info.email
                                            Toast.makeText(context, "✅ Đăng nhập Fshare thành công!", Toast.LENGTH_SHORT).show()
                                        }
                                        result.onFailure { e ->
                                            loginError = e.message
                                        }
                                        isLoggingIn = false
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔑 Đăng nhập Fshare", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // ═══ H-6: Sắp xếp các hàng phim ═══
        item {
            Text("🗂️ Sắp xếp trang chủ", color = C.TextPrimary, fontSize = 18.sp, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Kéo các nút ↑↓ để thay đổi thứ tự hiển thị", color = C.TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }

        // H-6: Render each section with move up/down + visibility toggle buttons
        item {
            val sectionOrder by SectionOrderManager.order.collectAsState(initial = emptyList())
            val sectionVisibility by SectionOrderManager.visibility.collectAsState(initial = emptyMap())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sectionOrder.forEachIndexed { idx, id ->
                    val info = SectionOrderManager.getSectionInfo(id)
                    val isVisible = sectionVisibility[id] ?: true
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isVisible) C.Surface else C.Surface.copy(alpha = 0.4f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${info?.emoji ?: "▪"} ${info?.label ?: id}",
                            color = if (isVisible) C.TextPrimary else C.TextMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            // Visibility toggle
                            IconButton(
                                onClick = { SectionOrderManager.toggleVisibility(id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    if (isVisible) "👁" else "🚫",
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(
                                onClick = { SectionOrderManager.moveUp(id) },
                                enabled = idx > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("↑", color = if (idx > 0) C.Primary else C.TextMuted, fontSize = 18.sp)
                            }
                            IconButton(
                                onClick = { SectionOrderManager.moveDown(id) },
                                enabled = idx < sectionOrder.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
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
        }

        // ═══ H-1: Hero Carousel Filter ═══
        item {
            val hiddenCount by HeroFilterManager.hiddenCount.collectAsState(initial = 0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🚫 Phim bị ẩn khỏi Carousel", color = C.TextPrimary, fontSize = 18.sp, fontFamily = JakartaFamily, fontWeight = FontWeight.Bold)
                    Text(
                        if (hiddenCount == 0) "Chưa ẩn phim nào" else "Đang ẩn $hiddenCount phim",
                        color = C.TextSecondary, fontSize = 13.sp
                    )
                }
                if (hiddenCount > 0) {
                    TextButton(onClick = { HeroFilterManager.clearAll() }) {
                        Text("Hiện lại tất cả", color = C.Primary, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Long press vào slide trên trang chủ → \"Bỏ qua phim này\" để ẩn phim khỏi Hero Carousel",
                color = C.TextMuted, fontSize = 12.sp
            )
            Spacer(Modifier.height(24.dp))
        }

        // Divider
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(24.dp))
        }

        // Country Filter đã bị xóa — Scope cố định: 🇰🇷 Hàn / 🇨🇳 Trung / 🇺🇸 Mỹ
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🌍 Nguồn phim", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        Constants.ALLOWED_COUNTRIES.joinToString(" · ") {
                            when(it) {
                                "han-quoc"   -> "🇰🇷 Hàn Quốc"
                                "trung-quoc" -> "🇨🇳 Trung Quốc"
                                "au-my"      -> "🇺🇸 Âu Mỹ"
                                else -> it
                            }
                        },
                        color = C.TextSecondary, fontSize = 12.sp
                    )
                }
                Text("Cố định", color = C.TextMuted, fontSize = 11.sp)
            }
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
                    Text("🎭 Thể loại", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
            Text("🗂️ Dữ liệu", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                        xyz.raidenhub.phim.data.local.SearchHistoryManager.clearAll(context)
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

        // SE-6: Export / Import backup
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            Text("📦 Sao lưu & Khôi phục", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable {
                        try {
                            val json = SettingsManager.exportBackup(context)
                            val fname = "raidenphim_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            dir.mkdirs()
                            File(dir, fname).writeText(json)
                            Toast.makeText(context, "✅ Đã xuất: Downloads/$fname", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ Lỗi xuất: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("📤 Xuất dữ liệu", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Lưu yêu thích + lịch sử ra Downloads", color = C.TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        item {
            var showImportConfirm by remember { mutableStateOf(false) }
            var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) { pendingImportUri = uri; showImportConfirm = true }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(C.Surface)
                    .clickable { importLauncher.launch(arrayOf("application/json")) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("📥 Nhập dữ liệu", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Khôi phục từ file backup .json", color = C.TextSecondary, fontSize = 12.sp)
                }
            }
            if (showImportConfirm && pendingImportUri != null) {
                AlertDialog(
                    onDismissRequest = { showImportConfirm = false },
                    title = { Text("Xác nhận nhập", color = C.TextPrimary) },
                    text = { Text("Dữ liệu hiện tại sẽ bị ghi đè. Tiếp tục?", color = C.TextSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            try {
                                val json = context.contentResolver.openInputStream(pendingImportUri!!)?.bufferedReader()?.readText() ?: ""
                                SettingsManager.importBackup(context, json)
                                showImportConfirm = false
                                Toast.makeText(context, "✅ Đã nhập dữ liệu", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "❌ Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("Nhập", color = C.Primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportConfirm = false }) { Text("Huỷ", color = C.TextSecondary) }
                    },
                    containerColor = C.Surface
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        // ═══ DEBUG: SuperStream API Test ═══
        item {
            HorizontalDivider(color = C.Surface, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))
            var testResult by remember { mutableStateOf("") }
            var isTesting by remember { mutableStateOf(false) }

            Text("🧪 SuperStream Test", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Test 1: ShowBox share_link (Cloudflare test)
                Button(
                    onClick = {
                        isTesting = true
                        testResult = "Testing..."
                        Thread {
                            try {
                                val client = okhttp3.OkHttpClient.Builder()
                                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                                    .build()
                                // Test showbox.media share_link — Two and a Half Men (ID:92, type:2=TV)
                                val req = okhttp3.Request.Builder()
                                    .url("https://www.showbox.media/index/share_link?id=92&type=2")
                                    .header("Accept-Language", "en")
                                    .header("User-Agent", "okhttp/3.2.0")
                                    .build()
                                val resp = client.newCall(req).execute()
                                val body = resp.body?.string() ?: ""
                                val msg = "ShowBox: ${resp.code}\n$body"
                                android.util.Log.d("SuperStream", msg)
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    testResult = msg
                                    isTesting = false
                                }
                            } catch (e: Exception) {
                                val msg = "Error: ${e.message}"
                                android.util.Log.e("SuperStream", msg)
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    testResult = msg
                                    isTesting = false
                                }
                            }
                        }.start()
                    },
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                ) { Text("Test ShowBox", fontSize = 12.sp) }

                // Test 2: FebBox file_list (should always work)
                Button(
                    onClick = {
                        isTesting = true
                        testResult = "Testing FebBox..."
                        Thread {
                            try {
                                val client = okhttp3.OkHttpClient()
                                val req = okhttp3.Request.Builder()
                                    .url("https://www.febbox.com/file/file_share_list?share_key=fNBTg8at")
                                    .header("Accept-Language", "en")
                                    .build()
                                val resp = client.newCall(req).execute()
                                val body = resp.body?.string() ?: ""
                                val msg = "FebBox: ${resp.code}\n${body.take(300)}"
                                android.util.Log.d("SuperStream", msg)
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    testResult = msg
                                    isTesting = false
                                }
                            } catch (e: Exception) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    testResult = "Error: ${e.message}"
                                    isTesting = false
                                }
                            }
                        }.start()
                    },
                    enabled = !isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = C.Surface)
                ) { Text("Test FebBox", fontSize = 12.sp, color = C.TextPrimary) }
            }

            if (testResult.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    testResult,
                    color = if (testResult.contains("200")) C.Primary else C.TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = InterFamily,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.Surface, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
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
                "📱 RaidenPhim v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                color = C.TextMuted,
                fontFamily = InterFamily,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(80.dp))
        }
    }

    // SE-1: Quality selector bottom sheet via AlertDialog
    if (showQualitySheet) {
        AlertDialog(
            onDismissRequest = { showQualitySheet = false },
            title = { Text("📺 Chọn chất lượng mặc định", color = C.TextPrimary) },
            text = {
                Column {
                    SettingsManager.ALL_QUALITIES.forEach { (slug, label) ->
                        val isSelected = slug == defaultQuality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
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
