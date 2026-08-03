package com.cineshelf.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The system font is used rather than a bundled typeface — embedding fonts adds
// real build risk (asset packaging, license files, download providers that need
// network at first paint) for a marginal gain. The character comes from the
// scale instead: tight negative tracking on large text, generous positive
// tracking on small caps-ish labels, and a wide weight spread between them.
private val baseFont = FontFamily.Default

/** Oversized screen header, outside Material's slots. */
val LargeTitle = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    letterSpacing = (-1.0).sp
)

/** All-caps section eyebrow above content rows. */
val SectionEyebrow = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    letterSpacing = 1.6.sp
)

/**
 * The app wordmark. Painted with a brush rather than a flat colour so the
 * aurora ramp is stated literally once, at the top of the library.
 */
val AuroraWordmark = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    letterSpacing = (-1.0).sp,
    brush = Brush.linearGradient(listOf(TextPrimary, AccentGlow, AuroraViolet))
)

val CineShelfTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = (-0.9).sp
    ),
    displaySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = (-0.15).sp
    ),
    titleSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)
