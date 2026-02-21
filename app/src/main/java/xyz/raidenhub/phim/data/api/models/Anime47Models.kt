package xyz.raidenhub.phim.data.api.models

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Anime47Item(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("poster") val poster: String = "",
    @SerializedName("posterUrl") val posterUrl: String = "",
    @SerializedName("backdropImage") val backdropImage: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("quality") val quality: String = "",
    @SerializedName("rating") val rating: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("episodes") val episodes: String? = null,
    @SerializedName("current_episode") val currentEpisode: String? = null,
    @SerializedName("season") val season: String = "",
    @SerializedName("year") val year: String = "",
    @SerializedName("rank") val rank: Int? = null,
    @SerializedName("legacy_id") val legacyId: Int? = null
) {
    val displayImage: String get() = poster.ifBlank { posterUrl.ifBlank { image } }
    val episodeLabel: String get() {
        val cur = currentEpisode ?: return ""
        val total = episodes ?: return "Tập $cur"
        return "Tập $cur/$total"
    }
}

// Wrapper for endpoints that return {"data": [...]}
data class Anime47DataWrapper(
    @SerializedName("data") val data: List<Anime47Item> = emptyList()
)

// Search response
data class Anime47SearchResponse(
    @SerializedName("query") val query: String = "",
    @SerializedName("results") val results: List<Anime47Item> = emptyList(),
    @SerializedName("count") val count: Int = 0,
    @SerializedName("total_pages") val totalPages: Int = 0,
    @SerializedName("has_more") val hasMore: Boolean = false
)

// Genre
@Immutable
data class Anime47Genre(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("posts_count") val postsCount: Int = 0
)

// Detail response wrapper — API returns {"data": {...}}
data class Anime47DetailWrapper(
    @SerializedName("data") val data: Anime47Detail = Anime47Detail()
)

// Detail response
@Immutable
data class Anime47Detail(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("poster") val poster: String = "",
    @SerializedName("backdropImage") val backdropImage: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("quality") val quality: String = "",
    @SerializedName("rating") val rating: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("status") val status: String = "",
    @SerializedName("views") val views: Int = 0,
    @SerializedName("latestEpisodes") val latestEpisodes: List<Anime47Episode> = emptyList(),
    @SerializedName("episodes") val episodes: Any? = null,  // can be map or list
    @SerializedName("animeGroups") val animeGroups: Any? = null,
    @SerializedName("characters") val characters: Any? = null,
    @SerializedName("score") val score: Int = 0,
    @SerializedName("year") val year: Int = 0
)

@Immutable
data class Anime47Episode(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("episodeNumber") val episodeNumber: Int = 0,
    @SerializedName("slug") val slug: String = "",
    @SerializedName("number") val number: Int = 0,
    @SerializedName("link") val link: String = ""
)

// ═══ Episode Stream — Hướng B ═══
// API: GET /episode/info/{id}?lang=vi
// Trả về link M3U8 + danh sách sources
data class Anime47EpisodeStreamWrapper(
    @SerializedName("data") val data: Anime47EpisodeStream = Anime47EpisodeStream()
)

@Immutable
data class Anime47EpisodeStream(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("link") val link: String = "",         // embed / fallback URL
    @SerializedName("streamUrl") val streamUrl: String = "", // Direct M3U8 (nếu có)
    @SerializedName("sources") val sources: List<Anime47Source> = emptyList(),
    @SerializedName("animeId") val animeId: Int = 0,
    @SerializedName("number") val number: Int = 0
) {
    /** Lấy M3U8 tốt nhất: ưu tiên streamUrl → HLS source → MP4 source */
    val bestStreamUrl: String get() {
        if (streamUrl.isNotBlank()) return streamUrl
        val hls = sources.firstOrNull { it.type == "hls" || it.url.contains(".m3u8") }
        if (hls != null) return hls.url
        return sources.firstOrNull { it.url.isNotBlank() }?.url ?: link
    }
}

@Immutable
data class Anime47Source(
    @SerializedName("url") val url: String = "",
    @SerializedName("type") val type: String = "",    // "hls", "mp4", "dash"
    @SerializedName("label") val label: String = ""   // "1080p", "720p", ...
)

