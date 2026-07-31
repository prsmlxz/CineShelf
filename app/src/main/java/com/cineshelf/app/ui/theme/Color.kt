package com.cineshelf.app.ui.theme

import androidx.compose.ui.graphics.Color

// Base surfaces - deep, near-black neutrals (not pure black, avoids OLED smear + feels premium)
val BackgroundPrimary = Color(0xFF0B0B0F)
val BackgroundSecondary = Color(0xFF141419)
val SurfaceCard = Color(0xFF1C1C22)
val SurfaceCardElevated = Color(0xFF24242B)
val SurfaceStroke = Color(0xFF2E2E36)

// Accent - a restrained, elegant blue (iOS system blue territory) plus a warm secondary
val AccentPrimary = Color(0xFF3D8BFF)
val AccentSoft = Color(0xFF3D8BFF).copy(alpha = 0.15f)
val AccentSuccess = Color(0xFF34C77B)
val AccentDanger = Color(0xFFFF4D5E)

// Text
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9C9CA6)
val TextTertiary = Color(0xFF6C6C76)

val Scrim = Color(0xFF000000).copy(alpha = 0.55f)
