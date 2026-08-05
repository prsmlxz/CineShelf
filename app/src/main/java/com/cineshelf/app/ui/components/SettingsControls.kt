package com.cineshelf.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.AccentBright
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.HairlineLight
import com.cineshelf.app.ui.theme.Motion
import com.cineshelf.app.ui.theme.Radius
import com.cineshelf.app.ui.theme.SectionEyebrow
import com.cineshelf.app.ui.theme.Spacing
import com.cineshelf.app.ui.theme.SurfaceCard
import com.cineshelf.app.ui.theme.SurfaceRaised
import com.cineshelf.app.ui.theme.TextPrimary
import com.cineshelf.app.ui.theme.TextSecondary
import com.cineshelf.app.ui.theme.TextTertiary
import com.cineshelf.app.ui.theme.premiumPressable
import com.cineshelf.app.ui.theme.premiumPressableNoScale
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Settings primitives.
//
// The previous settings UI was a stack of identical stepper rows — every
// control the same shape, so nothing communicated what kind of value it held.
// These replace it with affordances chosen by the shape of the data: a
// segmented control for a handful of named options, a slider for a continuous
// range, a toggle for a boolean. Grouping and headers come from SettingsGroup
// so related controls read as one card.
// ---------------------------------------------------------------------------

/** A titled group of settings rows on one card, with a quiet header above. */
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            title,
            style = SectionEyebrow,
            color = TextTertiary,
            modifier = Modifier.padding(start = Spacing.xs, bottom = Spacing.xs)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(SurfaceCard)
                .padding(vertical = Spacing.xs),
            content = content
        )
        if (footnote != null) {
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
            )
        }
    }
}

/**
 * iOS-style segmented control: one pill slides beneath the selected label
 * rather than each segment lighting up independently.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = index.toFloat(),
        animationSpec = Motion.standard(),
        label = "segment-index"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(SurfaceRaised)
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / options.size.coerceAtLeast(1)

        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Radius.xs))
                .background(Color.White.copy(alpha = 0.13f))
        )

        Row(Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val isSelected = option == selected
                val color by animateColorAsState(
                    targetValue = if (isSelected) TextPrimary else TextSecondary,
                    animationSpec = Motion.fade(Motion.Quick),
                    label = "segment-color"
                )
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .premiumPressableNoScale(onClick = { onSelect(option) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        labelOf(option),
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * A labelled slider row. The current value renders beside the label so the
 * setting is legible without interpreting the thumb's position.
 */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    Column(modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(valueLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        CineSlider(value = value, onValueChange = onValueChange, steps = steps)
    }
}

/**
 * A slider built by hand rather than Material's.
 *
 * Material's Slider brings a ripple, a value-indicator popup and its own touch
 * slop, none of which fit here — and its track is a single weight, whereas this
 * uses the same thick-active / hairline-inactive treatment as the scrubber so
 * the two read as one family of control.
 */
@Composable
fun CineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0
) {
    val density = LocalDensity.current
    val view = LocalView.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf(false) }

    val thumbRadius by animateDpAsState(
        targetValue = if (dragging) 11.dp else 8.dp,
        animationSpec = Motion.bouncy(),
        label = "slider-thumb"
    )

    fun quantise(raw: Float): Float {
        val clamped = raw.coerceIn(0f, 1f)
        if (steps <= 1) return clamped
        val n = steps - 1
        return ((clamped * n).roundToInt().toFloat() / n).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(steps) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    widthPx = size.width.toFloat()
                    dragging = true
                    var last = quantise(down.position.x / widthPx)
                    onValueChange(last)
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    down.consume()
                    drag(down.id) { change ->
                        val next = quantise(change.position.x / widthPx)
                        if (next != last) {
                            last = next
                            onValueChange(next)
                            if (steps > 1) {
                                view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.KEYBOARD_TAP
                                )
                            }
                        }
                        change.consume()
                    }
                    dragging = false
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        widthPx = with(density) { maxWidth.toPx() }
        val fraction = value.coerceIn(0f, 1f)
        val thumbDp = thumbRadius

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val cy = size.height / 2f
            val thumbPx = thumbDp.toPx()
            val usable = size.width - thumbPx * 2f
            val x = thumbPx + usable * fraction

            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(0f, cy - 1.5f),
                size = Size(size.width, 3f),
                cornerRadius = CornerRadius(1.5f)
            )
            drawRoundRect(
                color = AccentPrimary,
                topLeft = Offset(0f, cy - 3f),
                size = Size(x.coerceAtLeast(6f), 6f),
                cornerRadius = CornerRadius(3f)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = thumbPx + 1.5f,
                center = Offset(x, cy)
            )
            drawCircle(color = Color.White, radius = thumbPx, center = Offset(x, cy))
        }
    }
}

/** A row with a label, optional icon, trailing value, and optional tap target. */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.premiumPressableNoScale(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
        if (trailing != null) {
            if (value != null) Spacer(Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/** Hairline divider used between rows inside a group. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = Spacing.md)
            .height(1.dp)
            .background(HairlineLight)
    )
}

/** iOS-style toggle. */
@Composable
fun CineToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) AccentPrimary else SurfaceRaised,
        animationSpec = Motion.fade(Motion.Quick),
        label = "toggle-track"
    )
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = Motion.standard(),
        label = "toggle-knob"
    )
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 26.dp)
            .clip(CircleShape)
            .background(trackColor)
            .premiumPressable(scaleDown = 0.92f) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Small pill button for inline actions inside settings sheets. */
@Composable
fun MiniActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val bg by animateColorAsState(
        targetValue = if (active) AccentPrimary.copy(alpha = 0.20f) else SurfaceRaised,
        animationSpec = Motion.fade(Motion.Quick),
        label = "chip-bg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(bg)
            .premiumPressable(scaleDown = 0.93f, onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) AccentBright else TextSecondary,
            maxLines = 1
        )
    }
}
