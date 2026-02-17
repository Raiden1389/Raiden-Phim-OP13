package xyz.raidenhub.phim.util

object Constants {
    // ═══ API Base URLs ═══
    const val OPHIM_BASE_URL  = "https://ophim1.com/v1/api/"
    const val KKPHIM_BASE_URL = "https://phimapi.com/"
    const val ANIME47_BASE_URL = "https://anime47.love/api/"

    // ═══ Image CDNs ═══
    const val OPHIM_IMG_CDN  = "https://img.ophim.live/uploads/movies/"
    const val KKPHIM_IMG_CDN = "https://phimimg.com/"

    // ═══ Country Filter (phone: no filter by default) ═══
    val ALLOWED_COUNTRIES: Set<String>? = null // null = show all

    // ═══ Network ═══
    const val NETWORK_TIMEOUT_SECONDS = 10L

    // ═══ UI — Phone specific ═══
    const val CARDS_PER_ROW_PORTRAIT = 3
    const val CARDS_PER_ROW_LANDSCAPE = 5
    const val HOME_ROW_MAX_ITEMS = 12

    // ═══ Player ═══
    val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    const val SKIP_INTRO_MS = 85000L
    const val SKIP_INTRO_SHOW_UNTIL_MS = 120000L
    const val AUTO_NEXT_BEFORE_END_MS = 90_000L  // default fallback

    // ═══ Country/Type-aware auto-next timing ═══
    fun getAutoNextMs(country: String, type: String): Long {
        return when {
            // 🇰🇷 K-Show (variety show): outro ~60s, không intro
            country == "han-quoc" && type == "tvshows" -> 60_000L
            // 🇰🇷 K-Drama: outro + preview ~3 phút
            country == "han-quoc" -> 180_000L
            // 🇨🇳 Hoạt hình Tàu (donghua): ED + preview ~2-3 phút
            country == "trung-quoc" && type == "hoathinh" -> 180_000L
            // 🇨🇳 Phim Tàu: outro + preview ~1p30-2p
            country == "trung-quoc" -> 90_000L
            // 🇯🇵 Anime: ED + preview ~2-3 phút
            country == "nhat-ban" -> 180_000L
            // 🌍 Mặc định: safe 90s
            else -> 90_000L
        }
    }

    // ═══ Continue Watching ═══
    const val MAX_CONTINUE_ITEMS = 10
}
