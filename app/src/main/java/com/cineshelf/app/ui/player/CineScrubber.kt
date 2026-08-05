package com.cineshelf.app.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.AccentBright
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.Motion

/**
 * The scrubber.
 *
 * Design notes:
 *
 * - The two rails are deliberately *different* weights. The remaining time is a
 *   hairline; the elapsed time is a solid bar roughly twice as thick, sitting on
 *   the same centreline. That asymmetry is what reads as progress at a glance,
 *   without needing colour to do the work.
 * - It grows on touch. Both rails and the thumb spring outward together, so the
 *   control physically reacts to the finger rather than just moving a dot.
 * - It never fights the player. While dragging, the bar renders purely from
 *   local drag state; position updates from ExoPlayer are ignored until the
 *   finger lifts. That removes the rubber-banding where the thumb snaps
 *   backwards mid-drag.
 * - It reports position continuously, so the caller can show a preview frame
 *   that tracks the finger, and only commits the real seek on release.
 */
@Composable
fun CineScrubber(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    modifier: Modifier = Modifier,
    onScrubStart: () -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit
) {
    val density = LocalDensity.current
    val view = LocalView.current

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val playedFraction = if (isDragging) {
        dragFraction
    } else {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }
    val bufferedFraction = (bufferedMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    // Active rail: already substantial at rest, noticeably heavier on touch.
    val activeHeight by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 5.dp,
        animationSpec = Motion.bouncy(),
        label = "active-height"
    )
    // Inactive rail: a hairline that thickens only slightly, so the contrast
    // between "watched" and "remaining" survives the expansion.
    val inactiveHeight by animateDpAsState(
        targetValue = if (isDragging) 5.dp else 2.5.dp,
        animationSpec = Motion.bouncy(),
        label = "inactive-height"
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 11.dp else 6.5.dp,
        animationSpec = Motion.bouncy(),
        label = "thumb-radius"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.35f,
        animationSpec = Motion.standard(),
        label = "track-glow"
    )

    fun fractionFor(x: Float): Float = (x / trackWidthPx).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // A tall, transparent hit area: the visible bar is thin, but the
            // finger target stays comfortable.
            .height(44.dp)
            .pointerInput(durationMs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    trackWidthPx = size.width.toFloat()
                    isDragging = true
                    dragFraction = fractionFor(down.position.x)
                    onScrubStart()
                    onScrubMove((dragFraction * safeDuration).toLong())
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    down.consume()

                    drag(down.id) { change ->
                        dragFraction = fractionFor(change.position.x)
                        onScrubMove((dragFraction * safeDuration).toLong())
                        change.consume()
                    }

                    isDragging = false
                    onScrubEnd((dragFraction * safeDuration).toLong())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        trackWidthPx = widthPx

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val centerY = size.height / 2f
            val activeH = activeHeight.toPx()
            val inactiveH = inactiveHeight.toPx()
            val thumbR = thumbRadius.toPx()
            // Keep the thumb fully on-screen at both extremes.
            val usable = size.width - thumbR * 2f
            val playedX = thumbR + usable * playedFraction
            val bufferedX = thumbR + usable * bufferedFraction

            fun rail(color: Color, from: Float, to: Float, h: Float) {
                val w = (to - from).coerceAtLeast(h)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(from, centerY - h / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f)
                )
            }

            // Remaining — hairline, running the full width beneath everything.
            rail(Color.White.copy(alpha = 0.14f), 0f, size.width, inactiveH)

            // Buffered ahead — same hairline weight, a shade brighter.
            if (bufferedFraction > playedFraction) {
                rail(Color.White.copy(alpha = 0.24f), 0f, bufferedX, inactiveH)
            }

            // Elapsed — the heavy rail. A short ramp from deep to bright accent
            // rather than a multi-hue gradient, so it reads as one colour.
            if (playedFraction > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AccentPrimary, AccentBright),
                        startX = 0f,
                        endX = playedX.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(0f, centerY - activeH / 2f),
                    size = Size(playedX.coerceAtLeast(activeH), activeH),
                    cornerRadius = CornerRadius(activeH / 2f)
                )
            }

            // Glow beneath the thumb — the one place accent light is allowed to
            // spill past its shape.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentBright.copy(alpha = 0.45f * glowAlpha), Color.Transparent),
                    center = Offset(playedX, centerY),
                    radius = thumbR * 3.4f
                ),
                radius = thumbR * 3.4f,
                center = Offset(playedX, centerY)
            )

            // Thumb, with a soft contact shadow so it sits above the rail rather
            // than being punched out of it.
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = thumbR + 1.5f,
                center = Offset(playedX, centerY)
            )
            drawCircle(color = Color.White, radius = thumbR, center = Offset(playedX, centerY))
        }
    }
}

/**
 * The frame preview that rides above the scrubber while dragging. Positioned
 * to follow the thumb but clamped so it never runs off either edge.
 */
@Composable
fun ScrubPreviewAnchor(
    visible: Boolean,
    fraction: Float,
    previewWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!visible) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val maxW = maxWidth
        val half = previewWidth / 2
        val rawOffset = maxW * fraction.coerceIn(0f, 1f)
        val clamped = rawOffset.coerceIn(half, (maxW - half).coerceAtLeast(half))
        Box(modifier = Modifier.padding(start = clamped - half)) {
            content()
        }
    }
}
