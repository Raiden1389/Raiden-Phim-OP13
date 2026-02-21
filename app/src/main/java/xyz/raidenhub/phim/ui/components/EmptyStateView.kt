package xyz.raidenhub.phim.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import xyz.raidenhub.phim.ui.theme.JakartaFamily

/**
 * Cute animated empty state illustration.
 * Replaces boring plain text with engaging visuals.
 */
@Composable
fun EmptyStateView(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    // Floating animation for emoji
    val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_scale"
    )

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Floating animated emoji
            Text(
                text = emoji,
                fontSize = 64.sp,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = floatY
                        scaleX = scale
                        scaleY = scale
                    }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = title,
                color = C.TextPrimary,
                fontFamily = JakartaFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = subtitle,
                color = C.TextSecondary,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(0.7f)
            )

            if (action != null) {
                Spacer(Modifier.height(20.dp))
                action()
            }
        }
    }
}
