package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.util.ImageUtils
import xyz.raidenhub.phim.util.TextUtils

/** A-6: Parallax backdrop header with gradient, back button, title overlay */
@Composable
fun DetailBackdrop(
    movie: MovieDetail,
    accentColor: Color,
    scrollOffset: Float,
    parallaxProgress: Float,
    onBack: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(androidx.compose.ui.graphics.RectangleShape)
    ) {
        AsyncImage(
            model = ImageUtils.detailImage(movie.posterUrl.ifBlank { movie.thumbUrl }),
            contentDescription = movie.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -scrollOffset * 0.3f
                    val scale = 1f + (parallaxProgress * 0.1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (parallaxProgress * 0.3f)
                }
        )
        // Gradient overlay
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            listOf(
                Color.Transparent,
                C.Background.copy(alpha = 0.3f),
                C.Background.copy(alpha = 0.85f),
                C.Background
            ),
            startY = 50f
        )))
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
        }
        // Title area
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .graphicsLayer {
                    translationY = scrollOffset * 0.2f
                    alpha = 1f - (parallaxProgress * 0.8f)
                }
        ) {
            Text(movie.name, color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (movie.originName.isNotBlank())
                Text(movie.originName, color = C.TextSecondary, fontFamily = InterFamily, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (movie.quality.isNotBlank()) Badge3(movie.quality, accentColor)
                if (movie.lang.isNotBlank()) Badge3(TextUtils.shortLang(movie.lang), C.Badge)
                if (movie.episodeCurrent.isNotBlank()) Badge3(movie.episodeCurrent, C.SurfaceVariant)
            }
        }
    }
}
