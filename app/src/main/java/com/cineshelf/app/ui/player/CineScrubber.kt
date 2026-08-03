package com.cineshelf.app.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.Canvas
import com.cineshelf.app.ui.theme.AccentGlow
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.AuroraViolet
import com.cineshelf.app.ui.theme.Motion

/**
 * The scrubber.
 *
 * Three things make this feel different from a stock Slider:
 *
 * 1. It grows. Touching the bar springs its height up and its corner radius
 *    with it, so the control physically reacts to the finger rather than just
 *    moving a dot.
 * 2. It never fights the player. While dragging, the bar renders purely from
 *    local drag state; the periodic position updates coming from ExoPlayer are
 *    ignored until the finger lifts. That's what removes the "rubber-banding"
 *    where the thumb snaps backwards mid-drag.
 * 3. It reports position continuously, so the caller can show a preview frame
 *    that tracks the finger, and only commits the real seek on release.
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

    // The "grows in width when you hold it" behaviour: track thickness and the
    // thumb both spring outward, and the whole row lifts slightly.
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 4.dp,
        animationSpec = Motion.bouncy(),
        label = "track-height"
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 11.dp else 6.dp,
        animationSpec = Motion.bouncy(),
        label = "thumb-radius"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0.45f,
        animationSpec = Motion.standard(),
        label = "track-glow"
    )

    fun fractionFor(x: Float): Float = (x / trackWidthPx).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // A tall, transparent hit area: the visible bar is thin, but the
            // finger target stays comfortable.
            .height(40.dp)
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
                .height(28.dp)
        ) {
            val centerY = size.height / 2f
            val h = trackHeight.toPx()
            val r = h / 2f
            val thumbR = thumbRadius.toPx()
            // Keep the thumb fully on-screen at both extremes.
            val usable = size.width - thumbR * 2f
            val playedX = thumbR + usable * playedFraction
            val bufferedX = thumbR + usable * bufferedFraction

            // Inactive track.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(0f, centerY - r),
                size = Size(size.width, h),
                cornerRadius = CornerRadius(r)
            )

            // Buffered ahead.
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.26f),
                    topLeft = Offset(0f, centerY - r),
                    size = Size(bufferedX.coerceAtLeast(h), h),
                    cornerRadius = CornerRadius(r)
                )
            }

            // Played portion — an aurora gradient rather than a flat fill.
            if (playedFraction > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AuroraViolet, AccentPrimary, AccentGlow),
                        startX = 0f,
                        endX = playedX.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(0f, centerY - r),
                    size = Size(playedX.coerceAtLeast(h), h),
                    cornerRadius = CornerRadius(r)
                )
            }

            // Glow beneath the thumb.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentGlow.copy(alpha = 0.55f * glowAlpha), Color.Transparent),
                    center = Offset(playedX, centerY),
                    radius = thumbR * 3.2f
                ),
                radius = thumbR * 3.2f,
                center = Offset(playedX, centerY)
            )

            // Thumb.
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
