package xyz.raidenhub.phim.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.models.Anime47Genre
import xyz.raidenhub.phim.data.api.models.Anime47Item

object AnimeRepository {
    private val api = ApiClient.anime47

    data class AnimeHomeData(
        val hero: List<Anime47Item>,
        val trending: List<Anime47Item>,
        val latest: List<Anime47Item>,
        val upcoming: List<Anime47Item>,
        val genres: List<Anime47Genre>
    )

    suspend fun getHomeData(): Result<AnimeHomeData> = safeCall {
        coroutineScope {
            val heroD = async { api.getHero() }
            val trendD = async { api.getTrending() }
            val latestD = async { api.getLatestEpisodes().data }
            val upD = async { api.getUpcoming().data }
            val genreD = async {
                api.getGenres().filter {
                    it.category in listOf("genres", "demographics") && it.postsCount > 0
                }.sortedByDescending { it.postsCount }
            }
            AnimeHomeData(heroD.await(), trendD.await(), latestD.await(), upD.await(), genreD.await())
        }
    }

    suspend fun search(keyword: String) = safeCall {
        api.search(keyword).results
    }

    /**
     * Browse anime by genre slug — proper filter, accurate results.
     * Fallback: nếu endpoint /anime/list?genre= fail → thử search bằng tên genre.
     */
    suspend fun getAnimeByGenre(genreSlug: String, genreName: String, page: Int = 1) = safeCall {
        try {
            val result = api.getAnimeByGenre(genreSlug, page).data
            if (result.isNotEmpty()) result
            else api.search(genreName).results  // fallback
        } catch (_: Exception) {
            api.search(genreName).results        // fallback nếu endpoint chưa tồn tại
        }
    }

    suspend fun getAnimeDetail(id: Int) = safeCall {
        api.getAnimeDetail(id).data
    }

    /** Hướng B: Fetch M3U8 stream URL cho 1 tập Anime47 theo episode ID */
    suspend fun getEpisodeStream(episodeId: Int) = safeCall {
        api.getEpisodeStream(episodeId).data
    }

    suspend fun getGenres() = safeCall {
        api.getGenres().filter { it.postsCount > 0 }
    }

    /**
     * Fetch popular Donghua (Chinese animation) by searching known keywords.
     * Anime47 doesn't have a country filter so we search for popular donghua series.
     */
    suspend fun getDonghua() = safeCall {
        val keywords = listOf("già thiên", "đấu phá", "tiên nghịch", "vũ động càn khôn", "đấu la", "phàm nhân", "thôn phệ", "nguyên tôn", "vạn giới")
        val results = mutableListOf<Anime47Item>()
        val seen = mutableSetOf<Int>()
        for (kw in keywords) {
            if (results.size >= 15) break
            try {
                api.search(kw).results.forEach { item ->
                    if (item.id !in seen && results.size < 15) {
                        seen.add(item.id)
                        results.add(item)
                    }
                }
            } catch (_: Exception) {}
        }
        results
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            try { Result.success(block()) } catch (re: Exception) { Result.failure(re) }
        }
    }
}
