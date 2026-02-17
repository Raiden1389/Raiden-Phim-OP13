package xyz.raidenhub.phim.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.models.ConsumetDetail
import xyz.raidenhub.phim.data.api.models.ConsumetItem
import xyz.raidenhub.phim.data.api.models.ConsumetStreamResponse

object ConsumetRepository {
    private val api = ApiClient.consumet

    data class EnglishHomeData(
        val trending: List<ConsumetItem>,
        val recentMovies: List<ConsumetItem>,
        val recentShows: List<ConsumetItem>
    )

    suspend fun getHomeData(): Result<EnglishHomeData> = safeCall {
        coroutineScope {
            val trendD = async { api.getTrending().results }
            val moviesD = async { api.getRecentMovies().results }
            val showsD = async { api.getRecentShows().results }
            EnglishHomeData(trendD.await(), moviesD.await(), showsD.await())
        }
    }

    suspend fun search(query: String, page: Int = 1) = safeCall {
        api.search(query, page).results
    }

    suspend fun getDetail(id: String): Result<ConsumetDetail> = safeCall {
        api.getInfo(id)
    }

    suspend fun getStreamLinks(episodeId: String, mediaId: String): Result<ConsumetStreamResponse> = safeCall {
        api.getStreamLinks(episodeId, mediaId)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            try { Result.success(block()) } catch (re: Exception) { Result.failure(re) }
        }
    }
}
