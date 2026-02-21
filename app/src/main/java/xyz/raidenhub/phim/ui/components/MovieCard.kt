package xyz.raidenhub.phim.ui.components

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.util.ImageUtils
import xyz.raidenhub.phim.util.TextUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favorites by FavoriteManager.favorites.collectAsState()
    val isFav = favorites.any { it.slug == movie.slug }

    // ═══ Press scale animation — Netflix-style ═══
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "card_press"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick ?: {
                    val added = FavoriteManager.toggle(movie.slug, movie.name, movie.thumbUrl)
                    Toast.makeText(
                        context,
                        if (added) "❤️ Đã thêm vào Yêu thích" else "💔 Đã xoá khỏi Yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(C.Surface)
        ) {
            AsyncImage(
                model = ImageUtils.cardImage(movie.thumbUrl, movie.source),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // #6 — Badges: quality + year
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (movie.quality.isNotBlank()) Badge(movie.quality, C.Primary)
                if (movie.lang.isNotBlank()) Badge(TextUtils.shortLang(movie.lang), C.Badge)
            }

            // Year badge (top-right corner when no fav)
            if (movie.year > 0 && !isFav) {
                Badge(
                    text = "${movie.year}",
                    color = C.SurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                )
            }

            // Favorite heart indicator — pulse animation
            if (isFav) {
                val pulse = rememberInfiniteTransition(label = "fav_pulse")
                val heartScale by pulse.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "heart_scale"
                )
                Icon(
                    Icons.Default.Favorite,
                    "Favorited",
                    tint = C.Primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = heartScale
                            scaleY = heartScale
                        }
                )
            }

            // Episode badge
            if (movie.episodeCurrent.isNotBlank()) {
                Badge(
                    text = movie.episodeCurrent,
                    color = C.SurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                )
            }
        }

        Text(
            text = movie.name,
            color = C.TextPrimary,
            fontFamily = InterFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (movie.year > 0) Text("${movie.year}", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 11.sp)
            if (movie.country.isNotEmpty()) {
                Text("• ${movie.country.first().name}", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun Badge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = C.TextPrimary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(color.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
