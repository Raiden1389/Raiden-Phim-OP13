package xyz.raidenhub.phim.data.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import xyz.raidenhub.phim.data.api.models.FshareDownloadRequest
import xyz.raidenhub.phim.data.api.models.FshareDownloadResponse
import xyz.raidenhub.phim.data.api.models.FshareFile
import xyz.raidenhub.phim.data.api.models.FshareFolderRequest
import xyz.raidenhub.phim.data.api.models.FshareLoginRequest
import xyz.raidenhub.phim.data.api.models.FshareLoginResponse
import xyz.raidenhub.phim.data.api.models.FshareUserInfo

/**
 * Fshare API — Premium HD source
 * Base URL: https://api.fshare.vn/api/
 *
 * Keys reverse-engineered from VNMEDIA Kodi addon (kedon.py)
 */
interface FshareApi {

    companion object {
        const val BASE_URL = "https://api.fshare.vn/api/"

        // Keys injected from local.properties via BuildConfig
        val APP_KEY: String get() = xyz.raidenhub.phim.BuildConfig.FSHARE_APP_KEY
        val USER_AGENT: String get() = xyz.raidenhub.phim.BuildConfig.FSHARE_USER_AGENT

        // Share param (VietMediaF adds this to download URLs)
        const val SHARE_PARAM = "share=8805984"
    }

    @POST("user/login")
    suspend fun login(@Body request: FshareLoginRequest): FshareLoginResponse

    @GET("user/get")
    suspend fun getUserInfo(@Header("Cookie") session: String): FshareUserInfo

    @POST("fileops/getFolderList")
    suspend fun getFolderList(
        @Header("Cookie") session: String,
        @Body request: FshareFolderRequest
    ): ResponseBody

    @POST("session/download")
    suspend fun getDownloadLink(
        @Header("Cookie") session: String,
        @Body request: FshareDownloadRequest
    ): FshareDownloadResponse
}
