package com.cineshelf.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// One UI typography.
//
// The previous scale used FontWeight.Black at 42sp with -1.6sp tracking — a
// wordmark treatment applied to interface text. It shouted, and because every
// heading shouted, none of them ranked.
//
// This scale is built on three rules taken from One UI:
//
//   Weight   — headings top out at Bold. Nothing is Black. Emphasis inside a
//              heading comes from size, not from adding weight on top of weight.
//   Tracking — near zero. Slight negative only on the largest sizes, where
//              default tracking genuinely looks loose. Small text is NOT
//              letter-spaced open; that is a decorative tic, not hierarchy.
//   Rhythm   — line heights around 1.35x for headings and 1.45x for body, so
//              text blocks have air without drifting apart.
//
// The result is quieter per element and therefore clearer as a whole.
private val baseFont = FontFamily.Default

/** The screen title in a One UI large-title header. Big, but not heavy. */
val LargeTitle = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    letterSpacing = (-0.5).sp,
    lineHeight = 38.sp
)

/**
 * Section eyebrow. Sentence-cased at the call site now rather than shouted in
 * caps — One UI labels sections, it doesn't stamp them.
 */
val SectionEyebrow = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.sp
)

/** Metadata badge: resolution, codec, channel count. Quiet by construction. */
val BadgeLabel = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.1.sp
)

/** Timecode. Medium weight so digits hold their shape at small sizes. */
val TimeLabel = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.sp
)

/** The app wordmark. The one place a heavier weight is justified. */
val AuroraWordmark = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    letterSpacing = (-0.5).sp,
    color = TextPrimary
)

val CineShelfTypography = Typography(
    // Display — at most one per screen.
    displayLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.6).sp,
        lineHeight = 42.sp
    ),
    displayMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 38.sp
    ),
    displaySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 32.sp
    ),

    // Headline — section and card headers.
    headlineLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 28.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 24.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),

    // Title — item names inside cards and rows.
    titleLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    titleSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
        lineHeight = 18.sp
    ),

    // Body — prose. Always Normal.
    bodyLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
        lineHeight = 17.sp
    ),

    // Label — buttons and chips.
    labelLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.1.sp
    )
)
