package xyz.raidenhub.phim.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import xyz.raidenhub.phim.data.api.models.CineFshareLink
import xyz.raidenhub.phim.data.api.models.CineMovie
import xyz.raidenhub.phim.util.Constants
import java.text.Normalizer

/**
 * ThuVienCine Repository — scrapes thuviencine.com for movie listings with Fshare links
 *
 * HTML structure (confirmed from VietMediaF tvcine.py source):
 *   div[id^="post-"]
 *     h2.movie-title     → "Tên Việt – Tên Anh"
 *     span.movie-date    → year
 *     span.genre         → genre
 *     div.imdb-rating    → rating
 *     span[class^="item-quality"] → quality (4K, FHD, Vietsub)
 *     p.movie-description → plot
 *     img.lazy[data-src] → poster (can upgrade resolution)
 *     div.movie-backdrop[data-backdrop] → fanart
 *     a[href]            → detail link
 *
 * Flow:
 *   getMovies(categoryUrl, page) → [CineMovie]
 *   search(query) → [CineMovie]
 *   getFshareLink(detailUrl) → CineFshareLink (fshare.vn/folder/XXX)
 */
class ThuVienCineRepository : FshareSource {

    override val sourceId = "cine"
    override val domain = "thuviencine.com"
    override val homeMoviesUrl = "$BASE/movies/"
    override val homeSeriesUrl = "$BASE/tv-series/"

    // ═══ Movie Listings ═══

    /**
     * Fetch movies from a category URL with pagination
     * @param categoryUrl e.g. "https://thuviencine.com/movies/"
     * @param page 1-based page number
     */
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

    /**
     * Search movies by keyword
     */
    override suspend fun search(query: String): List<CineMovie> = withContext(Dispatchers.IO) {
        try {
            val doc = fetchDocument("$BASE/?s=$query")
            parseMovieList(doc)
        } catch (e: Exception) {
            Log.e(TAG, "search failed: $query", e)
            emptyList()
        }
    }

    // ═══ Movie Detail → Full Info + Fshare Link ═══

    /** Result from detail page scraping */
    data class CineDetailResult(
        val title: String,
        val originName: String,
        val posterUrl: String,
        val backdropUrl: String,
        val description: String,
        val year: String,
        val rating: Float,
        val fshareLink: CineFshareLink?,
        val country: String = ""  // OPhim slug: "han-quoc", "trung-quoc", etc. "" = unknown
    )

