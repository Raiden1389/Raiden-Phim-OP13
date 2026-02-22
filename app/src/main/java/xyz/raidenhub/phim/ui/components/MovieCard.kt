package xyz.raidenhub.phim.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.WatchlistManager
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
    onPlay: ((String) -> Unit)? = null,  // IA-1: quick play from popup
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favorites by FavoriteManager.favorites.collectAsState(initial = emptyList())
    val isFav by remember(movie.slug) {
        derivedStateOf { favorites.any { it.slug == movie.slug } }
    }
    val watchlist by WatchlistManager.items.collectAsState(initial = emptyList())
    val isInWatchlist by remember(movie.slug) {
        derivedStateOf { watchlist.any { it.slug == movie.slug } }
    }

    // MU-2: Double-tap popup state
    var showInfoPopup by remember { mutableStateOf(false) }

    // IA-1: Long press context menu state
    var showContextMenu by remember { mutableStateOf(false) }

    // ═══ Press scale animation — Netflix-style ═══
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
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
                onLongClick = {
                    // IA-1: Show context menu instead of default long-click
                    showContextMenu = true
                },
                onDoubleClick = {
                    // MU-2: Show info popup
                    showInfoPopup = true
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

    // MU-2: Double-tap info popup
    if (showInfoPopup) {
        Dialog(
            onDismissRequest = { showInfoPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.Surface)
            ) {
                Column {
                    // Poster
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        AsyncImage(
                            model = ImageUtils.cardImage(movie.thumbUrl, movie.source),
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient overlay
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.7f)),
                                    startY = 80f
                                ))
                        )
                        // Badges row
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (movie.quality.isNotBlank()) Badge(movie.quality, C.Primary)
                            if (movie.lang.isNotBlank()) Badge(TextUtils.shortLang(movie.lang), C.Badge)
                            if (movie.year > 0) Badge("${movie.year}", C.SurfaceVariant)
                        }
                        // Title at bottom of poster
                        Text(
                            movie.name,
                            color = Color.White,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        )
                    }
                    // Info row
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (movie.country.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                movie.country.take(3).forEach {
                                    Text("🇳 ${it.name}", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
                                }
                            }
                        }
                        if (movie.episodeCurrent.isNotBlank()) {
                            Text("📺 ${movie.episodeCurrent}", color = C.Primary, fontFamily = InterFamily, fontSize = 13.sp)
                        }
                        // Action buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Play button
                            Button(
                                onClick = {
                                    showInfoPopup = false
                                    onClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("▶️ Xem", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            // Fav toggle
                            IconButton(
                                onClick = {
                                    FavoriteManager.toggle(movie.slug, movie.name, movie.thumbUrl)
                                }
                            ) {
                                Icon(
                                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (isFav) C.Primary else C.TextSecondary
                                )
                            }
                            // Watchlist toggle
                            IconButton(
                                onClick = {
                                    WatchlistManager.toggle(movie.slug, movie.name, movie.thumbUrl)
                                    Toast.makeText(
                                        context,
                                        if (isInWatchlist) "🗑️ Đã xóa khỏi Xem sau" else "🔖 Đã lưu vào Xem sau",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    null,
                                    tint = if (isInWatchlist) C.Primary else C.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // IA-1: Long press context menu
    if (showContextMenu) {
        MovieContextMenu(
            movie = movie,
            isFav = isFav,
            isInWatchlist = isInWatchlist,
            onDismiss = { showContextMenu = false },
            onClick = { showContextMenu = false; onClick() },
            onPlay = onPlay,
            context = context
        )
    }
}

// IA-1: Rich context menu bottom sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieContextMenu(
    movie: Movie,
    isFav: Boolean,
    isInWatchlist: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onPlay: ((String) -> Unit)? = null,
    context: android.content.Context
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Movie header
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageUtils.cardImage(movie.thumbUrl, movie.source),
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(48.dp).aspectRatio(2f/3f).clip(RoundedCornerShape(6.dp))
                )
                Column {
                    Text(movie.name, color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (movie.year > 0) Text("${movie.year}", color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = C.SurfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            // ▶️ Xem ngay
            ContextMenuItem("▶️ Xem ngay", "Mở trang phím") { onClick() }

            // ❤️ / 💔 Favorite
            ContextMenuItem(
                if (isFav) "💔 Xóa khỏi Yêu thích" else "❤️ Thêm vào Yêu thích",
                if (isFav) "Đang trong danh sách yêu thích" else "Lưu phim yêu thích"
            ) {
                FavoriteManager.toggle(movie.slug, movie.name, movie.thumbUrl)
                Toast.makeText(context, if (isFav) "💔 Đã xóa" else "❤️ Đã thêm", Toast.LENGTH_SHORT).show()
                onDismiss()
            }

            // 🔖 Watchlist
            ContextMenuItem(
                if (isInWatchlist) "🗑️ Xóa khỏi Xem sau" else "🔖 Thêm vào Xem sau",
                if (isInWatchlist) "Đang trong danh sách" else "Lưu xem sau"
            ) {
                WatchlistManager.toggle(movie.slug, movie.name, movie.thumbUrl)
                Toast.makeText(context, if (isInWatchlist) "🗑️ Đã xóa" else "🔖 Đã lưu", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }
}

@Composable
fun ContextMenuItem(label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = C.TextPrimary, fontFamily = InterFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = C.TextSecondary, fontFamily = InterFamily, fontSize = 12.sp)
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
