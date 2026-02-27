package xyz.raidenhub.phim.ui.screens.player

import android.app.Activity
import android.media.AudioManager
import androidx.compose.runtime.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.delay
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.util.Constants

/**
 * PlayerUiState — All mutable/derived UI state + timer effects.
 *
 * Returns a data class holding all state for the player UI.
 */
data class PlayerUiStateHolder(
    val showControls: MutableState<Boolean>,
    val speedIdx: MutableState<Int>,
    val speeds: List<Float>,
    val isLocked: MutableState<Boolean>,
    val showSettingsSheet: MutableState<Boolean>,
    val showEpisodeSheet: MutableState<Boolean>,
    val brightness: MutableState<Float>,
    val showBrightnessIndicator: MutableState<Boolean>,
    val volume: MutableState<Float>,
    val showVolumeIndicator: MutableState<Boolean>,
    val seekAnimSide: MutableState<Int>,
    val seekAnimAmount: MutableState<Int>,
    val aspectRatioMode: MutableState<Int>,
    val currentPos: MutableState<Long>,
    val duration: MutableState<Long>,
    val showRemaining: MutableState<Boolean>,
    val isHSwipeSeeking: MutableState<Boolean>,
    val hSwipeSeekPos: MutableState<Long>,
    val isSeekbarDragging: MutableState<Boolean>,
    val seekbarDragFraction: MutableState<Float>,
    val showSubtitleDialog: MutableState<Boolean>,
    val showAudioDialog: MutableState<Boolean>,
    val showSkipIntro: State<Boolean>,
)

@Composable
fun rememberPlayerUiState(
    activity: Activity,
    audioManager: AudioManager,
    maxVolume: Int,
    player: ExoPlayer,
    effectiveConfig: IntroOutroManager.SeriesConfig?,
): PlayerUiStateHolder {
    val showControls = remember { mutableStateOf(true) }
    val speedIdx = remember { mutableIntStateOf(2) }
    val isLocked = remember { mutableStateOf(false) }
    val showSettingsSheet = remember { mutableStateOf(false) }
    val showEpisodeSheet = remember { mutableStateOf(false) }
    val brightness = remember {
        mutableStateOf(activity.window.attributes.screenBrightness.let { if (it < 0) 0.5f else it })
    }
    val showBrightnessIndicator = remember { mutableStateOf(false) }
    val volume = remember {
        mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    val showVolumeIndicator = remember { mutableStateOf(false) }
    val seekAnimSide = remember { mutableIntStateOf(0) }
    val seekAnimAmount = remember { mutableIntStateOf(0) }
    val aspectRatioMode = remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    val currentPos = remember { mutableStateOf(0L) }
    val duration = remember { mutableStateOf(0L) }
    val showRemaining = remember { mutableStateOf(false) }
    val isHSwipeSeeking = remember { mutableStateOf(false) }
    val hSwipeSeekPos = remember { mutableStateOf(0L) }
    val isSeekbarDragging = remember { mutableStateOf(false) }
    val seekbarDragFraction = remember { mutableStateOf(0f) }
    val showSubtitleDialog = remember { mutableStateOf(false) }
    val showAudioDialog = remember { mutableStateOf(false) }

    val showSkipIntro by remember {
        derivedStateOf {
            val cfg = effectiveConfig ?: return@derivedStateOf false
            if (!cfg.hasIntro && cfg.introEndMs <= 0) return@derivedStateOf false
            val introStart = if (cfg.introStartMs >= 0) cfg.introStartMs else 0L
            currentPos.value in introStart..cfg.introEndMs
        }
    }
    val showSkipIntroState = rememberUpdatedState(showSkipIntro)

    // Position tracker
    LaunchedEffect(player) {
        while (true) {
            currentPos.value = player.currentPosition.coerceAtLeast(0)
            duration.value = player.duration.coerceAtLeast(0)
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls.value) {
        if (showControls.value) { delay(4000); showControls.value = false }
    }

    // Auto-hide seek animation
    LaunchedEffect(seekAnimSide.intValue, seekAnimAmount.intValue) {
        if (seekAnimSide.intValue != 0) { delay(700); seekAnimSide.intValue = 0; seekAnimAmount.intValue = 0 }
    }

    return PlayerUiStateHolder(
        showControls = showControls, speedIdx = speedIdx,
        speeds = Constants.PLAYBACK_SPEEDS, isLocked = isLocked,
        showSettingsSheet = showSettingsSheet, showEpisodeSheet = showEpisodeSheet,
        brightness = brightness, showBrightnessIndicator = showBrightnessIndicator,
        volume = volume, showVolumeIndicator = showVolumeIndicator,
        seekAnimSide = seekAnimSide, seekAnimAmount = seekAnimAmount,
        aspectRatioMode = aspectRatioMode, currentPos = currentPos,
        duration = duration, showRemaining = showRemaining,
        isHSwipeSeeking = isHSwipeSeeking, hSwipeSeekPos = hSwipeSeekPos,
        isSeekbarDragging = isSeekbarDragging, seekbarDragFraction = seekbarDragFraction,
        showSubtitleDialog = showSubtitleDialog, showAudioDialog = showAudioDialog,
        showSkipIntro = showSkipIntroState,
    )
}
