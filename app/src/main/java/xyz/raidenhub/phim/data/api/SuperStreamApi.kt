package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.*

// ═══════════════════════════════════════════════════
//  SuperStream APIs — TMDB + ShowBox (OkHttp)
//  FebBox uses WebView, NOT Retrofit
// ═══════════════════════════════════════════════════

// ═══ TMDB API ═══
interface TmdbApi {

    // Search movies + TV shows
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): TmdbSearchResponse

    // Trending movies (weekly)
    @GET("trending/movie/week")
    suspend fun trendingMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbTrendingResponse

    // Trending TV (weekly)
    @GET("trending/tv/week")
    suspend fun trendingTv(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): TmdbTrendingResponse

    // Movie detail
    @GET("movie/{id}")
    suspend fun movieDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetail

    // TV detail
    @GET("tv/{id}")
    suspend fun tvDetail(
        @Path("id") id: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbTvDetail

    // TV season detail (episodes list)
    @GET("tv/{id}/season/{season}")
    suspend fun tvSeasonDetail(
        @Path("id") id: Int,
        @Path("season") season: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbSeasonDetail
}

// ═══ ShowBox API ═══
interface ShowBoxApi {

    // Get FebBox share key
    @GET("index/share_link")
    suspend fun getShareLink(
        @Query("id") id: Int,
        @Query("type") type: Int   // 1 = movie, 2 = tv
    ): ShowBoxShareResponse
}
