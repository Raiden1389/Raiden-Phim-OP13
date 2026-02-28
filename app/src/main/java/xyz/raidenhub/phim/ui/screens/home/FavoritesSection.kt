package xyz.raidenhub.phim.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.local.FavoriteItem
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils

// ═══ Favorites Section ═══

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesSection(
    favorites: List<FavoriteItem>,
    onMovieClick: (String) -> Unit,
) {
    if (favorites.isEmpty()) return

    val context = LocalContext.current

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("❤️ Yêu thích", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${favorites.size} phim", color = C.TextSecondary, fontSize = 13.sp)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(favorites, key = { "${it.slug}_${it.source}" }) { fav ->
                Box(modifier = Modifier.width(130.dp)) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = { onMovieClick(fav.slug) },
                                onLongClick = {
                                    FavoriteManager.toggle(fav.slug, fav.name)
                                    Toast.makeText(context, "💔 Đã xoá ${fav.name}", Toast.LENGTH_SHORT).show()
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
                                model = ImageUtils.cardImage(fav.thumbUrl, fav.source),
                                contentDescription = fav.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove button
                            IconButton(
                                onClick = {
                                    FavoriteManager.toggle(fav.slug, fav.name)
                                    Toast.makeText(context, "💔 Đã xoá ${fav.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(0.6f), RoundedCornerShape(50))
                            ) {
                                Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            // Source badge
                            if (fav.source == "fshare") {
                                Text(
                                    "F",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            } else if (fav.source == "superstream") {
                                Text(
                                    "SS",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(Color(0xFF2196F3), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            fav.name,
                            color = C.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
