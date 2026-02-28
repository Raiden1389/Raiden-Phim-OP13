package xyz.raidenhub.phim.ui.screens.settings

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 📦 Sao lưu & Khôi phục + SuperStream Test + About */
@Composable
fun BackupSection() {
    val context = LocalContext.current

    HorizontalDivider(color = C.Surface, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
    Text("📦 Sao lưu & Khôi phục", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    // Export
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable {
                try {
                    val json = SettingsManager.exportBackup(context)
                    val fname = "raidenphim_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    File(dir, fname).writeText(json)
                    Toast.makeText(context, "✅ Đã xuất: Downloads/$fname", Toast.LENGTH_LONG).show()
                } catch (e: Exception) { Toast.makeText(context, "❌ Lỗi xuất: ${e.message}", Toast.LENGTH_SHORT).show() }
            }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("📤 Xuất dữ liệu", color = C.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("Lưu yêu thích + lịch sử ra Downloads", color = C.TextSecondary, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(8.dp))

    // Import
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { pendingImportUri = uri; showImportConfirm = true }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable { importLauncher.launch(arrayOf("application/json")) }.padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
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
                    } catch (e: Exception) { Toast.makeText(context, "❌ Lỗi: ${e.message}", Toast.LENGTH_SHORT).show() }
                }) { Text("Nhập", color = C.Primary) }
            },
            dismissButton = { TextButton(onClick = { showImportConfirm = false }) { Text("Huỷ", color = C.TextSecondary) } },
            containerColor = C.Surface
        )
    }
    Spacer(Modifier.height(24.dp))

    // ═══ SuperStream Test ═══
    SuperStreamTestSection()

    // ═══ About ═══
    HorizontalDivider(color = C.Surface, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
    Text("💡 Bỏ trống = hiện tất cả. Chọn quốc gia/thể loại → chỉ hiện phim phù hợp trên Trang chủ.", color = C.TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(16.dp))
    Text("📱 RaidenPhim v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})", color = C.TextMuted, fontFamily = InterFamily, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(80.dp))
}

/** 🧪 SuperStream API Test — debug section */
@Composable
private fun SuperStreamTestSection() {
    HorizontalDivider(color = C.Surface, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
    var testResult by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }

    Text("🧪 SuperStream Test", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                isTesting = true; testResult = "Testing..."
                Thread {
                    try {
                        val client = okhttp3.OkHttpClient.Builder()
                            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build()
                        val req = okhttp3.Request.Builder()
                            .url("https://www.showbox.media/index/share_link?id=92&type=2")
                            .header("Accept-Language", "en").header("User-Agent", "okhttp/3.2.0").build()
                        val resp = client.newCall(req).execute()
                        val body = resp.body?.string() ?: ""
                        val msg = "ShowBox: ${resp.code}\n$body"
                        android.os.Handler(android.os.Looper.getMainLooper()).post { testResult = msg; isTesting = false }
                    } catch (e: Exception) {
                        val msg = "Error: ${e.message}"
                        android.os.Handler(android.os.Looper.getMainLooper()).post { testResult = msg; isTesting = false }
                    }
                }.start()
            },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
        ) { Text("Test ShowBox", fontSize = 12.sp) }

        Button(
            onClick = {
                isTesting = true; testResult = "Testing FebBox..."
                Thread {
                    try {
                        val client = okhttp3.OkHttpClient()
                        val req = okhttp3.Request.Builder()
                            .url("https://www.febbox.com/file/file_share_list?share_key=fNBTg8at")
                            .header("Accept-Language", "en").build()
                        val resp = client.newCall(req).execute()
                        val body = resp.body?.string() ?: ""
                        val msg = "FebBox: ${resp.code}\n${body.take(300)}"
                        android.os.Handler(android.os.Looper.getMainLooper()).post { testResult = msg; isTesting = false }
                    } catch (e: Exception) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post { testResult = "Error: ${e.message}"; isTesting = false }
                    }
                }.start()
            },
            enabled = !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = C.Surface)
        ) { Text("Test FebBox", fontSize = 12.sp, color = C.TextPrimary) }
    }

    if (testResult.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(testResult, color = if (testResult.contains("200")) C.Primary else C.TextSecondary, fontSize = 11.sp, fontFamily = InterFamily, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().background(C.Surface, shape = RoundedCornerShape(8.dp)).padding(12.dp))
    }
    Spacer(Modifier.height(16.dp))
}
