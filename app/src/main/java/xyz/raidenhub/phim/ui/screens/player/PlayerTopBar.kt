package xyz.raidenhub.phim.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/**
 * PlayerTopBar — Back, title, speed pill, PiP, lock, settings.
 */
@Composable
fun PlayerTopBar(
    title: String,
    epName: String,
    speedIdx: Int,
    speeds: List<Float>,
    effectiveConfig: IntroOutroManager.SeriesConfig?,
    onBack: () -> Unit,
    onSpeedClick: () -> Unit,
    onLock: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
        val displayTitle = if (epName.isNotBlank()) {
            "$title — ${smartEpLabel(epName, 0)}"
        } else title
        Text(
            displayTitle, color = Color.White, fontFamily = JakartaFamily,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )

        // Speed pill
        Surface(
            shape = RoundedCornerShape(14.dp), color = Color.White.copy(0.15f),
            modifier = Modifier.clickable { onSpeedClick() }
        ) {
            Text(
                "${speeds[speedIdx]}x",
                color = if (speedIdx != 2) C.Accent else Color.White,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(8.dp))

        // PiP — safe-cast to avoid crash in preview/context wrapper
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activity = LocalContext.current as? Activity
            if (activity != null) {
                IconButton(
                    onClick = {
                        val params = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build()
                        activity.enterPictureInPictureMode(params)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.PictureInPicture, "PiP", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Lock
        IconButton(onClick = onLock, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Lock, "Lock", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // Settings
        IconButton(onClick = onShowSettings, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Settings, "Settings",
                tint = if (effectiveConfig != null) C.Accent else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
