package com.cineshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Monochrome base, single purple accent.
//
// The interface is black and neutral gray. Colour appears only where it carries
// meaning: playback progress, the active item in a list, a focus ring. Anything
// decorative is white-at-low-alpha instead, so the video itself is always the
// most saturated thing on screen.
//
// Surfaces are true neutral (equal R/G/B) — a blue bias in the grays is what
// made the previous palette read as "purple everywhere" even where no accent
// was drawn.
// ---------------------------------------------------------------------------

// Base surfaces, layered dark-to-light for elevation.
val BackgroundPrimary = Color(0xFF000000)
val BackgroundSecondary = Color(0xFF070708)
val SurfaceSunken = Color(0xFF0B0B0C)
val SurfaceCard = Color(0xFF121213)
val SurfaceCardElevated = Color(0xFF1A1A1C)
val SurfaceRaised = Color(0xFF232326)
val SurfaceStroke = Color.White.copy(alpha = 0.07f)
val SurfaceStrokeStrong = Color.White.copy(alpha = 0.14f)

// The accent. One hue, three intensities — nothing else in the app is coloured.
val AccentPrimary = Color(0xFF9E7BFF)
val AccentBright = Color(0xFFB79AFF)
val AccentDeep = Color(0xFF7C5CE0)
val AccentGlow = AccentBright
val AccentSoft = AccentPrimary.copy(alpha = 0.14f)
val AccentFaint = AccentPrimary.copy(alpha = 0.07f)

// Retained aliases so accent references resolve to the single hue rather than
// reintroducing a second colour family.
val AuroraIndigo = AccentPrimary
val AuroraViolet = AccentBright
val AuroraCyan = AccentBright
val AuroraBlue = AccentDeep
val AuroraPink = AccentBright

// Status colours. Desaturated on purpose: they annotate, they don't decorate.
val AccentSuccess = Color(0xFF4ADE80)
val AccentDanger = Color(0xFFF87171)
val AccentWarning = Color(0xFFFBBF24)

// Ambient bleeds. Near-neutral lifts that keep the black from reading as a flat
// void, plus one barely-there accent tint.
val BleedNeutral = Color.White.copy(alpha = 0.045f)
val BleedNeutralSoft = Color.White.copy(alpha = 0.025f)
val BleedAccent = AccentPrimary.copy(alpha = 0.055f)

// Glass — translucent fills meant to sit over blurred/darkened content.
val GlassFill = Color.White.copy(alpha = 0.055f)
val GlassFillLight = Color.White.copy(alpha = 0.09f)
val GlassFillStrong = Color.White.copy(alpha = 0.13f)
val GlassStroke = Color.White.copy(alpha = 0.10f)
val GlassStrokeBright = Color.White.copy(alpha = 0.20f)

// Hairlines.
val HairlineLight = Color.White.copy(alpha = 0.06f)
val HairlineMid = Color.White.copy(alpha = 0.11f)
val HairlineStrong = Color.White.copy(alpha = 0.18f)

// Scrims for video overlays.
val ScrimSoft = Color.Black.copy(alpha = 0.28f)
val ScrimMedium = Color.Black.copy(alpha = 0.45f)
val ScrimStrong = Color.Black.copy(alpha = 0.70f)
val Scrim = ScrimStrong

/** Fill behind the transport glyphs: neutral gray, never tinted. */
val ControlCircleFill = Color.White.copy(alpha = 0.11f)
val ControlCircleStroke = Color.White.copy(alpha = 0.14f)

// Text.
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9C9CA3)
val TextTertiary = Color(0xFF6A6A70)
val TextQuaternary = Color(0xFF43434A)
