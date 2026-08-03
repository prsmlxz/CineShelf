package com.cineshelf.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.*

data class PopupOption(val key: String, val label: String, val trailing: String? = null)

/**
 * Bottom-anchored glass sheet used for every "choose one" control. Slides up
 * on a spring with the scrim fading in behind it.
 */
@Composable
fun BoxScope.GlassPopupMenu(
    title: String,
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
            .glassPanel(
                shape = RoundedCornerShape(Radius.xxl),
                fill = SurfaceCardElevated.copy(alpha = 0.94f),
                stroke = GlassStrokeBright
            )
            .padding(vertical = Spacing.sm)
    ) {
        // Grabber
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = Spacing.xs)
                .size(width = 36.dp, height = 4.dp)
                .background(HairlineStrong, RoundedCornerShape(Radius.pill))
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = TextTertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
        )
        LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
            items(options, key = { it.key }) { option ->
                val isSelected = option.key == selectedKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
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
                            style = MaterialTheme.typography.bodySmall,
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
                                Icons.Outlined.Check,
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
            .glassPanel(
                shape = RoundedCornerShape(Radius.lg),
                fill = ScrimStrong,
                stroke = GlassStroke
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(Spacing.sm))
        Box(
            Modifier
                .width(90.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White.copy(alpha = 0.20f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .background(
                        Brush.horizontalGradient(listOf(AccentPrimary, AccentGlow)),
                        RoundedCornerShape(Radius.pill)
                    )
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * The preview card shown while scrubbing: a real decoded frame with the target
 * timestamp and delta beneath it.
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
            .glassPanel(
                shape = RoundedCornerShape(Radius.md),
                fill = ScrimStrong,
                stroke = GlassStrokeBright
            )
            .padding(Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(168.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Radius.sm))
                .background(Color.Black)
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
        Text(
            timeLabel,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            deltaLabel,
            style = MaterialTheme.typography.labelSmall,
            color = AccentGlow
        )
        Spacer(Modifier.height(2.dp))
    }
}

/** Feedback pill for double-tap seek. */
@Composable
fun BoxScope.SeekBubble(text: String, isLeft: Boolean) {
    Column(
        modifier = Modifier
            .align(if (isLeft) Alignment.CenterStart else Alignment.CenterEnd)
            .padding(horizontal = Spacing.xl)
            .auroraGlow(color = AccentPrimary, radius = 20.dp, glowAlpha = 0.35f)
            .glassPanel(shape = CircleShape, fill = ScrimStrong, stroke = GlassStrokeBright)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/** Badge shown while long-press speed boost is held. */
@Composable
fun BoxScope.SpeedBoostBadge(label: String) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 72.dp)
            .auroraGlow(color = AuroraViolet, radius = 18.dp, glowAlpha = 0.4f)
            .glassPanel(shape = CircleShape, fill = ScrimStrong, stroke = GlassStrokeBright)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
