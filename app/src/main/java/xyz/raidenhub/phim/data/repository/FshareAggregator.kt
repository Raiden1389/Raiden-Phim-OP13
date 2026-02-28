package xyz.raidenhub.phim.data.repository

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.raidenhub.phim.data.api.models.CineMovie

/**
 * FshareAggregator — orchestrates multiple FshareSource implementations.
 *
 * Home: parallel fetch from all sources → merge + dedup by normalized title.
 * Detail/Category: route to correct source by URL domain.
 * Search: aggregate from all sources.
 *
 * Fallback: if one source fails, the other still returns results.
 */
class FshareAggregator {

    private val sources: List<FshareSource> = listOf(
        ThuVienCineRepository(),
        ThuVienHdSource()
    )

    // ═══ Home Screen (parallel merge) ═══

    /** Fetch phim lẻ from ALL sources, merge + dedup */
    suspend fun getHomeMovies(): List<CineMovie> = coroutineScope {
        val results = sources.map { source ->
            async {
                try {
                    source.getMovies(source.homeMoviesUrl).also {
                        Log.d(TAG, "[${source.sourceId}] movies: ${it.size}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[${source.sourceId}] movies failed: ${e.message}")
                    emptyList()
                }
            }
        }
        mergeAndDedup(results.map { it.await() })
    }

    /** Fetch phim bộ from ALL sources, merge + dedup */
    suspend fun getHomeSeries(): List<CineMovie> = coroutineScope {
        val results = sources.map { source ->
            async {
                try {
                    source.getMovies(source.homeSeriesUrl).also {
                        Log.d(TAG, "[${source.sourceId}] series: ${it.size}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[${source.sourceId}] series failed: ${e.message}")
                    emptyList()
                }
            }
        }
        mergeAndDedup(results.map { it.await() })
    }

    // ═══ Category / Pagination (route by domain) ═══

    /** Fetch movies from a specific category URL — routes to correct source */
    suspend fun getMovies(categoryUrl: String, page: Int = 1): List<CineMovie> {
        val source = findSource(categoryUrl)
        return source.getMovies(categoryUrl, page)
    }

    // ═══ Detail (route by domain) ═══

    /** Get detail — routes to correct parser by URL domain */
    suspend fun getDetailWithFshare(detailUrl: String): ThuVienCineRepository.CineDetailResult {
        val source = findSource(detailUrl)
        return source.getDetailWithFshare(detailUrl)
    }

    // ═══ Search (aggregate) ═══

    /** Search across all sources */
    suspend fun search(query: String): List<CineMovie> = coroutineScope {
        val results = sources.map { source ->
            async {
                try { source.search(query) }
                catch (_: Exception) { emptyList() }
            }
        }
        mergeAndDedup(results.map { it.await() })
    }

    // ═══ Internal ═══

    /** Find the source that matches the URL domain, fallback to first */
    private fun findSource(url: String): FshareSource {
        return sources.find { it.domain in url } ?: sources.first()
    }

    /**
     * Merge movies from multiple sources:
     * 1. Interleave (source1[0], source2[0], source1[1], source2[1], ...)
     * 2. Dedup by normalized title
     */
    private fun mergeAndDedup(sourceLists: List<List<CineMovie>>): List<CineMovie> {
        // Interleave: take items round-robin from each source
        val maxLen = sourceLists.maxOfOrNull { it.size } ?: 0
        val interleaved = mutableListOf<CineMovie>()
        for (i in 0 until maxLen) {
            for (list in sourceLists) {
                list.getOrNull(i)?.let { interleaved.add(it) }
            }
        }
        // Dedup: keep first occurrence by normalized title
        return interleaved.distinctBy { normalizeTitle(it.title) }
    }

    /** Normalize title for dedup: lowercase, trim, collapse whitespace */
    private fun normalizeTitle(title: String): String =
        title.lowercase().trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[–—\\-]"), "")
            .replace(Regex("\\(\\d{4}\\)"), "")  // strip year parens

    companion object {
        private const val TAG = "FshareHub"
    }
}
