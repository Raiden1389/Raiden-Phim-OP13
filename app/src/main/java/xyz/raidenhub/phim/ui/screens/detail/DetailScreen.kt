package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import xyz.raidenhub.phim.data.api.models.Movie
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.data.local.PlaylistManager
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.data.repository.MovieRepository
import xyz.raidenhub.phim.ui.components.ShimmerDetailScreen
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.util.ImageUtils

/**
 * DetailScreen — orchestrator composable.
 *
 * All visual sections are extracted into dedicated files:
 *   DetailBackdrop, DetailActionRow, DetailInfoSection,
 *   DetailEpisodeGrid, DetailSeasonRow, DetailRelatedRow,
 *   DetailPlaylistDialog, DetailAnimations
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val favorites by FavoriteManager.favorites.collectAsState(initial = emptyList())
    val watchedEpIndices by WatchHistoryManager.getWatchedEpisodes(slug).collectAsState(initial = emptyList())
    val continueList by WatchHistoryManager.continueList.collectAsState(initial = emptyList())

    when (val s = state) {
        is DetailState.Loading -> ShimmerDetailScreen(
            thumbUrl = PendingDetailState.thumbUrl,
            title = PendingDetailState.title
        )
        is DetailState.Error -> Box(Modifier.fillMaxSize().background(C.Background), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("😕 ${s.message}", color = C.TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("← Quay lại", color = C.Primary, modifier = Modifier.clickable(onClick = onBack))
            }
        }
        is DetailState.Success -> {
            PendingDetailState.clear()
            // B-3: Entrance animation
            val enterAlpha = remember { Animatable(0f) }
            val enterScale = remember { Animatable(0.95f) }
            LaunchedEffect(Unit) {
                launch { enterAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
                launch { enterScale.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
            }

            val movie = s.movie
            val episodes = s.episodes
            var selectedServer by remember { mutableIntStateOf(0) }

            // A-8: Dynamic accent color
            val posterUrl = movie.posterUrl.ifBlank { movie.thumbUrl }
            val rawDominant = rememberDominantColor(ImageUtils.detailImage(posterUrl))
            val accentColor by animateColorAsState(rawDominant, tween(600), label = "accent_color")

            val isFav = favorites.any { it.slug == slug }
            val watchedSet = watchedEpIndices.toSet()

            // C-4: Watchlist
            val watchlistItems by WatchlistManager.items.collectAsState(initial = emptyList())
            val isWatchlisted = watchlistItems.any { it.slug == slug }

            // C-5: Playlist
            val playlists by PlaylistManager.playlists.collectAsState(initial = emptyList())
            var showPlaylistSheet by remember { mutableStateOf(false) }

            // Continue watching
            val continueItem = continueList.find { it.slug == slug }
            val continueEp = continueItem?.episode ?: 0
            val hasContinue = continueItem != null

            // D-8: Episode sort
            var episodeSortAsc by remember { mutableStateOf(true) }

            // #17 / D-3 — External ratings + cast photos
            var imdbRating by remember { mutableStateOf<String?>(null) }
            var tmdbRating by remember { mutableStateOf<String?>(null) }
            var actorPhotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            LaunchedEffect(movie.originName, movie.name) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val title = movie.originName.ifBlank { movie.name }
                        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                        val year = if (movie.year > 0) "&y=${movie.year}" else ""
                        val client = OkHttpClient()
                        // IMDb via OMDB
                        val omdbUrl = "https://www.omdbapi.com/?apikey=2692d710&t=$encodedTitle$year"
                        val omdbResp = client.newCall(Request.Builder().url(omdbUrl).build()).execute()
                        val omdbJson = JSONObject(omdbResp.body?.string() ?: "")
                        if (omdbJson.optString("Response") == "True") {
                            val rating = omdbJson.optString("imdbRating", "N/A")
                            if (rating != "N/A") imdbRating = rating
                        }
                        // TMDB
                        val tmdbToken = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3NTg5MDVlZjk4MGM3YjE3YWJhYjU0NDFlODAzMzkxNCIsIm5iZiI6MTc3MTM5MDkwMS40NzksInN1YiI6IjY5OTU0N2I1MjZlZTNlMWFlM2ZhNDBhNyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.-3O6afnx0tBl0Ybkf7Lvd4m0N2NUSSZMmzM43nhHZB0"
                        val tmdbSearchUrl = "https://api.themoviedb.org/3/search/multi?query=$encodedTitle&language=vi-VN"
                        val tmdbResp = client.newCall(Request.Builder().url(tmdbSearchUrl).header("Authorization", "Bearer $tmdbToken").build()).execute()
                        val tmdbJson = JSONObject(tmdbResp.body?.string() ?: "")
                        val tmdbResults = tmdbJson.optJSONArray("results")
                        if (tmdbResults != null && tmdbResults.length() > 0) {
                            val firstResult = tmdbResults.getJSONObject(0)
                            val voteAvg = firstResult.optDouble("vote_average", 0.0)
                            if (voteAvg > 0.0) tmdbRating = String.format("%.1f", voteAvg)
                            val tmdbId = firstResult.optInt("id", 0)
                            val mediaType = firstResult.optString("media_type", "movie")
                            if (tmdbId > 0) {
                                try {
                                    val creditsUrl = "https://api.themoviedb.org/3/$mediaType/$tmdbId/credits?language=en-US"
                                    val creditsResp = client.newCall(Request.Builder().url(creditsUrl).header("Authorization", "Bearer $tmdbToken").build()).execute()
                                    val creditsJson = JSONObject(creditsResp.body?.string() ?: "")
                                    val castArray = creditsJson.optJSONArray("cast")
                                    if (castArray != null) {
                                        val photoMap = mutableMapOf<String, String>()
                                        for (i in 0 until minOf(castArray.length(), 20)) {
                                            val castMember = castArray.getJSONObject(i)
                                            val name = castMember.optString("name", "")
                                            val profilePath = castMember.optString("profile_path", "")
                                            if (name.isNotBlank() && profilePath.isNotBlank() && profilePath != "null") {
                                                photoMap[name] = "https://image.tmdb.org/t/p/w185$profilePath"
                                            }
                                        }
                                        actorPhotos = photoMap
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                    } catch (_: Exception) { }
                }
            }

            // #40 — Season grouping
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

            // D-5: Related movies
            var relatedMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
            val firstGenre = movie.category.firstOrNull()?.slug ?: ""
            LaunchedEffect(slug, firstGenre) {
                if (firstGenre.isNotBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        MovieRepository.search(firstGenre)
                            .onSuccess { results ->
                                relatedMovies = results.filter { it.slug != slug }.take(12)
                            }
                    }
                }
            }

            // A-6: Parallax scroll
            val listState = rememberLazyListState()
            val backdropHeightPx = with(LocalDensity.current) { 320.dp.toPx() }
            val scrollOffset by remember {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat()
                    else backdropHeightPx
                }
            }
            val parallaxProgress = (scrollOffset / backdropHeightPx).coerceIn(0f, 1f)

            // ═══ LazyColumn — delegates to extracted composables ═══
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(C.Background)
                    .graphicsLayer {
                        alpha = enterAlpha.value
                        scaleX = enterScale.value
                        scaleY = enterScale.value
                    }
            ) {
                item { DetailBackdrop(movie, accentColor, scrollOffset, parallaxProgress, onBack) }

                item {
                    DetailActionRow(
                        slug = slug, accentColor = accentColor, isFav = isFav,
                        isWatchlisted = isWatchlisted, hasContinue = hasContinue,
                        continueItem = continueItem, continueEp = continueEp,
                        selectedServer = selectedServer, onPlay = onPlay,
                        onToggleFav = { FavoriteManager.toggle(slug, movie.name, movie.thumbUrl) },
                        onToggleWatchlist = { WatchlistManager.toggle(slug, movie.name, movie.thumbUrl) },
                        onShowPlaylist = { showPlaylistSheet = true }
                    )
                }

                item { DetailInfoSection(movie, imdbRating, tmdbRating, actorPhotos) }

                item { DetailServerTabs(episodes, selectedServer) { selectedServer = it } }

                if (relatedSeasons.isNotEmpty()) {
                    item { DetailSeasonRow(currentSeason, relatedSeasons, seasonRegex, onSeasonClick) }
                }

                if (episodes.isNotEmpty()) {
                    val eps = episodes.getOrNull(selectedServer)?.serverData.orEmpty()
                    item {
                        DetailEpisodeGrid(
                            eps = eps, slug = slug, selectedServer = selectedServer,
                            watchedSet = watchedSet, continueItem = continueItem,
                            episodeSortAsc = episodeSortAsc,
                            onToggleSort = { episodeSortAsc = !episodeSortAsc },
                            onPlay = onPlay
                        )
                    }
                }

                if (relatedMovies.isNotEmpty()) {
                    item { DetailRelatedRow(relatedMovies, onMovieClick) }
                } else {
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // C-5: Playlist dialog
            if (showPlaylistSheet) {
                DetailPlaylistDialog(
                    slug = slug, movieName = movie.name, thumbUrl = movie.thumbUrl,
                    playlists = playlists, onDismiss = { showPlaylistSheet = false }
                )
            }
        }
    }
}
