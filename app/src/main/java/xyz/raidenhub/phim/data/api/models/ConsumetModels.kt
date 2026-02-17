package xyz.raidenhub.phim.data.api.models

import com.google.gson.annotations.SerializedName

// ═══ Consumet FlixHQ Models ═══

data class ConsumetSearchResponse(
    @SerializedName("currentPage") val currentPage: Int = 1,
    @SerializedName("hasNextPage") val hasNextPage: Boolean = false,
    @SerializedName("results") val results: List<ConsumetItem> = emptyList()
)

data class ConsumetItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("type") val type: String = "",           // "Movie" | "TV Series"
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("duration") val duration: String = "",
    @SerializedName("url") val url: String = ""
)

data class ConsumetDetail(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String = "",
    @SerializedName("cover") val cover: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("releaseDate") val releaseDate: String = "",
    @SerializedName("genres") val genres: List<String> = emptyList(),
    @SerializedName("duration") val duration: String = "",
    @SerializedName("production") val production: String = "",
    @SerializedName("casts") val casts: List<String> = emptyList(),
    @SerializedName("episodes") val episodes: List<ConsumetEpisode> = emptyList()
)

data class ConsumetEpisode(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("number") val number: Int = 0,
    @SerializedName("season") val season: Int = 0,
    @SerializedName("url") val url: String = ""
)

data class ConsumetStreamResponse(
    @SerializedName("sources") val sources: List<ConsumetSource> = emptyList(),
    @SerializedName("subtitles") val subtitles: List<ConsumetSubtitle> = emptyList()
)

data class ConsumetSource(
    @SerializedName("url") val url: String = "",
    @SerializedName("quality") val quality: String = "",
    @SerializedName("isM3U8") val isM3U8: Boolean = true
)

data class ConsumetSubtitle(
    @SerializedName("url") val url: String = "",
    @SerializedName("lang") val lang: String = ""
)
