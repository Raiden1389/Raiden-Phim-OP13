package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.Anime47DataWrapper
import xyz.raidenhub.phim.data.api.models.Anime47DetailWrapper
import xyz.raidenhub.phim.data.api.models.Anime47EpisodeStreamWrapper
import xyz.raidenhub.phim.data.api.models.Anime47Genre
import xyz.raidenhub.phim.data.api.models.Anime47Item
import xyz.raidenhub.phim.data.api.models.Anime47SearchResponse

interface Anime47Api {

    // ═══ Home Page ═══
    @GET("home-page/hero-section")
    suspend fun getHero(@Query("lang") lang: String = "vi"): List<Anime47Item>

    @GET("home-page/trending-carousel")
    suspend fun getTrending(@Query("lang") lang: String = "vi"): List<Anime47Item>

    @GET("home-page/latest-episode-posts")
    suspend fun getLatestEpisodes(@Query("lang") lang: String = "vi"): Anime47DataWrapper

    @GET("home-page/upcoming")
    suspend fun getUpcoming(@Query("lang") lang: String = "vi"): Anime47DataWrapper

    // ═══ Search ═══
    @GET("search/live/")
    suspend fun search(
        @Query("keyword") keyword: String,
        @Query("lang") lang: String = "vi"
    ): Anime47SearchResponse

    // ═══ Genres ═══
    @GET("genres/")
    suspend fun getGenres(@Query("lang") lang: String = "vi"): List<Anime47Genre>

    // ═══ Detail ═══
    @GET("anime/info/{id}")
    suspend fun getAnimeDetail(
        @Path("id") id: Int,
        @Query("lang") lang: String = "vi"
    ): Anime47DetailWrapper

    // ═══ Browse by Genre (slug) ═══
    // Trả về danh sách anime theo thể loại chính xác theo slug
    @GET("anime/list")
    suspend fun getAnimeByGenre(
        @Query("genre") genreSlug: String,
        @Query("page") page: Int = 1,
        @Query("lang") lang: String = "vi"
    ): Anime47DataWrapper

    // ═══ Browse by Category tab ═══
    // category: "genres" | "demographics" | "themes" | "explicit"
    @GET("anime/list")
    suspend fun getAnimeByCategory(
        @Query("category") category: String,
        @Query("page") page: Int = 1,
        @Query("lang") lang: String = "vi"
    ): Anime47DataWrapper

    // ═══ Episode Stream — Hướng B ═══
    // Fetch M3U8 / stream sources cho một tập cụ thể
    @GET("episode/info/{id}")
    suspend fun getEpisodeStream(
        @Path("id") id: Int,
        @Query("lang") lang: String = "vi"
    ): Anime47EpisodeStreamWrapper
}
