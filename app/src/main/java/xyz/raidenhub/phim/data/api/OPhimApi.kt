package xyz.raidenhub.phim.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.raidenhub.phim.data.api.models.OPhimDetailResponse
import xyz.raidenhub.phim.data.api.models.OPhimListResponse

interface OPhimApi {
    @GET("danh-sach/phim-moi-cap-nhat")
    suspend fun getNewMovies(@Query("page") page: Int = 1): OPhimListResponse

    @GET("danh-sach/phim-bo")
    suspend fun getSeries(@Query("page") page: Int = 1): OPhimListResponse

    @GET("danh-sach/phim-le")
    suspend fun getSingleMovies(@Query("page") page: Int = 1): OPhimListResponse

    @GET("danh-sach/hoat-hinh")
    suspend fun getAnime(@Query("page") page: Int = 1): OPhimListResponse

    @GET("danh-sach/tv-shows")
    suspend fun getTvShows(@Query("page") page: Int = 1): OPhimListResponse

    @GET("quoc-gia/han-quoc")
    suspend fun getKorean(@Query("page") page: Int = 1): OPhimListResponse

    @GET("quoc-gia/trung-quoc")
    suspend fun getChinese(@Query("page") page: Int = 1): OPhimListResponse

    @GET("quoc-gia/au-my")
    suspend fun getWestern(@Query("page") page: Int = 1): OPhimListResponse

    @GET("phim/{slug}")
    suspend fun getMovieDetail(@Path("slug") slug: String): OPhimDetailResponse

    @GET("tim-kiem")
    suspend fun search(@Query("keyword") keyword: String, @Query("page") page: Int = 1): OPhimListResponse
}
