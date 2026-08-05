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
//   Springs  — anything the finger is currently touching, and anything with
//              physical presence (a sheet arriving, a thumb tracking a drag).
//              Springs retarget mid-flight, so rapid input never queues up.
//   Easings  — flat cross-fades where nothing moves through space: controls
//              appearing over video, a colour changing.
//
// Timings are deliberate, not defaults:
//
//   Tap response   140ms — fast enough to read as caused by the finger.
//   Press scale    0.97  — a compression, not a shrink. Visible at a glance,
//                          invisible as an effect.
//   Sheets       ~280ms  — spring, so it decelerates into place with mass.
//
// Nothing in the app changes state instantly.
// ---------------------------------------------------------------------------

object Motion {
    // --- Durations (ms) --------------------------------------------------
    /** Touch acknowledgement. */
    const val Instant = 140
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

    /** Default UI spring. Nearly critically damped — settles, never wobbles. */
    fun <T> standard(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 500f)

    /**
     * For controls that grow under the finger (scrubber rails, slider thumb).
     * A trace of overshoot only — the previous 0.58 damping visibly bounced,
     * which reads as a toy rather than as a precision control.
     */
    fun <T> bouncy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 420f)

    /** Sheets and large panels. Weighty, no overshoot, ~280ms to rest. */
    fun <T> gentle(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.92f, stiffness = 340f)

    // --- Tweens ----------------------------------------------------------
    fun <T> fade(durationMs: Int = Base): FiniteAnimationSpec<T> =
        tween(durationMs, easing = smooth)

    fun <T> enter(durationMs: Int = Base): FiniteAnimationSpec<T> =
        tween(durationMs, easing = emphasized)

    fun <T> exit(durationMs: Int = Quick): FiniteAnimationSpec<T> =
        tween(durationMs, easing = accelerate)

    // --- Composite transitions -------------------------------------------
    /** Dialogs and popups: fade with a whisper of scale, never from nothing. */
    fun menuEnter(origin: TransformOrigin = TransformOrigin(0.5f, 1f)): EnterTransition =
        fadeIn(enter(Base)) + scaleIn(enter(Base), initialScale = 0.97f, transformOrigin = origin)

    fun menuExit(origin: TransformOrigin = TransformOrigin(0.5f, 1f)): ExitTransition =
        fadeOut(exit(Quick)) + scaleOut(exit(Quick), targetScale = 0.98f, transformOrigin = origin)

    /**
     * Bottom sheets. The slide is spring-driven so the sheet decelerates into
     * its resting position with mass behind it; a tween of the same duration
     * arrives at a constant-feeling stop and reads as cheaper.
     */
    fun sheetEnter(): EnterTransition =
        fadeIn(tween(Quick, easing = smooth)) + slideInVertically(gentle()) { it / 3 }

    fun sheetExit(): ExitTransition =
        fadeOut(exit(Quick)) + slideOutVertically(tween(Base, easing = accelerate)) { it / 4 }

    /** Playback controls: a plain, unhurried cross-fade. */
    fun controlsEnter(): EnterTransition = fadeIn(tween(Relaxed, easing = smooth))

    fun controlsExit(): ExitTransition = fadeOut(tween(Base, easing = smooth))
}

/**
 * Touch feedback: a compression to 97% and back.
 *
 * The scale starts changing on the same frame as touch-down, which is the
 * single biggest lever for making a tap feel instant. The depth is deliberately
 * shallow — a control that visibly shrinks looks squashed; one that compresses
 * looks pressed.
 */
@Composable
fun Modifier.premiumPressable(
    scaleDown: Float = 0.97f,
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
 * Compression plus dim, for bare glyphs floating over video. With no surface to
 * compress against, the scale alone is too subtle to register; the dim is what
 * actually carries the acknowledgement.
 */
@Composable
fun Modifier.premiumPressableSoft(
    scaleDown: Float = 0.94f,
    dimTo: Float = 0.6f,
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
 * Dim without scale — for rows inside menus and lists, where a scaling row
 * looks broken next to its neighbours.
 */
@Composable
fun Modifier.premiumPressableNoScale(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.55f else 1f,
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
 * A flat translucent panel.
 *
 * This previously also painted a white top-lit gradient and a bright rim on
 * every surface it touched, which is precisely what made the app read as
 * glassmorphism rather than as a dark interface: dozens of panels each
 * announcing their own edge. It is now a fill and nothing else — the border is
 * opt-in by passing a non-transparent [stroke], and almost nothing should.
 */
fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(Radius.lg),
    fill: Color = GlassFill,
    stroke: Color = Color.Transparent,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fill)
    .then(if (stroke == Color.Transparent) Modifier else Modifier.border(strokeWidth, stroke, shape))

/**
 * Panel for surfaces floating directly over video: an opaque black base beneath
 * the translucent fill, so bright frames underneath can't bleed through and
 * muddy the panel. No sheen, no rim.
 */
fun Modifier.glassPanelOverVideo(
    shape: Shape = RoundedCornerShape(Radius.lg),
    baseAlpha: Float = 0.55f,
    fill: Color = GlassFill,
    stroke: Color = Color.Transparent,
    strokeWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(Color.Black.copy(alpha = baseAlpha))
    .background(fill)
    .then(if (stroke == Color.Transparent) Modifier else Modifier.border(strokeWidth, stroke, shape))

/**
 * The ambient wash behind screen content: black, with one barely-there neutral
 * lift at the top so it reads as a deep surface rather than a void.
 *
 * There is deliberately no accent bleed. A tinted backdrop is what made every
 * screen look purple regardless of what was actually drawn on it.
 */
fun Modifier.auroraBackdrop(): Modifier = this.drawBehind {
    drawRect(BackgroundPrimary)

    val top = Offset(size.width * 0.5f, -size.height * 0.10f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BleedNeutral, Color.Transparent),
            center = top,
            radius = size.width * 1.15f
        ),
        radius = size.width * 1.15f,
        center = top
    )
}
