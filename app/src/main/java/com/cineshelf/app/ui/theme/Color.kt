package com.cineshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces — true near-black neutrals, layered rather than flat,
// so cards read as "elevated glass" instead of pasted-on rectangles.
val BackgroundPrimary = Color(0xFF000000)
val BackgroundSecondary = Color(0xFF0A0A0C)
val SurfaceCard = Color(0xFF121214)
val SurfaceCardElevated = Color(0xFF1C1C1F)
val SurfaceStroke = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val SurfaceStrokeStrong = Color(0xFFFFFFFF).copy(alpha = 0.14f)

// Glass — translucent layers meant to sit over a blurred background.
val GlassFillLight = Color(0xFFFFFFFF).copy(alpha = 0.10f)
val GlassFill = Color(0xFFFFFFFF).copy(alpha = 0.07f)
val GlassFillStrong = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val GlassStroke = Color(0xFFFFFFFF).copy(alpha = 0.14f)
val HairlineLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val HairlineMid = Color(0xFFFFFFFF).copy(alpha = 0.16f)
val HairlineStrong = Color(0xFFFFFFFF).copy(alpha = 0.24f)
val ScrimSoft = Color(0xFF000000).copy(alpha = 0.35f)
val ScrimMedium = Color(0xFF000000).copy(alpha = 0.5f)
val ScrimStrong = Color(0xFF000000).copy(alpha = 0.72f)

// A single restrained accent (Apple system blue territory) — used sparingly:
// active states, progress, and the one or two things per screen that matter.
val AccentPrimary = Color(0xFF0A84FF)
val AccentSoft = AccentPrimary.copy(alpha = 0.16f)
val AccentSuccess = Color(0xFF30D158)
val AccentDanger = Color(0xFFFF453A)
val AccentWarning = Color(0xFFFFD60A)

// Play/pause button — a low-opacity neutral gray glass fill, deliberately
// NOT a solid white disc, plus a soft accent glow used behind it and behind
// the timebar thumb while actively dragging.
val PlayButtonFill = Color(0xFF8E8E93).copy(alpha = 0.30f)
val AccentGlow = AccentPrimary.copy(alpha = 0.45f)

// Text — Apple system gray scale equivalents
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFAEAEB2)
val TextTertiary = Color(0xFF6E6E73)
val TextQuaternary = Color(0xFF48484A)

val Scrim = ScrimStrong
