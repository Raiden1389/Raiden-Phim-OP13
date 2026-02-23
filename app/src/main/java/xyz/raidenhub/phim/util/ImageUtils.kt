package xyz.raidenhub.phim.util

object ImageUtils {
    // Phone APK: direct CDN URL, không cần wsrv.nl proxy
    // wsrv.nl hợp lý cho TV APK (2GB RAM, GPU yếu) nhưng phone CDN VN đã đủ nhanh
    // Direct URL = 1 URL duy nhất → Coil cache reuse giữa card / shimmer / detail

    fun cardImage(path: String, source: String = "ophim"): String = originalUrl(source, path)

    fun detailImage(path: String, source: String = "ophim"): String = originalUrl(source, path)

    fun heroImage(path: String, source: String = "ophim"): String = originalUrl(source, path)

    fun originalUrl(source: String, path: String): String {
        if (path.startsWith("http")) return path
        val cdn = if (source == "kkphim") Constants.KKPHIM_IMG_CDN else Constants.OPHIM_IMG_CDN
        return "$cdn$path"
    }
}
