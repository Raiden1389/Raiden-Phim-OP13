package xyz.raidenhub.phim.data.api.models

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════
// Fshare API Models
// ═══════════════════════════════════════════════

// --- Login ---
data class FshareLoginRequest(
    val app_key: String = "dMnqMMZMUnN5YpvKENaEhdQQ5jxDqddt",
    val user_email: String,
    val password: String
)

data class FshareLoginResponse(
    val code: Int,
    val msg: String,
    val token: String?,
    val session_id: String?
)

// --- User Info ---
data class FshareUserInfo(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val account_type: String = "",      // "Vip"
    val expire_vip: String = "",        // Unix timestamp
    @SerializedName("webspace")
    val totalSpace: Long = 0,
    @SerializedName("webspace_used")
    val usedSpace: Long = 0
) {
    val isVip: Boolean get() = account_type.equals("Vip", ignoreCase = true)

    /** Expire date as readable string */
    val expireDate: String
        get() = try {
            val ts = expire_vip.toLong() * 1000
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                .format(java.util.Date(ts))
        } catch (_: Exception) {
            "N/A"
        }
}

// --- Folder List ---
data class FshareFolderRequest(
    val token: String,
    val url: String,
    val dirOnly: Int = 0,
    val pageIndex: Int = 0,
    val limit: Int = 100
)

data class FshareFile(
    val name: String = "",
    val furl: String = "",       // https://www.fshare.vn/file/XXX
    val size: Long = 0,
    val type: Int = 1            // 0=folder, 1=file
) {
    val isFolder: Boolean get() = type == 0

    /** Human-readable file size */
    val sizeFormatted: String
        get() {
            val gb = size / (1024.0 * 1024.0 * 1024.0)
            val mb = size / (1024.0 * 1024.0)
            return when {
                gb >= 1.0 -> String.format("%.1f GB", gb)
                mb >= 1.0 -> String.format("%.0f MB", mb)
                else -> String.format("%.0f KB", size / 1024.0)
            }
        }

    /** Extract quality hint from filename */
    val quality: String
        get() = when {
            name.contains("2160p", ignoreCase = true) || name.contains("4K", ignoreCase = true) -> "4K"
            name.contains("1080p", ignoreCase = true) -> "1080p"
            name.contains("720p", ignoreCase = true) -> "720p"
            name.contains("Bluray", ignoreCase = true) -> "Bluray"
            else -> "HD"
        }

    /** Check if this is a playable video file */
    val isVideo: Boolean
        get() = name.endsWith(".mkv", ignoreCase = true) ||
                name.endsWith(".mp4", ignoreCase = true) ||
                name.endsWith(".avi", ignoreCase = true) ||
                name.endsWith(".ts", ignoreCase = true)

    /**
     * Extract episode label from filename.
     * Patterns: E01, Ep01, Ep.01, Episode.01, Tap 01, - 01 -, .01., S01E01
     * Fallback: cleaned filename without extension
     */
    val episodeLabel: String
        get() {
            // Try common patterns
            val patterns = listOf(
                Regex("""[Ss]\d+[Ee](\d+)"""),           // S01E01
                Regex("""[Ee][Pp]?\.?(\d+)"""),           // E01, Ep01, Ep.01
                Regex("""[Tt](?:ap|ập)\s*(\d+)"""),       // Tap 01, Tập 01
                Regex("""[\.\-\s](\d{1,3})[\.\-\s]"""),   // .01. or - 01 -
            )
            for (pattern in patterns) {
                val match = pattern.find(name)
                if (match != null) {
                    val num = match.groupValues[1].trimStart('0').ifEmpty { "0" }
                    return "Tập $num"
                }
            }
            // Fallback: clean filename
            return name
                .substringBeforeLast(".")        // remove extension
                .replace(Regex("""\d{3,4}p"""), "") // remove quality
                .replace(Regex("""[\.\-_]"""), " ") // dots/dashes to spaces
                .trim()
                .take(30)
        }
}

// --- Download / Resolve ---
data class FshareDownloadRequest(
    val token: String,
    val url: String,
    val password: String = "",
    val zipflag: Int = 0
)

data class FshareDownloadResponse(
    val location: String?,        // Direct CDN URL!
    val code: Int? = null,
    val msg: String? = null
) {
    val isSuccess: Boolean get() = location != null && location.isNotBlank()
}
