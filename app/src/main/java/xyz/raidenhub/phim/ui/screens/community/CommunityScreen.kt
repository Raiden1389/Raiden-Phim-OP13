package xyz.raidenhub.phim.ui.screens.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.raidenhub.phim.data.repository.CommunityRepository.CommunityMovie
import xyz.raidenhub.phim.data.repository.CommunityRepository.CommunitySource
import xyz.raidenhub.phim.ui.theme.C

/**
 * CommunityScreen — Browse community-shared Fshare collections (Mobile).
 *
 * Level 1: List of community sources (people who shared)
 * Level 2+: Movies/categories from a selected source (supports drill-down)
 * → Click movie → navigate to Fshare detail or play directly
 */
@Composable
fun CommunityScreen(
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    LaunchedEffect(Unit) { viewModel.loadSources() }

    BackHandler(enabled = viewModel.currentLevel >= 2) {
        viewModel.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(C.Background)
    ) {
        // ═══ Top Bar ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (!viewModel.goBack()) onBack()
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = C.TextPrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (viewModel.currentLevel == 1) "👥 Fshare Community"
                           else "📂 ${viewModel.currentSourceName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = C.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (viewModel.currentLevel >= 2) {
                    Text(
                        text = viewModel.breadcrumb,
                        style = MaterialTheme.typography.bodySmall,
                        color = C.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (viewModel.sources.isNotEmpty()) {
                    Text(
                        text = "${viewModel.sources.size} nguồn chia sẻ",
                        style = MaterialTheme.typography.bodySmall,
                        color = C.TextMuted
                    )
                }
            }
        }

        // ═══ Content ═══
        Crossfade(
            targetState = viewModel.currentLevel,
            label = "community_level"
        ) { level ->
            when (level) {
                1 -> {
                    when {
                        viewModel.isLoadingSources -> LoadingIndicator("Đang tải danh sách nguồn...")
                        viewModel.sourcesError != null -> ErrorMessage(viewModel.sourcesError!!)
                        else -> SourceList(
                            sources = viewModel.sources,
                            onSourceClick = { viewModel.loadMovies(it) }
                        )
                    }
                }
                else -> {
                    when {
                        viewModel.isLoadingMovies -> LoadingIndicator("Đang tải phim...")
                        viewModel.moviesError != null -> ErrorMessage(viewModel.moviesError!!)
                        else -> MovieList(
                            movies = viewModel.movies,
                            onMovieClick = { movie ->
                                when {
                                    movie.isFshareFolder -> onMovieClick("fshare-folder:${movie.link}|||${movie.name}|||${movie.thumbnailUrl}")
                                    movie.isFshareFile -> onMovieClick("fshare-file:${movie.link}|||${movie.name}|||${movie.thumbnailUrl}")
                                    movie.isGoogleSheet -> {
                                        viewModel.loadMovies(
                                            CommunitySource(
                                                name = movie.name,
                                                sheetUrl = movie.link,
                                                thumbnailUrl = movie.thumbnailUrl
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ═══ Level 1: Source List ═══

@Composable
private fun SourceList(
    sources: List<CommunitySource>,
    onSourceClick: (CommunitySource) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = sources,
            key = { _, source -> source.sheetUrl }
        ) { _, source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSourceClick(source) }
                    .background(C.Surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle with first letter
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(C.Primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = C.Primary
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Source info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = C.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (source.description.isNotEmpty()) {
                        Text(
                            text = source.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = C.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Type badge
                val badge = when {
                    source.isFshareFolder -> "📁"
                    source.isGoogleSheet -> "📋"
                    else -> ""
                }
                if (badge.isNotEmpty()) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ═══ Level 2+: Movie List ═══

@Composable
private fun MovieList(
    movies: List<CommunityMovie>,
    onMovieClick: (CommunityMovie) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(
            items = movies,
            key = { idx, movie -> "${movie.link}_$idx" }
        ) { _, movie ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onMovieClick(movie) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Movie name
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = C.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Type indicator
                val typeText = when {
                    movie.isFshareFolder -> "📁"
                    movie.isFshareFile -> "▶️"
                    movie.isGoogleSheet -> "📂"
                    else -> ""
                }
                if (typeText.isNotEmpty()) {
                    Text(
                        text = typeText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Genre badge
                if (movie.genre.isNotEmpty()) {
                    Text(
                        text = movie.genre,
                        style = MaterialTheme.typography.bodySmall,
                        color = C.TextMuted,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ═══ Shared UI ═══

@Composable
private fun LoadingIndicator(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = C.Primary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(text, color = C.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("⚠️ $message", color = C.Error, style = MaterialTheme.typography.bodyMedium)
    }
}
