package xyz.raidenhub.phim.util

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// VP-3: Mỗi thể loại có gradient màu riêng
// Dùng cho: GenreHubScreen cards, genre chips, CategoryScreen header
object GenreColors {

    data class GenrePalette(
        val start: Color,
        val end: Color,
        val label: Color = Color.White
    ) {
        val gradient: Brush get() = Brush.linearGradient(listOf(start, end))
        val verticalGradient: Brush get() = Brush.verticalGradient(listOf(start, end))
    }

    fun palette(genreSlug: String): GenrePalette = when {
        "hanh-dong"  in genreSlug -> GenrePalette(Color(0xFFFF6B35), Color(0xFFCC0000))
        "vo-thuat"   in genreSlug -> GenrePalette(Color(0xFFFF6B35), Color(0xFFCC0000))
        "chien-tranh" in genreSlug -> GenrePalette(Color(0xFF78350F), Color(0xFF292524))
        "kinh-di"    in genreSlug -> GenrePalette(Color(0xFF6B21A8), Color(0xFF1A0030))
        "bi-an"      in genreSlug -> GenrePalette(Color(0xFF4C1D95), Color(0xFF0F0C29))
        "tinh-cam"   in genreSlug -> GenrePalette(Color(0xFFEC4899), Color(0xFF9D174D))
        "hoc-duong"  in genreSlug -> GenrePalette(Color(0xFFF472B6), Color(0xFF7C3AED))
        "hai-huoc"   in genreSlug -> GenrePalette(Color(0xFFFBBF24), Color(0xFFF97316))
        "gia-dinh"   in genreSlug -> GenrePalette(Color(0xFF34D399), Color(0xFF059669))
        "co-trang"   in genreSlug -> GenrePalette(Color(0xFFD97706), Color(0xFF78350F))
        "than-thoai" in genreSlug -> GenrePalette(Color(0xFFD97706), Color(0xFF7C2D12))
        "khoa-hoc"   in genreSlug -> GenrePalette(Color(0xFF06B6D4), Color(0xFF1E40AF))
        "vien-tuong" in genreSlug -> GenrePalette(Color(0xFF8B5CF6), Color(0xFF1E1B4B))
        "phieu-luu"  in genreSlug -> GenrePalette(Color(0xFF10B981), Color(0xFF0F4C75))
        "tam-ly"     in genreSlug -> GenrePalette(Color(0xFF3B82F6), Color(0xFF1E3A5F))
        "hinh-su"    in genreSlug -> GenrePalette(Color(0xFF64748B), Color(0xFF1E293B))
        "chinh-kich" in genreSlug -> GenrePalette(Color(0xFF6366F1), Color(0xFF312E81))
        "am-nhac"    in genreSlug -> GenrePalette(Color(0xFFA78BFA), Color(0xFF7C3AED))
        "the-thao"   in genreSlug -> GenrePalette(Color(0xFF22C55E), Color(0xFF15803D))
        "tai-lieu"   in genreSlug -> GenrePalette(Color(0xFF94A3B8), Color(0xFF334155))
        else                       -> GenrePalette(Color(0xFF6366F1), Color(0xFF312E81)) // default indigo
    }

    // Convenience: lấy màu start để dùng cho chip background (semi-transparent)
    fun chipColor(genreSlug: String): Color = palette(genreSlug).start.copy(alpha = 0.2f)
    fun chipBorderColor(genreSlug: String): Color = palette(genreSlug).start.copy(alpha = 0.6f)
}
