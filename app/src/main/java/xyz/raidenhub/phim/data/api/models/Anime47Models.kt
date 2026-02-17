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
    @SerializedName("legacy_id") val legacyId: Int? = null
) {
    val displayImage: String get() = poster.ifBlank { image }
    val episodeLabel: String get() {
        val cur = currentEpisode ?: return ""
        val total = episodes ?: return "Tập $cur"
        return "Tập $cur/$total"
    }
}

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
    @SerializedName("latestEpisodes") val latestEpisodes: List<Anime47Episode> = emptyList(),
    @SerializedName("episodes") val episodes: Any? = null  // can be map or list
)

@Immutable
data class Anime47Episode(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("number") val number: Int = 0
)
