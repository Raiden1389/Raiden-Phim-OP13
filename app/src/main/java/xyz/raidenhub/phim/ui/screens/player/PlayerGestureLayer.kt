package xyz.raidenhub.phim.ui.screens.player

import android.app.Activity
import android.media.AudioManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.media3.exoplayer.ExoPlayer
import xyz.raidenhub.phim.ui.theme.JakartaFamily
import xyz.raidenhub.phim.ui.theme.InterFamily

/** State holder for all gesture-related UI state */
class GestureState(
    var showControls: MutableState<Boolean>,
    var isLocked: MutableState<Boolean>,
    var brightness: MutableState<Float>,
    var showBrightnessIndicator: MutableState<Boolean>,
    var volume: MutableState<Float>,
    var showVolumeIndicator: MutableState<Boolean>,
    var seekAnimSide: MutableState<Int>,
    var seekAnimAmount: MutableState<Int>,
    var isHSwipeSeeking: MutableState<Boolean>,
    var hSwipeSeekPos: MutableState<Long>,
    var currentPos: MutableState<Long>,
)

/**
 * PlayerGestureLayer — Tap/double-tap, vertical drag (brightness/volume),
 * horizontal swipe seek, seek animation HUD, swipe seek billboard.
 *
 * Returns [Modifier] with gesture handling attached.
 */
@Composable
fun gestureModifiers(
    player: ExoPlayer,
    activity: Activity,
    audioManager: AudioManager,
    maxVolume: Int,
    gs: GestureState,
): Modifier {
    val haptic = LocalHapticFeedback.current

    return Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { gs.showControls.value = !gs.showControls.value },
                onDoubleTap = { offset ->
                    if (gs.isLocked.value) return@detectTapGestures
                    val third = size.width / 3
                    when {
                        offset.x < third -> {
                            player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                            gs.seekAnimSide.value = -1
                            gs.seekAnimAmount.value += 10
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        offset.x > size.width - third -> {
                            player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                            gs.seekAnimSide.value = 1
                            gs.seekAnimAmount.value += 10
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        else -> {
                            if (player.isPlaying) player.pause() else player.play()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
            )
        }
        .pointerInput(gs.isLocked.value) {
            if (gs.isLocked.value) return@pointerInput
            detectVerticalDragGestures(
                onDragEnd = {
                    gs.showBrightnessIndicator.value = false
                    gs.showVolumeIndicator.value = false
                }
            ) { change, dragAmount ->
                change.consume()
                val delta = -dragAmount / size.height
                if (change.position.x < size.width / 2) {
                    gs.brightness.value = (gs.brightness.value + delta).coerceIn(0.01f, 1f)
                    val params = activity.window.attributes
                    params.screenBrightness = gs.brightness.value
                    activity.window.attributes = params
                    gs.showBrightnessIndicator.value = true
                } else {
                    gs.volume.value = (gs.volume.value + delta).coerceIn(0f, 1f)
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        (gs.volume.value * maxVolume).toInt(), 0
                    )
                    gs.showVolumeIndicator.value = true
                }
            }
        }
}

/** Horizontal swipe seek overlay — invisible touch target */
@Composable
fun BoxScope.HorizontalSwipeSeekLayer(
    player: ExoPlayer,
    isLocked: Boolean,
    gs: GestureState,
) {
    if (!isLocked) {
        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    gs.isHSwipeSeeking.value = true
                    gs.hSwipeSeekPos.value = player.currentPosition
                    gs.showControls.value = true
                },
                onDragEnd = {
                    player.seekTo(gs.hSwipeSeekPos.value)
                    gs.currentPos.value = gs.hSwipeSeekPos.value
                    gs.isHSwipeSeeking.value = false
                },
                onDragCancel = { gs.isHSwipeSeeking.value = false }
            ) { change, delta ->
                change.consume()
                gs.hSwipeSeekPos.value = (gs.hSwipeSeekPos.value + (delta * 200).toLong())
                    .coerceIn(0L, player.duration.coerceAtLeast(0L))
            }
        })
    }
}

/** Seek animation HUD (double-tap ⏪/⏩ overlay) */
@Composable
fun BoxScope.SeekAnimationOverlay(seekAnimSide: Int, seekAnimAmount: Int) {
    AnimatedVisibility(
        visible = seekAnimSide != 0,
        modifier = Modifier.align(if (seekAnimSide == -1) Alignment.CenterStart else Alignment.CenterEnd),
        enter = fadeIn(), exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.padding(48.dp)
                .background(Color.Black.copy(0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (seekAnimSide == -1) "⏪" else "⏩", fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text("${seekAnimAmount}s", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Horizontal swipe seek billboard (center HUD) */
@Composable
fun BoxScope.SwipeSeekBillboard(isHSwipeSeeking: Boolean, hSwipeSeekPos: Long, duration: Long) {
    AnimatedVisibility(
        visible = isHSwipeSeeking,
        enter = fadeIn(tween(80)), exit = fadeOut(tween(200)),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Box(
            modifier = Modifier.background(Color.Black.copy(0.78f), RoundedCornerShape(14.dp))
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "↔  ${formatTime(hSwipeSeekPos)}",
                    color = Color.White, fontFamily = JakartaFamily,
                    fontSize = 26.sp, fontWeight = FontWeight.Bold
                )
                if (duration > 0) {
                    Text(
                        "/ ${formatTime(duration)}",
                        color = Color.White.copy(0.55f), fontFamily = InterFamily, fontSize = 13.sp
                    )
                }
            }
        }
    }
}
