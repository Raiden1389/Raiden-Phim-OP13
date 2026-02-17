package xyz.raidenhub.phim.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RaidenDarkScheme = darkColorScheme(
    primary = C.Primary,
    onPrimary = C.TextPrimary,
    secondary = C.Accent,
    background = C.Background,
    onBackground = C.TextPrimary,
    surface = C.Surface,
    onSurface = C.TextPrimary,
    surfaceVariant = C.SurfaceVariant,
    onSurfaceVariant = C.TextSecondary,
    error = C.Error,
    onError = C.TextPrimary
)

@Composable
fun RaidenPhimTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = C.Background.toArgb()
            window.navigationBarColor = C.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = RaidenDarkScheme,
        content = content
    )
}
