package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.notification.EpisodeCheckWorker
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily

/** 🔔 Thông báo — Episode notification toggle */
@Composable
fun NotificationSection() {
    val notifyNewEpisode by SettingsManager.notifyNewEpisode.collectAsState()
    val context = LocalContext.current

    Text("🔔 Thông báo", color = C.TextPrimary, fontSize = 18.sp, fontFamily = xyz.raidenhub.phim.ui.theme.JakartaFamily, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Surface)
            .clickable {
                val newVal = !notifyNewEpisode
                SettingsManager.setNotifyNewEpisode(newVal)
                if (newVal) EpisodeCheckWorker.schedule(context) else EpisodeCheckWorker.cancel(context)
            }.padding(16.dp),
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
                if (newVal) EpisodeCheckWorker.schedule(context) else EpisodeCheckWorker.cancel(context)
            },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = C.Primary, uncheckedThumbColor = C.TextSecondary, uncheckedTrackColor = C.Surface)
        )
    }
    Spacer(Modifier.height(24.dp))
}
