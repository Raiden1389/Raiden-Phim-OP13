package xyz.raidenhub.phim.ui.screens.superstream

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.TmdbSearchItem
import xyz.raidenhub.phim.data.repository.SuperStreamRepository

/**
 * ViewModel for SuperStream browse/search screen.
 */
class SuperStreamViewModel : ViewModel() {

    private val _trendingMovies = MutableStateFlow<List<TmdbSearchItem>>(emptyList())
    val trendingMovies = _trendingMovies.asStateFlow()

    private val _trendingTv = MutableStateFlow<List<TmdbSearchItem>>(emptyList())
    val trendingTv = _trendingTv.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TmdbSearchItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            // Load movies and TV in parallel
            launch {
                SuperStreamRepository.trendingMovies()
                    .onSuccess { _trendingMovies.value = it }
            }
            launch {
                SuperStreamRepository.trendingTv()
                    .onSuccess { _trendingTv.value = it }
            }
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            SuperStreamRepository.search(query)
                .onSuccess { response ->
                    _searchResults.value = response.results.filter {
                        it.type == "movie" || it.type == "tv"
                    }
                }
                .onFailure { _searchResults.value = emptyList() }
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
