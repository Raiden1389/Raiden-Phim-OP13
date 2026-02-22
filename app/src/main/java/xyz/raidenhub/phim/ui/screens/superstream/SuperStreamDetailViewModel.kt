package xyz.raidenhub.phim.ui.screens.superstream

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.*
import xyz.raidenhub.phim.data.repository.SuperStreamRepository

/**
 * ViewModel for SuperStream detail screen.
 * TV shows: TMDB seasons/episodes (always available)
 * Streaming: ShowBox → FebBox Direct (OkHttp)
 *
 * Availability check: calls ShowBox to see if content has a FebBox link.
 * 🟢 Available → show Play button
 * 🔴 Not available → show "Not available" message
 */
class SuperStreamDetailViewModel : ViewModel() {

    companion object {
        private const val TAG = "SSDetailVM"
    }

    // ═══ State ═══

    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state = _state.asStateFlow()

    // Availability: is this content on ShowBox/FebBox?
    private val _availability = MutableStateFlow<AvailabilityState>(AvailabilityState.Checking)
    val availability = _availability.asStateFlow()

    // Cached share key for streaming
    private var cachedShareKey: String? = null

    // TV: TMDB seasons (from TmdbTvDetail.seasons)
    private val _tmdbSeasons = MutableStateFlow<List<TmdbSeason>>(emptyList())
    val tmdbSeasons = _tmdbSeasons.asStateFlow()

    // TV: TMDB episodes (from season detail)
    private val _tmdbEpisodes = MutableStateFlow<List<TmdbEpisode>>(emptyList())
    val tmdbEpisodes = _tmdbEpisodes.asStateFlow()

    private val _selectedSeason = MutableStateFlow(0)
    val selectedSeason = _selectedSeason.asStateFlow()

    private val _streamState = MutableStateFlow<StreamState>(StreamState.Idle)
    val streamState = _streamState.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    private var currentTmdbId: Int = 0
    private var currentType: String = ""
    private var currentTitle: String = ""

    // ═══ Load Detail ═══

