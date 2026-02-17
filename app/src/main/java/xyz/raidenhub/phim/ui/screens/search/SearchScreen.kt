package xyz.raidenhub.phim.ui.screens.search

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.ui.components.MovieCard
import xyz.raidenhub.phim.ui.theme.C

// ═══ Search History Manager ═══
object SearchHistoryManager {
    private const val PREF_NAME = "search_history"
    private const val KEY = "recent"
    private const val MAX_ITEMS = 15

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history = _history.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _history.value = prefs.getString(KEY, null)?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun add(query: String, context: Context) {
        if (query.isBlank() || query.length < 2) return
        val current = _history.value.toMutableList()
        current.remove(query)
        current.add(0, query.trim())
        val trimmed = current.take(MAX_ITEMS)
        _history.value = trimmed
        save(context, trimmed)
    }

    fun remove(query: String, context: Context) {
        val current = _history.value.toMutableList()
        current.remove(query)
        _history.value = current
        save(context, current)
    }

    fun clearAll(context: Context) {
        _history.value = emptyList()
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun save(context: Context, items: List<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY, items.joinToString("|||")).apply()
    }
}

class SearchViewModel : ViewModel() {
    private val _results = MutableStateFlow<List<Movie>>(emptyList())
    val results = _results.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        if (query.length < 2) { _results.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _loading.value = true
            MovieRepository.search(query)
                .onSuccess { _results.value = it }
                .onFailure { _results.value = emptyList() }
            _loading.value = false
        }
    }
}

// ═══ Trending / Suggested Keywords ═══
private val TRENDING_KEYWORDS = listOf(
    "Hành động", "Tình cảm", "Kinh dị", "Hoạt hình",
    "Võ thuật", "Hài hước", "Phiêu lưu", "Ma",
    "Chiến tranh", "Viễn tưởng"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()
    val loading by vm.loading.collectAsState()
    val context = LocalContext.current
    val history by SearchHistoryManager.history.collectAsState()

    // Init history
    LaunchedEffect(Unit) { SearchHistoryManager.init(context) }

    Column(Modifier.fillMaxSize().background(C.Background).padding(top = 8.dp)) {
        // Search bar with clear button
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            placeholder = { Text("Tìm phim...", color = C.TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = C.TextSecondary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; vm.search("") }) {
                        Icon(Icons.Default.Clear, "Clear", tint = C.TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = C.TextPrimary,
                unfocusedTextColor = C.TextPrimary,
                focusedBorderColor = C.Primary,
                unfocusedBorderColor = C.SurfaceVariant,
                cursorColor = C.Primary,
                focusedContainerColor = C.Surface,
                unfocusedContainerColor = C.Surface
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        if (query.length < 2) {
            // #9 — Search history
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕐 Tìm gần đây", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Xoá tất cả",
                        color = C.Primary,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { SearchHistoryManager.clearAll(context) }
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { term ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(C.Surface)
                                .clickable { query = term; vm.search(term) }
                                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(term, color = C.TextPrimary, fontSize = 13.sp)
                            IconButton(
                                onClick = { SearchHistoryManager.remove(term, context) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, "Remove", tint = C.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // #11 — Trending searches
            Spacer(Modifier.height(16.dp))
            Text("🔥 Xu hướng", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TRENDING_KEYWORDS.forEach { keyword ->
                    Text(
                        keyword,
                        color = C.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C.Surface)
                            .clickable { query = keyword; vm.search(keyword) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        } else if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(32.dp))
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🔍 Không tìm thấy phim nào", color = C.TextSecondary, fontSize = 16.sp)
            }
        } else {
            // Save search when results appear
            LaunchedEffect(results) {
                if (query.length >= 2 && results.isNotEmpty()) {
                    SearchHistoryManager.add(query, context)
                }
            }

            // #12 — Result count
            Text(
                "📋 ${results.size} kết quả cho \"$query\"",
                color = C.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp, 4.dp, 8.dp, 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.slug }) { movie ->
                    MovieCard(movie = movie, onClick = { onMovieClick(movie.slug) })
                }
            }
        }
    }
}
