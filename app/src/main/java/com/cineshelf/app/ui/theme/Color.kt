package com.cineshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Aurora Glass — a deep-space palette.
//
// Surfaces are near-black but never pure #000: each layer carries a faint blue
// bias so stacked panels read as depth rather than as flat cutouts. Accents are
// drawn from an indigo -> cyan -> violet aurora ramp used for glows and active
// states, deliberately kept off the surfaces themselves so video content stays
// the brightest thing on screen.
// ---------------------------------------------------------------------------

// Base surfaces — cool near-blacks, layered light-to-dark for elevation.
val BackgroundPrimary = Color(0xFF06060A)
val BackgroundSecondary = Color(0xFF0A0A12)
val SurfaceCard = Color(0xFF101019)
val SurfaceCardElevated = Color(0xFF17172A)
val SurfaceRaised = Color(0xFF1E1E2E)
val SurfaceStroke = Color.White.copy(alpha = 0.07f)
val SurfaceStrokeStrong = Color.White.copy(alpha = 0.14f)

// Aurora accent ramp.
val AuroraIndigo = Color(0xFF6366F1)
val AuroraViolet = Color(0xFFA855F7)
val AuroraCyan = Color(0xFF22D3EE)
val AuroraBlue = Color(0xFF3B82F6)
val AuroraPink = Color(0xFFEC4899)

/** The one accent used for progress, active states, and focus. */
val AccentPrimary = AuroraIndigo
val AccentGlow = AuroraCyan
val AccentSoft = AuroraIndigo.copy(alpha = 0.16f)
val AccentSuccess = Color(0xFF34D399)
val AccentDanger = Color(0xFFFB7185)
val AccentWarning = Color(0xFFFBBF24)

// Ambient bleeds — very low alpha washes painted behind content to give the
// near-black background its subtle colored glow.
val BleedIndigo = AuroraIndigo.copy(alpha = 0.20f)
val BleedViolet = AuroraViolet.copy(alpha = 0.16f)
val BleedCyan = AuroraCyan.copy(alpha = 0.12f)

// Glass — translucent fills meant to sit over blurred/darkened content.
val GlassFill = Color.White.copy(alpha = 0.06f)
val GlassFillLight = Color.White.copy(alpha = 0.10f)
val GlassFillStrong = Color.White.copy(alpha = 0.14f)
val GlassStroke = Color.White.copy(alpha = 0.12f)
val GlassStrokeBright = Color.White.copy(alpha = 0.22f)

// Hairlines.
val HairlineLight = Color.White.copy(alpha = 0.06f)
val HairlineMid = Color.White.copy(alpha = 0.12f)
val HairlineStrong = Color.White.copy(alpha = 0.20f)

// Scrims for video overlays.
val ScrimSoft = Color.Black.copy(alpha = 0.30f)
val ScrimMedium = Color.Black.copy(alpha = 0.50f)
val ScrimStrong = Color.Black.copy(alpha = 0.72f)
val Scrim = ScrimStrong

/**
 * The play/pause fill: a low-opacity gray circle behind a plain play glyph,
 * rather than a heavy white disc.
 */
val ControlCircleFill = Color(0xFF8A8A99).copy(alpha = 0.22f)
val ControlCircleStroke = Color.White.copy(alpha = 0.16f)

// Text.
val TextPrimary = Color(0xFFF8F8FC)
val TextSecondary = Color(0xFFA9A9BC)
val TextTertiary = Color(0xFF6C6C82)
val TextQuaternary = Color(0xFF454557)
