package com.cineshelf.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Strict 8pt spacing scale (with a 4dp half-step for tight cases like icon
 * padding). Every margin/padding/gap in the app should reference one of
 * these rather than a magic number, so spacing stays consistent everywhere.
 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
}

/**
 * Corner radii, tuned to One UI's proportions: rounded, but never so round that
 * a card starts reading as a pill. The previous 36dp sheet corner was wide
 * enough to eat into the first row of content.
 */
object Radius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 28.dp
    val pill = 999.dp
}
