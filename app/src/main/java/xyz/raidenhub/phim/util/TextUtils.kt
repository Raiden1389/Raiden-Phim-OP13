package xyz.raidenhub.phim.util

object TextUtils {
    fun dedupKey(name: String, year: Int): String {
        return "${name.lowercase().trim().replace(Regex("\\s+"), " ")}_$year"
    }

    fun shortLang(lang: String): String = when {
        lang.contains("Vietsub") && lang.contains("Thuyết Minh") -> "VS+TM"
        lang.contains("Vietsub") -> "VS"
        lang.contains("Thuyết Minh") -> "TM"
        lang.contains("Lồng Tiếng") -> "LT"
        else -> lang
    }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}
