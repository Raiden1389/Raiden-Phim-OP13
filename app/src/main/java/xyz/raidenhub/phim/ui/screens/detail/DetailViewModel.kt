package xyz.raidenhub.phim.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.util.toAppError

class DetailViewModel : ViewModel() {
    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state = _state.asStateFlow()

    fun load(slug: String) {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            MovieRepository.getMovieDetail(slug)
                .onSuccess { _state.value = DetailState.Success(it.movie, it.episodes) }
                .onFailure { e ->
                    val err = e.toAppError()
                    _state.value = DetailState.Error(
                        message = err.userMessage,
                        isRetryable = err.isRetryable
                    )
                }
        }
    }
}

sealed class DetailState {
    data object Loading : DetailState()
    data class Success(val movie: MovieDetail, val episodes: List<EpisodeServer>) : DetailState()
    data class Error(
        val message: String,
        val isRetryable: Boolean = true
    ) : DetailState()
}
