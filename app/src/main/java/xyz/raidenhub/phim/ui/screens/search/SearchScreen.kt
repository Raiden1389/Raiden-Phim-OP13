package xyz.raidenhub.phim.ui.screens.search

import android.app.Activity
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
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.raidenhub.phim.data.local.SearchHistoryManager
import xyz.raidenhub.phim.ui.components.MovieCard
import xyz.raidenhub.phim.ui.components.EmptyStateView
import xyz.raidenhub.phim.ui.components.ShimmerGrid
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

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

    // S-1: Filter state
    var filterYear by remember { mutableStateOf<Int?>(null) }
    var filterType by remember { mutableStateOf<String?>(null) } // "series"|"single"
    // S-4: Sort
    var sortMode by remember { mutableStateOf(SearchSort.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Apply filter + sort on results
    val displayResults = remember(results, filterYear, filterType, sortMode) {
        var list = results
        filterYear?.let { y -> list = list.filter { it.year == y } }
        filterType?.let { t ->
            list = when (t) {
                "series" -> list.filter {
                    val ep = it.episodeCurrent.lowercase()
                    !ep.contains("full") && !ep.contains("full hd")
                }
                "single" -> list.filter {
                    val ep = it.episodeCurrent.lowercase()
                    ep.contains("full") || ep.isBlank()
                }
                else -> list
            }
        }
        when (sortMode) {
            SearchSort.NEWEST -> list.sortedByDescending { it.year }
            SearchSort.OLDEST -> list.sortedBy { it.year }
            SearchSort.AZ -> list.sortedBy { it.name }
        }
    }

    // Init history
    LaunchedEffect(Unit) { SearchHistoryManager.init(context) }

    // #10 — Voice search launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                // S-3: normalize before search
                val normalized = normalizeKeyword(spoken)
                query = normalized
                vm.search(normalized)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(C.Background).padding(top = 8.dp)) {
        // Search bar with voice + clear buttons
        OutlinedTextField(
            value = query,
            onValueChange = { raw ->
                // S-3: normalize as user types (only for exact matches, else pass through)
                query = raw
                vm.search(raw)
            },
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
                            .clickable {
                                val normalized = normalizeKeyword(suggestion)
                                query = normalized
                                vm.search(normalized)
                            }
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
            // S-2: Genre quick search row
            Text("🎥 Thể loại", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(GENRE_CHIPS) { (_, label) ->
                    Text(
                        label,
                        color = C.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(C.Primary.copy(0.18f))
                            .clickable {
                                // Extract display name (after emoji)
                                val displayName = label.trim().substring(label.trim().indexOfFirst { it == ' ' } + 1)
                                query = displayName
                                vm.search(displayName)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // #9 — Search history
            if (history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕐 Tìm gần đây", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            Text("🔥 Xu hướng", color = C.TextPrimary, fontFamily = JakartaFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold,
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
        } else if (loading && results.isEmpty()) {
            ShimmerGrid(rows = 3)
        } else if (displayResults.isEmpty() && !loading) {
            EmptyStateView(
                emoji = "🔍",
                title = "Không tìm thấy phim nào",
                subtitle = "Thử tìm với từ khoá khác hoặc kiểm tra lại chính tả"
            )
        } else {
            // Save search when results appear
            LaunchedEffect(results) {
                if (query.length >= 2 && results.isNotEmpty()) {
                    SearchHistoryManager.add(query, context)
                }
            }

            // S-1: Filter chips + S-4 Sort
            val availableYears = remember(results) {
                results.map { it.year }.filter { it > 2000 }.distinct().sortedDescending().take(6)
            }

            Column {
                // Count + Sort row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // #12 — Result count
                    Text(
                        "📋 ${displayResults.size} kết quả cho \"$query\"",
                        color = C.TextSecondary,
                        fontSize = 13.sp
                    )
                    // S-4: Sort dropdown
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Text(sortMode.label, color = C.Primary, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = C.Surface
                        ) {
                            SearchSort.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label, color = C.TextPrimary, fontSize = 13.sp) },
                                    onClick = { sortMode = mode; showSortMenu = false }
                                )
                            }
                        }
                    }
                }

                // S-1: Filter chips row (Type + Year)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    item {
                        val selected = filterType == null && filterYear == null
                        FilterChip(
                            selected = selected,
                            onClick = { filterType = null; filterYear = null },
                            label = { Text("Tất cả", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary.copy(0.25f),
                                selectedLabelColor = C.Primary
                            )
                        )
                    }
                    item {
                        val selected = filterType == "series"
                        FilterChip(
                            selected = selected,
                            onClick = { filterType = if (selected) null else "series" },
                            label = { Text("📺 Phim bộ", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary.copy(0.25f),
                                selectedLabelColor = C.Primary
                            )
                        )
                    }
                    item {
                        val selected = filterType == "single"
                        FilterChip(
                            selected = selected,
                            onClick = { filterType = if (selected) null else "single" },
                            label = { Text("🎬 Phim lẻ", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary.copy(0.25f),
                                selectedLabelColor = C.Primary
                            )
                        )
                    }
                    // Year chips
                    items(availableYears) { year ->
                        val selected = filterYear == year
                        FilterChip(
                            selected = selected,
                            onClick = { filterYear = if (selected) null else year },
                            label = { Text("$year", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary.copy(0.25f),
                                selectedLabelColor = C.Primary
                            )
                        )
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    color = C.Primary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp, 4.dp, 8.dp, 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayResults, key = { it.slug }) { movie ->
                    MovieCard(movie = movie, onClick = { onMovieClick(movie.slug) })
                }
            }
        }
    }
}
