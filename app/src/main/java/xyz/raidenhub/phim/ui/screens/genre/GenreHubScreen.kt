package xyz.raidenhub.phim.ui.screens.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.ui.theme.C

// C-2: Genre Hub — danh sách thể loại → CategoryScreen
private data class GenreItem(val emoji: String, val name: String, val slug: String)

private val GENRE_ITEMS = listOf(
    GenreItem("🔥", "Hành Động", "hanh-dong"),
    GenreItem("💕", "Tình Cảm", "tinh-cam"),
    GenreItem("😂", "Hài Hước", "hai-huoc"),
    GenreItem("🏯", "Cổ Trang", "co-trang"),
    GenreItem("🧠", "Tâm Lý", "tam-ly"),
    GenreItem("🔎", "Hình Sự", "hinh-su"),
    GenreItem("👻", "Kinh Dị", "kinh-di"),
    GenreItem("🚀", "Viễn Tưởng", "vien-tuong"),
    GenreItem("🗺️", "Phiêu Lưu", "phieu-luu"),
    GenreItem("🥋", "Võ Thuật", "vo-thuat"),
    GenreItem("🎓", "Học Đường", "hoc-duong"),
    GenreItem("🕵️", "Bí Ẩn", "bi-an"),
    GenreItem("🎭", "Chính Kịch", "chinh-kich"),
    GenreItem("👨‍👩‍👧", "Gia Đình", "gia-dinh"),
    GenreItem("⚔️", "Chiến Tranh", "chien-tranh"),
    GenreItem("🎵", "Âm Nhạc", "am-nhac"),
    GenreItem("🐉", "Thần Thoại", "than-thoai"),
    GenreItem("🔬", "Khoa Học", "khoa-hoc"),
    GenreItem("⚽", "Thể Thao", "the-thao"),
    GenreItem("📹", "Tài Liệu", "tai-lieu"),
)

@Composable
fun GenreHubScreen(
    onBack: () -> Unit,
    onGenreClick: (slug: String, name: String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(C.Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
            }
            Text("🎭 Thể Loại", color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "Chọn thể loại để khám phá phim",
            color = C.TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 80.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(GENRE_ITEMS, key = { it.slug }) { genre ->
                GenreCard(genre = genre, onClick = { onGenreClick(genre.slug, genre.name) })
            }
        }
    }
}

@Composable
private fun GenreCard(genre: GenreItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.Surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(genre.emoji, fontSize = 28.sp)
        Text(
            genre.name,
            color = C.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
