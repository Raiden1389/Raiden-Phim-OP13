package xyz.raidenhub.phim.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.data.local.WatchHistoryManager

/**
 * PlayerSessionEffects — Fullscreen, audio focus (unified), save progress, release player.
 *
 * Bug fixes applied:
 * - Duplicate audio focus request consolidated into single path
 * - activity safe-cast via requireActivity()
 */

/** Sets up fullscreen, orientation lock, keep-screen-on */
@Composable
fun FullscreenEffect(activity: Activity) {
    DisposableEffect(Unit) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

/**
 * Unified audio focus — single request path for all API levels.
 * Also pauses player on transient/permanent loss.
 */
@Composable
fun AudioFocusEffect(player: ExoPlayer, audioManager: AudioManager) {
    DisposableEffect(player) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener { fc ->
                    when (fc) {
                        AudioManager.AUDIOFOCUS_LOSS,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
                    }
                }
                .build()
            audioManager.requestAudioFocus(focusRequest)
            onDispose { audioManager.abandonAudioFocusRequest(focusRequest) }
        } else {
            val focusListener = AudioManager.OnAudioFocusChangeListener { fc ->
                when (fc) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
                }
            }
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            onDispose {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusListener)
            }
        }
    }
}

/** Save watch progress + release player on dispose */
@Composable
fun SaveProgressEffect(
    player: ExoPlayer,
    effectiveSlug: String,
    title: String,
    source: String,
    currentEp: Int,
    streamEpisode: Int,
    tmdbId: Int,
    episodes: List<xyz.raidenhub.phim.data.api.models.Episode>,
) {
    DisposableEffect(Unit) {
        onDispose {
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0 && pos > 0) {
                val saveEpIdx = if (source == "superstream") (streamEpisode - 1).coerceAtLeast(0) + currentEp else currentEp
                val epName = episodes.getOrNull(currentEp)?.name ?: "Tập ${saveEpIdx + 1}"
                WatchHistoryManager.updateContinue(
                    slug = effectiveSlug, name = title, thumbUrl = "", source = source,
                    episodeIdx = saveEpIdx, episodeName = epName,
                    positionMs = pos, durationMs = dur
                )
                if (source == "superstream" && tmdbId > 0 && pos.toFloat() / dur >= 0.70f) {
                    WatchHistoryManager.markWatched("ss_tv_$tmdbId", saveEpIdx)
                }
            }
            player.release()
        }
    }
}
