package xyz.raidenhub.phim.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.CineMovie
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.data.repository.FshareAggregator
import xyz.raidenhub.phim.util.AppError
import xyz.raidenhub.phim.util.toAppError

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        loadFshare()  // Fshare loads independently, non-blocking
        viewModelScope.launch {
            // Cache hit → Success ngay, không qua Loading → zero shimmer khi swipe tab
            MovieRepository.getCachedHomeData()?.let {
                _state.value = HomeState.Success(it)
                return@launch
            }
            _state.value = HomeState.Loading
            MovieRepository.getHomeData()
                .onSuccess { _state.value = HomeState.Success(it) }
                .onFailure { e ->
                    val err = e.toAppError()
                    _state.value = HomeState.Error(
                        message = err.userMessage,
                        isRetryable = err.isRetryable
                    )
                }
        }
    }
    // ═══ Fshare HD rows (ThuVienCine) ═══
    private val _fshareMovies = MutableStateFlow<List<CineMovie>>(emptyList())
    val fshareMovies = _fshareMovies.asStateFlow()
    private val _fshareSeries = MutableStateFlow<List<CineMovie>>(emptyList())
    val fshareSeries = _fshareSeries.asStateFlow()

    private fun loadFshare() {
        val hub = FshareAggregator()
        viewModelScope.launch {
            try {
                val movies = hub.getHomeMovies()
                _fshareMovies.value = movies.take(16)
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Fshare movies failed", e)
            }
        }
        viewModelScope.launch {
            try {
                val series = hub.getHomeSeries()
                _fshareSeries.value = series.take(16)
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "Fshare series failed", e)
            }
        }
    }
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val data: MovieRepository.HomeData) : HomeState()
    data class Error(
        val message: String,
        val isRetryable: Boolean = true  // "Thử lại" button chỉ hiện khi NetworkError
    ) : HomeState()
}
