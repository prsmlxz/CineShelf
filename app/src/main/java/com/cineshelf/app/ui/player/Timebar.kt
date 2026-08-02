package com.cineshelf.app.ui.player

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.AccentGlow
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.HairlineMid
import com.cineshelf.app.ui.theme.Radius
import com.cineshelf.app.ui.theme.Spacing
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The scrub track. A dedicated component (rather than Material's generic
 * Slider) so it can do the three things asked for: grow smoothly the
 * instant it's held, show a real frame preview of where a drag will land,
 * and never stutter — the preview lookup is a cheap nearest-neighbor scan
 * over ~10-20 pre-cached, already-decoded thumbnails, not a fresh decode
 * per frame, so there's nothing here that *can* lag.
 */
@Composable
fun Timebar(
    positionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    thumbnails: List<Pair<Long, String>>,
    onScrubStart: () -> Unit,
    onScrubMove: (Long) -> Unit,
    onScrubEnd: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)

    var isScrubbing by remember { mutableStateOf(false) }
    var dragXPx by remember { mutableStateOf(0f) }
    var trackWidthPx by remember { mutableStateOf(1f) }

    val trackHeight by animateDpAsState(
        targetValue = if (isScrubbing) 8.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "timebar-height"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isScrubbing) 20.dp else 13.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "timebar-thumb"
    )

    fun fractionFor(xPx: Float) = (xPx / trackWidthPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    fun msFor(xPx: Float) = (fractionFor(xPx) * safeDuration).toLong()

    val displayFraction = if (isScrubbing) {
        fractionFor(dragXPx)
    } else {
        (positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    Column(modifier = modifier) {
        if (isScrubbing) {
            val targetMs = msFor(dragXPx)
            val nearestThumb = remember(targetMs, thumbnails) {
                thumbnails.minByOrNull { abs(it.first - targetMs) }?.second
            }
            TimebarPeek(
                timeLabel = formatTime(targetMs),
                thumbnailPath = nearestThumb,
                fraction = displayFraction,
                trackWidthPx = trackWidthPx
            )
            Spacer(Modifier.height(Spacing.xs))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(durationMs) {
                    detectTapGestures(
                        onTap = { offset ->
                            onScrubStart()
                            onScrubEnd(msFor(offset.x))
                        }
                    )
                }
                .pointerInput(durationMs) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            dragXPx = offset.x
                            onScrubStart()
                        },
                        onDragEnd = {
                            onScrubEnd(msFor(dragXPx))
                            isScrubbing = false
                        },
                        onDragCancel = { isScrubbing = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragXPx = change.position.x.coerceIn(0f, trackWidthPx)
                            onScrubMove(msFor(dragXPx))
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Unplayed track
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White.copy(alpha = 0.20f))
            )
            // Buffered segment
            Box(
                Modifier
                    .fillMaxWidth(bufferedPercentage / 100f)
                    .align(Alignment.CenterStart)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Color.White.copy(alpha = 0.34f))
            )
            // Played segment
            Box(
                Modifier
                    .fillMaxWidth(displayFraction)
                    .align(Alignment.CenterStart)
                    .height(trackHeight)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(AccentPrimary)
            )
            // Thumb, with a soft glow ring while actively dragging
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset((trackWidthPx * displayFraction).roundToInt() - thumbSize.roundToPx() / 2, 0)
                    }
            ) {
                if (isScrubbing) {
                    Box(
                        Modifier
                            .size(thumbSize + 22.dp)
                            .align(Alignment.Center)
                            .background(Brush.radialGradient(listOf(AccentGlow, Color.Transparent)), CircleShape)
                    )
                }
                Box(
                    Modifier
                        .size(thumbSize)
                        .align(Alignment.Center)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

/** Floating frame-preview shown above the thumb while dragging. */
@Composable
private fun TimebarPeek(timeLabel: String, thumbnailPath: String?, fraction: Float, trackWidthPx: Float) {
    var bubbleWidthPx by remember { mutableStateOf(0) }
    val rawX = trackWidthPx * fraction - bubbleWidthPx / 2f
    val clampedX = rawX.coerceIn(0f, (trackWidthPx - bubbleWidthPx).coerceAtLeast(0f))

    Box(
        modifier = Modifier
            .offset { IntOffset(clampedX.roundToInt(), 0) }
            .onSizeChanged { bubbleWidthPx = it.width }
            .clip(RoundedCornerShape(Radius.md))
            .background(Color(0xFF1C1C1F).copy(alpha = 0.94f))
            .border(1.dp, HairlineMid, RoundedCornerShape(Radius.md))
            .padding(Spacing.xxs)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (thumbnailPath != null) {
                coil.compose.AsyncImage(
                    model = File(thumbnailPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(Radius.sm))
                )
            }
            Text(
                timeLabel,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
    }
}
