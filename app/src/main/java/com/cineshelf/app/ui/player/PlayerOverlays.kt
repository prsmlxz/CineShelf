package com.cineshelf.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cineshelf.app.ui.theme.*

data class PopupOption(val key: String, val label: String)

/**
 * A floating, bottom-anchored glass panel used for every "choose one"
 * control (speed, subtitles, audio, sleep timer). Slides up with a fade,
 * dismisses on scrim tap — this replaces the old plain DropdownMenu with
 * something that matches the rest of the player's visual language.
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
            .padding(Spacing.md)
            .glassPanel(shape = RoundedCornerShape(Radius.xl))
            .padding(vertical = Spacing.sm)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
        )
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(options, key = { it.key }) { option ->
                val isSelected = option.key == selectedKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .premiumPressableNoScale(onClick = { onSelect(option.key) })
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
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
                    .background(Color.White, RoundedCornerShape(Radius.pill))
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

/** Large centered time-delta HUD shown while horizontally scrubbing, with a real preview frame when available. */
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
