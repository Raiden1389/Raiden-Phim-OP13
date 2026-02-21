package xyz.raidenhub.phim.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * RaidenPhim Micro-interactions Library
 *
 * Reusable animation modifiers for premium feel:
 *   - bounceClick     — scale bounce on press (cards, buttons)
 *   - pulseEffect     — infinite subtle pulse (favorite hearts, live indicators)
 *   - pressScale      — scale shrink while pressed (Netflix-style card press)
 *   - shimmerGlow      — shimmer sweep (loading states)
 */

// ═══ 1. Bounce Click — tap → scale down → spring back ═══
// Usage: Modifier.bounceClick { onClick() }
fun Modifier.bounceClick(
    scaleDown: Float = 0.92f,
    onClick: () -> Unit
) = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scale.animateTo(scaleDown, tween(100))
                    }
                    tryAwaitRelease()
                    scope.launch {
                        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
                    }
                },
                onTap = { onClick() }
            )
        }
}

// ═══ 2. Press Scale — Netflix-style scale while pressed ═══
// Usage: Modifier.pressScale(0.95f).clickable { ... }
fun Modifier.pressScale(
    targetScale: Float = 0.95f,
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "press_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {}
        )
}

// ═══ 3. Pulse Effect — infinite subtle pulse ═══
// Usage: Modifier.pulseEffect()
// Great for: favorite heart icon, live dot, notification badge
fun Modifier.pulseEffect(
    minScale: Float = 0.85f,
    maxScale: Float = 1.15f,
    durationMs: Int = 800
) = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ═══ 4. Favorite Bounce — single bounce when toggled ═══
// Usage: val bounce = rememberFavoriteBounce()
//        Icon(modifier = bounce.modifier)
//        onClick { isFav = !isFav; bounce.trigger() }

class FavoriteBounceState {
    val scale = Animatable(1f)

    suspend fun trigger() {
        scale.animateTo(1.4f, tween(120))
        scale.animateTo(0.8f, tween(80))
        scale.animateTo(1.1f, tween(100))
        scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 600f))
    }

    val modifier: Modifier
        get() = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}

@Composable
fun rememberFavoriteBounce(): FavoriteBounceState {
    return remember { FavoriteBounceState() }
}

// ═══ 5. Slide-In fade — item appears with slide + alpha ═══
// For LazyColumn/LazyRow items entering viewport
fun Modifier.slideInFade(
    initialOffsetY: Float = 30f,
    durationMs: Int = 400
) = composed {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs),
        label = "slide_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else initialOffsetY,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "slide_offset"
    )

    LaunchedEffect(Unit) { visible = true }

    this.graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}
