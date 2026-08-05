package com.cineshelf.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.AccentBright
import com.cineshelf.app.ui.theme.AccentPrimary
import com.cineshelf.app.ui.theme.AccentSoft
import com.cineshelf.app.ui.theme.BadgeLabel
import com.cineshelf.app.ui.theme.GlassFill
import com.cineshelf.app.ui.theme.GlassStroke
import com.cineshelf.app.ui.theme.GlassStrokeBright
import com.cineshelf.app.ui.theme.HairlineStrong
import com.cineshelf.app.ui.theme.Motion
import com.cineshelf.app.ui.theme.Radius
import com.cineshelf.app.ui.theme.ScrimMedium
import com.cineshelf.app.ui.theme.ScrimStrong
import com.cineshelf.app.ui.theme.SectionEyebrow
import com.cineshelf.app.ui.theme.Spacing
import com.cineshelf.app.ui.theme.SurfaceCardElevated
import com.cineshelf.app.ui.theme.SurfaceRaised
import com.cineshelf.app.ui.theme.TextPrimary
import com.cineshelf.app.ui.theme.TextSecondary
import com.cineshelf.app.ui.theme.TextTertiary
import com.cineshelf.app.ui.theme.glassPanelOverVideo
import com.cineshelf.app.ui.theme.premiumPressableNoScale

data class PopupOption(val key: String, val label: String, val trailing: String? = null)

/**
 * Shared header for every sheet: an accent-tinted glyph, a title, and a line of
 * context. Previously all four sheets opened with the same bare grey caption,
 * which is what made them indistinguishable from one another.
 */
@Composable
fun SheetHeader(icon: ImageVector, title: String, subtitle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(AccentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AccentBright,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
        }
    }
}

/** The grabber handle at the top of a sheet. */
@Composable
fun SheetGrabber() {
    Box(
        Modifier
            .padding(bottom = Spacing.xs)
            .size(width = 34.dp, height = 4.dp)
            .background(HairlineStrong, RoundedCornerShape(Radius.pill))
    )
}

/**
 * Bottom-anchored sheet used for every "choose one" control. The scrim dims the
 * video behind it so the sheet reads as floating above the picture rather than
 * pasted onto it.
 */
@Composable
fun BoxScope.GlassPopupMenu(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    options: List<PopupOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScrimMedium)
            .premiumPressableNoScale(onClick = onDismiss)
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(Spacing.sm)
            .glassPanelOverVideo(
                shape = RoundedCornerShape(Radius.xxl),
                baseAlpha = 0.90f,
                fill = SurfaceCardElevated.copy(alpha = 0.86f),
                stroke = GlassStrokeBright
            )
            .padding(vertical = Spacing.sm)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally)) { SheetGrabber() }
        SheetHeader(icon = icon, title = title, subtitle = subtitle)
        Spacer(Modifier.height(Spacing.xs))
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(options, key = { it.key }) { option ->
                val isSelected = option.key == selectedKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(if (isSelected) AccentSoft else Color.Transparent)
                        .premiumPressableNoScale(onClick = { onSelect(option.key) })
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    option.trailing?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(end = Spacing.xs)
                        )
                    }
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(AccentPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

/** Brightness/volume HUD shown during a vertical swipe. */
@Composable
fun BoxScope.LevelHud(icon: ImageVector, level: Float, label: String) {
    val animated by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = Motion.standard(),
        label = "hud-level"
    )
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .glassPanelOverVideo(
                shape = RoundedCornerShape(Radius.lg),
                baseAlpha = 0.60f,
                fill = GlassFill,
                stroke = GlassStroke
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(Spacing.sm))
        Box(
            Modifier
                .width(88.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(Color.White, RoundedCornerShape(Radius.pill))
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.92f)
        )
    }
}

/**
 * The preview card shown while scrubbing: a real decoded frame with the target
 * timestamp and delta beneath it. Shows a neutral placeholder rather than a
 * stale frame when no nearby frame has decoded yet.
 */
