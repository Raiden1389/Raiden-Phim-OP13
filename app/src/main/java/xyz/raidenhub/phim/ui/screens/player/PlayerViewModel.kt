package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.data.repository.SuperStreamRepository
import xyz.raidenhub.phim.util.Constants

class PlayerViewModel : ViewModel() {
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes = _episodes.asStateFlow()
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    private val _currentEp = MutableStateFlow(0)
    val currentEp = _currentEp.asStateFlow()

    // Country/Type-aware auto-next
    private val _autoNextMs = MutableStateFlow(Constants.AUTO_NEXT_BEFORE_END_MS)
    val autoNextMs = _autoNextMs.asStateFlow()
    private val _country = MutableStateFlow("")
    val country = _country.asStateFlow()
    private var _type = ""

    fun load(slug: String, serverIdx: Int, epIdx: Int) {
        viewModelScope.launch {
            MovieRepository.getMovieDetail(slug)
                .onSuccess { result ->
                    _title.value = result.movie.name
                    val eps = result.episodes.getOrNull(serverIdx)?.serverData.orEmpty()
                    _episodes.value = eps
                    _currentEp.value = epIdx.coerceIn(0, (eps.size - 1).coerceAtLeast(0))
                    // Detect country + type for smart timing
                    _country.value = result.movie.country.firstOrNull()?.slug ?: ""
                    _type = result.movie.type
                    _autoNextMs.value = Constants.getAutoNextMs(_country.value, _type)
                }
        }
    }

    fun setEpisode(idx: Int) { _currentEp.value = idx }
    fun hasNext() = _currentEp.value < _episodes.value.size - 1
    fun nextEp() { if (hasNext()) _currentEp.value++ }

    // ═══ SuperStream vars ═══
    private var ssTmdbId = 0
    private var ssSeason = 0
    private var ssShareKey: String? = null
    private var ssTitle: String? = null

    /**
     * SuperStream source: direct m3u8 URL.
     * For TV: builds placeholder episode list for the season, fetch current ep immediately.
     * For Movie: single-episode list (no next).
     */
    fun loadSuperStream(
        url: String,
        videoTitle: String = "",
        tmdbId: Int = 0,
        season: Int = 0,
        episode: Int = 0,
        type: String = "",
        totalEpisodes: Int = 0,
        shareKey: String = ""
    ) {
        _title.value = videoTitle
        ssTmdbId = tmdbId
        ssSeason = season
        ssShareKey = shareKey.ifBlank { null }
        ssTitle = videoTitle.ifBlank { null }

        if (type == "tv" && totalEpisodes > 0) {
            // Build placeholder list for all episodes in this season
            val placeholders = (1..totalEpisodes).map { ep ->
                Episode(
                    name = "Episode $ep",
                    slug = "ss::${tmdbId}::${season}::${ep}",
                    linkEmbed = "",
                    linkM3u8 = if (ep == episode) url else "" // Current ep has URL, others are lazy
                )
            }
            _episodes.value = placeholders
            _currentEp.value = episode - 1 // 0-based index
        } else {
            // Movie or unknown → single episode
            _episodes.value = listOf(
                Episode(
                    name = videoTitle,
                    slug = "direct",
                    linkEmbed = "",
                    linkM3u8 = url
                )
            )
            _currentEp.value = 0
        }
    }

    /** Fetch stream URL for a SuperStream episode (lazy, called by prefetch or next-ep) */
    fun fetchSuperStreamEp(season: Int, episode: Int) {
        viewModelScope.launch {
            SuperStreamRepository.streamTvEpisode(ssTmdbId, season, episode, ssTitle, ssShareKey)
                .onSuccess { stream ->
                    val slug = "ss::${ssTmdbId}::${season}::${episode}"
                    _episodes.value = _episodes.value.map { ep ->
                        if (ep.slug == slug) ep.copy(linkM3u8 = stream.url) else ep
                    }
                }
        }
    }

    // ═══ Fshare vars ═══
    private var fsSlug: String = ""
    private var fsPosterUrl: String = ""

    /**
     * Fshare source: resolve CDN URL via FsharePlayerLoader.
     * Builds episode list from folder, resolves current episode's download URL.
     */
    fun loadFshare(context: Context, movieSlug: String, episodeSlug: String, epIdx: Int = 0) {
        fsSlug = movieSlug
        viewModelScope.launch {
            try {
                val result = FsharePlayerLoader.load(context, movieSlug, episodeSlug)
                _title.value = result.movieName
                fsPosterUrl = result.posterUrl
                _episodes.value = result.episodes

                // Find current episode index by slug
                val idx = result.episodes.indexOfFirst { it.slug == episodeSlug }
                _currentEp.value = if (idx >= 0) idx else epIdx.coerceIn(0, (result.episodes.size - 1).coerceAtLeast(0))

                // Put CDN download URL into the episode's linkM3u8 for playback
                _episodes.value = _episodes.value.mapIndexed { i, ep ->
                    if (i == _currentEp.value) ep.copy(linkM3u8 = result.downloadUrl)
                    else ep
                }
            } catch (e: Exception) {
                // Fallback: single episode with error
                _title.value = "Fshare"
                _episodes.value = listOf(
                    Episode(name = "Lỗi: ${e.message}", slug = "error", linkEmbed = "", linkM3u8 = "")
                )
            }
        }
    }

    /**
     * Resolve CDN URL for a specific Fshare episode (lazy, called when switching episodes).
     */
    fun fetchFshareEp(context: Context, episodeSlug: String) {
        viewModelScope.launch {
            try {
                val fshareRepo = xyz.raidenhub.phim.data.repository.FshareRepository.getInstance(context)
                val cdnUrl = fshareRepo.resolveLink(episodeSlug)
                _episodes.value = _episodes.value.map { ep ->
                    if (ep.slug == episodeSlug) ep.copy(linkM3u8 = cdnUrl)
                    else ep
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerVM", "Fshare resolve failed: ${e.message}")
            }
        }
    }

    /** Fshare poster URL (for watch history thumbnail) */
    fun getFsharePosterUrl(): String = fsPosterUrl
}

// ═══ Utility ═══

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

/**
 * Strip quality/size suffixes from Fshare episode names.
 * "Tập 5 . 1080 3,3 GB" → "Tập 5"
 * "E05.mkv" → "E05"
 * Also trims common file extensions.
 */
fun cleanEpName(raw: String): String {
    return raw
        .replace(Regex("""\.mkv$|\.mp4$|\.avi$""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*[.\s]+\s*\d{3,4}p?\s*[\d.,]+\s*(GB|MB|KB|TB).*""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\.\s*\d{3,4}\s+[\d.,]+\s*(GB|MB).*""", RegexOption.IGNORE_CASE), "")
        .trim()
}

/**
 * Smart episode label — avoids double "Tập" prefix.
 * If name already starts with "Tập" or a number, just use it.
 * Otherwise prepend "Tập ".
 */
fun smartEpLabel(name: String, fallbackIdx: Int): String {
    val clean = cleanEpName(name.ifBlank { "${fallbackIdx + 1}" })
    return if (clean.startsWith("Tập", ignoreCase = true) ||
        clean.startsWith("Episode", ignoreCase = true) ||
        clean.first().isDigit()
    ) clean else "Tập $clean"
}
