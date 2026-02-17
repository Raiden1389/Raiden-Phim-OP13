package xyz.raidenhub.phim.ui.screens.english

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.api.models.ConsumetItem
import xyz.raidenhub.phim.data.repository.ConsumetRepository
import xyz.raidenhub.phim.ui.theme.C

// #44 — English Search Screen (Consumet FlixHQ)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishSearchScreen(
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ConsumetItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun doSearch(q: String) {
        searchJob?.cancel()
        if (q.length < 2) { results = emptyList(); hasSearched = false; return }
        searchJob = scope.launch {
            delay(400) // debounce
            isLoading = true
            ConsumetRepository.search(q)
                .onSuccess { results = it; hasSearched = true }
                .onFailure { results = emptyList(); hasSearched = true }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(C.Background)
    ) {
        // ═══ Search Bar ═══
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; doSearch(it) },
            placeholder = { Text("Search English movies & TV...", color = C.TextSecondary) },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextSecondary)
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList(); hasSearched = false }) {
                        Icon(Icons.Default.Clear, "Clear", tint = C.TextSecondary)
                    }
                } else {
                    Icon(Icons.Default.Search, "Search", tint = C.TextSecondary)
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = C.Surface,
                unfocusedContainerColor = C.Surface,
                focusedBorderColor = C.Primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = C.Primary,
                focusedTextColor = C.TextPrimary,
                unfocusedTextColor = C.TextPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // ═══ Results ═══
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = C.Primary)
            }
            results.isNotEmpty() -> {
                Text(
                    "${results.size} results for \"$query\"",
                    color = C.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.id }) { movie ->
                        EnglishSearchCard(movie, onMovieClick)
                    }
                }
            }
            hasSearched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No results for \"$query\"", color = C.TextSecondary, fontSize = 15.sp)
                }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍿", fontSize = 64.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Search English Movies & TV", color = C.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Try: Avengers, Breaking Bad, ...", color = C.TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun EnglishSearchCard(movie: ConsumetItem, onClick: (String) -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick(movie.id) }
    ) {
        Box {
            AsyncImage(
                model = movie.image,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(10.dp))
            )
            if (movie.type.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (movie.type == "Movie") C.Primary else Color(0xFF9C27B0),
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                ) {
                    Text(
                        movie.type,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
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
        if (movie.releaseDate.isNotBlank()) {
            Text(movie.releaseDate, color = C.TextSecondary, fontSize = 10.sp)
        }
    }
}
