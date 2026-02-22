package xyz.raidenhub.phim.ui.screens.superstream

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import xyz.raidenhub.phim.PlayerActivity
import xyz.raidenhub.phim.data.api.models.*
import xyz.raidenhub.phim.data.local.WatchHistoryManager
import xyz.raidenhub.phim.data.local.WatchlistManager
import xyz.raidenhub.phim.data.repository.SubtitleRepository
import xyz.raidenhub.phim.ui.theme.C

/**
 * SuperStream Detail Screen.
 * Shows movie/TV detail from TMDB + episodes from TMDB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperStreamDetailScreen(
    tmdbId: Int,
    type: String,
    onBack: () -> Unit,
    vm: SuperStreamDetailViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val availability by vm.availability.collectAsState()
    val tmdbSeasons by vm.tmdbSeasons.collectAsState()
    val tmdbEpisodes by vm.tmdbEpisodes.collectAsState()
    val selectedSeason by vm.selectedSeason.collectAsState()
    val streamState by vm.streamState.collectAsState()
    val statusMessage by vm.statusMessage.collectAsState()
    val context = LocalContext.current

    // Load detail on first compose
    LaunchedEffect(tmdbId, type) {
        vm.loadDetail(tmdbId, type)
    }

    // Pre-fetch subtitles when title + season is known (background, silent)
    var subsReady by remember { mutableStateOf(false) }
    val actualSeasonNum = tmdbSeasons.getOrNull(selectedSeason)?.seasonNumber
    LaunchedEffect(state, actualSeasonNum) {
        val title = when (val s = state) {
            is DetailState.MovieSuccess -> s.movie.title
            is DetailState.TvSuccess -> s.tv.name
            else -> null
        }
        if (title != null) {
            subsReady = false
            SubtitleRepository.prefetchSeason(
                filmName = title,
                type = type,
                season = if (type == "tv") actualSeasonNum else null,
                languages = "en"
            )
            subsReady = true
        }
    }

    // Handle stream ready → launch player
    LaunchedEffect(streamState) {
        val ss = streamState
        if (ss is StreamState.Ready) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra("stream_url", ss.stream.url)
                putExtra("stream_quality", ss.stream.quality)
                putExtra("source", "superstream")
                putExtra("stream_season", ss.season)
                putExtra("stream_episode", ss.episode)
                putExtra("stream_type", type) // "movie" or "tv"
                putExtra("tmdb_id", tmdbId)
                putExtra("total_episodes", tmdbEpisodes.size)
                // Pass share key so player can fetch next episodes without re-lookup
                val avail = availability
                if (avail is AvailabilityState.Available) {
                    putExtra("share_key", avail.shareKey)
                }
                putExtra("title", when (val s = state) {
                    is DetailState.MovieSuccess -> s.movie.title
                    is DetailState.TvSuccess -> s.tv.name
                    else -> ""
                })
            }
            context.startActivity(intent)
            vm.resetStreamState()
        }
    }

    // Favorite state
    val favSlug = "ss_${type}_${tmdbId}"
    val watchlistItems by WatchlistManager.items.collectAsState(initial = emptyList())
    val isFavorite = remember(watchlistItems, favSlug) {
        watchlistItems.any { it.slug == favSlug }
    }
    val favTitle = remember(state) {
        when (val s = state) {
            is DetailState.MovieSuccess -> s.movie.title
            is DetailState.TvSuccess -> s.tv.name
            else -> ""
        }
    }
    val favThumb = remember(state) {
        when (val s = state) {
            is DetailState.MovieSuccess -> s.movie.posterUrl
            is DetailState.TvSuccess -> s.tv.posterUrl
            else -> ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = C.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        WatchlistManager.toggle(favSlug, favTitle, favThumb, "superstream")
                    }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFE91E63) else C.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = C.Background
    ) { padding ->

        when (val s = state) {
            is DetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = C.Accent)
                }
            }

            is DetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${s.message}", color = C.Error)
                }
            }

            is DetailState.MovieSuccess -> {
                MovieDetailContent(
                    movie = s.movie,
                    availability = availability,
                    streamState = streamState,
                    statusMessage = statusMessage,
                    subsReady = subsReady,
                    onPlay = { vm.playMovie() },
                    modifier = Modifier.padding(padding)
                )
            }

            is DetailState.TvSuccess -> {
                // Watched episodes tracking
                val watchSlug = "ss_tv_${tmdbId}"
                val watchedEpIndices by WatchHistoryManager.getWatchedEpisodes(watchSlug).collectAsState(initial = emptyList())
                val watchedSet = watchedEpIndices.toSet()

                TvDetailContent(
                    tv = s.tv,
                    availability = availability,
                    tmdbSeasons = tmdbSeasons,
                    tmdbEpisodes = tmdbEpisodes,
                    selectedSeason = selectedSeason,
                    streamState = streamState,
                    statusMessage = statusMessage,
                    watchedSet = watchedSet,
                    subsReady = subsReady,
                    onSeasonSelect = { vm.selectSeason(it) },
                    onEpisodeClick = { ep ->
                        val seasonNum = tmdbSeasons.getOrNull(selectedSeason)?.seasonNumber ?: 1
                        vm.playTvEpisode(seasonNum, ep.episodeNumber)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun MovieDetailContent(
    movie: TmdbMovieDetail,
    availability: AvailabilityState,
    streamState: StreamState,
    statusMessage: String,
    subsReady: Boolean = false,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // Hero backdrop
        item {
            DetailHeroBanner(backdropUrl = movie.backdropUrl)
        }

        // Info
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    movie.title,
                    color = C.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ ${"%.1f".format(movie.voteAverage)}", color = Color(0xFFFFD700), fontSize = 14.sp)
                    Text("  •  ${movie.year}", color = C.TextSecondary, fontSize = 14.sp)
                    if (movie.runtime > 0) {
                        Text("  •  ${movie.runtime}min", color = C.TextSecondary, fontSize = 14.sp)
                    }
                }

                if (movie.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        movie.genres.joinToString(" • ") { it.name },
                        color = C.TextMuted,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Availability badge
                AvailabilityBadge(availability)

                Spacer(Modifier.height(12.dp))

                // Play button (only enabled when available)
                val canPlay = availability is AvailabilityState.Available && streamState !is StreamState.Loading
                Button(
                    onClick = onPlay,
                    enabled = canPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (availability is AvailabilityState.Available) C.Primary else C.SurfaceVariant,
                        contentColor = Color.White,
                        disabledContainerColor = C.SurfaceVariant,
                        disabledContentColor = C.TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (streamState is StreamState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Loading stream...")
                    } else if (availability is AvailabilityState.NotAvailable) {
                        Text("❌ Not available for streaming", fontWeight = FontWeight.Bold)
                    } else if (availability is AvailabilityState.Checking) {
                        CircularProgressIndicator(
                            color = C.TextMuted,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Checking availability...")
                    } else {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("▶ Play Movie", fontWeight = FontWeight.Bold)
                    }
                }

                if (streamState is StreamState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠ ${(streamState as StreamState.Error).message}",
                        color = C.Error,
                        fontSize = 12.sp
                    )
                }

                if (statusMessage.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(statusMessage, color = C.TextMuted, fontSize = 11.sp)
                }

                Spacer(Modifier.height(16.dp))

                if (movie.overview.isNotBlank()) {
                    Text(
                        movie.overview,
                        color = C.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDetailContent(
    tv: TmdbTvDetail,
    availability: AvailabilityState,
    tmdbSeasons: List<TmdbSeason>,
    tmdbEpisodes: List<TmdbEpisode>,
    selectedSeason: Int,
    streamState: StreamState,
    statusMessage: String,
    watchedSet: Set<Int> = emptySet(),
    subsReady: Boolean = false,
    onSeasonSelect: (Int) -> Unit,
    onEpisodeClick: (TmdbEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        // Hero backdrop
        item {
            DetailHeroBanner(backdropUrl = tv.backdropUrl)
        }

        // Info
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    tv.name,
                    color = C.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐ ${"%.1f".format(tv.voteAverage)}", color = Color(0xFFFFD700), fontSize = 14.sp)
                    Text("  •  ${tv.year}", color = C.TextSecondary, fontSize = 14.sp)
                    Text("  •  ${tv.numberOfSeasons} seasons", color = C.TextSecondary, fontSize = 14.sp)
                }

                if (tv.genres.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tv.genres.joinToString(" • ") { it.name },
                        color = C.TextMuted,
                        fontSize = 12.sp
                    )
                }

                if (tv.overview.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tv.overview,
                        color = C.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Status messages
        if (streamState is StreamState.Loading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = C.Accent,
                    trackColor = C.SurfaceVariant
                )
            }
        }

        if (streamState is StreamState.Error) {
            item {
                Text(
                    "⚠ ${(streamState as StreamState.Error).message}",
                    color = C.Error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        if (statusMessage.isNotBlank()) {
            item {
                Text(
                    statusMessage,
                    color = C.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Subtitle prefetch indicator
        if (subsReady) {
            item {
                Text(
                    "🔤 Subtitles ready",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }

        // Season selector (TMDB seasons)
        if (tmdbSeasons.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Seasons",
                    color = C.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tmdbSeasons.size) { index ->
                        val season = tmdbSeasons[index]
                        val isSelected = index == selectedSeason

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSeasonSelect(index) },
                            label = {
                                Text(
                                    "S${season.seasonNumber} (${season.episodeCount} ep)",
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = C.Primary,
                                selectedLabelColor = Color.White,
                                containerColor = C.SurfaceVariant,
                                labelColor = C.TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Episode list (TMDB episodes)
        if (tmdbEpisodes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Episodes (${tmdbEpisodes.size})",
                    color = C.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(tmdbEpisodes, key = { it.id }) { ep ->
                val epIdx = ep.episodeNumber - 1 // 0-based index
                val isWatched = watchedSet.contains(epIdx)
                TmdbEpisodeItem(
                    episode = ep,
                    isWatched = isWatched,
                    onClick = { onEpisodeClick(ep) }
                )
            }
        } else if (tmdbSeasons.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = C.Accent, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Bottom spacer
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/**
 * Episode item using TMDB data.
 */
@Composable
private fun TmdbEpisodeItem(
    episode: TmdbEpisode,
    isWatched: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(C.SurfaceVariant)
        ) {
            if (episode.thumbUrl.isNotBlank()) {
                AsyncImage(
                    model = episode.thumbUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Play icon
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(C.Overlay, RoundedCornerShape(50))
                    .padding(4.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isWatched) "✓ Episode ${episode.episodeNumber}" else "Episode ${episode.episodeNumber}",
                color = if (isWatched) C.Primary else C.TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isWatched) FontWeight.Bold else FontWeight.SemiBold
            )

            if (episode.name.isNotBlank()) {
                Text(
                    episode.name,
                    color = C.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (episode.runtime != null && episode.runtime > 0) {
                Text(
                    "${episode.runtime}min",
                    color = C.TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ═══ Availability Badge ═══

@Composable
private fun AvailabilityBadge(availability: AvailabilityState) {
    val (icon, text, color) = when (availability) {
        is AvailabilityState.Checking -> Triple("⏳", "Checking availability...", C.TextMuted)
        is AvailabilityState.Available -> Triple("🟢", "Available for streaming", Color(0xFF4CAF50))
        is AvailabilityState.NotAvailable -> Triple("🔴", "Not available on ShowBox", C.Error)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
