package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.data.api.models.CineMovie
import xyz.raidenhub.phim.data.repository.ThuVienCineRepository
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/**
 * FshareCategoryScreen — Grid listing of ThuVienCine movies with pagination.
 * Similar to CategoryScreen but for ThuVienCine/Fshare content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FshareCategoryScreen(
    categoryUrl: String,
    title: String,
    onMovieClick: (String) -> Unit,  // enriched slug
    onBack: () -> Unit
) {
    val repo = remember { ThuVienCineRepository() }
    var movies by remember { mutableStateOf<List<CineMovie>>(emptyList()) }
    var currentPage by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    val gridState = rememberLazyGridState()

    // Load a specific page
    suspend fun loadPage(pg: Int) {
        if (isLoading || !hasMore) return
        isLoading = true
        try {
            val result = repo.getMovies(categoryUrl, pg)
            if (result.isEmpty()) {
                hasMore = false
            } else {
                movies = movies + result
                currentPage = pg
                hasMore = result.size >= 10
            }
        } catch (_: Exception) {
            hasMore = false
        }
        isLoading = false
    }

    // Load first page
    LaunchedEffect(categoryUrl) {
        loadPage(1)
    }

    // Infinite scroll trigger
    LaunchedEffect(gridState) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            lastVisible to total
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 6 && !isLoading && hasMore) {
                loadPage(currentPage + 1)
            }
        }
    }

    Scaffold(
        containerColor = C.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        color = C.TextPrimary,
                        fontFamily = JakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )
        }
    ) { padding ->
        if (movies.isEmpty() && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = C.Primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 8.dp, end = 8.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 80.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(movies, key = { it.slug }) { movie ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val enriched = "fshare-folder:${movie.detailUrl}|||${movie.title}|||${movie.thumbnailUrl}"
                                onMovieClick(enriched)
                            }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(C.Surface)
                        ) {
                            AsyncImage(
                                model = movie.thumbnailUrl,
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Quality badge
                            if (movie.quality.isNotBlank()) {
                                Text(
                                    movie.quality,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(C.Primary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            movie.title,
                            color = C.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (movie.year.isNotBlank()) {
                            Text(movie.year, color = C.TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                // Loading indicator at bottom
                if (isLoading && movies.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
