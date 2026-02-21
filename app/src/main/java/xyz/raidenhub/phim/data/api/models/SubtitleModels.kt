package xyz.raidenhub.phim.data.api.models

import com.google.gson.annotations.SerializedName

// ═══ Unified Subtitle Result ═══
data class SubtitleResult(
    val url: String,                    // Direct URL to .srt/.vtt file
    val language: String,               // "vi", "en", etc.
    val languageLabel: String,          // "Vietnamese", "English"
    val source: String,                 // "consumet", "subdl", "opensubtitles"
    val fileName: String = "",
    val downloadCount: Int = 0,
    val isHearingImpaired: Boolean = false
) {
    val flag: String get() = when (language) {
        "vi" -> "🇻🇳"
        "en" -> "🇬🇧"
        "ja" -> "🇯🇵" 
        "ko" -> "🇰🇷"
        "zh" -> "🇨🇳"
        "fr" -> "🇫🇷"
        "es" -> "🇪🇸"
        else -> "🌐"
    }
    
    val displayName: String get() = "$flag $languageLabel ($source)"
}

// ═══ Consumet Subtitle (from stream API response) ═══
data class ConsumetSubtitle(
    val url: String = "",
    val lang: String = ""
)

// ═══ SubDL Response ═══
data class SubDLResponse(
    @SerializedName("status") val status: Boolean = false,
    @SerializedName("results") val results: List<SubDLResult> = emptyList(),
    @SerializedName("subtitles") val subtitles: List<SubDLSubtitle> = emptyList()
)

data class SubDLResult(
    @SerializedName("sd_id") val sdId: Int = 0,
    @SerializedName("type") val type: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("imdb_id") val imdbId: String = "",
    @SerializedName("tmdb_id") val tmdbId: Int = 0,
    @SerializedName("first_air_date") val firstAirDate: String = "",
    @SerializedName("year") val year: Int = 0
)

data class SubDLSubtitle(
    @SerializedName("release_name") val releaseName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("lang") val lang: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("url") val url: String = "",           // Relative: /subtitle/xxx.zip
    @SerializedName("subtitlePage") val subtitlePage: String = "",
    @SerializedName("season") val season: Int? = null,
    @SerializedName("episode") val episode: Int? = null,
    @SerializedName("language") val language: String = "",  // Full name: "Vietnamese"
    @SerializedName("hi") val hearingImpaired: Boolean = false
)

// ═══ OpenSubtitles Response ═══
data class OpenSubtitlesResponse(
    @SerializedName("total_pages") val totalPages: Int = 0,
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("data") val data: List<OpenSubtitlesItem> = emptyList()
)

data class OpenSubtitlesItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("attributes") val attributes: OpenSubtitlesAttributes = OpenSubtitlesAttributes()
)

data class OpenSubtitlesAttributes(
    @SerializedName("subtitle_id") val subtitleId: String = "",
    @SerializedName("language") val language: String = "",
    @SerializedName("download_count") val downloadCount: Int = 0,
    @SerializedName("hearing_impaired") val hearingImpaired: Boolean = false,
    @SerializedName("release") val release: String = "",
    @SerializedName("files") val files: List<OpenSubtitlesFile> = emptyList()
)

data class OpenSubtitlesFile(
    @SerializedName("file_id") val fileId: Int = 0,
    @SerializedName("file_name") val fileName: String = ""
)
