package xyz.raidenhub.phim.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import xyz.raidenhub.phim.data.api.models.CineFshareLink
import xyz.raidenhub.phim.data.api.models.CineMovie
import java.text.Normalizer

/**
 * ThuVienHdSource — scrapes thuvienhd.top for movie listings with Fshare links.
 *
 * HTML structure (confirmed via browser inspection):
 *   article.item.movies
 *     .poster img          → poster
 *     .poster a            → detail link
 *     .data h3 a           → title + detail link
 *     .data span           → year
 *     .poster .quality_slider → quality badge (HD, 4K)
 *
 * Detail page:
 *   h1                     → "Tên Việt (year)"
 *   .data h2               → English title
 *   .poster img            → poster
 *   #info                  → description
 *   a.face-button[href*=fshare.vn/file/] → direct Fshare file links (multi-quality)
 *
 * Key difference from ThuVienCine:
 *   - Fshare links are DIRECT file URLs (not folder), multiple per movie
 *   - Uses /genre/phim-le and /genre/series URLs
 *   - Article-based card layout instead of div[id^=post-]
 */
class ThuVienHdSource : FshareSource {

    override val sourceId = "tvhd"
    override val domain = "thuvienhd.top"
    override val homeMoviesUrl = "$BASE/genre/phim-le"
    override val homeSeriesUrl = "$BASE/genre/series"

    // ═══ Movie Listings ═══

    override suspend fun getMovies(categoryUrl: String, page: Int): List<CineMovie> =
        withContext(Dispatchers.IO) {
            try {
                val url = if (page > 1) "${categoryUrl.trimEnd('/')}/page/$page/" else categoryUrl
                val doc = fetchDocument(url)
                parseMovieList(doc)
            } catch (e: Exception) {
                Log.e(TAG, "getMovies failed: $categoryUrl page=$page", e)
                emptyList()
            }
        }

    override suspend fun search(query: String): List<CineMovie> =
        withContext(Dispatchers.IO) {
            try {
                val doc = fetchDocument("$BASE/?s=$query")
                parseMovieList(doc)
            } catch (e: Exception) {
                Log.e(TAG, "search failed: $query", e)
                emptyList()
            }
        }

    // ═══ Movie Detail ═══

