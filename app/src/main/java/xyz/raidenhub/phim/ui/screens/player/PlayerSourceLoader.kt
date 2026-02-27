package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import androidx.compose.runtime.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * PlayerSourceLoader — Compute effectiveSlug, trigger VM loading, prefetch episodes, play media.
 */

/** Compute effectiveSlug from params */
fun computeEffectiveSlug(
    slug: String, source: String, tmdbId: Int, streamType: String, fshareEpSlug: String
): String = when {
    source == "superstream" && slug.isBlank() -> "ss_${streamType}_${tmdbId}"
    source == "fshare" -> {
        val epUrl = fshareEpSlug.ifBlank {
            slug.split("|||").firstOrNull()
                ?.removePrefix("fshare-folder:")
                ?.removePrefix("fshare-file:") ?: slug
        }
        "fshare:$epUrl"
    }
    else -> slug
}

/** Trigger initial source loading via ViewModel */
@Composable
fun SourceLoadEffect(
    slug: String, source: String, server: Int, episode: Int,
    streamUrl: String, streamTitle: String, tmdbId: Int,
    streamSeason: Int, streamEpisode: Int, streamType: String,
    totalEpisodes: Int, shareKey: String, fshareEpSlug: String,
    context: Context, vm: PlayerViewModel,
) {
    LaunchedEffect(slug, source) {
        when (source) {
            "superstream" -> {
                if (streamUrl.isNotBlank()) vm.loadSuperStream(
                    streamUrl, streamTitle, tmdbId, streamSeason,
                    streamEpisode, streamType, totalEpisodes, shareKey
                )
            }
            "fshare" -> {
                val epSlug = fshareEpSlug.ifBlank {
                    slug.split("|||").firstOrNull()
                        ?.removePrefix("fshare-folder:")
                        ?.removePrefix("fshare-file:") ?: slug
                }
                vm.loadFshare(context, slug, epSlug, episode)
            }
            else -> vm.load(slug, server, episode)
        }
    }
}

/** Prefetch current + next episode URLs */
@Composable
fun PrefetchEffect(source: String, context: Context, vm: PlayerViewModel) {
    val episodes by vm.episodes.collectAsState()
    val currentEpIdx by vm.currentEp.collectAsState()

    LaunchedEffect(episodes, currentEpIdx) {
        // Current episode
        val curEp = episodes.getOrNull(currentEpIdx)
        if (curEp != null && curEp.linkM3u8.isBlank()) {
            if (source == "superstream" && curEp.slug.startsWith("ss::")) {
                val parts = curEp.slug.split("::")
                if (parts.size == 4) {
                    val s = parts[2].toIntOrNull(); val e = parts[3].toIntOrNull()
                    if (s != null && e != null) vm.fetchSuperStreamEp(s, e)
                }
            } else if (source == "fshare" && curEp.slug.startsWith("https://")) {
                vm.fetchFshareEp(context, curEp.slug)
            }
        }

        // Next episode
        val nextEp = episodes.getOrNull(currentEpIdx + 1)
        if (nextEp != null && nextEp.linkM3u8.isBlank()) {
            if (source == "superstream" && nextEp.slug.startsWith("ss::")) {
                val parts = nextEp.slug.split("::")
                if (parts.size == 4) {
                    val s = parts[2].toIntOrNull(); val e = parts[3].toIntOrNull()
                    if (s != null && e != null) vm.fetchSuperStreamEp(s, e)
                }
            } else if (source == "fshare" && nextEp.slug.startsWith("https://")) {
                vm.fetchFshareEp(context, nextEp.slug)
            }
        }
    }
}

/** Play media when episode URL becomes available */
@Composable
fun PlayMediaEffect(
    player: ExoPlayer,
    episodes: List<xyz.raidenhub.phim.data.api.models.Episode>,
    currentEp: Int,
    startEpisode: Int,
    startPositionMs: Long,
) {
    var hasSeekOnce by remember { mutableStateOf(false) }
    var lastLoadedUrl by remember { mutableStateOf("") }

    LaunchedEffect(currentEp, episodes) {
        if (episodes.isNotEmpty()) {
            val ep = episodes.getOrNull(currentEp) ?: return@LaunchedEffect
            val url = ep.linkM3u8
            if (url.isNotBlank() && url != lastLoadedUrl) {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                if (!hasSeekOnce && startPositionMs > 0L && currentEp == startEpisode) {
                    player.seekTo(startPositionMs)
                    hasSeekOnce = true
                }
                player.playWhenReady = true
                player.play()
                lastLoadedUrl = url
            }
        }
    }
}
