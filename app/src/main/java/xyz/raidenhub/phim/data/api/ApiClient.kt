package xyz.raidenhub.phim.data.api

import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import xyz.raidenhub.phim.BuildConfig
import xyz.raidenhub.phim.util.Constants
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    var cacheDir: File? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .apply {
                cacheDir?.let { dir ->
                    // 300MB cho API JSON — response ~50-200KB → chứa được 1500-6000 responses
                    cache(Cache(File(dir, "http_cache"), 300L * 1024 * 1024))
                }
            }
            // Force-cache 30 phút: override server no-cache headers cho OPhim/KKPhim API
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=1800") // 30 phút
                    .removeHeader("Pragma")
                    .build()
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

    val ophim: OPhimApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.OPHIM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OPhimApi::class.java)
    }

    val kkphim: KKPhimApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.KKPHIM_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KKPhimApi::class.java)
    }

    // ═══ SuperStream APIs ═══

    val tmdb: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }

    val showbox: ShowBoxApi by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.SHOWBOX_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ShowBoxApi::class.java)
    }

    // FebBox OkHttp with cookie — direct API calls, no WebView needed
    val febboxClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Cookie", "ui=${Constants.FEBBOX_COOKIE}")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.febbox.com/")
                    .build()
                chain.proceed(req)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()
    }

}