    override suspend fun getDetailWithFshare(detailUrl: String): ThuVienCineRepository.CineDetailResult =
        withContext(Dispatchers.IO) {
            val doc = fetchDocument(detailUrl)

            // Title: h1 contains "Tên Việt (2026)"
            val h1Text = doc.selectFirst("h1")?.text()?.trim() ?: ""
            val yearFromH1 = Regex("""\((\d{4})\)""").find(h1Text)?.groupValues?.get(1) ?: ""
            val viTitle = h1Text.replace(Regex("""\s*\(\d{4}\)\s*"""), "").trim()

            // English title: .data h2
            val enTitle = doc.selectFirst(".data h2")?.text()?.trim() ?: ""

            // Poster: .poster img or meta og:image
            var posterUrl = doc.selectFirst(".poster img")?.attr("src")
                ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: ""
            posterUrl = upgradePosterResolution(posterUrl)

            // Backdrop: try og:image as fallback (thuvienhd doesn't have separate backdrop div)
            val backdropUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: posterUrl

            // Description: #info section text, or Nội dung section
            val description = doc.selectFirst("#info")?.text()?.trim()
                ?: doc.select(".wp-content p").text().trim()

            // Rating: look in .extra .metadata or IMDB text
            val ratingText = doc.select(".extra .metadata span, .imdb-rating, .dt_rating_vgs").text()
            val rating = Regex("""([\d.]+)""").find(ratingText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            // Country inference
            val seoText = buildString {
                doc.select("#info, .wp-content").forEach { append(it.text()).append(" ") }
                doc.select("meta[property=og:description], meta[name=description], meta[name=keywords]")
                    .forEach { append(it.attr("content")).append(" ") }
                append(h1Text).append(" ").append(enTitle)
            }
            val country = inferCountry(seoText).ifEmpty { inferCountry(doc.body().text()) }

            // Fshare links — DIRECT file URLs (multi-quality)
            val fshareLinks = doc.select("a[href*=fshare.vn/file/], a[href*=fshare.vn/folder/]")
            var fshareLink: CineFshareLink? = null

            if (fshareLinks.isNotEmpty()) {
                // Use the first link as the primary; player will use episodeSlug for direct access
                fshareLink = CineFshareLink(folderUrl = fshareLinks.first()?.attr("href") ?: "")
            } else {
                // Fallback: try any fshare.vn link
                val anyFshare = doc.selectFirst("a[href*=fshare.vn]")
                if (anyFshare != null) {
                    fshareLink = CineFshareLink(folderUrl = anyFshare.attr("href"))
                }
            }

            ThuVienCineRepository.CineDetailResult(
                title = viTitle,
                originName = enTitle,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                description = description,
                year = yearFromH1,
                rating = rating,
                fshareLink = fshareLink,
                country = country
            )
        }

    // ═══ Internal Parsing ═══

    private fun parseMovieList(doc: Document): List<CineMovie> {
        // Primary: article.item.movies (standard thuvienhd layout)
        val articles = doc.select("article.item.movies, article.item")
        if (articles.isNotEmpty()) {
            return articles.mapNotNull { parseArticleItem(it) }.distinctBy { it.detailUrl }
        }

        // Fallback: any article with links
        val fallback = doc.select("article")
        if (fallback.isNotEmpty()) {
            return fallback.mapNotNull { parseArticleItem(it) }.distinctBy { it.detailUrl }
        }

        // Last resort: link-based parsing
        return parseLinkBased(doc)
    }

    private fun parseArticleItem(article: Element): CineMovie? {
        try {
            // Title + detail link from .data h3 a
            val titleLink = article.selectFirst(".data h3 a")
                ?: article.selectFirst("h3 a")
                ?: return null

            val title = titleLink.text().trim()
            if (title.isBlank()) return null

            val detailUrl = titleLink.attr("abs:href")
            if (detailUrl.isBlank() || domain !in detailUrl) return null

            // Poster image
            val imgElem = article.selectFirst(".poster img")
            var posterUrl = imgElem?.attr("src")
                ?: imgElem?.attr("data-src")
                ?: imgElem?.attr("data-lazy-src")
                ?: ""
            posterUrl = upgradePosterResolution(posterUrl)

            // Year from .data span
            val year = article.selectFirst(".data span")?.text()?.trim() ?: ""

            // Quality badge
            val quality = article.selectFirst(".poster .quality_slider, .quality, .calidad")
                ?.text()?.trim() ?: ""

            // Slug from URL
            val slug = detailUrl.trimEnd('/').substringAfterLast('/')

            return CineMovie(
                title = title,
                slug = slug,
                thumbnailUrl = posterUrl,
                quality = quality,
                detailUrl = detailUrl,
                year = year
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseArticleItem error", e)
            return null
        }
    }

    private fun parseLinkBased(doc: Document): List<CineMovie> {
        return doc.select("a[href*=$domain][title]")
            .mapNotNull { link ->
                val href = link.attr("abs:href")
                val title = link.attr("title")
                if (title.isBlank() || href.contains("/page/") ||
                    href.contains("/genre/")) return@mapNotNull null

                val img = link.selectFirst("img")
                var thumbnail = img?.attr("src")
                    ?: img?.attr("data-src")
                    ?: ""
                thumbnail = upgradePosterResolution(thumbnail)

                val yearMatch = Regex("""\((\d{4})\)""").find(title)
                val year = yearMatch?.groupValues?.get(1) ?: ""
                val cleanTitle = title.replace(Regex("""\s*\(\d{4}\)\s*"""), "").trim()

                CineMovie(
                    title = cleanTitle,
                    slug = href.trimEnd('/').substringAfterLast('/'),
                    thumbnailUrl = thumbnail,
                    quality = "",
                    detailUrl = href,
                    year = year
                )
            }
            .distinctBy { it.detailUrl }
    }

    // ═══ Helpers ═══

    private fun upgradePosterResolution(url: String): String {
        if (url.isBlank()) return url
        return url.replace("w220_and_h330_face", "w600_and_h900_bestv2")
    }

    private fun fetchDocument(url: String): Document {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .get()
    }

    // ═══ Country Inference (shared logic with ThuVienCine) ═══

    private fun inferCountry(pageText: String): String {
        if (pageText.isBlank()) return ""
        val text = Normalizer.normalize(pageText, Normalizer.Form.NFC)

        val hangul = Regex("[\uAC00-\uD7AF\u1100-\u11FF]")
        val kana = Regex("[\u3040-\u309F\u30A0-\u30FF\u31F0-\u31FF]")
        val cjk = Regex("[\u4E00-\u9FFF\u3400-\u4DBF]")
        val thai = Regex("[\u0E00-\u0E7F]")
        val devanagari = Regex("[\u0900-\u097F]")

        return when {
            hangul.containsMatchIn(text) -> "han-quoc"
            kana.containsMatchIn(text) -> "nhat-ban"
            cjk.containsMatchIn(text) -> "trung-quoc"
            thai.containsMatchIn(text) -> "thai-lan"
            devanagari.containsMatchIn(text) -> "an-do"
            else -> ""
        }
    }

    companion object {
        private const val TAG = "ThuVienHd"
        private const val BASE = "https://thuvienhd.top"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        private const val TIMEOUT_MS = 15_000
    }
}
