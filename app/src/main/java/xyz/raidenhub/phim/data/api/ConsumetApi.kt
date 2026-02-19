package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.ConsumetDetail
import xyz.raidenhub.phim.data.api.models.ConsumetItem
import xyz.raidenhub.phim.data.api.models.ConsumetSearchResponse
import xyz.raidenhub.phim.data.api.models.ConsumetStreamResponse

interface ConsumetApi {

    // ═══ FlixHQ — Trending ═══
    @GET("movies/flixhq/trending")
    suspend fun getTrending(@Query("page") page: Int = 1): ConsumetSearchResponse

    @GET("movies/flixhq/recent-movies")
    suspend fun getRecentMovies(@Query("page") page: Int = 1): List<ConsumetItem>

    @GET("movies/flixhq/recent-shows")
    suspend fun getRecentShows(@Query("page") page: Int = 1): List<ConsumetItem>

    // ═══ Search ═══
    @GET("movies/flixhq/{query}")
    suspend fun search(
        @Path("query") query: String,
        @Query("page") page: Int = 1
    ): ConsumetSearchResponse

    // ═══ Detail ═══
    @GET("movies/flixhq/info")
    suspend fun getInfo(@Query("id") id: String): ConsumetDetail

    // ═══ Streaming ═══
    @GET("movies/flixhq/watch")
    suspend fun getStreamLinks(
        @Query("episodeId") episodeId: String,
        @Query("mediaId") mediaId: String,
        @Query("server") server: String = "vidcloud"
    ): ConsumetStreamResponse
}
