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
import xyz.raidenhub.phim.ui.screens.player.PlayerScreen
import xyz.raidenhub.phim.ui.theme.RaidenPhimTheme

/**
 * Separate Activity cho Player — fullscreen hoàn toàn, không share window/insets với MainActivity.
 * Pattern giống Netflix/YouTube/NewPipe: Player luôn ở Activity riêng.
 *
 * Extras:
 *  slug        — KKPhim/OPhim movie slug (source=kkphim)
 *  server      — server index
 *  episode     — episode index
 *  positionMs  — resume position
 *  source      — "kkphim" (default) | "anime47"
 *  episodeIds  — IntArray (chỉ dùng khi source=anime47)
 *  animeTitle  — String  (chỉ dùng khi source=anime47)
 */
class PlayerActivity : ComponentActivity() {

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

        // Tắt contrast enforcement — thủ phạm chính scrim navy trên Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        super.onCreate(savedInstanceState)

        // Hide system bars immediately
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // Extract args from Intent
        val source      = intent.getStringExtra("source") ?: "kkphim"
        val slug        = intent.getStringExtra("slug") ?: ""
        val server      = intent.getIntExtra("server", 0)
        val episode     = intent.getIntExtra("episode", 0)
        val positionMs  = intent.getLongExtra("positionMs", 0L)
        // Anime47 extras
        val episodeIds  = intent.getIntArrayExtra("episodeIds") ?: intArrayOf()
        val animeTitle  = intent.getStringExtra("animeTitle") ?: ""

        setContent {
            RaidenPhimTheme {
                PlayerScreen(
                    slug           = slug,
                    server         = server,
                    episode        = episode,
                    startPositionMs = positionMs,
                    source         = source,
                    episodeIds     = episodeIds,
                    animeTitle     = animeTitle,
                    onBack         = { finish() }
                )
            }
        }
    }
}
