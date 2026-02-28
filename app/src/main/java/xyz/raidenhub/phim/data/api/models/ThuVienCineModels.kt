package xyz.raidenhub.phim.data.api.models

/**
 * ThuVienCine models — scraped from thuviencine.com HTML
 *
 * HTML structure confirmed from VietMediaF tvcine.py source code.
 */

/** Movie item from listing/search pages */
data class CineMovie(
    val title: String,
    val slug: String,              // URL path: /movie-slug-fshare/
    val thumbnailUrl: String,      // Poster (TMDB, upgraded resolution)
    val quality: String,           // "4K", "FHD", "HD", "Vietsub"
    val detailUrl: String,         // Full URL to detail page
    val year: String = "",
    val backdropUrl: String = "",  // Fanart/backdrop (TMDB, upgraded)
    val description: String = "",  // Plot synopsis
    val rating: Float = 0f         // IMDB rating
) {
    /** Convert to Movie for unified search results display */
    fun toMovie(): Movie = Movie(
        name = title,
        slug = detailUrl,          // Use full URL as slug — DetailScreen routes by URL
        thumbUrl = thumbnailUrl,
        posterUrl = thumbnailUrl,
        year = year.toIntOrNull() ?: 0,
        quality = quality,
        lang = "Vietsub",
        source = "fshare"
    )
}

/** Fshare link extracted from movie detail page */
data class CineFshareLink(
    val folderUrl: String,         // https://fshare.vn/folder/XXX or /file/XXX
    val downloadId: String = ""    // ThuVienCine internal download ID
) {
    /** True if this is a folder (contains multiple files/qualities) */
    val isFolder: Boolean get() = "folder" in folderUrl
}

/** Category definition */
data class CineCategory(
    val name: String,
    val slug: String,              // e.g. "movies", "tv-series", "country/south-korea"
    val url: String                // Full URL
) {
    companion object {
        private const val BASE = "https://thuviencine.com"

        /** Predefined categories — Hàn, Trung, Mỹ focus */
        val ALL = listOf(
            // ═══ Main Categories ═══
            CineCategory("Phim lẻ", "movies", "$BASE/movies/"),
            CineCategory("Phim bộ", "tv-series", "$BASE/tv-series/"),
            CineCategory("Trending", "top", "$BASE/top/"),

            // ═══ Country Filter (Hàn, Trung, Mỹ) ═══
            CineCategory("🇰🇷 Hàn Quốc", "south-korea", "$BASE/country/south-korea/"),
            CineCategory("🇨🇳 Trung Quốc", "china", "$BASE/country/china/"),
            CineCategory("🇺🇸 Phim Mỹ", "usa", "$BASE/country/usa/"),

            // ═══ Genres ═══
            CineCategory("Hành động", "action", "$BASE/phim-hanh-dong/"),
            CineCategory("Chính kịch", "drama", "$BASE/phim-chinh-kich/"),
            CineCategory("Hài", "comedy", "$BASE/phim-hai/"),
            CineCategory("Kinh dị", "horror", "$BASE/phim-kinh-di/"),
            CineCategory("Khoa học", "scifi", "$BASE/phim-khoa-hoc-vien-tuong/"),
            CineCategory("Lãng mạn", "romance", "$BASE/phim-lang-man/"),
            CineCategory("Hoạt hình", "animation", "$BASE/phim-hoat-hinh/"),
            CineCategory("Thiếu nhi", "kids", "$BASE/kids/"),
        )

        /**
         * Map category slug → ThuVienCine URL
         * Only fshare-specific categories. OPhim categories (korean, chinese, western)
         * do NOT have Fshare equivalents — they show OPhim only.
         */
        val OPHIM_TO_CINE = mapOf(
            "fshare-movies"   to "$BASE/movies/",
            "fshare-series"   to "$BASE/tv-series/",
            "fshare-trending" to "$BASE/top/"
        )
    }
}
