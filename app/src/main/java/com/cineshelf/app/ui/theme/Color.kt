package com.cineshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// One UI palette: near-black surfaces, neutral gray elevation, one restrained
// accent.
//
// The rule that governs everything here: the accent is a *state marker*, not a
// brand. It is allowed to appear in exactly three places — playback progress,
// the selected item in a list, and the single primary action on a screen.
// Everywhere else that previously carried colour (badges, section rules, glows,
// background bleeds) is now white-at-low-alpha, because a monochrome interface
// is what lets the artwork and the video be the only saturated thing on screen.
//
// The accent itself was pulled well back from the old #9E7BFF: lower chroma,
// slightly deeper, so that at the small sizes it actually appears in it reads
// as "this one is selected" rather than as decoration.
// ---------------------------------------------------------------------------

// Base surfaces. One UI stacks near-blacks rather than jumping to gray, so
// elevation reads as depth instead of as a different material.
val BackgroundPrimary = Color(0xFF000000)
val BackgroundSecondary = Color(0xFF0A0A0B)
val SurfaceSunken = Color(0xFF101012)
val SurfaceCard = Color(0xFF17171A)
val SurfaceCardElevated = Color(0xFF1F1F23)
val SurfaceRaised = Color(0xFF2A2A2F)
val SurfaceStroke = Color.White.copy(alpha = 0.06f)
val SurfaceStrokeStrong = Color.White.copy(alpha = 0.12f)

/** The sheet surface. Opaque on purpose — a sheet is a place, not a filter. */
val SurfaceSheet = Color(0xFF1C1C20)

// The accent. Muted indigo-violet, three intensities. Nothing else is coloured.
val AccentPrimary = Color(0xFF7A6BD8)
val AccentBright = Color(0xFF9C8FEA)
val AccentDeep = Color(0xFF5F51B8)
val AccentGlow = AccentBright
val AccentSoft = AccentPrimary.copy(alpha = 0.16f)
val AccentFaint = AccentPrimary.copy(alpha = 0.08f)

// Retained aliases so nothing reintroduces a second hue.
val AuroraIndigo = AccentPrimary
val AuroraViolet = AccentBright
val AuroraCyan = AccentBright
val AuroraBlue = AccentDeep
val AuroraPink = AccentBright

// Status colours. Desaturated: they annotate, they don't decorate.
val AccentSuccess = Color(0xFF5CC98B)
val AccentDanger = Color(0xFFE5726F)
val AccentWarning = Color(0xFFE0B155)

// Ambient lifts. Neutral only — the accent no longer bleeds into backgrounds,
// which is what made the whole app read as tinted purple.
val BleedNeutral = Color.White.copy(alpha = 0.035f)
val BleedNeutralSoft = Color.White.copy(alpha = 0.02f)
val BleedAccent = Color.White.copy(alpha = 0.022f)

// Translucent fills for panels floating over video.
val GlassFill = Color.White.copy(alpha = 0.05f)
val GlassFillLight = Color.White.copy(alpha = 0.08f)
val GlassFillStrong = Color.White.copy(alpha = 0.12f)
val GlassStroke = Color.White.copy(alpha = 0.08f)
val GlassStrokeBright = Color.White.copy(alpha = 0.16f)

// Hairlines.
val HairlineLight = Color.White.copy(alpha = 0.05f)
val HairlineMid = Color.White.copy(alpha = 0.09f)
val HairlineStrong = Color.White.copy(alpha = 0.16f)

// Scrims for video overlays.
val ScrimSoft = Color.Black.copy(alpha = 0.25f)
val ScrimMedium = Color.Black.copy(alpha = 0.45f)
val ScrimStrong = Color.Black.copy(alpha = 0.72f)
val Scrim = ScrimStrong

/** Fill behind transport glyphs when one is needed at all. Never tinted. */
val ControlCircleFill = Color.White.copy(alpha = 0.10f)
val ControlCircleStroke = Color.White.copy(alpha = 0.12f)

// Text. Four levels, each a clear step down — hierarchy comes from these plus
// weight, never from colour.
val TextPrimary = Color(0xFFF2F2F5)
val TextSecondary = Color(0xFFA8A8B0)
val TextTertiary = Color(0xFF74747C)
val TextQuaternary = Color(0xFF4A4A52)
