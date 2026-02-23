package xyz.raidenhub.phim.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.ApiClient
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.ui.components.MovieCard
import xyz.raidenhub.phim.ui.components.ShimmerGrid
import xyz.raidenhub.phim.ui.theme.C

class CategoryViewModel : ViewModel() {
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies = _movies.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()
    private var currentPage = 1
    private var totalPages = 1
    private var currentSlug = ""
    private var currentCountry = "" // country filter
    private var currentYear = 0     // year filter (0 = all)

    fun load(slug: String, country: String = "", year: Int = 0) {
        if (slug == currentSlug && country == currentCountry && year == currentYear && _movies.value.isNotEmpty()) return
        currentSlug = slug
        currentCountry = country
        currentYear = year
        currentPage = 1
        _movies.value = emptyList()
        fetchPage(slug, 1, replace = true)
    }

    fun loadMore() {
        if (_loadingMore.value || currentPage >= totalPages) return
        fetchPage(currentSlug, currentPage + 1, replace = false)
    }

    fun hasMore() = currentPage < totalPages

    private val kkphimSlugs = setOf("tv-shows")

    private fun fetchPage(slug: String, page: Int, replace: Boolean) {
        viewModelScope.launch {
            if (replace) _loading.value = true else _loadingMore.value = true
            try {
                val isKK = slug in kkphimSlugs
                val response = when (slug) {
                    "phim-moi-cap-nhat" -> ApiClient.ophim.getNewMovies(page)
                    "phim-bo" -> ApiClient.ophim.getSeries(page)
                    "phim-le" -> ApiClient.ophim.getSingleMovies(page)
                    "hoat-hinh" -> ApiClient.ophim.getAnime(page)
                    "tv-shows" -> ApiClient.kkphim.getTvShows(page)
                    "han-quoc" -> ApiClient.ophim.getKorean(page)
                    "trung-quoc" -> ApiClient.ophim.getChinese(page)
                    "au-my" -> ApiClient.ophim.getWestern(page)
                    else -> ApiClient.ophim.getNewMovies(page)
                }
                var newMovies = response.data?.items.orEmpty()
                    .filter { !it.episodeCurrent.equals("Trailer", ignoreCase = true) }

                // Tag source for KKPhim
                if (isKK) {
                    newMovies = newMovies.map { it.copy(source = "kkphim") }
                }

                // Apply country filter if set
                if (currentCountry.isNotBlank()) {
                    newMovies = newMovies.filter { movie ->
                        movie.country.any { it.slug == currentCountry }
                    }
                }
                // C-1: Apply year filter if set
                if (currentYear > 0) {
                    newMovies = newMovies.filter { it.year == currentYear }
                }

                // Use totalPages directly from API — already computed server-side
                val pagination = response.data?.params?.pagination
                totalPages = when {
                    pagination?.totalPages != null && pagination.totalPages > 0 -> pagination.totalPages
                    pagination?.totalItems != null && pagination.totalItems > 0 -> {
                        // Fallback: calculate if totalPages absent
                        val perPage = pagination.totalItemsPerPage.takeIf { it > 0 } ?: 24
                        (pagination.totalItems + perPage - 1) / perPage
                    }
                    else -> if (newMovies.isNotEmpty()) currentPage + 1 else currentPage
                }
                currentPage = page

                if (replace) {
                    _movies.value = newMovies
                } else {
                    _movies.value = _movies.value + newMovies
                }
            } catch (_: Exception) {
                if (replace) _movies.value = emptyList()
            }
            _loading.value = false
            _loadingMore.value = false
        }
    }
}

data class CountryFilter(val name: String, val slug: String)

private val COUNTRY_FILTERS = listOf(
    CountryFilter("Tất cả", ""),
    CountryFilter("🇰🇷 Hàn Quốc", "han-quoc"),
    CountryFilter("🇨🇳 Trung Quốc", "trung-quoc"),
    CountryFilter("🇺🇸 Âu Mỹ", "au-my"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    slug: String,
    title: String,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    vm: CategoryViewModel = viewModel()
) {
    var selectedCountry by remember { mutableStateOf("") }
    var selectedYear by remember { mutableIntStateOf(0) }

    LaunchedEffect(slug, selectedCountry, selectedYear) { vm.load(slug, selectedCountry, selectedYear) }
    val movies by vm.movies.collectAsState()
    val loading by vm.loading.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()

    val gridState = rememberLazyGridState()

    // Infinite scroll: load more when near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6 // Load when 6 items from bottom
        }
    }

    // Key on loadingMore too — so after each page load, re-check if we need more
    LaunchedEffect(shouldLoadMore, loadingMore) {
        if (shouldLoadMore && !loading && !loadingMore && vm.hasMore()) {
            vm.loadMore()
        }
    }

    Column(Modifier.fillMaxSize().background(C.Background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
            }
            Text(
                java.net.URLDecoder.decode(title, "UTF-8"),
                color = C.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }

        // 🌍 Country filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            COUNTRY_FILTERS.forEach { filter ->
                val isActive = selectedCountry == filter.slug
                Text(
                    text = filter.name,
                    color = if (isActive) C.TextPrimary else C.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) C.Primary else C.Surface)
                        .clickable { selectedCountry = filter.slug }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // C-1: Year filter chips
        val currentYear2 = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val yearOptions = listOf(0) + (currentYear2 downTo currentYear2 - 7).toList()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            yearOptions.forEach { year ->
                val isActive = selectedYear == year
                Text(
                    text = if (year == 0) "Tất cả" else year.toString(),
                    color = if (isActive) C.TextPrimary else C.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) C.Primary.copy(0.8f) else C.Surface)
                        .clickable { selectedYear = year }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        if (loading && movies.isEmpty()) {
            ShimmerGrid()
        } else if (movies.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Không có phim nào", color = C.TextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp, 0.dp, 8.dp, 16.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(movies, key = { it.slug }) { movie ->
                    MovieCard(movie = movie, onClick = { onMovieClick(movie.slug) })
                }

                // Loading more indicator
                if (loadingMore) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = C.Primary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
