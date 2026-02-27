package xyz.raidenhub.phim.ui.screens.community

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.repository.CommunityRepository
import xyz.raidenhub.phim.data.repository.CommunityRepository.CommunityMovie
import xyz.raidenhub.phim.data.repository.CommunityRepository.CommunitySource

/**
 * ViewModel for Community screens.
 *
 * Handles multi-level navigation with a stack:
 *   Level 1: Community source list (people who shared)
 *   Level 2+: Movies/categories from a source (can drill-down into sub-sheets)
 *   → Click Fshare folder/file → handled by FshareDetailScreen
 */
class CommunityViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CommunityRepository()

    // ═══ Level 1: Source List ═══
    var sources by mutableStateOf<List<CommunitySource>>(emptyList())
        private set
    var isLoadingSources by mutableStateOf(false)
        private set
    var sourcesError by mutableStateOf<String?>(null)
        private set

    // ═══ Level 2+: Movie/Category List ═══
    var movies by mutableStateOf<List<CommunityMovie>>(emptyList())
        private set
    var isLoadingMovies by mutableStateOf(false)
        private set
    var moviesError by mutableStateOf<String?>(null)
        private set
    var currentSourceName by mutableStateOf("")
        private set

    // Navigation
    var currentLevel by mutableStateOf(1)
        private set

    // ═══ Navigation Stack ═══
    private data class NavEntry(
        val name: String,
        val sheetUrl: String,
        val movies: List<CommunityMovie>
    )
    private val navStack = mutableListOf<NavEntry>()

    fun loadSources() {
        if (sources.isNotEmpty()) return
        viewModelScope.launch {
            isLoadingSources = true
            sourcesError = null
            try {
                sources = repo.getCommunityList()
                if (sources.isEmpty()) {
                    sourcesError = "Không tìm thấy nguồn cộng đồng"
                }
            } catch (e: Exception) {
                sourcesError = "Lỗi: ${e.message}"
            }
            isLoadingSources = false
        }
    }

    fun loadMovies(source: CommunitySource) {
        if (currentLevel >= 2 && movies.isNotEmpty()) {
            navStack.add(NavEntry(currentSourceName, "", movies))
        }

        currentSourceName = source.name
        currentLevel = 2

        viewModelScope.launch {
            isLoadingMovies = true
            moviesError = null
            movies = emptyList()
            try {
                movies = repo.getSourceMovies(source.sheetUrl)
                if (movies.isEmpty()) {
                    moviesError = "Danh sách trống hoặc nguồn không khả dụng"
                }
            } catch (e: Exception) {
                moviesError = "Lỗi tải: ${e.message}"
            }
            isLoadingMovies = false
        }
    }

    /** Navigate back. Returns true if handled internally (don't exit screen). */
    fun goBack(): Boolean {
        if (currentLevel >= 2) {
            if (navStack.isNotEmpty()) {
                val prev = navStack.removeAt(navStack.lastIndex)
                currentSourceName = prev.name
                movies = prev.movies
                moviesError = null
                return true
            }
            currentLevel = 1
            movies = emptyList()
            moviesError = null
            return true
        }
        return false
    }

    /** Breadcrumb path for display */
    val breadcrumb: String
        get() {
            val parts = navStack.map { it.name } + currentSourceName
            return parts.joinToString(" › ")
        }
}
