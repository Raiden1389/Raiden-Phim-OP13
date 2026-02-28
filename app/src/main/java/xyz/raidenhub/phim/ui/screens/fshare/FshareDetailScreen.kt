package xyz.raidenhub.phim.ui.screens.fshare

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.launch
import xyz.raidenhub.phim.data.local.FavoriteManager
import xyz.raidenhub.phim.ui.theme.C

/**
 * FshareDetailScreen — Movie detail for Fshare HD content (Mobile).
 *
 * Adapted from PhimTV's two-panel TV layout → mobile vertical scroll.
 * - Backdrop image with gradient overlay
 * - Movie info (title, year, description)
 * - Action buttons (Play, Favorite)
 * - Episode list / folder expand
 */
@Composable
fun FshareDetailScreen(
    detailUrl: String,
    slug: String,
    isFshareDirect: Boolean = false,
    fshareName: String = "Fshare",
    fshareThumb: String = "",
    onEpisodeClick: (slug: String, episodeSlug: String, serverIndex: Int) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {},
    viewModel: FshareDetailViewModel = viewModel()
) {
    LaunchedEffect(detailUrl) {
        if (isFshareDirect) {
            viewModel.loadFolderDirect(detailUrl, fshareName, fshareThumb)
        } else {
            viewModel.loadDetail(detailUrl)
        }
    }

    // Intercept Back when inside subfolder → go back to parent folder
    androidx.activity.compose.BackHandler(enabled = viewModel.canNavigateBack) {
        viewModel.navigateBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(C.Background)) {
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = C.Primary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Đang tải Fshare...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = C.TextSecondary
                        )
                    }
                }
            }
            viewModel.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "⚠️ ${viewModel.error}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = C.Error
                    )
                }
            }
            viewModel.movie != null -> {
                FshareDetailContent(
                    viewModel = viewModel,
                    slug = slug,
                    onEpisodeClick = onEpisodeClick
                )
            }
        }

        // ═══ Back Button (floating over backdrop) ═══
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = C.TextPrimary
            )
        }
    }
}

/**
 * Main content: backdrop + vertical scroll layout (mobile).
 */
@Composable
private fun FshareDetailContent(
    viewModel: FshareDetailViewModel,
    slug: String,
    onEpisodeClick: (slug: String, episodeSlug: String, serverIndex: Int) -> Unit
) {
    val movie = viewModel.movie!!
    val episodes = viewModel.episodes
    val ctx = LocalContext.current
    val favScope = rememberCoroutineScope()

    // Favorite state
    val isFavorite by FavoriteManager.isFavoriteFlow(slug).collectAsState(initial = false)

    // Build enriched slug for player
    val fshareUrl = viewModel.fshareUrl
    val enrichedSlug = remember(slug, movie.name, movie.posterUrl, movie.thumbUrl, fshareUrl) {
        val poster = movie.posterUrl.ifEmpty { movie.thumbUrl }
        val existingParts = slug.split("|||")
        val urlPart = existingParts[0]
        val urlIsFshare = "fshare.vn" in urlPart

        // If we have a real fshare.vn URL from ViewModel, always use it
        if (fshareUrl != null && !urlIsFshare) {
            "fshare-folder:$fshareUrl|||${movie.name}|||$poster"
        } else if (slug.contains("|||")) {
            slug  // Already enriched with fshare.vn URL
        } else {
            val prefix = if (fshareUrl != null) "fshare-folder:$fshareUrl" else slug
            "$prefix|||${movie.name}|||$poster"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ═══ BACKDROP ═══
        val backdropUrl = movie.thumbUrl.ifEmpty { movie.posterUrl }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            if (backdropUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(backdropUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                C.Background.copy(alpha = 0.3f),
                                C.Background.copy(alpha = 0.7f),
                                C.Background
                            )
                        )
                    )
            )
        }

        // ═══ MOVIE INFO ═══
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Title
            Text(
                text = movie.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = C.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Origin name + Year
            val metaParts = mutableListOf<String>()
            if (movie.originName.isNotBlank()) metaParts.add(movie.originName)
            if (movie.year > 0) metaParts.add("${movie.year}")
            if (movie.episodeCurrent.isNotBlank()) metaParts.add(movie.episodeCurrent)
            movie.country.firstOrNull()?.name?.let { metaParts.add(it) }

            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = C.TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ═══ ACTION BUTTONS ═══
            FshareActionButtons(
                isFavorite = isFavorite,
                showPlay = !viewModel.isFolderPlaceholder && episodes.isNotEmpty(),
                onPlay = {
                    val firstEp = episodes.firstOrNull()?.serverData?.firstOrNull()
                    if (firstEp != null) {
                        onEpisodeClick(enrichedSlug, firstEp.slug, 0)
                    }
                },
                onToggleFavorite = {
                    val poster = movie.posterUrl.ifEmpty { movie.thumbUrl }
                    FavoriteManager.toggle(slug, movie.name, poster, "fshare")
                }
            )

            Spacer(Modifier.height(16.dp))

            // ═══ DESCRIPTION ═══
            if (movie.content.isNotBlank()) {
                Text(
                    text = movie.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = C.TextSecondary,
                    lineHeight = 20.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
            }

            // ═══ EPISODES ═══
            FshareEpisodePanel(
                episodes = episodes,
                isFolderPlaceholder = viewModel.isFolderPlaceholder,
                isFolderExpanding = viewModel.isFolderExpanding,
                folderError = viewModel.folderError,
                slug = enrichedSlug,
                onFolderClick = { url -> viewModel.expandFolder(url) },
                onEpisodeClick = onEpisodeClick
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
