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
            .connectionPool(ConnectionPool(3, 2, TimeUnit.MINUTES))
            .apply {
                cacheDir?.let { dir ->
                    cache(Cache(File(dir, "http_cache"), 50L * 1024 * 1024))
                }
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

    val anime47: Anime47Api by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.ANIME47_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Anime47Api::class.java)
    }

}

