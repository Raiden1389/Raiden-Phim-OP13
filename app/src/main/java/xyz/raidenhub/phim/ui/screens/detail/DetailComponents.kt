package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import xyz.raidenhub.phim.ui.theme.C

// ═══ A-8: Dynamic Color extraction from poster ═══

@Composable
fun rememberDominantColor(imageUrl: String): Color {
    var dominantColor by remember { mutableStateOf(C.Primary) }
    val context = LocalContext.current
    LaunchedEffect(imageUrl) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(128) // small for speed
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                swatch?.let { dominantColor = Color(it.rgb) }
            }
        } catch (_: Exception) { /* fallback to C.Primary */ }
    }
    return dominantColor
}

// ═══ Badge helpers ═══

@Composable
fun Badge3(text: String, color: Color) {
    Text(
        text,
        color = C.TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(0.85f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