    fun loadDetail(tmdbId: Int, type: String) {
        if (currentTmdbId == tmdbId) return  // Already loaded
        currentTmdbId = tmdbId
        currentType = type

        viewModelScope.launch {
            _state.value = DetailState.Loading
            _availability.value = AvailabilityState.Checking

            try {
                if (type == "movie") {
                    loadMovieDetail(tmdbId)
                } else {
                    loadTvDetail(tmdbId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadDetail error", e)
                _state.value = DetailState.Error(e.message ?: "Unknown error")
            }

            // Check ShowBox availability in parallel
            checkAvailability(tmdbId, type)
        }
    }

    private suspend fun loadMovieDetail(tmdbId: Int) {
        val detail = SuperStreamRepository.getMovieDetail(tmdbId).getOrThrow()
        currentTitle = detail.title
        _state.value = DetailState.MovieSuccess(detail)
        Log.d(TAG, "Movie loaded: ${detail.title}")
    }

    private suspend fun loadTvDetail(tmdbId: Int) {
        val detail = SuperStreamRepository.getTvDetail(tmdbId).getOrThrow()
        currentTitle = detail.name
        _state.value = DetailState.TvSuccess(detail)
        Log.d(TAG, "TV loaded: ${detail.name}, ${detail.seasons.size} seasons")

        // Use TMDB seasons directly (filter out "Specials" season 0)
        val realSeasons = detail.seasons.filter { it.seasonNumber > 0 }
        _tmdbSeasons.value = realSeasons

        // Auto-load first season episodes
        if (realSeasons.isNotEmpty()) {
            selectSeason(0)
        }
    }

    // ═══ Availability Check ═══

    private suspend fun checkAvailability(tmdbId: Int, type: String) {
        _availability.value = AvailabilityState.Checking

        // Primary: search ShowBox by title (correct internal ID mapping)
        if (currentTitle.isNotBlank()) {
            SuperStreamRepository.getShareKeyByTitle(currentTitle, type)
                .onSuccess { key ->
                    cachedShareKey = key
                    _availability.value = AvailabilityState.Available(key)
                    Log.d(TAG, "✅ Available via ShowBox search: $key")
                    return
                }
                .onFailure { e ->
                    Log.w(TAG, "ShowBox search failed, trying TMDB ID fallback: ${e.message}")
                }
        }

        // Fallback: try TMDB ID directly (works when IDs happen to match)
        SuperStreamRepository.getShareKey(tmdbId, type)
            .onSuccess { key ->
                cachedShareKey = key
                _availability.value = AvailabilityState.Available(key)
                Log.d(TAG, "✅ Available via TMDB ID fallback: $key")
            }
            .onFailure { e ->
                _availability.value = AvailabilityState.NotAvailable
                Log.w(TAG, "❌ Not available on ShowBox: ${e.message}")
            }
    }

    // ═══ Season/Episode Navigation ═══

    fun selectSeason(index: Int) {
        _selectedSeason.value = index
        val season = _tmdbSeasons.value.getOrNull(index) ?: return

        viewModelScope.launch {
            _tmdbEpisodes.value = emptyList()
            _statusMessage.value = "Loading episodes..."

            SuperStreamRepository.getTvSeasonDetail(currentTmdbId, season.seasonNumber)
                .onSuccess { seasonDetail ->
                    _tmdbEpisodes.value = seasonDetail.episodes
                    _statusMessage.value = ""
                    Log.d(TAG, "Loaded ${seasonDetail.episodes.size} episodes for S${season.seasonNumber}")
                }
                .onFailure { e ->
                    _statusMessage.value = "Failed to load episodes: ${e.message}"
                    Log.e(TAG, "Season detail error", e)
                }
        }
    }

    // ═══ Play via ShowBox + FebBox Direct ═══

    fun playTvEpisode(season: Int, episode: Int) {
        viewModelScope.launch {
            _streamState.value = StreamState.Loading
            _statusMessage.value = "Getting stream link..."

            SuperStreamRepository.streamTvEpisode(currentTmdbId, season, episode, currentTitle, cachedShareKey)
                .onSuccess { stream ->
                    _streamState.value = StreamState.Ready(stream, season = season, episode = episode)
                    _statusMessage.value = ""
                }
                .onFailure { e ->
                    _streamState.value = StreamState.Error(e.message ?: "Failed")
                    _statusMessage.value = "⚠ Stream error: ${e.message}"
                }
        }
    }

    fun playMovie() {
        viewModelScope.launch {
            _streamState.value = StreamState.Loading
            _statusMessage.value = "Getting stream link..."

            SuperStreamRepository.streamMovie(currentTmdbId, currentTitle, cachedShareKey)
                .onSuccess { stream ->
                    _streamState.value = StreamState.Ready(stream)
                    _statusMessage.value = ""
                }
                .onFailure { e ->
                    _streamState.value = StreamState.Error(e.message ?: "Failed")
                    _statusMessage.value = "⚠ Stream error: ${e.message}"
                }
        }
    }

    fun resetStreamState() {
        _streamState.value = StreamState.Idle
    }
}

// ═══ State sealed classes ═══

sealed class DetailState {
    data object Loading : DetailState()
    data class MovieSuccess(val movie: TmdbMovieDetail) : DetailState()
    data class TvSuccess(val tv: TmdbTvDetail) : DetailState()
    data class Error(val message: String) : DetailState()
}

sealed class StreamState {
    data object Idle : StreamState()
    data object Loading : StreamState()
    data class Ready(val stream: FebBoxStream, val season: Int = 0, val episode: Int = 0) : StreamState()
    data class Error(val message: String) : StreamState()
}

sealed class AvailabilityState {
    data object Checking : AvailabilityState()
    data class Available(val shareKey: String) : AvailabilityState()
    data object NotAvailable : AvailabilityState()
}
