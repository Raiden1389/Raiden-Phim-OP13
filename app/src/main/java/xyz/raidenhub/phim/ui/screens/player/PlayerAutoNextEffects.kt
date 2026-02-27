package xyz.raidenhub.phim.ui.screens.player

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.data.local.SettingsManager

/**
 * PlayerAutoNextEffects — Intro/outro config, auto-next loop (prefetch + countdown), playback ended fallback.
 *
 * Bug fix: LaunchedEffect(player) for STATE_ENDED now properly removes listener via DisposableEffect.
 */

/** Load intro/outro config */
@Composable
fun rememberEffectiveConfig(
    slug: String,
    movieCountry: String,
): MutableState<IntroOutroManager.SeriesConfig?> {
    val state = remember { mutableStateOf<IntroOutroManager.SeriesConfig?>(null) }
    LaunchedEffect(slug, movieCountry) {
        state.value = IntroOutroManager.getEffectiveConfig(slug, movieCountry)
    }
    return state
}

/** Auto-next loop: prefetch + countdown + auto-play */
@Composable
fun AutoNextEffect(
    player: ExoPlayer,
    source: String,
    currentEp: Int,
    autoNextMs: Long,
    effectiveConfig: IntroOutroManager.SeriesConfig?,
    episodes: List<xyz.raidenhub.phim.data.api.models.Episode>,
    context: Context,
    vm: PlayerViewModel,
) {
    val autoPlayEnabled by SettingsManager.autoPlayNext.collectAsState()
    var autoNextTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(currentEp) { autoNextTriggered = false }

    LaunchedEffect(player, currentEp, autoNextMs, autoPlayEnabled, effectiveConfig) {
        var prefetchTriggered = false
        while (true) {
            delay(3000)
            val dur = player.duration
            val pos = player.currentPosition
            if (dur <= 0) continue

            // SuperStream near-end prefetch
            if (source == "superstream" && vm.hasNext() && !prefetchTriggered) {
                val timeRemaining = dur - pos
                if (timeRemaining < 3 * 60 * 1000L || timeRemaining.toFloat() / dur < 0.15f) {
                    val nextEpItem = episodes.getOrNull(currentEp + 1)
                    if (nextEpItem != null && nextEpItem.linkM3u8.isBlank() && nextEpItem.slug.startsWith("ss::")) {
                        val parts = nextEpItem.slug.split("::")
                        if (parts.size == 4) {
                            val s = parts[2].toIntOrNull(); val e = parts[3].toIntOrNull()
                            if (s != null && e != null) { vm.fetchSuperStreamEp(s, e); prefetchTriggered = true }
                        }
                    } else prefetchTriggered = true
                }
            }

            // Auto-next logic (VN source only)
            if (!autoPlayEnabled || !vm.hasNext() || autoNextTriggered) continue
            if (source == "superstream" || source == "fshare") continue

            val shouldNext = if (effectiveConfig?.hasOutro == true) {
                pos >= effectiveConfig.outroStartMs
            } else {
                val remaining = dur - pos
                remaining in 1..autoNextMs
            }

            if (shouldNext) {
                autoNextTriggered = true
                val nextEpName = episodes.getOrNull(currentEp + 1)?.name ?: "tiếp"
                for (i in 5 downTo 1) {
                    Toast.makeText(context, "⏭ Tập $nextEpName trong ${i}s...", Toast.LENGTH_SHORT).show()
                    delay(1000)
                }
                Toast.makeText(context, "⏭ Chuyển sang Tập $nextEpName", Toast.LENGTH_SHORT).show()
                vm.nextEp()
            }
        }
    }
}

/** Fallback: auto-next when playback ends (fixed: proper listener cleanup) */
@Composable
fun PlaybackEndedEffect(player: ExoPlayer, source: String, vm: PlayerViewModel) {
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && vm.hasNext()
                    && SettingsManager.autoPlayNext.value
                    && source != "superstream" && source != "fshare"
                ) {
                    vm.nextEp()
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}
