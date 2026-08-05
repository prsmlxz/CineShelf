package com.cineshelf.app.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Motion language.
//
// Two families, used for different jobs:
//
//   Springs  — anything the finger is currently touching (press scale, a thumb
//              tracking a drag). Springs retarget mid-flight, so rapid repeated
//              input never queues up or stutters.
//   Easings  — anything that plays out on its own after a gesture ends: fades,
//              menu entrances, controls appearing. These are duration-based and
//              all live in the 180-300ms band, which is long enough to read as
//              deliberate and short enough to never feel like waiting.
//
// Nothing in the app should change state instantly.
// ---------------------------------------------------------------------------

object Motion {
    // --- Durations (ms) --------------------------------------------------
    const val Instant = 120
    const val Quick = 180
    const val Base = 240
    const val Relaxed = 300

    // --- Easing curves ---------------------------------------------------
    /** Accelerate out, settle long. The default for anything entering. */
    val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Slow to a stop with no initial ramp — for elements sliding into place. */
    val decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Ramp up with no tail — for elements leaving the screen. */
    val accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Symmetric ease for value cross-fades (colour, alpha) with no direction. */
    val smooth = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    // --- Springs ---------------------------------------------------------
    /** Snappy, no overshoot. For press states that must feel instant. */
    fun <T> snap(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    /** Default UI spring — a trace of overshoot so elements feel alive. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.75f, stiffness = 380f)

    /** Playful spring for elements that grow/pop (scrubber expansion, badges). */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.58f, stiffness = 420f)

    /** Heavier spring for large surfaces (sheets, panels) so they feel weighty. */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = 220f)

    // --- Tweens ----------------------------------------------------------
    fun <T> fade(durationMs: Int = Base): FiniteAnimationSpec<T> =
        tween(durationMs, easing = smooth)

    fun <T> enter(durationMs: Int = Base): FiniteAnimationSpec<T> =
        tween(durationMs, easing = emphasized)

    fun <T> exit(durationMs: Int = Quick): FiniteAnimationSpec<T> =
        tween(durationMs, easing = accelerate)

    // --- Composite transitions -------------------------------------------
    /**
     * Menus and popups: fade up from 96% scale, anchored to the given origin so
     * a sheet grows from its edge rather than from thin air.
     */
    fun menuEnter(origin: TransformOrigin = TransformOrigin(0.5f, 1f)): EnterTransition =
        fadeIn(enter(Base)) + scaleIn(enter(Base), initialScale = 0.96f, transformOrigin = origin)

    fun menuExit(origin: TransformOrigin = TransformOrigin(0.5f, 1f)): ExitTransition =
        fadeOut(exit(Quick)) + scaleOut(exit(Quick), targetScale = 0.97f, transformOrigin = origin)

    /** Bottom sheets: rise a short distance while fading, never a full slide. */
    fun sheetEnter(): EnterTransition =
        fadeIn(enter(Base)) + slideInVertically(tween(Base, easing = decelerate)) { it / 6 }

    fun sheetExit(): ExitTransition =
        fadeOut(exit(Quick)) + slideOutVertically(tween(Quick, easing = accelerate)) { it / 8 }

    /** Playback controls: a plain, unhurried cross-fade. No movement. */
    fun controlsEnter(): EnterTransition = fadeIn(tween(Relaxed, easing = smooth))

    fun controlsExit(): ExitTransition = fadeOut(tween(Base, easing = smooth))
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
 * Press feedback that both scales and dims. Used for the large transport
 * controls, where a scale alone is too subtle against moving video.
 */
@Composable
fun Modifier.premiumPressableSoft(
    scaleDown: Float = 0.90f,
    dimTo: Float = 0.78f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (active) scaleDown else 1f,
        animationSpec = Motion.standard(),
        label = "soft-press-scale"
    )
    val dim by animateFloatAsState(
        targetValue = if (active) dimTo else 1f,
        animationSpec = Motion.snap(),
        label = "soft-press-dim"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = dim }
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
 * RenderEffect and does not composite over a video SurfaceView, so the illusion
 * of depth comes from a top-lit gradient overlay instead: brighter along the top
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
                Color.White.copy(alpha = 0.055f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.06f)
            )
        )
    )
    .border(strokeWidth, stroke, shape)

/**
 * A heavier glass for surfaces that float directly over video — an opaque black
 * base beneath the translucent fill, so subtitles and bright frames underneath
 * can't bleed through and muddy the panel.
 */
fun Modifier.glassPanelOverVideo(
    shape: Shape = RoundedCornerShape(Radius.lg),
    baseAlpha: Float = 0.55f,
    fill: Color = GlassFill,
    stroke: Color = GlassStroke,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(Color.Black.copy(alpha = baseAlpha))
    .background(fill)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.07f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.05f)
            )
        )
    )
    .border(strokeWidth, stroke, shape)

/**
 * Casts a soft glow outward from behind an element. Drawn as a tinted radial
 * gradient underneath rather than an elevation shadow, so it can carry the
 * accent colour. Kept deliberately faint — a glow is a hint, not a light source.
 */
fun Modifier.auroraGlow(
    color: Color = AccentPrimary,
    radius: Dp = 24.dp,
    glowAlpha: Float = 0.35f,
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
 * The ambient wash painted behind screen content. Two near-neutral lifts keep
 * the pure black from reading as a flat void, and a single very faint accent
 * bleed sits low and centre — the only colour on an otherwise monochrome
 * background.
 */
fun Modifier.auroraBackdrop(): Modifier = this.drawBehind {
    drawRect(BackgroundPrimary)

    val topLeft = Offset(size.width * 0.10f, -size.height * 0.02f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedNeutral, Color.Transparent),
            center = topLeft,
            radius = size.width * 1.0f
        ),
        radius = size.width * 1.0f,
        center = topLeft
    )

    val topRight = Offset(size.width * 1.02f, size.height * 0.14f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedNeutralSoft, Color.Transparent),
            center = topRight,
            radius = size.width * 0.85f
        ),
        radius = size.width * 0.85f,
        center = topRight
    )

    val bottom = Offset(size.width * 0.5f, size.height * 1.0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedAccent, Color.Transparent),
            center = bottom,
            radius = size.width * 0.9f
        ),
        radius = size.width * 0.9f,
        center = bottom
    )
}
