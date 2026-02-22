package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.SubDLResponse
import xyz.raidenhub.phim.data.api.models.OpenSubtitlesResponse

// ═══ SubDL API ═══
interface SubDLApi {
    @GET("api/v1/subtitles")
    suspend fun search(
        @Query("api_key") apiKey: String,
        @Query("film_name") filmName: String,
        @Query("languages") languages: String = "vi,en",
        @Query("subs_per_page") subsPerPage: Int = 60,
        @Query("type") type: String? = null,
        @Query("year") year: String? = null,
        @Query("season_number") seasonNumber: Int? = null,
        @Query("episode_number") episodeNumber: Int? = null,
        @Query("page") page: Int? = null
    ): SubDLResponse
}

// ═══ OpenSubtitles API ═══
interface OpenSubtitlesApi {
    @GET("api/v1/subtitles")
    suspend fun search(
        @Header("Api-Key") apiKey: String,
        @Header("User-Agent") userAgent: String = "RaidenPhim v1.7",
        @Query("query") query: String,
        @Query("languages") languages: String = "vi,en",
        @Query("order_by") orderBy: String = "download_count",
        @Query("order_direction") orderDirection: String = "desc",
        @Query("type") type: String? = null,
        @Query("year") year: Int? = null,
        @Query("season_number") seasonNumber: Int? = null,
        @Query("episode_number") episodeNumber: Int? = null
    ): OpenSubtitlesResponse

    @GET("api/v1/download")
    suspend fun getDownloadLink(
        @Header("Api-Key") apiKey: String,
        @Header("User-Agent") userAgent: String = "RaidenPhim v1.7",
        @Query("file_id") fileId: Int
    ): OpenSubtitlesDownloadResponse
}

data class OpenSubtitlesDownloadResponse(
    val link: String = "",
    val file_name: String = "",
    val remaining: Int = 0
)

// ═══ SubSource API ═══
interface SubSourceApi {
    @GET("api/v1/movies/search")
    suspend fun searchMovies(
        @Header("X-API-Key") apiKey: String,
        @Query("q") query: String,
        @Query("searchType") searchType: String = "text"
    ): SubSourceSearchResponse

    @GET("api/v1/subtitles")
    suspend fun getSubtitles(
        @Header("X-API-Key") apiKey: String,
        @Query("movie_id") movieId: Int,
        @Query("language") language: String? = null
    ): SubSourceSubtitleResponse
}

data class SubSourceSearchResponse(
    val data: List<SubSourceMovie> = emptyList()
)

data class SubSourceMovie(
    val id: Int = 0,
    val title: String = "",
    val year: Int = 0,
    val type: String = ""  // "movie" or "tv"
)

data class SubSourceSubtitleResponse(
    val data: List<SubSourceSubtitle> = emptyList()
)

data class SubSourceSubtitle(
    val id: Int = 0,
    val language: String = "",
    val release_name: String = "",
    val download_count: Int = 0,
    val hearing_impaired: Boolean = false
)
