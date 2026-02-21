package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import xyz.raidenhub.phim.data.api.models.EpisodeServer
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.api.models.MovieDetail
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils
import xyz.raidenhub.phim.util.TextUtils

class DetailViewModel : ViewModel() {
    private val _state = MutableStateFlow<DetailState>(DetailState.Loading)
    val state = _state.asStateFlow()

    fun load(slug: String) {
        viewModelScope.launch {
            _state.value = DetailState.Loading
            MovieRepository.getMovieDetail(slug)
                .onSuccess { _state.value = DetailState.Success(it.movie, it.episodes) }
                .onFailure { _state.value = DetailState.Error(it.message ?: "Error") }
        }
    }
}

sealed class DetailState {
    data object Loading : DetailState()
    data class Success(val movie: MovieDetail, val episodes: List<EpisodeServer>) : DetailState()
    data class Error(val message: String) : DetailState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    slug: String,
    onBack: () -> Unit,
    onPlay: (slug: String, server: Int, episode: Int) -> Unit,
    onSeasonClick: (slug: String) -> Unit = {},
    onMovieClick: (slug: String) -> Unit = {},
    vm: DetailViewModel = viewModel()
) {
    LaunchedEffect(slug) { vm.load(slug) }
    val state by vm.state.collectAsState()
    val favorites by FavoriteManager.favorites.collectAsState()
    val watchedMap by WatchHistoryManager.watchedEps.collectAsState()
    val continueList by WatchHistoryManager.continueList.collectAsState()

    when (val s = state) {
        is DetailState.Loading -> Box(Modifier.fillMaxSize().background(C.Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = C.Primary)
        }
        is DetailState.Error -> Box(Modifier.fillMaxSize().background(C.Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😕 ${s.message}", color = C.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("← Quay lại", color = C.Primary, modifier = Modifier.clickable(onClick = onBack))
            }
        }
        is DetailState.Success -> {
            val movie = s.movie
            val episodes = s.episodes
            var selectedServer by remember { mutableIntStateOf(0) }
            val isFav = favorites.any { it.slug == slug }
            val watchedSet = watchedMap[slug] ?: emptySet()
            // C-4: Watchlist state
            val watchlistItems by WatchlistManager.items.collectAsState()
            val isWatchlisted = watchlistItems.any { it.slug == slug }
            // C-5: Playlist sheet
            val playlists by PlaylistManager.playlists.collectAsState()
            var showPlaylistSheet by remember { mutableStateOf(false) }

            // #20 — Detect continue watching position
            val continueItem = continueList.find { it.slug == slug }
            val continueEp = continueItem?.episode ?: 0
            val hasContinue = continueItem != null
            // D-8: Episode sort
            var episodeSortAsc by remember { mutableStateOf(true) }

            // #17 — Fetch IMDb rating from OMDB API
            var imdbRating by remember { mutableStateOf<String?>(null) }
            // D-3 — Fetch TMDB rating
            var tmdbRating by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(movie.originName, movie.name) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val title = movie.originName.ifBlank { movie.name }
                        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                        val year = if (movie.year > 0) "&y=${movie.year}" else ""
                        // IMDb via OMDB
                        val omdbUrl = "https://www.omdbapi.com/?apikey=2692d710&t=$encodedTitle$year"
                        val client = OkHttpClient()
                        val omdbResp = client.newCall(Request.Builder().url(omdbUrl).build()).execute()
                        val omdbJson = JSONObject(omdbResp.body?.string() ?: "")
                        if (omdbJson.optString("Response") == "True") {
                            val rating = omdbJson.optString("imdbRating", "N/A")
                            if (rating != "N/A") imdbRating = rating
                        }
                        // D-3: TMDB search → vote_average
                        val tmdbSearchUrl = "https://api.themoviedb.org/3/search/multi?api_key=4a6aef11e0f5ff2aff6f3f2e6b4af3c3&query=$encodedTitle&language=vi-VN"
                        val tmdbResp = client.newCall(Request.Builder().url(tmdbSearchUrl).build()).execute()
                        val tmdbJson = JSONObject(tmdbResp.body?.string() ?: "")
                        val tmdbResults = tmdbJson.optJSONArray("results")
                        if (tmdbResults != null && tmdbResults.length() > 0) {
                            val firstResult = tmdbResults.getJSONObject(0)
                            val voteAvg = firstResult.optDouble("vote_average", 0.0)
                            if (voteAvg > 0.0) {
                                tmdbRating = String.format("%.1f", voteAvg)
                            }
                        }
                    } catch (_: Exception) { }
                }
            }

            // #40 — Season Grouping state (hoisted above LazyColumn)
            val seasonRegex = remember { Regex("""[(\[（]?\s*(?:Phần|Season|Mùa|Part|SS)\s*(\d+)\s*[)\]）]?""", RegexOption.IGNORE_CASE) }
            val baseName = remember(movie.name) {
                movie.name.replace(seasonRegex, "").replace(Regex("""\s*\d+$"""), "").trim()
            }
            val currentSeason = remember(movie.name) {
                seasonRegex.find(movie.name)?.groupValues?.get(1)?.toIntOrNull()
            }
            var relatedSeasons by remember { mutableStateOf<List<Movie>>(emptyList()) }
            if (currentSeason != null && baseName.length >= 3) {
                LaunchedEffect(baseName) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        MovieRepository.search(baseName)
                            .onSuccess { results ->
                                relatedSeasons = results.filter { m ->
                                    val mBase = m.name.replace(seasonRegex, "").replace(Regex("""\s*\d+$"""), "").trim()
                                    mBase.equals(baseName, ignoreCase = true) && m.slug != slug
                                }.sortedBy {
                                    seasonRegex.find(it.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                                }
                            }
                    }
                }
            }

            // D-5: Related movies — fetch by first genre
            var relatedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
            val firstGenre = movie.category.firstOrNull()?.slug ?: ""
            LaunchedEffect(slug, firstGenre) {
                if (firstGenre.isNotBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        MovieRepository.search(firstGenre)
                            .onSuccess { results ->
                                relatedMovies = results
                                    .filter { it.slug != slug }
                                    .take(12)
                            }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().background(C.Background)
            ) {
                // Backdrop
                item {
                    Box(Modifier.fillMaxWidth().height(280.dp)) {
                        AsyncImage(
                            model = ImageUtils.detailImage(movie.posterUrl.ifBlank { movie.thumbUrl }),
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                            listOf(C.HeroGradientTop, C.HeroGradientBottom), startY = 80f
                        )))
                        // Back button
                        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = C.TextPrimary)
                        }
                        // Title area
                        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                            Text(movie.name, color = C.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            if (movie.originName.isNotBlank())
                                Text(movie.originName, color = C.TextSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (movie.quality.isNotBlank()) Badge3(movie.quality, C.Primary)
                                if (movie.lang.isNotBlank()) Badge3(TextUtils.shortLang(movie.lang), C.Badge)
                                if (movie.episodeCurrent.isNotBlank()) Badge3(movie.episodeCurrent, C.SurfaceVariant)
                            }
                        }
                    }
                }

                // #20 — Play / Continue + Favorite buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasContinue) onPlay(slug, selectedServer, continueEp)
                                else onPlay(slug, selectedServer, 0)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", tint = C.TextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (hasContinue) "Tiếp tục Tập ${continueItem?.epName ?: (continueEp + 1)}"
                                else "Xem Phim",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Favorite toggle
                        IconButton(
                            onClick = { FavoriteManager.toggle(slug, movie.name, movie.thumbUrl) },
                            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                "Favorite",
                                tint = if (isFav) C.Primary else C.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        // C-4: Watchlist toggle
                        IconButton(
                            onClick = { WatchlistManager.toggle(slug, movie.name, movie.thumbUrl) },
                            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
                        ) {
                            Text(if (isWatchlisted) "🔖" else "🔇", fontSize = 20.sp)
                        }
                        // C-5: Add to playlist
                        IconButton(
                            onClick = { showPlaylistSheet = true },
                            modifier = Modifier.size(48.dp).background(C.Surface, RoundedCornerShape(12.dp))
                        ) {
                            Text("📋", fontSize = 20.sp)
                        }
                    }
                }

                // Info grid
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        val infos = buildList {
                            if (imdbRating != null) add("⭐ IMDb $imdbRating/10")
                            if (tmdbRating != null) add("🍅 TMDB $tmdbRating/10")
                            if (movie.year > 0) add("📅 ${movie.year}")
                            if (movie.country.isNotEmpty()) add("🌍 ${movie.country.joinToString { it.name }}")
                            if (movie.time.isNotBlank() && !movie.time.contains("?")) add("⏱ ${movie.time}")
                            if (movie.episodeTotal.isNotBlank() && !movie.episodeTotal.contains("?")) {
                                // Tránh "? Tập tập" — episodeTotal đã có "Tập" thì không thêm nữa
                                val epText = movie.episodeTotal
                                val label = if (epText.contains("tập", ignoreCase = true)) epText else "$epText tập"
                                add("📺 $label")
                            }
                        }
                        if (infos.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                infos.forEach { Text(it, color = C.TextSecondary, fontSize = 13.sp) }
                            }
                        }

                        // Genres
                        if (movie.category.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                movie.category.forEach {
                                    Text(it.name, color = C.Accent, fontSize = 12.sp,
                                        modifier = Modifier.background(C.SurfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // #19 — Director
                        if (movie.director.isNotEmpty() && movie.director.any { it.isNotBlank() }) {
                            Text(
                                "🎬 Đạo diễn: ${movie.director.filter { it.isNotBlank() }.joinToString(", ")}",
                                color = C.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // D-6: Cast grid — scrollable horizontal row of actor chips
                        if (movie.actor.isNotEmpty() && movie.actor.any { it.isNotBlank() }) {
                            val actors = movie.actor.filter { it.isNotBlank() }
                            Text(
                                "🎭 Diễn viên:",
                                color = C.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                items(actors.take(12)) { actor ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(70.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(C.SurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) { Text("👤", fontSize = 22.sp) }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            actor.split(" ").lastOrNull() ?: actor,
                                            color = C.TextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // #14 / D-7 — Description (expandable with gradient fade)
                        if (movie.content.isNotBlank()) {
                            val cleaned = movie.content.replace(Regex("<[^>]*>"), "").trim()
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    cleaned,
                                    color = C.TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                // Gradient fade overlay when collapsed
                                if (!expanded) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Transparent,
                                                        C.Background.copy(alpha = 0.85f),
                                                        C.Background
                                                    ),
                                                    startY = 30f
                                                )
                                            )
                                    )
                                }
                            }
                            Text(
                                if (expanded) "Thu gọn ▲" else "Xem thêm ▼",
                                color = C.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { expanded = !expanded }
                                    .padding(bottom = 12.dp)
                            )
                        }
                    }
                }

                // Server tabs
                if (episodes.size > 1) {
                    item {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            episodes.forEachIndexed { idx, server ->
                                val isActive = idx == selectedServer
                                Text(
                                    text = server.serverName.ifBlank { "Server ${idx + 1}" },
                                    color = if (isActive) C.TextPrimary else C.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) C.Primary else C.Surface)
                                        .clickable { selectedServer = idx }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // #40 — Season Grouping (state is hoisted above LazyColumn)
                if (relatedSeasons.isNotEmpty()) {
                    item {
                        Text(
                            "📺 Các phần khác (${relatedSeasons.size + 1} phần)",
                            color = C.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Current season (highlighted)
                            item {
                                Text(
                                    "Phần $currentSeason ★",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(C.Primary, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            // Other seasons
                            items(relatedSeasons) { season ->
                                val sNum = seasonRegex.find(season.name)?.groupValues?.get(1) ?: "?"
                                Text(
                                    "Phần $sNum",
                                    color = C.TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(C.Surface)
                                        .clickable { onSeasonClick(season.slug) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // #21 — Episode grid with progress bars + D-8 sort toggle
                if (episodes.isNotEmpty()) {
                    val eps = episodes.getOrNull(selectedServer)?.serverData.orEmpty()
                    val displayEps = if (episodeSortAsc) eps else eps.reversed()
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Danh sách tập", color = C.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            // D-8: Sort toggle
                            if (eps.size > 1) {
                                Text(
                                    if (episodeSortAsc) "↓ Mới nhất" else "↑ Tập 1",
                                    color = C.Primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(C.Primary.copy(0.15f))
                                        .clickable { episodeSortAsc = !episodeSortAsc }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)) {
                            displayEps.chunked(5).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEachIndexed { _, ep ->
                                        val epIdx = eps.indexOf(ep)
                                        val isWatched = watchedSet.contains(epIdx)
                                        val epLabel = ep.name.ifBlank { "Tập ${epIdx + 1}" }
                                        val epProgress = if (continueItem?.episode == epIdx) continueItem.progress else if (isWatched) 1f else 0f

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isWatched) "✓ $epLabel" else epLabel,
                                                color = if (isWatched) C.Primary else C.TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (isWatched) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                    .background(if (isWatched) C.Primary.copy(0.15f) else C.Surface)
                                                    .clickable { onPlay(slug, selectedServer, epIdx) }
                                                    .padding(vertical = 10.dp)
                                                    .wrapContentWidth(Alignment.CenterHorizontally)
                                            )
                                            if (epProgress > 0f && epProgress < 1f) {
                                                LinearProgressIndicator(
                                                    progress = { epProgress },
                                                    modifier = Modifier.fillMaxWidth().height(3.dp),
                                                    color = C.Primary,
                                                    trackColor = C.Surface
                                                )
                                            } else if (isWatched) {
                                                Box(Modifier.fillMaxWidth().height(3.dp).background(C.Primary))
                                            } else {
                                                Spacer(Modifier.fillMaxWidth().height(3.dp))
                                            }
                                        }
                                    }
                                    repeat(5 - row.size) {
                                        Column(Modifier.weight(1f)) {
                                            Spacer(Modifier.fillMaxWidth().height(43.dp))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // D-5: Related movies (state hoisted to composable scope above LazyColumn — see state vars)
                if (relatedMovies.isNotEmpty()) {
                    item {
                        Text(
                            "🎞️ Có thể bạn thích",
                            color = C.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 80.dp)
                        ) {
                            items(relatedMovies, key = { it.slug }) { related ->
                                Column(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onMovieClick(related.slug) }
                                ) {
                                    AsyncImage(
                                        model = ImageUtils.cardImage(related.thumbUrl, related.source),
                                        contentDescription = related.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(2f / 3f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(C.Surface)
                                    )
                                    Text(
                                        related.name,
                                        color = C.TextPrimary,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // C-5: Playlist bottom sheet
            if (showPlaylistSheet) {
                AlertDialog(
                    onDismissRequest = { showPlaylistSheet = false },
                    title = { Text("📋 Thêm vào Playlist", color = C.TextPrimary) },
                    text = {
                        if (playlists.isEmpty()) {
                            Text("Chưa có playlist nào. Tạo playlist trong Mục Playlist.", color = C.TextSecondary)
                        } else {
                            Column {
                                playlists.forEach { pl ->
                                    val inList = PlaylistManager.isInPlaylist(pl.id, slug)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                if (inList) PlaylistManager.removeFromPlaylist(pl.id, slug)
                                                else PlaylistManager.addToPlaylist(pl.id, slug, movie.name, movie.thumbUrl)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(pl.name, color = C.TextPrimary, fontSize = 15.sp)
                                        Text(if (inList) "✓" else "+", color = if (inList) C.Primary else C.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Divider(color = C.SurfaceVariant, thickness = 0.5.dp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlaylistSheet = false }) {
                            Text("Xong", color = C.Primary, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = C.Surface
                )
            }
        } // end DetailState.Success
    } // end when
} // end DetailScreen

@Composable
private fun Badge3(text: String, color: Color) {
    Text(text, color = C.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.background(color.copy(0.85f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

private val C.Badge get() = Color(0xFF2196F3)

