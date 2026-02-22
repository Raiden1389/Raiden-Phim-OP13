package xyz.raidenhub.phim.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.SearchHistoryManager
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.util.AppError
import xyz.raidenhub.phim.util.toAppError

class SearchViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Movie>>(emptyList())
    val results = _results.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // #13 — Search suggestions (live autocomplete from history + trending)
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    /** Cache of history list for sync access in updateSuggestions */
    private var _cachedHistory: List<String> = emptyList()

    init {
        viewModelScope.launch {
            SearchHistoryManager.history.collect { _cachedHistory = it }
        }
    }

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        updateSuggestions(query)
        _error.value = null
        if (query.length < 2) { _results.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _loading.value = true
            MovieRepository.search(query)
                .onSuccess { _results.value = it }
                .onFailure { e ->
                    _results.value = emptyList()
                    val err = e.toAppError()
                    // Chỉ show error khi NetworkError — ParseError silently ignored
                    if (err is AppError.NetworkError) {
                        _error.value = err.userMessage
                    }
                }
            _loading.value = false
        }
    }


    private fun updateSuggestions(query: String) {
        if (query.length < 2) { _suggestions.value = emptyList(); return }
        val q = query.lowercase()
        val history = _cachedHistory
        val trending = TRENDING_KEYWORDS

        // Combine history + trending, filter by prefix match
        val combined = (history + trending)
            .filter { it.lowercase().contains(q) && it.lowercase() != q }
            .distinctBy { it.lowercase() }
            .take(5)
        _suggestions.value = combined
    }
}

// ═══ S-3: Smart Keyword Normalization ═══
private val KEYWORD_MAP = mapOf(
    // Country variants
    "han quoc" to "Hàn Quốc", "han" to "Hàn Quốc",
    "trung quoc" to "Trung Quốc", "trung" to "Trung Quốc",
    "my" to "Mỹ", "nhat" to "Nhật Bản", "nhat ban" to "Nhật Bản",
    "thai" to "Thái Lan", "thai lan" to "Thái Lan",
    "anh" to "Anh", "phap" to "Pháp", "duc" to "Đức",
    // Genre shorthand
    "kinh di" to "Kinh dị",
    "hanh dong" to "Hành động",
    "tinh cam" to "Tình cảm", "lam ly" to "Lãng mạn",
    "vien tuong" to "Viễn tưởng", "sci fi" to "Viễn tưởng",
    "hoat hinh" to "Hoạt hình",
    "co trang" to "Cổ trang", "vo thuat" to "Võ thuật",
    "hai huoc" to "Hài hước",
    "gia dinh" to "Gia đình", "tam ly" to "Tâm lý",
    "phieu luu" to "Phiêu lưu", "chien tranh" to "Chiến tranh",
)

fun normalizeKeyword(raw: String): String {
    val lower = raw.trim().lowercase()
    return KEYWORD_MAP[lower] ?: raw.trim()
}

// ═══ Trending / Suggested Keywords ═══
val TRENDING_KEYWORDS = listOf(
    "Hành động", "Tình cảm", "Kinh dị", "Hoạt hình",
    "Võ thuật", "Hài hước", "Phiêu lưu", "Ma",
    "Chiến tranh", "Viễn tưởng", "Siêu anh hùng", "Thám tử",
    "Cổ trang", "Anime", "Gia đình", "Lãng mạn"
)

// S-2: Genre Quick Search chips
val GENRE_CHIPS = listOf(
    "hanh-dong" to "🥊 Hành động", "tinh-cam" to "💖 Tình cảm",
    "kinh-di" to "👻 Kinh dị", "hoat-hinh" to "🎠 Hoạt hình",
    "hai-huoc" to "😂 Hài", "vien-tuong" to "🚀 Viễn tưởng",
    "co-trang" to "🏯 Cổ trang", "vo-thuat" to "🥋 Võ thuật",
    "phieu-luu" to "🏔️ Phiêu lưu", "gia-dinh" to "🏠 Gia đình",
)

// S-4: Sort options
enum class SearchSort(val label: String) {
    NEWEST("🕒 Mới nhất"),
    OLDEST("📋 Cũ nhất"),
    AZ("🔤 Tên A-Z")
}
