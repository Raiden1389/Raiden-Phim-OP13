package xyz.raidenhub.phim.ui.screens.superstream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.raidenhub.phim.data.api.models.TmdbSearchItem
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.ui.theme.C

/**
 * SuperStream Browse/Search Screen.
 * Main entry point for English content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperStreamScreen(
    onItemClick: (tmdbId: Int, type: String) -> Unit,
    onBack: () -> Unit,
    vm: SuperStreamViewModel = viewModel()
) {
    val trendingMovies by vm.trendingMovies.collectAsState()
    val trendingTv by vm.trendingTv.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val isSearching by vm.isSearching.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    // Favorites — filter WatchlistManager for SuperStream items (slug starts with "ss_")
    val watchlistItems by WatchlistManager.items.collectAsState(initial = emptyList())
    val favorites = remember(watchlistItems) {
        watchlistItems.filter { it.source == "superstream" }
    }

    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SuperStream",
                        color = C.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = C.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = C.Background
                )
            )
        },
        containerColor = C.Background
    ) { padding ->

        if (searchText.isNotBlank() && (searchResults.isNotEmpty() || isSearching)) {
            // Search results grid
            Column(modifier = Modifier.padding(padding)) {
                // Search bar pinned at top
                SearchBarField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        if (it.length >= 2) vm.search(it) else vm.clearSearch()
                    },
                    onClear = { searchText = ""; vm.clearSearch() },
                    onSearch = { vm.search(searchText); focusManager.clearFocus() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SearchResultsGrid(
                    results = searchResults,
                    isLoading = isSearching,
                    onItemClick = onItemClick
                )
            }
        } else {
            // Browse mode — trending content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Search bar
                item {
                    SearchBarField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            if (it.length >= 2) vm.search(it) else vm.clearSearch()
                        },
                        onClear = { searchText = ""; vm.clearSearch() },
                        onSearch = { vm.search(searchText); focusManager.clearFocus() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ⭐ Favorites
                if (favorites.isNotEmpty()) {
                    item {
                        SuperStreamRow(
                            title = "⭐ Favorites",
                            items = favorites.mapNotNull { fav ->
                                // Convert WatchlistItem back to TmdbSearchItem for display
                                val parts = fav.slug.removePrefix("ss_").split("_", limit = 2)
                                if (parts.size == 2) {
                                    val type = parts[0]
                                    val id = parts[1].toIntOrNull() ?: return@mapNotNull null
                                    TmdbSearchItem(
                                        id = id,
                                        title = if (type == "movie") fav.name else null,
                                        name = if (type == "tv") fav.name else null,
                                        posterPath = fav.thumbUrl.removePrefix("https://image.tmdb.org/t/p/w500"),
                                        backdropPath = null,
                                        mediaType = type,
                                        voteAverage = 0.0,
                                        releaseDate = null,
                                        firstAirDate = null,
                                        overview = ""
                                    )
                                } else null
                            },
                            onItemClick = { onItemClick(it.id, it.type) }
                        )
                    }
                }

                // 🎬 Trending Movies
                if (trendingMovies.isNotEmpty()) {
                    item {
                        SuperStreamRow(
                            title = "🎬 Trending Movies",
                            items = trendingMovies,
                            onItemClick = { onItemClick(it.id, it.type) }
                        )
                    }
                }

                // 📺 Trending TV Shows
                if (trendingTv.isNotEmpty()) {
                    item {
                        SuperStreamRow(
                            title = "📺 Trending TV Shows",
                            items = trendingTv,
                            onItemClick = { onItemClick(it.id, it.type) }
                        )
                    }
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = C.Accent)
                        }
                    }
                }
            }
        }
    }
}

// ═══ Search Bar Component ═══

@Composable
private fun SearchBarField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text("Search movies, TV shows...", color = C.TextMuted, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(Icons.Default.Search, null, tint = C.TextMuted)
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, null, tint = C.TextMuted)
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = C.TextPrimary,
            unfocusedTextColor = C.TextPrimary,
            cursorColor = C.Accent,
            focusedBorderColor = C.Accent,
            unfocusedBorderColor = C.SurfaceVariant,
            focusedContainerColor = C.Surface,
            unfocusedContainerColor = C.Surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

// ═══ Search Results Grid ═══

@Composable
private fun SearchResultsGrid(
    results: List<TmdbSearchItem>,
    isLoading: Boolean,
    onItemClick: (tmdbId: Int, type: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = C.Accent)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results, key = { "${it.id}_${it.type}" }) { item ->
            SuperStreamCard(
                item = item,
                onClick = { onItemClick(item.id, item.type) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
