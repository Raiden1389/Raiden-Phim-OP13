package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.Anime47DataWrapper
import xyz.raidenhub.phim.data.api.models.Anime47Detail
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
    ): Anime47Detail
}
