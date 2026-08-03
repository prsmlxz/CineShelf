package com.cineshelf.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Motion language.
//
// Everything the finger touches responds with a spring, not a duration-based
// tween — springs retarget mid-flight, so rapid repeated taps never queue up or
// stutter the way a fixed tween does. Durations are reserved for fades, where
// there is no physical object being moved.
// ---------------------------------------------------------------------------

object Motion {
    /** Snappy, no overshoot. For press states that must feel instant. */
    fun <T> snap(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Default UI spring — a trace of overshoot so elements feel alive. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 380f)

    /** Playful spring for elements that grow/pop (scrubber expansion, badges). */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 420f)

    /** Heavier spring for large surfaces (sheets, panels) so they feel weighty. */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = 220f)

    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    fun <T> fade(durationMs: Int = 200): FiniteAnimationSpec<T> =
        tween(durationMs, easing = emphasized)
}

/**
 * Immediate physical press feedback, replacing Material's ripple. The scale
 * starts changing on the same frame as touch-down, which is the single biggest
 * lever for making taps feel instant rather than laggy.
 */
@Composable
fun Modifier.premiumPressable(
    scaleDown: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = Motion.standard(),
        label = "press-scale"
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
 * Press feedback that dims instead of scaling — for rows inside menus and
 * lists, where a scaling row looks broken next to its neighbours.
 */
@Composable
fun Modifier.premiumPressableNoScale(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.6f else 1f,
        animationSpec = Motion.snap(),
        label = "press-alpha"
    )
    return this
        .graphicsLayer { alpha = pressAlpha }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Translucent fill + hairline border. Real backdrop blur needs API 31+
 * RenderEffect and is unreliable over a video surface, so the illusion of
 * depth comes from a top-lit gradient overlay instead: brighter along the top
 * edge, fading down, the way glass catches light.
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(Radius.lg),
    fill: Color = GlassFill,
    stroke: Color = GlassStroke,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fill)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.06f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.05f)
            )
        )
    )
    .border(strokeWidth, stroke, shape)

/**
 * Casts a soft colored glow outward from behind an element. Drawn as a tinted
 * radial gradient underneath rather than an elevation shadow, so it can carry
 * the aurora accent colors.
 */
fun Modifier.auroraGlow(
    color: Color = AccentPrimary,
    radius: Dp = 24.dp,
    glowAlpha: Float = 0.45f,
    cornerRadius: Dp = 100.dp
): Modifier = this.drawBehind {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = (size.maxDimension / 2f) + radius.toPx()
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}

/**
 * The ambient aurora wash painted behind screen content: three wide, very low
 * alpha radial bleeds. This is what keeps the near-black background from
 * reading as a flat void.
 */
fun Modifier.auroraBackdrop(): Modifier = this.drawBehind {
    drawRect(BackgroundPrimary)

    val topLeft = Offset(size.width * 0.12f, size.height * 0.04f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedIndigo, Color.Transparent),
            center = topLeft,
            radius = size.width * 0.95f
        ),
        radius = size.width * 0.95f,
        center = topLeft
    )

    val topRight = Offset(size.width * 0.95f, size.height * 0.20f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedViolet, Color.Transparent),
            center = topRight,
            radius = size.width * 0.8f
        ),
        radius = size.width * 0.8f,
        center = topRight
    )

    val bottom = Offset(size.width * 0.5f, size.height * 0.96f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedCyan, Color.Transparent),
            center = bottom,
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = bottom
    )
}
