package xyz.raidenhub.phim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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
    MaterialTheme(
        colorScheme = RaidenDarkScheme,
        content = content
    )
}
