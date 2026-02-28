package xyz.raidenhub.phim.ui.screens.home

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import xyz.raidenhub.phim.data.api.models.CineMovie
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.repository.MovieRepository

// ═══ Section Mapping & Filtering ═══

/** Genre filter only — country filter cưỡng bức ở tầng API (Constants.ALLOWED_COUNTRIES) */
fun List<Movie>.applySettingsFilter(settingsGenres: Set<String>): List<Movie> {
    if (settingsGenres.isEmpty()) return this
    return filter { m -> m.category.any { it.slug in settingsGenres } }
}

/** Map sectionId → (label, filteredList, categorySlug, categoryName) */
data class SectionInfo(
    val label: String,
    val movies: List<Movie>,
    val categorySlug: String,
    val categoryName: String,
)

fun buildSectionMap(data: MovieRepository.HomeData, settingsGenres: Set<String>): Map<String, SectionInfo> {
    return mapOf(
        "new"     to SectionInfo("🔥 Phim Mới",   data.newMovies.applySettingsFilter(settingsGenres),   "phim-moi-cap-nhat", "Phim Mới"),
        "korean"  to SectionInfo("🇰🇷 K-Drama",   data.korean.applySettingsFilter(settingsGenres),      "han-quoc",          "K-Drama"),
        "series"  to SectionInfo("📺 Phim Bộ",    data.series.applySettingsFilter(settingsGenres),      "phim-bo",           "Phim Bộ"),
        "single"  to SectionInfo("🎬 Phim Lẻ",    data.singleMovies.applySettingsFilter(settingsGenres),"phim-le",           "Phim Lẻ"),
        "anime"   to SectionInfo("🎌 Hoạt Hình",  data.anime.applySettingsFilter(settingsGenres),       "hoat-hinh",         "Hoạt Hình"),
        "tvshows" to SectionInfo("📺 TV Shows",   data.tvShows.applySettingsFilter(settingsGenres),     "tv-shows",          "TV Shows"),
    )
}

// ═══ Dynamic Sections Renderer ═══

/**
 * Render category rows (OPhim + Fshare) theo SectionOrderManager order.
 * Gọi từ LazyColumn scope trong HomeScreen.
 */
fun LazyListScope.renderDynamicSections(
    sectionOrder: List<String>,
    sectionMap: Map<String, SectionInfo>,
    fshareMovies: List<CineMovie>,
    fshareSeries: List<CineMovie>,
    onMovieClick: (String) -> Unit,
    onContinue: (slug: String, server: Int, episode: Int, positionMs: Long, source: String, fshareEpSlug: String) -> Unit,
    onCategoryClick: (String, String) -> Unit,
    onFshareClick: (String) -> Unit,
    onFshareSeeMore: (url: String, title: String) -> Unit,
    haptic: HapticFeedback,
) {
    sectionOrder.forEach { sectionId ->
        // OPhim rows
        val info = sectionMap[sectionId]
        if (info != null) {
            if (info.movies.isNotEmpty()) {
                item(key = info.label) {
                    MovieRowSection(info.label, info.movies, onMovieClick, onContinue, haptic) {
                        onCategoryClick(info.categorySlug, info.categoryName)
                    }
                }
            }
            return@forEach
        }

        // Fshare rows
        when (sectionId) {
            "fshare_movies" -> if (fshareMovies.isNotEmpty()) {
                item(key = "fshare_movies") {
                    FshareRow(
                        title = "💎 Fshare Phim Lẻ",
                        items = fshareMovies,
                        onItemClick = { movie ->
                            val enriched = "fshare-folder:${movie.detailUrl}|||${movie.title}|||${movie.thumbnailUrl}"
                            onFshareClick(enriched)
                        },
                        onSeeMore = { onFshareSeeMore("https://thuviencine.com/movies/", "Fshare Phim Lẻ") }
                    )
                }
            }
            "fshare_series" -> if (fshareSeries.isNotEmpty()) {
                item(key = "fshare_series") {
                    FshareRow(
                        title = "💎 Fshare Phim Bộ",
                        items = fshareSeries,
                        onItemClick = { movie ->
                            val enriched = "fshare-folder:${movie.detailUrl}|||${movie.title}|||${movie.thumbnailUrl}"
                            onFshareClick(enriched)
                        },
                        onSeeMore = { onFshareSeeMore("https://thuviencine.com/tv-series/", "Fshare Phim Bộ") }
                    )
                }
            }
        }
    }
}
