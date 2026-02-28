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
                Regex("""(?<!\d)[\.\-\s](\d{1,2})[\.\-\s]"""),   // .01. or - 01 - (max 2 digits, skip audio 5.1)
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

    /** Filename stripped of quality/codec/size markers for base-name comparison */
    val strippedName: String
        get() = name
            .substringBeforeLast(".")
            .replace(Regex("""\(.*?\)"""), "")
            .replace(Regex("""(?:2160|1080|720|480|360)[pi]?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:4K|UHD|Blu-?[Rr]ay|BDRip|REMUX|WEB-?DL|WEBRip|HDRip|DVDRip|HDTV)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:x264|x265|H[\s.]?264|H[\s.]?265|HEVC|AVC|10bit|8bit|HDR10?\+?|HDR|DV|DoVi)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:AAC|DTS\d*\.?\d*|DD[P+]?\s*\d*\.?\d*|AC3|EAC3|TrueHD|Atmos|FLAC)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\d+(?:[.,]\d+)?\s*(?:GB|MB|TB)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""(?:HQ|\d+fps|PROPER|REPACK)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[.*?]"""), "")
            .replace(Regex("""-\w+$"""), "")
            .replace(Regex("""[.\-_\s]+"""), " ")
            .trim()

    companion object {
        /** Extract normalized episode ID, null if no episode marker found */
        private fun extractEpisodeId(name: String): String? {
            Regex("""[Ss](\d+)[Ee](\d+)""").find(name)?.let {
                return "s${it.groupValues[1].trimStart('0').ifEmpty { "0" }}e${it.groupValues[2].trimStart('0').ifEmpty { "0" }}"
            }
            Regex("""[Ee][Pp]\.?\s*(\d+)""").find(name)?.let {
                return "ep${it.groupValues[1].trimStart('0').ifEmpty { "0" }}"
            }
            Regex("""[Tt](?:ap|ập)\s*(\d+)""").find(name)?.let {
                return "t${it.groupValues[1].trimStart('0').ifEmpty { "0" }}"
            }
            Regex("""Episode\s*(\d+)""", RegexOption.IGNORE_CASE).find(name)?.let {
                return "ep${it.groupValues[1].trimStart('0').ifEmpty { "0" }}"
            }
            return null
        }

        /**
         * Detect if files are quality variants (same content, different quality).
         * Handles: phim lẻ multi-quality AND series episode multi-quality (e.g. S02E01 x3).
         */
        fun areQualityVariants(files: List<FshareFile>): Boolean {
            if (files.size <= 1) return false
            val epIds = files.map { extractEpisodeId(it.name) }
            return when {
                epIds.all { it != null } -> epIds.toSet().size == 1
                epIds.all { it == null } -> files.map { it.strippedName.lowercase() }.toSet().size == 1
                else -> false
            }
        }

        /**
         * Build Episode list from Fshare files.
         * - Series (has S01E01): clean title → "Can This Love Be Translated S01E01 ViE 1080p"
         * - Movie (no episode marker): quality label → "Sub Viet · 1080p · BluRay Remux · Atmos"
         */
        fun toEpisodes(files: List<FshareFile>): List<Episode> {
            // Detect: if ANY file has episode marker → series mode
            val isSeries = files.any { EPISODE_PATTERN.containsMatchIn(it.name) }
            return files.map { file ->
                val rawName = file.name.substringBeforeLast(".")
                val displayName = if (isSeries) cleanSeriesName(rawName) else cleanMovieName(rawName)
                Episode(
                    name = displayName,
                    slug = file.furl,       // Fshare page URL — used by fetchFshareEp to resolve CDN
                    linkEmbed = file.furl,
                    linkM3u8 = ""           // Empty! CDN URL resolved on-demand by PrefetchEffect
                )
            }
        }

        // ═══ PATTERNS ═══

        private val EPISODE_PATTERN = Regex("""[.\s]S\d{1,2}E\d{1,3}[.\s]|[.\s]E[Pp]?\d{1,3}[.\s]""", RegexOption.IGNORE_CASE)

        // Source/codec tags to strip for series
        private val SOURCE_TAGS = listOf(
            "NF", "AMZN", "DSNP", "HMAX", "ATVP", "PCOK", "STAN", "iT", "CR",
            "WEB-DL", "WEBRip", "WEB\\.DL", "WEB", "BluRay", "BDRip", "BRRip",
            "HDRip", "HDTV", "DVDRip", "Remux",
            "DDP5", "DD5", "DDP", "AAC", "AC3", "EAC3", "Atmos", "TrueHD",
            "H\\.264", "H\\.265", "x264", "x265", "HEVC", "AVC", "10bit"
        )

        private val STRIP_REGEX by lazy {
            val tags = SOURCE_TAGS.joinToString("|")
            Regex("""[.\s](?:$tags)[.\s\-].*$""", RegexOption.IGNORE_CASE)
        }

        // ═══ SERIES MODE: clean title ═══
        fun cleanSeriesName(raw: String): String {
            return raw
                .replace(STRIP_REGEX, "")
                .replace('.', ' ')
                .replace('_', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        // ═══ MOVIE MODE: quality-focused label ═══

        private val SUB_PATTERN = Regex("""\(([^)]+)\)\s*""")
        private val RESOLUTION_PATTERN = Regex("""\b(2160p|1080p|720p|480p|4K|UHD)\b""", RegexOption.IGNORE_CASE)
        private val SOURCE_PATTERN = Regex("""\b(BluRay|BDRip|BRRip|WEB-DL|WEBRip|WEB\.DL|HDTV|DVDRip|HDRip|AMZN|NF|DSNP|HMAX|ATVP|Remux|Hybrid)\b""", RegexOption.IGNORE_CASE)
        private val AUDIO_PATTERN = Regex("""\b(Atmos|TrueHD[.\s]?\d[.\s]?\d?|DTS-HD[.\s]?MA\d[.\s]?\d?|DDP?\d[.\s]?\d|DDP|AAC|AC3|EAC3|DoVi|HDR10\+?|HDR)\b""", RegexOption.IGNORE_CASE)

        fun cleanMovieName(raw: String): String {
            // Extract sub type: "(Sub Viet)" or "(Thuyet Minh - Sub Viet)"
            val subMatch = SUB_PATTERN.find(raw)
            val subType = subMatch?.groupValues?.get(1)?.let { sub ->
                when {
                    "thuyet minh" in sub.lowercase() || "thuyết minh" in sub.lowercase() -> "TM"
                    "sub" in sub.lowercase() -> "Sub"
                    else -> sub.take(10)
                }
            } ?: ""

            // Extract resolution
            val resolutions = RESOLUTION_PATTERN.findAll(raw).map { it.value }.toList()
            val resolution = resolutions.firstOrNull() ?: ""
            val hasUHD = raw.contains("UHD", ignoreCase = true) && resolution == "2160p"

            // Extract source
            val sources = SOURCE_PATTERN.findAll(raw).map { it.value }.toList()
            val source = sources.joinToString(" ").take(20)

            // Extract audio
            val audios = AUDIO_PATTERN.findAll(raw).map { it.value }.toList()
            val audio = audios.take(2).joinToString(" ").take(20)

            // Build label: "Sub · 1080p · BluRay Remux · Atmos TrueHD"
            return listOfNotNull(
                subType.takeIf { it.isNotBlank() },
                (if (hasUHD) "$resolution UHD" else resolution).takeIf { it.isNotBlank() },
                source.takeIf { it.isNotBlank() },
                audio.takeIf { it.isNotBlank() }
            ).joinToString(" · ").ifBlank {
                // fallback: just clean the raw name
                raw.replace('.', ' ').replace('_', ' ').trim()
            }
        }
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
