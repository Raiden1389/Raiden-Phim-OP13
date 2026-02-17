package xyz.raidenhub.phim.ui.screens.search

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    // #13 — Search suggestions (live autocomplete from history + trending)
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        updateSuggestions(query)
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

    private fun updateSuggestions(query: String) {
        if (query.length < 2) { _suggestions.value = emptyList(); return }
        val q = query.lowercase()
        val history = SearchHistoryManager.history.value
        val trending = TRENDING_KEYWORDS

        // Combine history + trending, filter by prefix match
        val combined = (history + trending)
            .filter { it.lowercase().contains(q) && it.lowercase() != q }
            .distinct()
            .take(5)
        _suggestions.value = combined
    }
}

// ═══ Trending / Suggested Keywords ═══
private val TRENDING_KEYWORDS = listOf(
    "Hành động", "Tình cảm", "Kinh dị", "Hoạt hình",
    "Võ thuật", "Hài hước", "Phiêu lưu", "Ma",
    "Chiến tranh", "Viễn tưởng", "Siêu anh hùng", "Thám tử",
    "Cổ trang", "Anime", "Gia đình", "Lãng mạn"
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
    val suggestions by vm.suggestions.collectAsState()
    val context = LocalContext.current
    val history by SearchHistoryManager.history.collectAsState()

    // Init history
    LaunchedEffect(Unit) { SearchHistoryManager.init(context) }

    // #10 — Voice search launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                query = spoken
                vm.search(spoken)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(C.Background).padding(top = 8.dp)) {
        // Search bar with voice + clear buttons
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            placeholder = { Text("Tìm phim...", color = C.TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = C.TextSecondary) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // #10 — Voice search button
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên phim...")
                        }
                        try { voiceLauncher.launch(intent) } catch (_: Exception) { }
                    }) {
                        Text("🎤", fontSize = 18.sp)
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; vm.search("") }) {
                            Icon(Icons.Default.Clear, "Clear", tint = C.TextSecondary)
                        }
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

        // #13 — Autocomplete suggestions dropdown
        if (suggestions.isNotEmpty() && query.length >= 2 && results.isEmpty() && !loading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
                    .background(C.Surface)
            ) {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = suggestion; vm.search(suggestion) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, null, tint = C.TextMuted, modifier = Modifier.size(16.dp))
                        Text(suggestion, color = C.TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }

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