@Composable
fun ScrubPreviewCard(
    frame: ImageBitmap?,
    timeLabel: String,
    deltaLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .glassPanelOverVideo(
                shape = RoundedCornerShape(Radius.md),
                baseAlpha = 0.72f,
                fill = GlassFill,
                stroke = GlassStrokeBright
            )
            .padding(Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.sm))
                .background(SurfaceRaised)
        ) {
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                timeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(deltaLabel, style = BadgeLabel, color = AccentBright)
        }
        Spacer(Modifier.height(2.dp))
    }
}

/** A double-tap seek gesture awaiting its animation. [token] retriggers it. */
data class SeekRipple(val seconds: Int, val isLeft: Boolean, val token: Int)

/**
 * Double-tap seek feedback.
 *
 * Not a circle badge. A soft wash of light sweeps in from the tapped edge and
 * fades away, with a trio of chevrons rippling in the direction of travel — the
 * whole side of the screen acknowledges the gesture rather than a floating pill
 * appearing over the picture.
 *
 * The envelope rises fast and falls slow, which is what makes it read as light
 * catching the panel rather than a UI element being shown and hidden.
 */
@Composable
fun BoxScope.SeekRippleOverlay(ripple: SeekRipple) {
    val progress = remember(ripple.token) { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(ripple.token) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 620, easing = Motion.smooth))
    }

    val t = progress.value
    // Fast attack, long release.
    val envelope = if (t < 0.22f) t / 0.22f else 1f - ((t - 0.22f) / 0.78f)
    val alpha = envelope.coerceIn(0f, 1f)
    if (alpha <= 0.001f) return

    Box(
        modifier = Modifier
            .align(if (ripple.isLeft) Alignment.CenterStart else Alignment.CenterEnd)
            .fillMaxHeight()
            .fillMaxWidth(0.46f)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // The wash. Drawn as an oversized round rect anchored off the outer edge
        // so only its inner curve is visible on screen.
        Canvas(Modifier.fillMaxSize()) {
            val brush = if (ripple.isLeft) {
                Brush.horizontalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
                    startX = 0f,
                    endX = size.width
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.13f)),
                    startX = 0f,
                    endX = size.width
                )
            }
            val bulge = size.width * 0.55f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(if (ripple.isLeft) -bulge else 0f, 0f),
                size = Size(size.width + bulge, size.height),
                cornerRadius = CornerRadius(size.width * 0.75f)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Chevrons light in sequence, trailing in the seek direction.
                val order = if (ripple.isLeft) listOf(0, 1, 2) else listOf(2, 1, 0)
                order.forEach { slot ->
                    val phase = (t * 3.1f - slot * 0.38f).coerceIn(0f, 1f)
                    val chevronAlpha = if (phase <= 0f) 0f else (1f - (phase - 0.5f).coerceAtLeast(0f) * 1.2f)
                    Icon(
                        if (ripple.isLeft) Icons.Rounded.ChevronLeft
                        else Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = chevronAlpha.coerceIn(0f, 1f) * 0.95f),
                        modifier = Modifier
                            .size(26.dp)
                            .padding(horizontal = 0.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${ripple.seconds} seconds",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

/** Badge shown while long-press speed boost is held. */
@Composable
fun BoxScope.SpeedBoostBadge(label: String) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 76.dp)
            .glassPanelOverVideo(
                shape = CircleShape,
                baseAlpha = 0.62f,
                fill = GlassFill,
                stroke = GlassStrokeBright
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = BadgeLabel, color = Color.White)
    }
}

/**
 * Confirmation that playback picked up where it left off. Purely informational —
 * there is no button, because the resume already happened. The previous build
 * asked the user to choose, which meant every episode opened with a decision.
 */
@Composable
fun BoxScope.AutoResumeToast(timeLabel: String) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 76.dp)
            .glassPanelOverVideo(
                shape = CircleShape,
                baseAlpha = 0.62f,
                fill = GlassFill,
                stroke = GlassStroke
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            "Resumed",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.95f)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(timeLabel, style = BadgeLabel, color = AccentBright)
    }
}

/** Section eyebrow used inside the player's own sheets. */
@Composable
fun PlayerSheetEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = SectionEyebrow,
        color = TextTertiary,
        modifier = modifier
    )
}
