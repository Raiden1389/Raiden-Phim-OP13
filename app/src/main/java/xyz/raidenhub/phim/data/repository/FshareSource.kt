package xyz.raidenhub.phim.data.repository

import xyz.raidenhub.phim.data.api.models.CineMovie

/**
 * FshareSource — contract chung cho các nguồn Fshare (thuviencine, thuvienhd, ...).
 *
 * Mỗi nguồn implement interface này với parser HTML riêng,
 * nhưng output cùng CineMovie/CineDetailResult → UI không cần biết nguồn nào.
 *
 * Routing: FshareAggregator dùng [domain] để route đúng parser theo URL.
 */
interface FshareSource {
    /** Source identifier for logging/debug: "cine", "tvhd" */
    val sourceId: String

    /** Domain used for URL-based routing: "thuviencine.com", "thuvienhd.top" */
    val domain: String

    /** Default URL cho phim lẻ listing (Home screen row) */
    val homeMoviesUrl: String

    /** Default URL cho phim bộ listing (Home screen row) */
    val homeSeriesUrl: String

    // ═══ Movie Listings ═══

    /** Fetch movies from a category URL with pagination */
    suspend fun getMovies(categoryUrl: String, page: Int = 1): List<CineMovie>

    /** Search movies by keyword */
    suspend fun search(query: String): List<CineMovie>

    // ═══ Movie Detail ═══

    /** Scrape detail page for movie info + Fshare link(s) */
    suspend fun getDetailWithFshare(detailUrl: String): ThuVienCineRepository.CineDetailResult
}
