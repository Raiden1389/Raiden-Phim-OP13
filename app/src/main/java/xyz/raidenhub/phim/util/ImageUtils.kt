package xyz.raidenhub.phim.util

object ImageUtils {
    private const val PROXY_BASE = "https://wsrv.nl/?url="
    private const val CARD_PARAMS = "&w=200&h=300&fit=cover&output=webp&maxage=7d"
    private const val DETAIL_PARAMS = "&w=400&h=600&fit=cover&output=webp&maxage=7d&dpr=1&sharp=1"
    private const val HERO_PARAMS = "&w=800&h=450&fit=cover&output=webp&maxage=7d"

    fun cardImage(path: String, source: String = "ophim"): String {
        return "$PROXY_BASE${originalUrl(source, path)}$CARD_PARAMS"
    }

    fun detailImage(path: String, source: String = "ophim"): String {
        return "$PROXY_BASE${originalUrl(source, path)}$DETAIL_PARAMS"
    }

    fun heroImage(path: String, source: String = "ophim"): String {
        return "$PROXY_BASE${originalUrl(source, path)}$HERO_PARAMS"
    }

    fun originalUrl(source: String, path: String): String {
        if (path.startsWith("http")) return path
        val cdn = if (source == "kkphim") Constants.KKPHIM_IMG_CDN else Constants.OPHIM_IMG_CDN
        return "$cdn$path"
    }
}
