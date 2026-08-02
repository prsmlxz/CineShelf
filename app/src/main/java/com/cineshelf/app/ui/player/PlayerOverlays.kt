package com.cineshelf.app.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.*

data class PopupOption(val key: String, val label: String)

/**
 * A floating, bottom-anchored glass panel used for every "choose one"
 * control (speed, subtitles, audio, sleep timer). Slides up with a fade,
 * dismisses on scrim tap. `headerTrailing` lets a caller drop an extra
 * action next to the title — used by the Subtitles menu for its "Aa"
 * style-settings shortcut, so track selection and appearance live one tap
 * apart instead of being crammed into the same list.
 */
@Composable
fun BoxScope.GlassPopupMenu(
    title: String,
    options: List<PopupOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    headerTrailing: (@Composable () -> Unit)? = null
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
            .padding(Spacing.md)
            .glassPanel(shape = RoundedCornerShape(Radius.xl))
            .padding(vertical = Spacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            headerTrailing?.invoke()
        }
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(options, key = { it.key }) { option ->
                val isSelected = option.key == selectedKey
                val rowBackground by animateColorAsState(
                    targetValue = if (isSelected) AccentSoft else Color.Transparent,
                    label = "menu-row-bg"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = 2.dp)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(rowBackground)
                        .premiumPressableNoScale(onClick = { onSelect(option.key) })
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) AccentPrimary else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
    }
}

/** A minimal clickable surface with no press-scale — used inside menus/lists where scale feels wrong. */
@Composable
fun Modifier.premiumPressableNoScale(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

/**
 * The central play/pause control. Replaces the old solid-white disc with a
 * low-opacity glass surface plus a soft accent halo, so it reads as "an
 * icon with a subtle backing" rather than a sticker pasted on the video.
 */
@Composable
fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "play-button-scale"
    )

    Box(modifier = modifier.size(76.dp), contentAlignment = Alignment.Center) {
        // Soft ambient halo behind the button — a gradient falloff, not a hard ring.
        Box(
            Modifier
                .size(76.dp)
                .background(Brush.radialGradient(listOf(AccentGlow, Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(PlayButtonFill)
                .border(1.dp, HairlineMid, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = isPlaying, animationSpec = tween(160), label = "play-icon") { playing ->
                Icon(
                    if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    // The play triangle's glyph isn't optically centered in its bounds; nudge it right.
                    modifier = Modifier
                        .size(30.dp)
                        .padding(start = if (!playing) 2.dp else 0.dp)
                )
            }
        }
    }
}

/** Small pill HUD shown briefly while adjusting brightness or volume via vertical swipe. */
@Composable
fun BoxScope.LevelHud(icon: ImageVector, level: Float, label: String) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .glassPanel(shape = RoundedCornerShape(Radius.lg))
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(Spacing.xs))
        Box(
            Modifier
                .width(88.dp)
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(Radius.pill))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.coerceIn(0f, 1f))
                    .background(AccentPrimary, RoundedCornerShape(Radius.pill))
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Large centered time-delta HUD shown while horizontally swipe-scrubbing, with a real preview frame when available. */
@Composable
fun BoxScope.ScrubHud(targetTimeLabel: String, deltaLabel: String, thumbnailPath: String? = null) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .glassPanel(shape = RoundedCornerShape(Radius.lg))
            .padding(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (thumbnailPath != null) {
            coil.compose.AsyncImage(
                model = java.io.File(thumbnailPath),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(Radius.sm))
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        Text(targetTimeLabel, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(deltaLabel, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

/** Small pill feedback for double-tap seek, e.g. "-10s" / "+10s". */
@Composable
fun BoxScope.SeekBubble(text: String, isLeft: Boolean) {
    Box(
        modifier = Modifier
            .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
            .padding(horizontal = Spacing.xxl)
            .glassPanel(shape = CircleShape)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
    }
}
