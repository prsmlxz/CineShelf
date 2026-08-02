package com.cineshelf.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Replaces default Material ripple with an immediate, physical scale-down
 * on press — this is what makes taps feel instant rather than laggy, and is
 * the single biggest lever for the "premium/tactile" feel across the app.
 * No fade-in delay, no ripple spread animation: the response starts on the
 * same frame as the touch-down.
 */
@Composable
fun Modifier.premiumPressable(
    scaleDown: Float = 0.95f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "premium-press-scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * A translucent "glass" fill + hairline border, standing in for true
 * backdrop blur (which needs API 31+ RenderEffect and isn't reliable
 * enough over a video surface to depend on). Used for all floating
 * panels, menus, and overlays.
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(Radius.lg),
    fill: Color = GlassFill
): Modifier = this
    .clip(shape)
    .background(fill)
    .border(1.dp, HairlineMid, shape)
