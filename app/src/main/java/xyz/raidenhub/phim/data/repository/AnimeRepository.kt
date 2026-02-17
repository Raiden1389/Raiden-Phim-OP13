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

    suspend fun getGenres() = safeCall {
        api.getGenres().filter { it.postsCount > 0 }
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            try { Result.success(block()) } catch (re: Exception) { Result.failure(re) }
        }
    }
}
