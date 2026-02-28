package xyz.raidenhub.phim.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/**
 * SettingsScreen — Orchestrator only.
 *
 * Delegates each section to its own composable file:
 * - AppearanceSection.kt    (HomeLayout + CardShape)
 * - PlaybackSection.kt      (Auto-play + Quality)
 * - NotificationSection.kt  (Episode notifications)
 * - FshareSettingsSection.kt (Login/VIP)
 * - HomeSortSection.kt      (Section ordering + Hero filter)
 * - GenreFilterSection.kt   (Genre chips + Country info)
 * - DataManagementSection.kt (Clear history/favorites/search)
 * - BackupSection.kt        (Export/Import + SuperStream test + About)
 */
@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
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

        item { AppearanceSection() }
        item { SettingsDivider() }
        item { PlaybackSection() }
        item { SettingsDivider() }
        item { NotificationSection() }
        item { SettingsDivider() }
        item { FshareSettingsSection() }
        item { SettingsDivider() }
        item { HomeSortSection() }
        item { SettingsDivider() }
        item { CountrySourceInfo() }
        item { SettingsDivider() }
        item { GenreFilterSection() }
        item { SettingsDivider() }
        item { DataManagementSection() }
        item { BackupSection() }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = C.Surface, thickness = 1.dp)
    Spacer(Modifier.height(24.dp))
}
