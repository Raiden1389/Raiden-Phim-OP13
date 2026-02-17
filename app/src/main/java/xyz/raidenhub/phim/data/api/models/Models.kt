package xyz.raidenhub.phim.data.api.models

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Immutable
data class Movie(
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("thumb_url") val thumbUrl: String = "",
    @SerializedName("poster_url") val posterUrl: String = "",
    @SerializedName("year") val year: Int = 0,
    @SerializedName("quality") val quality: String = "",
    @SerializedName("lang") val lang: String = "",
    @SerializedName("episode_current") val episodeCurrent: String = "",
    @SerializedName("country") val country: List<Category> = emptyList(),
    @SerializedName("category") val category: List<Category> = emptyList(),
    @Transient val source: String = "ophim"
)

@Immutable
data class Category(
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = ""
)

@Immutable
data class MovieDetail(
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("origin_name") val originName: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("thumb_url") val thumbUrl: String = "",
    @SerializedName("poster_url") val posterUrl: String = "",
    @SerializedName("year") val year: Int = 0,
    @SerializedName("quality") val quality: String = "",
    @SerializedName("lang") val lang: String = "",
    @SerializedName("episode_current") val episodeCurrent: String = "",
    @SerializedName("episode_total") val episodeTotal: String = "",
    @SerializedName("time") val time: String = "",
    @SerializedName("country") val country: List<Category> = emptyList(),
    @SerializedName("category") val category: List<Category> = emptyList(),
    @SerializedName("director") val director: List<String> = emptyList(),
    @SerializedName("actor") val actor: List<String> = emptyList(),
    @SerializedName("type") val type: String = "",
    @SerializedName("episodes") val episodes: List<EpisodeServer> = emptyList()
)

@Immutable
data class EpisodeServer(
    @SerializedName("server_name") val serverName: String = "",
    @SerializedName("server_data") val serverData: List<Episode> = emptyList()
)

@Immutable
data class Episode(
    @SerializedName("name") val name: String = "",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("link_embed") val linkEmbed: String = "",
    @SerializedName("link_m3u8") val linkM3u8: String = ""
)

// ═══ API Response wrappers ═══
data class OPhimListResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("data") val data: OPhimListData? = null
)

data class OPhimListData(
    @SerializedName("items") val items: List<Movie> = emptyList(),
    @SerializedName("params") val params: OPhimPagination? = null
)

data class OPhimPagination(
    @SerializedName("pagination") val pagination: PaginationInfo? = null
)

data class PaginationInfo(
    @SerializedName("totalItems") val totalItems: Int = 0,
    @SerializedName("totalItemsPerPage") val totalItemsPerPage: Int = 24,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("currentPage") val currentPage: Int = 0
)

data class OPhimDetailResponse(
    @SerializedName("status") val status: String = "",
    @SerializedName("data") val data: OPhimDetailData? = null
)

data class OPhimDetailData(
    @SerializedName("item") val item: MovieDetail? = null,
    @SerializedName("episodes") val episodes: List<EpisodeServer> = emptyList()
)
