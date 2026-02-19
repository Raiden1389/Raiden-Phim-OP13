package xyz.raidenhub.phim

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import xyz.raidenhub.phim.navigation.AppNavigation
import xyz.raidenhub.phim.notification.EpisodeCheckWorker
import xyz.raidenhub.phim.ui.theme.RaidenPhimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: set cutout mode TRƯỚC super.onCreate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Tắt contrast enforcement — Android 15+ tự inject scrim vào gesture area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Managers đã init trong App.kt (Application level)

        // #34 — Init notification channel + schedule episode check
        EpisodeCheckWorker.createChannel(this)
        EpisodeCheckWorker.schedule(this)

        setContent {
            RaidenPhimTheme {
                AppNavigation()
            }
        }
    }
}
