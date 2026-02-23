package xyz.raidenhub.phim.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.util.AppError
import xyz.raidenhub.phim.util.Constants
import xyz.raidenhub.phim.util.toAppError

object MovieRepository {
    private val api = ApiClient.ophim
    private val kkApi = ApiClient.kkphim

    suspend fun getNewMovies(page: Int = 1) = safeCall {
        api.getNewMovies(page).data?.items.orEmpty().filterCountry()
    }

    suspend fun getSeries(page: Int = 1) = safeCall {
        api.getSeries(page).data?.items.orEmpty().filterCountry().filterTrailer().sortNewest()
    }

    suspend fun getSingleMovies(page: Int = 1) = safeCall {
        api.getSingleMovies(page).data?.items.orEmpty().filterCountry().filterTrailer().sortNewest()
    }

    suspend fun getAnime(page: Int = 1) = safeCall {
        api.getAnime(page).data?.items.orEmpty().filterTrailer().sortNewest()
    }

    suspend fun getKorean(page: Int = 1) = safeCall {
        api.getKorean(page).data?.items.orEmpty().filterTrailer().sortNewest()
    }

    suspend fun search(keyword: String, page: Int = 1) = safeCall {
        api.search(keyword, page).data?.items.orEmpty().filterCountry()
    }

    data class DetailResult(
        val movie: MovieDetail,
        val episodes: List<EpisodeServer>,
        val source: String = "ophim"
    )

    // ─── In-memory detail cache (TTL 5 phút) ───────────────────────────────
    // Back rồi vào lại cùng Detail → instant, không re-fetch network
    private data class CacheEntry(val result: DetailResult, val timestamp: Long)
    private val detailCache = LinkedHashMap<String, CacheEntry>(100, 0.75f, true)
    private const val CACHE_TTL_MS = 30 * 60 * 1000L  // 30 phút
    private const val CACHE_MAX = 100

    private fun getCached(slug: String): DetailResult? {
        val entry = detailCache[slug] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            detailCache.remove(slug)
            return null
        }
        return entry.result
    }

    private fun putCache(slug: String, result: DetailResult) {
        if (detailCache.size >= CACHE_MAX) {
            detailCache.entries.firstOrNull()?.key?.let { detailCache.remove(it) }
        }
        detailCache[slug] = CacheEntry(result, System.currentTimeMillis())
    }
    // ────────────────────────────────────────────────────────────────────────

    suspend fun getMovieDetail(slug: String): Result<DetailResult> {
        // Cache hit → trả ngay, không fetch network
        getCached(slug)?.let { return Result.success(it) }

        return safeCall {
            // Try OPhim first
            try {
                val response = api.getMovieDetail(slug)
                val movie = response.data?.item ?: throw AppError.ParseError("Not found on OPhim")
                val episodes = movie.episodes.ifEmpty { response.data?.episodes.orEmpty() }
                val result = DetailResult(movie, episodes, "ophim")
                putCache(slug, result)
                return@safeCall result
            } catch (e: AppError.NetworkError) {
                throw e
            } catch (_: Exception) { }
            // Fallback to KKPhim
            val response = kkApi.getMovieDetail(slug)
            val movie = response.data?.item ?: throw AppError.ParseError("Not found on both sources")
            val episodes = movie.episodes.ifEmpty { response.data?.episodes.orEmpty() }
            val result = DetailResult(movie, episodes, "kkphim")
            putCache(slug, result)
            result
        }
    }

    // Home page: parallel fetch all categories
    data class HomeData(
        val newMovies: List<Movie>,
        val series: List<Movie>,
        val singleMovies: List<Movie>,
        val anime: List<Movie>,
        val korean: List<Movie>,
        val tvShows: List<Movie> = emptyList()
    )

    // ─── In-memory Home cache (TTL 5 phút) ────────────────────────────────
    private var homeCacheData: HomeData? = null
    private var homeCacheTime: Long = 0L
    private const val HOME_CACHE_TTL_MS = 60 * 60 * 1000L  // 1 giờ

    fun getCachedHomeData(): HomeData? {
        val data = homeCacheData ?: return null
        return if (System.currentTimeMillis() - homeCacheTime < HOME_CACHE_TTL_MS) data else null
    }
    // ────────────────────────────────────────────────────────────────────────

    suspend fun getHomeData(): Result<HomeData> {
        getCachedHomeData()?.let { return Result.success(it) }
        return safeCall {
        coroutineScope {
            val newD  = async { api.getNewMovies(1).data?.items.orEmpty().filterCountry() }
            val serD  = async { api.getSeries(1).data?.items.orEmpty().filterCountry().filterTrailer().sortNewest() }
            val movD  = async { api.getSingleMovies(1).data?.items.orEmpty().filterCountry().filterTrailer().sortNewest() }
            val aniD  = async { api.getAnime(1).data?.items.orEmpty().filterTrailer().sortNewest() }
            val korD  = async { api.getKorean(1).data?.items.orEmpty().filterTrailer().sortNewest() }
            val tvD   = async {
                try {
                    val p1 = async { kkApi.getTvShows(1).data?.items.orEmpty() }
                    val p2 = async { kkApi.getTvShows(2).data?.items.orEmpty() }
                    (p1.await() + p2.await())
                        .distinctBy { it.slug }
                        .tagSource("kkphim")
                        .filterTrailer()
                        .sortNewest()
                } catch (_: Exception) { emptyList() }
            }
            val result = HomeData(newD.await(), serD.await(), movD.await(), aniD.await(), korD.await(), tvD.await())
            homeCacheData = result
            homeCacheTime = System.currentTimeMillis()
            result
        }
        }
    }

    // ═══ Helpers ═══
    private fun List<Movie>.filterCountry(): List<Movie> {
        // Scope cố định: Hàn / Trung / Mỹ (Constants.ALLOWED_COUNTRIES)
        val allowed = Constants.ALLOWED_COUNTRIES
        return filter { m -> m.country.isEmpty() || m.country.any { it.slug in allowed } }
    }

    private fun List<Movie>.filterTrailer() = filter {
        !it.episodeCurrent.equals("Trailer", ignoreCase = true)
    }

    private fun List<Movie>.sortNewest() = sortedByDescending { it.year }

    private fun List<Movie>.tagSource(src: String) = map { it.copy(source = src) }

    /**
     * Smart safeCall:
     * - Phân loại exception → AppError (NetworkError / HttpError / ParseError)
     * - Chỉ retry nếu NetworkError (mất mạng / timeout) — delay 1s
     * - KHÔNG retry HttpError (404, 500) hay ParseError → fail nhanh
     */
    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Throwable) {
            val appError = e.toAppError()
            if (appError.isRetryable) {
                // Single retry sau 1 giây — chỉ cho NetworkError
                delay(1000L)
                try {
                    Result.success(block())
                } catch (re: Throwable) {
                    Result.failure(re.toAppError())
                }
            } else {
                // HttpError / ParseError / Unknown → fail ngay
                Result.failure(appError)
            }
        }
    }
}
