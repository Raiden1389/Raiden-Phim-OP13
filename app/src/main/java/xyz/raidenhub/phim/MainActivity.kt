package xyz.raidenhub.phim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.SettingsManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.navigation.AppNavigation
import xyz.raidenhub.phim.ui.theme.RaidenPhimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init
        ApiClient.cacheDir = cacheDir
        FavoriteManager.init(this)
        WatchHistoryManager.init(this)
        SettingsManager.init(this)

        setContent {
            RaidenPhimTheme {
                AppNavigation()
            }
        }
    }
}
