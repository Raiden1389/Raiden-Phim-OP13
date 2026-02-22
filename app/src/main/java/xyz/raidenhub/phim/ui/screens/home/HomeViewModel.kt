package xyz.raidenhub.phim.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.util.AppError
import xyz.raidenhub.phim.util.toAppError

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
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
}

sealed class HomeState {
    data object Loading : HomeState()
    data class Success(val data: MovieRepository.HomeData) : HomeState()
    data class Error(
        val message: String,
        val isRetryable: Boolean = true  // "Thử lại" button chỉ hiện khi NetworkError
    ) : HomeState()
}
