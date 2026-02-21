package xyz.raidenhub.phim.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import xyz.raidenhub.phim.R

/**
 * RaidenPhim Typography System
 *
 * Design rationale:
 *   - Headings:    Plus Jakarta Sans — geometric, modern, sharp feel
 *   - Body:        Inter — best readability for UI text
 *   - Labels/Mono: JetBrains Mono — crisp for episode numbers, ratings, badges
 *
 * All fonts loaded from Google Fonts provider (downloadable fonts, no APK bloat).
 * Fallback: sans-serif (system default) if download fails.
 */

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// ═══ Font definitions ═══

private val JakartaSans = GoogleFont("Plus Jakarta Sans")
private val Inter = GoogleFont("Inter")
private val JetBrainsMono = GoogleFont("JetBrains Mono")

val JakartaFamily = FontFamily(
    Font(googleFont = JakartaSans, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JakartaSans, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JakartaSans, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = JakartaSans, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = JakartaSans, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val InterFamily = FontFamily(
    Font(googleFont = Inter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = Inter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = Inter, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = Inter, fontProvider = provider, weight = FontWeight.Bold),
)

val MonoFamily = FontFamily(
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMono, fontProvider = provider, weight = FontWeight.Bold),
)

// ═══ Typography Scale ═══

val RaidenTypography = Typography(
    // Display — Hero titles, splash
    displayLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    ),
    displaySmall = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),

    // Headline — Section headers
    headlineLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),

    // Title — Card titles, list items
    titleLarge = TextStyle(
        fontFamily = JakartaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),

    // Body — Descriptions, paragraphs
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),

    // Label — Badges, chips, buttons, episode numbers
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    ),
)
