package xyz.raidenhub.phim.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.data.local.IntroOutroManager
import xyz.raidenhub.phim.ui.theme.C

/**
 * PlayerControlsOverlay — Layout shell + wiring.
 * Delegates to: PlayerTopBar, PlayerTransportControls, PlayerSeekSection,
 *               PlayerBottomActions, VerticalSliderColumn, gesture indicators.
 */
@Composable
fun PlayerControlsOverlay(
    player: ExoPlayer,
    showControls: Boolean,
    isLocked: Boolean,
    isFetchingEp: Boolean,
    currentEp: Int,
    title: String,
    episodes: List<xyz.raidenhub.phim.data.api.models.Episode>,
    speedIdx: Int,
    speeds: List<Float>,
    brightness: Float,
    volume: Float,
    aspectRatioMode: Int,
    currentPos: Long,
    duration: Long,
    showRemaining: Boolean,
    showSkipIntro: Boolean,
    effectiveConfig: IntroOutroManager.SeriesConfig?,
    subtitleTracks: List<TrackInfo>,
    audioTracks: List<TrackInfo>,
    showBrightnessIndicator: Boolean,
    showVolumeIndicator: Boolean,
    isSeekbarDragging: Boolean,
    seekbarDragFraction: Float,
    onBack: () -> Unit,
    onToggleLock: (Boolean) -> Unit,
    onToggleControls: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onAspectRatioChange: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onSeekbarDrag: (Float) -> Unit,
    onSeekbarDragEnd: () -> Unit,
    onToggleRemaining: () -> Unit,
    onShowSettings: () -> Unit,
    onShowEpisodes: () -> Unit,
    onShowSubtitles: () -> Unit,
    onShowAudio: () -> Unit,
    onPrevEp: () -> Unit,
    onNextEp: () -> Unit,
    onSkipIntro: () -> Unit,
    hasNext: Boolean,
) {
    if (showControls) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f))) {
            // Gradient scrims
            Box(
                Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
            )
            Box(
                Modifier.fillMaxWidth().height(180.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
            )

            if (!isLocked) {
                // Top bar
                PlayerTopBar(
                    title = title,
                    epName = episodes.getOrNull(currentEp)?.name ?: "",
                    speedIdx = speedIdx, speeds = speeds,
                    effectiveConfig = effectiveConfig,
                    onBack = onBack,
                    onSpeedClick = {
                        val newIdx = (speedIdx + 1) % speeds.size
                        player.setPlaybackSpeed(speeds[newIdx])
                        onSpeedChange(newIdx)
                    },
                    onLock = { onToggleLock(true) },
                    onShowSettings = onShowSettings,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Brightness slider (left)
                VerticalSliderColumn(
                    value = brightness,
                    icon = if (brightness > 0.5f) Icons.Default.LightMode else Icons.Default.BrightnessLow,
                    fillColor = Color.White.copy(0.6f), thumbColor = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                )

                // Volume slider (right)
                VerticalSliderColumn(
                    value = volume,
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    fillColor = C.Primary.copy(0.8f), thumbColor = C.Primary,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                )

                // Transport controls (center)
                PlayerTransportControls(
                    player = player, isFetchingEp = isFetchingEp,
                    currentEp = currentEp, hasNext = hasNext,
                    onPrevEp = onPrevEp, onNextEp = onNextEp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom section: seek + actions
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    PlayerSeekSection(
                        player = player, currentPos = currentPos, duration = duration,
                        showRemaining = showRemaining,
                        isSeekbarDragging = isSeekbarDragging, seekbarDragFraction = seekbarDragFraction,
                        onSeek = onSeek, onSeekbarDrag = onSeekbarDrag,
                        onSeekbarDragEnd = onSeekbarDragEnd, onToggleRemaining = onToggleRemaining
                    )

                    PlayerBottomActions(
                        aspectRatioMode = aspectRatioMode,
                        subtitleTracks = subtitleTracks, audioTracks = audioTracks,
                        episodes = episodes, currentEp = currentEp,
                        showSkipIntro = showSkipIntro,
                        onAspectRatioChange = onAspectRatioChange,
                        onShowSubtitles = onShowSubtitles, onShowAudio = onShowAudio,
                        onShowEpisodes = onShowEpisodes, onSkipIntro = onSkipIntro
                    )
                }
            } else {
                // Locked mode
                IconButton(
                    onClick = { onToggleLock(false) },
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
                        .background(Color.White.copy(0.15f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.LockOpen, "Unlock", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }

    // Gesture drag indicators (visible when controls hidden)
    if (showBrightnessIndicator && !showControls) {
        BrightnessIndicator(brightness, modifier = Modifier.padding(start = 24.dp))
    }
    if (showVolumeIndicator && !showControls) {
        VolumeIndicator(volume, modifier = Modifier.padding(end = 24.dp))
    }
}
