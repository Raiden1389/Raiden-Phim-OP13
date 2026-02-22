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

    suspend fun getMovieDetail(slug: String): Result<DetailResult> {
        return safeCall {
            // Try OPhim first
            try {
                val response = api.getMovieDetail(slug)
                val movie = response.data?.item ?: throw AppError.ParseError("Not found on OPhim")
                val episodes = movie.episodes.ifEmpty { response.data?.episodes.orEmpty() }
                return@safeCall DetailResult(movie, episodes, "ophim")
            } catch (e: AppError.NetworkError) {
                throw e  // Propagate network error — retry thay vì fallback
            } catch (_: Exception) { }
            // Fallback to KKPhim (chỉ khi OPhim 404/parse fail)
            val response = kkApi.getMovieDetail(slug)
            val movie = response.data?.item ?: throw AppError.ParseError("Not found on both sources")
            val episodes = movie.episodes.ifEmpty { response.data?.episodes.orEmpty() }
            DetailResult(movie, episodes, "kkphim")
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

    suspend fun getHomeData(): Result<HomeData> = safeCall {
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
            HomeData(newD.await(), serD.await(), movD.await(), aniD.await(), korD.await(), tvD.await())
        }
    }

    // ═══ Helpers ═══
    private fun List<Movie>.filterCountry(): List<Movie> {
        val allowed = Constants.ALLOWED_COUNTRIES ?: return this
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
