package xyz.raidenhub.phim.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import xyz.raidenhub.phim.data.api.SubDLApi
import xyz.raidenhub.phim.data.api.OpenSubtitlesApi
import xyz.raidenhub.phim.data.api.SubSourceApi
import xyz.raidenhub.phim.data.api.models.ConsumetSubtitle
import xyz.raidenhub.phim.data.api.models.SubtitleResult
import java.util.concurrent.TimeUnit

object SubtitleRepository {

    // ═══ API Keys (free tier) ═══
    private const val SUBDL_API_KEY = "TZl_QW7_oilBuLi_sFHsdXt9xEIbZoCE"
    private const val OPENSUBTITLES_API_KEY = ""  // TODO: Register at opensubtitles.com
    private const val SUBSOURCE_API_KEY = "sk_2671f0db43c54ffeb17d71e0c18b9de17aa7e97e8cdf17b55f155ef87294e4ea"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val subDLApi: SubDLApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.subdl.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubDLApi::class.java)
    }

    private val openSubtitlesApi: OpenSubtitlesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.opensubtitles.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenSubtitlesApi::class.java)
    }

    private val subSourceApi: SubSourceApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.subsource.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SubSourceApi::class.java)
    }

    /**
     * Search subtitles from ALL sources simultaneously
     * Priority: Vietnamese > English > others
     */
    suspend fun searchSubtitles(
        filmName: String,
        year: String? = null,
        type: String? = null,
        season: Int? = null,
        episode: Int? = null,
        consumetSubtitles: List<ConsumetSubtitle> = emptyList()
    ): List<SubtitleResult> = coroutineScope {
        val results = mutableListOf<SubtitleResult>()

        // Source 1: Consumet (already available from stream response)
        val consumetSubs = consumetSubtitles.map { sub ->
            SubtitleResult(
                url = sub.url,
                language = guessLanguageCode(sub.lang),
                languageLabel = sub.lang,
                source = "FlixHQ"
            )
        }
        results.addAll(consumetSubs)

        // Source 2, 3, 4: SubDL + OpenSubtitles + Subscene (parallel)
        val deferredResults = listOfNotNull(
            // SubDL
            if (SUBDL_API_KEY.isNotBlank()) async {
                searchSubDL(filmName, year, type, season, episode)
            } else null,

            // OpenSubtitles
            if (OPENSUBTITLES_API_KEY.isNotBlank()) async {
                searchOpenSubtitles(filmName, year, type, season, episode)
            } else null,

            // Subscene (scrape)
            async {
                searchSubscene(filmName)
            },

            // SubSource
            if (SUBSOURCE_API_KEY.isNotBlank()) async {
                searchSubSource(filmName)
            } else null
        )

        deferredResults.awaitAll().forEach { results.addAll(it) }

        // Sort: Vietnamese first, then English, then by download count
        results.sortedWith(
            compareByDescending<SubtitleResult> { it.language == "vi" }
                .thenByDescending { it.language == "en" }
                .thenByDescending { it.downloadCount }
        )
    }

    // ═══ SubDL Provider ═══
    /**
     * Direct access to SubDL search (for download flow in ViewModel)
     */
    suspend fun searchSubDLDirect(filmName: String): xyz.raidenhub.phim.data.api.models.SubDLResponse {
        return subDLApi.search(
            apiKey = SUBDL_API_KEY,
            filmName = filmName,
            languages = "vi,en"
        )
    }

    private suspend fun searchSubDL(
        filmName: String,
        year: String?,
        type: String?,
        season: Int?,
        episode: Int?
    ): List<SubtitleResult> {
        return try {
            val response = subDLApi.search(
                apiKey = SUBDL_API_KEY,
                filmName = filmName,
                languages = "vi,en",
                type = type,
                year = year,
                seasonNumber = season,
                episodeNumber = episode
            )
            response.subtitles.map { sub ->
                SubtitleResult(
                    url = "https://dl.subdl.com${sub.url}",
                    language = mapSubDLLanguage(sub.lang),
                    languageLabel = sub.language.ifBlank { sub.lang },
                    source = "SubDL",
                    fileName = sub.releaseName,
                    isHearingImpaired = sub.hearingImpaired
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ═══ OpenSubtitles Provider ═══
    private suspend fun searchOpenSubtitles(
        filmName: String,
        year: String?,
        type: String?,
        season: Int?,
        episode: Int?
    ): List<SubtitleResult> {
        return try {
            val response = openSubtitlesApi.search(
                apiKey = OPENSUBTITLES_API_KEY,
                query = filmName,
                languages = "vi,en",
                type = type,
                year = year?.toIntOrNull(),
                seasonNumber = season,
                episodeNumber = episode
            )
            response.data.flatMap { item ->
                item.attributes.files.map { file ->
                    SubtitleResult(
                        url = "",
                        language = item.attributes.language,
                        languageLabel = getLanguageName(item.attributes.language),
                        source = "OpenSubs",
                        fileName = file.fileName,
                        downloadCount = item.attributes.downloadCount,
                        isHearingImpaired = item.attributes.hearingImpaired
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ═══ SubSource Provider ═══
    private suspend fun searchSubSource(filmName: String): List<SubtitleResult> {
        return try {
            // Step 1: Search movie
            val searchResult = subSourceApi.searchMovies(
                apiKey = SUBSOURCE_API_KEY,
                query = filmName
            )
            val movie = searchResult.data.firstOrNull() ?: return emptyList()

            // Step 2: Get Vietnamese + English subs
            val subs = subSourceApi.getSubtitles(
                apiKey = SUBSOURCE_API_KEY,
                movieId = movie.id
            )
            subs.data
                .filter { it.language.lowercase() in listOf("vietnamese", "english", "vi", "en") }
                .map { sub ->
                    SubtitleResult(
                        url = "https://api.subsource.net/api/v1/subtitles/${sub.id}/download",
                        language = guessLanguageCode(sub.language),
                        languageLabel = sub.language,
                        source = "SubSource",
                        fileName = sub.release_name,
                        downloadCount = sub.download_count,
                        isHearingImpaired = sub.hearing_impaired
                    )
                }
                .take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ═══ Subscene Provider (HTML Scrape) ═══
    private suspend fun searchSubscene(filmName: String): List<SubtitleResult> {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val results = mutableListOf<SubtitleResult>()

                // Step 1: Search for the movie on Subscene
                val searchUrl = "https://subscene.com/subtitles/searchbytitle?query=${
                    java.net.URLEncoder.encode(filmName, "UTF-8")
                }"
                val searchHtml = httpGet(searchUrl) ?: return@withContext emptyList()

                // Step 2: Find first matching result link
                // Pattern: <a href="/subtitles/movie-name">
                val linkRegex = Regex("""<a\s+href="(/subtitles/[^"]+?)"\s*>""")
                val firstMatch = linkRegex.find(searchHtml)
                    ?: return@withContext emptyList()
                val subtitlePagePath = firstMatch.groupValues[1]

                // Skip if it's the search page itself
                if (subtitlePagePath.contains("searchbytitle")) {
                    return@withContext emptyList()
                }

                // Step 3: Get subtitle list page
                val subtitleUrl = "https://subscene.com$subtitlePagePath"
                val pageHtml = httpGet(subtitleUrl) ?: return@withContext emptyList()

                // Step 4: Parse subtitles — look for Vietnamese and English
                // Each subtitle row has language span + release name
                val subRegex = Regex(
                    """<a\s+href="(/subtitles/[^"]+?)"[^>]*>\s*<span[^>]*>\s*(\w+)\s*</span>\s*<span>\s*([^<]*?)\s*</span>""",
                    RegexOption.DOT_MATCHES_ALL
                )

                for (match in subRegex.findAll(pageHtml)) {
                    val path = match.groupValues[1]
                    val langName = match.groupValues[2].trim()
                    val releaseName = match.groupValues[3].trim()

                    // Only keep Vietnamese and English
                    val langCode = guessLanguageCode(langName)
                    if (langCode != "vi" && langCode != "en") continue

                    results.add(
                        SubtitleResult(
                            url = "https://subscene.com$path",
                            language = langCode,
                            languageLabel = langName,
                            source = "Subscene",
                            fileName = releaseName
                        )
                    )

                    // Limit to 10 per source
                    if (results.size >= 10) break
                }

                results
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ═══ HTTP Helper for scraping ═══
    private fun httpGet(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9,vi;q=0.8")
                .header("Accept", "text/html,application/xhtml+xml")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ═══ Language Mapping ═══
    private fun guessLanguageCode(lang: String): String = when {
        lang.contains("vietnam", ignoreCase = true) || lang == "vi" -> "vi"
        lang.contains("english", ignoreCase = true) || lang == "en" -> "en"
        lang.contains("japan", ignoreCase = true) || lang == "ja" -> "ja"
        lang.contains("korean", ignoreCase = true) || lang == "ko" -> "ko"
        lang.contains("chinese", ignoreCase = true) || lang == "zh" -> "zh"
        lang.contains("french", ignoreCase = true) || lang == "fr" -> "fr"
        lang.contains("spanish", ignoreCase = true) || lang == "es" -> "es"
        lang.contains("arabic", ignoreCase = true) || lang == "ar" -> "ar"
        else -> lang.lowercase().take(2)
    }

    private fun mapSubDLLanguage(lang: String): String = when (lang) {
        "vietnamese" -> "vi"
        "english" -> "en"
        "japanese" -> "ja"
        "korean" -> "ko"
        "chinese" -> "zh"
        else -> lang.lowercase().take(2)
    }

    private fun getLanguageName(code: String): String = when (code) {
        "vi" -> "Vietnamese"
        "en" -> "English"
        "ja" -> "Japanese"
        "ko" -> "Korean"
        "zh" -> "Chinese"
        "fr" -> "French"
        "es" -> "Spanish"
        else -> code.uppercase()
    }
}
