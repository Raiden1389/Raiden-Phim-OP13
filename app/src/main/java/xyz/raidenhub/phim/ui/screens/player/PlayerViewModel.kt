package xyz.raidenhub.phim.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Episode
import xyz.raidenhub.phim.data.repository.AnimeRepository
import xyz.raidenhub.phim.data.repository.MovieRepository
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

    /**
     * Hướng B — Anime47 source:
     * episodeIds = danh sách ID của tất cả tập (từ latestEpisodes)
     * epIdx      = index tập muốn phát
     *
     * Flow: fetch tập đang chọn → bestStreamUrl → build Episode list
     * Tập kế: fetch on-demand khi user nhấn next
     */
    fun loadAnime47(episodeIds: IntArray, epIdx: Int, animeTitle: String = "") {
        viewModelScope.launch {
            _title.value = animeTitle
            val safeIdx = epIdx.coerceIn(0, (episodeIds.size - 1).coerceAtLeast(0))
            _currentEp.value = safeIdx
            // Build Episode placeholder list với id encoded vào slug (fetch lazy)
            // Format slug: "anime47::{episodeId}" để PlayerScreen biết cách fetch
            val placeholders = episodeIds.mapIndexed { i, id ->
                Episode(
                    name     = "Tập ${i + 1}",
                    slug     = "anime47::$id",
                    linkEmbed = "",
                    linkM3u8  = ""  // sẽ fetch khi play
                )
            }
            _episodes.value = placeholders
            // Fetch ngay stream cho tập hiện tại
            fetchAnime47Stream(episodeIds[safeIdx])
        }
    }

    /** Fetch M3U8 cho episode id, update episode trong list */
    fun fetchAnime47Stream(episodeId: Int) {
        viewModelScope.launch {
            AnimeRepository.getEpisodeStream(episodeId)
                .onSuccess { stream ->
                    val url = stream.bestStreamUrl
                    // Update placeholder → real M3U8
                    _episodes.value = _episodes.value.map { ep ->
                        if (ep.slug == "anime47::$episodeId") ep.copy(linkM3u8 = url) else ep
                    }
                }
        }
    }
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
