package com.cineshelf.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The system font is used rather than a bundled typeface — embedding fonts adds
// real build risk (asset packaging, license files, download providers that need
// network at first paint) for a marginal gain.
//
// Hierarchy comes from contrast, not from colour. Three deliberate gaps:
//
//   Weight  — headings jump straight from Normal body text to Bold/Black.
//             Nothing sits at SemiBold pretending to be both.
//   Size    — each step is ~1.3x the one below it, so adjacent levels are
//             unmistakably different rather than "one point smaller".
//   Tracking— large text tightens (negative), small text opens up (positive).
//             This is what makes a 28sp title and an 11sp label read as two
//             different kinds of object rather than the same text resized.
private val baseFont = FontFamily.Default

/** Oversized screen header, outside Material's slots. */
val LargeTitle = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Black,
    fontSize = 34.sp,
    letterSpacing = (-1.1).sp
)

/** All-caps section eyebrow above content rows. The quietest text in the app. */
val SectionEyebrow = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 1.8.sp
)

/** Small caps-ish metadata badge: resolution, HDR, codec. */
val BadgeLabel = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    letterSpacing = 0.9.sp
)

/** Tabular-feeling timecode. Medium weight so digits hold their shape small. */
val TimeLabel = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.4.sp
)

/** The app wordmark. Flat white — the accent appears as a dot beside it. */
val AuroraWordmark = TextStyle(
    fontFamily = baseFont,
    fontWeight = FontWeight.Black,
    fontSize = 30.sp,
    letterSpacing = (-1.2).sp,
    color = TextPrimary
)

val CineShelfTypography = Typography(
    // Display — used once per screen at most.
    displayLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        letterSpacing = (-1.6).sp,
        lineHeight = 46.sp
    ),
    displayMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Black,
        fontSize = 33.sp,
        letterSpacing = (-1.2).sp,
        lineHeight = 37.sp
    ),
    displaySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        letterSpacing = (-0.8).sp,
        lineHeight = 30.sp
    ),

    // Headline — section and card headers.
    headlineLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.6).sp,
        lineHeight = 26.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 20.sp
    ),

    // Title — item names inside cards and rows.
    titleLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.3).sp,
        lineHeight = 21.sp
    ),
    titleMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 18.sp
    ),
    titleSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 16.sp
    ),

    // Body — prose. Always Normal, always the lightest thing on screen.
    bodyLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
        lineHeight = 16.sp
    ),

    // Label — buttons and chips. Tighter and heavier than body at the same size.
    labelLarge = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = (-0.1).sp
    ),
    labelMedium = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = baseFont,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp
    )
)
