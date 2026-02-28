package xyz.raidenhub.phim.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.BuildConfig
import xyz.raidenhub.phim.data.repository.FshareRepository
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily

/** 📁 Fshare HD — Login/logout + VIP status */
@Composable
fun FshareSettingsSection() {
    val context = LocalContext.current
    val settingsScope = rememberCoroutineScope()
    val fshareRepo = remember { FshareRepository.getInstance(context) }
    var fsLoggedIn by remember { mutableStateOf(fshareRepo.isLoggedIn()) }
    var fsEmail by remember { mutableStateOf(fshareRepo.getSavedEmail() ?: "") }
    var fsUserInfo by remember { mutableStateOf(fshareRepo.currentUser) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    Text("📁 Fshare HD", color = C.TextPrimary, fontFamily = xyz.raidenhub.phim.ui.theme.JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            fsLoggedIn && fsUserInfo != null -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✅ Đã đăng nhập", color = C.Primary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (fsUserInfo!!.isVip) {
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.background(C.Primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
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
                        fshareRepo.logout(); fsLoggedIn = false; fsUserInfo = null; fsEmail = ""
                        Toast.makeText(context, "✅ Đã đăng xuất Fshare", Toast.LENGTH_SHORT).show()
                    }) { Text("Đăng xuất", color = C.Error, fontSize = 13.sp) }
                }
            }
            isLoggingIn -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Đang đăng nhập Fshare...", color = C.TextSecondary, fontSize = 14.sp)
                }
            }
            else -> {
                Text("Chưa đăng nhập Fshare", color = C.TextSecondary, fontSize = 14.sp)
                if (loginError != null) Text("⚠️ $loginError", color = C.Error, fontSize = 12.sp)
                Text(
                    if (BuildConfig.FSHARE_EMAIL.isNotBlank()) "Tài khoản Fshare đã được cấu hình sẵn trong ứng dụng"
                    else "Cần cấu hình FSHARE_EMAIL trong local.properties",
                    color = C.TextMuted, fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.Primary)
                        .clickable {
                            if (isLoggingIn) return@clickable
                            isLoggingIn = true; loginError = null
                            settingsScope.launch {
                                val result = fshareRepo.autoLogin()
                                result.onSuccess { info -> fsLoggedIn = true; fsUserInfo = info; fsEmail = info.email; Toast.makeText(context, "✅ Đăng nhập Fshare thành công!", Toast.LENGTH_SHORT).show() }
                                result.onFailure { e -> loginError = e.message }
                                isLoggingIn = false
                            }
                        }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("🔑 Đăng nhập Fshare", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
}
