package xyz.raidenhub.phim.ui.screens.detail

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.ui.theme.C

// VP-2: Count-up animation cho số — premium feel
@Composable
fun AnimatedIntCounter(
    target: Int,
    suffix: String = "",
    prefix: String = "",
    durationMs: Int = 900
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(target) { started = true }
    val animatedValue by animateIntAsState(
        targetValue = if (started) target else 0,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "int_counter"
    )
    Text("$prefix$animatedValue$suffix", color = C.TextSecondary, fontSize = 13.sp)
}

@Composable
fun AnimatedFloatCounter(
    target: Float,
    suffix: String = "",
    prefix: String = "",
    durationMs: Int = 1000
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(target) { started = true }
    val animatedValue by animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "float_counter"
    )
    Text("$prefix${String.format("%.1f", animatedValue)}$suffix", color = C.TextSecondary, fontSize = 13.sp)
}
