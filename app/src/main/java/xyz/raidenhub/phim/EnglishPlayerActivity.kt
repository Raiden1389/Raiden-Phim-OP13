package xyz.raidenhub.phim

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import xyz.raidenhub.phim.ui.screens.english.EnglishPlayerScreen
import xyz.raidenhub.phim.ui.theme.RaidenPhimTheme

/**
 * Separate Activity cho English Player — fullscreen riêng, không share insets với MainActivity.
 */
class EnglishPlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ══ FULLSCREEN SETUP — TRƯỚC super.onCreate ══
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        val episodeId = intent.getStringExtra("episodeId") ?: ""
        val mediaId = intent.getStringExtra("mediaId") ?: ""
        val filmName = intent.getStringExtra("filmName") ?: ""

        setContent {
            RaidenPhimTheme {
                EnglishPlayerScreen(
                    episodeId = episodeId,
                    mediaId = mediaId,
                    filmName = filmName,
                    onBack = { finish() }
                )
            }
        }
    }
}