    /**
     * Scrape detail page for movie info + Fshare link (1 request)
     */
    override suspend fun getDetailWithFshare(detailUrl: String): CineDetailResult =
        withContext(Dispatchers.IO) {
            val doc = fetchDocument(detailUrl)

            // Title
            val fullTitle = doc.selectFirst("h1, h2.movie-title, .entry-title")?.text()?.trim() ?: ""
            val (viTitle, enTitle) = splitTitle(fullTitle)

            // Poster: meta og:image or img in detail
            var posterUrl = doc.selectFirst("meta[property=og:image]")?.attr("content") ?: ""
            if (posterUrl.isBlank()) {
                val imgElem = doc.selectFirst(".detail-poster img, .movie-poster img, .entry-content img")
                posterUrl = imgElem?.attr("data-src") ?: imgElem?.attr("src") ?: ""
            }
            posterUrl = upgradePosterResolution(posterUrl)

            // Backdrop
            val backdropElem = doc.selectFirst("div.movie-backdrop, div[data-backdrop]")
            var backdropUrl = backdropElem?.attr("data-backdrop") ?: posterUrl
            backdropUrl = upgradeBackdropResolution(backdropUrl)

            // Description
            val description = doc.selectFirst(".movie-description, .entry-content p, meta[name=description]")?.text()?.trim() ?: ""

            // Year
            val year = doc.selectFirst("span.movie-date, .release-date")?.text()?.trim() ?: ""

            // Rating
            val ratingText = doc.selectFirst("div.imdb-rating, span.imdb")?.text() ?: ""
            val rating = Regex("""([\d.]+)""").find(ratingText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            // Country inference: scan SEO zones first, fallback to full body
            val seoText = buildString {
                doc.select(".entry-content, .movie-description").forEach { append(it.text()).append(" ") }
                doc.select("meta[property=og:description], meta[name=description], meta[name=keywords]")
                    .forEach { append(it.attr("content")).append(" ") }
                doc.selectFirst("h1, h2.movie-title")?.parent()?.text()?.let { append(it) }
            }
            val country = inferCountry(seoText).ifEmpty { inferCountry(doc.body().text()) }

            // Fshare link (reuse existing logic inline)
            var fshareLink: CineFshareLink? = null

            val directFshare = doc.select("a[href*=fshare.vn]").firstOrNull()
            if (directFshare != null) {
                fshareLink = CineFshareLink(folderUrl = directFshare.attr("href"))
            } else {
                val downloadLink = doc.select("a[href*=download?id]").firstOrNull()
                    ?: doc.select("a[href*=download]").firstOrNull()
                if (downloadLink != null) {
                    try {
                        val downloadUrl = downloadLink.attr("abs:href")
                        val downloadDoc = fetchDocument(downloadUrl)
                        val fshare = downloadDoc.select("a[href*=fshare.vn]").firstOrNull()
                        if (fshare != null) {
                            fshareLink = CineFshareLink(
                                folderUrl = fshare.attr("href"),
                                downloadId = downloadUrl.substringAfter("id=", "")
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Download page fetch failed", e)
                    }
                }
            }

            CineDetailResult(
                title = viTitle,
                originName = enTitle,
                posterUrl = posterUrl,
                backdropUrl = backdropUrl,
                description = description,
                year = year,
                rating = rating,
                fshareLink = fshareLink,
                country = country
            )
        }

    /**
     * Extract Fshare folder/file link from a movie detail page
     * Flow: detail page → find fshare.vn links directly or via download?id=XXX
     */
    suspend fun getFshareLink(detailUrl: String): CineFshareLink? =
        withContext(Dispatchers.IO) {
            try {
                val detailDoc = fetchDocument(detailUrl)

                // Try direct fshare link in page first
                val directFshare = detailDoc.select("a[href*=fshare.vn]").firstOrNull()
                if (directFshare != null) {
                    return@withContext CineFshareLink(
                        folderUrl = directFshare.attr("href")
                    )
                }

                // Fallback: find download?id=XXX link → navigate → find fshare link
                val downloadLink = detailDoc.select("a[href*=download?id]").firstOrNull()
                    ?: detailDoc.select("a[href*=download]").firstOrNull()

                if (downloadLink != null) {
                    val downloadUrl = downloadLink.attr("abs:href")
                    val downloadDoc = fetchDocument(downloadUrl)

                    val fshareLink = downloadDoc.select("a[href*=fshare.vn]").firstOrNull()
                    if (fshareLink != null) {
                        return@withContext CineFshareLink(
                            folderUrl = fshareLink.attr("href"),
                            downloadId = downloadUrl.substringAfter("id=", "")
                        )
                    }
                }

                Log.w(TAG, "No Fshare link found in: $detailUrl")
                null
            } catch (e: Exception) {
                Log.e(TAG, "getFshareLink failed: $detailUrl", e)
                null
            }
        }

    // ═══ Internal Parsing ═══

    private fun parseMovieList(doc: Document): List<CineMovie> {
        val items = doc.select("div[id^=post-]")

        if (items.isNotEmpty()) {
            return items.mapNotNull { parsePostDiv(it) }.distinctBy { it.detailUrl }
        }

        val fallbackItems = doc.select("article, .item, .result-item, .post")
        if (fallbackItems.isNotEmpty()) {
            return fallbackItems.mapNotNull { parseFallbackItem(it) }.distinctBy { it.detailUrl }
        }

        return parseLinkBased(doc)
    }

    private fun parsePostDiv(div: Element): CineMovie? {
        try {
            val titleElem = div.selectFirst("h2.movie-title") ?: return null
            val fullTitle = titleElem.text().trim()
            if (fullTitle.isBlank()) return null

            val (viTitle, _) = splitTitle(fullTitle)
            val year = div.selectFirst("span.movie-date")?.text()?.trim() ?: ""
            val quality = div.selectFirst("span[class^=item-quality]")?.text()?.trim() ?: ""

            val imgElem = div.selectFirst("img.lazy")
            var posterUrl = imgElem?.attr("data-src") ?: imgElem?.attr("src") ?: ""
            posterUrl = upgradePosterResolution(posterUrl)

            val backdropElem = div.selectFirst("div.movie-backdrop")
            var backdropUrl = backdropElem?.attr("data-backdrop") ?: posterUrl
            backdropUrl = upgradeBackdropResolution(backdropUrl)

            val linkElem = div.selectFirst("a[href]") ?: return null
            val detailUrl = linkElem.attr("abs:href")
            if (detailUrl.isBlank() || !detailUrl.contains("thuviencine.com")) return null

            val description = div.selectFirst("p.movie-description")?.text()?.trim() ?: ""
            val ratingText = div.selectFirst("div.imdb-rating")?.text()?.trim() ?: ""
            val rating = Regex("""([\d.]+)""").find(ratingText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            return CineMovie(
                title = viTitle,
                slug = detailUrl.trimEnd('/').substringAfterLast('/'),
                thumbnailUrl = posterUrl,
                backdropUrl = backdropUrl,
                quality = quality,
                detailUrl = detailUrl,
                year = year,
                description = description,
                rating = rating
            )
        } catch (e: Exception) {
            Log.w(TAG, "parsePostDiv error", e)
            return null
        }
    }

    private fun parseFallbackItem(item: Element): CineMovie? {
        try {
            val link = item.selectFirst("a[href*=thuviencine.com]")
                ?: item.selectFirst("a[href]")
                ?: return null

            val href = link.attr("abs:href")
            if (href.isBlank() || !href.contains("thuviencine.com")) return null
            if (href.contains("/country/") || href.contains("/page/") ||
                href.endsWith("/movies/") || href.endsWith("/tv-series/")) return null

            val img = item.selectFirst("img")
            var thumbnail = img?.attr("data-src")
                ?: img?.attr("data-lazy-src")
                ?: img?.attr("src")
                ?: ""
            thumbnail = upgradePosterResolution(thumbnail)

            val title = link.attr("title").ifBlank {
                item.selectFirst("h2, h3, .title")?.text() ?: ""
            }
            if (title.isBlank()) return null

            val quality = item.selectFirst("span.quality, .calidad, span[class^=item-quality]")
                ?.text()?.trim() ?: ""

            val yearMatch = Regex("""\((\d{4})\)""").find(title)
            val year = yearMatch?.groupValues?.get(1) ?: ""
            val cleanTitle = title.replace(Regex("""\s*\(\d{4}\)\s*"""), "").trim()

            return CineMovie(
                title = cleanTitle,
                slug = href.trimEnd('/').substringAfterLast('/'),
                thumbnailUrl = thumbnail,
                quality = quality,
                detailUrl = href,
                year = year
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseLinkBased(doc: Document): List<CineMovie> {
        return doc.select("a[href*=thuviencine.com][title]")
            .mapNotNull { link ->
                val href = link.attr("abs:href")
                val title = link.attr("title")
                if (title.isBlank() || href.contains("/page/") ||
                    href.endsWith("/movies/") || href.endsWith("/tv-series/") ||
                    href.contains("/country/")) return@mapNotNull null

                val img = link.selectFirst("img")
                var thumbnail = img?.attr("data-src")
                    ?: img?.attr("data-lazy-src")
                    ?: img?.attr("src")
                    ?: ""
                thumbnail = upgradePosterResolution(thumbnail)

                val quality = link.selectFirst("span")?.text()?.trim() ?: ""
                val yearMatch = Regex("""\((\d{4})\)""").find(title)
                val year = yearMatch?.groupValues?.get(1) ?: ""
                val cleanTitle = title.replace(Regex("""\s*\(\d{4}\)\s*"""), "").trim()

                CineMovie(
                    title = cleanTitle,
                    slug = href.trimEnd('/').substringAfterLast('/'),
                    thumbnailUrl = thumbnail,
                    quality = quality,
                    detailUrl = href,
                    year = year
                )
            }
            .distinctBy { it.detailUrl }
    }

    // ═══ Helpers ═══

    private fun splitTitle(fullTitle: String): Pair<String, String> {
        val separators = listOf(" – ", " - ")
        for (sep in separators) {
            if (sep in fullTitle) {
                val parts = fullTitle.split(sep, limit = 2)
                return parts[0].trim() to parts.getOrElse(1) { "" }.trim()
            }
        }
        return fullTitle.trim() to ""
    }

    private fun upgradePosterResolution(url: String): String {
        if (url.isBlank()) return url
        return url.replace("w220_and_h330_face", "w600_and_h900_bestv2")
    }

    private fun upgradeBackdropResolution(url: String): String {
        if (url.isBlank()) return url
        return url.replace("w300", "w1280")
    }

    // ═══ HTTP ═══

    private fun fetchDocument(url: String): Document {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .followRedirects(true)
            .get()
    }

    // ═══ Country Inference ═══

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
        private const val TAG = "ThuVienCine"
        private const val BASE = Constants.THUVIENCINE_URL
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        private const val TIMEOUT_MS = 15_000

        /** Map OPhim country slug to display name */
        fun countryDisplayName(slug: String): String = when (slug) {
            "han-quoc" -> "Hàn Quốc"
            "trung-quoc" -> "Trung Quốc"
            "nhat-ban" -> "Nhật Bản"
            "thai-lan" -> "Thái Lan"
            "an-do" -> "Ấn Độ"
            "au-my" -> "Âu Mỹ"
            else -> slug.replaceFirstChar { it.uppercase() }
        }
    }
}
