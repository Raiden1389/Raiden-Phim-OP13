package xyz.raidenhub.phim.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.raidenhub.phim.ui.theme.C
import xyz.raidenhub.phim.ui.theme.InterFamily
import kotlin.random.Random

// ═══════════════════════════════════════════════
// CN-2: "My Theater" Splash Screen
// Cinematic / premium feel — auto-dismisses after 2.2s
// ═══════════════════════════════════════════════

// 🎬 Movie quotes — hiện ngẫu nhiên mỗi lần mở app
private val MOVIE_QUOTES = listOf(
    "\"Tất cả những gì chúng ta phải quyết định là làm gì với thời gian được trao cho ta.\"\n— Gandalf, The Lord of the Rings",
    "\"Cuộc sống như hộp socola, bạn không bao giờ biết mình sẽ nhận được gì.\"\n— Forrest Gump",
    "\"Tôi sắp tạo ra cho hắn một đề nghị mà hắn không thể từ chối.\"\n— Don Corleone, The Godfather",
    "\"Tại sao chúng ta lại ngã? Để học cách đứng dậy.\"\n— Alfred, The Dark Knight",
    "\"Đừng trở thành ai đó để bù đắp quá khứ. Trở thành ai đó để tạo nên tương lai.\"\n— Interstellar",
    "\"Hãy cứ cố gắng. Thất bại. Cố gắng lại. Thất bại lại. Nhưng thất bại tốt hơn.\"\n— Samuel Beckett",
    "\"Có những ngày bạn chọn những kẻ yêu thích. Rồi có những ngày những kẻ yêu thích chọn bạn.\"\n— Major League",
    "\"Bạn không thể sống một cuộc đời hoàn toàn mới, nhưng bạn có thể bắt đầu một trang mới.\"\n— The Secret Life of Walter Mitty",
    "\"Đừng để ai nói với bạn rằng bạn không thể làm điều gì đó.\"\n— The Pursuit of Happyness",
    "\"Hãy luôn nhớ: gia đình là tất cả.\"\n— Dominic Toretto, Fast & Furious",
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // Random quote per session
    val quote = remember { MOVIE_QUOTES[Random.nextInt(MOVIE_QUOTES.size)] }

    // ─── Animation states ───
    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Logo entrance (0–600ms)
        logoScale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 250f)
        )
        logoAlpha.animateTo(1f, animationSpec = tween(400))

        // Phase 2: Quote fade-in (600–1000ms)
        delay(150)
        textAlpha.animateTo(1f, animationSpec = tween(500))
        textOffsetY.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))

        // Phase 3: Hold (1000–1800ms)
        delay(800)

        // Phase 4: Fade-out screen (1800–2200ms)
        screenAlpha.animateTo(
            0f,
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )

        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0A2E),  // deep purple center
                        Color(0xFF0D0D1A),  // dark navy edge
                    ),
                    radius = 1400f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow effect — subtle purple orb behind logo
        Box(
            modifier = Modifier
                .size(220.dp)
                .alpha(logoAlpha.value * 0.35f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.Center)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // ─── App Logo / Icon ───
            Text(
                text = "🎬",
                fontSize = 72.sp,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(Modifier.height(16.dp))

            // ─── App Name ───
            Text(
                text = "Raiden's Theater",
                fontFamily = InterFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Color.White,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                letterSpacing = 1.5.sp
            )

            Text(
                text = "✦ Rạp chiếu phim riêng của bạn ✦",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = Color(0xFF9B8FD4),
                modifier = Modifier
                    .alpha(logoAlpha.value)
                    .padding(top = 4.dp),
                letterSpacing = 0.5.sp
            )

            // ─── Divider ───
            Spacer(Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(1.dp)
                    .alpha(textAlpha.value)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF7C3AED),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(Modifier.height(24.dp))

            // ─── Movie Quote ───
            val (quoteText, quoteSource) = remember(quote) {
                val parts = quote.split("\n— ", limit = 2)
                if (parts.size == 2) "\"${parts[0].trim('"')}\"" to "— ${parts[1]}"
                else quote to ""
            }

            Text(
                text = quoteText,
                fontFamily = InterFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                color = Color(0xFFE2DAFF),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textOffsetY.value.dp)
            )

            if (quoteSource.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = quoteSource,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color(0xFF7C3AED),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(textAlpha.value * 0.8f)
                        .offset(y = textOffsetY.value.dp)
                )
            }
        }

        // ─── Bottom: Version tag ───
        Text(
            text = "v1.20.3",
            fontFamily = InterFamily,
            fontSize = 10.sp,
            color = Color(0xFF4A4065),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .alpha(textAlpha.value * 0.6f)
        )
    }
}
