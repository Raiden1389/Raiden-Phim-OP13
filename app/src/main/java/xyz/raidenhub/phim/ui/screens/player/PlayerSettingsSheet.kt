package xyz.raidenhub.phim.ui.screens.player

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.ui.theme.C

/**
 * PlayerSettingsSheet — Mark intro/outro settings + promote dialog.
 * Extracted from PlayerScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsSheet(
    showSheet: Boolean,
    slug: String,
    movieCountry: String,
    currentPos: Long,
    playerCurrentPosition: Long,
    effectiveConfig: IntroOutroManager.SeriesConfig?,
    onConfigChanged: suspend () -> IntroOutroManager.SeriesConfig?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var settingsHasOverride by remember { mutableStateOf(false) }
    var settingsCountryDefault by remember { mutableStateOf<IntroOutroManager.SeriesConfig?>(null) }
    var showPromoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSheet, movieCountry) {
        if (showSheet) {
            settingsHasOverride = IntroOutroManager.hasSeriesOverride(slug)
            settingsCountryDefault = IntroOutroManager.getCountryDefault(movieCountry)
        }
    }

    if (showSheet) {
        val countryName = IntroOutroManager.getCountryDisplayName(movieCountry)
        val countryDefault = settingsCountryDefault
        val hasOverride = settingsHasOverride

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = C.Surface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("⚙️ Player Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text("Đánh dấu intro/outro • $countryName", fontSize = 13.sp, color = C.TextSecondary)
                Spacer(Modifier.height(16.dp))

                // Config status
                val cfg = effectiveConfig
                if (cfg != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = C.SurfaceVariant) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val sourceLabel = if (hasOverride) "📌 Config riêng (series)" else "⭐ Mặc định $countryName"
                            Text(sourceLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Accent)
                            Spacer(Modifier.height(4.dp))
                            if (cfg.introStartMs >= 0) Text("   Intro Start: ${formatTime(cfg.introStartMs)}", fontSize = 12.sp, color = C.TextSecondary)
                            if (cfg.introEndMs > 0) Text("   Intro End: ${formatTime(cfg.introEndMs)}", fontSize = 12.sp, color = C.TextSecondary)
                            if (cfg.outroStartMs > 0) Text("   Outro Start: ${formatTime(cfg.outroStartMs)}", fontSize = 12.sp, color = C.TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (hasOverride && countryDefault != null) {
                        Text(
                            "   ↳ Mặc định $countryName: Intro ${formatTime(countryDefault.introEndMs)}, Outro ${formatTime(countryDefault.outroStartMs)}",
                            fontSize = 11.sp, color = C.TextMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                } else {
                    Text("❌ Chưa có config", fontSize = 12.sp, color = C.TextMuted)
                    if (countryDefault != null) {
                        Text("   ⭐ Mặc định $countryName có sẵn", fontSize = 11.sp, color = C.Accent)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Current position
                Surface(shape = RoundedCornerShape(8.dp), color = C.Primary.copy(0.15f)) {
                    Text(
                        "⏱ Vị trí hiện tại: ${formatTime(currentPos)}", fontSize = 13.sp,
                        fontWeight = FontWeight.Medium, color = C.Primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Mark buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            IntroOutroManager.saveIntroStart(slug, playerCurrentPosition)
                            scope.launch { onConfigChanged() }
                            Toast.makeText(context, "✅ Intro Start: ${formatTime(playerCurrentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("📌 Intro\nStart", fontSize = 11.sp, lineHeight = 14.sp) }

                    Button(
                        onClick = {
                            IntroOutroManager.saveIntroEnd(slug, playerCurrentPosition)
                            scope.launch { onConfigChanged() }
                            Toast.makeText(context, "✅ Intro End: ${formatTime(playerCurrentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                    ) { Text("📌 Intro\nEnd", fontSize = 11.sp, lineHeight = 14.sp) }

                    Button(
                        onClick = {
                            IntroOutroManager.saveOutroStart(slug, playerCurrentPosition)
                            scope.launch { onConfigChanged() }
                            Toast.makeText(context, "✅ Outro Start: ${formatTime(playerCurrentPosition)}", Toast.LENGTH_SHORT).show()
                            showPromoteDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = C.Accent)
                    ) { Text("📌 Outro\nStart", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Black) }
                }

                Spacer(Modifier.height(12.dp))

                // Reset buttons
                if (hasOverride) {
                    TextButton(
                        onClick = {
                            IntroOutroManager.resetConfig(slug)
                            scope.launch { onConfigChanged() }
                            Toast.makeText(context, "🗑 Đã xoá config riêng → dùng mặc định $countryName", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, "Reset", tint = C.Error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Xoá config riêng (series)", color = C.Error, fontSize = 13.sp)
                    }
                }
                if (countryDefault != null) {
                    TextButton(
                        onClick = {
                            IntroOutroManager.resetCountryDefault(movieCountry)
                            scope.launch { onConfigChanged() }
                            Toast.makeText(context, "🗑 Đã xoá mặc định $countryName", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, "Reset Country", tint = C.TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Xoá mặc định $countryName", color = C.TextMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ═══ PROMOTE DIALOG ═══
    if (showPromoteDialog && movieCountry.isNotBlank()) {
        val countryName = IntroOutroManager.getCountryDisplayName(movieCountry)
        AlertDialog(
            onDismissRequest = { showPromoteDialog = false },
            containerColor = C.Surface,
            title = { Text("🌏 Áp dụng cho tất cả phim $countryName?", fontSize = 16.sp, color = Color.White) },
            text = {
                Text(
                    "Config vừa mark sẽ được dùng làm mặc định cho tất cả phim $countryName chưa có config riêng.",
                    fontSize = 13.sp, color = C.TextSecondary
                )
            },
            dismissButton = {
                TextButton(onClick = { showPromoteDialog = false }) {
                    Text("Chỉ series này", color = C.TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        IntroOutroManager.promoteToCountryDefault(slug, movieCountry)
                        scope.launch { onConfigChanged() }
                        Toast.makeText(context, "⭐ Đã đặt mặc định cho phim $countryName", Toast.LENGTH_SHORT).show()
                        showPromoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                ) { Text("✅ Tất cả phim $countryName") }
            }
        )
    }
}
